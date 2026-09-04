package com.bookshelf.ui.browse.migration.search

import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.bookshelf.core.common.Constants
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.feature.migration.dialog.MigrateTextbookDialog
import com.bookshelf.feature.migration.list.MigrationListScreen
import com.bookshelf.i18n.MR
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.rounded.FilterList
import com.bookshelf.presentation.browse.BrowseSourceContent
import com.bookshelf.presentation.components.SearchToolbar
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.core.screens.LoadingScreen
import com.bookshelf.presentation.core.util.collectAsLazyPagingItems
import com.bookshelf.presentation.util.Screen
import com.bookshelf.source.local.LocalSource
import com.bookshelf.source.online.HttpSource
import com.bookshelf.ui.browse.source.browse.BrowseSourceViewModel
import com.bookshelf.ui.browse.source.browse.SourceFilterDialog
import com.bookshelf.ui.home.HomeScreen
import com.bookshelf.ui.textbook.TextbookScreen
import com.bookshelf.ui.webview.WebViewScreen
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.launch

data class MigrateSourceSearchScreen(
    private val currentManga: Textbook,
    private val sourceId: Long,
    private val query: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val viewModel =
            assistedMetroViewModel<BrowseSourceViewModel, BrowseSourceViewModel.Factory> {
                create(sourceId = sourceId, listingQuery = query)
            }
        val state by viewModel.state.collectAsState()

        val source = state.source
        if (source == null) {
            LoadingScreen()
            return
        }

        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.toolbarQuery ?: "",
                    onChangeSearchQuery = viewModel::setToolbarQuery,
                    onClickCloseSearch = navigator::pop,
                    onSearch = viewModel::search,
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.action_filter)) },
                    icon = { Icon(MaterialSymbols.Rounded.FilterList, contentDescription = null) },
                    onClick = viewModel::openFilterSheet,
                    modifier = Modifier.animateFloatingActionButton(
                        visible = state.filters.isNotEmpty(),
                        alignment = Alignment.BottomEnd,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            val openMigrateDialog: (Textbook) -> Unit = {
                val migrateListScreen = navigator.items
                    .filterIsInstance<MigrationListScreen>()
                    .lastOrNull()

                if (migrateListScreen == null) {
                    viewModel.setDialog(BrowseSourceViewModel.Dialog.Migrate(target = it, current = currentManga))
                } else {
                    migrateListScreen.addMatchOverride(current = currentManga.id, target = it.id)
                    navigator.popUntil { screen -> screen is MigrationListScreen }
                }
            }
            BrowseSourceContent(
                source = source,
                mangaList = viewModel.mangaPagerFlowFlow.collectAsLazyPagingItems(),
                columns = viewModel.getColumnsPreference(LocalConfiguration.current.orientation),
                displayMode = viewModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = paddingValues,
                onWebViewClick = {
                    val httpSource = source as? HttpSource ?: return@BrowseSourceContent
                    navigator.push(
                        WebViewScreen(
                            url = httpSource.getHomeUrl(),
                            initialTitle = httpSource.name,
                            sourceId = httpSource.id,
                        ),
                    )
                },
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalSourceHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) },
                onMangaClick = openMigrateDialog,
                onMangaLongClick = { navigator.push(TextbookScreen(it.id, true)) },
            )
        }

        val onDismissRequest = { viewModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is BrowseSourceViewModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    onReset = viewModel::resetFilters,
                    onFilter = { viewModel.search(filters = state.filters) },
                    onUpdate = viewModel::setFilters,
                )
            }
            is BrowseSourceViewModel.Dialog.Migrate -> {
                MigrateTextbookDialog(
                    current = currentManga,
                    target = dialog.target,
                    // Initiated from the context of [currentManga] so we show [dialog.target].
                    onClickTitle = { navigator.push(TextbookScreen(dialog.target.id)) },
                    onDismissRequest = onDismissRequest,
                    onComplete = {
                        scope.launch {
                            navigator.popUntilRoot()
                            HomeScreen.openTab(HomeScreen.Tab.Browse())
                            navigator.push(TextbookScreen(dialog.target.id))
                        }
                    },
                )
            }
            else -> {}
        }
    }
}
