package com.bookshelf.domain.textbook.model

import com.bookshelf.source.model.STextbook

fun STextbook.toDomainTextbook(sourceId: Long): Textbook {
    return Textbook.create().copy(
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = status.toLong(),
        thumbnailUrl = thumbnail_url,
        updateStrategy = update_strategy,
        initialized = initialized,
        memo = memo,
        source = sourceId,
    )
}
