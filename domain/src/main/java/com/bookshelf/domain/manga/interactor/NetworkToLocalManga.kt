package com.bookshelf.domain.manga.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.domain.manga.repository.MangaRepository

@Inject
class NetworkToLocalManga(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(manga: Manga): Manga {
        return invoke(listOf(manga)).single()
    }

    suspend operator fun invoke(manga: List<Manga>): List<Manga> {
        return mangaRepository.insertNetworkManga(manga)
    }
}
