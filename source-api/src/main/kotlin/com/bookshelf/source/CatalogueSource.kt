package com.bookshelf.source

import com.bookshelf.source.model.FilterList
import com.bookshelf.source.model.MangasPage
import com.bookshelf.source.model.Page
import com.bookshelf.source.model.SChapter
import com.bookshelf.source.model.SManga
import com.bookshelf.source.model.SMangaUpdate
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import rx.Observable
import com.bookshelf.core.common.util.lang.awaitSingle

interface CatalogueSource : Source {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang: String

    @Suppress("DEPRECATION")
    override suspend fun getPopularManga(page: Int): MangasPage = fetchPopularManga(page).awaitSingle()

    @Suppress("DEPRECATION")
    override suspend fun getLatestUpdates(page: Int): MangasPage = fetchLatestUpdates(page).awaitSingle()

    @Suppress("DEPRECATION")
    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage = fetchSearchManga(page, query, filters).awaitSingle()

    @Suppress("DEPRECATION")
    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = supervisorScope {
        val asyncManga = if (fetchDetails) async { fetchMangaDetails(manga).awaitSingle() } else null
        val asyncChapters = if (fetchChapters) async { fetchChapterList(manga).awaitSingle() } else null
        SMangaUpdate(asyncManga?.await() ?: manga, asyncChapters?.await() ?: chapters)
    }

    @Suppress("DEPRECATION")
    override suspend fun getPageList(chapter: SChapter): List<Page> = fetchPageList(chapter).awaitSingle()

    /**
     * Returns an observable containing a page with a list of manga.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the suspend API instead", ReplaceWith("getPopularManga"))
    fun fetchPopularManga(page: Int): Observable<MangasPage> = throw UnsupportedOperationException()

    /**
     * Returns an observable containing a page with a list of manga.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Deprecated("Use the suspend API instead", ReplaceWith("getSearchManga"))
    fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangasPage> = throw UnsupportedOperationException()

    /**
     * Returns an observable containing a page with a list of latest manga updates.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the suspend API instead", ReplaceWith("getLatestUpdates"))
    fun fetchLatestUpdates(page: Int): Observable<MangasPage> = throw UnsupportedOperationException()
}
