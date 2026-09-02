package com.bookshelf.source.online

import com.bookshelf.source.model.TextbooksPage
import com.bookshelf.source.model.Page
import com.bookshelf.source.model.SChapter
import com.bookshelf.source.model.STextbook
import com.bookshelf.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A simple implementation for sources from a website using Jsoup, an HTML parser.
 */
@Deprecated(
    message = "In most cases sources only require a subset of the methods from this class. " +
        "Source developers should make their own implementation according to their needs.",
)
abstract class ParsedHttpSource : HttpSource() {

    /**
     * Parses the response from the site and returns a [TextbooksPage] object.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        "The helper functions are inherently limiting and hides the underlying implementation. Source developers should make their own implementation according to their needs.",
    )
    override fun popularTextbooksParse(response: Response): TextbooksPage {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }

        val hasNextPage = popularMangaNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return TextbooksPage(mangas, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    protected abstract fun popularMangaSelector(): String

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [popularMangaSelector].
     */
    protected abstract fun popularMangaFromElement(element: Element): STextbook

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun popularMangaNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [TextbooksPage] object.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        "The helper functions are inherently limiting and hides the underlying implementation. Source developers should make their own implementation according to their needs.",
    )
    override fun searchTextbooksParse(response: Response): TextbooksPage {
        val document = response.asJsoup()

        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }

        val hasNextPage = searchMangaNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return TextbooksPage(mangas, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    protected abstract fun searchMangaSelector(): String

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchMangaSelector].
     */
    protected abstract fun searchMangaFromElement(element: Element): STextbook

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun searchMangaNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [TextbooksPage] object.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        "The helper functions are inherently limiting and hides the underlying implementation. Source developers should make their own implementation according to their needs.",
    )
    override fun latestUpdatesParse(response: Response): TextbooksPage {
        val document = response.asJsoup()

        val mangas = document.select(latestUpdatesSelector()).map { element ->
            latestUpdatesFromElement(element)
        }

        val hasNextPage = latestUpdatesNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return TextbooksPage(mangas, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    protected abstract fun latestUpdatesSelector(): String

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [latestUpdatesSelector].
     */
    protected abstract fun latestUpdatesFromElement(element: Element): STextbook

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun latestUpdatesNextPageSelector(): String?

    /**
     * Parses the response from the site and returns the details of a manga.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        "The helper functions are inherently limiting and hides the underlying implementation. Source developers should make their own implementation according to their needs.",
    )
    override fun textbookDetailsParse(response: Response): STextbook {
        return textbookDetailsParse(response.asJsoup())
    }

    /**
     * Returns the details of the manga from the given [document].
     *
     * @param document the parsed document.
     */
    protected abstract fun textbookDetailsParse(document: Document): STextbook

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        "The helper functions are inherently limiting and hides the underlying implementation. Source developers should make their own implementation according to their needs.",
    )
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each chapter.
     */
    protected abstract fun chapterListSelector(): String

    /**
     * Returns a chapter from the given element.
     *
     * @param element an element obtained from [chapterListSelector].
     */
    protected abstract fun chapterFromElement(element: Element): SChapter

    /**
     * Parses the response from the site and returns the page list.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        "The helper functions are inherently limiting and hides the underlying implementation. Source developers should make their own implementation according to their needs.",
    )
    override fun pageListParse(response: Response): List<Page> {
        return pageListParse(response.asJsoup())
    }

    /**
     * Returns a page list from the given document.
     *
     * @param document the parsed document.
     */
    protected abstract fun pageListParse(document: Document): List<Page>

    /**
     * Parse the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        "The helper functions are inherently limiting and hides the underlying implementation. Source developers should make their own implementation according to their needs.",
    )
    override fun imageUrlParse(response: Response): String {
        return imageUrlParse(response.asJsoup())
    }

    /**
     * Returns the absolute url to the source image from the document.
     *
     * @param document the parsed document.
     */
    protected abstract fun imageUrlParse(document: Document): String
}
