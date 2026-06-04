package com.rgremote.app.roku

import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RokuEcpClientTest {
    private var server: HttpServer? = null

    @After
    fun tearDown() {
        server?.stop(0)
        server = null
    }

    @Test
    fun queryDeviceInfo_parsesSuccessfulResponse() = runBlocking {
        var requestedPath: String? = null
        var requestMethod: String? = null
        val device = startServer { path, method ->
            requestedPath = path
            requestMethod = method
            200 to """
                <device-info>
                    <friendly-device-name>Test Roku</friendly-device-name>
                    <power-mode>PowerOn</power-mode>
                    <supports-tv-power-control>true</supports-tv-power-control>
                </device-info>
            """.trimIndent()
        }

        val info = RokuEcpClient(timeoutMillis = 5_000).queryDeviceInfo(device)

        assertEquals("GET", requestMethod)
        assertEquals("query/device-info", requestedPath)
        assertEquals("Test Roku", info.friendlyName)
        assertEquals("PowerOn", info.powerMode)
        assertEquals(true, info.supportsTvPowerControl)
    }

    @Test
    fun send_forbiddenSurfacesNetworkAccessException() = runBlocking {
        val device = startServer { _, _ -> 403 to "" }

        val error = RokuEcpClient(timeoutMillis = 5_000)
            .send(device, RemoteCommand.Home)
            .exceptionOrNull()

        assertTrue(error is RokuNetworkAccessException)
    }

    @Test
    fun send_powerOnPostsHomeKeypress() = runBlocking {
        var postedPath: String? = null
        var postedMethod: String? = null
        val device = startServer { path, method ->
            postedPath = path
            postedMethod = method
            200 to ""
        }

        val result = RokuEcpClient(timeoutMillis = 5_000).send(device, RemoteCommand.PowerOn)

        assertTrue(result.isSuccess)
        assertEquals("POST", postedMethod)
        assertEquals("keypress/Home", postedPath)
    }

    @Test
    fun send_powerOffPostsDocumentedKeypress() = runBlocking {
        var postedPath: String? = null
        val device = startServer { path, method ->
            if (method == "POST") postedPath = path
            200 to ""
        }

        assertTrue(
            RokuEcpClient(timeoutMillis = 5_000)
                .send(device, RemoteCommand.PowerOff)
                .isSuccess
        )
        assertEquals("keypress/PowerOff", postedPath)
    }

    private fun startServer(
        handler: (path: String, method: String) -> Pair<Int, String>,
    ): RegisteredDevice {
        val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        http.createContext("/") { exchange ->
            val path = exchange.requestURI.path.removePrefix("/")
            val (code, body) = handler(path, exchange.requestMethod)
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            if (bytes.isNotEmpty()) {
                exchange.responseBody.use { it.write(bytes) }
            } else {
                exchange.responseBody.close()
            }
        }
        http.start()
        server = http
        return testDevice(port = http.address.port)
    }

    private fun testDevice(port: Int): RegisteredDevice =
        RegisteredDevice(
            id = "roku:test",
            type = DeviceType.ROKU_TV,
            ipAddress = "127.0.0.1",
            port = port,
            uniqueId = "test",
            friendlyName = "Test Roku",
            lastSeenMillis = 0L,
            hdmiPortMapping = null,
            isOnline = true,
            consecutiveFailures = 0
        )
}
