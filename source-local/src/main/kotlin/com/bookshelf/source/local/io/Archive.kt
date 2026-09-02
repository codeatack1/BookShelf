package com.bookshelf.source.local.io

import com.bookshelf.core.common.storage.extension
import com.hippo.unifile.UniFile

object Archive {

    private val SUPPORTED_ARCHIVE_TYPES = listOf("zip", "cbz", "rar", "cbr", "7z", "cb7", "tar", "cbt")

    fun isSupported(file: UniFile): Boolean {
        return file.extension?.lowercase() in SUPPORTED_ARCHIVE_TYPES
    }
}
