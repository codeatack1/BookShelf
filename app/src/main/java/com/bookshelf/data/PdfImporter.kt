package com.bookshelf.data

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object PdfImporter {

    private const val MAX_PAGES = 600

    fun parse(file: File, bookId: String): List<ChapterEntity> {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(fd).use { renderer ->
            if (renderer.pageCount == 0) throw IllegalArgumentException("pdf has no pages")
            if (renderer.pageCount > MAX_PAGES) {
                throw IllegalArgumentException("pdf too large: ${renderer.pageCount} pages")
            }

            val chapters = ArrayList<ChapterEntity>(renderer.pageCount)
            for (pageIndex in 0 until renderer.pageCount) {
                renderer.openPage(pageIndex).use { page ->
                    val scale = minOf(1f, 1400f / page.width)
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)

                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    bmp.recycle()

                    val data = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    val number = pageIndex + 1

                    chapters.add(
                        ChapterEntity(
                            id = "$bookId-ch$number",
                            bookId = bookId,
                            number = number,
                            name = "Стр. $number",
                            content = "data:image/jpeg;base64,$data",
                            contentType = "image",
                            read = false,
                        )
                    )
                }
            }

            if (chapters.isEmpty()) throw IllegalArgumentException("no pages rendered in pdf")
            return chapters
        }
    }
}
