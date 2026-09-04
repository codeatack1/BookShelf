package com.bookshelf.domain.track.interactor

import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.track.model.Track
import com.bookshelf.domain.track.repository.TrackRepository
import dev.zacsweers.metro.Inject
import logcat.LogPriority

@Inject
class InsertTrack(
    private val trackRepository: TrackRepository,
) {

    suspend fun await(track: Track) {
        try {
            trackRepository.insert(track)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(tracks: List<Track>) {
        try {
            trackRepository.insertAll(tracks)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
