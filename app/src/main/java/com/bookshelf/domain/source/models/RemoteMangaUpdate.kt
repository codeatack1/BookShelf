package com.bookshelf.domain.source.models

import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.manga.model.Manga

data class RemoteMangaUpdate(
    val manga: Manga,
    val newChapters: List<Chapter>,
)
