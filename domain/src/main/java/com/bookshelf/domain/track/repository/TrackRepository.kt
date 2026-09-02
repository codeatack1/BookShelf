package com.bookshelf.domain.track.repository

import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.track.model.Track

interface TrackRepository {

    suspend fun getTrackById(id: Long): Track?

    suspend fun getTracksByTextbookId(textbookId: Long): List<Track>

    fun getTracksAsFlow(): Flow<List<Track>>

    fun getTracksByTextbookIdAsFlow(textbookId: Long): Flow<List<Track>>

    suspend fun delete(textbookId: Long, trackerId: Long)

    suspend fun insert(track: Track)

    suspend fun insertAll(tracks: List<Track>)
}
