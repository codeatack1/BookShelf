package com.bookshelf.ui.browse.migration.manga

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.manga.components.BaseMangaListItem
import com.bookshelf.presentation.util.Screen
import com.bookshelf.ui.manga.MangaScreen
import com.bookshelf.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import com.bookshelf.feature.migration.config.MigrationConfigScreen
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.automirroredrounded.ArrowForward
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.components.FastScrollLazyColumn
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.core.screens.EmptyScreen
import com.bookshelf.presentation.core.screens.LoadingScreen
import com.bookshelf.presentation.core.util.selectedBackground
import com.bookshelf.presentation.core.util.shouldExpandFAB

data class MigrateMangaScreen(
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel =
            assistedMetroViewModel<MigrateMangaViewModel, MigrateMangaViewModel.Factory> { create(sourceId = sourceId) }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        BackHandler(enabled = state.selectionMode) {
            viewModel.clearSelection()
        }

        val lazyListState = rememberLazyListState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = state.source!!.name,
                    navigateUp = {
                        if (state.selectionMode) {
                            viewModel.clearSelection()
                        } else {
                            navigator.pop()
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                    icon = {
                        Icon(imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward, contentDescription = null)
                    },
                    onClick = {
                        val selection = state.selection
                        viewModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    },
                    expanded = lazyListState.shouldExpandFAB(),
                    modifier = Modifier.animateFloatingActionButton(
                        visible = state.selectionMode,
                        alignment = Alignment.BottomEnd,
                    ),
                )
            },
        ) { contentPadding ->
            if (state.isEmpty) {
                EmptyScreen(
                    stringRes = MR.strings.empty_screen,
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            MigrateMangaContent(
                lazyListState = lazyListState,
                contentPadding = contentPadding,
                state = state,
                onClickItem = viewModel::toggleSelection,
                onClickCover = { navigator.push(MangaScreen(it.id)) },
            )
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    MigrationMangaEvent.FailedFetchingFavorites -> {
                        context.toast(MR.strings.internal_error)
                    }
                }
            }
        }
    }

    @Composable
    private fun MigrateMangaContent(
        lazyListState: LazyListState,
        contentPadding: PaddingValues,
        state: MigrateMangaViewModel.State,
        onClickItem: (Manga) -> Unit,
        onClickCover: (Manga) -> Unit,
    ) {
        FastScrollLazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            items(state.titles) { manga ->
                MigrateMangaItem(
                    manga = manga,
                    isSelected = manga.id in state.selection,
                    onClickItem = onClickItem,
                    onClickCover = onClickCover,
                )
            }
        }
    }

    @Composable
    private fun MigrateMangaItem(
        manga: Manga,
        isSelected: Boolean,
        onClickItem: (Manga) -> Unit,
        onClickCover: (Manga) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        BaseMangaListItem(
            modifier = modifier.selectedBackground(isSelected),
            manga = manga,
            onClickItem = { onClickItem(manga) },
            onClickCover = { onClickCover(manga) },
        )
    }
}
