package com.bookshelf.util.chapter

import com.bookshelf.data.download.DownloadManager
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.model.applyFilters
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.ui.textbook.ChapterList

/**
 * Gets next unread chapter with filters and sorting applied
 */
fun List<Chapter>.getNextUnread(manga: Textbook, downloadManager: DownloadManager): Chapter? {
    return applyFilters(manga, downloadManager).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.read }
        } else {
            chapters.find { !it.read }
        }
    }
}

/**
 * Gets next unread chapter with filters and sorting applied
 */
fun List<ChapterList.Item>.getNextUnread(manga: Textbook): Chapter? {
    return applyFilters(manga).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.chapter.read }
        } else {
            chapters.find { !it.chapter.read }
        }
    }?.chapter
}
