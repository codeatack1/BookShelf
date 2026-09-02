package com.bookshelf.domain.chapter.interactor

import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject
import logcat.LogPriority

@Inject
class GetBookmarkedChaptersByTextbookId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(textbookId: Long): List<Chapter> {
        return try {
            chapterRepository.getBookmarkedChaptersByTextbookId(textbookId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
