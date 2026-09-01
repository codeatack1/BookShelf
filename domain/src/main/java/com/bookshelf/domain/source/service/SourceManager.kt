package com.bookshelf.domain.source.service

import com.bookshelf.source.Source
import com.bookshelf.source.online.HttpSource
import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.source.model.StubSource

interface SourceManager {

    val sources: Flow<List<Source>>

    suspend fun get(sourceKey: Long): Source?

    suspend fun getOrStub(sourceKey: Long): Source

    suspend fun getAll(): List<Source>

    suspend fun getOnlineSources(): List<HttpSource>

    suspend fun getStubSources(): List<StubSource>
}
