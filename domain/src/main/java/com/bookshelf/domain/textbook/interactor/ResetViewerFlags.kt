package com.bookshelf.domain.textbook.interactor

import com.bookshelf.domain.textbook.repository.TextbookRepository
import dev.zacsweers.metro.Inject

@Inject
class ResetViewerFlags(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun await(): Boolean {
        return mangaRepository.resetViewerFlags()
    }
}
