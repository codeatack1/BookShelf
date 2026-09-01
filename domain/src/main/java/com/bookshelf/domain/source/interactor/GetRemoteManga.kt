package com.bookshelf.domain.source.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.source.model.FilterList
import com.bookshelf.domain.source.repository.SourcePagingSource
import com.bookshelf.domain.source.repository.SourceRepository

@Inject
class GetRemoteManga(
    private val repository: SourceRepository,
) {

    operator fun invoke(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource {
        return when (query) {
            QUERY_POPULAR -> repository.getPopular(sourceId)
            QUERY_LATEST -> repository.getLatest(sourceId)
            else -> repository.search(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "com.bookshelf.domain.source.interactor.POPULAR"
        const val QUERY_LATEST = "com.bookshelf.domain.source.interactor.LATEST"
    }
}
