package com.bookshelf.ui.reader.viewer

import com.bookshelf.data.database.models.toDomainChapter
import com.bookshelf.domain.chapter.service.calculateChapterGap as domainCalculateChapterGap
import com.bookshelf.ui.reader.model.ReaderChapter

fun calculateChapterGap(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Int {
    return domainCalculateChapterGap(
        higherReaderChapter?.chapter?.toDomainChapter(),
        lowerReaderChapter?.chapter?.toDomainChapter(),
    )
}
