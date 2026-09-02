package com.bookshelf.domain.track.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.bookshelf.domain.track.model.Track
import com.bookshelf.domain.track.repository.TrackRepository

@Inject
class GetTracksPerTextbook(
    private val trackRepository: TrackRepository,
) {

    fun subscribe(): Flow<Map<Long, List<Track>>> {
        return trackRepository.getTracksAsFlow().map { tracks -> tracks.groupBy { it.textbookId } }
    }
}
