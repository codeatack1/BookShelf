package com.bookshelf.domain.textbook.interactor

import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.bookshelf.data.Database
import com.bookshelf.data.subscribeToList

@Inject
class GetExcludedScanlators(
    private val database: Database,
) {

    suspend fun await(textbookId: Long): Set<String> {
        return database.excluded_scanlatorsQueries
            .getExcludedScanlatorsByTextbookId(textbookId)
            .awaitAsList()
            .toSet()
    }

    fun subscribe(textbookId: Long): Flow<Set<String>> {
        return database.excluded_scanlatorsQueries
            .getExcludedScanlatorsByTextbookId(textbookId)
            .subscribeToList()
            .map { it.toSet() }
    }
}
