package com.bookshelf.domain.textbook.interactor

import com.bookshelf.domain.textbook.interactor.FetchInterval
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.repository.TextbookRepository
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Inject
class UpdateTextbook(
    private val mangaRepository: TextbookRepository,
    private val fetchInterval: FetchInterval,
) {

    suspend fun await(mangaUpdate: TextbookUpdate): Boolean {
        return mangaRepository.update(mangaUpdate)
    }

    suspend fun awaitAll(mangaUpdates: List<TextbookUpdate>): Boolean {
        return mangaRepository.updateAll(mangaUpdates)
    }

    suspend fun awaitUpdateFetchInterval(
        manga: Textbook,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        dateTime: LocalDateTime = Clock.System.now().toLocalDateTime(timeZone),
        window: Pair<Long, Long> = fetchInterval.getWindow(dateTime.date, timeZone),
    ): Boolean {
        return mangaRepository.update(
            fetchInterval.toTextbookUpdate(manga, dateTime, timeZone, window),
        )
    }

    suspend fun awaitUpdateLastUpdate(textbookId: Long): Boolean {
        return mangaRepository.update(TextbookUpdate(id = textbookId, lastUpdate = Clock.System.now().toEpochMilliseconds()))
    }

    suspend fun awaitUpdateCoverLastModified(textbookId: Long): Boolean {
        return mangaRepository.update(
            TextbookUpdate(
                id = textbookId,
                coverLastModified = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun awaitUpdateFavorite(textbookId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Clock.System.now().toEpochMilliseconds()
            false -> 0
        }
        return mangaRepository.update(
            TextbookUpdate(id = textbookId, favorite = favorite, dateAdded = dateAdded),
        )
    }
}
