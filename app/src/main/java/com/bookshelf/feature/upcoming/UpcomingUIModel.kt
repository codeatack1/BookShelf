package com.bookshelf.feature.upcoming

import kotlinx.datetime.LocalDate
import com.bookshelf.domain.textbook.model.Textbook

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val mangaCount: Int) : UpcomingUIModel
    data class Item(val manga: Textbook) : UpcomingUIModel
}
