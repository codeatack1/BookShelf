package com.bookshelf.domain.upcoming.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.source.model.SManga
import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.domain.manga.repository.MangaRepository

@Inject
class GetUpcomingManga(
    private val mangaRepository: MangaRepository,
) {

    private val includedStatuses = setOf(
        SManga.ONGOING.toLong(),
        SManga.PUBLISHING_FINISHED.toLong(),
    )

    suspend fun subscribe(
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
    ): Flow<List<Manga>> {
        return mangaRepository.getUpcomingManga(
            includedStatuses,
            excludedCategories = excludedCategories,
            includedCategories = includedCategories,
        )
    }
}
