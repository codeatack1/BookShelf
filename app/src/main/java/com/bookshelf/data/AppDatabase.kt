package com.bookshelf.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppStateEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao
}
