package com.bookshelf.data.backup

import android.net.Uri
import dev.zacsweers.metro.Inject
import com.bookshelf.data.track.TrackerManager
import com.bookshelf.domain.source.service.SourceManager

@Inject
class BackupFileValidator(
    private val sourceManager: SourceManager,
    private val trackerManager: TrackerManager,
    private val backupDecoder: BackupDecoder,
) {

    /**
     * Checks for critical backup file data.
     *
     * @return List of missing sources or missing trackers.
     */
    suspend fun validate(uri: Uri): Results {
        val backup = try {
            backupDecoder.decode(uri)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        val sources = backup.backupSources.associate { it.sourceId to it.name }
        val missingSources = sources
            .filterKeys { sourceManager.get(it) == null }
            .values.map {
                val id = it.toLongOrNull()
                if (id == null) {
                    it
                } else {
                    sourceManager.getOrStub(id).toString()
                }
            }
            .distinct()
            .sorted()

        val trackers = backup.backupManga
            .flatMap { it.tracking }
            .map { it.syncId }
            .distinct()
        val missingTrackers = trackers
            .mapNotNull { trackerManager.get(it.toLong()) }
            .filter { !it.isLoggedIn }
            .map { it.name }
            .sorted()

        return Results(missingSources, missingTrackers)
    }

    data class Results(
        val missingSources: List<String>,
        val missingTrackers: List<String>,
    )
}
