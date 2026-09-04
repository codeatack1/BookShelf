package com.bookshelf.server

import android.content.Context
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

object LocalServer {

    private const val PORT_RANGE_START = 8735
    private const val PORT_RANGE_END = 8755

    private val startedPort = AtomicInteger(-1)

    fun start(context: Context): Int {
        val current = startedPort.get()
        if (current != -1) return current

        synchronized(this) {
            val recheck = startedPort.get()
            if (recheck != -1) return recheck

            val port = findFreePort()
            if (port == null) return -1

            return try {
                val server = embeddedServer(Netty, host = "127.0.0.1", port = port) {
                    routing {
                        Api.register(this)
                        StaticContent.register(this, context)
                    }
                }
                server.start(wait = false)
                startedPort.set(port)
                port
            } catch (_: Exception) {
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