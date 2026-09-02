package com.bookshelf.domain.textbook.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.TextbookWithChapterCount
import com.bookshelf.domain.textbook.repository.TextbookRepository

@Inject
class GetDuplicateLibraryTextbook(
    private val mangaRepository: TextbookRepository,
) {

    suspend operator fun invoke(manga: Textbook): List<TextbookWithChapterCount> {
        return mangaRepository.getDuplicateLibraryTextbook(manga.id, manga.title.lowercase())
    }
}
