package com.bookshelf.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    lateinit var db: AppDatabase
        private set

    fun init(context: Context) {
        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "bookshelf.db",
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }
}
