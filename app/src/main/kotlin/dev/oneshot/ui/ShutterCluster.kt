package dev.oneshot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.RecordingStatus

@Composable
fun ShutterCluster(
    status: RecordingStatus,
    onTakeStillPhoto: () -> Unit,
    onCaptureMotionPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Saved Notice Pill (clickable to jump directly to DCIM/OneShot in Gallery)
        AnimatedVisibility(
            visible = !status.isRecording && status.userNotice != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color(0xDD2E7D32))
                    .clickable(onClick = onOpenGallery)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✓ ${status.userNotice} (点击查看)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Video Recording Status HUD (Timer + Real-time File size)
        AnimatedVisibility(
            visible = status.isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val minutes = status.durationSeconds / 60
            val seconds = status.durationSeconds % 60
            val sizeMb = status.recordedBytes.toFloat() / (1024 * 1024)

            Row(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color(0xCC000000))
                    .border(width = 1.dp, color = Color(0x44FF1744), shape = CapsuleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val transition = rememberInfiniteTransition(label = "pulse")
                val pulse by transition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotPulse"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Color(0xFFFF1744))
                )
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "|  %.1f MB".format(sizeMb),
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Three-Shutter Bar Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Far Left: Gallery Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x33222222))
                    .border(width = 1.dp, color = Color(0x66FFFFFF), shape = CircleShape)
                    .clickable(
                        enabled = !status.isRecording && !status.isMotionCapturing,
                        onClick = onOpenGallery
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "相册",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Left Shutter: Motion Photo (Live Photo)
            MotionPhotoShutterButton(
                isCapturing = status.isMotionCapturing,
                enabled = !status.isRecording && !status.isStillCapturing,
                onClick = onCaptureMotionPhoto
            )

            // Center Shutter: High-Spec Still Photography
            StillPhotoShutterButton(
                isCapturing = status.isStillCapturing,
                enabled = !status.isRecording && !status.isMotionCapturing,
                onClick = onTakeStillPhoto
            )

            // Right Shutter: Video Recording
            VideoShutterButton(
                isRecording = status.isRecording,
                enabled = !status.isStillCapturing && !status.isMotionCapturing,
                onClick = {
                    if (status.isRecording) onStopRecording() else onStartRecording()
                }
            )

            // Far Right: Lens Flip Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x33222222))
                    .border(width = 1.dp, color = Color(0x44FFFFFF), shape = CircleShape)
                    .clickable(
                        enabled = !status.isRecording && !status.isMotionCapturing,
                        onClick = onFlipCamera
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "翻转",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Left Shutter: Motion Photo button with LIVE badge and concentric ring aesthetics.
 */
@Composable
private fun MotionPhotoShutterButton(
    isCapturing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1.0f, label = "motionScale")

    Box(
        modifier = Modifier
            .size(54.dp)
            .scale(scale)
            .alpha(if (enabled) 1.0f else 0.3f)
            .border(width = 2.dp, color = Color(0xAAFFD54F), shape = CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color(0x22FFD54F))
            .clickable(
                enabled = enabled && !isCapturing,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFFFFD54F),
                strokeWidth = 2.dp
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .border(width = 1.5.dp, color = Color(0xFFFFD54F), shape = CircleShape)
                )
                Text(
                    text = "LIVE",
                    color = Color(0xFFFFD54F),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Center Shutter: Professional dual-ring camera shutter button.
 */
@Composable
private fun StillPhotoShutterButton(
    isCapturing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.90f else 1.0f, label = "stillScale")

    Box(
        modifier = Modifier
            .size(78.dp)
            .scale(scale)
            .alpha(if (enabled) 1.0f else 0.25f)
            .border(width = 4.dp, color = Color.White, shape = CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(
                enabled = enabled && !isCapturing,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Right Shutter: Video record button with morphing red circle -> square stop transition.
 */
@Composable
private fun VideoShutterButton(
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1.0f, label = "videoScale")

    Box(
        modifier = Modifier
            .size(54.dp)
            .scale(scale)
            .alpha(if (enabled) 1.0f else 0.3f)
            .border(width = 2.5.dp, color = Color(0xFFFF1744), shape = CircleShape)
            .padding(4.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Square stop icon
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFFF1744))
            )
        } else {
            // Round red record dot
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF1744))
            )
        }
    }
}

private val CapsuleShape = RoundedCornerShape(50)
