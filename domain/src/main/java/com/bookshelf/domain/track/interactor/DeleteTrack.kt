package com.bookshelf.domain.track.interactor

import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.track.repository.TrackRepository
import dev.zacsweers.metro.Inject
import logcat.LogPriority

@Inject
class DeleteTrack(
    private val trackRepository: TrackRepository,
) {

    suspend fun await(textbookId: Long, trackerId: Long) {
        try {
            trackRepository.delete(textbookId, trackerId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
