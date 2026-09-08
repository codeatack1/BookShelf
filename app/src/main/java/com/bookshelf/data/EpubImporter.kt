package com.bookshelf.data

import android.util.Base64
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object EpubImporter {

    private const val DC_NS = "http://purl.org/dc/elements/1.1/"
    private const val MAX_IMAGE_BYTES = 2_000_000
    private const val MAX_TOTAL_CONTENT = 30_000_000

    private val imgSrcRegex =
        Regex("""(<img\b[^>]*\bsrc\s*=\s*["'])([^"']*)(["'])""", RegexOption.IGNORE_CASE)
    private val imageXlinkRegex =
        Regex("""(<image\b[^>]*\bxlink:href\s*=\s*["'])([^"']*)(["'])""", RegexOption.IGNORE_CASE)
    private val titleRegex =
        Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE)

    fun parse(file: File, bookId: String): List<ChapterEntity> {
        ZipFile(file).use { zip ->
            val entries = HashMap<String, ZipEntry>()
            val names = zip.entries()
            while (names.hasMoreElements()) {
                val entry = names.nextElement()
                if (!entry.isDirectory) entries[normalizeName(entry.name)] = entry
            }

            val opfPath = findOpfPath(zip, entries)
            val opfEntry = entries[opfPath] ?: throw IllegalArgumentException("opf entry not found: $opfPath")
            val opf = parseOpf(zip, opfEntry)
            val dir = parentDir(opfPath)

            val chapters = ArrayList<ChapterEntity>()
            var totalContent = 0L
            for ((index, idref) in opf.spine.withIndex()) {
                val item = opf.manifest[idref] ?: continue
                if (!isChapter(item.mediaType, item.href)) continue
                val href2 = resolve(dir, item.href)
                val entry = entries[href2] ?: continue
                val raw = readEntryText(zip, entry)
                val content = if (totalContent <= MAX_TOTAL_CONTENT.toLong()) {
                    inlineImages(zip, raw, parentDir(href2), entries)
                } else {
                    raw
                }
                totalContent += content.length

                val number = index + 1
                val name = extractTitle(content).ifEmpty { "Глава $number" }
                chapters.add(
                    ChapterEntity(
                        id = "$bookId-ch$number",
                        bookId = bookId,
                        number = number,
                        name = name,
                        content = content,
                        contentType = "html",
                        read = false,
                    )
                )
            }

            if (chapters.isEmpty()) throw IllegalArgumentException("no readable chapters in epub")
            return chapters
        }
    }

    private fun findOpfPath(zip: ZipFile, entries: Map<String, ZipEntry>): String {
        val container = entries["META-INF/container.xml"]
            ?: entries.keys.firstOrNull { it.equals("META-INF/container.xml", ignoreCase = true) }?.let { entries[it] }
            ?: throw IllegalArgumentException("container.xml not found in epub")
        return parseContainer(zip, container)
    }

    private fun parseContainer(zip: ZipFile, entry: ZipEntry): String {
        zip.getInputStream(entry).use { input ->
            val parser = newParser(input)
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    val fullPath = parser.getAttributeValue(null, "full-path")
                    if (!fullPath.isNullOrBlank()) return normalizeName(fullPath)
                }
            }
        }
        throw IllegalArgumentException("rootfile with full-path not found in container.xml")
    }

    private fun parseOpf(zip: ZipFile, entry: ZipEntry): OpfData {
        val manifest = LinkedHashMap<String, ManifestItem>()
        val spine = ArrayList<String>()
        var title: String? = null
        zip.getInputStream(entry).use { input ->
            val parser = newParser(input)
            var inMetadata = false
            var inManifest = false
            var inSpine = false
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "metadata" -> inMetadata = true
                            "manifest" -> inManifest = true
                            "spine" -> inSpine = true
                            "title" -> {
                                if (inMetadata && parser.namespace == DC_NS) {
                                    title = try {
                                        parser.nextText()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            "item" -> {
                                if (inManifest) {
                                    val id = parser.getAttributeValue(null, "id")
                                    val href = parser.getAttributeValue(null, "href")
                                    if (id != null && href != null) {
                                        manifest[id] = ManifestItem(href.trim(), parser.getAttributeValue(null, "media-type"))
                                    }
                                }
                            }
                            "itemref" -> {
                                if (inSpine) {
                                    val idref = parser.getAttributeValue(null, "idref")
                                    if (idref != null) spine.add(idref)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "metadata" -> inMetadata = false
                            "manifest" -> inManifest = false
                            "spine" -> inSpine = false
                        }
                    }
                }
            }
        }
        return OpfData(title, manifest, spine)
    }

    private fun isChapter(mediaType: String?, href: String): Boolean {
        if (mediaType != null) {
            val mt = mediaType.lowercase()
            return mt.endsWith("xhtml") || mt.endsWith("html") || mt.contains("html")
        }
        val h = href.lowercase()
        return h.endsWith(".xhtml") || h.endsWith(".html") || h.endsWith(".htm")
    }

    private fun readEntryText(zip: ZipFile, entry: ZipEntry): String {
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        val offset =
            if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3 else 0
        return String(bytes, offset, bytes.size - offset, Charsets.UTF_8)
    }

    private fun extractTitle(html: String): String {
        val match = titleRegex.find(html) ?: return ""
        var title = match.groupValues[1].trim()
        if (title.length > 200) title = title.substring(0, 200)
        return title
    }

    private fun inlineImages(
        zip: ZipFile,
        content: String,
        htmlDir: String,
        entries: Map<String, ZipEntry>
    ): String {
        var result = imgSrcRegex.replace(content) { m ->
            val data = buildDataUri(zip, m.groupValues[2], htmlDir, entries)
            if (data != null) m.groupValues[1] + data + m.groupValues[3] else m.value
        }
        result = imageXlinkRegex.replace(result) { m ->
            val data = buildDataUri(zip, m.groupValues[2], htmlDir, entries)
            if (data != null) m.groupValues[1] + data + m.groupValues[3] else m.value
        }
        return result
    }

    private fun buildDataUri(
        zip: ZipFile,
        ref: String,
        htmlDir: String,
        entries: Map<String, ZipEntry>
    ): String? {
        val path = resolve(htmlDir, ref)
        val entry = entries[path] ?: return null
        if (entry.size > MAX_IMAGE_BYTES.toLong()) return null
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        if (bytes.size > MAX_IMAGE_BYTES) return null
        val mime = mimeFor(extensionOf(path))
        return "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun mimeFor(extension: String): String = when (extension) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "svg" -> "image/svg+xml"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }

    private fun extensionOf(path: String): String {
        val name = path.substringAfterLast('/')
        val idx = name.lastIndexOf('.')
        return if (idx < 0) "" else name.substring(idx + 1).lowercase()
    }

    private fun parentDir(path: String): String {
        val idx = path.lastIndexOf('/')
        return if (idx < 0) "" else path.substring(0, idx)
    }

    private fun normalizeName(name: String): String =
        resolve("", name.replace('\\', '/').trimStart('/'))

    private fun resolve(baseDir: String, href: String): String {
        val normalizedHref = href.replace('\\', '/')
        val combined =
            if (normalizedHref.startsWith("/")) normalizedHref
            else if (baseDir.isEmpty()) normalizedHref
            else "$baseDir/$normalizedHref"
        val stack = ArrayList<String>()
        for (part in combined.split('/')) {
            when (part) {
                "", "." -> {}
                ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                else -> stack.add(part)
            }
        }
        return stack.joinToString("/")
    }

    private fun newParser(input: InputStream): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(input, "UTF-8")
        return parser
    }

    private data class ManifestItem(val href: String, val mediaType: String?)

    private data class OpfData(
        val title: String?,
        val manifest: Map<String, ManifestItem>,
        val spine: List<String>,
    )
}