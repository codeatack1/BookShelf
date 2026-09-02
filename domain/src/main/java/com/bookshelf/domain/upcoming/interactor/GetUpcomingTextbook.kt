package com.bookshelf.domain.upcoming.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.source.model.STextbook
import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.repository.TextbookRepository

@Inject
class GetUpcomingTextbook(
    private val mangaRepository: TextbookRepository,
) {

    private val includedStatuses = setOf(
        STextbook.ONGOING.toLong(),
        STextbook.PUBLISHING_FINISHED.toLong(),
    )

    suspend fun subscribe(
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
    ): Flow<List<Textbook>> {
        return mangaRepository.getUpcomingTextbooks(
            includedStatuses,
            excludedCategories = excludedCategories,
            includedCategories = includedCategories,
        )
    }
}
