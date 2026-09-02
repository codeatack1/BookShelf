package com.bookshelf.domain.textbook.interactor

import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.repository.TextbookRepository
import dev.zacsweers.metro.Inject

@Inject
class UpdateTextbookNotes(
    private val mangaRepository: TextbookRepository,
) {

    suspend operator fun invoke(textbookId: Long, notes: String): Boolean {
        return mangaRepository.update(
            TextbookUpdate(
                id = textbookId,
                notes = notes,
            ),
        )
    }
}
