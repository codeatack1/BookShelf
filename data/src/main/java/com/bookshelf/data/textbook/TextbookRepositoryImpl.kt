package com.bookshelf.data.textbook

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.data.Database
import com.bookshelf.data.MemoColumnAdapter
import com.bookshelf.data.StringListColumnAdapter
import com.bookshelf.data.UpdateStrategyColumnAdapter
import com.bookshelf.data.subscribeToList
import com.bookshelf.data.subscribeToOne
import com.bookshelf.data.subscribeToOneOrNull
import com.bookshelf.domain.library.model.LibraryTextbook
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.model.TextbookWithChapterCount
import com.bookshelf.domain.textbook.repository.TextbookRepository
import kotlin.time.Clock

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TextbookRepositoryImpl(
    private val database: Database,
) : TextbookRepository {

    override suspend fun getTextbookById(id: Long): Textbook {
        return database.textbooksQueries
            .getTextbookById(id, TextbookMapper::mapManga)
            .awaitAsOne()
    }

    override fun getTextbookByIdAsFlow(id: Long): Flow<Textbook> {
        return database.textbooksQueries
            .getTextbookById(id, TextbookMapper::mapManga)
            .subscribeToOne()
    }

    override suspend fun getTextbookByUrlAndSourceId(url: String, sourceId: Long): Textbook? {
        return database.textbooksQueries
            .getTextbookByUrlAndSource(url, sourceId, TextbookMapper::mapManga)
            .awaitAsOneOrNull()
    }

    override fun getTextbookByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Textbook?> {
        return database.textbooksQueries
            .getTextbookByUrlAndSource(url, sourceId, TextbookMapper::mapManga)
            .subscribeToOneOrNull()
    }

    override suspend fun getFavorites(): List<Textbook> {
        return database.textbooksQueries
            .getFavoriteTextbooks(TextbookMapper::mapManga)
            .awaitAsList()
    }

    override suspend fun getReadMangaNotInLibrary(): List<Textbook> {
        return database.textbooksQueries
            .getReadTextbooksNotInLibrary(TextbookMapper::mapManga)
            .awaitAsList()
    }

    override suspend fun getLibraryTextbook(): List<LibraryTextbook> {
        return database.libraryTextViewQueries
            .library(TextbookMapper::mapLibraryTextbook)
            .awaitAsList()
    }

    override fun getLibraryTextbookAsFlow(): Flow<List<LibraryTextbook>> {
        return database.libraryTextViewQueries
            .library(TextbookMapper::mapLibraryTextbook)
            .subscribeToList()
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Textbook>> {
        return database.textbooksQueries
            .getFavoriteTextbooksBySourceId(sourceId, TextbookMapper::mapManga)
            .subscribeToList()
    }

    override suspend fun getDuplicateLibraryTextbook(id: Long, title: String): List<TextbookWithChapterCount> {
        return database.textbooksQueries
            .getDuplicateLibraryTextbook(id, title, TextbookMapper::mapTextbookWithChapterCount)
            .awaitAsList()
    }

    override suspend fun getUpcomingTextbooks(
        statuses: Set<Long>,
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
    ): Flow<List<Textbook>> {
        val timeZone = TimeZone.currentSystemDefault()
        val epochMillis =
            Clock.System.now().toLocalDateTime(timeZone).date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        return database.textbooksQueries
            .getUpcomingTextbooks(
                startOfDay = epochMillis,
                statuses = statuses,
                includedEmpty = includedCategories.isEmpty(),
                includedCategories = includedCategories,
                excludedEmpty = excludedCategories.isEmpty(),
                excludedCategories = excludedCategories,
                mapper = TextbookMapper::mapManga,
            )
            .subscribeToList()
    }

    override suspend fun resetViewerFlags(): Boolean {
        return try {
            database.textbooksQueries.resetViewerFlags()
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun setTextbookCategories(textbookId: Long, categoryIds: List<Long>) {
        database.transaction {
            database.textbooks_categoriesQueries.deleteTextbookCategoryByTextbookId(textbookId)
            categoryIds.forEach { categoryId ->
                database.textbooks_categoriesQueries.insert(textbookId, categoryId)
            }
        }
    }

    override suspend fun update(update: TextbookUpdate): Boolean {
        return try {
            partialUpdate(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAll(mangaUpdates: List<TextbookUpdate>): Boolean {
        return try {
            partialUpdate(*mangaUpdates.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun insertNetworkTextbook(manga: List<Textbook>): List<Textbook> {
        return database.transactionWithResult {
            manga.map {
                database.textbooksQueries.insertNetworkTextbook(
                    source = it.source,
                    url = it.url,
                    artist = it.artist,
                    author = it.author,
                    description = it.description,
                    genre = it.genre,
                    title = it.title,
                    status = it.status,
                    thumbnailUrl = it.thumbnailUrl,
                    favorite = it.favorite,
                    lastUpdate = it.lastUpdate,
                    nextUpdate = it.nextUpdate,
                    calculateInterval = it.fetchInterval.toLong(),
                    initialized = it.initialized,
                    viewerFlags = it.viewerFlags,
                    chapterFlags = it.chapterFlags,
                    coverLastModified = it.coverLastModified,
                    dateAdded = it.dateAdded,
                    updateStrategy = it.updateStrategy,
                    version = it.version,
                    memo = it.memo,
                    updateTitle = it.title.isNotBlank(),
                    updateCover = !it.thumbnailUrl.isNullOrBlank(),
                    updateDetails = it.initialized,
                    mapper = TextbookMapper::mapManga,
                )
                    .awaitAsOne()
            }
        }
    }

    private suspend fun partialUpdate(vararg mangaUpdates: TextbookUpdate) {
        database.transaction {
            mangaUpdates.forEach { value ->
                database.textbooksQueries.update(
                    source = value.source,
                    url = value.url,
                    artist = value.artist,
                    author = value.author,
                    description = value.description,
                    genre = value.genre?.let(StringListColumnAdapter::encode),
                    title = value.title,
                    status = value.status,
                    thumbnailUrl = value.thumbnailUrl,
                    favorite = value.favorite,
                    lastUpdate = value.lastUpdate,
                    nextUpdate = value.nextUpdate,
                    calculateInterval = value.fetchInterval?.toLong(),
                    initialized = value.initialized,
                    viewer = value.viewerFlags,
                    chapterFlags = value.chapterFlags,
                    coverLastModified = value.coverLastModified,
                    dateAdded = value.dateAdded,
                    textbookId = value.id,
                    updateStrategy = value.updateStrategy?.let(UpdateStrategyColumnAdapter::encode),
                    version = value.version,
                    isSyncing = 0,
                    notes = value.notes,
                    memo = value.memo?.let(MemoColumnAdapter::encode),
                )
            }
        }
    }
}
