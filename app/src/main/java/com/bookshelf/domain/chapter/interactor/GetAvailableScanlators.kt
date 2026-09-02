package com.bookshelf.domain.chapter.interactor

import com.bookshelf.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class GetAvailableScanlators(
    private val repository: ChapterRepository,
) {

    private fun List<String>.cleanupAvailableScanlators(): Set<String> {
        return mapNotNull { it.ifBlank { null } }.toSet()
    }

    suspend fun await(textbookId: Long): Set<String> {
        return repository.getScanlatorsByTextbookId(textbookId)
            .cleanupAvailableScanlators()
    }

    fun subscribe(textbookId: Long): Flow<Set<String>> {
        return repository.getScanlatorsByTextbookIdAsFlow(textbookId)
            .map { it.cleanupAvailableScanlators() }
    }
}
