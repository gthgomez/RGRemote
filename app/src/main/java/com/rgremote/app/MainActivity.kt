package com.rgremote.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rgremote.app.di.AppContainer
import com.rgremote.app.ui.RGRemoteApp
import com.rgremote.app.ui.RGRemoteViewModel
import com.rgremote.app.ui.theme.RGRemoteTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: RGRemoteViewModel

    private val showSettingsPrompt = mutableStateOf(false)

    private val nearbyWifiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        handleNearbyWifiPermissionResult(granted)
    }

    private companion object {
        var settingsPromptShownThisProcess = false
        const val PREF_NEARBY_WIFI_DENIED_BEFORE = "nearby_wifi_denied_before"
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
                PermanentDenialSettingsDialog(
                    visible = showSettingsPrompt.value,
                    onDismiss = { showSettingsPrompt.value = false },
                    onOpenSettings = {
                        showSettingsPrompt.value = false
                        openAppDetailsSettings()
                    },
                )
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
            markDeniedBefore(denied = false)
            viewModel.onNearbyWifiPermissionResult(granted = true)
            return
        }
        val permanentlyDenied = isDeniedBefore() &&
            !shouldShowRequestPermissionRationale(permission)
        if (permanentlyDenied) {
            viewModel.onNearbyWifiPermissionResult(granted = false)
            showRecoveryPromptOnce()
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

    private fun handleNearbyWifiPermissionResult(granted: Boolean) {
        if (!granted) {
            markDeniedBefore(denied = true)
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(Manifest.permission.NEARBY_WIFI_DEVICES)
            ) {
                showRecoveryPromptOnce()
            }
        }
        viewModel.onNearbyWifiPermissionResult(granted = granted)
    }

    private fun showRecoveryPromptOnce() {
        if (settingsPromptShownThisProcess) return
        settingsPromptShownThisProcess = true
        showSettingsPrompt.value = true
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
        runCatching { startActivity(intent) }
            .onFailure {
                runCatching {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
    }

    private fun isDeniedBefore(): Boolean =
        getPreferences(MODE_PRIVATE).getBoolean(PREF_NEARBY_WIFI_DENIED_BEFORE, false)

    private fun markDeniedBefore(denied: Boolean) {
        getPreferences(MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_NEARBY_WIFI_DENIED_BEFORE, denied)
            .apply()
    }
}

@Composable
private fun PermanentDenialSettingsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local network access needed") },
        text = {
            Text(
                "RGRemote needs nearby-network access to find and control TVs on your Wi-Fi network. " +
                    "Without it, device discovery finds nothing.\n\n" +
                    "You previously denied this permission, so Android can't ask again from here. " +
                    "You can enable Nearby devices for RGRemote in system Settings."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}
