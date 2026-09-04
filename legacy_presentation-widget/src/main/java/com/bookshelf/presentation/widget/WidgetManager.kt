package com.bookshelf.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.LifecycleCoroutineScope
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.core.security.SecurityPreferences
import com.bookshelf.domain.updates.interactor.GetUpdates
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority

@Inject
class WidgetManager(
    private val getUpdates: GetUpdates,
    private val securityPreferences: SecurityPreferences,
) {

    context(context: Context)
    fun init(scope: LifecycleCoroutineScope) {
        combine(
            getUpdates.subscribe(read = false, after = BaseUpdatesGridGlanceWidget.DateLimit.toEpochMilliseconds()),
            securityPreferences.useAuthenticator.changes(),
            transform = { a, b -> a to b },
        )
            .distinctUntilChanged { old, new ->
                old.second == new.second &&
                    old.first.map { it.chapterId }.toSet() == new.first.map { it.chapterId }.toSet()
            }
            .onEach {
                try {
                    UpdatesGridGlanceWidget().updateAll(context)
                    UpdatesGridCoverScreenGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    this.logcat(LogPriority.ERROR, e) { "Failed to update widget" }
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(scope)
    }
}
