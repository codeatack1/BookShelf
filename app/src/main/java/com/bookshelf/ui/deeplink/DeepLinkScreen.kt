package com.bookshelf.ui.deeplink

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.util.Screen
import com.bookshelf.ui.browse.source.globalsearch.GlobalSearchScreen
import com.bookshelf.ui.manga.MangaScreen
import com.bookshelf.ui.reader.ReaderActivity
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.core.screens.LoadingScreen

class DeepLinkScreen(
    val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = assistedMetroViewModel<DeepLinkViewModel, DeepLinkViewModel.Factory> { create(query = query) }
        val state by viewModel.state.collectAsState()
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.action_search_hint),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when (state) {
                is DeepLinkViewModel.State.Loading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                is DeepLinkViewModel.State.NoResults -> {
                    navigator.replace(GlobalSearchScreen(query))
                }
                is DeepLinkViewModel.State.Result -> {
                    val resultState = state as DeepLinkViewModel.State.Result
                    if (resultState.chapterId == null) {
                        navigator.replace(
                            MangaScreen(
                                resultState.manga.id,
                                true,
                            ),
                        )
                    } else {
                        navigator.pop()
                        ReaderActivity.newIntent(
                            context,
                            resultState.manga.id,
                            resultState.chapterId,
                        ).also(context::startActivity)
                    }
                }
            }
        }
    }
}
