package com.rgremote.app.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgremote.app.ui.ConnectionStatus
import com.rgremote.app.ui.drawHaloRing
import com.rgremote.app.ui.theme.HaloSpec
import com.rgremote.app.ui.theme.LocalHaloColors
import com.rgremote.app.ui.theme.RGRemoteTheme
import com.rgremote.app.ui.theme.ThemeMode

/**
 * Deterministic fixtures for the Halo visual contract (`docs/ui-target.md`).
 * The layout here is a static draft of the target composition: it renders the
 * background, header, device chip, ring, dock, and More Controls affordance
 * with fixed content and no animation, so dark/light x status previews are
 * reproducible. PR02 replaces the placeholder ring with HaloDpad and PR03
 * replaces this fixture with the real minimal remote screen.
 */
@Composable
fun HaloVisualContractFixture(status: ConnectionStatus) {
    val halo = LocalHaloColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(halo.backgroundTop, halo.backgroundBase, halo.backgroundDeep)
                )
            )
    ) {
        AmbientBlooms()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = HaloSpec.ScreenHorizontalPaddingDp.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderRow()
            Spacer(Modifier.height(12.dp))
            DeviceChip(
                deviceName = "55\" TCL Roku TV",
                status = status
            )
            Spacer(Modifier.weight(1f))
            RingPlaceholder(enabled = status == ConnectionStatus.ONLINE)
            Spacer(Modifier.weight(1f))
            DockPlaceholder(enabled = status == ConnectionStatus.ONLINE)
            Spacer(Modifier.height(20.dp))
            MoreControlsAffordance()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AmbientBlooms() {
    val halo = LocalHaloColors.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(halo.bloomViolet.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(0.08f, 0.62f),
                        radius = 900f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(halo.bloomBlue.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(0.95f, 0.40f),
                        radius = 850f
                    )
                )
        )
        if (halo.bloomPink != Color.Transparent) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(halo.bloomPink.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(0.50f, 0.86f),
                            radius = 520f
                        )
                    )
            )
        }
    }
}

@Composable
private fun HeaderRow() {
    val halo = LocalHaloColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HaloSpec.HeaderHeightDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "RG",
            color = halo.wordmarkAccent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Remote",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(halo.dockWell, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = halo.dockIcon
            )
        }
    }
}

@Composable
private fun DeviceChip(deviceName: String, status: ConnectionStatus) {
    val halo = LocalHaloColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HaloSpec.ChipHeightDp.dp)
            .background(halo.chipFill, shape)
            .border(1.dp, halo.chipBorder, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(halo.wordmarkAccent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Tv,
                contentDescription = null,
                tint = halo.wordmarkAccent
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = deviceName,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .width(1.dp)
                .height(20.dp)
                .background(halo.dockSeparator)
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(statusDotColor(status), CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText(status),
                    color = statusDotColor(status),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Switch device",
            tint = halo.dockIcon
        )
    }
}

@Composable
private fun statusDotColor(status: ConnectionStatus): Color {
    val halo = LocalHaloColors.current
    return when (status) {
        ConnectionStatus.ONLINE -> halo.statusOnline
        ConnectionStatus.CHECKING -> halo.statusChecking
        ConnectionStatus.NOT_PAIRED,
        ConnectionStatus.PAIRED -> halo.statusUnpaired
        else -> halo.statusOffline
    }
}

private fun statusText(status: ConnectionStatus): String = when (status) {
    ConnectionStatus.ONLINE -> "Online"
    ConnectionStatus.CHECKING -> "Checking…"
    ConnectionStatus.NOT_PAIRED -> "Not paired — tap to set up"
    ConnectionStatus.PAIRED -> "Paired"
    ConnectionStatus.CONNECTION_FAILED -> "Connection failed"
    ConnectionStatus.WAKE_UNAVAILABLE -> "Offline"
    ConnectionStatus.OFFLINE -> "Offline"
}

/**
 * Static ring draft rendered with the shared drawHaloRing (HaloDpad visuals).
 */
@Composable
private fun RingPlaceholder(enabled: Boolean) {
    val halo = LocalHaloColors.current
    val configuration = LocalConfiguration.current
    val diameterDp = minOf(
        configuration.screenWidthDp * HaloSpec.RingWidthFraction,
        HaloSpec.RingMaxDiameterDp
    ).dp
    val alpha = if (enabled) 1f else 0.35f
    Canvas(
        modifier = Modifier
            .width(diameterDp)
            .aspectRatio(1f)
            .alpha(alpha)
    ) {
        drawHaloRing(halo, pressed = null)
    }
}

@Composable
private fun DockPlaceholder(enabled: Boolean) {
    val halo = LocalHaloColors.current
    val shape = RoundedCornerShape(28.dp)
    val alpha = if (enabled) 1f else 0.35f
    Row(
        modifier = Modifier
            .fillMaxWidth(HaloSpec.DockWidthFraction)
            .height(HaloSpec.DockHeightDp.dp)
            .alpha(alpha)
            .background(halo.dockFill, shape)
            .border(1.dp, halo.dockBorder, shape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DockWell(icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = halo.dockIcon) })
        DockSeparator()
        DockWell(icon = { Icon(Icons.Filled.Home, contentDescription = "Home", tint = halo.dockIcon) })
        DockSeparator()
        DockWell(
            wellColor = halo.powerWellTint,
            icon = {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Composable
private fun DockWell(wellColor: Color = LocalHaloColors.current.dockWell, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(HaloSpec.DockWellDp.dp)
            .background(wellColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun DockSeparator() {
    Box(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(LocalHaloColors.current.dockSeparator)
    )
}

@Composable
private fun MoreControlsAffordance() {
    val halo = LocalHaloColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(halo.dockWell, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandLess,
                contentDescription = null,
                tint = halo.moreControls
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "MORE CONTROLS",
            color = halo.moreControls,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Dark · Online", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloDarkOnlinePreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloVisualContractFixture(ConnectionStatus.ONLINE)
    }
}

@Preview(name = "Dark · Checking", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloDarkCheckingPreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloVisualContractFixture(ConnectionStatus.CHECKING)
    }
}

@Preview(name = "Dark · Offline", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloDarkOfflinePreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloVisualContractFixture(ConnectionStatus.OFFLINE)
    }
}

@Preview(name = "Dark · Not paired", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloDarkNotPairedPreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloVisualContractFixture(ConnectionStatus.NOT_PAIRED)
    }
}

@Preview(name = "Light · Online", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloLightOnlinePreview() {
    RGRemoteTheme(ThemeMode.LIGHT) {
        HaloVisualContractFixture(ConnectionStatus.ONLINE)
    }
}

@Preview(name = "Light · Checking", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloLightCheckingPreview() {
    RGRemoteTheme(ThemeMode.LIGHT) {
        HaloVisualContractFixture(ConnectionStatus.CHECKING)
    }
}

@Preview(name = "Light · Offline", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloLightOfflinePreview() {
    RGRemoteTheme(ThemeMode.LIGHT) {
        HaloVisualContractFixture(ConnectionStatus.OFFLINE)
    }
}

@Preview(name = "Light · Not paired", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HaloLightNotPairedPreview() {
    RGRemoteTheme(ThemeMode.LIGHT) {
        HaloVisualContractFixture(ConnectionStatus.NOT_PAIRED)
    }
}
