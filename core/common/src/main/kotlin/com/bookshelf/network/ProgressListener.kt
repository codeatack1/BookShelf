package com.bookshelf.network

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
