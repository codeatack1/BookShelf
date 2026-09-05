package com.bookshelf.server

import android.content.Context
import android.util.Log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "BookShelf"

object LocalServer {

    private const val PORT_RANGE_START = 8735
    private const val PORT_RANGE_END = 8755

    private val startedPort = AtomicInteger(-1)

    fun start(context: Context): Int {
        Log.i(TAG, "LocalServer.start called")
        val current = startedPort.get()
        if (current != -1) {
            Log.i(TAG, "LocalServer.start: already started, port=$current")
            return current
        }

        synchronized(this) {
            val recheck = startedPort.get()
            if (recheck != -1) {
                Log.i(TAG, "LocalServer.start: already started, port=$recheck")
                return recheck
            }

            val port = findFreePort()
            if (port == null) {
                Log.e(TAG, "LocalServer: no free port in $PORT_RANGE_START..$PORT_RANGE_END")
                return -1
            }
            Log.d(TAG, "LocalServer: probing found free port=$port")

            return try {
                val server = embeddedServer(Netty, host = "127.0.0.1", port = port) {
                    routing {
                        Api.register(this)
                        StaticContent.register(this, context)
                    }
                }
                server.start(wait = false)
                startedPort.set(port)
                Log.i(TAG, "LocalServer: Ktor running on http://127.0.0.1:$port")
                port
            } catch (t: Throwable) {
                Log.e(TAG, "LocalServer: failed to start", t)
                -1
            }
        }
    }

    private fun findFreePort(): Int? {
        for (port in PORT_RANGE_START..PORT_RANGE_END) {
            if (isPortFree(port)) return port
        }
        return null
    }

    private fun isPortFree(port: Int): Boolean = try {
        ServerSocket().use { it.bind(InetSocketAddress("127.0.0.1", port)) }
        true
    } catch (_: Exception) {
        false
    }
}