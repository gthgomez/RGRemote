package com.rgremote.app.roku

/**
 * Shared extension function to produce a user-facing message from a [Throwable].
 *
 * Prefer this over raw [Throwable.message] in UI contexts so that
 * [RokuNetworkAccessException] gets its actionable instruction string rather
 * than a generic error message.
 */
internal fun Throwable.displayMessage(): String =
    when (this) {
        is RokuNetworkAccessException -> message.orEmpty()
        else -> message ?: "Unknown error"
    }
