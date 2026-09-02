package com.bookshelf.domain.history.repository

import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.history.model.History
import com.bookshelf.domain.history.model.HistoryUpdate
import com.bookshelf.domain.history.model.HistoryWithRelations

interface HistoryRepository {

    fun getHistory(query: String): Flow<List<HistoryWithRelations>>

    suspend fun getLastHistory(): HistoryWithRelations?

    suspend fun getTotalReadDuration(): Long

    suspend fun getHistoryByTextbookId(textbookId: Long): List<History>

    suspend fun resetHistory(historyId: Long)

    suspend fun resetHistoryByTextbookId(textbookId: Long)

    suspend fun deleteAllHistory(): Boolean

    suspend fun upsertHistory(historyUpdate: HistoryUpdate)
}
