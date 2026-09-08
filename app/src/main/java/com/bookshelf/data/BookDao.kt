package com.bookshelf.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookDao {
    @Query("SELECT COUNT(*) FROM books")
    suspend fun countBooks(): Int

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: String): BookEntity?

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY number ASC")
    suspend fun getChapters(bookId: String): List<ChapterEntity>

    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId")
    suspend fun chapterCount(bookId: String): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId AND read = 0")
    suspend fun unreadCount(bookId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE chapters SET read = :read WHERE bookId = :bookId AND id = :chapterId")
    suspend fun setChapterRead(bookId: String, chapterId: String, read: Boolean)

    @Query(
        "SELECT * FROM books WHERE (:query IS NULL OR title LIKE '%' || :query || '%') " +
            "AND (:category IS NULL OR category = :category) ORDER BY dateAdded DESC"
    )
    suspend fun searchBooks(query: String?, category: String?): List<BookEntity>
}
