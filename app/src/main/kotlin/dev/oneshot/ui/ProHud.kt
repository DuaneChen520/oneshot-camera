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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import dev.oneshot.data.ProHudPresets
import dev.oneshot.ui.theme.OneShotTheme
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

val ALL_STANDARD_SHUTTER_SPEEDS = listOf(
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

val ALL_STANDARD_ISOS = listOf(50, 64, 100, 200, 400, 800, 1600, 3200, 6400)

val ALL_STANDARD_FOCUS_DISTANCES = listOf(
    0.0f to "∞",
    0.2f to "5.0m",
    0.33f to "3.0m",
    0.66f to "1.5m",
    1.0f to "1.0m",
    2.0f to "0.5m",
    5.0f to "0.2m",
    10.0f to "0.1m",
)

val ALL_STANDARD_CCTS = listOf(
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
    onUpdateHudPresets: ((ProHudPresets) -> ProHudPresets) -> Unit,
    onOpenWbDialog: () -> Unit,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf<ProHudTab?>(null) }
    var editingTab by remember { mutableStateOf<ProHudTab?>(null) }

    val presets = settings.hudPresets

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Upper Custom Presets Bar (Layout: [Reset/Auto] + 5 Presets + [Edit])
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
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                when (activeTab) {
                    ProHudTab.SHUTTER -> {
                        ShutterPresetBar(
                            isAuto = settings.isAutoShutter,
                            currentNanos = settings.shutterSpeedNanos,
                            presets = presets.shutterPresets.take(5),
                            onSelectAuto = {
                                onToggleAutoShutter(true)
                                hapticManager.shutterClick()
                            },
                            onSelectSpeed = { speed ->
                                onToggleAutoShutter(false)
                                onShutterSpeedChange(speed)
                                hapticManager.tick()
                            },
                            onOpenEdit = { editingTab = ProHudTab.SHUTTER }
                        )
                    }
                    ProHudTab.ISO -> {
                        IsoPresetBar(
                            isAuto = settings.isAutoIso,
                            currentIso = settings.isoValue,
                            presets = presets.isoPresets.take(5),
                            onSelectAuto = {
                                onToggleAutoIso(true)
                                hapticManager.shutterClick()
                            },
                            onSelectIso = { iso ->
                                onToggleAutoIso(false)
                                onIsoChange(iso)
                                hapticManager.tick()
                            },
                            onOpenEdit = { editingTab = ProHudTab.ISO }
                        )
                    }
                    ProHudTab.EV -> {
                        EvPresetBar(
                            currentIndex = currentEvIndex,
                            step = exposureStep,
                            presets = presets.evPresets.take(5),
                            onSelectEv = { idx ->
                                onEvChange(idx)
                                hapticManager.tick()
                            },
                            onResetZero = {
                                onEvChange(0)
                                hapticManager.shutterClick()
                            },
                            onOpenEdit = { editingTab = ProHudTab.EV }
                        )
                    }
                    ProHudTab.ZOOM -> {
                        ZoomPresetBar(
                            currentZoom = currentZoom,
                            presets = presets.zoomPresets.take(5),
                            onSelectZoom = { z ->
                                onZoomChange(z)
                                hapticManager.tick()
                            },
                            onResetOne = {
                                onZoomChange(1.0f)
                                hapticManager.shutterClick()
                            },
                            onOpenEdit = { editingTab = ProHudTab.ZOOM }
                        )
                    }
                    ProHudTab.FOCUS -> {
                        FocusPresetBar(
                            isAuto = settings.isAutoFocus,
                            currentDistance = settings.focusDistance,
                            presets = presets.focusPresets.take(5),
                            onSelectAuto = {
                                onToggleAutoFocus(true)
                                hapticManager.shutterClick()
                            },
                            onSelectDistance = { dist ->
                                onToggleAutoFocus(false)
                                onFocusDistanceChange(dist)
                                hapticManager.tick()
                            },
                            onOpenEdit = { editingTab = ProHudTab.FOCUS }
                        )
                    }
                    ProHudTab.WB -> {
                        WbPresetBar(
                            isAuto = settings.isAutoWb,
                            currentCct = settings.cctKelvin,
                            presets = presets.wbPresets.take(5),
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
                            onOpenEdit = { editingTab = ProHudTab.WB }
                        )
                    }
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
                ALL_STANDARD_SHUTTER_SPEEDS.indexOfFirst { abs(it.first - settings.shutterSpeedNanos) < 100_000L }
                    .takeIf { it >= 0 } ?: 7
            }
            InPlaceDynamicPill(
                title = "SEC",
                valueText = if (settings.isAutoShutter) "AUTO" else ALL_STANDARD_SHUTTER_SPEEDS[currentShutterIdx].second,
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
                onSwipeDelta = { deltaX, initialHoldValue ->
                    // 每移动 18px 切换 1 档快门
                    val stepPx = 18f
                    val stepsOffset = (deltaX / stepPx).toInt()
                    val targetIdx = (initialHoldValue + stepsOffset).coerceIn(0, ALL_STANDARD_SHUTTER_SPEEDS.lastIndex)
                    if (targetIdx != currentShutterIdx) {
                        onToggleAutoShutter(false)
                        onShutterSpeedChange(ALL_STANDARD_SHUTTER_SPEEDS[targetIdx].first)
                        hapticManager.tick()
                    }
                },
                getInitialHoldInt = { currentShutterIdx },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 2: 感光度 (ISO)
            val currentIsoIdx = remember(settings.isoValue) {
                ALL_STANDARD_ISOS.indexOfFirst { it == settings.isoValue }.takeIf { it >= 0 } ?: 2
            }
            InPlaceDynamicPill(
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
                onSwipeDelta = { deltaX, initialHoldValue ->
                    val stepPx = 18f
                    val stepsOffset = (deltaX / stepPx).toInt()
                    val targetIdx = (initialHoldValue + stepsOffset).coerceIn(0, ALL_STANDARD_ISOS.lastIndex)
                    if (targetIdx != currentIsoIdx) {
                        onToggleAutoIso(false)
                        onIsoChange(ALL_STANDARD_ISOS[targetIdx])
                        hapticManager.tick()
                    }
                },
                getInitialHoldInt = { currentIsoIdx },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 3: 曝光补偿 (EV)
            val evValue = currentEvIndex * exposureStep
            InPlaceDynamicPill(
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
                onSwipeDelta = { deltaX, initialHoldValue ->
                    // 每移动 16px 连续切换 1 档 EV（支持从 -4.0 到 +4.0 连续跨越多档）
                    val stepPx = 16f
                    val stepsOffset = (deltaX / stepPx).toInt()
                    val targetIdx = (initialHoldValue + stepsOffset).coerceIn(exposureRange.first, exposureRange.last)
                    if (targetIdx != currentEvIndex) {
                        onEvChange(targetIdx)
                        hapticManager.tick()
                    }
                },
                getInitialHoldInt = { currentEvIndex },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 4: 焦距 (Zoom) —— 居中突出核心 (Prominent Center)
            // 真正全范围平滑连续变焦 (从 0.7x 到长焦 120x)
            InPlaceDynamicZoomPill(
                currentZoom = currentZoom,
                zoomRange = zoomRange,
                isSelected = activeTab == ProHudTab.ZOOM,
                onTap = {
                    activeTab = if (activeTab == ProHudTab.ZOOM) null else ProHudTab.ZOOM
                    hapticManager.tick()
                },
                onLongPress = {
                    onZoomChange(1.0f)
                    hapticManager.shutterClick()
                },
                onZoomChange = onZoomChange,
                hapticManager = hapticManager,
                modifier = Modifier.weight(1.05f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 5: 对焦 (Focus / AF/MF)
            val focusText = if (settings.isAutoFocus) {
                "AF"
            } else {
                val closest = ALL_STANDARD_FOCUS_DISTANCES.minByOrNull { abs(it.first - settings.focusDistance) }
                "MF ${closest?.second ?: "%.1fd".format(settings.focusDistance)}"
            }
            val currentFocusIdx = remember(settings.focusDistance) {
                ALL_STANDARD_FOCUS_DISTANCES.indexOfFirst { abs(it.first - settings.focusDistance) < 0.15f }
                    .takeIf { it >= 0 } ?: 0
            }
            InPlaceDynamicPill(
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
                onSwipeDelta = { deltaX, initialHoldValue ->
                    val stepPx = 18f
                    val stepsOffset = (deltaX / stepPx).toInt()
                    val targetIdx = (initialHoldValue + stepsOffset).coerceIn(0, ALL_STANDARD_FOCUS_DISTANCES.lastIndex)
                    if (targetIdx != currentFocusIdx) {
                        onToggleAutoFocus(false)
                        onFocusDistanceChange(ALL_STANDARD_FOCUS_DISTANCES[targetIdx].first)
                        hapticManager.tick()
                    }
                },
                getInitialHoldInt = { currentFocusIdx },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Pill 6: 白平衡 (WB)
            val wbText = if (settings.isAutoWb) "AUTO" else "${settings.cctKelvin}K"
            InPlaceDynamicPill(
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
                onSwipeDelta = { deltaX, initialHoldValue ->
                    // 每移动 12px 增减 100K
                    val stepPx = 12f
                    val kelvinOffset = ((deltaX / stepPx).toInt()) * 100
                    val targetCct = (initialHoldValue + kelvinOffset).coerceIn(2800, 8000)
                    if (targetCct != settings.cctKelvin) {
                        onToggleAutoWb(false)
                        onCctChange(targetCct)
                        hapticManager.tick()
                    }
                },
                getInitialHoldInt = { settings.cctKelvin },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // 3. Preset Editor Dialog
    if (editingTab != null) {
        PresetEditDialog(
            tab = editingTab!!,
            presets = presets,
            exposureStep = exposureStep,
            onSave = { updatedPresets ->
                onUpdateHudPresets { updatedPresets }
                editingTab = null
                hapticManager.shutterClick()
            },
            onDismiss = { editingTab = null }
        )
    }
}

// =============================================================================
// REAL-TIME CONTINUOUS / STEP DRAG ENGINE (TRUE SWIPE ON PARAMETER PILLS)
// =============================================================================

@Composable
private fun InPlaceDynamicPill(
    title: String,
    valueText: String,
    isAuto: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeDelta: (deltaX: Float, initialHoldInt: Int) -> Unit,
    getInitialHoldInt: () -> Int,
    modifier: Modifier = Modifier,
    isNeutral: Boolean = false,
) {
    val borderColor = when {
        isSelected -> OneShotTheme.AmberGrain
        else -> OneShotTheme.TitaniumBorder.copy(alpha = 0.4f)
    }

    val backgroundColor = when {
        isSelected -> OneShotTheme.ChassisCard.copy(alpha = 0.9f)
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
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val initialVal = getInitialHoldInt()
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
                        val totalDrag = change.position.x - startX

                        if (!isDrag && !isLongPressFired && elapsed >= 400L && abs(totalDrag) < 10f) {
                            isLongPressFired = true
                            onLongPress()
                        }

                        if (abs(totalDrag) > 10f) {
                            isDrag = true
                        }

                        if (isDrag) {
                            change.consume()
                            onSwipeDelta(totalDrag, initialVal)
                        }
                    }
                }
            },
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
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

/**
 * True smooth continuous zoom pill.
 * Maps horizontal drag seamlessly from 0.7x up to 120x periscope telephoto.
 */
@Composable
private fun InPlaceDynamicZoomPill(
    currentZoom: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onZoomChange: (Float) -> Unit,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder.copy(alpha = 0.9f)
    val backgroundColor = if (isSelected) OneShotTheme.ChassisCard.copy(alpha = 0.9f) else OneShotTheme.ChassisCard
    val valueColor = if (currentZoom == 1.0f) OneShotTheme.FilmSilver else OneShotTheme.AmberGrain

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.2.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .pointerInput(zoomRange) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val initialZoom = currentZoom
                    var isDrag = false
                    var isLongPressFired = false
                    var lastTickZoom = initialZoom
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
                        val totalDrag = change.position.x - startX

                        if (!isDrag && !isLongPressFired && elapsed >= 400L && abs(totalDrag) < 10f) {
                            isLongPressFired = true
                            onLongPress()
                        }

                        if (abs(totalDrag) > 8f) {
                            isDrag = true
                        }

                        if (isDrag) {
                            change.consume()
                            // Professional logarithmic lens mapping: fine control at low zoom, smooth rapid scaling at high zoom
                            val factor = if (totalDrag >= 0) {
                                1.0f + (totalDrag / 140f) * (1.0f + totalDrag / 240f)
                            } else {
                                1.0f / (1.0f + (-totalDrag / 140f))
                            }
                            val targetZoom = (initialZoom * factor).coerceIn(zoomRange.start, zoomRange.endInclusive)
                            val rounded = (targetZoom * 10f).roundToInt() / 10f

                            onZoomChange(rounded)

                            if (abs(rounded - lastTickZoom) >= 0.5f) {
                                hapticManager.tick()
                                lastTickZoom = rounded
                            }
                        }
                    }
                }
            },
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
                    text = "ZOOM",
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
                text = "%.1fx".format(currentZoom),
                color = valueColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

// =============================================================================
// FIXED 7-ELEMENT PRESET BARS: [Default/Auto/Zero] + [P1..P5] + [Edit Button]
// =============================================================================

@Composable
private fun ShutterPresetBar(
    isAuto: Boolean,
    currentNanos: Long,
    presets: List<Long>,
    onSelectAuto: () -> Unit,
    onSelectSpeed: (Long) -> Unit,
    onOpenEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Element 1: Auto / Default
        PresetChip(
            label = "AUTO",
            isSelected = isAuto,
            isSpecial = true,
            onClick = onSelectAuto,
            modifier = Modifier.weight(1.1f)
        )

        // Elements 2..6: Presets 1 to 5
        presets.forEach { nanos ->
            val label = ALL_STANDARD_SHUTTER_SPEEDS.firstOrNull { abs(it.first - nanos) < 100_000L }?.second ?: "1/?"
            val isSelected = !isAuto && abs(nanos - currentNanos) < 100_000L
            Spacer(modifier = Modifier.width(3.dp))
            PresetChip(
                label = label,
                isSelected = isSelected,
                isSpecial = false,
                onClick = { onSelectSpeed(nanos) },
                modifier = Modifier.weight(1f)
            )
        }

        // Element 7: Edit Button
        Spacer(modifier = Modifier.width(4.dp))
        EditButton(onClick = onOpenEdit)
    }
}

@Composable
private fun IsoPresetBar(
    isAuto: Boolean,
    currentIso: Int,
    presets: List<Int>,
    onSelectAuto: () -> Unit,
    onSelectIso: (Int) -> Unit,
    onOpenEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PresetChip(
            label = "AUTO",
            isSelected = isAuto,
            isSpecial = true,
            onClick = onSelectAuto,
            modifier = Modifier.weight(1.1f)
        )

        presets.forEach { iso ->
            val isSelected = !isAuto && iso == currentIso
            Spacer(modifier = Modifier.width(3.dp))
            PresetChip(
                label = "$iso",
                isSelected = isSelected,
                isSpecial = false,
                onClick = { onSelectIso(iso) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        EditButton(onClick = onOpenEdit)
    }
}

@Composable
private fun EvPresetBar(
    currentIndex: Int,
    step: Float,
    presets: List<Int>,
    onSelectEv: (Int) -> Unit,
    onResetZero: () -> Unit,
    onOpenEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PresetChip(
            label = "±0.0",
            isSelected = currentIndex == 0,
            isSpecial = true,
            onClick = onResetZero,
            modifier = Modifier.weight(1.1f)
        )

        presets.forEach { idx ->
            val evVal = idx * step
            val isSelected = idx == currentIndex
            Spacer(modifier = Modifier.width(3.dp))
            PresetChip(
                label = "%+.1f".format(evVal),
                isSelected = isSelected,
                isSpecial = false,
                onClick = { onSelectEv(idx) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        EditButton(onClick = onOpenEdit)
    }
}

@Composable
private fun ZoomPresetBar(
    currentZoom: Float,
    presets: List<Float>,
    onSelectZoom: (Float) -> Unit,
    onResetOne: () -> Unit,
    onOpenEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PresetChip(
            label = "1.0x",
            isSelected = abs(currentZoom - 1.0f) < 0.05f,
            isSpecial = true,
            onClick = onResetOne,
            modifier = Modifier.weight(1.1f)
        )

        presets.forEach { z ->
            val isSelected = abs(currentZoom - z) < 0.05f
            Spacer(modifier = Modifier.width(3.dp))
            PresetChip(
                label = if (z >= 10f) "%.0fx".format(z) else "%.1fx".format(z),
                isSelected = isSelected,
                isSpecial = false,
                onClick = { onSelectZoom(z) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        EditButton(onClick = onOpenEdit)
    }
}

@Composable
private fun FocusPresetBar(
    isAuto: Boolean,
    currentDistance: Float,
    presets: List<Float>,
    onSelectAuto: () -> Unit,
    onSelectDistance: (Float) -> Unit,
    onOpenEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PresetChip(
            label = "AF",
            isSelected = isAuto,
            isSpecial = true,
            onClick = onSelectAuto,
            modifier = Modifier.weight(1.1f)
        )

        presets.forEach { dist ->
            val label = ALL_STANDARD_FOCUS_DISTANCES.firstOrNull { abs(it.first - dist) < 0.15f }?.second ?: "%.1fd".format(dist)
            val isSelected = !isAuto && abs(dist - currentDistance) < 0.15f
            Spacer(modifier = Modifier.width(3.dp))
            PresetChip(
                label = label,
                isSelected = isSelected,
                isSpecial = false,
                onClick = { onSelectDistance(dist) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        EditButton(onClick = onOpenEdit)
    }
}

@Composable
private fun WbPresetBar(
    isAuto: Boolean,
    currentCct: Int,
    presets: List<Int>,
    onSelectAuto: () -> Unit,
    onSelectCct: (Int) -> Unit,
    onOpen2dPad: () -> Unit,
    onOpenEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PresetChip(
            label = "AUTO",
            isSelected = isAuto,
            isSpecial = true,
            onClick = onSelectAuto,
            modifier = Modifier.weight(1.1f)
        )

        presets.forEach { cct ->
            val isSelected = !isAuto && abs(cct - currentCct) < 200
            Spacer(modifier = Modifier.width(3.dp))
            PresetChip(
                label = "${cct}K",
                isSelected = isSelected,
                isSpecial = false,
                onClick = { onSelectCct(cct) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        EditButton(onClick = onOpenEdit)
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    isSpecial: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isSelected && isSpecial -> OneShotTheme.AutoSkyBlue
        isSelected -> OneShotTheme.AmberGrain
        else -> OneShotTheme.TitaniumBorder.copy(alpha = 0.4f)
    }

    val textColor = when {
        isSelected && isSpecial -> OneShotTheme.AutoSkyBlue
        isSelected -> OneShotTheme.AmberGrain
        isSpecial -> OneShotTheme.AutoSkyBlue.copy(alpha = 0.8f)
        else -> OneShotTheme.FilmSilver
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) OneShotTheme.ChassisCard else OneShotTheme.ChassisDark)
            .border(width = 0.75.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun EditButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(OneShotTheme.ChassisCard)
            .border(width = 0.75.dp, color = OneShotTheme.AmberGrain.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⚙ 编辑",
            color = OneShotTheme.AmberGrain,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// =============================================================================
// PRESET CONFIGURATION DIALOG (UP TO 5 PRESETS PER PARAMETER)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PresetEditDialog(
    tab: ProHudTab,
    presets: ProHudPresets,
    exposureStep: Float,
    onSave: (ProHudPresets) -> Unit,
    onDismiss: () -> Unit,
) {
    var workingShutter by remember { mutableStateOf(presets.shutterPresets.toMutableList()) }
    var workingIso by remember { mutableStateOf(presets.isoPresets.toMutableList()) }
    var workingEv by remember { mutableStateOf(presets.evPresets.toMutableList()) }
    var workingZoom by remember { mutableStateOf(presets.zoomPresets.toMutableList()) }
    var workingFocus by remember { mutableStateOf(presets.focusPresets.toMutableList()) }
    var workingWb by remember { mutableStateOf(presets.wbPresets.toMutableList()) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(OneShotTheme.ChassisDark)
                .border(width = 1.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            val title = when (tab) {
                ProHudTab.SHUTTER -> "自定义快门速度预设 (上限 5 项)"
                ProHudTab.ISO -> "自定义感光度 ISO 预设 (上限 5 项)"
                ProHudTab.EV -> "自定义曝光补偿 EV 预设 (上限 5 项)"
                ProHudTab.ZOOM -> "自定义焦距倍率预设 (上限 5 项)"
                ProHudTab.FOCUS -> "自定义对焦距离预设 (上限 5 项)"
                ProHudTab.WB -> "自定义白平衡色温预设 (上限 5 项)"
            }

            Text(
                text = title,
                color = OneShotTheme.AmberGrain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "点击候选标准档位加入/移除预设，已选 ${when (tab) {
                    ProHudTab.SHUTTER -> workingShutter.size
                    ProHudTab.ISO -> workingIso.size
                    ProHudTab.EV -> workingEv.size
                    ProHudTab.ZOOM -> workingZoom.size
                    ProHudTab.FOCUS -> workingFocus.size
                    ProHudTab.WB -> workingWb.size
                }} / 5 项：",
                color = OneShotTheme.TitaniumGray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Candidate pool flow
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (tab) {
                    ProHudTab.SHUTTER -> {
                        ALL_STANDARD_SHUTTER_SPEEDS.forEach { (nanos, label) ->
                            val isSelected = workingShutter.any { abs(it - nanos) < 100_000L }
                            CandidateChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        workingShutter = workingShutter.filter { abs(it - nanos) >= 100_000L }.toMutableList()
                                    } else if (workingShutter.size < 5) {
                                        workingShutter = (workingShutter + nanos).toMutableList()
                                    }
                                }
                            )
                        }
                    }
                    ProHudTab.ISO -> {
                        ALL_STANDARD_ISOS.forEach { iso ->
                            val isSelected = workingIso.contains(iso)
                            CandidateChip(
                                label = "$iso",
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        workingIso = workingIso.filter { it != iso }.toMutableList()
                                    } else if (workingIso.size < 5) {
                                        workingIso = (workingIso + iso).toMutableList()
                                    }
                                }
                            )
                        }
                    }
                    ProHudTab.EV -> {
                        (-8..8).forEach { idx ->
                            val evVal = idx * exposureStep
                            val isSelected = workingEv.contains(idx)
                            CandidateChip(
                                label = "%+.1f".format(evVal),
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        workingEv = workingEv.filter { it != idx }.toMutableList()
                                    } else if (workingEv.size < 5) {
                                        workingEv = (workingEv + idx).toMutableList()
                                    }
                                }
                            )
                        }
                    }
                    ProHudTab.ZOOM -> {
                        listOf(0.7f, 1.0f, 1.5f, 2.0f, 2.6f, 3.0f, 5.0f, 10.0f, 20.0f, 30.0f, 60.0f, 120.0f).forEach { z ->
                            val isSelected = workingZoom.any { abs(it - z) < 0.05f }
                            CandidateChip(
                                label = "%.1fx".format(z),
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        workingZoom = workingZoom.filter { abs(it - z) >= 0.05f }.toMutableList()
                                    } else if (workingZoom.size < 5) {
                                        workingZoom = (workingZoom + z).toMutableList()
                                    }
                                }
                            )
                        }
                    }
                    ProHudTab.FOCUS -> {
                        ALL_STANDARD_FOCUS_DISTANCES.forEach { (dist, label) ->
                            val isSelected = workingFocus.any { abs(it - dist) < 0.15f }
                            CandidateChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        workingFocus = workingFocus.filter { abs(it - dist) >= 0.15f }.toMutableList()
                                    } else if (workingFocus.size < 5) {
                                        workingFocus = (workingFocus + dist).toMutableList()
                                    }
                                }
                            )
                        }
                    }
                    ProHudTab.WB -> {
                        ALL_STANDARD_CCTS.forEach { (cct, label) ->
                            val isSelected = workingWb.contains(cct)
                            CandidateChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        workingWb = workingWb.filter { it != cct }.toMutableList()
                                    } else if (workingWb.size < 5) {
                                        workingWb = (workingWb + cct).toMutableList()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset to Default
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(OneShotTheme.ChassisDark)
                        .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(4.dp))
                        .clickable {
                            val defaults = ProHudPresets()
                            when (tab) {
                                ProHudTab.SHUTTER -> workingShutter = defaults.shutterPresets.toMutableList()
                                ProHudTab.ISO -> workingIso = defaults.isoPresets.toMutableList()
                                ProHudTab.EV -> workingEv = defaults.evPresets.toMutableList()
                                ProHudTab.ZOOM -> workingZoom = defaults.zoomPresets.toMutableList()
                                ProHudTab.FOCUS -> workingFocus = defaults.focusPresets.toMutableList()
                                ProHudTab.WB -> workingWb = defaults.wbPresets.toMutableList()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = "恢复推荐默认", color = OneShotTheme.TitaniumGray, fontSize = 10.5.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OneShotTheme.ChassisDark)
                            .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(4.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "取消", color = OneShotTheme.FilmSilver, fontSize = 10.5.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OneShotTheme.AmberGrain)
                            .clickable {
                                val updated = when (tab) {
                                    ProHudTab.SHUTTER -> presets.copy(shutterPresets = workingShutter.take(5))
                                    ProHudTab.ISO -> presets.copy(isoPresets = workingIso.take(5))
                                    ProHudTab.EV -> presets.copy(evPresets = workingEv.take(5))
                                    ProHudTab.ZOOM -> presets.copy(zoomPresets = workingZoom.take(5))
                                    ProHudTab.FOCUS -> presets.copy(focusPresets = workingFocus.take(5))
                                    ProHudTab.WB -> presets.copy(wbPresets = workingWb.take(5))
                                }
                                onSave(updated)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "保存", color = OneShotTheme.ObsidianBlack, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) OneShotTheme.AmberGrain.copy(alpha = 0.2f) else OneShotTheme.ChassisCard)
            .border(
                width = 0.75.dp,
                color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
