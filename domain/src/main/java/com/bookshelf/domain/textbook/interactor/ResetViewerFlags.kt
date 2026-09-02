package com.bookshelf.domain.textbook.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.textbook.repository.TextbookRepository

@Inject
class ResetViewerFlags(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun await(): Boolean {
        return mangaRepository.resetViewerFlags()
    }
}
