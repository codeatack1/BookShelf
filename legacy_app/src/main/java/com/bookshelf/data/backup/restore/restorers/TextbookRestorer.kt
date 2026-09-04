package com.bookshelf.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.bookshelf.data.Database
import com.bookshelf.data.MemoColumnAdapter
import com.bookshelf.data.UpdateStrategyColumnAdapter
import com.bookshelf.data.backup.models.BackupCategory
import com.bookshelf.data.backup.models.BackupChapter
import com.bookshelf.data.backup.models.BackupHistory
import com.bookshelf.data.backup.models.BackupTextbook
import com.bookshelf.data.backup.models.BackupTracking
import com.bookshelf.domain.category.interactor.GetCategories
import com.bookshelf.domain.chapter.interactor.GetChaptersByTextbookId
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.textbook.interactor.FetchInterval
import com.bookshelf.domain.textbook.interactor.GetTextbookByUrlAndSourceId
import com.bookshelf.domain.textbook.interactor.UpdateTextbook
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.track.interactor.GetTracks
import com.bookshelf.domain.track.interactor.InsertTrack
import com.bookshelf.domain.track.model.Track
import dev.zacsweers.metro.Inject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Date
import kotlin.math.max
import kotlin.time.Clock

@Inject
class TextbookRestorer(
    private val database: Database,
    private val getCategories: GetCategories,
    private val getTextbookByUrlAndSourceId: GetTextbookByUrlAndSourceId,
    private val getChaptersByTextbookId: GetChaptersByTextbookId,
    private val updateManga: UpdateTextbook,
    private val getTracks: GetTracks,
    private val insertTrack: InsertTrack,
    fetchInterval: FetchInterval,
) {

    private val timeZone = TimeZone.currentSystemDefault()
    private val now = Clock.System.now().toLocalDateTime(timeZone)
    private val currentFetchWindow = fetchInterval.getWindow(now.date, timeZone)

    suspend fun sortByNew(backupMangas: List<BackupTextbook>): List<BackupTextbook> {
        val urlsBySource = database.textbooksQueries
            .getAllTextbookSourceAndUrl()
            .awaitAsList()
            .groupBy({ it.source }, { it.url })

        return backupMangas
            .sortedWith(
                compareBy<BackupTextbook> { it.url in urlsBySource[it.source].orEmpty() }
                    .then(compareByDescending { it.lastModifiedAt }),
            )
    }

    suspend fun restore(
        backupManga: BackupTextbook,
        backupCategories: List<BackupCategory>,
    ) {
        database.transaction {
            val dbManga = findExistingManga(backupManga)
            val manga = backupManga.getMangaImpl()
            val restoredManga = if (dbManga == null) {
                restoreNewManga(manga)
            } else {
                restoreExistingManga(manga, dbManga)
            }

            restoreMangaDetails(
                manga = restoredManga,
                chapters = backupManga.chapters,
                categories = backupManga.categories,
                backupCategories = backupCategories,
                history = backupManga.history,
                tracks = backupManga.tracking,
                excludedScanlators = backupManga.excludedScanlators,
            )
        }
    }

    private suspend fun findExistingManga(backupManga: BackupTextbook): Textbook? {
        return getTextbookByUrlAndSourceId.await(backupManga.url, backupManga.source)
    }

    private suspend fun restoreExistingManga(manga: Textbook, dbManga: Textbook): Textbook {
        return if (manga.version > dbManga.version) {
            updateManga(dbManga.copyFrom(manga).copy(id = dbManga.id))
        } else {
            updateManga(manga.copyFrom(dbManga).copy(id = dbManga.id))
        }
    }

    private fun Textbook.copyFrom(newer: Textbook): Textbook {
        return this.copy(
            favorite = this.favorite || newer.favorite,
            author = newer.author,
            artist = newer.artist,
            description = newer.description,
            genre = newer.genre,
            thumbnailUrl = newer.thumbnailUrl,
            status = newer.status,
            initialized = this.initialized || newer.initialized,
            version = newer.version,
        )
    }

    private suspend fun updateManga(manga: Textbook): Textbook {
        database.textbooksQueries.update(
            source = manga.source,
            url = manga.url,
            artist = manga.artist,
            author = manga.author,
            description = manga.description,
            genre = manga.genre?.joinToString(separator = ", "),
            title = manga.title,
            status = manga.status,
            thumbnailUrl = manga.thumbnailUrl,
            favorite = manga.favorite,
            lastUpdate = manga.lastUpdate,
            nextUpdate = null,
            calculateInterval = null,
            initialized = manga.initialized,
            viewer = manga.viewerFlags,
            chapterFlags = manga.chapterFlags,
            coverLastModified = manga.coverLastModified,
            dateAdded = manga.dateAdded,
            textbookId = manga.id,
            updateStrategy = manga.updateStrategy.let(UpdateStrategyColumnAdapter::encode),
            version = manga.version,
            isSyncing = 1,
            notes = manga.notes,
            memo = manga.memo.let(MemoColumnAdapter::encode),
        )
        return manga
    }

    private suspend fun restoreNewManga(
        manga: Textbook,
    ): Textbook {
        return manga.copy(
            id = insertManga(manga),
        )
    }

    private suspend fun restoreChapters(manga: Textbook, backupChapters: List<BackupChapter>) {
        val dbChaptersByUrl = getChaptersByTextbookId.await(manga.id)
            .associateBy { it.url }

        val (existingChapters, newChapters) = backupChapters
            .mapNotNull {
                val chapter = it.toChapterImpl().copy(textbookId = manga.id)

                val dbChapter = dbChaptersByUrl[chapter.url]
                    ?: // New chapter
                    return@mapNotNull chapter

                if (chapter.forComparison() == dbChapter.forComparison()) {
                    // Same state; skip
                    return@mapNotNull null
                }

                // Update to an existing chapter
                var updatedChapter = chapter
                    .copyFrom(dbChapter)
                    .copy(
                        id = dbChapter.id,
                        bookmark = chapter.bookmark || dbChapter.bookmark,
                    )
                if (dbChapter.read && !updatedChapter.read) {
                    updatedChapter = updatedChapter.copy(
                        read = true,
                        lastPageRead = dbChapter.lastPageRead,
                    )
                } else if (updatedChapter.lastPageRead == 0L && dbChapter.lastPageRead != 0L) {
                    updatedChapter = updatedChapter.copy(
                        lastPageRead = dbChapter.lastPageRead,
                    )
                }
                updatedChapter
            }
            .partition { it.id > 0 }

        insertNewChapters(newChapters)
        updateExistingChapters(existingChapters)
    }

    private fun Chapter.forComparison() =
        this.copy(id = 0L, textbookId = 0L, dateFetch = 0L, dateUpload = 0L, lastModifiedAt = 0L, version = 0L)

    private suspend fun insertNewChapters(chapters: List<Chapter>) {
        database.transaction {
            chapters.forEach { chapter ->
                database.chaptersQueries.insert(
                    chapter.textbookId,
                    chapter.url,
                    chapter.name,
                    chapter.scanlator,
                    chapter.read,
                    chapter.bookmark,
                    chapter.lastPageRead,
                    chapter.chapterNumber,
                    chapter.sourceOrder,
                    chapter.dateFetch,
                    chapter.dateUpload,
                    chapter.version,
                    chapter.memo,
                )
            }
        }
    }

    private suspend fun updateExistingChapters(chapters: List<Chapter>) {
        database.transaction {
            chapters.forEach { chapter ->
                database.chaptersQueries.update(
                    textbookId = null,
                    url = null,
                    name = null,
                    scanlator = null,
                    read = chapter.read,
                    bookmark = chapter.bookmark,
                    lastPageRead = chapter.lastPageRead,
                    chapterNumber = null,
                    sourceOrder = null,
                    dateFetch = null,
                    dateUpload = null,
                    chapterId = chapter.id,
                    version = chapter.version,
                    isSyncing = 0,
                    memo = chapter.memo.let(MemoColumnAdapter::encode),
                )
            }
        }
    }

    /**
     * Inserts manga and returns id
     *
     * @return id of [Textbook], null if not found
     */
    private suspend fun insertManga(manga: Textbook): Long {
        return database.textbooksQueries.insertReturningId(
            source = manga.source,
            url = manga.url,
            artist = manga.artist,
            author = manga.author,
            description = manga.description,
            genre = manga.genre,
            title = manga.title,
            status = manga.status,
            thumbnailUrl = manga.thumbnailUrl,
            favorite = manga.favorite,
            lastUpdate = manga.lastUpdate,
            nextUpdate = 0L,
            calculateInterval = 0L,
            initialized = manga.initialized,
            viewerFlags = manga.viewerFlags,
            chapterFlags = manga.chapterFlags,
            coverLastModified = manga.coverLastModified,
            dateAdded = manga.dateAdded,
            updateStrategy = manga.updateStrategy,
            version = manga.version,
            notes = manga.notes,
            memo = manga.memo,
        )
            .awaitAsOne()
    }

    private suspend fun restoreMangaDetails(
        manga: Textbook,
        chapters: List<BackupChapter>,
        categories: List<Long>,
        backupCategories: List<BackupCategory>,
        history: List<BackupHistory>,
        tracks: List<BackupTracking>,
        excludedScanlators: List<String>,
    ): Textbook {
        restoreCategories(manga, categories, backupCategories)
        restoreChapters(manga, chapters)
        restoreTracking(manga, tracks)
        restoreHistory(manga, history)
        restoreExcludedScanlators(manga, excludedScanlators)
        updateManga.awaitUpdateFetchInterval(manga, timeZone, now, currentFetchWindow)
        return manga
    }

    /**
     * Restores the categories a manga is in.
     *
     * @param manga the manga whose categories have to be restored.
     * @param categories the categories to restore.
     */
    private suspend fun restoreCategories(
        manga: Textbook,
        categories: List<Long>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategories = getCategories.await()
        val dbCategoriesByName = dbCategories.associateBy { it.name }

        val backupCategoriesByOrder = backupCategories.associateBy { it.order }

        val mangaCategoriesToUpdate = categories.mapNotNull { backupCategoryOrder ->
            backupCategoriesByOrder[backupCategoryOrder]?.let { backupCategory ->
                dbCategoriesByName[backupCategory.name]?.let { dbCategory ->
                    Pair(manga.id, dbCategory.id)
                }
            }
        }

        if (mangaCategoriesToUpdate.isNotEmpty()) {
            database.transaction {
                database.textbooks_categoriesQueries.deleteTextbookCategoryByTextbookId(manga.id)
                mangaCategoriesToUpdate.forEach { (textbookId, categoryId) ->
                    database.textbooks_categoriesQueries.insert(textbookId, categoryId)
                }
            }
        }
    }

    private suspend fun restoreHistory(manga: Textbook, backupHistory: List<BackupHistory>) {
        val toUpdate = backupHistory.mapNotNull { history ->
            val dbHistory = database.historyQueries
                .getHistoryByChapterUrlAndTextbookId(history.url, manga.id)
                .awaitAsOneOrNull()
            val item = history.getHistoryImpl()

            if (dbHistory == null) {
                val chapter = database.chaptersQueries
                    .getChapterByUrlAndTextbookId(history.url, manga.id)
                    .awaitAsOneOrNull()
                return@mapNotNull if (chapter == null) {
                    // Chapter doesn't exist; skip
                    null
                } else {
                    // New history entry
                    item.copy(chapterId = chapter._id)
                }
            }

            // Update history entry
            item.copy(
                id = dbHistory._id,
                chapterId = dbHistory.chapter_id,
                readAt = max(item.readAt?.time ?: 0L, dbHistory.last_read?.time ?: 0L)
                    .takeIf { it > 0L }
                    ?.let { Date(it) },
                readDuration = max(item.readDuration, dbHistory.time_read) - dbHistory.time_read,
            )
        }

        if (toUpdate.isEmpty()) return
        database.transaction {
            toUpdate.forEach {
                database.historyQueries.upsert(
                    it.chapterId,
                    it.readAt,
                    it.readDuration,
                )
            }
        }
    }

    private suspend fun restoreTracking(manga: Textbook, backupTracks: List<BackupTracking>) {
        val dbTrackByTrackerId = getTracks.await(manga.id).associateBy { it.trackerId }

        val (existingTracks, newTracks) = backupTracks
            .mapNotNull {
                val track = it.getTrackImpl()
                val dbTrack = dbTrackByTrackerId[track.trackerId]
                    ?: // New track
                    return@mapNotNull track.copy(
                        id = 0, // Let DB assign new ID
                        textbookId = manga.id,
                    )

                if (track.forComparison() == dbTrack.forComparison()) {
                    // Same state; skip
                    return@mapNotNull null
                }

                // Update to an existing track
                dbTrack.copy(
                    remoteId = track.remoteId,
                    libraryId = track.libraryId,
                    lastChapterRead = max(dbTrack.lastChapterRead, track.lastChapterRead),
                )
            }
            .partition { it.id > 0 }

        if (newTracks.isNotEmpty()) {
            insertTrack.awaitAll(newTracks)
        }

        if (existingTracks.isEmpty()) return
        database.transaction {
            existingTracks.forEach { track ->
                database.textbook_syncQueries.update(
                    track.textbookId,
                    track.trackerId,
                    track.remoteId,
                    track.libraryId,
                    track.title,
                    track.lastChapterRead,
                    track.totalChapters,
                    track.status,
                    track.score,
                    track.remoteUrl,
                    track.startDate,
                    track.finishDate,
                    track.private,
                    track.id,
                )
            }
        }
    }

    private fun Track.forComparison() = this.copy(id = 0L, textbookId = 0L)

    /**
     * Restores the excluded scanlators for the manga.
     *
     * @param manga the manga whose excluded scanlators have to be restored.
     * @param excludedScanlators the excluded scanlators to restore.
     */
    private suspend fun restoreExcludedScanlators(manga: Textbook, excludedScanlators: List<String>) {
        if (excludedScanlators.isEmpty()) return
        val existingExcludedScanlators = database.excluded_scanlatorsQueries
            .getExcludedScanlatorsByTextbookId(manga.id)
            .awaitAsList()
        val toInsert = excludedScanlators.filter { it !in existingExcludedScanlators }
        if (toInsert.isEmpty()) return
        toInsert.forEach { database.excluded_scanlatorsQueries.insert(manga.id, it) }
    }
}
