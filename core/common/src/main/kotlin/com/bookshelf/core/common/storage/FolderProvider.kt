package com.bookshelf.core.common.storage

import java.io.File

interface FolderProvider {

    fun directory(): File

    fun path(): String
}
