package com.bookshelf.domain.source.interactor

import com.bookshelf.core.common.util.lang.compareToWithCollator
import com.bookshelf.domain.source.model.Source
import com.bookshelf.domain.source.repository.SourceRepository
import com.bookshelf.domain.source.service.SourcePreferences
import com.bookshelf.source.local.isLocal
import dev.zacsweers.metro.Inject
import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
class GetSourcesWithFavoriteCount(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Pair<Source, Long>>> {
        return combine(
            preferences.migrationSortingDirection.changes(),
            preferences.migrationSortingMode.changes(),
            repository.getSourcesWithFavoriteCount(),
        ) { direction, mode, list ->
            list
                .filterNot { it.first.isLocal() }
                .sortedWith(sortFn(direction, mode))
        }
    }

    private fun sortFn(
        direction: SetMigrateSorting.Direction,
        sorting: SetMigrateSorting.Mode,
    ): java.util.Comparator<Pair<Source, Long>> {
        val sortFn: (Pair<Source, Long>, Pair<Source, Long>) -> Int = { a, b ->
            when (sorting) {
                SetMigrateSorting.Mode.ALPHABETICAL -> {
                    when {
                        a.first.isStub && !b.first.isStub -> -1
                        b.first.isStub && !a.first.isStub -> 1
                        else -> a.first.name.lowercase().compareToWithCollator(b.first.name.lowercase())
                    }
                }
                SetMigrateSorting.Mode.TOTAL -> {
                    when {
                        a.first.isStub && !b.first.isStub -> -1
                        b.first.isStub && !a.first.isStub -> 1
                        else -> a.second.compareTo(b.second)
                    }
                }
            }
        }

        return when (direction) {
            SetMigrateSorting.Direction.ASCENDING -> Comparator(sortFn)
            SetMigrateSorting.Direction.DESCENDING -> Collections.reverseOrder(sortFn)
        }
    }
}
