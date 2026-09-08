package com.bookshelf.server

import android.util.Log
import com.bookshelf.data.AppStorage
import com.bookshelf.data.BookEntity
import com.bookshelf.data.BookRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "BookShelf"

@Serializable
data class LibraryBookDto(
    val id: String,
    val title: String,
    val author: String?,
    val category: String,
    val cover: String?,
    val description: String?,
    val genre: String?,
    val year: Int?,
    val source: String?,
    val lang: String,
    val totalChapters: Int,
    val unreadCount: Int,
    val downloaded: Int,
    val isLocal: Boolean,
    val bookmarked: Boolean,
    val started: Boolean,
    val completed: Boolean,
    val lastReadAt: Long?,
    val dateAdded: Long,
)

@Serializable
data class ChapterDto(
    val id: String,
    val number: Int,
    val name: String,
    val contentType: String,
    val read: Boolean,
)

@Serializable
data class ChapterContentDto(
    val id: String,
    val number: Int,
    val name: String,
    val contentType: String,
    val content: String,
    val read: Boolean,
)

@Serializable
data class BookDetailDto(
    val book: LibraryBookDto,
    val chapters: List<ChapterDto>,
)

@Serializable
data class ProgressPutRequest(
    val started: Boolean? = null,
    val completed: Boolean? = null,
    val lastReadAt: Long? = null,
    val lastReadChapterId: String? = null,
    val chapterId: String? = null,
    val read: Boolean? = null,
    val bookmarked: Boolean? = null,
)

@Serializable
data class StatePutRequest(val value: String)

@Serializable
data class StateResponse(val key: String, val value: String)

@Serializable
data class StateOkResponse(val ok: Boolean = true)

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
            val query = call.request.queryParameters["query"]
            val category = call.request.queryParameters["category"]
            Log.d(TAG, "API GET /api/library query=$query category=$category")
            val books = BookRepository.searchBooks(query, category)
            val dtos = books.map { book ->
                val totalChapters = BookRepository.chapterCount(book.id)
                val unreadCount = BookRepository.unreadCount(book.id)
                book.toDto(totalChapters, unreadCount)
            }
            val body = json.encodeToString(dtos)
            call.respondText(body, ContentType.Application.Json)
        }

        route.get("/api/books/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest,
                )
                return@get
            }
            Log.d(TAG, "API GET /api/books id=$id")
            val book = BookRepository.getBook(id)
            if (book == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.NotFound,
                )
                return@get
            }
            val totalChapters = BookRepository.chapterCount(id)
            val unreadCount = BookRepository.unreadCount(id)
            val chapters = BookRepository.getChapters(id)
            val dto = BookDetailDto(
                book = book.toDto(totalChapters, unreadCount),
                chapters = chapters.map {
                    ChapterDto(
                        id = it.id,
                        number = it.number,
                        name = it.name,
                        contentType = it.contentType,
                        read = it.read,
                    )
                },
            )
            val body = json.encodeToString(dto)
            call.respondText(body, ContentType.Application.Json)
        }

        route.get("/api/books/{bookId}/chapters/{number}") {
            val bookId = call.parameters["bookId"]
            if (bookId == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest,
                )
                return@get
            }
            val numberStr = call.parameters["number"]
            val number = numberStr?.toIntOrNull()
            if (number == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest,
                )
                return@get
            }
            Log.d(TAG, "API GET /api/books/$bookId/chapters/$number")
            val book = BookRepository.getBook(bookId)
            if (book == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.NotFound,
                )
                return@get
            }
            val chapter = BookRepository.getChapter(bookId, number)
            if (chapter == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.NotFound,
                )
                return@get
            }
            val body = json.encodeToString(
                ChapterContentDto(
                    id = chapter.id,
                    number = chapter.number,
                    name = chapter.name,
                    contentType = chapter.contentType,
                    content = chapter.content,
                    read = chapter.read,
                )
            )
            call.respondText(body, ContentType.Application.Json)
        }

        route.put("/api/books/{id}/progress") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest,
                )
                return@put
            }
            val text = call.receiveText()
            val request = try {
                json.decodeFromString<ProgressPutRequest>(text)
            } catch (e: Exception) {
                Log.e(TAG, "API PUT /api/books/$id/progress: bad body", e)
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest,
                )
                return@put
            }
            Log.d(TAG, "API PUT /api/books/$id/progress $request")
            val book = BookRepository.getBook(id)
            if (book == null) {
                call.respondText(
                    json.encodeToString(StateOkResponse(ok = false)),
                    ContentType.Application.Json,
                    status = HttpStatusCode.NotFound,
                )
                return@put
            }
            if (request.chapterId != null && request.read != null) {
                BookRepository.setChapterRead(id, request.chapterId, request.read)
            }
            var updated = book
            if (request.started != null) updated = updated.copy(started = request.started)
            if (request.completed != null) updated = updated.copy(completed = request.completed)
            if (request.lastReadAt != null) updated = updated.copy(lastReadAt = request.lastReadAt)
            if (request.lastReadChapterId != null) updated = updated.copy(lastReadChapterId = request.lastReadChapterId)
            if (request.bookmarked != null) updated = updated.copy(bookmarked = request.bookmarked)
            BookRepository.updateBook(updated)
            val body = json.encodeToString(StateOkResponse())
            call.respondText(body, ContentType.Application.Json)
        }

        route.get("/api/state/{key}") {
            val key = call.parameters["key"].orEmpty()
            Log.d(TAG, "API GET /api/state key=$key")
            val value = AppStorage.getString(key)
            if (value == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val body = json.encodeToString(StateResponse(key = key, value = value))
                call.respondText(body, ContentType.Application.Json)
            }
        }

        route.put("/api/state/{key}") {
            val key = call.parameters["key"].orEmpty()
            Log.d(TAG, "API PUT /api/state key=$key")
            if (key.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }
            val text = call.receiveText()
            val request = json.decodeFromString<StatePutRequest>(text)
            val value = request.value
            if (value.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }
            AppStorage.putString(key, value)
            val body = json.encodeToString(StateOkResponse())
            call.respondText(body, ContentType.Application.Json)
        }
    }
}

private fun BookEntity.toDto(totalChapters: Int, unreadCount: Int): LibraryBookDto = LibraryBookDto(
    id = id,
    title = title,
    author = author,
    category = category,
    cover = coverUrl,
    description = description,
    genre = genre,
    year = year,
    source = source,
    lang = lang,
    totalChapters = totalChapters,
    unreadCount = unreadCount,
    downloaded = 0,
    isLocal = totalChapters > 0,
    bookmarked = bookmarked,
    started = started,
    completed = completed,
    lastReadAt = lastReadAt,
    dateAdded = dateAdded,
)
