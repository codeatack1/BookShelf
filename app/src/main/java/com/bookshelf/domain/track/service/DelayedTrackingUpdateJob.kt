package com.bookshelf.domain.track.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import com.bookshelf.domain.track.interactor.TrackChapter
import com.bookshelf.domain.track.store.DelayedTrackingStore
import com.bookshelf.util.system.workManager
import logcat.LogPriority
import com.bookshelf.app.di.AppGraph
import com.bookshelf.core.metro.metroGraph
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.track.interactor.GetTracks
import java.util.concurrent.TimeUnit

class DelayedTrackingUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val graph: AppGraph = context.metroGraph()

    @Inject lateinit var getTracks: GetTracks

    @Inject lateinit var trackChapter: TrackChapter

    @Inject lateinit var delayedTrackingStore: DelayedTrackingStore

    override suspend fun doWork(): Result {
        graph.inject(this)

        if (runAttemptCount > 3) {
            return Result.failure()
        }

        withIOContext {
            delayedTrackingStore.getItems()
                .mapNotNull {
                    val track = getTracks.awaitOne(it.trackId)
                    if (track == null) {
                        delayedTrackingStore.remove(it.trackId)
                    }
                    track?.copy(lastChapterRead = it.lastChapterRead.toDouble())
                }
                .forEach { track ->
                    logcat(LogPriority.DEBUG) {
                        "Updating delayed track item: ${track.mangaId}, last chapter read: ${track.lastChapterRead}"
                    }
                    trackChapter.await(context, track.mangaId, track.lastChapterRead, setupJobOnFailure = false)
                }
        }

        return if (delayedTrackingStore.getItems().isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "DelayedTrackingUpdate"

        fun setupTask(context: Context) {
            val constraints = Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
            )

            val request = OneTimeWorkRequestBuilder<DelayedTrackingUpdateJob>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
