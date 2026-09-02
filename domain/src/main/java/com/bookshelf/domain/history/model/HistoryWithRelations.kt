package com.bookshelf.domain.history.model

import com.bookshelf.domain.textbook.model.TextbookCover
import java.util.Date

data class HistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val textbookId: Long,
    val title: String,
    val chapterNumber: Double,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: TextbookCover,
)
