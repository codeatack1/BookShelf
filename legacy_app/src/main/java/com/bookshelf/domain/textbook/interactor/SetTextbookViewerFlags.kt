package com.bookshelf.domain.textbook.interactor

import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.repository.TextbookRepository
import com.bookshelf.ui.reader.setting.ReaderOrientation
import com.bookshelf.ui.reader.setting.ReadingMode
import dev.zacsweers.metro.Inject

@Inject
class SetTextbookViewerFlags(
    private val mangaRepository: TextbookRepository,
) {

    suspend fun awaitSetReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getTextbookById(id)
        mangaRepository.update(
            TextbookUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReadingMode.MASK.toLong()),
            ),
        )
    }

    suspend fun awaitSetOrientation(id: Long, flag: Long) {
        val manga = mangaRepository.getTextbookById(id)
        mangaRepository.update(
            TextbookUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReaderOrientation.MASK.toLong()),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
