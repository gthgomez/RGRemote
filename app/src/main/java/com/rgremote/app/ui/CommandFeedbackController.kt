package com.rgremote.app.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared feedback controller used by [RGRemoteViewModel] and [DeviceCommandExecutor]
 * to avoid duplicated [setCommandFeedback] implementations.
 *
 * Shows a transient user-facing message that auto-clears after [CLEAR_DELAY_MILLIS].
 * Passing null clears any active feedback immediately.
 */
class CommandFeedbackController(
    private val scope: CoroutineScope,
    private val updateState: ((RGRemoteUiState) -> RGRemoteUiState) -> Unit,
) {
    private var clearJob: Job? = null

    /** Cancels any pending auto-clear timer without changing the current feedback text. */
    fun cancel() {
        clearJob?.cancel()
    }

    fun show(message: String?) {
        clearJob?.cancel()
        if (message == null) {
            updateState { it.copy(userFeedback = null, showFeedback = false) }
            return
        }
        updateState { it.copy(userFeedback = message, showFeedback = true) }
        clearJob = scope.launch {
            delay(CLEAR_DELAY_MILLIS)
            updateState { state ->
                if (state.userFeedback == message) {
                    state.copy(userFeedback = null, showFeedback = false)
                } else {
                    state
                }
            }
        }
    }

    companion object {
        private const val CLEAR_DELAY_MILLIS = 3_000L
    }
}
