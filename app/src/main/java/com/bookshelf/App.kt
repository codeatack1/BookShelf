package com.bookshelf

import android.app.Application
import com.bookshelf.server.LocalServer

class App : Application() {

    companion object {
        @Volatile
        var serverPort: Int = -1
            private set
    }

    override fun onCreate() {
        super.onCreate()
        serverPort = LocalServer.start(this)
    }
}