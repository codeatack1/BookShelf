package com.bookshelf.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.bookshelf.presentation.browse.components.GlobalSearchCardRow
import com.bookshelf.presentation.browse.components.GlobalSearchErrorResultItem
import com.bookshelf.presentation.browse.components.GlobalSearchLoadingResultItem
import com.bookshelf.presentation.browse.components.GlobalSearchResultItem
import com.bookshelf.presentation.browse.components.GlobalSearchToolbar
import com.bookshelf.com.bookshelf.source.Source
import com.bookshelf.ui.browse.source.globalsearch.SearchItemResult
import com.bookshelf.ui.browse.source.globalsearch.SearchViewModel
import com.bookshelf.ui.browse.source.globalsearch.SourceFilter
import com.bookshelf.util.system.LocaleHelper
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.presentation.core.components.material.Scaffold

@Composable
fun GlobalSearchScreen(
    state: SearchViewModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                hideSourceFilter = false,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<Source, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = fromSourceId?.let {
                        "▶ ${source.name}".takeIf { source.id == fromSourceId }
                    } ?: source.name,
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                    modifier = Modifier.animateItem(),
                ) {
                    when (result) {
                        SearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is SearchItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = result.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is SearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}
