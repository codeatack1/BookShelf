package com.bookshelf.domain.manga.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.manga.model.MangaUpdate
import com.bookshelf.domain.manga.repository.MangaRepository

@Inject
class UpdateMangaNotes(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(mangaId: Long, notes: String): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = mangaId,
                notes = notes,
            ),
        )
    }
}
