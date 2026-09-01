package com.bookshelf.data.extension.model

import com.bookshelf.domain.extension.model.ExtensionStore

interface BaseNetworkExtensionStore {
    fun toExtensionStore(indexUrl: String): ExtensionStore
}
