package com.bookshelf.domain.category.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.manga.repository.MangaRepository

@Inject
class SetMangaCategories(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(mangaId: Long, categoryIds: List<Long>) {
        try {
            mangaRepository.setMangaCategories(mangaId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
