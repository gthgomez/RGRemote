package com.rgremote.app.ui

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
