package dev.oneshot.ui

import android.graphics.PointF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

import dev.oneshot.ui.theme.OneShotTheme

@Composable
fun FocusIndicator(
    point: PointF?,
    modifier: Modifier = Modifier,
) {
    if (point == null) return

    val scale = remember(point) { Animatable(1.4f) }
    val alpha = remember(point) { Animatable(1.0f) }

    LaunchedEffect(point) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.08f,
            animationSpec = tween(durationMillis = 120)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 80)
        )
        delay(1800)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(point.x, point.y)
            val baseRadius = 34f * scale.value
            val ringColor = OneShotTheme.AmberGrain.copy(alpha = alpha.value)

            // Outer focus circle
            drawCircle(
                color = ringColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.0f)
            )

            // Inner optical rangefinder dot
            drawCircle(
                color = ringColor,
                radius = 2.5f,
                center = center
            )

            // 4 tick marks
            val tickLen = 8f
            drawLine(
                color = ringColor,
                start = Offset(center.x, center.y - baseRadius - tickLen),
                end = Offset(center.x, center.y - baseRadius + tickLen),
                strokeWidth = 2f
            )
            drawLine(
                color = ringColor,
                start = Offset(center.x, center.y + baseRadius - tickLen),
                end = Offset(center.x, center.y + baseRadius + tickLen),
                strokeWidth = 2f
            )
            drawLine(
                color = ringColor,
                start = Offset(center.x - baseRadius - tickLen, center.y),
                end = Offset(center.x - baseRadius + tickLen, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = ringColor,
                start = Offset(center.x + baseRadius - tickLen, center.y),
                end = Offset(center.x + baseRadius + tickLen, center.y),
                strokeWidth = 2f
            )
        }
    }
}
private suspend fun delay(ms: Long) = kotlinx.coroutines.delay(ms)
