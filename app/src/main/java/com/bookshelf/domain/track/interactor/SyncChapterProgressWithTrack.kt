package com.bookshelf.domain.track.interactor

import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.data.track.EnhancedTracker
import com.bookshelf.data.track.Tracker
import com.bookshelf.domain.chapter.interactor.GetChaptersByTextbookId
import com.bookshelf.domain.chapter.interactor.UpdateChapter
import com.bookshelf.domain.chapter.model.toChapterUpdate
import com.bookshelf.domain.track.interactor.InsertTrack
import com.bookshelf.domain.track.model.Track
import com.bookshelf.domain.track.model.toDbTrack
import dev.zacsweers.metro.Inject
import kotlin.math.max
import logcat.LogPriority

@Inject
class SyncChapterProgressWithTrack(
    private val updateChapter: UpdateChapter,
    private val insertTrack: InsertTrack,
    private val getChaptersByTextbookId: GetChaptersByTextbookId,
) {

    suspend fun await(
        textbookId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ) {
        if (tracker !is EnhancedTracker) {
            return
        }

        val sortedChapters = getChaptersByTextbookId.await(textbookId)
            .sortedBy { it.chapterNumber }
            .filter { it.isRecognizedNumber }

        val chapterUpdates = sortedChapters
            .filter { chapter -> chapter.chapterNumber <= remoteTrack.lastChapterRead && !chapter.read }
            .map { it.copy(read = true).toChapterUpdate() }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        val lastRead = max(remoteTrack.lastChapterRead, localLastRead.toDouble())
        val updatedTrack = remoteTrack.copy(lastChapterRead = lastRead)

        try {
            tracker.update(updatedTrack.toDbTrack())
            updateChapter.awaitAll(chapterUpdates)
            insertTrack.await(updatedTrack)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
