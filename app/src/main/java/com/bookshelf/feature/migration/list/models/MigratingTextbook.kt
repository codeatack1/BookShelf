package com.bookshelf.feature.migration.list.models

import com.bookshelf.domain.textbook.model.Textbook
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

class MigratingTextbook(
    val manga: Textbook,
    val chapterCount: Int,
    val latestChapter: Double?,
    val source: String,
    parentContext: CoroutineContext,
) {
    val migrationScope = CoroutineScope(parentContext + SupervisorJob() + Dispatchers.Default)

    val searchResult = MutableStateFlow<SearchResult>(SearchResult.Searching)

    sealed interface SearchResult {
        data object Searching : SearchResult
        data object NotFound : SearchResult
        data class Success(
            val manga: Textbook,
            val chapterCount: Int,
            val latestChapter: Double?,
            val source: String,
        ) : SearchResult
    }
}
