package com.bookshelf.domain.source.models

import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.textbook.model.Textbook

data class RemoteTextbookUpdate(
    val manga: Textbook,
    val newChapters: List<Chapter>,
)
