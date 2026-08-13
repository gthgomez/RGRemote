package com.rgremote.app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rgremote.app.data.apps.AppPinStore
import com.rgremote.app.data.preferences.RemoteUiPreferences
import com.rgremote.app.data.db.RGRemoteDatabase
import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.discovery.DiscoveryService
import com.rgremote.app.discovery.GoogleTvNsdDiscovery
import com.rgremote.app.discovery.RokuSsdpDiscovery
import com.rgremote.app.google.GoogleTvAdapter
import com.rgremote.app.google.GoogleTvKeyStore
import com.rgremote.app.google.GoogleTvPairingManager
import com.rgremote.app.roku.RokuEcpClient
import com.rgremote.app.ui.RGRemoteApp
import com.rgremote.app.ui.RGRemoteViewModel
import com.rgremote.app.ui.theme.RGRemoteTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: RGRemoteViewModel
    private lateinit var discoveryService: DiscoveryService
    private val nearbyWifiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNearbyWifiPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = RGRemoteDatabase.get(applicationContext).deviceDao()
        val registry = DeviceRegistry(dao)
        val rokuClient = RokuEcpClient()
        val keyStore = GoogleTvKeyStore(applicationContext)
        discoveryService = DiscoveryService(
            registry = registry,
            rokuSsdpDiscovery = RokuSsdpDiscovery(applicationContext),
            googleTvNsdDiscovery = GoogleTvNsdDiscovery(applicationContext),
            rokuClient = rokuClient,
        )
        val pairingManager = GoogleTvPairingManager(registry, keyStore)
        val googleTvAdapter = GoogleTvAdapter(registry, keyStore)
        val appPinStore = AppPinStore(applicationContext)
        val uiPreferences = RemoteUiPreferences(applicationContext)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RGRemoteViewModel(
                        context = applicationContext,
                        registry = registry,
                        discoveryService = discoveryService,
                        rokuAdapter = rokuClient,
                        googleTvAdapter = googleTvAdapter,
                        pairingManager = pairingManager,
                        appPinStore = appPinStore,
                        uiPreferences = uiPreferences,
                    ) as T
            },
        )[RGRemoteViewModel::class.java]

        setContent {
            RGRemoteTheme {
                RGRemoteApp(viewModel = viewModel)
            }
        }

        requestNearbyWifiPermissionOrStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        // DiscoveryService lifecycle is owned by the ViewModel.
        // Do NOT call discoveryService.destroy() here — the ViewModel survives
        // configuration changes and would be left with a permanently-cancelled
        // NSD coroutine scope.
    }

    private fun requestNearbyWifiPermissionOrStart() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.onNearbyWifiPermissionResult(granted = true)
            return
        }
        val permission = Manifest.permission.NEARBY_WIFI_DEVICES
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onNearbyWifiPermissionResult(granted = true)
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Nearby Wi-Fi devices")
            .setMessage(
                "RGRemote scans your local Wi-Fi network to find Roku TVs and Google TV boxes on the same LAN. " +
                    "This permission is used for device discovery only—not for location tracking. " +
                    "The app declares neverForLocation in the manifest."
            )
            .setPositiveButton("Continue") { _, _ ->
                nearbyWifiPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Not now") { _, _ ->
                viewModel.onNearbyWifiPermissionResult(granted = false)
            }
            .setCancelable(false)
            .show()
    }
}
