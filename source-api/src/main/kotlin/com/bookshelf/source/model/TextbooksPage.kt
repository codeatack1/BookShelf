package com.bookshelf.source.model

class TextbooksPage(val textbooks: List<STextbook>, val hasNextPage: Boolean) {

    @Deprecated("TextbooksPage is now a regular class")
    operator fun component1(): List<STextbook> = textbooks

    @Deprecated("TextbooksPage is now a regular class")
    operator fun component2(): Boolean = hasNextPage

    @Deprecated("TextbooksPage is now a regular class")
    fun copy(
        textbooks: List<STextbook> = this.textbooks,
        hasNextPage: Boolean = this.hasNextPage,
    ): TextbooksPage = TextbooksPage(
        textbooks = textbooks,
        hasNextPage = hasNextPage,
    )
}