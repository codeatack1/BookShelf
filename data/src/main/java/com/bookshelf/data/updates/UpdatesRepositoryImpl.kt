package com.bookshelf.data.updates

import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import com.bookshelf.core.common.util.lang.toLong
import com.bookshelf.data.Database
import com.bookshelf.data.subscribeToList
import com.bookshelf.domain.textbook.model.TextbookCover
import com.bookshelf.domain.updates.model.UpdatesWithRelations
import com.bookshelf.domain.updates.repository.UpdatesRepository

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class UpdatesRepositoryImpl(
    private val database: Database,
) : UpdatesRepository {

    override suspend fun awaitWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return database.updatesTextViewQueries
            .getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
            .awaitAsList()
    }

    override fun subscribeAll(
        after: Long,
        limit: Long,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
        includedCategories: List<Long>,
        excludedCategories: List<Long>,
    ): Flow<List<UpdatesWithRelations>> {
        return database.updatesTextViewQueries
            .getRecentUpdatesWithFilters(
                after = after,
                limit = limit,
                read = unread?.let { !it },
                started = started?.toLong(),
                bookmarked = bookmarked,
                hideExcludedScanlators = hideExcludedScanlators.toLong(),
                includedEmpty = includedCategories.isEmpty(),
                excludedEmpty = excludedCategories.isEmpty(),
                includedCategories = includedCategories,
                excludedCategories = excludedCategories,
                mapper = ::mapUpdatesWithRelations,
            )
            .subscribeToList()
    }

    override fun subscribeWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return database.updatesTextViewQueries
            .getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
            .subscribeToList()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun mapUpdatesWithRelations(
        textbookId: Long,
        textbookTitle: String,
        chapterId: Long,
        chapterName: String,
        scanlator: String?,
        chapterUrl: String,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        sourceId: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
        excludedScanlator: String?,
    ): UpdatesWithRelations = UpdatesWithRelations(
        textbookId = textbookId,
        textbookTitle = textbookTitle,
        chapterId = chapterId,
        chapterName = chapterName,
        scanlator = scanlator,
        chapterUrl = chapterUrl,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = TextbookCover(
            textbookId = textbookId,
            sourceId = sourceId,
            isFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
