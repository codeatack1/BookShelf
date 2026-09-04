package com.bookshelf.server

import android.content.Context
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

object StaticContent {

    private val contentTypes: Map<String, ContentType> = mapOf(
        "html" to ContentType.Text.Html,
        "js" to ContentType("application", "javascript"),
        "css" to ContentType("text", "css"),
        "svg" to ContentType("image", "svg+xml"),
        "png" to ContentType("image", "png"),
        "jpg" to ContentType("image", "jpeg"),
        "jpeg" to ContentType("image", "jpeg"),
        "webp" to ContentType("image", "webp"),
        "gif" to ContentType("image", "gif"),
        "ico" to ContentType("image", "x-icon"),
        "json" to ContentType.Application.Json,
        "woff" to ContentType("font", "woff"),
        "woff2" to ContentType("font", "woff2"),
        "ttf" to ContentType("font", "ttf"),
        "txt" to ContentType.Text.Plain,
        "map" to ContentType("application", "json"),
    )

    fun register(route: Route, context: Context) {
        route.get("/{path...}") { call ->
            serveAsset(call, context)
        }
    }

    private suspend fun serveAsset(call: ApplicationCall, context: Context) {
        val requested = call.parameters["path"] ?: "index.html"
        val normalized = normalize(requested)
        if (normalized == null) {
            call.respondText(
                """{"error":"not_found"}""",
                ContentType.Application.Json,
                HttpStatusCode.NotFound,
            )
            return
        }

        val isIndex = normalized == "index.html"
        val assetPath = "web/$normalized"

        val bytes = try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (_: Exception) {
            null
        }

        if (bytes == null) {
            if (isIndex) {
                call.respondText(
                    context.getString(com.bookshelf.R.string.web_not_built),
                    ContentType.Text.Html,
                    HttpStatusCode.NotFound,
                )
            } else {
                call.respondText(
                    """{"error":"not_found","path":"$normalized"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.NotFound,
                )
            }
            return
        }

        val extension = normalized.substringAfterLast('.', "").lowercase()
        val contentType = contentTypes[extension] ?: ContentType.Application.OctetStream
        call.respondBytes(bytes, contentType)
    }

    private fun normalize(path: String): String? {
        var cleaned = path.removePrefix("/")
        if (cleaned.isEmpty()) cleaned = "index.html"
        val segments = cleaned.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) return null
        return cleaned
    }
}