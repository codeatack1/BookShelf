package com.bookshelf.ui.deeplink

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import com.bookshelf.source.Source
import com.bookshelf.source.model.SChapter
import com.bookshelf.source.online.ResolvableSource
import com.bookshelf.source.online.UriType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.bookshelf.domain.manga.model.toDomainManga
import com.bookshelf.domain.source.interactor.UpdateMangaFromRemote
import com.bookshelf.core.common.util.lang.launchIO
import com.bookshelf.domain.chapter.interactor.GetChapterByUrlAndMangaId
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.manga.interactor.NetworkToLocalManga
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.domain.source.service.SourceManager

@AssistedInject
class DeepLinkViewModel(
    @Assisted query: String,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
    private val getChapterByUrlAndMangaId: GetChapterByUrlAndMangaId,
    private val updateMangaFromRemote: UpdateMangaFromRemote,
) : ViewModel() {

    val state: StateFlow<DeepLinkViewModel.State>
        field = MutableStateFlow<DeepLinkViewModel.State>(State.Loading)

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(query: String): DeepLinkViewModel
    }

    init {
        viewModelScope.launchIO {
            val source = sourceManager.getAll()
                .filterIsInstance<ResolvableSource>()
                .firstOrNull { it.getUriType(query) != UriType.Unknown }

            val manga = source?.getManga(query)?.let {
                networkToLocalManga(it.toDomainManga(source.id))
            }

            val chapter = if (source?.getUriType(query) == UriType.Chapter && manga != null) {
                source.getChapter(query)?.let { getChapterFromSChapter(it, manga, source) }
            } else {
                null
            }

            state.update {
                if (manga == null) {
                    State.NoResults
                } else {
                    if (chapter == null) {
                        State.Result(manga)
                    } else {
                        State.Result(manga, chapter.id)
                    }
                }
            }
        }
    }

    private suspend fun getChapterFromSChapter(sChapter: SChapter, manga: Manga, source: Source): Chapter? {
        val localChapter = getChapterByUrlAndMangaId.await(sChapter.url, manga.id)

        return localChapter
            ?: updateMangaFromRemote(manga, fetchChapters = true)
                .getOrElse { return null }
                .newChapters
                .find { it.url == sChapter.url }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object NoResults : State

        @Immutable
        data class Result(val manga: Manga, val chapterId: Long? = null) : State
    }
}
