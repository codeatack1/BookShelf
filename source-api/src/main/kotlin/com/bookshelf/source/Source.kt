package com.bookshelf.source

import com.bookshelf.source.model.FilterList
import com.bookshelf.source.model.TextbooksPage
import com.bookshelf.source.model.Page
import com.bookshelf.source.model.SChapter
import com.bookshelf.source.model.STextbook
import com.bookshelf.source.model.STextbookUpdate
import rx.Observable

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface Source {

    /**
     * ID for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    val lang: String
        get() = ""

    /**
     * Whether the source has support for latest updates.
     */
    val supportsLatest: Boolean

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList = FilterList()

    /**
     * Get a page with a list of manga.
     *
     * @since tachiyomix 1.6
     * @param page the page number to retrieve.
     */
    suspend fun getPopularTextbooks(page: Int): TextbooksPage

    /**
     * Get a page with a list of latest manga updates.
     *
     * @since tachiyomix 1.6
     * @param page the page number to retrieve.
     */
    suspend fun getLatestTextbooks(page: Int): TextbooksPage

    /**
     * Get a page with a list of manga.
     *
     * @since tachiyomix 1.6
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    suspend fun getSearchTextbooks(page: Int, query: String, filters: FilterList): TextbooksPage

    /**
     * Fetches updated information for a manga.
     *
     * Depending on the provided flags or source availability, this may include
     * updated manga metadata, available chapters, or both.
     *
     * If a value is not requested, the existing provided value can be returned as-is.
     * The host app may apply any returned updates regardless of the flags,
     * so care should be taken to only return accurate and intentional changes.
     *
     * @since tachiyomix 1.6
     * @param manga The manga to fetch updates for.
     * @param chapters Existing chapters of the manga
     * @param fetchDetails Whether to fetch updated manga details.
     * @param fetchChapters Whether to fetch available chapters.
     */
    suspend fun getTextbookUpdate(
        manga: STextbook,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): STextbookUpdate

    /**
     * Get the list of pages a chapter has. Pages should be returned
     * in the expected order; the index is ignored.
     *
     * @since tachiyomix 1.6
     * @param chapter the chapter.
     * @return the pages for the chapter.
     */
    suspend fun getPageList(chapter: SChapter): List<Page>

    @Deprecated("Use the combined suspend API instead", ReplaceWith("getTextbookUpdate"))
    fun fetchTextbookDetails(manga: STextbook): Observable<STextbook> = throw UnsupportedOperationException()

    @Deprecated("Use the combined suspend API instead", ReplaceWith("getTextbookUpdate"))
    fun fetchChapterList(manga: STextbook): Observable<List<SChapter>> = throw UnsupportedOperationException()

    @Deprecated("Use the suspend API instead", ReplaceWith("getPageList"))
    fun fetchPageList(chapter: SChapter): Observable<List<Page>> = throw UnsupportedOperationException()
}
