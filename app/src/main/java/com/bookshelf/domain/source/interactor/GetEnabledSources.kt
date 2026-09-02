package com.bookshelf.domain.source.interactor

import com.bookshelf.domain.source.model.Pin
import com.bookshelf.domain.source.model.Pins
import com.bookshelf.domain.source.model.Source
import com.bookshelf.domain.source.repository.SourceRepository
import com.bookshelf.domain.source.service.SourcePreferences
import com.bookshelf.source.local.isLocal
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Inject
class GetEnabledSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Source>> {
        return combine(
            preferences.pinnedSources.changes(),
            preferences.enabledLanguages.changes(),
            preferences.disabledSources.changes(),
            preferences.lastUsedSource.changes(),
            repository.getSources(),
        ) { pinnedSourceIds, enabledLanguages, disabledSources, lastUsedSource, sources ->
            sources
                .filter { it.lang in enabledLanguages || it.isLocal() }
                .filterNot { it.id.toString() in disabledSources }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .flatMap {
                    val flag = if ("${it.id}" in pinnedSourceIds) Pins.pinned else Pins.unpinned
                    val source = it.copy(pin = flag)
                    val toFlatten = mutableListOf(source)
                    if (source.id == lastUsedSource) {
                        toFlatten.add(source.copy(isUsedLast = true, pin = source.pin - Pin.Actual))
                    }
                    toFlatten
                }
        }
            .distinctUntilChanged()
    }
}
