package com.rgremote.app.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgremote.app.ui.HaloDpad
import com.rgremote.app.ui.RingAction
import com.rgremote.app.ui.theme.HaloSpec
import com.rgremote.app.ui.theme.LocalHaloColors
import com.rgremote.app.ui.theme.RGRemoteTheme
import com.rgremote.app.ui.theme.ThemeMode

/**
 * Deterministic previews for the standalone HaloDpad component (PR02). The
 * ring is not wired to the real screen until PR03; these fixtures verify the
 * idle/pressed/disabled visuals and the HaloSpec geometry against both themes.
 */
@Composable
private fun HaloDpadFixture(
    enabled: Boolean = true,
    forcedPressedAction: RingAction? = null,
    label: String,
) {
    val halo = LocalHaloColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(halo.backgroundTop, halo.backgroundBase, halo.backgroundDeep)
                )
            )
    ) {
        var lastAction by remember { mutableStateOf<String?>(null) }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(HaloSpec.ScreenHorizontalPaddingDp.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val diameter = minOf(
                LocalConfiguration.current.screenWidthDp * HaloSpec.RingWidthFraction,
                HaloSpec.RingMaxDiameterDp
            ).dp
            HaloDpad(
                enabled = enabled,
                onPress = { lastAction = it.name },
                onSequenceStart = { },
                onSequenceEnd = { },
                modifier = Modifier.width(diameter),
                forcedPressedAction = forcedPressedAction
            )
            Text(
                text = lastAction ?: label,
                color = halo.moreControls,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

@Preview(name = "Ring dark · idle", showBackground = true, widthDp = 412, heightDp = 620)
@Composable
private fun HaloDpadDarkIdlePreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloDpadFixture(label = "IDLE")
    }
}

@Preview(name = "Ring dark · pressed up", showBackground = true, widthDp = 412, heightDp = 620)
@Composable
private fun HaloDpadDarkPressedUpPreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloDpadFixture(forcedPressedAction = RingAction.UP, label = "PRESSED UP")
    }
}

@Preview(name = "Ring dark · pressed select", showBackground = true, widthDp = 412, heightDp = 620)
@Composable
private fun HaloDpadDarkPressedSelectPreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloDpadFixture(forcedPressedAction = RingAction.SELECT, label = "PRESSED SELECT")
    }
}

@Preview(name = "Ring dark · disabled", showBackground = true, widthDp = 412, heightDp = 620)
@Composable
private fun HaloDpadDarkDisabledPreview() {
    RGRemoteTheme(ThemeMode.DARK) {
        HaloDpadFixture(enabled = false, label = "DISABLED")
    }
}

@Preview(name = "Ring light · idle", showBackground = true, widthDp = 412, heightDp = 620)
@Composable
private fun HaloDpadLightIdlePreview() {
    RGRemoteTheme(ThemeMode.LIGHT) {
        HaloDpadFixture(label = "IDLE")
    }
}

@Preview(name = "Ring light · pressed right", showBackground = true, widthDp = 412, heightDp = 620)
@Composable
private fun HaloDpadLightPressedRightPreview() {
    RGRemoteTheme(ThemeMode.LIGHT) {
        HaloDpadFixture(forcedPressedAction = RingAction.RIGHT, label = "PRESSED RIGHT")
    }
}
