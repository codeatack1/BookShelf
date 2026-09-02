package com.bookshelf.domain.source.interactor

import com.bookshelf.core.common.preference.getAndSet
import com.bookshelf.domain.source.service.SourcePreferences
import dev.zacsweers.metro.Inject

@Inject
class ToggleLanguage(
    val preferences: SourcePreferences,
) {

    fun await(language: String) {
        val isEnabled = language in preferences.enabledLanguages.get()
        preferences.enabledLanguages.getAndSet { enabled ->
            if (isEnabled) enabled.minus(language) else enabled.plus(language)
        }
    }
}
