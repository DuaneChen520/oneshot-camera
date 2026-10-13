package dev.oneshot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.HapticManager
import dev.oneshot.data.CameraSettings
import dev.oneshot.ui.theme.OneShotTheme
import kotlin.math.abs
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

private val STANDARD_FOCUS_DISTANCES = listOf(
    0.0f to "∞",
    0.33f to "3.0m",
    1.0f to "1.0m",
    2.0f to "0.5m",
    5.0f to "0.2m",
    10.0f to "0.1m",
)

private val STANDARD_CCTS = listOf(
    2800 to "2800K",
    3200 to "3200K",
    4000 to "4000K",
    5000 to "5000K",
    5500 to "5500K",
    6500 to "6500K",
    7500 to "7500K",
)

enum class ProHudTab {
    SHUTTER,
    ISO,
    EV,
    ZOOM,
    FOCUS,
    WB,
}

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
    onToggleAutoFocus: (Boolean) -> Unit,
    onFocusDistanceChange: (Float) -> Unit,
    onToggleAutoWb: (Boolean) -> Unit,
    onCctChange: (Int) -> Unit,
    onOpenWbDialog: () -> Unit,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf<ProHudTab?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Upper Precision Stepper Dial Strip (Expands on active tab)
        AnimatedVisibility(
            visible = activeTab != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OneShotTheme.ChassisDark.copy(alpha = 0.95f))
                    .border(width = 0.75.dp, color = OneShotTheme.AmberGrain.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                when (activeTab) {
                    ProHudTab.SHUTTER -> ShutterDialStrip(
                        isAuto = settings.isAutoShutter,
                        currentNanos = settings.shutterSpeedNanos,
                        onSelectAuto = {
                            onToggleAutoShutter(true)
                            hapticManager.shutterClick()
                        },
                        onSelectSpeed = { speed ->
                            onToggleAutoShutter(false)
                            onShutterSpeedChange(speed)
                            hapticManager.tick()
                        },
                        onClose = { activeTab = null }
                    )
                    ProHudTab.ISO -> IsoDialStrip(
                        isAuto = settings.isAutoIso,
                        currentIso = settings.isoValue,
                        onSelectAuto = {
                            onToggleAutoIso(true)
                            hapticManager.shutterClick()
                        },
                        onSelectIso = { iso ->
                            onToggleAutoIso(false)
                            onIsoChange(iso)
                            hapticManager.tick()
                        },
                        onClose = { activeTab = null }
                    )
                    ProHudTab.EV -> EvDialStrip(
                        currentIndex = currentEvIndex,
                        range = exposureRange,
                        step = exposureStep,
                        onSelectEv = { idx ->
                            onEvChange(idx)
                            hapticManager.tick()
                        },
                        onResetZero = {
                            onEvChange(0)
                            hapticManager.shutterClick()
                        },
                        onClose = { activeTab = null }
                    )
                    ProHudTab.ZOOM -> ZoomDialStrip(
                        currentZoom = currentZoom,
                        range = zoomRange,
                        onZoomChange = { zoom ->
                            onZoomChange(zoom)
                            hapticManager.tick()
                        },
                        onResetOne = {
                            onZoomChange(1.0f)
                            hapticManager.shutterClick()
                        },
                        onClose = { activeTab = null }
                    )
                    ProHudTab.FOCUS -> FocusDialStrip(
                        isAuto = settings.isAutoFocus,
                        currentDistance = settings.focusDistance,
                        onSelectAuto = {
                            onToggleAutoFocus(true)
                            hapticManager.shutterClick()
                        },
                        onSelectDistance = { dist ->
                            onToggleAutoFocus(false)
                            onFocusDistanceChange(dist)
                            hapticManager.tick()
                        },
                        onClose = { activeTab = null }
                    )
                    ProHudTab.WB -> WbDialStrip(
                        isAuto = settings.isAutoWb,
                        currentCct = settings.cctKelvin,
                        onSelectAuto = {
                            onToggleAutoWb(true)
                            hapticManager.shutterClick()
                        },
                        onSelectCct = { cct ->
                            onToggleAutoWb(false)
                            onCctChange(cct)
                            hapticManager.tick()
                        },
                        onOpen2dPad = {
                            onOpenWbDialog()
                            hapticManager.tick()
                        },
                        onClose = { activeTab = null }
                    )
                    null -> {}
                }
            }
        }

        // 2. Precision 6-Pill Mechanical Chassis
        // Strict sequence: [快门 S] -> [感光度 ISO] -> [曝光补偿 EV] -> [焦距 ZOOM (Center)] -> [对焦 FOCUS] -> [白平衡 WB]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(OneShotTheme.ChassisDark)
                .border(width = 0.75.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill 1: 快门 (Shutter / S)
            val currentShutterIdx = remember(settings.shutterSpeedNanos) {
                STANDARD_SHUTTER_SPEEDS.indexOfFirst { abs(it.first - settings.shutterSpeedNanos) < 100_000L }
                    .takeIf { it >= 0 } ?: 7
            }
            InPlaceHudPill(
                title = "SEC",
                valueText = if (settings.isAutoShutter) "AUTO" else STANDARD_SHUTTER_SPEEDS[currentShutterIdx].second,
                isAuto = settings.isAutoShutter,
                isSelected = activeTab == ProHudTab.SHUTTER,
                onTap = {
                    activeTab = if (activeTab == ProHudTab.SHUTTER) null else ProHudTab.SHUTTER
                    hapticManager.tick()
                },
                onLongPress = {
                    onToggleAutoShutter(true)
                    hapticManager.shutterClick()
                },
                onSwipeLeft = {
                    // Shorter exposure (faster speed)
                    val next = (currentShutterIdx - 1).coerceAtLeast(0)
                    onToggleAutoShutter(false)
                    onShutterSpeedChange(STANDARD_SHUTTER_SPEEDS[next].first)
                    hapticManager.tick()
                },
                onSwipeRight = {
                    // Longer exposure (slower speed)
                    val next = (currentShutterIdx + 1).coerceAtMost(STANDARD_SHUTTER_SPEEDS.lastIndex)
                    onToggleAutoShutter(false)
                    onShutterSpeedChange(STANDARD_SHUTTER_SPEEDS[next].first)
                    hapticManager.tick()
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 2: 感光度 (ISO)
            val currentIsoIdx = remember(settings.isoValue) {
                STANDARD_ISOS.indexOfFirst { it == settings.isoValue }.takeIf { it >= 0 } ?: 2
            }
            InPlaceHudPill(
                title = "ISO",
                valueText = if (settings.isAutoIso) "AUTO" else "${settings.isoValue}",
                isAuto = settings.isAutoIso,
                isSelected = activeTab == ProHudTab.ISO,
                onTap = {
                    activeTab = if (activeTab == ProHudTab.ISO) null else ProHudTab.ISO
                    hapticManager.tick()
                },
                onLongPress = {
                    onToggleAutoIso(true)
                    hapticManager.shutterClick()
                },
                onSwipeLeft = {
                    val next = (currentIsoIdx - 1).coerceAtLeast(0)
                    onToggleAutoIso(false)
                    onIsoChange(STANDARD_ISOS[next])
                    hapticManager.tick()
                },
                onSwipeRight = {
                    val next = (currentIsoIdx + 1).coerceAtMost(STANDARD_ISOS.lastIndex)
                    onToggleAutoIso(false)
                    onIsoChange(STANDARD_ISOS[next])
                    hapticManager.tick()
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 3: 曝光补偿 (EV)
            val evValue = currentEvIndex * exposureStep
            InPlaceHudPill(
                title = "EV",
                valueText = "%+.1f".format(evValue),
                isAuto = false,
                isNeutral = currentEvIndex == 0,
                isSelected = activeTab == ProHudTab.EV,
                onTap = {
                    activeTab = if (activeTab == ProHudTab.EV) null else ProHudTab.EV
                    hapticManager.tick()
                },
                onLongPress = {
                    onEvChange(0)
                    hapticManager.shutterClick()
                },
                onSwipeLeft = {
                    val next = (currentEvIndex - 1).coerceAtLeast(exposureRange.first)
                    if (next != currentEvIndex) {
                        onEvChange(next)
                        hapticManager.tick()
                    }
                },
                onSwipeRight = {
                    val next = (currentEvIndex + 1).coerceAtMost(exposureRange.last)
                    if (next != currentEvIndex) {
                        onEvChange(next)
                        hapticManager.tick()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 4: 焦距 (Zoom) —— 居中突出核心 (Prominent Center)
            InPlaceHudPill(
                title = "ZOOM",
                valueText = "%.1fx".format(currentZoom),
                isAuto = false,
                isNeutral = currentZoom == 1.0f,
                isSelected = activeTab == ProHudTab.ZOOM,
                isCenterProminent = true,
                onTap = {
                    activeTab = if (activeTab == ProHudTab.ZOOM) null else ProHudTab.ZOOM
                    hapticManager.tick()
                },
                onLongPress = {
                    onZoomChange(1.0f)
                    hapticManager.shutterClick()
                },
                onSwipeLeft = {
                    val next = (currentZoom - 0.2f).coerceIn(zoomRange.start, zoomRange.endInclusive)
                    onZoomChange(next)
                    hapticManager.tick()
                },
                onSwipeRight = {
                    val next = (currentZoom + 0.2f).coerceIn(zoomRange.start, zoomRange.endInclusive)
                    onZoomChange(next)
                    hapticManager.tick()
                },
                modifier = Modifier.weight(1.05f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 5: 对焦 (Focus / AF/MF) —— 补齐核心参数
            val focusText = if (settings.isAutoFocus) {
                "AF"
            } else {
                val closest = STANDARD_FOCUS_DISTANCES.minByOrNull { abs(it.first - settings.focusDistance) }
                "MF ${closest?.second ?: "%.1fd".format(settings.focusDistance)}"
            }
            val currentFocusIdx = remember(settings.focusDistance) {
                STANDARD_FOCUS_DISTANCES.indexOfFirst { abs(it.first - settings.focusDistance) < 0.15f }
                    .takeIf { it >= 0 } ?: 0
            }
            InPlaceHudPill(
                title = "FOCUS",
                valueText = focusText,
                isAuto = settings.isAutoFocus,
                isSelected = activeTab == ProHudTab.FOCUS,
                onTap = {
                    activeTab = if (activeTab == ProHudTab.FOCUS) null else ProHudTab.FOCUS
                    hapticManager.tick()
                },
                onLongPress = {
                    onToggleAutoFocus(true)
                    hapticManager.shutterClick()
                },
                onSwipeLeft = {
                    // Focus towards infinity (lower diopters)
                    val next = (currentFocusIdx - 1).coerceAtLeast(0)
                    onToggleAutoFocus(false)
                    onFocusDistanceChange(STANDARD_FOCUS_DISTANCES[next].first)
                    hapticManager.tick()
                },
                onSwipeRight = {
                    // Focus towards macro (higher diopters)
                    val next = (currentFocusIdx + 1).coerceAtMost(STANDARD_FOCUS_DISTANCES.lastIndex)
                    onToggleAutoFocus(false)
                    onFocusDistanceChange(STANDARD_FOCUS_DISTANCES[next].first)
                    hapticManager.tick()
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 6: 白平衡 (WB)
            val wbText = if (settings.isAutoWb) "AUTO" else "${settings.cctKelvin}K"
            val currentCctIdx = remember(settings.cctKelvin) {
                STANDARD_CCTS.indexOfFirst { abs(it.first - settings.cctKelvin) < 300 }.takeIf { it >= 0 } ?: 4
            }
            InPlaceHudPill(
                title = "WB",
                valueText = wbText,
                isAuto = settings.isAutoWb,
                isSelected = activeTab == ProHudTab.WB,
                onTap = {
                    activeTab = if (activeTab == ProHudTab.WB) null else ProHudTab.WB
                    hapticManager.tick()
                },
                onLongPress = {
                    onToggleAutoWb(true)
                    hapticManager.shutterClick()
                },
                onSwipeLeft = {
                    val next = (currentCctIdx - 1).coerceAtLeast(0)
                    onToggleAutoWb(false)
                    onCctChange(STANDARD_CCTS[next].first)
                    hapticManager.tick()
                },
                onSwipeRight = {
                    val next = (currentCctIdx + 1).coerceAtMost(STANDARD_CCTS.lastIndex)
                    onToggleAutoWb(false)
                    onCctChange(STANDARD_CCTS[next].first)
                    hapticManager.tick()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * High-precision single-touch gesture detector for parameter pills.
 * Guarantees zero gesture-conflict between horizontal swipe, tap, and long press.
 */
private fun Modifier.proHudInteractivePill(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    stepThresholdPx: Float = 26f,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalDrag = 0f
        var isDrag = false
        var isLongPressFired = false
        val startTime = System.currentTimeMillis()

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break

            if (!change.pressed) {
                if (!isDrag && !isLongPressFired) {
                    onTap()
                }
                break
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (!isDrag && !isLongPressFired && elapsed >= 420L && abs(totalDrag) < 12f) {
                isLongPressFired = true
                onLongPress()
            }

            val deltaX = change.position.x - change.previousPosition.x
            totalDrag += deltaX

            if (abs(totalDrag) > 10f) {
                isDrag = true
            }

            if (isDrag) {
                change.consume()
                if (totalDrag >= stepThresholdPx) {
                    onSwipeRight()
                    totalDrag = 0f
                } else if (totalDrag <= -stepThresholdPx) {
                    onSwipeLeft()
                    totalDrag = 0f
                }
            }
        }
    }
}

@Composable
private fun InPlaceHudPill(
    title: String,
    valueText: String,
    isAuto: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    isNeutral: Boolean = false,
    isCenterProminent: Boolean = false,
) {
    val borderColor = when {
        isSelected -> OneShotTheme.AmberGrain
        isCenterProminent -> OneShotTheme.TitaniumBorder.copy(alpha = 0.9f)
        else -> OneShotTheme.TitaniumBorder.copy(alpha = 0.4f)
    }

    val backgroundColor = when {
        isSelected -> OneShotTheme.ChassisCard.copy(alpha = 0.9f)
        isCenterProminent -> OneShotTheme.ChassisCard
        else -> OneShotTheme.ChassisDark
    }

    val valueColor = when {
        isAuto -> OneShotTheme.AutoSkyBlue
        isNeutral -> OneShotTheme.FilmSilver
        else -> OneShotTheme.AmberGrain
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.2.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .proHudInteractivePill(
                onTap = onTap,
                onLongPress = onLongPress,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(OneShotTheme.AmberGrain)
                    )
                }
            }

            Text(
                text = valueText,
                color = valueColor,
                fontSize = if (isCenterProminent) 11.5.sp else 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

// =============================================================================
// EXPANDED PRECISION MECHANICAL DIAL STRIPS (Continuous Ruler & Steppers)
// =============================================================================

@Composable
private fun ShutterDialStrip(
    isAuto: Boolean,
    currentNanos: Long,
    onSelectAuto: () -> Unit,
    onSelectSpeed: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Auto Chip
        DialChip(
            label = "AUTO",
            isSelected = isAuto,
            isAutoStyle = true,
            onClick = onSelectAuto
        )

        // Shutter Speeds
        STANDARD_SHUTTER_SPEEDS.forEach { (nanos, label) ->
            val selected = !isAuto && abs(nanos - currentNanos) < 100_000L
            DialChip(
                label = label,
                isSelected = selected,
                isAutoStyle = false,
                onClick = { onSelectSpeed(nanos) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        CloseDialButton(onClick = onClose)
    }
}

@Composable
private fun IsoDialStrip(
    isAuto: Boolean,
    currentIso: Int,
    onSelectAuto: () -> Unit,
    onSelectIso: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DialChip(
            label = "AUTO",
            isSelected = isAuto,
            isAutoStyle = true,
            onClick = onSelectAuto
        )

        STANDARD_ISOS.forEach { iso ->
            val selected = !isAuto && iso == currentIso
            DialChip(
                label = "$iso",
                isSelected = selected,
                isAutoStyle = false,
                onClick = { onSelectIso(iso) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        CloseDialButton(onClick = onClose)
    }
}

@Composable
private fun EvDialStrip(
    currentIndex: Int,
    range: IntRange,
    step: Float,
    onSelectEv: (Int) -> Unit,
    onResetZero: () -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DialChip(
            label = "0.0 归零",
            isSelected = currentIndex == 0,
            isAutoStyle = false,
            onClick = onResetZero
        )

        for (idx in range.first..range.last) {
            val evVal = idx * step
            DialChip(
                label = "%+.1f".format(evVal),
                isSelected = idx == currentIndex,
                isAutoStyle = false,
                onClick = { onSelectEv(idx) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        CloseDialButton(onClick = onClose)
    }
}

@Composable
private fun ZoomDialStrip(
    currentZoom: Float,
    range: ClosedFloatingPointRange<Float>,
    onZoomChange: (Float) -> Unit,
    onResetOne: () -> Unit,
    onClose: () -> Unit,
) {
    val presets = listOf(0.6f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
        .filter { it in range.start..range.endInclusive }
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DialChip(
            label = "1.0x 标准",
            isSelected = abs(currentZoom - 1.0f) < 0.05f,
            isAutoStyle = false,
            onClick = onResetOne
        )

        presets.forEach { z ->
            val selected = abs(currentZoom - z) < 0.05f
            DialChip(
                label = "%.1fx".format(z),
                isSelected = selected,
                isAutoStyle = false,
                onClick = { onZoomChange(z) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        CloseDialButton(onClick = onClose)
    }
}

@Composable
private fun FocusDialStrip(
    isAuto: Boolean,
    currentDistance: Float,
    onSelectAuto: () -> Unit,
    onSelectDistance: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DialChip(
            label = "AF 自动",
            isSelected = isAuto,
            isAutoStyle = true,
            onClick = onSelectAuto
        )

        STANDARD_FOCUS_DISTANCES.forEach { (dist, label) ->
            val selected = !isAuto && abs(dist - currentDistance) < 0.15f
            DialChip(
                label = "MF $label",
                isSelected = selected,
                isAutoStyle = false,
                onClick = { onSelectDistance(dist) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        CloseDialButton(onClick = onClose)
    }
}

@Composable
private fun WbDialStrip(
    isAuto: Boolean,
    currentCct: Int,
    onSelectAuto: () -> Unit,
    onSelectCct: (Int) -> Unit,
    onOpen2dPad: () -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DialChip(
            label = "AUTO",
            isSelected = isAuto,
            isAutoStyle = true,
            onClick = onSelectAuto
        )

        STANDARD_CCTS.forEach { (cct, label) ->
            val selected = !isAuto && abs(cct - currentCct) < 250
            DialChip(
                label = label,
                isSelected = selected,
                isAutoStyle = false,
                onClick = { onSelectCct(cct) }
            )
        }

        DialChip(
            label = "2D 调色盘",
            isSelected = false,
            isAutoStyle = false,
            onClick = onOpen2dPad
        )

        Spacer(modifier = Modifier.width(4.dp))
        CloseDialButton(onClick = onClose)
    }
}

@Composable
private fun DialChip(
    label: String,
    isSelected: Boolean,
    isAutoStyle: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        isSelected && isAutoStyle -> OneShotTheme.AutoSkyBlue
        isSelected -> OneShotTheme.AmberGrain
        else -> OneShotTheme.TitaniumBorder.copy(alpha = 0.5f)
    }

    val textColor = when {
        isSelected && isAutoStyle -> OneShotTheme.AutoSkyBlue
        isSelected -> OneShotTheme.AmberGrain
        else -> OneShotTheme.FilmSilver
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) OneShotTheme.ChassisCard else OneShotTheme.ChassisDark)
            .border(width = 0.75.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CloseDialButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(OneShotTheme.ChassisDark)
            .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✕",
            color = OneShotTheme.TitaniumGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
