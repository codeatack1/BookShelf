package com.bookshelf.domain.extension.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.extension.repository.ExtensionStoreRepository

@Inject
class GetExtensionStoreCountAsFlow(
    private val repository: ExtensionStoreRepository,
) {
    operator fun invoke() = repository.getCountAsFlow()
}
