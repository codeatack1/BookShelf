package com.bookshelf.feature.library

import com.bookshelf.domain.library.model.search.AndNode
import com.bookshelf.domain.library.model.search.ComparisonField
import com.bookshelf.domain.library.model.search.ComparisonQueryNode
import com.bookshelf.domain.library.model.search.EmptyQueryNode
import com.bookshelf.domain.library.model.search.FieldQueryNode
import com.bookshelf.domain.library.model.search.GeneralQueryNode
import com.bookshelf.domain.library.model.search.NotNode
import com.bookshelf.domain.library.model.search.OrNode
import com.bookshelf.domain.library.model.search.QueryNode
import com.bookshelf.domain.library.model.search.TextbookField
import com.bookshelf.source.local.LocalSource
import com.bookshelf.ui.library.LibraryItem
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun QueryNode.matches(item: LibraryItem): Boolean {
    return when (this) {
        is AndNode -> children.all { it.matches(item) }
        is OrNode -> children.any { it.matches(item) }
        is NotNode -> !child.matches(item)
        is EmptyQueryNode -> true
        is GeneralQueryNode -> matches(item)
        is FieldQueryNode -> matches(item)
        is ComparisonQueryNode -> matches(item)
    }
}

private fun GeneralQueryNode.matches(item: LibraryItem): Boolean {
    val manga = item.libraryManga.textbook

    // Use when so each added field has to be handled explicitly
    val match = TextbookField.entries.any { field ->
        if (field.fieldOnly) return@any false

        when (field) {
            TextbookField.TITLE -> manga.title.contains(value, ignoreCase = true)
            TextbookField.AUTHOR -> manga.author?.contains(value, ignoreCase = true) ?: false
            TextbookField.ARTIST -> manga.artist?.contains(value, ignoreCase = true) ?: false
            TextbookField.DESCRIPTION -> manga.description?.contains(value, ignoreCase = true) ?: false
            TextbookField.GENRE -> manga.genre?.any { it.contains(value, ignoreCase = true) } ?: false
            TextbookField.SOURCE -> {
                item.sourceName.contains(value, ignoreCase = true) ||
                    (value.equals("local", ignoreCase = true) && manga.source == LocalSource.ID)
            }
            TextbookField.NOTES -> manga.notes.contains(value, ignoreCase = true)

            // field-only queries; unreachable; added here to make `when` exhaustive
            TextbookField.LANGUAGE, TextbookField.SOURCE_ID -> error("How did we get here?")
        }
    }
    return if (negated) !match else match
}

private fun FieldQueryNode.matches(item: LibraryItem): Boolean {
    val manga = item.libraryManga.textbook

    val match = when (field) {
        TextbookField.GENRE -> {
            if (value.isEmpty()) {
                manga.genre.isNullOrEmpty()
            } else {
                manga.genre?.any { it.contains(value, ignoreCase = true) } ?: false
            }
        }

        TextbookField.SOURCE -> {
            if (value.isEmpty()) {
                item.sourceName.isEmpty()
            } else {
                item.sourceName.contains(value, ignoreCase = true) ||
                    (value.equals("local", ignoreCase = true) && manga.source == LocalSource.ID)
            }
        }

        TextbookField.SOURCE_ID -> {
            value.toLongOrNull()?.let { it == manga.source } ?: false
        }

        else -> {
            val text = when (field) {
                TextbookField.TITLE -> manga.title
                TextbookField.AUTHOR -> manga.author
                TextbookField.ARTIST -> manga.artist
                TextbookField.DESCRIPTION -> manga.description
                TextbookField.NOTES -> manga.notes
                TextbookField.LANGUAGE -> item.sourceLanguage

                // unreachable; added here to make `when` exhaustive
                TextbookField.GENRE, TextbookField.SOURCE, TextbookField.SOURCE_ID -> error("How did we get here?")
            }

            if (value.isEmpty()) {
                text.isNullOrEmpty()
            } else {
                text?.contains(value, ignoreCase = true) ?: false
            }
        }
    }

    return if (negated) !match else match
}

private fun ComparisonQueryNode.matches(item: LibraryItem): Boolean {
    val manga = item.libraryManga.textbook

    fun compareDates(timestamp: Long, value: String): Boolean? {
        val inputDate = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
        val mangaDate = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).date
        return queryComparator.apply(mangaDate, inputDate)
    }

    val match = when (field) {
        ComparisonField.ID -> value.toLongOrNull()?.let { queryComparator.apply(manga.id, it) }

        ComparisonField.DATE_ADDED -> compareDates(manga.dateAdded, value)

        ComparisonField.FETCH_INTERVAL -> value.toIntOrNull()
            ?.let { queryComparator.apply(abs(manga.fetchInterval), it) }

        ComparisonField.NEXT_UPDATE -> compareDates(manga.nextUpdate, value)

        ComparisonField.UNREAD -> {
            value.toLongOrNull()?.let {
                queryComparator.apply(item.unreadCount, it)
            }
        }

        ComparisonField.READ -> {
            value.toLongOrNull()?.let {
                queryComparator.apply(item.libraryManga.readCount, it)
            }
        }

        ComparisonField.TOTAL -> {
            value.toLongOrNull()?.let {
                queryComparator.apply(item.libraryManga.totalChapters, it)
            }
        }
    } ?: false

    return if (negated) !match else match
}
