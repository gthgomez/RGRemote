package com.rgremote.app.ui.controller

import android.util.Log
import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.DpadDirection
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
import com.rgremote.app.ui.CommandFeedbackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeviceCommandExecutor(
    private val scope: CoroutineScope,
    private val registry: DeviceRegistry,
    private val rokuConnection: RokuConnectionCoordinator,
    private val googleTvAdapter: RemoteAdapter,
    private val statusWriter: DeviceStatusWriter,
    private val getState: () -> RGRemoteUiState,
    private val onCommandSuccess: suspend (RegisteredDevice, RemoteCommand) -> Unit,
    private val feedback: CommandFeedbackController,
) {
    private val commandQueue = Channel<suspend () -> Unit>(64)
    private val dpadGate = DpadSequenceGate()

    init {
        scope.launch {
            for (action in commandQueue) {
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e // Re-throw to respect coroutine cancellation contract
                } catch (e: Exception) {
                    Log.e("DeviceCmdExecutor", "Unhandled exception in command queue consumer", e)
                }
            }
        }
    }

    fun enqueueCommand(action: suspend () -> Unit) {
        val result = commandQueue.trySend(action)
        if (result.isFailure) {
            Log.w("DeviceCmdExecutor", "Command queue full (capacity 64); dropping command")
        }
    }

    fun send(command: RemoteCommand) {
        val device = getState().selectedDevice ?: return feedback.show("No selected device")
        enqueueCommand {
            if (sendTo(device, command).isSuccess) {
                onCommandSuccess(device, command)
            }
        }
    }

    /** Opens a hold-repeat sequence; returns the token every repeat must carry. */
    fun beginDpadSequence(): Long = dpadGate.beginSequence()

    /**
     * Ends the active hold-repeat sequence: bumps the generation so dpad
     * commands already queued but not yet executed are dropped on dequeue.
     */
    fun cancelQueuedDpads(): Long = dpadGate.cancelActiveSequence()

    /** Gated repeat dispatch for the halo ring; stale repeats are skipped. */
    fun sendDpad(direction: DpadDirection, generation: Long) {
        val device = getState().selectedDevice ?: return feedback.show("No selected device")
        enqueueCommand {
            if (!dpadGate.isValid(generation)) return@enqueueCommand
            val command = RemoteCommand.Dpad(direction)
            if (sendTo(device, command).isSuccess) {
                onCommandSuccess(device, command)
            }
        }
    }

    /**
     * Sends a batch (e.g. typed characters) as ONE queued action so long text
     * never overflows the fixed-capacity command queue and keeps its ordering.
     */
    fun sendSequence(commands: List<RemoteCommand>) {
        if (commands.isEmpty()) return
        val device = getState().selectedDevice ?: return feedback.show("No selected device")
        enqueueCommand {
            commands.forEach { command ->
                if (sendTo(device, command).isSuccess) {
                    onCommandSuccess(device, command)
                }
            }
        }
    }

    fun switchTvInput(port: HdmiPort) {
        val roku = getState().rokuDevice ?: return feedback.show("Add a Roku TV first")
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
            statusWriter.writeForDevice(device.id, withStatus(ConnectionStatus.WAKE_UNAVAILABLE))
            if (showFeedback) {
                feedback.show("${command.label()} is not supported by ${device.friendlyName}")
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
                statusWriter.writeForDevice(device.id, withStatus(ConnectionStatus.ONLINE))
                if (showFeedback && !command.isHighFrequency()) {
                    feedback.show("Sent ${command.label()} to ${device.friendlyName}")
                }
            }
            .onFailure { error ->
                registry.markCommandFailure(device)
                if (command == RemoteCommand.PowerOn) {
                    statusWriter.writeForDevice(device.id) { state ->
                        state.copy(
                            connectionStatus = ConnectionStatus.WAKE_UNAVAILABLE,
                            diagnosticMessage = "Wake failed — this TV can't be woken over the network.",
                        )
                    }
                    if (showFeedback) {
                        val powerMode = getState().rokuPowerModeByDeviceId[device.id]
                        val hint = RokuPowerMode.wakeHint(powerMode)
                        feedback.show(hint ?: "${device.friendlyName}: ${error.displayMessage()}")
                    }
                } else {
                    statusWriter.writeForDevice(device.id, withStatus(ConnectionStatus.CONNECTION_FAILED))
                    if (showFeedback) {
                        feedback.show("${device.friendlyName}: ${error.displayMessage()}")
                    }
                }
            }
    }

    /** Commands fired repeatedly during normal use; per-press success feedback would spam. */
    private fun RemoteCommand.isHighFrequency(): Boolean =
        when (this) {
            is RemoteCommand.Dpad, is RemoteCommand.Volume -> true
            RemoteCommand.Select, RemoteCommand.Home, RemoteCommand.Back, RemoteCommand.PlayPause -> true
            is RemoteCommand.Character, RemoteCommand.KeyboardEnter, RemoteCommand.KeyboardBackspace -> true
            RemoteCommand.Rewind, RemoteCommand.FastForward -> true
            else -> false
        }

    private fun RemoteCommand.label(): String =
        when (this) {
            is RemoteCommand.Dpad -> direction.name.lowercase()
            RemoteCommand.Select -> "select"
            RemoteCommand.Home -> "home"
            RemoteCommand.Back -> "back"
            RemoteCommand.PlayPause -> "play"
            RemoteCommand.Rewind -> "rewind"
            RemoteCommand.FastForward -> "fast forward"
            is RemoteCommand.Character -> "character"
            RemoteCommand.KeyboardEnter -> "enter"
            RemoteCommand.KeyboardBackspace -> "backspace"
            is RemoteCommand.Volume -> command.name.lowercase()
            RemoteCommand.PowerOn -> "wake/home"
            RemoteCommand.PowerOff -> "power off"
            RemoteCommand.PowerToggle -> "power"
            is RemoteCommand.LaunchApp -> "launch"
            is RemoteCommand.SetInput -> port.displayName
        }

}
