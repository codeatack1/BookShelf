package com.bookshelf.domain.textbook.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.repository.TextbookRepository

@Inject
class NetworkToLocalTextbook(
    private val mangaRepository: TextbookRepository,
) {

    suspend operator fun invoke(manga: Textbook): Textbook {
        return invoke(listOf(manga)).single()
    }

    suspend operator fun invoke(manga: List<Textbook>): List<Textbook> {
        return mangaRepository.insertNetworkTextbook(manga)
    }
}
