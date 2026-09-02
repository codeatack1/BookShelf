package com.bookshelf.domain.textbook.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.repository.TextbookRepository

@Inject
class SetTextbookChapterFlags(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun awaitSetDownloadedFilter(manga: Textbook, flag: Long): Boolean {
        return mangaRepository.update(
            TextbookUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Textbook.CHAPTER_DOWNLOADED_MASK),
            ),
        )
    }

    suspend fun awaitSetUnreadFilter(manga: Textbook, flag: Long): Boolean {
        return mangaRepository.update(
            TextbookUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Textbook.CHAPTER_UNREAD_MASK),
            ),
        )
    }

    suspend fun awaitSetBookmarkFilter(manga: Textbook, flag: Long): Boolean {
        return mangaRepository.update(
            TextbookUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Textbook.CHAPTER_BOOKMARKED_MASK),
            ),
        )
    }

    suspend fun awaitSetDisplayMode(manga: Textbook, flag: Long): Boolean {
        return mangaRepository.update(
            TextbookUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Textbook.CHAPTER_DISPLAY_MASK),
            ),
        )
    }

    suspend fun awaitSetSortingModeOrFlipOrder(manga: Textbook, flag: Long): Boolean {
        val newFlags = manga.chapterFlags.let {
            if (manga.sorting == flag) {
                // Just flip the order
                val orderFlag = if (manga.sortDescending()) {
                    Textbook.CHAPTER_SORT_ASC
                } else {
                    Textbook.CHAPTER_SORT_DESC
                }
                it.setFlag(orderFlag, Textbook.CHAPTER_SORT_DIR_MASK)
            } else {
                // Set new flag with ascending order
                it
                    .setFlag(flag, Textbook.CHAPTER_SORTING_MASK)
                    .setFlag(Textbook.CHAPTER_SORT_ASC, Textbook.CHAPTER_SORT_DIR_MASK)
            }
        }
        return mangaRepository.update(
            TextbookUpdate(
                id = manga.id,
                chapterFlags = newFlags,
            ),
        )
    }

    suspend fun awaitSetAllFlags(
        textbookId: Long,
        unreadFilter: Long,
        downloadedFilter: Long,
        bookmarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayMode: Long,
    ): Boolean {
        return mangaRepository.update(
            TextbookUpdate(
                id = textbookId,
                chapterFlags = 0L.setFlag(unreadFilter, Textbook.CHAPTER_UNREAD_MASK)
                    .setFlag(downloadedFilter, Textbook.CHAPTER_DOWNLOADED_MASK)
                    .setFlag(bookmarkedFilter, Textbook.CHAPTER_BOOKMARKED_MASK)
                    .setFlag(sortingMode, Textbook.CHAPTER_SORTING_MASK)
                    .setFlag(sortingDirection, Textbook.CHAPTER_SORT_DIR_MASK)
                    .setFlag(displayMode, Textbook.CHAPTER_DISPLAY_MASK),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
