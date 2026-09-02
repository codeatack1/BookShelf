package com.bookshelf.domain.history.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.chapter.interactor.GetChaptersByTextbookId
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.service.getChapterSort
import com.bookshelf.domain.history.repository.HistoryRepository
import com.bookshelf.domain.textbook.interactor.GetTextbook
import kotlin.math.max

@Inject
class GetNextChapters(
    private val getChaptersByTextbookId: GetChaptersByTextbookId,
    private val getManga: GetTextbook,
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(onlyUnread: Boolean = true): List<Chapter> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        return await(history.textbookId, history.chapterId, onlyUnread)
    }

    suspend fun await(textbookId: Long, onlyUnread: Boolean = true): List<Chapter> {
        val manga = getManga.await(textbookId) ?: return emptyList()
        val chapters = getChaptersByTextbookId.await(textbookId, applyScanlatorFilter = true)
            .sortedWith(getChapterSort(manga, sortDescending = false))

        return if (onlyUnread) {
            chapters.filterNot { it.read }
        } else {
            chapters
        }
    }

    suspend fun await(
        textbookId: Long,
        fromChapterId: Long,
        onlyUnread: Boolean = true,
    ): List<Chapter> {
        val chapters = await(textbookId, onlyUnread)
        val currChapterIndex = chapters.indexOfFirst { it.id == fromChapterId }
        val nextChapters = chapters.subList(max(0, currChapterIndex), chapters.size)

        if (onlyUnread) {
            return nextChapters
        }

        // The "next chapter" is either:
        // - The current chapter if it isn't completely read
        // - The chapters after the current chapter if the current one is completely read
        val fromChapter = chapters.getOrNull(currChapterIndex)
        return if (fromChapter != null && !fromChapter.read) {
            nextChapters
        } else {
            nextChapters.drop(1)
        }
    }
}
