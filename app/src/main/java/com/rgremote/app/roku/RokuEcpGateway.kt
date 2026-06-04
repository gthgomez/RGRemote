package com.rgremote.app.roku

import com.rgremote.app.domain.ActiveApp
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.RokuDeviceInfo

interface RokuEcpGateway {
    suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit>
    suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo
    suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp?
    suspend fun queryApps(device: RegisteredDevice): List<RokuApp>
}
