package com.bookshelf.util

import com.bookshelf.domain.textbook.interactor.UpdateTextbook
import com.bookshelf.domain.textbook.model.toSTextbook
import com.bookshelf.data.cache.CoverCache
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.source.local.image.LocalCoverManager
import com.bookshelf.source.local.isLocal
import java.io.InputStream
import kotlin.time.Clock

fun Textbook.removeCovers(coverCache: CoverCache): Textbook {
    if (isLocal()) return this
    return if (coverCache.deleteFromCache(this, true) > 0) {
        copy(coverLastModified = Clock.System.now().toEpochMilliseconds())
    } else {
        this
    }
}

suspend fun Textbook.editCover(
    coverManager: LocalCoverManager,
    stream: InputStream,
    updateManga: UpdateTextbook,
    coverCache: CoverCache,
) {
    if (isLocal()) {
        coverManager.update(toSTextbook(), stream)
        updateManga.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(this, stream)
        updateManga.awaitUpdateCoverLastModified(id)
    }
}
