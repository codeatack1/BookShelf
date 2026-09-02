package com.bookshelf.domain.source.model

import com.bookshelf.source.Source
import com.bookshelf.source.model.FilterList
import com.bookshelf.source.model.TextbooksPage
import com.bookshelf.source.model.Page
import com.bookshelf.source.model.SChapter
import com.bookshelf.source.model.STextbook
import com.bookshelf.source.model.STextbookUpdate

class StubSource(
    override val id: Long,
    override val lang: String,
    override val name: String,
) : Source {

    private val isInvalid: Boolean = name.isBlank() || lang.isBlank()

    override val supportsLatest: Boolean = false

    override suspend fun getPopularTextbooks(page: Int): TextbooksPage = throw SourceNotInstalledException()

    override suspend fun getLatestTextbooks(page: Int): TextbooksPage = throw SourceNotInstalledException()

    override suspend fun getSearchTextbooks(page: Int, query: String, filters: FilterList): TextbooksPage =
        throw SourceNotInstalledException()

    override suspend fun getTextbookUpdate(
        manga: STextbook,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): STextbookUpdate = throw SourceNotInstalledException()

    override suspend fun getPageList(chapter: SChapter): List<Page> =
        throw SourceNotInstalledException()

    override fun toString(): String =
        if (!isInvalid) "$name (${lang.uppercase()})" else id.toString()

    companion object {
        fun from(source: Source): StubSource {
            return StubSource(id = source.id, lang = source.lang, name = source.name)
        }
    }
}

class SourceNotInstalledException : Exception()
