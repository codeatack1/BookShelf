package com.bookshelf.domain.textbook.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.repository.TextbookRepository

@Inject
class GetTextbook(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun await(id: Long): Textbook? {
        return try {
            mangaRepository.getTextbookById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    fun subscribe(id: Long): Flow<Textbook> {
        return mangaRepository.getTextbookByIdAsFlow(id)
    }

    fun subscribe(url: String, sourceId: Long): Flow<Textbook?> {
        return mangaRepository.getTextbookByUrlAndSourceIdAsFlow(url, sourceId)
    }
}
