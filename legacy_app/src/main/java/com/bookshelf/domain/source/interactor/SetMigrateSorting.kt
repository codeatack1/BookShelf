package com.bookshelf.domain.source.interactor

import com.bookshelf.domain.source.service.SourcePreferences
import dev.zacsweers.metro.Inject

@Inject
class SetMigrateSorting(
    private val preferences: SourcePreferences,
) {

    fun await(mode: Mode, direction: Direction) {
        preferences.migrationSortingMode.set(mode)
        preferences.migrationSortingDirection.set(direction)
    }

    enum class Mode {
        ALPHABETICAL,
        TOTAL,
    }

    enum class Direction {
        ASCENDING,
        DESCENDING,
    }
}
