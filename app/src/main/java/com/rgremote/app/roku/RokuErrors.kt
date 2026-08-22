package com.rgremote.app.roku

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Shared extension functions to produce user-facing messages from a [Throwable].
 *
 * [displayMessage] maps common failure shapes to short actionable copy so raw
 * jargon (socket/HTTP internals) never reaches snackbars. Prefer it over raw
 * [Throwable.message] in UI contexts; [RokuNetworkAccessException] keeps its
 * actionable network-access instruction.
 *
 * [diagnosticDetail] additionally carries the raw cause text and must only be
 * used on diagnostic surfaces, never snackbars.
 */
internal fun Throwable.displayMessage(): String {
    val description = message?.lowercase().orEmpty()
    return when (this) {
        is RokuNetworkAccessException ->
            "The TV is blocking remote commands. On the TV, set Settings > System > " +
                "Advanced system settings > Control by mobile apps > Network access to Enabled."
        is ConnectException -> unreachableTvMessage()
        is SocketTimeoutException ->
            if ("connect" in description) unreachableTvMessage() else TV_NOT_RESPONDING_MESSAGE
        is IOException -> when {
            description.hasAny(PAIRING_TERMS) -> TV_NOT_PAIRED_MESSAGE
            description.hasAny(TIMEOUT_TERMS) || description.hasAny(DROPPED_CONNECTION_TERMS) ->
                TV_NOT_RESPONDING_MESSAGE
            description.contains("power toggle") -> POWER_TOGGLE_UNSUPPORTED_MESSAGE
            else -> UNEXPECTED_TV_ERROR_MESSAGE
        }
        else -> UNEXPECTED_TV_ERROR_MESSAGE
    }
}

/** Humanized message plus the raw cause text; diagnostics only, never snackbars. */
internal fun Throwable.diagnosticDetail(): String {
    val humanized = displayMessage()
    val raw = message?.takeIf { it.isNotBlank() } ?: return humanized
    return "$humanized ($raw)"
}

private const val TV_NOT_RESPONDING_MESSAGE = "The TV didn't respond. Try again."
private const val UNEXPECTED_TV_ERROR_MESSAGE = "Something went wrong controlling the TV."
private const val TV_NOT_PAIRED_MESSAGE = "This TV isn't paired yet. Pair it in the app to control it."
private const val POWER_TOGGLE_UNSUPPORTED_MESSAGE =
    "This Roku can't toggle power over the network — use Power Off or Wake instead."

private val TIMEOUT_TERMS = listOf("timeout", "timed out")
private val DROPPED_CONNECTION_TERMS = listOf(
    "connection reset",
    "connection aborted",
    "connection closed",
    "socket closed",
    "broken pipe",
)
private val PAIRING_TERMS = listOf("not paired", "re-pair", "pair the device again")

private fun Throwable.unreachableTvMessage(): String {
    val host = Regex("""\d{1,3}(?:\.\d{1,3}){3}""").find(message.orEmpty())?.value
    return if (host != null) {
        "Couldn't reach the TV at $host — make sure the TV is on and your phone is on the same Wi-Fi."
    } else {
        "Couldn't reach the TV — make sure the TV is on and your phone is on the same Wi-Fi."
    }
}

private fun String.hasAny(terms: List<String>): Boolean = terms.any { contains(it) }
