package com.bookshelf.server

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object BookSearch {

    private const val TAG = "BookSearch"
    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT = 8000
    private const val USER_AGENT = "BookShelf/1.0"
    private const val GOOGLE_RETRY_DELAY_MS = 800L

    private data class ProviderResult(
        val code: Int?,
        val response: SearchResponse?,
        val retryable: Boolean = false,
    ) {
        val isOk: Boolean
            get() = code != null && code >= 200 && code <= 299
    }

    suspend fun search(query: String, page: Int): SearchResponse = withContext(Dispatchers.IO) {
        val enc = URLEncoder.encode(query, "UTF-8")

        val google = searchGoogle(enc, page)
        val googleResp = google.response
        if (google.isOk && googleResp != null && googleResp.results.isNotEmpty()) {
            Log.d(
                TAG,
                "result: results=" + googleResp.results.size + ", hasNext=" + googleResp.hasNextPage + ", error=" + googleResp.error,
            )
            return@withContext googleResp
        }

        val openLibrary = searchOpenLibrary(enc, page)
        val openResp = openLibrary.response
        Log.d(TAG, "openlibrary: http=" + openLibrary.code + ", docs=" + (openResp?.results?.size ?: 0))

        val result = when {
            google.isOk -> {
                if (openResp != null && openResp.results.isNotEmpty()) {
                    openResp
                } else {
                    googleResp?.copy(error = null) ?: SearchResponse(error = "search_unavailable")
                }
            }
            openLibrary.isOk -> {
                if (openResp != null && openResp.results.isNotEmpty()) {
                    openResp
                } else {
                    SearchResponse(error = "search_unavailable")
                }
            }
            else -> SearchResponse(error = "search_unavailable")
        }
        Log.d(
            TAG,
            "result: results=" + result.results.size + ", hasNext=" + result.hasNextPage + ", error=" + result.error,
        )
        result
    }

    private suspend fun searchGoogle(enc: String, page: Int): ProviderResult {
        var attempt = attemptGoogle(enc, page)
        if (attempt.code == 429 || attempt.code == 403 || attempt.retryable) {
            delay(GOOGLE_RETRY_DELAY_MS)
            attempt = attemptGoogle(enc, page)
        }
        Log.d(TAG, "google: http=" + attempt.code + ", items=" + (attempt.response?.results?.size ?: 0))
        return attempt
    }

    private suspend fun attemptGoogle(enc: String, page: Int): ProviderResult {
        val startIndex = (page - 1) * 20
        val url = URL("https://www.googleapis.com/books/v1/volumes?q=$enc&startIndex=$startIndex&maxResults=20&country=US")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", USER_AGENT)
        return try {
            val code = conn.responseCode
            if (code != 200) {
                ProviderResult(code = code, response = null)
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(body).jsonObject
                val items = root["items"]?.jsonArray.orEmpty()
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
                ProviderResult(code = code, response = SearchResponse(results = results, hasNextPage = items.size == 20))
            }
        } catch (e: IOException) {
            ProviderResult(code = null, response = null, retryable = true)
        } catch (e: Exception) {
            ProviderResult(code = null, response = null)
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun searchOpenLibrary(enc: String, page: Int): ProviderResult {
        val url = URL("https://openlibrary.org/search.json?q=$enc&page=$page&limit=20")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", USER_AGENT)
        return try {
            val code = conn.responseCode
            if (code != 200) {
                ProviderResult(code = code, response = null)
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
                ProviderResult(code = code, response = SearchResponse(results = results, hasNextPage = hasNextPage))
            }
        } catch (e: Exception) {
            ProviderResult(code = null, response = null)
        } finally {
            conn.disconnect()
        }
    }
}