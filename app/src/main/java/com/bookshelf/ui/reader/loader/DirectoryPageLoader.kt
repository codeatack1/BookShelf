package com.bookshelf.ui.reader.loader

import com.bookshelf.core.common.util.system.ImageUtil
import com.bookshelf.source.model.Page
import com.bookshelf.ui.reader.model.ReaderPage
import com.bookshelf.util.lang.compareToCaseInsensitiveNaturalOrder
import com.hippo.unifile.UniFile

/**
 * Loader used to load a chapter from a directory given on [file].
 */
internal class DirectoryPageLoader(val file: UniFile) : PageLoader() {

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        return file.listFiles()
            ?.filter { !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() } }
            ?.sortedWith { f1, f2 -> f1.name.orEmpty().compareToCaseInsensitiveNaturalOrder(f2.name.orEmpty()) }
            ?.mapIndexed { i, file ->
                val streamFn = { file.openInputStream() }
                ReaderPage(i).apply {
                    stream = streamFn
                    status = Page.State.Ready
                }
            }
            .orEmpty()
    }
}
