package com.bookshelf.util.chapter

import com.bookshelf.data.download.DownloadCache
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.source.local.isLocal

/**
 * Returns a copy of the list with not downloaded chapters removed.
 */
fun List<Chapter>.filterDownloaded(manga: Manga, downloadCache: DownloadCache): List<Chapter> {
    if (manga.isLocal()) return this

    return filter { downloadCache.isChapterDownloaded(it.name, it.scanlator, it.url, manga.title, manga.source) }
}
