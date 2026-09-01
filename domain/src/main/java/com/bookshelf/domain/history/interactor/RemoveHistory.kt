package com.bookshelf.domain.history.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.history.model.HistoryWithRelations
import com.bookshelf.domain.history.repository.HistoryRepository

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

    suspend fun await(mangaId: Long) {
        repository.resetHistoryByMangaId(mangaId)
    }
}
