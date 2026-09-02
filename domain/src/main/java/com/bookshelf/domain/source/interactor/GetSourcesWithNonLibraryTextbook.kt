package com.bookshelf.domain.source.interactor

import com.bookshelf.domain.source.model.SourceWithCount
import com.bookshelf.domain.source.repository.SourceRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class GetSourcesWithNonLibraryTextbook(
    private val repository: SourceRepository,
) {

    fun subscribe(): Flow<List<SourceWithCount>> {
        return repository.getSourcesWithNonLibraryTextbook()
    }
}
