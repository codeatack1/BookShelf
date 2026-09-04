package com.bookshelf.ui.textbook

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.hasCustomCover
import com.bookshelf.domain.textbook.model.toSTextbook
import com.bookshelf.feature.migration.config.MigrationConfigScreen
import com.bookshelf.feature.migration.dialog.MigrateTextbookDialog
import com.bookshelf.presentation.category.components.ChangeCategoryDialog
import com.bookshelf.presentation.components.NavigatorAdaptiveSheet
import com.bookshelf.presentation.core.screens.LoadingScreen
import com.bookshelf.presentation.textbook.ChapterSettingsDialog
import com.bookshelf.presentation.textbook.DuplicateTextbookDialog
import com.bookshelf.presentation.textbook.EditCoverAction
import com.bookshelf.presentation.textbook.TextbookScreen
import com.bookshelf.presentation.textbook.components.DeleteChaptersDialog
import com.bookshelf.presentation.textbook.components.ScanlatorFilterDialog
import com.bookshelf.presentation.textbook.components.SetIntervalDialog
import com.bookshelf.presentation.textbook.components.TextbookCoverDialog
import com.bookshelf.presentation.util.AssistContentScreen
import com.bookshelf.presentation.util.Screen
import com.bookshelf.presentation.util.isTabletUi
import com.bookshelf.source.Source
import com.bookshelf.source.isLocalOrStub
import com.bookshelf.source.online.HttpSource
import com.bookshelf.ui.browse.source.browse.BrowseSourceScreen
import com.bookshelf.ui.browse.source.globalsearch.GlobalSearchScreen
import com.bookshelf.ui.category.CategoryScreen
import com.bookshelf.ui.home.HomeScreen
import com.bookshelf.ui.reader.ReaderActivity
import com.bookshelf.ui.setting.SettingsScreen
import com.bookshelf.ui.textbook.notes.TextbookNotesScreen
import com.bookshelf.ui.textbook.track.TrackInfoDialogHomeScreen
import com.bookshelf.ui.webview.WebViewScreen
import com.bookshelf.util.system.copyToClipboard
import com.bookshelf.util.system.toShareIntent
import com.bookshelf.util.system.toast
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.launch
import logcat.LogPriority

class TextbookScreen(
    private val textbookId: Long,
    val fromSource: Boolean = false,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val viewModel =
            assistedMetroViewModel<TextbookViewModel, TextbookViewModel.Factory> {
                create(textbookId = textbookId, isFromSource = fromSource)
            }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state is TextbookViewModel.State.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as TextbookViewModel.State.Success
        val isHttpSource = remember { successState.source is HttpSource }

        LaunchedEffect(successState.manga, viewModel.source) {
            if (isHttpSource) {
                try {
                    withIOContext {
                        assistUrl = getTextbookUrl(viewModel.manga, viewModel.source)
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to get manga URL" }
                }
            }
        }

        TextbookScreen(
            state = successState,
            snackbarHostState = viewModel.snackbarHostState,
            nextUpdate = successState.manga.expectedNextUpdate,
            isTabletUi = isTabletUi(),
            chapterSwipeStartAction = viewModel.chapterSwipeStartAction,
            chapterSwipeEndAction = viewModel.chapterSwipeEndAction,
            navigateUp = navigator::pop,
            onChapterClicked = { openChapter(context, it) },
            onDownloadChapter = viewModel::runChapterDownloadActions.takeIf { !successState.source.isLocalOrStub() },
            onAddToLibraryClicked = {
                viewModel.toggleFavorite()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onWebViewClicked = {
                openMangaInWebView(
                    navigator,
                    viewModel.manga,
                    viewModel.source,
                )
            }.takeIf { isHttpSource },
            onWebViewLongClicked = {
                copyMangaUrl(
                    context,
                    viewModel.manga,
                    viewModel.source,
                )
            }.takeIf { isHttpSource },
            onTrackingClicked = {
                if (!successState.hasLoggedInTrackers) {
                    navigator.push(SettingsScreen(SettingsScreen.Destination.Tracking))
                } else {
                    viewModel.showTrackDialog()
                }
            },
            onTagSearch = { scope.launch { performGenreSearch(navigator, it, viewModel.source!!) } },
            onFilterButtonClicked = viewModel::showSettingsDialog,
            onRefresh = viewModel::fetchAllFromSource,
            onContinueReading = { continueReading(context, viewModel.getNextUnreadChapter()) },
            onSearch = { query, global -> scope.launch { performSearch(navigator, query, global) } },
            onCoverClicked = viewModel::showCoverDialog,
            onShareClicked = { shareManga(context, viewModel.manga, viewModel.source) }.takeIf { isHttpSource },
            onDownloadActionClicked = viewModel::runDownloadAction.takeIf { !successState.source.isLocalOrStub() },
            onEditCategoryClicked = viewModel::showChangeCategoryDialog.takeIf { successState.manga.favorite },
            onEditFetchIntervalClicked = viewModel::showSetFetchIntervalDialog.takeIf {
                successState.manga.favorite
            },
            onMigrateClicked = {
                navigator.push(MigrationConfigScreen(successState.manga.id))
            }.takeIf { successState.manga.favorite },
            onEditNotesClicked = { navigator.push(TextbookNotesScreen(manga = successState.manga)) },
            onMultiBookmarkClicked = viewModel::bookmarkChapters,
            onMultiMarkAsReadClicked = viewModel::markChaptersRead,
            onMarkPreviousAsReadClicked = viewModel::markPreviousChapterRead,
            onMultiDeleteClicked = viewModel::showDeleteChapterDialog,
            onChapterSwipe = viewModel::chapterSwipe,
            onChapterSelected = viewModel::toggleSelection,
            onAllChapterSelected = viewModel::toggleAllSelection,
            onInvertSelection = viewModel::invertSelection,
        )

        var showScanlatorsDialog by remember { mutableStateOf(false) }

        val onDismissRequest = { viewModel.dismissDialog() }
        when (val dialog = successState.dialog) {
            null -> {}
            is TextbookViewModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.moveMangaToCategoriesAndAddToLibrary(dialog.manga, include)
                    },
                )
            }
            is TextbookViewModel.Dialog.DeleteChapters -> {
                DeleteChaptersDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        viewModel.toggleAllSelection(false)
                        viewModel.deleteChapters(dialog.chapters)
                    },
                )
            }

            is TextbookViewModel.Dialog.DuplicateTextbook -> {
                DuplicateTextbookDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.toggleFavorite(onRemoved = {}, checkDuplicate = false) },
                    onOpenManga = { navigator.push(TextbookScreen(it.id)) },
                    onMigrate = { viewModel.showMigrateDialog(it) },
                )
            }

            is TextbookViewModel.Dialog.Migrate -> {
                MigrateTextbookDialog(
                    current = dialog.current,
                    target = dialog.target,
                    // Initiated from the context of [dialog.target] so we show [dialog.current].
                    onClickTitle = { navigator.push(TextbookScreen(dialog.current.id)) },
                    onDismissRequest = onDismissRequest,
                )
            }
            TextbookViewModel.Dialog.SettingsSheet -> ChapterSettingsDialog(
                onDismissRequest = onDismissRequest,
                manga = successState.manga,
                onDownloadFilterChanged = viewModel::setDownloadedFilter,
                onUnreadFilterChanged = viewModel::setUnreadFilter,
                onBookmarkedFilterChanged = viewModel::setBookmarkedFilter,
                onSortModeChanged = viewModel::setSorting,
                onDisplayModeChanged = viewModel::setDisplayMode,
                onSetAsDefault = viewModel::setCurrentSettingsAsDefault,
                onResetToDefault = viewModel::resetToDefaultSettings,
                scanlatorFilterActive = successState.scanlatorFilterActive,
                onScanlatorFilterClicked = { showScanlatorsDialog = true },
            )
            TextbookViewModel.Dialog.TrackSheet -> {
                NavigatorAdaptiveSheet(
                    screen = TrackInfoDialogHomeScreen(
                        textbookId = successState.manga.id,
                        textbookTitle = successState.manga.title,
                        sourceId = successState.source.id,
                    ),
                    enableSwipeDismiss = { it.lastItem is TrackInfoDialogHomeScreen },
                    onDismissRequest = onDismissRequest,
                )
            }
            TextbookViewModel.Dialog.FullCover -> {
                val vm =
                    assistedMetroViewModel<TextbookCoverViewModel, TextbookCoverViewModel.Factory> {
                        create(textbookId = textbookId)
                    }
                val manga by vm.state.collectAsStateWithLifecycle()
                if (manga != null) {
                    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                        if (it == null) return@rememberLauncherForActivityResult
                        vm.editCover(context, it)
                    }
                    TextbookCoverDialog(
                        manga = manga!!,
                        snackbarHostState = vm.snackbarHostState,
                        isCustomCover = remember(manga) { manga!!.hasCustomCover() },
                        onShareClick = { vm.shareCover(context) },
                        onSaveClick = { vm.saveCover(context) },
                        onEditClick = {
                            when (it) {
                                EditCoverAction.EDIT -> getContent.launch("image/*")
                                EditCoverAction.DELETE -> vm.deleteCustomCover(context)
                            }
                        },
                        onDismissRequest = onDismissRequest,
                    )
                } else {
                    LoadingScreen(Modifier.systemBarsPadding())
                }
            }
            is TextbookViewModel.Dialog.SetFetchInterval -> {
                SetIntervalDialog(
                    interval = dialog.manga.fetchInterval,
                    nextUpdate = dialog.manga.expectedNextUpdate,
                    onDismissRequest = onDismissRequest,
                    onValueChanged = { interval: Int -> viewModel.setFetchInterval(dialog.manga, interval) }
                        .takeIf { viewModel.isUpdateIntervalEnabled },
                )
            }
        }

        if (showScanlatorsDialog) {
            ScanlatorFilterDialog(
                availableScanlators = successState.availableScanlators,
                excludedScanlators = successState.excludedScanlators,
                onDismissRequest = { showScanlatorsDialog = false },
                onConfirm = viewModel::setExcludedScanlators,
            )
        }
    }

    private fun continueReading(context: Context, unreadChapter: Chapter?) {
        if (unreadChapter != null) openChapter(context, unreadChapter)
    }

    private fun openChapter(context: Context, chapter: Chapter) {
        context.startActivity(ReaderActivity.newIntent(context, chapter.textbookId, chapter.id))
    }

    private fun getTextbookUrl(manga_: Textbook?, source_: Source?): String? {
        val manga = manga_ ?: return null
        val source = source_ as? HttpSource ?: return null

        return try {
            source.getTextbookUrl(manga.toSTextbook())
        } catch (e: Exception) {
            null
        }
    }

    private fun openMangaInWebView(navigator: Navigator, manga_: Textbook?, source_: Source?) {
        getTextbookUrl(manga_, source_)?.let { url ->
            navigator.push(
                WebViewScreen(
                    url = url,
                    initialTitle = manga_?.title,
                    sourceId = source_?.id,
                ),
            )
        }
    }

    private fun shareManga(context: Context, manga_: Textbook?, source_: Source?) {
        try {
            getTextbookUrl(manga_, source_)?.let { url ->
                val intent = url.toUri().toShareIntent(context, type = "text/plain")
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    private suspend fun performSearch(navigator: Navigator, query: String, global: Boolean) {
        if (global) {
            navigator.push(GlobalSearchScreen(query))
            return
        }

        if (navigator.size < 2) {
            return
        }

        when (val previousController = navigator.items[navigator.size - 2]) {
            is HomeScreen -> {
                navigator.pop()
                previousController.search(query)
            }
            is BrowseSourceScreen -> {
                navigator.pop()
                previousController.search(query)
            }
        }
    }

    /**
     * Performs a genre search using the provided genre name.
     *
     * @param genreName the search genre to the parent controller
     */
    private suspend fun performGenreSearch(navigator: Navigator, genreName: String, source: Source) {
        if (navigator.size < 2) {
            return
        }

        val previousController = navigator.items[navigator.size - 2]
        if (previousController is BrowseSourceScreen && source is HttpSource) {
            navigator.pop()
            previousController.searchGenre(genreName)
        } else {
            performSearch(navigator, genreName, global = false)
        }
    }

    /**
     * Copy Textbook URL to Clipboard
     */
    private fun copyMangaUrl(context: Context, manga_: Textbook?, source_: Source?) {
        val manga = manga_ ?: return
        val source = source_ as? HttpSource ?: return
        val url = source.getTextbookUrl(manga.toSTextbook())
        context.copyToClipboard(url, url)
    }
}
