package com.bookshelf.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

object AppStorage {

    const val KEY_BACKGROUND = "background"

    private lateinit var db: AppDatabase

    @Volatile
    var cachedBackground: Int? = null

    fun init(context: Context) {
        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "bookshelf.db",
        ).build()
    }

    suspend fun getString(key: String): String? =
        db.appStateDao().getValue(key)

    suspend fun putString(key: String, value: String) {
        db.appStateDao().put(AppStateEntity(id = key, value = value))
        if (key == KEY_BACKGROUND) {
            cachedBackground = value.toIntOrNull()
        }
    }

    fun observe(key: String): Flow<String?> =
        db.appStateDao().observeValue(key)
}
