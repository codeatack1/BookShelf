package com.bookshelf.domain.textbook.model

data class TextbookWithChapterCount(
    val textbook: Textbook,
    val chapterCount: Long,
)
