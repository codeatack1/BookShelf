package com.bookshelf.data.backup.create.creators

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.bookshelf.data.Database
import com.bookshelf.data.MemoColumnAdapter
import com.bookshelf.data.backup.create.BackupOptions
import com.bookshelf.data.backup.models.BackupChapter
import com.bookshelf.data.backup.models.BackupHistory
import com.bookshelf.data.backup.models.BackupTextbook
import com.bookshelf.data.backup.models.backupChapterMapper
import com.bookshelf.data.backup.models.backupTrackMapper
import com.bookshelf.domain.category.interactor.GetCategories
import com.bookshelf.domain.history.interactor.GetHistory
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.ui.reader.setting.ReadingMode
import dev.zacsweers.metro.Inject

@Inject
class TextbookBackupCreator(
    private val database: Database,
    private val getCategories: GetCategories,
    private val getHistory: GetHistory,
) {

    suspend operator fun invoke(mangas: List<Textbook>, options: BackupOptions): List<BackupTextbook> {
        return mangas.map {
            backupManga(it, options)
        }
    }

    private suspend fun backupManga(manga: Textbook, options: BackupOptions): BackupTextbook {
        // Entry for this manga
        val mangaObject = manga.toBackupTextbook()

        mangaObject.excludedScanlators = database.excluded_scanlatorsQueries
            .getExcludedScanlatorsByTextbookId(manga.id)
            .awaitAsList()

        if (options.chapters) {
            // Backup all the chapters
            database.chaptersQueries
                .getChaptersByTextbookId(
                    textbookId = manga.id,
                    applyScanlatorFilter = 0, // false
                    mapper = backupChapterMapper,
                )
                .awaitAsList()
                .takeUnless(List<BackupChapter>::isEmpty)
                ?.let { mangaObject.chapters = it }
        }

        if (options.categories) {
            // Backup categories for this manga
            val categoriesForManga = getCategories.await(manga.id)
            if (categoriesForManga.isNotEmpty()) {
                mangaObject.categories = categoriesForManga.map { it.order }
            }
        }

        if (options.tracking) {
            val tracks = database.textbook_syncQueries
                .getTracksByTextbookId(manga.id, backupTrackMapper)
                .awaitAsList()
            if (tracks.isNotEmpty()) {
                mangaObject.tracking = tracks
            }
        }

        if (options.history) {
            val historyByMangaId = getHistory.await(manga.id)
            if (historyByMangaId.isNotEmpty()) {
                val history = historyByMangaId.map { history ->
                    val chapter = database.chaptersQueries
                        .getChapterById(history.chapterId)
                        .awaitAsOne()
                    BackupHistory(chapter.url, history.readAt?.time ?: 0L, history.readDuration)
                }
                if (history.isNotEmpty()) {
                    mangaObject.history = history
                }
            }
        }

        return mangaObject
    }
}

private fun Textbook.toBackupTextbook() =
    BackupTextbook(
        url = this.url,
        title = this.title,
        artist = this.artist,
        author = this.author,
        description = this.description,
        genre = this.genre.orEmpty(),
        status = this.status.toInt(),
        thumbnailUrl = this.thumbnailUrl,
        favorite = this.favorite,
        source = this.source,
        dateAdded = this.dateAdded,
        viewer = (this.viewerFlags.toInt() and ReadingMode.MASK),
        viewer_flags = this.viewerFlags.toInt(),
        chapterFlags = this.chapterFlags.toInt(),
        updateStrategy = this.updateStrategy,
        lastModifiedAt = this.lastModifiedAt,
        favoriteModifiedAt = this.favoriteModifiedAt,
        version = this.version,
        notes = this.notes,
        initialized = this.initialized,
        memo = MemoColumnAdapter.encode(this.memo),
    )
