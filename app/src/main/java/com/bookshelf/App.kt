package com.bookshelf

import android.app.Application
import android.os.Process
import android.util.Log
import com.bookshelf.server.LocalServer

private const val TAG = "BookShelf"

class App : Application() {

    companion object {
        @Volatile
        var serverPort: Int = -1
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App.onCreate: pid=${Process.myPid()} package=$packageName")
        val port = LocalServer.start(this)
        serverPort = port
        Log.i(TAG, "App.onCreate: LocalServer.start returned port=$port")
        if (port < 0) {
            Log.e(TAG, "App.onCreate: server failed to start")
        }
    }
}