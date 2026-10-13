package dev.oneshot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.oneshot.camera.HapticManager
import kotlin.math.roundToInt

import dev.oneshot.ui.theme.OneShotTheme

@Composable
fun AbgmTintDialog(
    isAuto: Boolean,
    cctKelvin: Int,
    tintGm: Int,
    onSetAuto: () -> Unit,
    onSetWb: (kelvin: Int, tint: Int) -> Unit,
    onDismiss: () -> Unit,
    hapticManager: HapticManager,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OneShotTheme.ChassisDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OneShotTheme.TitaniumBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ABGM 影视级白平衡调色盘",
                    color = OneShotTheme.FilmSilver,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAuto) "当前状态: 自动白平衡 (AWB)" else "${cctKelvin}K  |  Tint: %s%d".format(if (tintGm >= 0) "M+" else "G", tintGm),
                    color = if (isAuto) OneShotTheme.AutoSkyBlue else OneShotTheme.AmberGrain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2D Interactive Color Pad
                AbgmPad(
                    cctKelvin = cctKelvin,
                    tintGm = tintGm,
                    onUpdate = { k, t ->
                        onSetWb(k, t)
                        hapticManager.tick()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Presets row: Day 5600K, Tungsten 3200K, Fluorescent 4000K, Cloudy 6500K
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WbPresetChip(label = "日光 5600K", onClick = { onSetWb(5600, 0); hapticManager.tick() })
                    WbPresetChip(label = "钨丝 3200K", onClick = { onSetWb(3200, 0); hapticManager.tick() })
                    WbPresetChip(label = "荧光 4000K", onClick = { onSetWb(4000, 10); hapticManager.tick() })
                    WbPresetChip(label = "阴天 6500K", onClick = { onSetWb(6500, 0); hapticManager.tick() })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Reset to Auto, Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            onSetAuto()
                            hapticManager.shutterClick()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OneShotTheme.ChassisCard),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "恢复 AUTO", color = OneShotTheme.AutoSkyBlue, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = OneShotTheme.AmberGrain),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "完成设定", color = OneShotTheme.ObsidianBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AbgmPad(
    cctKelvin: Int,
    tintGm: Int,
    onUpdate: (kelvin: Int, tint: Int) -> Unit,
) {
    val padSize = 220.dp

    Box(
        modifier = Modifier
            .size(padSize)
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = Color(0x66FFFFFF), shape = RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .size(padSize)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val k = offsetToKelvin(offset.x, size.width.toFloat())
                        val t = offsetToTint(offset.y, size.height.toFloat())
                        onUpdate(k, t)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val k = offsetToKelvin(change.position.x, size.width.toFloat())
                        val t = offsetToTint(change.position.y, size.height.toFloat())
                        onUpdate(k, t)
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // Background horizontal gradient: Amber (Warm Yellow) to Blue (Cold)
            val amberColor = Color(0xFFFFB74D)
            val blueColor = Color(0xFF64B5F6)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(amberColor, Color(0xFFEEEEEE), blueColor),
                    startX = 0f,
                    endX = width
                )
            )

            // Vertical tint overlay: Green at top to Magenta at bottom
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x884CAF50), Color(0x00000000), Color(0x88E91E63)),
                    startY = 0f,
                    endY = height
                )
            )

            // Center crosshairs
            drawLine(
                color = Color(0x55000000),
                start = Offset(0f, height / 2f),
                end = Offset(width, height / 2f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0x55000000),
                start = Offset(width / 2f, 0f),
                end = Offset(width / 2f, height),
                strokeWidth = 2f
            )

            // Target crosshair indicator dot
            val normX = ((cctKelvin - 2800f) / (8000f - 2800f)).coerceIn(0f, 1f)
            val targetX = normX * width

            // tintGm: -100 (top Green) to +100 (bottom Magenta)
            val normY = ((tintGm - (-100f)) / (100f - (-100f))).coerceIn(0f, 1f)
            val targetY = normY * height

            drawCircle(
                color = Color.Black,
                radius = 10f,
                center = Offset(targetX, targetY)
            )
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = Offset(targetX, targetY)
            )
        }
    }
}

private fun offsetToKelvin(x: Float, width: Float): Int {
    val norm = (x / width).coerceIn(0f, 1f)
    return (2800 + norm * (8000 - 2800)).roundToInt()
}

private fun offsetToTint(y: Float, height: Float): Int {
    val norm = (y / height).coerceIn(0f, 1f)
    return (-100 + norm * 200).roundToInt()
}

@Composable
private fun WbPresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OneShotTheme.ChassisCard)
            .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
    ) {
        Text(text = label, color = OneShotTheme.FilmSilver, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
