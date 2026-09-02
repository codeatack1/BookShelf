package com.bookshelf.source.online

import com.bookshelf.source.Source
import com.bookshelf.source.model.SChapter
import com.bookshelf.source.model.STextbook

/**
 * A source that may handle opening an STextbook or SChapter for a given URI.
 *
 * @since extensions-lib 1.5
 */
interface ResolvableSource : Source {

    /**
     * Returns what the given URI may open.
     * Returns [UriType.Unknown] if the source is not able to resolve the URI.
     *
     * @since extensions-lib 1.5
     */
    fun getUriType(uri: String): UriType

    /**
     * Called if [getUriType] is [UriType.Textbook].
     * Returns the corresponding STextbook, if possible.
     *
     * @since extensions-lib 1.5
     */
    suspend fun getManga(uri: String): STextbook?

    /**
     * Called if [getUriType] is [UriType.Chapter].
     * Returns the corresponding SChapter, if possible.
     *
     * @since extensions-lib 1.5
     */
    suspend fun getChapter(uri: String): SChapter?
}

sealed interface UriType {
    data object Textbook : UriType
    data object Chapter : UriType
    data object Unknown : UriType
}
