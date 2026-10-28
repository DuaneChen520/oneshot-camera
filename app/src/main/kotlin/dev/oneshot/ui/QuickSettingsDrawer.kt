package dev.oneshot.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import dev.oneshot.camera.DeviceCapabilities
import dev.oneshot.camera.HapticManager
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.ColorSpacePreset
import dev.oneshot.data.CustomLut
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.HistogramMode
import dev.oneshot.data.LutPreset
import dev.oneshot.data.MotionDurationPreset
import dev.oneshot.data.PhotoFormat
import dev.oneshot.data.QMenuDefaults
import dev.oneshot.data.QSlotItem
import dev.oneshot.data.QualityPreset
import dev.oneshot.data.StillResolutionPreset
import dev.oneshot.data.VideoBitrateHelper
import dev.oneshot.data.VideoCodec
import dev.oneshot.data.VideoStabilizationMode
import dev.oneshot.data.VolumeKeyAction
import dev.oneshot.ui.theme.CinemaIcon
import dev.oneshot.ui.theme.OneShotTheme
import dev.oneshot.ui.theme.iconRes

enum class QMenuTab(val label: String, val icon: String) {
    HOME("常用矩阵", "🎛️"),
    FILM("胶卷色彩", "🎞️"),
    FORMAT("格式编码", "📐"),
    ASSIST("拍摄辅助", "🎯"),
}

@Composable
fun QuickSettingsDrawer(
    isOpen: Boolean,
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onClose: () -> Unit,
    onSetAspect: (AspectRatioMode) -> Unit,
    onSetQuality: (QualityPreset) -> Unit,
    onSetStillResolution: (StillResolutionPreset) -> Unit = {},
    onSetFps: (FpsPreset) -> Unit,
    onSetVideoBitrate: (Int) -> Unit = {},
    onSetVideoCodec: (VideoCodec) -> Unit = {},
    onSetPhotoFormat: (PhotoFormat) -> Unit = {},
    onSetVideoStabilization: (VideoStabilizationMode) -> Unit = {},
    onToggleScreenDimmer: (Boolean) -> Unit = {},
    onSetDynamicRange: (DynamicRangeMode) -> Unit,
    onToggleDcg: (Boolean) -> Unit,
    onToggleUltraHdr: (Boolean) -> Unit,
    onSetMotionDuration: (MotionDurationPreset) -> Unit,
    onSetVolumeUp: (VolumeKeyAction) -> Unit,
    onSetVolumeDown: (VolumeKeyAction) -> Unit,
    onSetColorSpace: (ColorSpacePreset) -> Unit,
    onToggleLut: (Boolean) -> Unit,
    onSetLutPreset: (LutPreset) -> Unit,
    onSelectCustomLut: (String) -> Unit = {},
    onImportLutClick: () -> Unit = {},
    onDeleteCustomLut: (CustomLut) -> Unit = {},
    onToggleLutBurnIn: (Boolean) -> Unit,
    onToggleFocusPeaking: (Boolean) -> Unit,
    onToggleZebraPattern: (Boolean) -> Unit,
    onSetHistogramMode: (HistogramMode) -> Unit = {},
    onSetQMenuSlots: (List<QSlotItem>) -> Unit = {},
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(QMenuTab.HOME) }
    var isEditMode by remember { mutableStateOf(false) }
    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Semi-transparent backdrop scrim (tap outside to dismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    hapticManager.tick()
                    isEditMode = false
                    editingSlotIndex = null
                    onClose()
                },
            contentAlignment = Alignment.Center
        ) {
            // 2. Centered Square Console Window (1:1 Aspect Ratio)
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(OneShotTheme.ChassisDark)
                    .border(
                        width = 1.dp,
                        color = OneShotTheme.TitaniumBorder,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Prevent click-through to backdrop
                    }
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar: Brand Identity + Close Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(OneShotTheme.AmberGrain)
                            )
                            Text(
                                text = "${OneShotTheme.BRAND_NAME} · Q 控制台",
                                color = OneShotTheme.FilmSilver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        // Close Pill button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(OneShotTheme.ChassisCard)
                                .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = CircleShape)
                                .clickable {
                                    hapticManager.tick()
                                    isEditMode = false
                                    editingSlotIndex = null
                                    onClose()
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✕",
                                color = OneShotTheme.AmberGrain,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Multi-Tab Navigation Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        QMenuTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        hapticManager.tick()
                                        selectedTab = tab
                                        isEditMode = false
                                        editingSlotIndex = null
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Content Viewport
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            QMenuTab.HOME -> {
                                if (editingSlotIndex != null) {
                                    // Slot item selection overlay
                                    SlotItemPickerView(
                                        slotIndex = editingSlotIndex!!,
                                        currentSlot = settings.qMenuSlots.getOrElse(editingSlotIndex!!) { QSlotItem.ASPECT_RATIO },
                                        onSelectSlot = { newItem ->
                                            hapticManager.tick()
                                            val updated = settings.qMenuSlots.toMutableList()
                                            if (editingSlotIndex!! < updated.size) {
                                                updated[editingSlotIndex!!] = newItem
                                            } else {
                                                updated.add(newItem)
                                            }
                                            onSetQMenuSlots(updated.take(QMenuDefaults.MAX_SLOTS))
                                            editingSlotIndex = null
                                        },
                                        onCancel = {
                                            hapticManager.tick()
                                            editingSlotIndex = null
                                        }
                                    )
                                } else {
                                    HomeMatrixView(
                                        slots = settings.qMenuSlots,
                                        settings = settings,
                                        capabilities = capabilities,
                                        isEditMode = isEditMode,
                                        onToggleEditMode = {
                                            hapticManager.tick()
                                            isEditMode = !isEditMode
                                        },
                                        onSlotClick = { index, slotItem ->
                                            if (isEditMode) {
                                                hapticManager.tick()
                                                editingSlotIndex = index
                                            } else {
                                                hapticManager.tick()
                                                cycleSlot(
                                                    item = slotItem,
                                                    settings = settings,
                                                    capabilities = capabilities,
                                                    onSetAspect = onSetAspect,
                                                    onSetQuality = onSetQuality,
                                                    onSetFps = onSetFps,
                                                    onSetVideoCodec = onSetVideoCodec,
                                                    onSetPhotoFormat = onSetPhotoFormat,
                                                    onSetDynamicRange = onSetDynamicRange,
                                                    onToggleDcg = onToggleDcg,
                                                    onToggleUltraHdr = onToggleUltraHdr,
                                                    onSetMotionDuration = onSetMotionDuration,
                                                    onSetColorSpace = onSetColorSpace,
                                                    onToggleLut = onToggleLut,
                                                    onSetLutPreset = onSetLutPreset,
                                                    onSelectCustomLut = onSelectCustomLut,
                                                    onToggleLutBurnIn = onToggleLutBurnIn,
                                                    onToggleFocusPeaking = onToggleFocusPeaking,
                                                    onToggleZebraPattern = onToggleZebraPattern,
                                                    onSetHistogramMode = onSetHistogramMode,
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            QMenuTab.FILM -> {
                                FilmColorView(
                                    settings = settings,
                                    onToggleLut = onToggleLut,
                                    onSetLutPreset = onSetLutPreset,
                                    onSelectCustomLut = onSelectCustomLut,
                                    onImportLutClick = onImportLutClick,
                                    onDeleteCustomLut = onDeleteCustomLut,
                                    onToggleLutBurnIn = onToggleLutBurnIn,
                                    hapticManager = hapticManager
                                )
                            }
                            QMenuTab.FORMAT -> {
                                FormatOpticsView(
                                    settings = settings,
                                    capabilities = capabilities,
                                    onSetAspect = onSetAspect,
                                    onSetQuality = onSetQuality,
                                    onSetStillResolution = onSetStillResolution,
                                    onSetFps = onSetFps,
                                    onSetVideoCodec = onSetVideoCodec,
                                    onSetPhotoFormat = onSetPhotoFormat,
                                    onSetVideoBitrate = onSetVideoBitrate,
                                    onSetVideoStabilization = onSetVideoStabilization,
                                    hapticManager = hapticManager
                                )
                            }
                            QMenuTab.ASSIST -> {
                                AssistHardwareView(
                                    settings = settings,
                                    capabilities = capabilities,
                                    onToggleFocusPeaking = onToggleFocusPeaking,
                                    onToggleZebraPattern = onToggleZebraPattern,
                                    onSetHistogramMode = onSetHistogramMode,
                                    onSetMotionDuration = onSetMotionDuration,
                                    onSetVolumeUp = onSetVolumeUp,
                                    onSetVolumeDown = onSetVolumeDown,
                                    onToggleScreenDimmer = onToggleScreenDimmer,
                                    hapticManager = hapticManager
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMatrixView(
    slots: List<QSlotItem>,
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onSlotClick: (Int, QSlotItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Section Header with Edit mode toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditMode) "点击方格替换功能" else "常用矩阵 (单击切换)",
                color = if (isEditMode) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isEditMode) OneShotTheme.AmberGrain else OneShotTheme.ChassisCard)
                    .border(
                        width = 0.5.dp,
                        color = if (isEditMode) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable(onClick = onToggleEditMode)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isEditMode) "✓ 完成" else "⚙️ 自定义",
                    color = if (isEditMode) OneShotTheme.ObsidianBlack else OneShotTheme.AmberGrain,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2 Rows × 3 Columns Grid of rectangular tiles
        val safeSlots = remember(slots) {
            slots.take(6).let { current ->
                if (current.size < 6) {
                    current + QMenuDefaults.DEFAULT_SLOTS.drop(current.size).take(6 - current.size)
                } else current
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1 (Slots 0, 1, 2)
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0..2) {
                    val slotItem = safeSlots.getOrElse(i) { QSlotItem.ASPECT_RATIO }
                    MatrixSlotTile(
                        item = slotItem,
                        displayValue = getSlotDisplayValue(slotItem, settings),
                        isEditMode = isEditMode,
                        modifier = Modifier.weight(1f),
                        onClick = { onSlotClick(i, slotItem) }
                    )
                }
            }

            // Row 2 (Slots 3, 4, 5)
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 3..5) {
                    val slotItem = safeSlots.getOrElse(i) { QSlotItem.FOCUS_PEAKING }
                    MatrixSlotTile(
                        item = slotItem,
                        displayValue = getSlotDisplayValue(slotItem, settings),
                        isEditMode = isEditMode,
                        modifier = Modifier.weight(1f),
                        onClick = { onSlotClick(i, slotItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixSlotTile(
    item: QSlotItem,
    displayValue: String,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(OneShotTheme.ChassisCard)
            .border(
                width = if (isEditMode) 1.dp else 0.75.dp,
                color = if (isEditMode) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon + Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                CinemaIcon(
                    resId = item.iconRes,
                    contentDescription = item.label,
                    size = 12.dp,
                    tint = OneShotTheme.TitaniumLight
                )
                Text(
                    text = item.label,
                    color = OneShotTheme.TitaniumGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Value capsule badge or Edit badge
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(OneShotTheme.AmberGrainMuted)
                        .border(0.5.dp, OneShotTheme.AmberGrain, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "替换 ↻",
                        color = OneShotTheme.AmberGrain,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0x33F5A623))
                        .border(0.5.dp, OneShotTheme.AmberGrain.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = displayValue,
                        color = OneShotTheme.AmberGrain,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotItemPickerView(
    slotIndex: Int,
    currentSlot: QSlotItem,
    onSelectSlot: (QSlotItem) -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择插槽 #${slotIndex + 1} 功能",
                color = OneShotTheme.FilmSilver,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(OneShotTheme.ChassisCard)
                    .border(0.5.dp, OneShotTheme.TitaniumBorder, RoundedCornerShape(6.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "取消",
                    color = OneShotTheme.TitaniumGray,
                    fontSize = 10.sp
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(QSlotItem.entries) { item ->
                val isCurrent = item == currentSlot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
                        .border(
                            width = 1.dp,
                            color = if (isCurrent) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectSlot(item) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CinemaIcon(
                            resId = item.iconRes,
                            contentDescription = item.label,
                            size = 15.dp,
                            tint = if (isCurrent) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver
                        )
                        Text(
                            text = item.label,
                            color = if (isCurrent) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilmColorView(
    settings: CameraSettings,
    onToggleLut: (Boolean) -> Unit,
    onSetLutPreset: (LutPreset) -> Unit,
    onSelectCustomLut: (String) -> Unit,
    onImportLutClick: () -> Unit,
    onDeleteCustomLut: (CustomLut) -> Unit,
    onToggleLutBurnIn: (Boolean) -> Unit,
    hapticManager: HapticManager,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Title: 35mm Canister Shelf
        Text(
            text = "35mm 实体胶卷仓 (Film Shelf)",
            color = OneShotTheme.TitaniumGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Horizontal 35mm Canisters Shelf
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Canister 0: Direct Optics (No LUT)
            FilmCanisterCard(
                brand = "OPTICS",
                name = "原色直出",
                subtitle = "RAW NEUTRAL",
                accentColor = OneShotTheme.FilmSilver,
                isSelected = !settings.lutEnabled,
                onClick = {
                    hapticManager.tick()
                    onToggleLut(false)
                }
            )

            // Canister 1: Kodak Portra 400
            FilmCanisterCard(
                brand = "KODAK",
                name = "PORTRA 400",
                subtitle = "WARM SKIN",
                accentColor = Color(0xFFF5A623),
                isSelected = settings.lutEnabled && settings.selectedCustomLutId == null && settings.lutPreset == LutPreset.KODAK_PORTRA,
                onClick = {
                    hapticManager.tick()
                    if (!settings.lutEnabled) onToggleLut(true)
                    onSetLutPreset(LutPreset.KODAK_PORTRA)
                }
            )

            // Canister 2: Cine 250 / Teal & Orange
            FilmCanisterCard(
                brand = "CINEMA",
                name = "CINE 250",
                subtitle = "TEAL & ORANGE",
                accentColor = Color(0xFF00E5FF),
                isSelected = settings.lutEnabled && settings.selectedCustomLutId == null && settings.lutPreset == LutPreset.TEAL_AND_ORANGE,
                onClick = {
                    hapticManager.tick()
                    if (!settings.lutEnabled) onToggleLut(true)
                    onSetLutPreset(LutPreset.TEAL_AND_ORANGE)
                }
            )

            // Canister 3: CineLog Flat
            FilmCanisterCard(
                brand = "LOG",
                name = "CINELOG",
                subtitle = "WIDE LATITUDE",
                accentColor = Color(0xFF90A4AE),
                isSelected = settings.lutEnabled && settings.selectedCustomLutId == null && settings.lutPreset == LutPreset.FLAT_LOG,
                onClick = {
                    hapticManager.tick()
                    if (!settings.lutEnabled) onToggleLut(true)
                    onSetLutPreset(LutPreset.FLAT_LOG)
                }
            )

            // User Imported Custom LUTs
            settings.customLuts.forEach { customLut ->
                val isSelected = settings.lutEnabled && settings.selectedCustomLutId == customLut.id
                FilmCanisterCard(
                    brand = "CUSTOM",
                    name = customLut.name.take(10),
                    subtitle = "${customLut.size}³ CUBE",
                    accentColor = OneShotTheme.AmberGrain,
                    isSelected = isSelected,
                    onDelete = {
                        hapticManager.tick()
                        onDeleteCustomLut(customLut)
                    },
                    onClick = {
                        hapticManager.tick()
                        if (!settings.lutEnabled) onToggleLut(true)
                        onSelectCustomLut(customLut.id)
                    }
                )
            }

            // Import .cube Action Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OneShotTheme.ChassisCard)
                    .border(1.dp, OneShotTheme.AmberGrain.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable {
                        hapticManager.tick()
                        onImportLutClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "+", color = OneShotTheme.AmberGrain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "导入 .cube", color = OneShotTheme.AmberGrain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dual-Throw Switch: LUT Burn-In vs Preview Only
        Text(
            text = "色彩流向机制 (Signal Pipeline)",
            color = OneShotTheme.TitaniumGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!settings.lutBurnIn) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
                    .border(
                        width = 1.dp,
                        color = if (!settings.lutBurnIn) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        hapticManager.tick()
                        onToggleLutBurnIn(false)
                    }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "仅监看 (Preview)",
                    color = if (!settings.lutBurnIn) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                    fontSize = 11.sp,
                    fontWeight = if (!settings.lutBurnIn) FontWeight.Bold else FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (settings.lutBurnIn) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
                    .border(
                        width = 1.dp,
                        color = if (settings.lutBurnIn) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        hapticManager.tick()
                        onToggleLutBurnIn(true)
                    }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "直出写盘 (Burn-in)",
                    color = if (settings.lutBurnIn) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                    fontSize = 11.sp,
                    fontWeight = if (settings.lutBurnIn) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Text(
            text = if (settings.lutBurnIn) "🔥 写盘模式：3D LUT 烘焙固化进录制 MP4 视频与 JPEG 照片中"
                   else "👁️ 监看模式：仅在屏幕取景器预览色彩风格，写入磁盘保留 Raw 色彩",
            color = OneShotTheme.TitaniumGray,
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun FilmCanisterCard(
    brand: String,
    name: String,
    subtitle: String,
    accentColor: Color,
    isSelected: Boolean,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
            .border(
                width = 1.dp,
                color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = brand,
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x338E8E93))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(text = "✕", color = OneShotTheme.TitaniumGray, fontSize = 8.sp)
                    }
                }
            }

            Text(
                text = name,
                color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                color = OneShotTheme.TitaniumGray,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FormatOpticsView(
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onSetAspect: (AspectRatioMode) -> Unit,
    onSetQuality: (QualityPreset) -> Unit,
    onSetStillResolution: (StillResolutionPreset) -> Unit = {},
    onSetFps: (FpsPreset) -> Unit,
    onSetVideoCodec: (VideoCodec) -> Unit,
    onSetPhotoFormat: (PhotoFormat) -> Unit,
    onSetVideoBitrate: (Int) -> Unit,
    onSetVideoStabilization: (VideoStabilizationMode) -> Unit,
    hapticManager: HapticManager,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Aspect Ratio Ladder
        DrawerSectionTitle("取景与画幅比例 (Aspect Ratio)")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AspectRatioMode.entries.forEach { mode ->
                DrawerTile(
                    title = mode.label,
                    isSelected = settings.aspectMode == mode,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        hapticManager.tick()
                        onSetAspect(mode)
                    }
                )
            }
        }

        // 2. Video Stabilization Mode
        DrawerSectionTitle("视频防抖模式 (Stabilization)")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VideoStabilizationMode.entries.forEach { mode ->
                DrawerTile(
                    title = mode.shortLabel + " · " + when (mode) {
                        VideoStabilizationMode.STANDARD -> "纯光学"
                        VideoStabilizationMode.ENHANCED -> "光学+电子"
                        VideoStabilizationMode.OFF -> "关闭"
                    },
                    isSelected = settings.videoStabilization == mode,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        hapticManager.tick()
                        onSetVideoStabilization(mode)
                    }
                )
            }
        }

        // 3. Still Photo Resolution (Decoupled from Video)
        DrawerSectionTitle("静态拍照分辨率 (Still Resolution · 独立调节)")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StillResolutionPreset.entries.forEach { res ->
                DrawerTile(
                    title = "${res.label} (${res.approxMegaPixels})",
                    isSelected = settings.stillResolution == res,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        hapticManager.tick()
                        onSetStillResolution(res)
                    }
                )
            }
        }

        // 4. Video Resolution Ladder
        DrawerSectionTitle("视频画质天梯 (Video Quality · 独立调节)")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            capabilities.supportedQualities.forEach { q ->
                DrawerTile(
                    title = q.label,
                    isSelected = settings.quality == q,
                    onClick = {
                        hapticManager.tick()
                        onSetQuality(q)
                    }
                )
            }
        }

        // 4. Frame Rate Ladder
        DrawerSectionTitle("电影帧率 (Frame Rate)")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            capabilities.supportedFpsPresets.forEach { fps ->
                DrawerTile(
                    title = fps.label,
                    isSelected = settings.fpsPreset == fps,
                    onClick = {
                        hapticManager.tick()
                        onSetFps(fps)
                    }
                )
            }
        }

        // 4. Video Codec & Photo Format
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                DrawerSectionTitle("视频编码 (Codec)")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    VideoCodec.entries.forEach { codec ->
                        DrawerTile(
                            title = if (codec == VideoCodec.HEVC) "HEVC" else "H.264",
                            isSelected = settings.videoCodec == codec,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                hapticManager.tick()
                                onSetVideoCodec(codec)
                            }
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                DrawerSectionTitle("照片存储 (Photo)")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PhotoFormat.entries.forEach { fmt ->
                        DrawerTile(
                            title = if (fmt == PhotoFormat.JPEG) "JPEG" else "HEIF",
                            isSelected = settings.photoFormat == fmt,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                hapticManager.tick()
                                onSetPhotoFormat(fmt)
                            }
                        )
                    }
                }
            }
        }

        // 5. Video Bitrate Slider
        DrawerSectionTitle("视频编码码率 (Video Bitrate)")
        VideoBitrateSection(
            currentBitrateMbps = settings.videoBitrateMbps,
            onSetBitrate = onSetVideoBitrate,
            hapticManager = hapticManager
        )
    }
}

@Composable
private fun AssistHardwareView(
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onToggleFocusPeaking: (Boolean) -> Unit,
    onToggleZebraPattern: (Boolean) -> Unit,
    onSetHistogramMode: (HistogramMode) -> Unit,
    onSetMotionDuration: (MotionDurationPreset) -> Unit,
    onSetVolumeUp: (VolumeKeyAction) -> Unit,
    onSetVolumeDown: (VolumeKeyAction) -> Unit,
    onToggleScreenDimmer: (Boolean) -> Unit,
    hapticManager: HapticManager,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DrawerSectionTitle("取景与辅助视觉工具 (Visual Assists)")

        // Focus Peaking Row
        AssistMicroToggleRow(
            title = "🎯 峰值对焦 (Focus Peaking)",
            subtitle = "高反差边缘荧光轮廓",
            isEnabled = settings.focusPeakingEnabled,
            onToggle = {
                hapticManager.tick()
                onToggleFocusPeaking(!settings.focusPeakingEnabled)
            }
        )

        // Zebra Pattern Row
        AssistMicroToggleRow(
            title = "🦓 斑马纹高光告警 (Zebra Pattern)",
            subtitle = ">95% IRE 过曝对角线条",
            isEnabled = settings.zebraPatternEnabled,
            onToggle = {
                hapticManager.tick()
                onToggleZebraPattern(!settings.zebraPatternEnabled)
            }
        )

        // Screen Dimmer Power Saving Row
        AssistMicroToggleRow(
            title = "💡 录制防发热暗屏 (OLED 省电保护)",
            subtitle = "录制 3.5s 无触控自动微暗降温，触屏即全亮",
            isEnabled = settings.screenDimmerEnabled,
            onToggle = {
                hapticManager.tick()
                onToggleScreenDimmer(!settings.screenDimmerEnabled)
            }
        )

        // Histogram Mode Ladder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📊 实时示波器 :", color = OneShotTheme.TitaniumGray, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HistogramMode.entries.forEach { mode ->
                    DrawerTile(
                        title = when (mode) {
                            HistogramMode.OFF -> "关闭"
                            HistogramMode.LUMA -> "亮度"
                            HistogramMode.RGB -> "RGB"
                        },
                        isSelected = settings.histogramMode == mode,
                        onClick = {
                            hapticManager.tick()
                            onSetHistogramMode(mode)
                        }
                    )
                }
            }
        }

        // Motion Photo Duration
        DrawerSectionTitle("动态照片抓拍时长 (Live Motion Photo)")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MotionDurationPreset.entries.forEach { dur ->
                DrawerTile(
                    title = dur.label,
                    isSelected = settings.motionDuration == dur,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        hapticManager.tick()
                        onSetMotionDuration(dur)
                    }
                )
            }
        }

        // Hardware Volume Keys
        DrawerSectionTitle("硬件音量键映射 (Volume Buttons)")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "音量上键 [+] :", color = OneShotTheme.TitaniumGray, fontSize = 11.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(VolumeKeyAction.STILL_SHUTTER, VolumeKeyAction.VIDEO_SHUTTER, VolumeKeyAction.MOTION_PHOTO).forEach { act ->
                        DrawerTile(
                            title = act.label,
                            isSelected = settings.volumeKeyUpAction == act,
                            onClick = {
                                hapticManager.tick()
                                onSetVolumeUp(act)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "音量下键 [-] :", color = OneShotTheme.TitaniumGray, fontSize = 11.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(VolumeKeyAction.VIDEO_SHUTTER, VolumeKeyAction.STILL_SHUTTER, VolumeKeyAction.ZOOM_IN).forEach { act ->
                        DrawerTile(
                            title = act.label,
                            isSelected = settings.volumeKeyDownAction == act,
                            onClick = {
                                hapticManager.tick()
                                onSetVolumeDown(act)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistMicroToggleRow(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(OneShotTheme.ChassisCard)
            .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = OneShotTheme.FilmSilver, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = OneShotTheme.TitaniumGray, fontSize = 9.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isEnabled) OneShotTheme.AmberGrainMuted else Color(0x33222224))
                .border(
                    width = 1.dp,
                    color = if (isEnabled) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (isEnabled) "开 (ON)" else "关 (OFF)",
                color = if (isEnabled) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        color = OneShotTheme.TitaniumGray,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun DrawerTile(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
            .border(
                width = 1.dp,
                color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoBitrateSection(
    currentBitrateMbps: Int,
    onSetBitrate: (Int) -> Unit,
    hapticManager: HapticManager,
) {
    var sliderValue by remember(currentBitrateMbps) {
        mutableFloatStateOf(if (currentBitrateMbps > 0) currentBitrateMbps.toFloat() else 60f)
    }
    var lastHapticAnchor by remember { mutableIntStateOf(-1) }

    val isAuto = currentBitrateMbps <= 0
    val activeLabel = VideoBitrateHelper.getLabel(currentBitrateMbps)
    val storageEstimate = VideoBitrateHelper.estimateStoragePerMinute(currentBitrateMbps)
    val anchors = VideoBitrateHelper.KEY_ANCHORS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(OneShotTheme.ChassisCard)
            .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // Current Mode & Storage Estimate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isAuto) OneShotTheme.FilmSilver else OneShotTheme.AmberGrain)
                )
                Text(
                    text = activeLabel,
                    color = if (isAuto) OneShotTheme.FilmSilver else OneShotTheme.AmberGrain,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = storageEstimate,
                color = OneShotTheme.TitaniumGray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Pill Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isAuto) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisDark)
                    .border(
                        width = 0.75.dp,
                        color = if (isAuto) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        hapticManager.tick()
                        onSetBitrate(0)
                    }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "AUTO",
                    color = if (isAuto) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                    fontSize = 9.sp,
                    fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace
                )
            }

            anchors.forEach { mbps ->
                val isSelected = !isAuto && currentBitrateMbps == mbps
                val label = when (mbps) {
                    12 -> "12M"
                    100 -> "100M ★"
                    else -> "${mbps}M"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisDark)
                        .border(
                            width = 0.75.dp,
                            color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            hapticManager.tick()
                            sliderValue = mbps.toFloat()
                            onSetBitrate(mbps)
                        }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider (10 ~ 200 Mbps)
        Slider(
            value = sliderValue,
            onValueChange = { newVal ->
                sliderValue = newVal
                val targetInt = newVal.roundToInt().coerceIn(10, 200)
                if (targetInt in anchors && targetInt != lastHapticAnchor) {
                    hapticManager.tick()
                    lastHapticAnchor = targetInt
                } else if (targetInt !in anchors) {
                    lastHapticAnchor = -1
                }
                onSetBitrate(targetInt)
            },
            valueRange = 10f..200f,
            colors = SliderDefaults.colors(
                thumbColor = OneShotTheme.AmberGrain,
                activeTrackColor = OneShotTheme.AmberGrain,
                inactiveTrackColor = OneShotTheme.ChassisDark,
            ),
            modifier = Modifier.fillMaxWidth().height(22.dp)
        )
    }
}

private fun cycleSlot(
    item: QSlotItem,
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onSetAspect: (AspectRatioMode) -> Unit,
    onSetQuality: (QualityPreset) -> Unit,
    onSetStillResolution: (StillResolutionPreset) -> Unit = {},
    onSetFps: (FpsPreset) -> Unit,
    onSetVideoCodec: (VideoCodec) -> Unit,
    onSetPhotoFormat: (PhotoFormat) -> Unit,
    onSetDynamicRange: (DynamicRangeMode) -> Unit,
    onToggleDcg: (Boolean) -> Unit,
    onToggleUltraHdr: (Boolean) -> Unit,
    onSetMotionDuration: (MotionDurationPreset) -> Unit,
    onSetColorSpace: (ColorSpacePreset) -> Unit,
    onToggleLut: (Boolean) -> Unit,
    onSetLutPreset: (LutPreset) -> Unit,
    onSelectCustomLut: (String) -> Unit,
    onToggleLutBurnIn: (Boolean) -> Unit,
    onToggleFocusPeaking: (Boolean) -> Unit,
    onToggleZebraPattern: (Boolean) -> Unit,
    onSetHistogramMode: (HistogramMode) -> Unit,
) {
    when (item) {
        QSlotItem.ASPECT_RATIO -> {
            val next = if (settings.aspectMode == AspectRatioMode.RATIO_4_3) AspectRatioMode.RATIO_16_9 else AspectRatioMode.RATIO_4_3
            onSetAspect(next)
        }
        QSlotItem.DYNAMIC_RANGE -> {
            if (capabilities.isHlg10Supported) {
                val next = if (settings.dynamicRange == DynamicRangeMode.HLG10) DynamicRangeMode.SDR else DynamicRangeMode.HLG10
                onSetDynamicRange(next)
            }
        }
        QSlotItem.FPS_PRESET -> {
            val list = capabilities.supportedFpsPresets.ifEmpty { FpsPreset.entries }
            val currentIndex = list.indexOf(settings.fpsPreset)
            val nextIndex = (if (currentIndex >= 0) currentIndex + 1 else 0) % list.size
            onSetFps(list[nextIndex])
        }
        QSlotItem.FILM_STOCK -> {
            if (!settings.lutEnabled) {
                onToggleLut(true)
                onSetLutPreset(LutPreset.TEAL_AND_ORANGE)
            } else if (settings.selectedCustomLutId != null) {
                val customIndex = settings.customLuts.indexOfFirst { it.id == settings.selectedCustomLutId }
                if (customIndex in 0 until settings.customLuts.lastIndex) {
                    onSelectCustomLut(settings.customLuts[customIndex + 1].id)
                } else {
                    onToggleLut(false)
                }
            } else {
                when (settings.lutPreset) {
                    LutPreset.TEAL_AND_ORANGE -> onSetLutPreset(LutPreset.KODAK_PORTRA)
                    LutPreset.KODAK_PORTRA -> onSetLutPreset(LutPreset.FLAT_LOG)
                    LutPreset.FLAT_LOG -> {
                        if (settings.customLuts.isNotEmpty()) {
                            onSelectCustomLut(settings.customLuts.first().id)
                        } else {
                            onToggleLut(false)
                        }
                    }
                }
            }
        }
        QSlotItem.DCG_HDR -> {
            if (capabilities.isDcgSupported) {
                onToggleDcg(!settings.dcgHdrEnabled)
            }
        }
        QSlotItem.PHOTO_FORMAT -> {
            val next = if (settings.photoFormat == PhotoFormat.JPEG) PhotoFormat.HEIF else PhotoFormat.JPEG
            onSetPhotoFormat(next)
        }
        QSlotItem.STILL_RESOLUTION -> {
            val next = if (settings.stillResolution == StillResolutionPreset.RES_50MP) StillResolutionPreset.RES_12MP else StillResolutionPreset.RES_50MP
            onSetStillResolution(next)
        }
        QSlotItem.MOTION_DURATION -> {
            val list = MotionDurationPreset.entries
            val currentIndex = list.indexOf(settings.motionDuration)
            val nextIndex = (if (currentIndex >= 0) currentIndex + 1 else 0) % list.size
            onSetMotionDuration(list[nextIndex])
        }
        QSlotItem.FOCUS_PEAKING -> {
            onToggleFocusPeaking(!settings.focusPeakingEnabled)
        }
        QSlotItem.ZEBRA_PATTERN -> {
            onToggleZebraPattern(!settings.zebraPatternEnabled)
        }
        QSlotItem.VIDEO_CODEC -> {
            val next = if (settings.videoCodec == VideoCodec.HEVC) VideoCodec.H264 else VideoCodec.HEVC
            onSetVideoCodec(next)
        }
        QSlotItem.HISTOGRAM -> {
            val list = HistogramMode.entries
            val currentIndex = list.indexOf(settings.histogramMode)
            val nextIndex = (if (currentIndex >= 0) currentIndex + 1 else 0) % list.size
            onSetHistogramMode(list[nextIndex])
        }
        QSlotItem.LUT_BURN_IN -> {
            onToggleLutBurnIn(!settings.lutBurnIn)
        }
        QSlotItem.QUALITY -> {
            val list = capabilities.supportedQualities.ifEmpty { QualityPreset.entries }
            val currentIndex = list.indexOf(settings.quality)
            val nextIndex = (if (currentIndex >= 0) currentIndex + 1 else 0) % list.size
            onSetQuality(list[nextIndex])
        }
        QSlotItem.COLOR_SPACE -> {
            val list = ColorSpacePreset.entries
            val currentIndex = list.indexOf(settings.colorSpace)
            val nextIndex = (if (currentIndex >= 0) currentIndex + 1 else 0) % list.size
            onSetColorSpace(list[nextIndex])
        }
        QSlotItem.ULTRA_HDR -> {
            if (capabilities.isUltraHdrSupported) {
                onToggleUltraHdr(!settings.ultraHdrEnabled)
            }
        }
    }
}

private fun getSlotDisplayValue(
    item: QSlotItem,
    settings: CameraSettings,
): String = when (item) {
    QSlotItem.ASPECT_RATIO -> if (settings.aspectMode == AspectRatioMode.RATIO_4_3) "4:3 OPEN" else "16:9"
    QSlotItem.DYNAMIC_RANGE -> if (settings.dynamicRange == DynamicRangeMode.HLG10) "10-bit HLG" else "SDR 8-bit"
    QSlotItem.FPS_PRESET -> "${settings.fpsPreset.targetFps} FPS"
    QSlotItem.FILM_STOCK -> {
        if (!settings.lutEnabled) "原色直出"
        else if (settings.selectedCustomLutId != null) {
            settings.customLuts.find { it.id == settings.selectedCustomLutId }?.name?.take(8) ?: "CUSTOM"
        } else when (settings.lutPreset) {
            LutPreset.KODAK_PORTRA -> "PORTRA 400"
            LutPreset.TEAL_AND_ORANGE -> "CINE 250"
            LutPreset.FLAT_LOG -> "CINELOG"
        }
    }
    QSlotItem.DCG_HDR -> if (settings.dcgHdrEnabled) "开启" else "关闭"
    QSlotItem.PHOTO_FORMAT -> if (settings.photoFormat == PhotoFormat.JPEG) "JPEG" else "HEIF"
    QSlotItem.STILL_RESOLUTION -> settings.stillResolution.shortLabel
    QSlotItem.MOTION_DURATION -> "${settings.motionDuration.seconds}s"
    QSlotItem.FOCUS_PEAKING -> if (settings.focusPeakingEnabled) "开启" else "关闭"
    QSlotItem.ZEBRA_PATTERN -> if (settings.zebraPatternEnabled) "开启" else "关闭"
    QSlotItem.VIDEO_CODEC -> if (settings.videoCodec == VideoCodec.HEVC) "HEVC" else "H.264"
    QSlotItem.HISTOGRAM -> when (settings.histogramMode) {
        HistogramMode.OFF -> "关闭"
        HistogramMode.LUMA -> "亮度"
        HistogramMode.RGB -> "RGB"
    }
    QSlotItem.LUT_BURN_IN -> if (settings.lutBurnIn) "直出写盘" else "仅监看"
    QSlotItem.QUALITY -> settings.quality.label.split(" ").first()
    QSlotItem.COLOR_SPACE -> when (settings.colorSpace) {
        ColorSpacePreset.SRGB -> "sRGB"
        ColorSpacePreset.DISPLAY_P3 -> "P3"
        ColorSpacePreset.REC_2020 -> "Rec.2020"
    }
    QSlotItem.ULTRA_HDR -> if (settings.ultraHdrEnabled) "开启" else "关闭"
}
