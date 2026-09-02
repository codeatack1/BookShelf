package com.bookshelf.data.source

import com.bookshelf.data.Database
import com.bookshelf.data.subscribeToList
import com.bookshelf.domain.source.model.Source as DomainSource
import com.bookshelf.domain.source.model.SourceWithCount
import com.bookshelf.domain.source.model.StubSource
import com.bookshelf.domain.source.repository.SourcePagingSource
import com.bookshelf.domain.source.repository.SourceRepository
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.domain.textbook.interactor.NetworkToLocalTextbook
import com.bookshelf.source.Source
import com.bookshelf.source.model.FilterList
import com.bookshelf.source.online.HttpSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SourceRepositoryImpl(
    private val sourceManager: SourceManager,
    private val database: Database,
    private val networkToLocalManga: NetworkToLocalTextbook,
) : SourceRepository {

    override fun getSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources.map {
                mapSourceToDomainSource(it).copy(
                    supportsLatest = it.supportsLatest,
                )
            }
        }
    }

    override fun getOnlineSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources
                .filterIsInstance<HttpSource>()
                .map(::mapSourceToDomainSource)
        }
    }

    override fun getSourcesWithFavoriteCount(): Flow<List<Pair<DomainSource, Long>>> {
        val sourceIdWithFavoriteCountFlow = database.textbooksQueries
            .getSourceIdWithFavoriteCount()
            .subscribeToList()
        return combine(sourceIdWithFavoriteCountFlow, sourceManager.sources) { sourceIdWithFavoriteCount, _ ->
            sourceIdWithFavoriteCount
        }
            .map {
                it.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mapSourceToDomainSource(source).copy(
                        isStub = source is StubSource,
                    )
                    domainSource to count
                }
            }
    }

    override fun getSourcesWithNonLibraryTextbook(): Flow<List<SourceWithCount>> {
        return database.textbooksQueries
            .getSourceIdsWithNonLibraryTextbook()
            .subscribeToList()
            .map { sourceId ->
                sourceId.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mapSourceToDomainSource(source).copy(
                        isStub = source is StubSource,
                    )
                    SourceWithCount(domainSource, count)
                }
            }
    }

    override fun search(
        sourceId: Long,
        query: String,
        filterList: FilterList,
    ): SourcePagingSource {
        return SourceSearchPagingSource(
            { sourceManager.getOrStub(sourceId) },
            query,
            filterList,
            networkToLocalManga,
        )
    }

    override fun getPopular(sourceId: Long): SourcePagingSource {
        return SourcePopularPagingSource({ sourceManager.getOrStub(sourceId) }, networkToLocalManga)
    }

    override fun getLatest(sourceId: Long): SourcePagingSource {
        return SourceLatestPagingSource({ sourceManager.getOrStub(sourceId) }, networkToLocalManga)
    }

    private fun mapSourceToDomainSource(source: Source): DomainSource = DomainSource(
        id = source.id,
        lang = source.lang,
        name = source.name,
        supportsLatest = false,
        isStub = false,
    )
}
