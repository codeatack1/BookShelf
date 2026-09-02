package com.bookshelf.domain.category.repository

import com.bookshelf.domain.category.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    suspend fun get(id: Long): Category?

    suspend fun getAll(): List<Category>

    fun getAllAsFlow(): Flow<List<Category>>

    suspend fun getCategoriesByTextbookId(textbookId: Long): List<Category>

    fun getCategoriesByTextbookIdAsFlow(textbookId: Long): Flow<List<Category>>

    suspend fun insert(category: Category)

    suspend fun updateName(categoryId: Long, name: String)

    suspend fun updateFlags(categoryId: Long, flags: Long)

    suspend fun updateAllFlags(flags: Long?)

    suspend fun updateAllOrders(orderedIds: List<Long>)

    suspend fun delete(categoryId: Long)
}
