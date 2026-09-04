package com.bookshelf.feature.upcoming

import com.bookshelf.domain.textbook.model.Textbook
import kotlinx.datetime.LocalDate

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val mangaCount: Int) : UpcomingUIModel
    data class Item(val manga: Textbook) : UpcomingUIModel
}
