package com.bookshelf.domain.updates.repository

import com.bookshelf.domain.updates.model.UpdatesWithRelations
import kotlinx.coroutines.flow.Flow

interface UpdatesRepository {

    suspend fun awaitWithRead(read: Boolean, after: Long, limit: Long): List<UpdatesWithRelations>

    fun subscribeAll(
        after: Long,
        limit: Long,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
        includedCategories: List<Long>,
        excludedCategories: List<Long>,
    ): Flow<List<UpdatesWithRelations>>

    fun subscribeWithRead(read: Boolean, after: Long, limit: Long): Flow<List<UpdatesWithRelations>>
}
