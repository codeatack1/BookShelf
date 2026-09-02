package com.bookshelf.domain.chapter.repository

import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.model.ChapterUpdate
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {

    suspend fun addAll(chapters: List<Chapter>): List<Chapter>

    suspend fun update(chapterUpdate: ChapterUpdate)

    suspend fun updateAll(chapterUpdates: List<ChapterUpdate>)

    suspend fun removeChaptersWithIds(chapterIds: List<Long>)

    suspend fun getChapterByTextbookId(textbookId: Long, applyScanlatorFilter: Boolean = false): List<Chapter>

    suspend fun getScanlatorsByTextbookId(textbookId: Long): List<String>

    fun getScanlatorsByTextbookIdAsFlow(textbookId: Long): Flow<List<String>>

    suspend fun getBookmarkedChaptersByTextbookId(textbookId: Long): List<Chapter>

    suspend fun getChapterById(id: Long): Chapter?

    suspend fun getChapterByTextbookIdAsFlow(textbookId: Long, applyScanlatorFilter: Boolean = false): Flow<List<Chapter>>

    suspend fun getChapterByUrlAndTextbookId(url: String, textbookId: Long): Chapter?
}
