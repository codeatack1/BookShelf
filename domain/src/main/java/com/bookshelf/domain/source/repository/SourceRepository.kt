package com.bookshelf.domain.source.repository

import androidx.paging.PagingSource
import com.bookshelf.source.model.FilterList
import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.domain.source.model.Source
import com.bookshelf.domain.source.model.SourceWithCount

typealias SourcePagingSource = PagingSource<Long, Manga>

interface SourceRepository {

    fun getSources(): Flow<List<Source>>

    fun getOnlineSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>>

    fun search(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource

    fun getPopular(sourceId: Long): SourcePagingSource

    fun getLatest(sourceId: Long): SourcePagingSource
}
