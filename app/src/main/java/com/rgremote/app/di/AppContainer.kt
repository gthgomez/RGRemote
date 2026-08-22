package com.rgremote.app.di

import android.content.Context
import com.rgremote.app.data.apps.AppPinStore
import com.rgremote.app.data.db.RGRemoteDatabase
import com.rgremote.app.data.preferences.RemoteUiPreferences
import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.discovery.DiscoveryService
import com.rgremote.app.discovery.GoogleTvNsdDiscovery
import com.rgremote.app.discovery.RokuSsdpDiscovery
import com.rgremote.app.google.GoogleTvAdapter
import com.rgremote.app.google.GoogleTvKeyStore
import com.rgremote.app.google.GoogleTvPairingManager
import com.rgremote.app.roku.RokuEcpClient

/**
 * Application-scoped DI graph, constructed exactly once per process so the
 * surviving ViewModel keeps the same collaborator instances across Activity
 * recreations. Holds application context only — never an Activity.
 */
class AppContainer private constructor(context: Context) {
    private val appContext = context.applicationContext

    private val keyStore = GoogleTvKeyStore(appContext)

    val registry: DeviceRegistry = DeviceRegistry(RGRemoteDatabase.get(appContext).deviceDao())
    val rokuClient: RokuEcpClient = RokuEcpClient()
    val discoveryService: DiscoveryService = DiscoveryService(
        registry = registry,
        rokuSsdpDiscovery = RokuSsdpDiscovery(appContext),
        googleTvNsdDiscovery = GoogleTvNsdDiscovery(appContext),
        rokuClient = rokuClient,
    )
    val pairingManager: GoogleTvPairingManager = GoogleTvPairingManager(registry, keyStore)
    val googleTvAdapter: GoogleTvAdapter = GoogleTvAdapter(registry, keyStore)
    val appPinStore: AppPinStore = AppPinStore(appContext)
    val uiPreferences: RemoteUiPreferences = RemoteUiPreferences(appContext)

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
