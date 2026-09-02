package com.bookshelf.domain.history.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.history.model.History
import com.bookshelf.domain.history.model.HistoryWithRelations
import com.bookshelf.domain.history.repository.HistoryRepository

@Inject
class GetHistory(
    private val repository: HistoryRepository,
) {

    suspend fun await(textbookId: Long): List<History> {
        return repository.getHistoryByTextbookId(textbookId)
    }

    fun subscribe(query: String): Flow<List<HistoryWithRelations>> {
        return repository.getHistory(query)
    }
}
