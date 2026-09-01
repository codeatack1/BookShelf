package com.bookshelf.feature.migration.list.search

import com.bookshelf.source.Source
import com.bookshelf.source.model.SManga
import com.bookshelf.domain.manga.model.toDomainManga
import com.bookshelf.domain.manga.model.Manga

class SmartSourceSearchEngine(extraSearchParams: String?) : BaseSmartSearchEngine<SManga>(extraSearchParams) {

    override fun getTitle(result: SManga) = result.title

    suspend fun regularSearch(source: Source, title: String): Manga? {
        return regularSearch(makeSearchAction(source), title).let {
            it?.toDomainManga(source.id)
        }
    }

    suspend fun deepSearch(source: Source, title: String): Manga? {
        return deepSearch(makeSearchAction(source), title).let {
            it?.toDomainManga(source.id)
        }
    }

    private fun makeSearchAction(source: Source): SearchAction<SManga> = { query ->
        source.getSearchManga(1, query, source.getFilterList()).mangas
    }
}
