package com.bookshelf.domain.migration.usecases

import com.bookshelf.data.cache.CoverCache
import com.bookshelf.data.download.DownloadManager
import com.bookshelf.data.track.EnhancedTracker
import com.bookshelf.data.track.TrackerManager
import com.bookshelf.domain.category.interactor.GetCategories
import com.bookshelf.domain.category.interactor.SetTextbookCategories
import com.bookshelf.domain.chapter.interactor.GetChaptersByTextbookId
import com.bookshelf.domain.chapter.interactor.UpdateChapter
import com.bookshelf.domain.chapter.model.toChapterUpdate
import com.bookshelf.domain.migration.models.MigrationFlag
import com.bookshelf.domain.source.interactor.UpdateTextbookFromRemote
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.domain.source.service.SourcePreferences
import com.bookshelf.domain.textbook.interactor.UpdateTextbook
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.model.hasCustomCover
import com.bookshelf.domain.track.interactor.GetTracks
import com.bookshelf.domain.track.interactor.InsertTrack
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException

@Inject
class MigrateTextbookUseCase(
    private val sourcePreferences: SourcePreferences,
    private val trackerManager: TrackerManager,
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
    private val updateManga: UpdateTextbook,
    private val getChaptersByTextbookId: GetChaptersByTextbookId,
    private val updateChapter: UpdateChapter,
    private val getCategories: GetCategories,
    private val setTextbookCategories: SetTextbookCategories,
    private val getTracks: GetTracks,
    private val insertTrack: InsertTrack,
    private val coverCache: CoverCache,
    private val updateMangaFromRemote: UpdateTextbookFromRemote,
) {
    private val enhancedServices by lazy { trackerManager.trackers.filterIsInstance<EnhancedTracker>() }

    suspend operator fun invoke(current: Textbook, target: Textbook, replace: Boolean) {
        val targetSource = sourceManager.get(target.source) ?: return
        val currentSource = sourceManager.get(current.source)
        val flags = sourcePreferences.migrationFlags.get()

        try {
            updateMangaFromRemote(target, fetchChapters = true).getOrThrow()

            // Update chapters read, bookmark and dateFetch
            if (MigrationFlag.CHAPTER in flags) {
                val prevMangaChapters = getChaptersByTextbookId.await(current.id)
                val mangaChapters = getChaptersByTextbookId.await(target.id)

                val maxChapterRead = prevMangaChapters
                    .filter { it.read }
                    .maxOfOrNull { it.chapterNumber }

                val updatedMangaChapters = mangaChapters.map { mangaChapter ->
                    var updatedChapter = mangaChapter
                    if (updatedChapter.isRecognizedNumber) {
                        val prevChapter = prevMangaChapters
                            .find { it.isRecognizedNumber && it.chapterNumber == updatedChapter.chapterNumber }

                        if (prevChapter != null) {
                            updatedChapter = updatedChapter.copy(
                                dateFetch = prevChapter.dateFetch,
                                bookmark = prevChapter.bookmark,
                            )
                        }

                        if (maxChapterRead != null && updatedChapter.chapterNumber <= maxChapterRead) {
                            updatedChapter = updatedChapter.copy(read = true)
                        }
                    }

                    updatedChapter
                }

                val chapterUpdates = updatedMangaChapters.map { it.toChapterUpdate() }
                updateChapter.awaitAll(chapterUpdates)
            }

            // Update categories
            if (MigrationFlag.CATEGORY in flags) {
                val categoryIds = getCategories.await(current.id).map { it.id }
                setTextbookCategories.await(target.id, categoryIds)
            }

            // Update track
            getTracks.await(current.id).mapNotNull { track ->
                val updatedTrack = track.copy(textbookId = target.id)

                val service = enhancedServices
                    .firstOrNull { it.isTrackFrom(updatedTrack, current, currentSource) }

                if (service != null) {
                    service.migrateTrack(updatedTrack, target, targetSource)
                } else {
                    updatedTrack
                }
            }
                .takeIf { it.isNotEmpty() }
                ?.let { insertTrack.awaitAll(it) }

            // Delete downloaded
            if (MigrationFlag.REMOVE_DOWNLOAD in flags && currentSource != null) {
                downloadManager.deleteManga(current, currentSource)
            }

            // Update custom cover (recheck if custom cover exists)
            if (MigrationFlag.CUSTOM_COVER in flags && current.hasCustomCover()) {
                coverCache.setCustomCoverToCache(target, coverCache.getCustomCoverFile(current.id).inputStream())
            }

            val currentTextbookUpdate = TextbookUpdate(
                id = current.id,
                favorite = false,
                dateAdded = 0,
            )
                .takeIf { replace }
            val targetTextbookUpdate = TextbookUpdate(
                id = target.id,
                favorite = true,
                chapterFlags = current.chapterFlags,
                viewerFlags = current.viewerFlags,
                dateAdded = if (replace) current.dateAdded else Clock.System.now().toEpochMilliseconds(),
                notes = if (MigrationFlag.NOTES in flags) current.notes else null,
            )

            updateManga.awaitAll(listOfNotNull(currentTextbookUpdate, targetTextbookUpdate))
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
        }
    }
}
