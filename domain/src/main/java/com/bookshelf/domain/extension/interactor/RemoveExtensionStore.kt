package com.bookshelf.domain.extension.interactor

import com.bookshelf.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject

@Inject
class RemoveExtensionStore(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke(indexUrl: String) {
        repository.remove(indexUrl)
    }
}
