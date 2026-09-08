package com.bookshelf.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

object AppStorage {

    const val KEY_BACKGROUND = "background"

    fun parseArgb32(value: String): Int? =
        value.toLongOrNull()
            ?.takeIf { it in 0L..0xFFFF_FFFFL }
            ?.toInt()

    private val db: AppDatabase
        get() = DatabaseProvider.db

    @Volatile
    var cachedBackground: Int? = null

    fun init(context: Context) {
        DatabaseProvider.init(context)
    }

    suspend fun getString(key: String): String? =
        db.appStateDao().getValue(key)

    suspend fun putString(key: String, value: String) {
        db.appStateDao().put(AppStateEntity(id = key, value = value))
        if (key == KEY_BACKGROUND) {
            cachedBackground = parseArgb32(value)
        }
    }

    fun observe(key: String): Flow<String?> =
        db.appStateDao().observeValue(key)
}
