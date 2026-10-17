package dev.oneshot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * OLED Screen Dimmer Overlay during video recording.
 * Automatically dims screen after inactivity to prevent thermal throttling and reduce battery drain.
 * Displays an unobtrusive top capsule badge without obstructing the center viewfinder framing.
 * Any touch interaction instantly restores full brightness.
 */
@Composable
fun ScreenDimmerOverlay(
    isRecording: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    dimDelayMillis: Long = 3500L,
    onUserInteraction: () -> Unit = {},
) {
    if (!enabled) return

    var isDimmed by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRecording, enabled, lastInteractionTime) {
        if (!isRecording || !enabled) {
            isDimmed = false
            return@LaunchedEffect
        }

        // Wait for inactivity timeout
        delay(dimDelayMillis)
        isDimmed = true
    }

    AnimatedVisibility(
        visible = isRecording && enabled && isDimmed,
        enter = fadeIn(animationSpec = tween(600)),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9050505))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        isDimmed = false
                        lastInteractionTime = System.currentTimeMillis()
                        onUserInteraction()
                    }
                )
        ) {
            // Unobtrusive Top-Center Pill Badge (Centers of viewfinder remains 100% clean for composition)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .background(Color(0x99181818), shape = RoundedCornerShape(16.dp))
                    .border(0.5.dp, Color(0x33FFFFFF), shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00E676), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "录制降温省电中 · 轻触屏幕唤醒",
                    color = Color(0xEEFFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
