package com.bookshelf.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object BookSearch {

    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT = 8000
    private const val USER_AGENT = "BookShelf/1.0"

    suspend fun search(query: String, page: Int): SearchResponse = withContext(Dispatchers.IO) {
        val enc = URLEncoder.encode(query, "UTF-8")
        val google = try {
            searchGoogle(enc, page)
        } catch (e: Exception) {
            null
        }
        if (google != null) {
            google
        } else {
            val openLibrary = try {
                searchOpenLibrary(enc, page)
            } catch (e: Exception) {
                null
            }
            openLibrary ?: SearchResponse(error = "search_unavailable")
        }
    }

    private fun searchGoogle(enc: String, page: Int): SearchResponse? {
        val startIndex = (page - 1) * 20
        val url = URL("https://www.googleapis.com/books/v1/volumes?q=$enc&startIndex=$startIndex&maxResults=20&country=US")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", USER_AGENT)
        return try {
            if (conn.responseCode != 200) {
                null
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(body).jsonObject
                val items = root["items"]?.jsonArray ?: return SearchResponse()
                val results = items.mapNotNull { item ->
                    val volumeInfo = item.jsonObject["volumeInfo"]?.jsonObject ?: return@mapNotNull null
                    val title = volumeInfo["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val author = volumeInfo["authors"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                    val coverUrl = volumeInfo["imageLinks"]?.jsonObject?.get("thumbnail")?.jsonPrimitive?.contentOrNull
                        ?.let {
                            if (it.startsWith("http://")) "https://" + it.removePrefix("http://") else it
                        }
                    val description = volumeInfo["description"]?.jsonPrimitive?.contentOrNull
                    val year = volumeInfo["publishedDate"]?.jsonPrimitive?.contentOrNull?.take(4)?.toIntOrNull()
                    SearchResultDto(
                        title = title,
                        author = author,
                        coverUrl = coverUrl,
                        description = description,
                        year = year,
                        source = "google",
                    )
                }
                SearchResponse(results = results, hasNextPage = items.size == 20)
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun searchOpenLibrary(enc: String, page: Int): SearchResponse? {
        val url = URL("https://openlibrary.org/search.json?q=$enc&page=$page&limit=20")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", USER_AGENT)
        return try {
            if (conn.responseCode != 200) {
                null
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(body).jsonObject
                val docs = root["docs"]?.jsonArray.orEmpty()
                val start = root["start"]?.jsonPrimitive?.longOrNull ?: 0L
                val numFound = root["numFound"]?.jsonPrimitive?.longOrNull ?: 0L
                val results = docs.mapNotNull { doc ->
                    val docObj = doc.jsonObject
                    val title = docObj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val author = docObj["author_name"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                    val coverId = docObj["cover_i"]?.jsonPrimitive?.longOrNull
                    val coverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
                    val year = docObj["first_publish_year"]?.jsonPrimitive?.longOrNull?.toInt()
                    SearchResultDto(
                        title = title,
                        author = author,
                        coverUrl = coverUrl,
                        year = year,
                        source = "openlibrary",
                    )
                }
                val hasNextPage = (start + numFound) > (page * 20L)
                SearchResponse(results = results, hasNextPage = hasNextPage)
            }
        } finally {
            conn.disconnect()
        }
    }
}