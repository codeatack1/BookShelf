package com.bookshelf.domain.manga.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.domain.manga.model.MangaWithChapterCount
import com.bookshelf.domain.manga.repository.MangaRepository

@Inject
class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(manga: Manga): List<MangaWithChapterCount> {
        return mangaRepository.getDuplicateLibraryManga(manga.id, manga.title.lowercase())
    }
}
