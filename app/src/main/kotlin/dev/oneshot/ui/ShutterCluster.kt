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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.MediaThumbnail
import dev.oneshot.camera.RecordingStatus
import dev.oneshot.ui.theme.OneShotTheme

@Composable
fun ShutterCluster(
    status: RecordingStatus,
    onTakeStillPhoto: () -> Unit,
    onCaptureMotionPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    latestMedia: MediaThumbnail? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Saved Notice Pill (Clickable to jump directly to DCIM/OneShot in Gallery)
        AnimatedVisibility(
            visible = !status.isRecording && status.userNotice != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color(0xE01C2E1E))
                    .border(width = 0.75.dp, color = OneShotTheme.HardwareGreen.copy(alpha = 0.6f), shape = CapsuleShape)
                    .clickable(onClick = onOpenGallery)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✓ 已定格: ${status.userNotice} (查看底片)",
                    color = OneShotTheme.FilmSilver,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Video Recording Status HUD (Tally Pulse + Timecode + Real-time File size)
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
                    .background(OneShotTheme.ChassisDark)
                    .border(width = 1.dp, color = OneShotTheme.DarkroomRed.copy(alpha = 0.6f), shape = CapsuleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val transition = rememberInfiniteTransition(label = "tallyPulse")
                val pulse by transition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotPulse"
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(OneShotTheme.DarkroomRed)
                )
                Text(
                    text = "REC %02d:%02d".format(minutes, seconds),
                    color = OneShotTheme.FilmSilver,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "|  %.1f MB".format(sizeMb),
                    color = OneShotTheme.TitaniumGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Three-Shutter Bar Row (Physical Cluster Architecture)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Far Left: Film Gallery Button (Machined Chassis with live thumbnail)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(OneShotTheme.ChassisDark)
                    .border(
                        width = if (latestMedia?.bitmap != null) 1.5.dp else 1.dp,
                        color = if (latestMedia?.bitmap != null) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                        shape = CircleShape
                    )
                    .clickable(
                        enabled = !status.isRecording && !status.isMotionCapturing,
                        onClick = onOpenGallery
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (latestMedia?.bitmap != null) {
                    Image(
                        bitmap = latestMedia.bitmap.asImageBitmap(),
                        contentDescription = "最近底片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (latestMedia.isVideo) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶",
                                color = OneShotTheme.FilmSilver,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "底片",
                        color = OneShotTheme.AmberGrain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Left Shutter: Motion Photo (Live Emulsion Shutter)
            MotionPhotoShutterButton(
                isCapturing = status.isMotionCapturing,
                enabled = !status.isRecording && !status.isStillCapturing,
                onClick = onCaptureMotionPhoto
            )

            // Center Shutter: Decisive Frame Still Photography (Mechanical Shutter)
            StillPhotoShutterButton(
                isCapturing = status.isStillCapturing,
                enabled = !status.isRecording && !status.isMotionCapturing,
                onClick = onTakeStillPhoto
            )

            // Right Shutter: Video Recording (Darkroom Red Tally Shutter)
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
                    .background(OneShotTheme.ChassisDark)
                    .border(width = 1.dp, color = OneShotTheme.TitaniumBorder, shape = CircleShape)
                    .clickable(
                        enabled = !status.isRecording && !status.isMotionCapturing,
                        onClick = onFlipCamera
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "翻转",
                    color = OneShotTheme.FilmSilver,
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
            .border(width = 2.dp, color = OneShotTheme.AmberGrain.copy(alpha = 0.85f), shape = CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(OneShotTheme.AmberGrainMuted)
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
                color = OneShotTheme.AmberGrain,
                strokeWidth = 2.dp
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .border(width = 1.5.dp, color = OneShotTheme.AmberGrain, shape = CircleShape)
                )
                Text(
                    text = "LIVE",
                    color = OneShotTheme.AmberGrain,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Center Shutter: Professional dual-ring camera mechanical shutter button.
 * Outer machined steel ring + inner porcelain white button + subtle Amber Grain halo.
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
            .border(width = 3.5.dp, color = OneShotTheme.FilmSilver, shape = CircleShape)
            .padding(5.dp)
            .border(width = 1.dp, color = OneShotTheme.AmberGrain.copy(alpha = 0.5f), shape = CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
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
                color = OneShotTheme.AmberGrain,
                strokeWidth = 3.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(OneShotTheme.FilmSilver)
            )
        }
    }
}

/**
 * Right Shutter: Video record button with morphing Darkroom Red circle -> square stop transition.
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
            .border(width = 2.5.dp, color = OneShotTheme.DarkroomRed, shape = CircleShape)
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
            // Square stop icon (Darkroom Red)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(OneShotTheme.DarkroomRed)
            )
        } else {
            // Round red record dot (Darkroom Red)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(OneShotTheme.DarkroomRed)
            )
        }
    }
}

private val CapsuleShape = RoundedCornerShape(50)
