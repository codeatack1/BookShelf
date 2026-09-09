package com.bookshelf.server

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object BookSearch {

    private const val TAG = "BookSearch"
    private const val SEARCH_URL = "https://pidruchnyk.com.ua/index.php?do=search"
    private const val SITE_URL = "https://pidruchnyk.com.ua"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"

    private val TITLE_REGEX = Regex("""^(.+?)\s*\((.+?)\)\s+(\d+)\s+klas\s*(.*)$""")

    suspend fun search(query: String, page: Int): SearchResponse = withContext(Dispatchers.IO) {
        if (query.trim().length < 4) {
            return@withContext SearchResponse(error = "query_too_short")
        }
        val response = try {
            val doc = Jsoup.connect(SEARCH_URL)
                .userAgent(USER_AGENT)
                .data("do", "search")
                .data("subaction", "search")
                .data("story", query)
                .data("search_start", (page - 1).toString())
                .data("full_search", "0")
                .data("result_from", ((page - 1) * 10 + 1).toString())
                .post()
            val results = doc.select("article.post").mapNotNull { el ->
                val titleEl = el.selectFirst("h2.title a") ?: return@mapNotNull null
                val title = titleEl.text()
                val href = titleEl.absUrl("href").ifEmpty { SITE_URL + titleEl.attr("href") }
                val cover = el.selectFirst("img")?.let { img ->
                    img.absUrl("src").ifEmpty { SITE_URL + img.attr("src") }
                }
                val match = TITLE_REGEX.find(title)
                val subject = match?.groupValues?.getOrNull(1) ?: title
                val author = match?.groupValues?.getOrNull(2)
                val grade = match?.groupValues?.getOrNull(3)?.toIntOrNull()
                val year = match?.groupValues?.getOrNull(4)?.toIntOrNull()
                val description = if (grade != null) "$grade клас · $subject" else null
                SearchResultDto(
                    title = title,
                    author = author,
                    coverUrl = cover,
                    description = description,
                    year = year,
                    source = "pidruchnyk",
                    pageUrl = href,
                    grade = grade,
                )
            }
            Log.d(TAG, "pidruchnyk: results=" + results.size)
            SearchResponse(results = results, hasNextPage = results.size >= 10)
        } catch (e: Exception) {
            SearchResponse(error = "search_unavailable")
        }
        Log.d(TAG, "result: results=" + response.results.size + ", hasNext=" + response.hasNextPage + ", error=" + response.error)
        response
    }
}