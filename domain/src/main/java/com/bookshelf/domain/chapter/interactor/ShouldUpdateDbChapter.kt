package com.bookshelf.domain.chapter.interactor

import com.bookshelf.domain.chapter.model.Chapter
import dev.zacsweers.metro.Inject

@Inject
class ShouldUpdateDbChapter {

    fun await(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
        return dbChapter.scanlator != sourceChapter.scanlator ||
            dbChapter.name != sourceChapter.name ||
            dbChapter.dateUpload != sourceChapter.dateUpload ||
            dbChapter.chapterNumber != sourceChapter.chapterNumber ||
            dbChapter.sourceOrder != sourceChapter.sourceOrder ||
            dbChapter.memo != sourceChapter.memo
    }
}
