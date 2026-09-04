package com.bookshelf.ui.browse.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.browse.SourcesFilterScreen
import com.bookshelf.presentation.core.screens.LoadingScreen
import com.bookshelf.presentation.util.Screen
import com.bookshelf.util.system.toast
import dev.zacsweers.metrox.viewmodel.metroViewModel

class SourcesFilterScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<SourcesFilterViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state is SourcesFilterViewModel.State.Loading) {
            LoadingScreen()
            return
        }

        if (state is SourcesFilterViewModel.State.Error) {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.toast(MR.strings.internal_error)
                navigator.pop()
            }
            return
        }

        val successState = state as SourcesFilterViewModel.State.Success

        SourcesFilterScreen(
            navigateUp = navigator::pop,
            state = successState,
            onClickLanguage = viewModel::toggleLanguage,
            onClickSource = viewModel::toggleSource,
        )
    }
}
