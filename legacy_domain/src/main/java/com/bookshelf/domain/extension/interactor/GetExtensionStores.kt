package com.bookshelf.domain.extension.interactor

import com.bookshelf.domain.extension.model.ExtensionStore
import com.bookshelf.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class GetExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    suspend fun get(): List<ExtensionStore> = repository.getAll()

    fun subscribe(): Flow<List<ExtensionStore>> = repository.getAllAsFlow()
}
