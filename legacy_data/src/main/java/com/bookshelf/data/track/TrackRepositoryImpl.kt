package com.bookshelf.data.track

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.bookshelf.data.Database
import com.bookshelf.data.subscribeToList
import com.bookshelf.domain.track.model.Track
import com.bookshelf.domain.track.repository.TrackRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TrackRepositoryImpl(
    private val database: Database,
) : TrackRepository {

    override suspend fun getTrackById(id: Long): Track? {
        return database.textbook_syncQueries
            .getTrackById(id, TrackMapper::mapTrack)
            .awaitAsOneOrNull()
    }

    override suspend fun getTracksByTextbookId(textbookId: Long): List<Track> {
        return database.textbook_syncQueries
            .getTracksByTextbookId(textbookId, TrackMapper::mapTrack)
            .awaitAsList()
    }

    override fun getTracksAsFlow(): Flow<List<Track>> {
        return database.textbook_syncQueries
            .getTracks(TrackMapper::mapTrack)
            .subscribeToList()
    }

    override fun getTracksByTextbookIdAsFlow(textbookId: Long): Flow<List<Track>> {
        return database.textbook_syncQueries
            .getTracksByTextbookId(textbookId, TrackMapper::mapTrack)
            .subscribeToList()
    }

    override suspend fun delete(textbookId: Long, trackerId: Long) {
        database.textbook_syncQueries.delete(
            textbookId = textbookId,
            syncId = trackerId,
        )
    }

    override suspend fun insert(track: Track) {
        insertValues(track)
    }

    override suspend fun insertAll(tracks: List<Track>) {
        insertValues(*tracks.toTypedArray())
    }

    private suspend fun insertValues(vararg tracks: Track) {
        database.transaction {
            tracks.forEach { mangaTrack ->
                database.textbook_syncQueries.insert(
                    textbookId = mangaTrack.textbookId,
                    syncId = mangaTrack.trackerId,
                    remoteId = mangaTrack.remoteId,
                    libraryId = mangaTrack.libraryId,
                    title = mangaTrack.title,
                    lastChapterRead = mangaTrack.lastChapterRead,
                    totalChapters = mangaTrack.totalChapters,
                    status = mangaTrack.status,
                    score = mangaTrack.score,
                    remoteUrl = mangaTrack.remoteUrl,
                    startDate = mangaTrack.startDate,
                    finishDate = mangaTrack.finishDate,
                    private = mangaTrack.private,
                )
            }
        }
    }
}
