package com.bookshelf.server

import android.util.Log
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "BookShelf"

@Serializable
data class LibraryBookDto(
    val id: String,
    val title: String,
    val author: String?,
    val category: String,
    val totalChapters: Int,
    val unreadCount: Int,
    val downloaded: Int,
    val isLocal: Boolean,
    val lang: String,
    val bookmarked: Boolean,
    val started: Boolean,
    val completed: Boolean,
    val lastReadAt: Long?,
    val dateAdded: Long,
)

val sampleBooks: List<LibraryBookDto> = listOf(
    LibraryBookDto(
        id = "1",
        title = "Учебник по математике. 5 класс",
        author = "Виленкин Н.Я., Жохов В.И.",
        category = "Учебники",
        totalChapters = 15,
        unreadCount = 3,
        downloaded = 12,
        isLocal = true,
        lang = "ru",
        bookmarked = false,
        started = true,
        completed = false,
        lastReadAt = 1_755_000_000,
        dateAdded = 1_700_000_000,
    ),
    LibraryBookDto(
        id = "2",
        title = "Физика. 9 класс",
        author = "Пёрышкин А.В., Гутник Е.М.",
        category = "Учебники",
        totalChapters = 10,
        unreadCount = 10,
        downloaded = 0,
        isLocal = false,
        lang = "ru",
        bookmarked = true,
        started = false,
        completed = false,
        lastReadAt = null,
        dateAdded = 1_701_000_000,
    ),
    LibraryBookDto(
        id = "3",
        title = "История России. 6 класс",
        author = "Арсентьев Н.М., Данилов А.А.",
        category = "Учебники",
        totalChapters = 20,
        unreadCount = 1,
        downloaded = 20,
        isLocal = true,
        lang = "ru",
        bookmarked = false,
        started = true,
        completed = true,
        lastReadAt = 1_725_000_000,
        dateAdded = 1_695_000_000,
    ),
    LibraryBookDto(
        id = "4",
        title = "Русский язык. 7 класс",
        author = "Ладыженская Т.А., Баранов М.Т.",
        category = "Учебники",
        totalChapters = 12,
        unreadCount = 12,
        downloaded = 4,
        isLocal = false,
        lang = "ru",
        bookmarked = false,
        started = false,
        completed = false,
        lastReadAt = null,
        dateAdded = 1_720_000_000,
    ),
)

private val json = Json { encodeDefaults = true }

object Api {

    fun register(route: Route) {
        route.get("/api/health") {
            Log.d(TAG, "API GET /api/health")
            call.respondText(
                """{"status":"ok","app":"BookShelf","version":"0.1.0"}""",
                ContentType.Application.Json,
            )
        }

        route.get("/api/library") {
            Log.d(TAG, "API GET /api/library -> ${sampleBooks.size} books")
            val body = json.encodeToString(sampleBooks)
            call.respondText(body, ContentType.Application.Json)
        }
    }
}