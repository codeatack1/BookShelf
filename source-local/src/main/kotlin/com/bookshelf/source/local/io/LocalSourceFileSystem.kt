package com.bookshelf.source.local.io

import com.bookshelf.domain.storage.service.StorageManager
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class LocalSourceFileSystem(
    private val storageManager: StorageManager,
) {

    fun getBaseDirectory(): UniFile? {
        return storageManager.getLocalSourceDirectory()
    }

    fun getFilesInBaseDirectory(): List<UniFile> {
        return getBaseDirectory()?.listFiles().orEmpty().toList()
    }

    fun getTextbookDirectory(name: String): UniFile? {
        return getBaseDirectory()
            ?.findFile(name)
            ?.takeIf { it.isDirectory }
    }

    fun getFilesInTextbookDirectory(name: String): List<UniFile> {
        return getTextbookDirectory(name)?.listFiles().orEmpty().toList()
    }
}
