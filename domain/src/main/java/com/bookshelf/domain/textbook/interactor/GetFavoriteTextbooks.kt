package com.bookshelf.domain.textbook.interactor

import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.repository.TextbookRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class GetFavoriteTextbooks(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun await(): List<Textbook> {
        return mangaRepository.getFavorites()
    }

    fun subscribe(sourceId: Long): Flow<List<Textbook>> {
        return mangaRepository.getFavoritesBySourceId(sourceId)
    }
}
