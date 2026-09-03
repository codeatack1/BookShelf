package com.bookshelf.data.coil

import coil3.key.Keyer
import coil3.request.Options
import com.bookshelf.data.cache.CoverCache
import com.bookshelf.domain.textbook.model.TextbookCover
import com.bookshelf.domain.textbook.model.hasCustomCover
import com.bookshelf.domain.textbook.model.Textbook as DomainTextbook

class TextbookKeyer : Keyer<DomainTextbook> {
    override fun key(data: DomainTextbook, options: Options): String {
        return if (data.hasCustomCover()) {
            "${data.id};${data.coverLastModified}"
        } else {
            "${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class TextbookCoverKeyer(
    private val coverCache: CoverCache,
) : Keyer<TextbookCover> {
    override fun key(data: TextbookCover, options: Options): String {
        return if (coverCache.getCustomCoverFile(data.textbookId).exists()) {
            "${data.textbookId};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}
