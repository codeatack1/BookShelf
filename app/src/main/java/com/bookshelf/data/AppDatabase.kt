package com.bookshelf.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AppStateEntity::class, BookEntity::class, ChapterEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao
    abstract fun bookDao(): BookDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS books (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                        "author TEXT, category TEXT NOT NULL, lang TEXT NOT NULL, coverUrl TEXT, description TEXT, " +
                        "genre TEXT, year INTEGER, source TEXT, dateAdded INTEGER NOT NULL, " +
                        "bookmarked INTEGER NOT NULL DEFAULT 0, started INTEGER NOT NULL DEFAULT 0, " +
                        "completed INTEGER NOT NULL DEFAULT 0, lastReadAt INTEGER, lastReadChapterId TEXT)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chapters (id TEXT NOT NULL PRIMARY KEY, bookId TEXT NOT NULL, " +
                        "number INTEGER NOT NULL, name TEXT NOT NULL, content TEXT NOT NULL DEFAULT '', " +
                        "contentType TEXT NOT NULL DEFAULT 'none', read INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_bookId ON chapters(bookId)")
            }
        }
    }
}
