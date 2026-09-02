package com.bookshelf.domain.updates.model

import com.bookshelf.domain.textbook.model.TextbookCover

data class UpdatesWithRelations(
    val textbookId: Long,
    val textbookTitle: String,
    val chapterId: Long,
    val chapterName: String,
    val scanlator: String?,
    val chapterUrl: String,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: TextbookCover,
)
