package com.bookshelf.domain.source.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.source.model.SourceWithCount
import com.bookshelf.domain.source.repository.SourceRepository

@Inject
class GetSourcesWithNonLibraryTextbook(
    private val repository: SourceRepository,
) {

    fun subscribe(): Flow<List<SourceWithCount>> {
        return repository.getSourcesWithNonLibraryTextbook()
    }
}
