package com.bookshelf.domain.textbook.repository

import com.bookshelf.domain.library.model.LibraryTextbook
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.model.TextbookWithChapterCount
import kotlinx.coroutines.flow.Flow

interface TextbookRepository {

    suspend fun getTextbookById(id: Long): Textbook

    fun getTextbookByIdAsFlow(id: Long): Flow<Textbook>

    suspend fun getTextbookByUrlAndSourceId(url: String, sourceId: Long): Textbook?

    fun getTextbookByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Textbook?>

    suspend fun getFavorites(): List<Textbook>

    suspend fun getReadMangaNotInLibrary(): List<Textbook>

    suspend fun getLibraryTextbook(): List<LibraryTextbook>

    fun getLibraryTextbookAsFlow(): Flow<List<LibraryTextbook>>

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Textbook>>

    suspend fun getDuplicateLibraryTextbook(id: Long, title: String): List<TextbookWithChapterCount>

    suspend fun getUpcomingTextbooks(
        statuses: Set<Long>,
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
    ): Flow<List<Textbook>>

    suspend fun resetViewerFlags(): Boolean

    suspend fun setTextbookCategories(textbookId: Long, categoryIds: List<Long>)

    suspend fun update(update: TextbookUpdate): Boolean

    suspend fun updateAll(mangaUpdates: List<TextbookUpdate>): Boolean

    suspend fun insertNetworkTextbook(manga: List<Textbook>): List<Textbook>
}
