package com.bookshelf.domain.download.interactor

import com.bookshelf.core.common.util.lang.withNonCancellableContext
import com.bookshelf.data.download.DownloadManager
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.domain.textbook.model.Textbook
import dev.zacsweers.metro.Inject

@Inject
class DeleteDownload(
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
) {

    suspend fun awaitAll(manga: Textbook, vararg chapters: Chapter) = withNonCancellableContext {
        sourceManager.get(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }
}
