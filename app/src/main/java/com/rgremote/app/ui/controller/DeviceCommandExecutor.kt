package com.rgremote.app.ui.controller

import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteAdapter
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.roku.RokuConnectionCoordinator
import com.rgremote.app.roku.RokuPowerMode
import com.rgremote.app.roku.displayMessage
import com.rgremote.app.ui.ConnectionStatus
import com.rgremote.app.ui.RokuControlAvailability
import com.rgremote.app.ui.RGRemoteUiState
import com.rgremote.app.ui.withStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeviceCommandExecutor(
    private val scope: CoroutineScope,
    private val registry: DeviceRegistry,
    private val rokuConnection: RokuConnectionCoordinator,
    private val googleTvAdapter: RemoteAdapter,
    private val getState: () -> RGRemoteUiState,
    private val updateState: ((RGRemoteUiState) -> RGRemoteUiState) -> Unit,
    private val onCommandSuccess: suspend (RegisteredDevice, RemoteCommand) -> Unit
) {
    private val commandQueue = Channel<suspend () -> Unit>(64)
    private var feedbackClearJob: Job? = null

    init {
        scope.launch {
            for (action in commandQueue) {
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e // Re-throw to respect coroutine cancellation contract
                } catch (e: Exception) {
                    // Prevent queue failure on non-cancellation errors
                }
            }
        }
    }

    fun enqueueCommand(action: suspend () -> Unit) {
        val result = commandQueue.trySend(action)
        if (result.isFailure) {
            // Channel full, drop command to prevent OOM
        }
    }

    fun send(command: RemoteCommand) {
        val device = getState().selectedDevice ?: return setCommandFeedback("No selected device")
        enqueueCommand {
            if (sendTo(device, command).isSuccess) {
                onCommandSuccess(device, command)
            }
        }
    }

    fun switchTvInput(port: HdmiPort) {
        val roku = getState().rokuDevice ?: return setCommandFeedback("Add a Roku TV first")
        enqueueCommand {
            if (sendTo(roku, RemoteCommand.SetInput(port)).isSuccess) {
                delay(500)
                onCommandSuccess(roku, RemoteCommand.SetInput(port))
            }
        }
    }

    suspend fun sendTo(
        device: RegisteredDevice,
        command: RemoteCommand,
        showFeedback: Boolean = true,
    ): Result<Unit> {
        val controls = getState().rokuControlsByDeviceId[device.id] ?: RokuControlAvailability()
        if (device.type == DeviceType.ROKU_TV && !controls.supports(command)) {
            updateState(withStatus(ConnectionStatus.WAKE_UNAVAILABLE))
            if (showFeedback) {
                setCommandFeedback("${command.label()} is not supported by ${device.friendlyName}")
            }
            return Result.failure(UnsupportedOperationException("Roku device does not support ${command.label()}"))
        }
        val result = when (device.type) {
            DeviceType.ROKU_TV -> rokuConnection.sendWithRecovery(device, command)
            DeviceType.GOOGLE_TV -> googleTvAdapter.send(device, command)
        }
        return result
            .onSuccess {
                registry.markCommandSuccess(device)
                updateState(withStatus(ConnectionStatus.ONLINE))
                if (showFeedback) {
                    setCommandFeedback("Sent ${command.label()} to ${device.friendlyName}")
                }
            }
            .onFailure { error ->
                registry.markCommandFailure(device)
                if (command == RemoteCommand.PowerOn) {
                    updateState(withStatus(ConnectionStatus.WAKE_UNAVAILABLE))
                    if (showFeedback) {
                        val powerMode = getState().rokuPowerModeByDeviceId[device.id]
                        val hint = RokuPowerMode.wakeHint(powerMode)
                        setCommandFeedback(hint ?: "${device.friendlyName}: ${error.displayMessage()}")
                    }
                } else {
                    updateState(withStatus(ConnectionStatus.CONNECTION_FAILED))
                    if (showFeedback) {
                        setCommandFeedback("${device.friendlyName}: ${error.displayMessage()}")
                    }
                }
            }
    }

    private fun setCommandFeedback(message: String?) {
        feedbackClearJob?.cancel()
        if (message == null) {
            updateState { state ->
                state.copy(userFeedback = null, showFeedback = false)
            }
            return
        }
        updateState { state ->
            state.copy(userFeedback = message, showFeedback = true)
        }
        feedbackClearJob = scope.launch {
            delay(FEEDBACK_CLEAR_DELAY_MILLIS)
            updateState { state ->
                if (state.userFeedback == message) {
                    state.copy(userFeedback = null, showFeedback = false)
                } else {
                    state
                }
            }
        }
    }

    private fun RemoteCommand.label(): String =
        when (this) {
            is RemoteCommand.Dpad -> direction.name.lowercase()
            RemoteCommand.Select -> "select"
            RemoteCommand.Home -> "home"
            RemoteCommand.Back -> "back"
            RemoteCommand.PlayPause -> "play"
            is RemoteCommand.Volume -> command.name.lowercase()
            RemoteCommand.PowerOn -> "wake/home"
            RemoteCommand.PowerOff -> "power off"
            RemoteCommand.PowerToggle -> "power"
            is RemoteCommand.LaunchApp -> "launch"
            is RemoteCommand.SetInput -> port.displayName
        }

    companion object {
        private const val FEEDBACK_CLEAR_DELAY_MILLIS = 3_000L
    }
}
