package com.bookshelf.domain.history.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.history.model.HistoryUpdate
import com.bookshelf.domain.history.repository.HistoryRepository

@Inject
class UpsertHistory(
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(historyUpdate: HistoryUpdate) {
        historyRepository.upsertHistory(historyUpdate)
    }
}
