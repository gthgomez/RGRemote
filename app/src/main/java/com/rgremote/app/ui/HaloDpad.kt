package com.rgremote.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rgremote.app.ui.theme.HaloColors
import com.rgremote.app.ui.theme.HaloSpec
import com.rgremote.app.ui.theme.LocalHaloColors
import androidx.compose.ui.input.pointer.changedToUp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/** The five zones of the halo ring. */
enum class RingAction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    SELECT,
}

/**
 * Pure hit-testing contract for the halo ring (see `docs/ui-target.md`):
 * the inner [HaloSpec.SelectZoneRadiusFraction] of the radius is Select, the
 * rest of the circle is split into four sectors by |dx| >= |dy|, and anything
 * outside the outer radius misses.
 */
fun hitTestRing(x: Float, y: Float, diameter: Float): RingAction? {
    if (diameter <= 0f) return null
    val radius = diameter / 2f
    val dx = x - radius
    val dy = y - radius
    val distance = hypot(dx, dy) / radius
    if (distance > 1f) return null
    if (distance <= HaloSpec.SelectZoneRadiusFraction) return RingAction.SELECT
    return if (abs(dx) >= abs(dy)) {
        if (dx >= 0) RingAction.RIGHT else RingAction.LEFT
    } else {
        if (dy >= 0) RingAction.DOWN else RingAction.UP
    }
}

/**
 * The halo ring D-pad: one continuous glowing circle that behaves as five
 * touch zones. Drawing is independent of command dispatch — callers translate
 * [RingAction] presses into ecosystem commands.
 *
 * Gesture contract: the first press fires immediately, repeats after
 * [HoldRepeatInitialDelayMillis] every [HoldRepeatIntervalMillis], and
 * [onSequenceEnd] always fires on release/cancel so callers can drop queued
 * repeats. Accessibility exposes five independent actions; TalkBack never sees
 * an unlabeled circle.
 *
 * [forcedPressedAction] exists for deterministic previews/screenshots only.
 */
@Composable
fun HaloDpad(
    enabled: Boolean,
    onPress: (RingAction) -> Unit,
    onSequenceStart: () -> Unit,
    onSequenceEnd: () -> Unit,
    modifier: Modifier = Modifier,
    forcedPressedAction: RingAction? = null,
) {
    val halo = LocalHaloColors.current
    var pressed by rememberSaveable { mutableStateOf<RingAction?>(null) }
    val shown = forcedPressedAction ?: pressed
    val scope = rememberCoroutineScope()
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnSequenceStart by rememberUpdatedState(onSequenceStart)
    val currentOnSequenceEnd by rememberUpdatedState(onSequenceEnd)
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    fun flash(action: RingAction) {
        pressed = action
        currentOnPress(action)
        scope.launch {
            delay(150)
            if (pressed == action) pressed = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            repeatJob?.cancel()
            repeatJob = null
        }
    }

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(enabled) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                down.consume()
                val action = hitTestRing(down.position.x, down.position.y, size.width.toFloat())
                    ?: return@awaitEachGesture
                pressed = action
                currentOnSequenceStart()
                currentOnPress(action)
                repeatJob?.cancel()
                repeatJob = scope.launch {
                    delay(HoldRepeatInitialDelayMillis)
                    while (isActive) {
                        currentOnPress(action)
                        delay(HoldRepeatIntervalMillis)
                    }
                }
                try {
                    val slop = viewConfiguration.touchSlop
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (change.changedToUp()) break
                        if (change.positionChanged() &&
                            (change.position.x < -slop || change.position.x > size.width + slop ||
                                change.position.y < -slop || change.position.y > size.height + slop)
                        ) break
                    }
                } finally {
                    repeatJob?.cancel()
                    repeatJob = null
                    pressed = null
                    currentOnSequenceEnd()
                }
            }
        }
    } else {
        Modifier
    }

    val semanticsModifier = if (enabled) {
        Modifier.semantics {
            contentDescription = "Directional pad ring"
            customActions = RingAction.values().map { action ->
                CustomAccessibilityAction(action.accessibilityLabel()) {
                    flash(action)
                    true
                }
            }
        }
    } else {
        Modifier.semantics {
            contentDescription = "Remote controls disabled"
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(gestureModifier)
            .then(semanticsModifier)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (enabled) 1f else DisabledAlpha)
        ) {
            drawHaloRing(halo, shown)
        }
    }
}

private const val DisabledAlpha = 0.35f

private fun RingAction.accessibilityLabel(): String = when (this) {
    RingAction.UP -> "Navigate up"
    RingAction.DOWN -> "Navigate down"
    RingAction.LEFT -> "Navigate left"
    RingAction.RIGHT -> "Navigate right"
    RingAction.SELECT -> "Select"
}

internal fun DrawScope.drawHaloRing(halo: HaloColors, pressed: RingAction?) {
    val radius = size.minDimension / 2f
    val center = Offset(radius, radius)
    val coreStroke = radius * 2f * HaloSpec.RingCoreStrokeFraction
    val sweep = Brush.sweepGradient(
        0.00f to halo.ringEdge,
        0.12f to halo.ringViolet,
        0.25f to halo.ringVioletCore,
        0.42f to halo.ringBlend,
        0.60f to halo.ringCyan,
        0.75f to halo.ringCyanCore,
        0.90f to halo.ringCyan,
        1.00f to halo.ringEdge
    )
    // Soft glow shoulder under the bright core.
    drawCircle(
        brush = sweep,
        radius = radius - coreStroke * 0.9f,
        center = center,
        style = Stroke(width = coreStroke * 1.8f, cap = StrokeCap.Round),
        alpha = 0.35f
    )
    drawCircle(
        brush = sweep,
        radius = radius - coreStroke * 0.5f,
        center = center,
        style = Stroke(width = coreStroke, cap = StrokeCap.Round)
    )
    val discRadius = radius * HaloSpec.CenterDiscFraction
    drawCircle(color = halo.ringCenterFill, radius = discRadius, center = center)
    if (pressed == RingAction.SELECT) {
        drawCircle(
            color = halo.ringCyanCore,
            radius = discRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    } else {
        drawCircle(
            color = halo.ringCenterBorder,
            radius = discRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
    if (pressed != null && pressed != RingAction.SELECT) {
        drawDirectionArrow(pressed, center, radius, discRadius, halo)
    }
}

/** Draws a small solid triangle in the pressed sector, between disc and ring. */
private fun DrawScope.drawDirectionArrow(
    action: RingAction,
    center: Offset,
    ringRadius: Float,
    discRadius: Float,
    halo: HaloColors,
) {
    val angle = when (action) {
        RingAction.UP -> 0f
        RingAction.RIGHT -> 90f
        RingAction.DOWN -> 180f
        RingAction.LEFT -> 270f
        RingAction.SELECT -> 0f
    }
    val distance = discRadius + (ringRadius - discRadius) / 2f
    val size = ringRadius * 0.12f
    val path = Path().apply {
        moveTo(center.x, center.y - distance - size)
        lineTo(center.x - size, center.y - distance + size)
        lineTo(center.x + size, center.y - distance + size)
        close()
    }
    rotate(degrees = angle, pivot = center) {
        drawPath(path, color = halo.ringCyanCore)
    }
}
