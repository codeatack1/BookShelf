package com.bookshelf.ui.browse.migration.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.bookshelf.feature.migration.dialog.MigrateTextbookDialog
import com.bookshelf.feature.migration.list.MigrationListScreen
import com.bookshelf.presentation.browse.MigrateSearchScreen
import com.bookshelf.presentation.util.Screen
import com.bookshelf.ui.browse.source.globalsearch.SearchViewModel
import com.bookshelf.ui.textbook.TextbookScreen
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

class MigrateSearchScreen(private val textbookId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel =
            assistedMetroViewModel<MigrateSearchViewModel, MigrateSearchViewModel.Factory> { create(textbookId = textbookId) }
        val state by viewModel.state.collectAsState()

        MigrateSearchScreen(
            state = state,
            fromSourceId = state.from?.source,
            navigateUp = navigator::pop,
            onChangeSearchQuery = viewModel::updateSearchQuery,
            onSearch = { viewModel.search() },
            getManga = { viewModel.getManga(it) },
            onChangeSearchFilter = viewModel::setSourceFilter,
            onToggleResults = viewModel::toggleFilterResults,
            onClickSource = { navigator.push(MigrateSourceSearchScreen(state.from!!, it.id, state.searchQuery)) },
            onClickItem = {
                val migrateListScreen = navigator.items
                    .filterIsInstance<MigrationListScreen>()
                    .lastOrNull()

                if (migrateListScreen == null) {
                    viewModel.setMigrateDialog(textbookId, it)
                } else {
                    migrateListScreen.addMatchOverride(current = textbookId, target = it.id)
                    navigator.popUntil { screen -> screen is MigrationListScreen }
                }
            },
            onLongClickItem = { navigator.push(TextbookScreen(it.id, true)) },
        )

        when (val dialog = state.dialog) {
            is SearchViewModel.Dialog.Migrate -> {
                MigrateTextbookDialog(
                    current = dialog.current,
                    target = dialog.target,
                    // Initiated from the context of [dialog.current] so we show [dialog.target].
                    onClickTitle = { navigator.push(TextbookScreen(dialog.target.id, true)) },
                    onDismissRequest = { viewModel.clearDialog() },
                    onComplete = {
                        if (navigator.lastItem is TextbookScreen) {
                            val lastItem = navigator.lastItem
                            navigator.popUntil { navigator.items.contains(lastItem) }
                            navigator.push(TextbookScreen(dialog.target.id))
                        } else {
                            navigator.replace(TextbookScreen(dialog.target.id))
                        }
                    },
                )
            }
            else -> {}
        }
    }
}
