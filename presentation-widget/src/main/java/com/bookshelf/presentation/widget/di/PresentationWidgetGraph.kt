package com.bookshelf.presentation.widget.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import com.bookshelf.presentation.widget.BaseUpdatesGridGlanceWidget

@ContributesTo(AppScope::class)
interface PresentationWidgetGraph {
    fun inject(widget: BaseUpdatesGridGlanceWidget)
}
