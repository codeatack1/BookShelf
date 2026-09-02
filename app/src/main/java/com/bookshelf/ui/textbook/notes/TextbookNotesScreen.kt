package com.bookshelf.ui.textbook.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.bookshelf.core.common.util.lang.launchNonCancellable
import com.bookshelf.domain.textbook.interactor.UpdateTextbookNotes
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.presentation.manga.TextbookNotesScreen
import com.bookshelf.presentation.util.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TextbookNotesScreen(
    private val manga: Textbook,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = assistedMetroViewModel<Model, Model.Factory> { create(manga = manga) }
        val state by viewModel.state.collectAsState()

        TextbookNotesScreen(
            state = state,
            navigateUp = navigator::pop,
            onUpdate = viewModel::updateNotes,
        )
    }

    @AssistedInject
    class Model(
        @Assisted private val manga: Textbook,
        private val updateMangaNotes: UpdateTextbookNotes,
    ) : ViewModel() {

        val state: StateFlow<State>
            field = MutableStateFlow<State>(State(manga, manga.notes))

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(manga: Textbook): Model
        }

        fun updateNotes(content: String) {
            if (content == state.value.notes) return

            state.update {
                it.copy(notes = content)
            }

            viewModelScope.launchNonCancellable {
                updateMangaNotes(manga.id, content)
            }
        }
    }

    @Immutable
    data class State(
        val manga: Textbook,
        val notes: String,
    )
}
