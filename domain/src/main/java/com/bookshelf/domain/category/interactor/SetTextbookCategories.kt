package com.bookshelf.domain.category.interactor

import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.textbook.repository.TextbookRepository
import dev.zacsweers.metro.Inject
import logcat.LogPriority

@Inject
class SetTextbookCategories(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun await(textbookId: Long, categoryIds: List<Long>) {
        try {
            mangaRepository.setTextbookCategories(textbookId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
