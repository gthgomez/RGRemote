package com.rgremote.app.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import java.util.ArrayDeque
import java.util.Locale

@Suppress("DEPRECATION")
class GoogleTvNsdDiscovery(
    context: Context,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val nsdManager = context.applicationContext
        .getSystemService(Context.NSD_SERVICE) as NsdManager

    private var listener: NsdManager.DiscoveryListener? = null
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var isResolvingService = false
    private var discoveryGeneration = 0L

    fun start(
        onDevice: (RegisteredDevice) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        val generation = ++discoveryGeneration
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                queueResolve(serviceInfo, generation, onDevice, onError)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                onError("Google TV discovery failed to start: $errorCode")
                stop()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                onError("Google TV discovery failed to stop: $errorCode")
            }
        }
        listener = discoveryListener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stop() {
        discoveryGeneration += 1
        listener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        listener = null
        synchronized(resolveQueue) {
            resolveQueue.clear()
            isResolvingService = false
        }
    }

    private fun queueResolve(
        serviceInfo: NsdServiceInfo,
        generation: Long,
        onDevice: (RegisteredDevice) -> Unit,
        onError: (String) -> Unit
    ) {
        if (generation != discoveryGeneration) return
        var startResolve = false
        synchronized(resolveQueue) {
            resolveQueue.addLast(serviceInfo)
            if (!isResolvingService) {
                isResolvingService = true
                startResolve = true
            }
        }
        if (startResolve) {
            resolveNextQueuedService(generation, onDevice, onError)
        }
    }

    private fun resolveNextQueuedService(
        generation: Long,
        onDevice: (RegisteredDevice) -> Unit,
        onError: (String) -> Unit
    ) {
        if (generation != discoveryGeneration) return
        val next = synchronized(resolveQueue) {
            if (resolveQueue.isNotEmpty()) {
                resolveQueue.removeFirst()
            } else {
                isResolvingService = false
                null
            }
        } ?: return

        nsdManager.resolveService(
            next,
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    if (generation != discoveryGeneration) return
                    onError("Google TV resolve failed: $errorCode")
                    finishResolve(generation, onDevice, onError)
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    if (generation != discoveryGeneration) return
                    val host = serviceInfo.host?.hostAddress ?: run {
                        finishResolve(generation, onDevice, onError)
                        return
                    }
                    val port = if (serviceInfo.port > 0) serviceInfo.port else 6466
                    val id = "googletv:${serviceInfo.serviceName.lowercase(Locale.US)}"
                    onDevice(
                        RegisteredDevice(
                            id = id,
                            type = DeviceType.GOOGLE_TV,
                            ipAddress = host,
                            port = port,
                            uniqueId = serviceInfo.serviceName,
                            friendlyName = serviceInfo.serviceName.ifBlank { "Google TV $host" },
                            lastSeenMillis = clockMillis(),
                            hdmiPortMapping = null,
                            isOnline = true,
                            consecutiveFailures = 0
                        )
                    )
                    finishResolve(generation, onDevice, onError)
                }
            }
        )
    }

    private fun finishResolve(
        generation: Long,
        onDevice: (RegisteredDevice) -> Unit,
        onError: (String) -> Unit
    ) {
        if (generation != discoveryGeneration) return
        synchronized(resolveQueue) {
            isResolvingService = false
        }
        resolveNextQueuedService(generation, onDevice, onError)
    }

    companion object {
        const val SERVICE_TYPE = "_androidtvremote2._tcp."
    }
}
