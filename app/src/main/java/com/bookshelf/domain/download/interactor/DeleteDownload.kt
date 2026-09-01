package com.bookshelf.domain.download.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.com.bookshelf.data.download.DownloadManager
import com.bookshelf.core.common.util.lang.withNonCancellableContext
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.domain.source.service.SourceManager

@Inject
class DeleteDownload(
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
) {

    suspend fun awaitAll(manga: Manga, vararg chapters: Chapter) = withNonCancellableContext {
        sourceManager.get(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }
}
