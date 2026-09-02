package com.bookshelf.ui.browse.migration.search

import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import com.bookshelf.domain.source.service.SourcePreferences
import com.bookshelf.extension.ExtensionManager
import com.bookshelf.source.Source
import com.bookshelf.ui.browse.source.globalsearch.SearchItemResult
import com.bookshelf.ui.browse.source.globalsearch.SearchViewModel
import kotlinx.coroutines.launch
import com.bookshelf.domain.textbook.interactor.GetTextbook
import com.bookshelf.domain.textbook.interactor.NetworkToLocalTextbook
import com.bookshelf.domain.source.service.SourceManager

@AssistedInject
class MigrateSearchViewModel(
    @Assisted val textbookId: Long,
    sourcePreferences: SourcePreferences,
    extensionManager: ExtensionManager,
    networkToLocalManga: NetworkToLocalTextbook,
    getManga: GetTextbook,
    preferences: SourcePreferences,
    private val sourceManager: SourceManager,
) : SearchViewModel(
    sourcePreferences = sourcePreferences,
    sourceManager = sourceManager,
    extensionManager = extensionManager,
    networkToLocalManga = networkToLocalManga,
    getManga = getManga,
    preferences = preferences,
) {
    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(textbookId: Long): MigrateSearchViewModel
    }

    private val migrationSources by lazy { sourcePreferences.migrationSources.get() }

    override val sortComparator = { map: Map<Source, SearchItemResult> ->
        compareBy<Source>(
            { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true },
            { migrationSources.indexOf(it.id) },
        )
    }

    init {
        viewModelScope.launch {
            val manga = getManga.await(textbookId)!!
            updateState {
                it.copy(
                    from = manga,
                    searchQuery = manga.title,
                )
            }
            search()
        }
    }

    override suspend fun getEnabledSources(): List<Source> {
        return migrationSources.mapNotNull { sourceManager.get(it) }
    }
}
