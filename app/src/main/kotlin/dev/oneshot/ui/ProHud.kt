package dev.oneshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.HapticManager
import dev.oneshot.data.CameraSettings
import kotlin.math.roundToInt

private val STANDARD_SHUTTER_SPEEDS = listOf(
    1_000_000_000L / 8000L to "1/8000",
    1_000_000_000L / 4000L to "1/4000",
    1_000_000_000L / 2000L to "1/2000",
    1_000_000_000L / 1000L to "1/1000",
    1_000_000_000L / 500L to "1/500",
    1_000_000_000L / 250L to "1/250",
    1_000_000_000L / 125L to "1/125",
    1_000_000_000L / 60L to "1/60",
    1_000_000_000L / 30L to "1/30",
    1_000_000_000L / 15L to "1/15",
    1_000_000_000L / 8L to "1/8",
    1_000_000_000L / 4L to "1/4",
    1_000_000_000L / 2L to "1/2",
    1_000_000_000L to "1s",
)

private val STANDARD_ISOS = listOf(50, 64, 100, 200, 400, 800, 1600, 3200, 6400)

@Composable
fun ProHud(
    settings: CameraSettings,
    zoomRange: ClosedFloatingPointRange<Float>,
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    exposureRange: IntRange,
    exposureStep: Float,
    currentEvIndex: Int,
    onEvChange: (Int) -> Unit,
    onToggleAutoShutter: (Boolean) -> Unit,
    onShutterSpeedChange: (Long) -> Unit,
    onToggleAutoIso: (Boolean) -> Unit,
    onIsoChange: (Int) -> Unit,
    onToggleAutoWb: (Boolean) -> Unit,
    onCctChange: (Int) -> Unit,
    onOpenWbDialog: () -> Unit,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // In-place parameter pills row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC111111))
                .border(width = 0.5.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. ZOOM Parameter Pill (Drag left/right to zoom, double-tap to 1.0x)
            InPlaceZoomPill(
                zoom = currentZoom,
                range = zoomRange,
                onZoomChange = onZoomChange,
                hapticManager = hapticManager
            )

            // 2. EV Exposure Compensation Pill (Drag to change EV, long press resets to 0)
            InPlaceEvPill(
                currentEvIndex = currentEvIndex,
                exposureRange = exposureRange,
                exposureStep = exposureStep,
                onEvChange = onEvChange,
                hapticManager = hapticManager
            )

            // 3. Shutter Speed Pill (Drag to step through speeds, long press resets to AUTO)
            InPlaceShutterPill(
                isAuto = settings.isAutoShutter,
                shutterSpeedNanos = settings.shutterSpeedNanos,
                onToggleAuto = onToggleAutoShutter,
                onSpeedChange = onShutterSpeedChange,
                hapticManager = hapticManager
            )

            // 4. ISO Sensitivity Pill (Drag to step ISO, long press resets to AUTO)
            InPlaceIsoPill(
                isAuto = settings.isAutoIso,
                isoValue = settings.isoValue,
                onToggleAuto = onToggleAutoIso,
                onIsoChange = onIsoChange,
                hapticManager = hapticManager
            )

            // 5. White Balance Pill (Drag to adjust CCT, long press resets to AUTO, tap opens 2D ABGM pad)
            InPlaceWbPill(
                isAuto = settings.isAutoWb,
                cctKelvin = settings.cctKelvin,
                onToggleAuto = onToggleAutoWb,
                onCctChange = onCctChange,
                onOpenPad = onOpenWbDialog,
                hapticManager = hapticManager
            )
        }
    }
}

@Composable
private fun InPlaceZoomPill(
    zoom: Float,
    range: ClosedFloatingPointRange<Float>,
    onZoomChange: (Float) -> Unit,
    hapticManager: HapticManager,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33333333))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onZoomChange(1.0f)
                        hapticManager.tick()
                    }
                )
            }
            .pointerInput(zoom, range) {
                detectDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount.x
                        if (kotlin.math.abs(accumulatedDrag) >= 15f) {
                            val delta = (accumulatedDrag / 15f) * 0.1f
                            val nextZoom = (zoom + delta).coerceIn(range.start, range.endInclusive)
                            onZoomChange(nextZoom)
                            accumulatedDrag = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "ZOOM", color = Color(0x88FFFFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "%.1fx".format(zoom),
                color = Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun InPlaceEvPill(
    currentEvIndex: Int,
    exposureRange: IntRange,
    exposureStep: Float,
    onEvChange: (Int) -> Unit,
    hapticManager: HapticManager,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val evValue = currentEvIndex * exposureStep

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33333333))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onEvChange(0)
                        hapticManager.shutterClick()
                    }
                )
            }
            .pointerInput(currentEvIndex, exposureRange) {
                detectDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount.x
                        if (accumulatedDrag >= 20f) {
                            val next = (currentEvIndex + 1).coerceAtMost(exposureRange.last)
                            if (next != currentEvIndex) onEvChange(next)
                            accumulatedDrag = 0f
                        } else if (accumulatedDrag <= -20f) {
                            val next = (currentEvIndex - 1).coerceAtLeast(exposureRange.first)
                            if (next != currentEvIndex) onEvChange(next)
                            accumulatedDrag = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "EV", color = Color(0x88FFFFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "%+.1f".format(evValue),
                color = if (currentEvIndex == 0) Color.White else Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun InPlaceShutterPill(
    isAuto: Boolean,
    shutterSpeedNanos: Long,
    onToggleAuto: (Boolean) -> Unit,
    onSpeedChange: (Long) -> Unit,
    hapticManager: HapticManager,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val currentIndex = remember(shutterSpeedNanos) {
        STANDARD_SHUTTER_SPEEDS.indexOfFirst { kotlin.math.abs(it.first - shutterSpeedNanos) < 100_000L }
            .takeIf { it >= 0 } ?: 7
    }
    val speedLabel = if (isAuto) "AUTO" else STANDARD_SHUTTER_SPEEDS[currentIndex].second

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33333333))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .pointerInput(isAuto) {
                detectTapGestures(
                    onLongPress = {
                        onToggleAuto(true)
                        hapticManager.shutterClick()
                    },
                    onTap = {
                        if (isAuto) {
                            onToggleAuto(false)
                            onSpeedChange(STANDARD_SHUTTER_SPEEDS[7].first) // 1/60 default
                        } else {
                            onToggleAuto(true)
                        }
                        hapticManager.tick()
                    }
                )
            }
            .pointerInput(isAuto, currentIndex) {
                detectDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount.x
                        if (accumulatedDrag >= 25f) {
                            if (isAuto) onToggleAuto(false)
                            val nextIdx = (currentIndex + 1).coerceAtMost(STANDARD_SHUTTER_SPEEDS.lastIndex)
                            onSpeedChange(STANDARD_SHUTTER_SPEEDS[nextIdx].first)
                            accumulatedDrag = 0f
                        } else if (accumulatedDrag <= -25f) {
                            if (isAuto) onToggleAuto(false)
                            val nextIdx = (currentIndex - 1).coerceAtLeast(0)
                            onSpeedChange(STANDARD_SHUTTER_SPEEDS[nextIdx].first)
                            accumulatedDrag = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "SEC", color = Color(0x88FFFFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(
                text = speedLabel,
                color = if (isAuto) Color(0xFF64B5F6) else Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun InPlaceIsoPill(
    isAuto: Boolean,
    isoValue: Int,
    onToggleAuto: (Boolean) -> Unit,
    onIsoChange: (Int) -> Unit,
    hapticManager: HapticManager,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val currentIndex = remember(isoValue) {
        STANDARD_ISOS.indexOfFirst { it == isoValue }.takeIf { it >= 0 } ?: 2
    }
    val isoLabel = if (isAuto) "AUTO" else "$isoValue"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33333333))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .pointerInput(isAuto) {
                detectTapGestures(
                    onLongPress = {
                        onToggleAuto(true)
                        hapticManager.shutterClick()
                    },
                    onTap = {
                        if (isAuto) {
                            onToggleAuto(false)
                            onIsoChange(100)
                        } else {
                            onToggleAuto(true)
                        }
                        hapticManager.tick()
                    }
                )
            }
            .pointerInput(isAuto, currentIndex) {
                detectDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount.x
                        if (accumulatedDrag >= 25f) {
                            if (isAuto) onToggleAuto(false)
                            val nextIdx = (currentIndex + 1).coerceAtMost(STANDARD_ISOS.lastIndex)
                            onIsoChange(STANDARD_ISOS[nextIdx])
                            accumulatedDrag = 0f
                        } else if (accumulatedDrag <= -25f) {
                            if (isAuto) onToggleAuto(false)
                            val nextIdx = (currentIndex - 1).coerceAtLeast(0)
                            onIsoChange(STANDARD_ISOS[nextIdx])
                            accumulatedDrag = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "ISO", color = Color(0x88FFFFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(
                text = isoLabel,
                color = if (isAuto) Color(0xFF64B5F6) else Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun InPlaceWbPill(
    isAuto: Boolean,
    cctKelvin: Int,
    onToggleAuto: (Boolean) -> Unit,
    onCctChange: (Int) -> Unit,
    onOpenPad: () -> Unit,
    hapticManager: HapticManager,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val wbLabel = if (isAuto) "AUTO" else "${cctKelvin}K"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33333333))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .pointerInput(isAuto) {
                detectTapGestures(
                    onLongPress = {
                        onToggleAuto(true)
                        hapticManager.shutterClick()
                    },
                    onTap = {
                        onOpenPad()
                        hapticManager.tick()
                    }
                )
            }
            .pointerInput(isAuto, cctKelvin) {
                detectDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount.x
                        if (accumulatedDrag >= 20f) {
                            if (isAuto) onToggleAuto(false)
                            val next = (cctKelvin + 100).coerceAtMost(8000)
                            onCctChange(next)
                            accumulatedDrag = 0f
                        } else if (accumulatedDrag <= -20f) {
                            if (isAuto) onToggleAuto(false)
                            val next = (cctKelvin - 100).coerceAtLeast(2800)
                            onCctChange(next)
                            accumulatedDrag = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "WB", color = Color(0x88FFFFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(
                text = wbLabel,
                color = if (isAuto) Color(0xFF64B5F6) else Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
