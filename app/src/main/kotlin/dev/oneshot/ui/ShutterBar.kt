package dev.oneshot.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.RecordingStatus

@Composable
fun ShutterBar(
    status: RecordingStatus,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Saved Notice Pill
        if (!status.isRecording && status.userNotice != null) {
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
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Recording Status Info (Timer + File size)
        if (status.isRecording) {
            val minutes = status.durationSeconds / 60
            val seconds = status.durationSeconds % 60
            val sizeMb = status.recordedBytes.toFloat() / (1024 * 1024)

            Row(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color(0xCC000000))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing red dot
                val transition = rememberInfiniteTransition()
                val pulse by transition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    )
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action row: Left Gallery, Center Shutter, Right Flip Camera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Gallery / Photos shortcut
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(width = 1.dp, color = Color(0x66FFFFFF), shape = CircleShape)
                    .clickable(enabled = !status.isRecording, onClick = onOpenGallery),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "相册", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Center: Big Shutter / Record Button
            RecordButton(
                isRecording = status.isRecording,
                onClick = {
                    if (status.isRecording) onStopRecording() else onStartRecording()
                }
            )

            // Right: Flip Camera
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable(enabled = !status.isRecording, onClick = onFlipCamera),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "翻转", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .border(width = 4.dp, color = Color.White, shape = CircleShape)
            .padding(6.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Square stop button
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF1744))
            )
        } else {
            // Round red record dot
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF1744))
            )
        }
    }
}

private val CapsuleShape = RoundedCornerShape(50)
