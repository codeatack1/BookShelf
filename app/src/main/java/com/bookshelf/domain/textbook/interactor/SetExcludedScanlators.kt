package com.bookshelf.domain.textbook.interactor

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.bookshelf.data.Database
import dev.zacsweers.metro.Inject

@Inject
class SetExcludedScanlators(
    private val database: Database,
) {

    suspend fun await(textbookId: Long, excludedScanlators: Set<String>) {
        database.transaction {
            val currentExcluded = database.excluded_scanlatorsQueries
                .getExcludedScanlatorsByTextbookId(textbookId)
                .awaitAsList()
                .toSet()
            val toAdd = excludedScanlators.minus(currentExcluded)
            for (scanlator in toAdd) {
                database.excluded_scanlatorsQueries.insert(textbookId, scanlator)
            }
            val toRemove = currentExcluded.minus(excludedScanlators)
            database.excluded_scanlatorsQueries.remove(textbookId, toRemove)
        }
    }
}
