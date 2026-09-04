package com.bookshelf.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.util.fastAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.bookshelf.R
import com.bookshelf.core.common.i18n.stringResource
import com.bookshelf.core.common.util.lang.launchIO
import com.bookshelf.data.library.LibraryUpdateJob
import com.bookshelf.domain.category.model.Category
import com.bookshelf.domain.library.model.LibraryTextbook
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.feature.migration.config.MigrationConfigScreen
import com.bookshelf.i18n.MR
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.automirroredrounded.Help
import com.bookshelf.presentation.category.components.ChangeCategoryDialog
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.core.screens.EmptyScreen
import com.bookshelf.presentation.core.screens.EmptyScreenAction
import com.bookshelf.presentation.core.screens.LoadingScreen
import com.bookshelf.presentation.library.DeleteLibraryTextbookDialog
import com.bookshelf.presentation.library.LibrarySettingsDialog
import com.bookshelf.presentation.library.components.LibraryContent
import com.bookshelf.presentation.library.components.LibraryToolbar
import com.bookshelf.presentation.more.onboarding.GETTING_STARTED_URL
import com.bookshelf.presentation.textbook.components.LibraryBottomActionMenu
import com.bookshelf.presentation.util.Tab
import com.bookshelf.source.local.isLocal
import com.bookshelf.ui.browse.source.globalsearch.GlobalSearchScreen
import com.bookshelf.ui.category.CategoryScreen
import com.bookshelf.ui.home.HomeScreen
import com.bookshelf.ui.main.MainActivity
import com.bookshelf.ui.reader.ReaderActivity
import com.bookshelf.ui.textbook.TextbookScreen
import com.bookshelf.util.system.workManager
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        requestOpenSettingsSheet()
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val viewModel = metroViewModel<LibraryViewModel>()
        val settingsViewModel = metroViewModel<LibrarySettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }

        val onClickRefresh: (Category?) -> Boolean = { category ->
            val started = LibraryUpdateJob.startNow(context.workManager, category)
            scope.launch {
                val msgRes = when {
                    !started -> MR.strings.update_already_running
                    category != null -> MR.strings.updating_category
                    else -> MR.strings.updating_library
                }
                snackbarHostState.showSnackbar(context.stringResource(msgRes))
            }
            started
        }

        Scaffold(
            topBar = { scrollBehavior ->
                val title = state.getToolbarTitle(
                    defaultTitle = stringResource(MR.strings.label_library),
                    defaultCategoryTitle = stringResource(MR.strings.label_default),
                    page = state.coercedActiveCategoryIndex,
                )
                LibraryToolbar(
                    hasActiveFilters = state.hasActiveFilters,
                    selectedCount = state.selection.size,
                    title = title,
                    onClickUnselectAll = viewModel::clearSelection,
                    onClickSelectAll = viewModel::selectAll,
                    onClickInvertSelection = viewModel::invertSelection,
                    onClickFilter = viewModel::showSettingsDialog,
                    onClickRefresh = { onClickRefresh(state.activeCategory) },
                    onClickGlobalUpdate = { onClickRefresh(null) },
                    onClickOpenRandomManga = {
                        scope.launch {
                            val randomItem = viewModel.getRandomLibraryItemForCurrentCategory()
                            if (randomItem != null) {
                                navigator.push(TextbookScreen(randomItem.libraryManga.textbook.id))
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.stringResource(MR.strings.information_no_entries_found),
                                )
                            }
                        }
                    },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::search,
                    // For scroll overlay when no tab
                    scrollBehavior = scrollBehavior.takeIf { !state.showCategoryTabs },
                )
            },
            bottomBar = {
                LibraryBottomActionMenu(
                    visible = state.selectionMode,
                    onChangeCategoryClicked = viewModel::openChangeCategoryDialog,
                    onMarkAsReadClicked = { viewModel.markReadSelection(true) },
                    onMarkAsUnreadClicked = { viewModel.markReadSelection(false) },
                    onDownloadClicked = viewModel::performDownloadAction
                        .takeIf { state.selectedManga.fastAll { !it.isLocal() } },
                    onDeleteClicked = viewModel::openDeleteTextbookDialog,
                    onMigrateClicked = {
                        val selection = state.selection
                        viewModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    },
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            when {
                state.isLoading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
                    val handler = LocalUriHandler.current
                    EmptyScreen(
                        stringRes = MR.strings.information_empty_library,
                        modifier = Modifier.padding(contentPadding),
                        actions = listOf(
                            EmptyScreenAction(
                                stringRes = MR.strings.getting_started_guide,
                                icon = MaterialSymbols.AutoMirroredRounded.Help,
                                onClick = { handler.openUri(GETTING_STARTED_URL) },
                            ),
                        ),
                    )
                }
                else -> {
                    LibraryContent(
                        categories = state.displayedCategories,
                        searchQuery = state.searchQuery,
                        selection = state.selection,
                        contentPadding = contentPadding,
                        currentPage = state.coercedActiveCategoryIndex,
                        hasActiveFilters = state.hasActiveFilters,
                        showPageTabs = state.showCategoryTabs || !state.searchQuery.isNullOrEmpty(),
                        onChangeCurrentPage = viewModel::updateActiveCategoryIndex,
                        onClickManga = { navigator.push(TextbookScreen(it)) },
                        onContinueReadingClicked = { it: LibraryTextbook ->
                            scope.launchIO {
                                val chapter = viewModel.getNextUnreadChapter(it.textbook)
                                if (chapter != null) {
                                    context.startActivity(
                                        ReaderActivity.newIntent(context, chapter.textbookId, chapter.id),
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
                                }
                            }
                            Unit
                        }.takeIf { state.showMangaContinueButton },
                        onToggleSelection = viewModel::toggleSelection,
                        onToggleRangeSelection = { category, manga ->
                            viewModel.toggleRangeSelection(category, manga)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = { onClickRefresh(state.activeCategory) },
                        onGlobalSearchClicked = {
                            navigator.push(GlobalSearchScreen(viewModel.state.value.searchQuery ?: ""))
                        },
                        getItemCountForCategory = { state.getItemCountForCategory(it) },
                        getDisplayMode = { viewModel.getDisplayMode() },
                        getColumnsForOrientation = { viewModel.getColumnsForOrientation(it) },
                        getItemsForCategory = { state.getItemsForCategory(it) },
                    )
                }
            }
        }

        val onDismissRequest = viewModel::closeDialog
        when (val dialog = state.dialog) {
            is LibraryViewModel.Dialog.SettingsSheet -> run {
                LibrarySettingsDialog(
                    onDismissRequest = onDismissRequest,
                    viewModel = settingsViewModel,
                    category = state.activeCategory,
                )
            }
            is LibraryViewModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        viewModel.clearSelection()
                        navigator.push(CategoryScreen())
                    },
                    onConfirm = { include, exclude ->
                        viewModel.clearSelection()
                        viewModel.setTextbookCategories(dialog.manga, include, exclude)
                    },
                )
            }
            is LibraryViewModel.Dialog.DeleteTextbook -> {
                DeleteLibraryTextbookDialog(
                    containsLocalManga = dialog.manga.any(Textbook::isLocal),
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteManga, deleteChapter ->
                        viewModel.removeMangas(dialog.manga, deleteManga, deleteChapter)
                        viewModel.clearSelection()
                    },
                )
            }
            null -> {}
        }

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> viewModel.clearSelection()
                state.searchQuery != null -> viewModel.search(null)
            }
        }

        LaunchedEffect(state.selectionMode, state.dialog) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }

        LaunchedEffect(Unit) {
            launch { queryEvent.receiveAsFlow().collect(viewModel::search) }
            launch { requestSettingsSheetEvent.receiveAsFlow().collectLatest { viewModel.showSettingsDialog() } }
        }
    }

    // For invoking search from other screen
    private val queryEvent = Channel<String>()
    suspend fun search(query: String) = queryEvent.send(query)

    // For opening settings sheet in LibraryController
    private val requestSettingsSheetEvent = Channel<Unit>()
    private suspend fun requestOpenSettingsSheet() = requestSettingsSheetEvent.send(Unit)
}
