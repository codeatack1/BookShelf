package com.bookshelf.domain.extension.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.source.service.SourcePreferences
import com.bookshelf.extension.ExtensionManager
import com.bookshelf.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
class GetExtensionLanguages(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {
    fun subscribe(): Flow<List<String>> {
        return combine(
            preferences.enabledLanguages.changes(),
            extensionManager.availableExtensionsFlow,
        ) { enabledLanguage, availableExtensions ->
            availableExtensions
                .flatMap { ext ->
                    ext.sources.map { it.lang }
                }
                .distinct()
                .sortedWith(
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }
    }
}
