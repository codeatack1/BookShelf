package com.bookshelf.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.repository.ChapterRepository

@Inject
class GetChaptersByTextbookId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(textbookId: Long, applyScanlatorFilter: Boolean = false): List<Chapter> {
        return try {
            chapterRepository.getChapterByTextbookId(textbookId, applyScanlatorFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
