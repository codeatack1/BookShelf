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
    private const val CONFIRM_BODY_LIMIT = 65_536

    private val GDRIVE_ID_RE = Regex("""file/d/([^/?]+)""")

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

            val (fileUrl, linkSource) = findFileLink(doc)
                ?: return@withContext "no file link on page"
            println("RemoteImport: found link via $linkSource: $fileUrl")

            var ext = extFromUrl(fileUrl)

            val dir = File(appContext.filesDir, "remote")
            dir.mkdirs()
            val tempFile = File(dir, "$bookId.download")

            try {
                val downloadedContentType = downloadFile(fileUrl, tempFile)
                println("RemoteImport: download complete, content-type=$downloadedContentType")

                if (ext == null) {
                    ext = extFromContentType(downloadedContentType)
                }
                if (ext == null) {
                    ext = extFromMagicBytes(tempFile)
                }
                if (ext == null) {
                    tempFile.delete()
                    return@withContext "unable to determine file type (no extension, unknown content-type, unknown magic bytes)"
                }

                val file = File(dir, "$bookId.$ext")
                tempFile.renameTo(file)

                val chapters = try {
                    if (ext == "pdf") PdfImporter.parse(file, bookId) else EpubImporter.parse(file, bookId)
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }
                BookRepository.replaceChapters(bookId, chapters)
                null
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        } catch (e: Exception) {
            e.message ?: "import_failed"
        }
    }

    private fun findFileLink(doc: org.jsoup.nodes.Document): Pair<String, String>? {
        // Step 1: direct <a> links inside article.post, then whole page
        val directLink = findDirectLink(doc)
        if (directLink != null) return directLink

        // Step 2: iframe with Google Drive embed
        val gdriveLink = findGdriveLink(doc)
        if (gdriveLink != null) return gdriveLink

        // Step 3: iframe with direct file src (.pdf / .epub)
        val iframeLink = findIframeFileLink(doc)
        if (iframeLink != null) return iframeLink

        return null
    }

    private fun findDirectLink(doc: org.jsoup.nodes.Document): Pair<String, String>? {
        val postArticle = doc.select("article.post").first()
        val searchRoots = if (postArticle != null) listOf(postArticle, doc) else listOf(doc)

        for (root in searchRoots) {
            val links = root.select("a[href]")
            for (a in links) {
                val href = a.attr("href").lowercase()
                if (href.endsWith(".pdf") || href.endsWith(".epub") ||
                    href.contains(".pdf?") || href.contains(".epub?")) {
                    val abs = a.absUrl("href")
                    if (abs.isNotEmpty()) return abs to "direct-link"
                }
            }
        }
        return null
    }

    private fun findGdriveLink(doc: org.jsoup.nodes.Document): Pair<String, String>? {
        val iframes = doc.select("iframe[src]")
        for (iframe in iframes) {
            val src = iframe.attr("src")
            val match = GDRIVE_ID_RE.find(src) ?: continue
            val fileId = match.groupValues[1]
            val downloadUrl = "https://drive.google.com/uc?export=download&id=$fileId"
            return downloadUrl to "gdrive-iframe"
        }
        return null
    }

    private fun findIframeFileLink(doc: org.jsoup.nodes.Document): Pair<String, String>? {
        val iframes = doc.select("iframe[src]")
        for (iframe in iframes) {
            val src = iframe.attr("src")
            val srcLower = src.lowercase()
            if (srcLower.endsWith(".pdf") || srcLower.endsWith(".epub") ||
                srcLower.contains(".pdf?") || srcLower.contains(".epub?")) {
                val baseUri = doc.baseUri().ifEmpty { SITE_URL }
                val abs = try { URL(URL(baseUri), src).toString() } catch (_: Exception) { src }
                return abs to "iframe-file"
            }
        }
        return null
    }

    private fun extFromUrl(url: String): String? {
        val path = try { URL(url).path } catch (_: Exception) { url }
        val pathLower = path.lowercase()
        if (pathLower.endsWith(".pdf")) return "pdf"
        if (pathLower.endsWith(".epub")) return "epub"
        if (pathLower.contains(".pdf?")) return "pdf"
        if (pathLower.contains(".epub?")) return "epub"
        return null
    }

    private fun extFromContentType(ct: String?): String? {
        if (ct == null) return null
        val lower = ct.lowercase()
        if (lower.contains("application/pdf")) return "pdf"
        if (lower.contains("application/epub+zip")) return "epub"
        if (lower.contains("application/epub")) return "epub"
        return null
    }

    private fun extFromMagicBytes(file: File): String? {
        val header = ByteArray(4)
        val n = file.inputStream().use { it.read(header) }
        if (n >= 4) {
            if (header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
                header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()) {
                return "pdf"
            }
            if (header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte() &&
                header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
                return "epub"
            }
        }
        return null
    }

    private fun downloadFile(fileUrl: String, destFile: File): String? {
        var conn = openConnection(fileUrl)
        try {
            if (conn.responseCode != 200) {
                throw Exception("download failed: http=${conn.responseCode}")
            }

            val contentType = conn.contentType

            // Google Drive confirmation page for large files
            if (contentType != null && contentType.lowercase().startsWith("text/html") &&
                fileUrl.contains("drive.google.com")) {
                conn.disconnect()
                conn = handleGdriveConfirm(fileUrl)
            }

            val finalContentType = conn.contentType
            conn.inputStream.use { ins ->
                destFile.outputStream().use { outs ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = ins.read(buf)
                        if (read < 0) break
                        total += read
                        if (total > MAX_FILE_BYTES) {
                            destFile.delete()
                            throw Exception("file too large")
                        }
                        outs.write(buf, 0, read)
                    }
                }
            }
            return finalContentType
        } finally {
            conn.disconnect()
        }
    }

    private fun handleGdriveConfirm(originalUrl: String): HttpURLConnection {
        val confirmConn = openConnection(originalUrl)
        if (confirmConn.responseCode != 200) {
            confirmConn.disconnect()
            throw Exception("gdrive confirm page fetch failed: http=${confirmConn.responseCode}")
        }

        val body = confirmConn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val chars = CharArray(CONFIRM_BODY_LIMIT)
            val n = reader.read(chars)
            if (n > 0) String(chars, 0, n) else ""
        }
        confirmConn.disconnect()

        val confirmMatch = Regex("""name="confirm"\s+value="([^"]+)"""").find(body)
        val idMatch = Regex("""name="id"\s+value="([^"]+)"""").find(body)

        val confirmToken = confirmMatch?.groupValues?.get(1) ?: "t"
        val idValue = idMatch?.groupValues?.get(1) ?: run {
            val idFromUrl = Regex("""id=([^&]+)""").find(originalUrl)?.groupValues?.get(1)
            idFromUrl ?: throw Exception("gdrive: could not extract id from confirm page")
        }

        val retryUrl = "https://drive.google.com/uc?export=download&id=$idValue&confirm=$confirmToken"
        println("RemoteImport: gdrive confirm retry url=$retryUrl")

        val retryConn = openConnection(retryUrl)
        if (retryConn.responseCode != 200) {
            retryConn.disconnect()
            throw Exception("gdrive download after confirm failed: http=${retryConn.responseCode}")
        }

        val retryContentType = retryConn.contentType
        if (retryContentType != null && retryContentType.lowercase().startsWith("text/html")) {
            retryConn.disconnect()
            throw Exception("gdrive file did not download (still HTML after confirm)")
        }

        return retryConn
    }

    private fun openConnection(fileUrl: String): HttpURLConnection {
        val conn = URL(fileUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.instanceFollowRedirects = true
        return conn
    }
}
