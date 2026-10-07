package com.rgremote.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rgremote.app.R

/**
 * Returns a state transformation that sets the [ConnectionStatus].
 *
 * Eliminates the need for per-file private `setStatus` helpers whose only
 * difference is the [updateState] lambda.
 */
internal fun withStatus(status: ConnectionStatus): (RGRemoteUiState) -> RGRemoteUiState =
    { it.copy(connectionStatus = status) }

/**
 * Returns a state transformation that sets the diagnostic message.
 *
 * Eliminates the need for per-file private `setDiagnostic` helpers whose only
 * difference is the [updateState] lambda.
 */
internal fun withDiagnostic(message: String?): (RGRemoteUiState) -> RGRemoteUiState =
    { it.copy(diagnosticMessage = message) }

/** Localized label for a [ConnectionStatus]; the enum's [ConnectionStatus.label] stays English for logs. */
@Composable
internal fun connectionStatusText(status: ConnectionStatus): String =
    stringResource(
        when (status) {
            ConnectionStatus.ONLINE -> R.string.status_online
            ConnectionStatus.OFFLINE -> R.string.status_offline
            ConnectionStatus.CHECKING -> R.string.status_checking
            ConnectionStatus.PAIRED -> R.string.status_paired
            ConnectionStatus.NOT_PAIRED -> R.string.status_not_paired
            ConnectionStatus.CONNECTION_FAILED -> R.string.status_connection_failed
            ConnectionStatus.WAKE_UNAVAILABLE -> R.string.status_wake_unavailable
        }
    )
