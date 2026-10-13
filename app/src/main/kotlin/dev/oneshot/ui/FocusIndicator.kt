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
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(durationMillis = 150)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 100)
        )
        delay(1800)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(point.x, point.y)
            val baseRadius = 36f * scale.value
            val ringColor = Color(0xFFFFD54F).copy(alpha = alpha.value)

            // Outer focus circle
            drawCircle(
                color = ringColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.5f)
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
