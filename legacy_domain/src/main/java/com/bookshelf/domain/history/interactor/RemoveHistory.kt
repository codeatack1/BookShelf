package com.bookshelf.domain.history.interactor

import com.bookshelf.domain.history.model.HistoryWithRelations
import com.bookshelf.domain.history.repository.HistoryRepository
import dev.zacsweers.metro.Inject

@Inject
class RemoveHistory(
    private val repository: HistoryRepository,
) {

    suspend fun awaitAll(): Boolean {
        return repository.deleteAllHistory()
    }

    suspend fun await(history: HistoryWithRelations) {
        repository.resetHistory(history.id)
    }

    suspend fun await(textbookId: Long) {
        repository.resetHistoryByTextbookId(textbookId)
    }
}
