package com.bookshelf.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppStateDao {
    @Query("SELECT value FROM app_state WHERE id = :id")
    suspend fun getValue(id: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: AppStateEntity)

    @Query("SELECT value FROM app_state WHERE id = :id")
    fun observeValue(id: String): Flow<String?>
}
