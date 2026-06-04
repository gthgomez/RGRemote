package com.rgremote.app.roku

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class RokuLaunchTarget(
    val channelId: String,
    val query: String? = null
) {
    val path: String =
        buildString {
            append("launch/")
            append(channelId.rokuUrlComponent())
            query?.let {
                append('?')
                append(it)
            }
        }

    companion object {
        fun parse(rawTarget: String): RokuLaunchTarget {
            val normalized = rawTarget.trim()
            require(normalized.isNotBlank()) { "Roku launch target cannot be blank" }

            val commandTarget = normalized.withoutRokuLaunchPrefix()
            val channelId = commandTarget.substringBefore('?').trim().trim('/')
            require(channelId.isNotBlank()) { "Roku launch target needs a channel id" }

            val query = commandTarget.substringAfter('?', missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }
                ?.normalizeQuery()
            return RokuLaunchTarget(channelId = channelId, query = query)
        }

        private fun String.withoutRokuLaunchPrefix(): String {
            val target = if (contains("://")) {
                val uri = URI(this)
                buildString {
                    append(uri.path.orEmpty().trimStart('/'))
                    uri.rawQuery?.let {
                        append('?')
                        append(it)
                    }
                }
            } else {
                trimStart('/')
            }
            return when {
                target.startsWith("launch/", ignoreCase = true) -> target.substringAfter('/')
                target.startsWith("input/", ignoreCase = true) -> target.substringAfter('/')
                else -> target
            }
        }

        private fun String.normalizeQuery(): String =
            split('&')
                .mapNotNull { parameter ->
                    val trimmed = parameter.trim()
                    if (trimmed.isBlank()) return@mapNotNull null
                    val separator = trimmed.indexOf('=')
                    if (separator < 0) {
                        trimmed.decoded().rokuUrlComponent()
                    } else {
                        val key = trimmed.substring(0, separator).decoded().rokuUrlComponent()
                        val value = trimmed.substring(separator + 1).decoded().rokuUrlComponent()
                        "$key=$value"
                    }
                }
                .joinToString("&")

        private fun String.decoded(): String =
            URLDecoder.decode(this, Charsets.UTF_8.name())
    }
}

private fun String.rokuUrlComponent(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
