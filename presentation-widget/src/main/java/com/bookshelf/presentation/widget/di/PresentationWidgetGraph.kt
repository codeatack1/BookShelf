package com.bookshelf.presentation.widget.di

import com.bookshelf.presentation.widget.BaseUpdatesGridGlanceWidget
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
interface PresentationWidgetGraph {
    fun inject(widget: BaseUpdatesGridGlanceWidget)
}
