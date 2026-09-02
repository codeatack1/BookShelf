package com.bookshelf.source

import com.bookshelf.core.common.util.lang.awaitSingle
import com.bookshelf.source.model.FilterList
import com.bookshelf.source.model.Page
import com.bookshelf.source.model.SChapter
import com.bookshelf.source.model.STextbook
import com.bookshelf.source.model.STextbookUpdate
import com.bookshelf.source.model.TextbooksPage
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import rx.Observable

interface CatalogueSource : Source {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang: String

    @Suppress("DEPRECATION")
    override suspend fun getPopularTextbooks(page: Int): TextbooksPage = fetchPopularTextbooks(page).awaitSingle()

    @Suppress("DEPRECATION")
    override suspend fun getLatestTextbooks(page: Int): TextbooksPage = fetchLatestTextbooks(page).awaitSingle()

    @Suppress("DEPRECATION")
    override suspend fun getSearchTextbooks(
        page: Int,
        query: String,
        filters: FilterList,
    ): TextbooksPage = fetchSearchTextbooks(page, query, filters).awaitSingle()

    @Suppress("DEPRECATION")
    override suspend fun getTextbookUpdate(
        manga: STextbook,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): STextbookUpdate = supervisorScope {
        val asyncManga = if (fetchDetails) async { fetchTextbookDetails(manga).awaitSingle() } else null
        val asyncChapters = if (fetchChapters) async { fetchChapterList(manga).awaitSingle() } else null
        STextbookUpdate(asyncManga?.await() ?: manga, asyncChapters?.await() ?: chapters)
    }

    @Suppress("DEPRECATION")
    override suspend fun getPageList(chapter: SChapter): List<Page> = fetchPageList(chapter).awaitSingle()

    /**
     * Returns an observable containing a page with a list of manga.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the suspend API instead", ReplaceWith("getPopularTextbooks"))
    fun fetchPopularTextbooks(page: Int): Observable<TextbooksPage> = throw UnsupportedOperationException()

    /**
     * Returns an observable containing a page with a list of manga.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Deprecated("Use the suspend API instead", ReplaceWith("getSearchTextbooks"))
    fun fetchSearchTextbooks(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<TextbooksPage> = throw UnsupportedOperationException()

    /**
     * Returns an observable containing a page with a list of latest manga updates.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the suspend API instead", ReplaceWith("getLatestTextbooks"))
    fun fetchLatestTextbooks(page: Int): Observable<TextbooksPage> = throw UnsupportedOperationException()
}
