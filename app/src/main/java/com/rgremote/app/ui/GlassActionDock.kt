package com.rgremote.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rgremote.app.R
import com.rgremote.app.ui.ringGradientFill
import com.rgremote.app.ui.theme.HaloColors
import com.rgremote.app.ui.theme.HaloSpec
import com.rgremote.app.ui.theme.ringGradientColors
import com.rgremote.app.ui.theme.LocalHaloColors

private const val DisabledAlpha = 0.35f
private const val AsleepAlpha = 0f

/**
 * The mockup's segmented glass dock: Back | Home | Power in one floating
 * capsule with hairline separators and circular icon wells.
 */
@Composable
internal fun GlassActionDock(
    enabled: Boolean,
    /** False when the screen is idle: the dock dims and stops responding. */
    awake: Boolean,
    power: PowerPresentation,
    powerEnabled: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onPower: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val halo = LocalHaloColors.current
    val shape = RoundedCornerShape(HaloSpec.DockHeightDp.dp / 2)
    val dockAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> DisabledAlpha
            awake -> 1f
            else -> AsleepAlpha
        },
        animationSpec = tween(durationMillis = 300),
        label = "dockAwake"
    )
    Row(
        modifier = modifier
            .height(HaloSpec.DockHeightDp.dp)
            // alpha BEFORE background/border so the whole capsule (not just
            // the icons) fades — drawing after alpha() escapes the layer.
            .alpha(dockAlpha)
            .background(halo.dockFill, shape)
            .border(1.dp, halo.dockBorder, shape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DockSegment(
            enabled = enabled && awake,
            onAction = onBack,
            contentDescription = stringResource(R.string.remote_back)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.ringGradientFill(halo.ringGradientColors())
            )
        }
        DockSeparator()
        DockSegment(
            enabled = enabled && awake,
            onAction = onHome,
            contentDescription = stringResource(R.string.remote_home)
        ) {
            Icon(
                Icons.Filled.Home,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.ringGradientFill(halo.ringGradientColors())
            )
        }
        DockSeparator()
        val powerTint = when {
            power.isWake -> halo.statusOnline
            power.isUncertain -> halo.moreControls
            else -> MaterialTheme.colorScheme.error
        }
        DockSegment(
            enabled = enabled && awake && powerEnabled,
            onAction = onPower,
            contentDescription = if (power.isWake) {
                stringResource(R.string.remote_wake)
            } else {
                stringResource(R.string.remote_power)
            },
            wellColor = halo.powerWellTint
        ) {
            Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, tint = powerTint)
        }
    }
}

@Composable
private fun DockSegment(
    enabled: Boolean,
    onAction: () -> Unit,
    contentDescription: String,
    wellColor: Color = LocalHaloColors.current.dockWell,
    content: @Composable () -> Unit,
) {
    val halo = LocalHaloColors.current
    Box(
        modifier = Modifier
            .size(HaloSpec.DockWellDp.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .background(wellColor, CircleShape)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onAction),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DockSeparator() {
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight(0.4f)
            .background(LocalHaloColors.current.dockSeparator)
    )
}
