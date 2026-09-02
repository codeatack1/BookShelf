package com.bookshelf.domain.history.interactor

import com.bookshelf.domain.history.repository.HistoryRepository
import dev.zacsweers.metro.Inject

@Inject
class GetTotalReadDuration(
    private val repository: HistoryRepository,
) {

    suspend fun await(): Long {
        return repository.getTotalReadDuration()
    }
}
