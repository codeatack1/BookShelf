package com.bookshelf.domain.chapter.interactor

import com.bookshelf.core.common.util.lang.withNonCancellableContext
import com.bookshelf.domain.library.service.LibraryPreferences
import com.bookshelf.domain.textbook.interactor.GetFavoriteTextbooks
import com.bookshelf.domain.textbook.interactor.SetTextbookChapterFlags
import com.bookshelf.domain.textbook.model.Textbook
import dev.zacsweers.metro.Inject

@Inject
class SetTextbookDefaultChapterFlags(
    private val libraryPreferences: LibraryPreferences,
    private val setMangaChapterFlags: SetTextbookChapterFlags,
    private val getFavorites: GetFavoriteTextbooks,
) {

    suspend fun await(manga: Textbook) {
        withNonCancellableContext {
            with(libraryPreferences) {
                setMangaChapterFlags.awaitSetAllFlags(
                    textbookId = manga.id,
                    unreadFilter = filterChapterByRead.get(),
                    downloadedFilter = filterChapterByDownloaded.get(),
                    bookmarkedFilter = filterChapterByBookmarked.get(),
                    sortingMode = sortChapterBySourceOrNumber.get(),
                    sortingDirection = sortChapterByAscendingOrDescending.get(),
                    displayMode = displayChapterByNameOrNumber.get(),
                )
            }
        }
    }

    suspend fun awaitAll() {
        withNonCancellableContext {
            getFavorites.await().forEach { await(it) }
        }
    }
}
