package com.bookshelf.domain.textbook.model

/**
 * Contains the required data for TextbookCoverFetcher
 */
data class TextbookCover(
    val textbookId: Long,
    val sourceId: Long,
    val isFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
)

fun Textbook.asTextbookCover(): TextbookCover {
    return TextbookCover(
        textbookId = id,
        sourceId = source,
        isFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
