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
import com.rgremote.app.di.AppContainer
import com.rgremote.app.ui.RGRemoteApp
import com.rgremote.app.ui.RGRemoteViewModel
import com.rgremote.app.ui.theme.RGRemoteTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: RGRemoteViewModel
    private val nearbyWifiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNearbyWifiPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Application-scoped graph, built once per process. The factory below runs
        // only on first ViewModel creation, so later recreations must not rebuild it.
        val container = AppContainer.get(applicationContext)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RGRemoteViewModel(
                        context = applicationContext,
                        registry = container.registry,
                        discoveryService = container.discoveryService,
                        rokuAdapter = container.rokuClient,
                        googleTvAdapter = container.googleTvAdapter,
                        pairingManager = container.pairingManager,
                        appPinStore = container.appPinStore,
                        uiPreferences = container.uiPreferences,
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
