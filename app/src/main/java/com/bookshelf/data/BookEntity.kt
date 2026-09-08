package com.bookshelf.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val category: String,
    val lang: String,
    val coverUrl: String?,
    val description: String?,
    val genre: String?,
    val year: Int?,
    val source: String?,
    val dateAdded: Long,
    val bookmarked: Boolean,
    val started: Boolean,
    val completed: Boolean,
    val lastReadAt: Long?,
    val lastReadChapterId: String?,
)
