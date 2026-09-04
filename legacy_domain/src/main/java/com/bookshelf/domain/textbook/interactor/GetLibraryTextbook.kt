package com.bookshelf.domain.textbook.interactor

import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.library.model.LibraryTextbook
import com.bookshelf.domain.textbook.repository.TextbookRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import logcat.LogPriority
import kotlin.time.Duration.Companion.seconds

@Inject
class GetLibraryTextbook(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun await(): List<LibraryTextbook> {
        return mangaRepository.getLibraryTextbook()
    }

    fun subscribe(): Flow<List<LibraryTextbook>> {
        return mangaRepository.getLibraryTextbookAsFlow()
            .retry {
                if (it is NullPointerException) {
                    delay(0.5.seconds)
                    true
                } else {
                    false
                }
            }.catch {
                this@GetLibraryTextbook.logcat(LogPriority.ERROR, it)
            }
    }
}
