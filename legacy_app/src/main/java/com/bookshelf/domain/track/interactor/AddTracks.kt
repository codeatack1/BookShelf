package com.bookshelf.domain.track.interactor

import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.core.common.util.lang.withNonCancellableContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.data.database.models.Track
import com.bookshelf.data.track.EnhancedTracker
import com.bookshelf.data.track.Tracker
import com.bookshelf.data.track.TrackerManager
import com.bookshelf.domain.chapter.interactor.GetChaptersByTextbookId
import com.bookshelf.domain.history.interactor.GetHistory
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.track.interactor.InsertTrack
import com.bookshelf.domain.track.model.toDbTrack
import com.bookshelf.domain.track.model.toDomainTrack
import com.bookshelf.source.Source
import com.bookshelf.util.lang.convertEpochMillisZone
import dev.zacsweers.metro.Inject
import kotlinx.datetime.TimeZone
import logcat.LogPriority

@Inject
class AddTracks(
    private val insertTrack: InsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
    private val getChaptersByTextbookId: GetChaptersByTextbookId,
    private val trackerManager: TrackerManager,
    private val getHistory: GetHistory,
) {

    // TODO: update all trackers based on common data
    suspend fun bind(tracker: Tracker, item: Track, textbookId: Long) = withNonCancellableContext {
        withIOContext {
            val allChapters = getChaptersByTextbookId.await(textbookId)
            val hasReadChapters = allChapters.any { it.read }
            tracker.bind(item, hasReadChapters)

            var track = item.toDomainTrack(idRequired = false) ?: return@withIOContext

            insertTrack.await(track)

            // TODO: merge into [SyncChapterProgressWithTrack]?
            // Update chapter progress if newer chapters marked read locally
            if (hasReadChapters) {
                val latestLocalReadChapterNumber = allChapters
                    .sortedBy { it.chapterNumber }
                    .takeWhile { it.read }
                    .lastOrNull()
                    ?.chapterNumber ?: -1.0

                if (latestLocalReadChapterNumber > track.lastChapterRead) {
                    track = track.copy(
                        lastChapterRead = latestLocalReadChapterNumber,
                    )
                    tracker.setRemoteLastChapterRead(track.toDbTrack(), latestLocalReadChapterNumber.toInt())
                }

                if (track.startDate <= 0) {
                    val firstReadChapterDate = getHistory.await(textbookId)
                        .sortedBy { it.readAt }
                        .firstOrNull()
                        ?.readAt

                    firstReadChapterDate?.let {
                        val startDate = firstReadChapterDate.time.convertEpochMillisZone(
                            TimeZone.currentSystemDefault(),
                            TimeZone.UTC,
                        )
                        track = track.copy(
                            startDate = startDate,
                        )
                        tracker.setRemoteStartDate(track.toDbTrack(), startDate)
                    }
                }
            }

            syncChapterProgressWithTrack.await(textbookId, track, tracker)
        }
    }

    suspend fun bindEnhancedTrackers(manga: Textbook, source: Source) = withNonCancellableContext {
        withIOContext {
            trackerManager.loggedInTrackers()
                .filterIsInstance<EnhancedTracker>()
                .filter { it.accept(source) }
                .forEach { service ->
                    try {
                        service.match(manga)?.let { track ->
                            track.manga_id = manga.id
                            (service as Tracker).bind(track)
                            insertTrack.await(track.toDomainTrack(idRequired = false)!!)

                            syncChapterProgressWithTrack.await(
                                manga.id,
                                track.toDomainTrack(idRequired = false)!!,
                                service,
                            )
                        }
                    } catch (e: Exception) {
                        logcat(
                            LogPriority.WARN,
                            e,
                        ) { "Could not match manga: ${manga.title} with service $service" }
                    }
                }
        }
    }
}
