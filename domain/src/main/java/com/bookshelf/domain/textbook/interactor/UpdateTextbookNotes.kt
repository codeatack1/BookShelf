package com.bookshelf.domain.textbook.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.repository.TextbookRepository

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
