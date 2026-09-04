package com.bookshelf.domain.track.interactor

import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.track.model.Track
import com.bookshelf.domain.track.repository.TrackRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

@Inject
class GetTracks(
    private val trackRepository: TrackRepository,
) {

    suspend fun awaitOne(id: Long): Track? {
        return try {
            trackRepository.getTrackById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(textbookId: Long): List<Track> {
        return try {
            trackRepository.getTracksByTextbookId(textbookId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(textbookId: Long): Flow<List<Track>> {
        return trackRepository.getTracksByTextbookIdAsFlow(textbookId)
    }
}
