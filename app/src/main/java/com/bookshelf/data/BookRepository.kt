package com.bookshelf.data

import android.content.Context

object BookRepository {

    lateinit var db: AppDatabase
        private set

    fun init(context: Context) {
        db = DatabaseProvider.db
    }

    suspend fun searchBooks(query: String?, category: String?): List<BookEntity> =
        db.bookDao().searchBooks(query, category)

    suspend fun getBook(id: String): BookEntity? =
        db.bookDao().getBook(id)

    suspend fun getChapters(bookId: String): List<ChapterEntity> =
        db.bookDao().getChapters(bookId)

    suspend fun chapterCount(bookId: String): Int =
        db.bookDao().chapterCount(bookId)

    suspend fun unreadCount(bookId: String): Int =
        db.bookDao().unreadCount(bookId)

    suspend fun setChapterRead(bookId: String, chapterId: String, read: Boolean) {
        db.bookDao().setChapterRead(bookId, chapterId, read)
    }

    suspend fun updateBook(book: BookEntity) {
        db.bookDao().updateBook(book)
    }

    suspend fun countBooks(): Int =
        db.bookDao().countBooks()

    suspend fun insertBooks(books: List<BookEntity>) {
        db.bookDao().insertBooks(books)
    }
}
