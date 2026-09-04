package com.bookshelf.ui.reader.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.util.LruCache
import com.bookshelf.source.model.Page
import com.bookshelf.ui.reader.model.ReaderPage
import com.hippo.unifile.UniFile
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfPageLoader(
    private val file: UniFile,
    private val context: Context,
) : PageLoader() {

    override var isLocal: Boolean = true

    private var renderer: PdfRenderer? = null

    // LRU cache: page index -> PNG bytes
    private val pageCache = object : LruCache<Int, ByteArray>(20) {
        override fun sizeOf(key: Int, value: ByteArray) = 1
    }

    override suspend fun getPages(): List<ReaderPage> = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(file.uri, "r")
            ?: throw IllegalStateException("Cannot open PDF: ${file.name}")

        renderer = PdfRenderer(pfd).also {
            (0 until it.pageCount).map { index ->
                ReaderPage(index).apply {
                    stream = { renderPage(index) }
                    status = Page.State.Ready
                }
            }
        } ?: throw IllegalStateException("Failed to open PDF renderer")
    }

    private fun renderPage(pageIndex: Int): java.io.InputStream {
        // Check cache first
        pageCache.get(pageIndex)?.let {
            return it.inputStream()
        }

        val pdf = renderer ?: throw IllegalStateException("PDF renderer is closed")
        val page = pdf.openPage(pageIndex)

        // Scale: PdfRenderer uses 72 DPI natively, multiply by 2 for reasonable mobile quality
        val scale = 2
        val bitmap = Bitmap.createBitmap(
            page.width * scale,
            page.height * scale,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        // Encode to PNG bytes
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        bitmap.recycle()

        val bytes = baos.toByteArray()
        pageCache.put(pageIndex, bytes)
        return bytes.inputStream()
    }

    override fun recycle() {
        super.recycle()
        renderer?.close()
        renderer = null
        pageCache.evictAll()
    }
}
