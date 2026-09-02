package com.bookshelf.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.repository.ChapterRepository

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
