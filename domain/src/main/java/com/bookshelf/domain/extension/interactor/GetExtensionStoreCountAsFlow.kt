package com.bookshelf.domain.extension.interactor

import com.bookshelf.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject

@Inject
class GetExtensionStoreCountAsFlow(
    private val repository: ExtensionStoreRepository,
) {
    operator fun invoke() = repository.getCountAsFlow()
}
