package com.bookshelf.server

import android.content.Context
import com.bookshelf.data.BookRepository
import com.bookshelf.data.EpubImporter
import com.bookshelf.data.PdfImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object RemoteImport {

    lateinit var appContext: Context
        internal set

    private const val SITE_URL = "https://pidruchnyk.com.ua"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
    private const val CONNECT_TIMEOUT = 30_000
    private const val READ_TIMEOUT = 60_000
    private const val MAX_FILE_BYTES = 300_000_000L

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // null = успех; иначе сообщение об ошибке
    suspend fun importFromPage(bookId: String, pageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (pageUrl.startsWith("http")) pageUrl else SITE_URL + pageUrl
            val doc = try {
                Jsoup.connect(fullUrl).userAgent(USER_AGENT).get()
            } catch (e: Exception) {
                return@withContext e.message ?: "fetch page failed"
            }
            val fileUrl = doc.select("div.tilo-book a[href$=\".pdf\"]").first()?.absUrl("href")
                ?: doc.select("div.tilo-book a[href$=\".epub\"]").first()?.absUrl("href")
            if (fileUrl == null) {
                return@withContext "no file link on page"
            }
            val ext = fileUrl.substringAfterLast('.', "").lowercase()
            if (ext != "pdf" && ext != "epub") {
                return@withContext "unsupported file"
            }

            val dir = File(appContext.filesDir, "remote")
            dir.mkdirs()
            val file = File(dir, "$bookId.$ext")
            val conn = URL(fileUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("User-Agent", USER_AGENT)
            try {
                if (conn.responseCode != 200) {
                    return@withContext "download failed: http=" + conn.responseCode
                }
                conn.inputStream.use { ins ->
                    file.outputStream().use { outs ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val read = ins.read(buf)
                            if (read < 0) break
                            total += read
                            if (total > MAX_FILE_BYTES) {
                                file.delete()
                                return@withContext "file too large"
                            }
                            outs.write(buf, 0, read)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }

            val chapters = try {
                if (ext == "pdf") PdfImporter.parse(file, bookId) else EpubImporter.parse(file, bookId)
            } catch (e: Exception) {
                file.delete()
                throw e
            }
            BookRepository.replaceChapters(bookId, chapters)
            null
        } catch (e: Exception) {
            e.message ?: "import_failed"
        }
    }
}