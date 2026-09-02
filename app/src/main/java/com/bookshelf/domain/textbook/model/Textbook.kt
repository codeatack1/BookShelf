package com.bookshelf.domain.textbook.model

import android.content.Context
import com.bookshelf.app.di.appGraph
import com.bookshelf.core.common.preference.TriState
import com.bookshelf.core.metadata.comicinfo.ComicInfo
import com.bookshelf.core.metadata.comicinfo.ComicInfoPublishingStatus
import com.bookshelf.data.cache.CoverCache
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.source.model.STextbook
import com.bookshelf.ui.reader.setting.ReaderOrientation
import com.bookshelf.ui.reader.setting.ReadingMode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// TODO: move these into the domain model
val Textbook.readingMode: Long
    get() = viewerFlags and ReadingMode.MASK.toLong()

val Textbook.readerOrientation: Long
    get() = viewerFlags and ReaderOrientation.MASK.toLong()

val Textbook.downloadedFilter: TriState
    get() {
        if (Injekt.get<Context>().appGraph.basePreferences.downloadedOnly.get()) return TriState.ENABLED_IS
        return when (downloadedFilterRaw) {
            Textbook.CHAPTER_SHOW_DOWNLOADED -> TriState.ENABLED_IS
            Textbook.CHAPTER_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }
    }
fun Textbook.textbooksFiltered(): Boolean {
    return unreadFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED
}

fun Textbook.toSTextbook(): STextbook = STextbook.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre.orEmpty().joinToString()
    it.status = status.toInt()
    it.thumbnail_url = thumbnailUrl
    it.initialized = initialized
    it.memo = memo
}

fun Textbook.copyFrom(other: STextbook): Textbook {
    val author = other.author ?: author
    val artist = other.artist ?: artist
    val description = other.description ?: description
    val genres = if (other.genre != null) {
        other.getGenres()
    } else {
        genre
    }
    val thumbnailUrl = other.thumbnail_url ?: thumbnailUrl
    return this.copy(
        author = author,
        artist = artist,
        description = description,
        genre = genres,
        thumbnailUrl = thumbnailUrl,
        status = other.status.toLong(),
        updateStrategy = other.update_strategy,
        initialized = other.initialized && initialized,
        memo = other.memo,
    )
}

fun Textbook.hasCustomCover(coverCache: CoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}

/**
 * Creates a ComicInfo instance based on the manga and chapter metadata.
 */
fun getComicInfo(
    manga: Textbook,
    chapter: Chapter,
    urls: List<String>,
    categories: List<String>?,
    sourceName: String,
) = ComicInfo(
    title = ComicInfo.Title(chapter.name),
    series = ComicInfo.Series(manga.title),
    number = chapter.chapterNumber.takeIf { it >= 0 }?.let {
        if ((it.rem(1) == 0.0)) {
            ComicInfo.Number(it.toInt().toString())
        } else {
            ComicInfo.Number(it.toString())
        }
    },
    web = ComicInfo.Web(urls.joinToString(" ")),
    summary = manga.description?.let { ComicInfo.Summary(it) },
    writer = manga.author?.let { ComicInfo.Writer(it) },
    penciller = manga.artist?.let { ComicInfo.Penciller(it) },
    translator = chapter.scanlator?.let { ComicInfo.Translator(it) },
    genre = manga.genre?.let { ComicInfo.Genre(it.joinToString()) },
    publishingStatus = ComicInfo.PublishingStatusTachiyomi(
        ComicInfoPublishingStatus.toComicInfoValue(manga.status),
    ),
    categories = categories?.let { ComicInfo.CategoriesTachiyomi(it.joinToString()) },
    source = ComicInfo.SourceMihon(sourceName),
    inker = null,
    colorist = null,
    letterer = null,
    coverArtist = null,
    tags = null,
)
