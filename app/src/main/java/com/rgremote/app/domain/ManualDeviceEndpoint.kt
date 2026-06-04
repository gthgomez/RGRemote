package com.rgremote.app.domain

import java.net.URI

data class ManualDeviceEndpoint(
    val host: String,
    val port: Int
) {
    companion object {
        fun parse(rawAddress: String, defaultPort: Int): ManualDeviceEndpoint {
            val trimmed = rawAddress.trim()
            require(trimmed.isNotBlank()) { "Enter an IP address or host name" }
            require(defaultPort in 1..65_535) { "Default port is invalid" }

            val uri = URI(if (trimmed.contains("://")) trimmed else "rgremote://$trimmed")
            val host = uri.host ?: trimmed.substringBefore(':').trim('[', ']')
            require(host.isNotBlank()) { "Enter a valid IP address or host name" }

            val port = if (uri.port > 0) uri.port else defaultPort
            require(port in 1..65_535) { "Port must be between 1 and 65535" }

            return ManualDeviceEndpoint(host = host, port = port)
        }
    }
}
