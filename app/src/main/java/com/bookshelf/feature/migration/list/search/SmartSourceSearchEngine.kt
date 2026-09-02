package com.bookshelf.feature.migration.list.search

import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.toDomainTextbook
import com.bookshelf.source.Source
import com.bookshelf.source.model.STextbook

class SmartSourceSearchEngine(extraSearchParams: String?) : BaseSmartSearchEngine<STextbook>(extraSearchParams) {

    override fun getTitle(result: STextbook) = result.title

    suspend fun regularSearch(source: Source, title: String): Textbook? {
        return regularSearch(makeSearchAction(source), title).let {
            it?.toDomainTextbook(source.id)
        }
    }

    suspend fun deepSearch(source: Source, title: String): Textbook? {
        return deepSearch(makeSearchAction(source), title).let {
            it?.toDomainTextbook(source.id)
        }
    }

    private fun makeSearchAction(source: Source): SearchAction<STextbook> = { query ->
        source.getSearchTextbooks(1, query, source.getFilterList()).textbooks
    }
}
