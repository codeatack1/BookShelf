package com.bookshelf.data.source

import androidx.paging.PagingState
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.domain.source.repository.SourcePagingSource
import com.bookshelf.domain.textbook.interactor.NetworkToLocalTextbook
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.toDomainTextbook
import com.bookshelf.source.Source
import com.bookshelf.source.model.FilterList
import com.bookshelf.source.model.TextbooksPage
import kotlinx.coroutines.CancellationException

class SourceSearchPagingSource(
    source: suspend () -> Source,
    private val query: String,
    private val filters: FilterList,
    networkToLocalManga: NetworkToLocalTextbook,
) : BaseSourcePagingSource(source, networkToLocalManga) {
    override suspend fun requestNextPage(source: Source, currentPage: Int): TextbooksPage {
        return source.getSearchTextbooks(currentPage, query, filters)
    }
}

class SourcePopularPagingSource(
    source: suspend () -> Source,
    networkToLocalManga: NetworkToLocalTextbook,
) : BaseSourcePagingSource(source, networkToLocalManga) {
    override suspend fun requestNextPage(source: Source, currentPage: Int): TextbooksPage {
        return source.getPopularTextbooks(currentPage)
    }
}

class SourceLatestPagingSource(
    source: suspend () -> Source,
    networkToLocalManga: NetworkToLocalTextbook,
) : BaseSourcePagingSource(source, networkToLocalManga) {
    override suspend fun requestNextPage(source: Source, currentPage: Int): TextbooksPage {
        return source.getLatestTextbooks(currentPage)
    }
}

abstract class BaseSourcePagingSource(
    private val source: suspend () -> Source,
    private val networkToLocalManga: NetworkToLocalTextbook,
) : SourcePagingSource() {

    private val seenManga = hashSetOf<String>()

    abstract suspend fun requestNextPage(source: Source, currentPage: Int): TextbooksPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Textbook> {
        val page = params.key ?: 1

        return try {
            val source = source()
            val mangasPage = withIOContext {
                requestNextPage(source, page.toInt())
                    .takeIf { it.textbooks.isNotEmpty() }
                    ?: throw NoResultsException()
            }

            val manga = mangasPage.textbooks
                .map { it.toDomainTextbook(source.id) }
                .filter { seenManga.add(it.url) }
                .let { networkToLocalManga(it) }

            LoadResult.Page(
                data = manga,
                prevKey = null,
                nextKey = if (mangasPage.hasNextPage) page + 1 else null,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Textbook>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}

class NoResultsException : Exception()
