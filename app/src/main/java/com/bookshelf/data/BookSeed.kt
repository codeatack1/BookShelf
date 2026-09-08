package com.bookshelf.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val TAG = "BookSeed"

@Serializable
data class SeedFile(
    val version: Int,
    val generatedAt: Long,
    val books: List<SeedBook>,
)

@Serializable
data class SeedBook(
    val id: String,
    val title: String,
    val author: String? = null,
    val category: String,
    val lang: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val source: String? = null,
)

object BookSeed {

    private val json = Json { encodeDefaults = true }

    suspend fun importIfEmpty(context: Context) {
        val existing = BookRepository.countBooks()
        if (existing > 0) {
            Log.i(TAG, "Seed skipped: $existing books already exist")
            return
        }
        val text = context.assets.open("seed/books.json").bufferedReader().use { it.readText() }
        val seed = json.decodeFromString<SeedFile>(text)
        val entities = seed.books.map { book ->
            BookEntity(
                id = book.id,
                title = book.title,
                author = book.author,
                category = book.category,
                lang = book.lang,
                coverUrl = book.coverUrl,
                description = book.description,
                genre = book.genre,
                year = book.year,
                source = book.source,
                dateAdded = System.currentTimeMillis(),
                bookmarked = false,
                started = false,
                completed = false,
                lastReadAt = null,
                lastReadChapterId = null,
            )
        }
        BookRepository.insertBooks(entities)
        Log.i(TAG, "Seed imported: ${entities.size} books")
    }
}
