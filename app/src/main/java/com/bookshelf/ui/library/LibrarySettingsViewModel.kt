package com.bookshelf.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import com.bookshelf.domain.base.BasePreferences
import com.bookshelf.data.track.TrackerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.bookshelf.core.common.preference.Preference
import com.bookshelf.core.common.preference.TriState
import com.bookshelf.core.common.preference.getAndSet
import com.bookshelf.core.common.util.lang.launchIO
import com.bookshelf.domain.category.interactor.SetDisplayMode
import com.bookshelf.domain.category.interactor.SetSortModeForCategory
import com.bookshelf.domain.category.model.Category
import com.bookshelf.domain.library.model.LibraryDisplayMode
import com.bookshelf.domain.library.model.LibrarySort
import com.bookshelf.domain.library.service.LibraryPreferences
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class LibrarySettingsViewModel(
    val preferences: BasePreferences,
    val libraryPreferences: LibraryPreferences,
    private val setDisplayMode: SetDisplayMode,
    private val setSortModeForCategory: SetSortModeForCategory,
    trackerManager: TrackerManager,
) : ViewModel() {

    val trackersFlow = trackerManager.loggedInTrackersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            initialValue = trackerManager.loggedInTrackers(),
        )

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<TriState>) {
        preference(libraryPreferences).getAndSet {
            it.next()
        }
    }

    fun toggleTracker(id: Int) {
        toggleFilter { libraryPreferences.filterTracking(id) }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setDisplayMode.await(mode)
    }

    fun setSort(category: Category?, mode: LibrarySort.Type, direction: LibrarySort.Direction) {
        viewModelScope.launchIO {
            setSortModeForCategory.await(category, mode, direction)
        }
    }
}
