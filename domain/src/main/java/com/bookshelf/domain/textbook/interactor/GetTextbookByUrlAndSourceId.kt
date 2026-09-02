package com.bookshelf.domain.textbook.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.repository.TextbookRepository

@Inject
class GetTextbookByUrlAndSourceId(
    private val mangaRepository: TextbookRepository,
) {
    suspend fun await(url: String, sourceId: Long): Textbook? {
        return mangaRepository.getTextbookByUrlAndSourceId(url, sourceId)
    }
}
