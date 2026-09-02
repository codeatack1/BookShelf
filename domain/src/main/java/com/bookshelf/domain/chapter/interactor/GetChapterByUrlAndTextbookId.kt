package com.bookshelf.domain.chapter.interactor

import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject

@Inject
class GetChapterByUrlAndTextbookId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(url: String, sourceId: Long): Chapter? {
        return try {
            chapterRepository.getChapterByUrlAndTextbookId(url, sourceId)
        } catch (e: Exception) {
            null
        }
    }
}
