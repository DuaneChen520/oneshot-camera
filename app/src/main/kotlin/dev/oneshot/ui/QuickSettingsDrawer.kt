package dev.oneshot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import dev.oneshot.data.QualityPreset
import dev.oneshot.data.VideoBitrateHelper
import dev.oneshot.data.VideoCodec
import dev.oneshot.data.VolumeKeyAction

import dev.oneshot.ui.theme.OneShotTheme

@Composable
fun QuickSettingsDrawer(
    isOpen: Boolean,
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onClose: () -> Unit,
    onSetAspect: (AspectRatioMode) -> Unit,
    onSetQuality: (QualityPreset) -> Unit,
    onSetFps: (FpsPreset) -> Unit,
    onSetVideoBitrate: (Int) -> Unit = {},
    onSetVideoCodec: (VideoCodec) -> Unit = {},
    onSetPhotoFormat: (PhotoFormat) -> Unit = {},
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
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(OneShotTheme.ChassisDark)
                .border(width = 1.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Brand Manifesto & close pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(OneShotTheme.AmberGrain)
                            )
                            Text(
                                text = "${OneShotTheme.BRAND_NAME} · Q 设定",
                                color = OneShotTheme.FilmSilver,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = OneShotTheme.SLOGAN_SUB,
                            color = OneShotTheme.TitaniumGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(OneShotTheme.ChassisCard)
                            .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = CircleShape)
                            .clickable {
                                hapticManager.tick()
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "收起 ✕",
                            color = OneShotTheme.AmberGrain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Frame Aspect Ratio
                DrawerSectionTitle("取景与画幅比例 (Aspect Ratio)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioMode.entries.forEach { mode ->
                        DrawerTile(
                            title = mode.label,
                            isSelected = settings.aspectMode == mode,
                            onClick = {
                                onSetAspect(mode)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 1.5: Photo Storage Format (JPEG / HEIF)
                DrawerSectionTitle("照片存储格式 (Photo Format)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoFormat.entries.forEach { fmt ->
                        DrawerTile(
                            title = fmt.label,
                            isSelected = settings.photoFormat == fmt,
                            onClick = {
                                onSetPhotoFormat(fmt)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Video Resolution & Quality Ladder
                DrawerSectionTitle("视频画质天梯 (Resolution)")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    capabilities.supportedQualities.forEach { q ->
                        DrawerTile(
                            title = q.label,
                            isSelected = settings.quality == q,
                            onClick = {
                                onSetQuality(q)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Frame Rate
                DrawerSectionTitle("录像帧率 (Frame Rate)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    capabilities.supportedFpsPresets.forEach { fps ->
                        DrawerTile(
                            title = fps.label,
                            isSelected = settings.fpsPreset == fps,
                            onClick = {
                                onSetFps(fps)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3.2: Video Encoding Codec (HEVC / H.264)
                DrawerSectionTitle("视频编码格式 (Video Codec)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoCodec.entries.forEach { codec ->
                        DrawerTile(
                            title = codec.label,
                            isSelected = settings.videoCodec == codec,
                            onClick = {
                                onSetVideoCodec(codec)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3.5: Video Encoding Bitrate
                DrawerSectionTitle("视频编码码率 (Video Bitrate)")
                VideoBitrateSection(
                    currentBitrateMbps = settings.videoBitrateMbps,
                    onSetBitrate = onSetVideoBitrate,
                    hapticManager = hapticManager
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 4: Dynamic Range & Hardware DCG
                DrawerSectionTitle("高动态范围与色彩编码 (HDR / DCG)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val supportedDr = if (capabilities.isHlg10Supported) DynamicRangeMode.entries else listOf(DynamicRangeMode.SDR)
                    supportedDr.forEach { dr ->
                        DrawerTile(
                            title = dr.label,
                            isSelected = settings.dynamicRange == dr,
                            onClick = {
                                onSetDynamicRange(dr)
                                hapticManager.tick()
                            }
                        )
                    }
                    if (capabilities.isDcgSupported) {
                        DrawerTile(
                            title = if (settings.dcgHdrEnabled) "高通 DCG: 开启" else "高通 DCG: 关闭",
                            isSelected = settings.dcgHdrEnabled,
                            onClick = {
                                onToggleDcg(!settings.dcgHdrEnabled)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 5: Still Photography & Motion Photo
                DrawerSectionTitle("静态与动态抓拍 (Stills & Motion Photo)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (capabilities.isUltraHdrSupported) {
                        DrawerTile(
                            title = if (settings.ultraHdrEnabled) "UltraHDR: 开启" else "UltraHDR: 关",
                            isSelected = settings.ultraHdrEnabled,
                            onClick = {
                                onToggleUltraHdr(!settings.ultraHdrEnabled)
                                hapticManager.tick()
                            }
                        )
                    }
                    MotionDurationPreset.entries.forEach { dur ->
                        DrawerTile(
                            title = dur.label,
                            isSelected = settings.motionDuration == dur,
                            onClick = {
                                onSetMotionDuration(dur)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 6: Color Space (M4 / M5)
                DrawerSectionTitle("色彩空间与影视风格 (Color Space)")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorSpacePreset.entries.forEach { cs ->
                        DrawerTile(
                            title = cs.label,
                            isSelected = settings.colorSpace == cs,
                            onClick = {
                                onSetColorSpace(cs)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 7: 3D LUT Preview & Burn-In (M5)
                DrawerSectionTitle("3D LUT 风格化滤镜与烧录 (3D LUT)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrawerTile(
                        title = if (settings.lutEnabled) "LUT 实时滤镜: 开" else "LUT: 关",
                        isSelected = settings.lutEnabled,
                        onClick = {
                            onToggleLut(!settings.lutEnabled)
                            hapticManager.tick()
                        }
                    )
                    DrawerTile(
                        title = if (settings.lutBurnIn) "烧录模式: 直接写盘" else "预览模式: 仅屏幕",
                        isSelected = settings.lutBurnIn,
                        onClick = {
                            onToggleLutBurnIn(!settings.lutBurnIn)
                            hapticManager.tick()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LutPreset.values().forEach { preset ->
                        DrawerTile(
                            title = preset.label,
                            isSelected = settings.lutEnabled && settings.selectedCustomLutId == null && settings.lutPreset == preset,
                            onClick = {
                                onSetLutPreset(preset)
                                if (!settings.lutEnabled) onToggleLut(true)
                                hapticManager.tick()
                            }
                        )
                    }

                    // User imported custom LUTs
                    settings.customLuts.forEach { customLut ->
                        CustomLutTile(
                            customLut = customLut,
                            isSelected = settings.lutEnabled && settings.selectedCustomLutId == customLut.id,
                            onClick = {
                                onSelectCustomLut(customLut.id)
                                if (!settings.lutEnabled) onToggleLut(true)
                                hapticManager.tick()
                            },
                            onDelete = {
                                onDeleteCustomLut(customLut)
                                hapticManager.tick()
                            }
                        )
                    }

                    // Import Tile
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OneShotTheme.ChassisCard)
                            .border(
                                width = 1.dp,
                                color = OneShotTheme.AmberGrain.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onImportLutClick()
                                hapticManager.tick()
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "[ + 导入 .cube 胶卷 ]",
                            color = OneShotTheme.AmberGrain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 8: Professional Visual Assist Tools (Focus Peaking · Zebra · Scope)
                DrawerSectionTitle("专业摄影辅助工具 (Focus Peaking · Zebra · Scope)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrawerTile(
                        title = if (settings.focusPeakingEnabled) "峰值对焦: 开" else "峰值对焦: 关",
                        isSelected = settings.focusPeakingEnabled,
                        onClick = {
                            onToggleFocusPeaking(!settings.focusPeakingEnabled)
                            hapticManager.tick()
                        }
                    )
                    DrawerTile(
                        title = if (settings.zebraPatternEnabled) "斑马纹高光警告: 开" else "斑马纹警告: 关",
                        isSelected = settings.zebraPatternEnabled,
                        onClick = {
                            onToggleZebraPattern(!settings.zebraPatternEnabled)
                            hapticManager.tick()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "实时示波器 / 直方图 :", color = OneShotTheme.TitaniumGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HistogramMode.values().forEach { mode ->
                            DrawerTile(
                                title = when (mode) {
                                    HistogramMode.OFF -> "关闭"
                                    HistogramMode.LUMA -> "亮度直方图"
                                    HistogramMode.RGB -> "RGB 分离波形"
                                },
                                isSelected = settings.histogramMode == mode,
                                onClick = {
                                    onSetHistogramMode(mode)
                                    hapticManager.tick()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 9: Hardware Volume Keys Customization
                DrawerSectionTitle("硬件音量键自定义映射 (Volume Buttons)")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "音量上键 [+] :", color = OneShotTheme.TitaniumGray, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(VolumeKeyAction.STILL_SHUTTER, VolumeKeyAction.VIDEO_SHUTTER, VolumeKeyAction.MOTION_PHOTO).forEach { act ->
                                DrawerTile(
                                    title = act.label,
                                    isSelected = settings.volumeKeyUpAction == act,
                                    onClick = {
                                        onSetVolumeUp(act)
                                        hapticManager.tick()
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
                        Text(text = "音量下键 [-] :", color = OneShotTheme.TitaniumGray, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(VolumeKeyAction.VIDEO_SHUTTER, VolumeKeyAction.STILL_SHUTTER, VolumeKeyAction.ZOOM_IN).forEach { act ->
                                DrawerTile(
                                    title = act.label,
                                    isSelected = settings.volumeKeyDownAction == act,
                                    onClick = {
                                        onSetVolumeDown(act)
                                        hapticManager.tick()
                                    }
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
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        color = OneShotTheme.TitaniumGray,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun DrawerTile(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
            .border(
                width = 1.dp,
                color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CustomLutTile(
    customLut: CustomLut,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisCard)
            .border(
                width = 1.dp,
                color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "胶卷 · ${customLut.name} (${customLut.size}³)",
                color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x228E8E93))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "✕",
                    color = OneShotTheme.TitaniumGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
    val anchors = VideoBitrateHelper.KEY_ANCHORS // listOf(12, 30, 60, 100, 150, 200)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OneShotTheme.ChassisCard)
            .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // 1. Header: Current Mode & Storage Estimate
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
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isAuto) OneShotTheme.FilmSilver else OneShotTheme.AmberGrain)
                )
                Text(
                    text = activeLabel,
                    color = if (isAuto) OneShotTheme.FilmSilver else OneShotTheme.AmberGrain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = storageEstimate,
                color = OneShotTheme.TitaniumGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Preset Pill Bar (AUTO, 12M, 30M, 60M, 100M★, 150M, 200M)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // AUTO Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAuto) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisDark)
                    .border(
                        width = 1.dp,
                        color = if (isAuto) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        hapticManager.tick()
                        onSetBitrate(0)
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "AUTO",
                    color = if (isAuto) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                    fontSize = 11.sp,
                    fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Anchors: 12M, 30M, 60M, 100M★, 150M, 200M
            anchors.forEach { mbps ->
                val isSelected = !isAuto && currentBitrateMbps == mbps
                val label = when (mbps) {
                    12 -> "12M"
                    100 -> "100M ★"
                    else -> "${mbps}M"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) OneShotTheme.AmberGrainMuted else OneShotTheme.ChassisDark)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) OneShotTheme.AmberGrain else OneShotTheme.TitaniumBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            hapticManager.tick()
                            sliderValue = mbps.toFloat()
                            onSetBitrate(mbps)
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Smooth Continuous Slider (10 ~ 200 Mbps) with Tick Haptics
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
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        )

        // 4. Tick Scale Markings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("10M", color = OneShotTheme.TitaniumGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("12M", color = if (!isAuto && currentBitrateMbps == 12) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("30M", color = if (!isAuto && currentBitrateMbps == 30) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("60M", color = if (!isAuto && currentBitrateMbps == 60) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("100M ★", color = OneShotTheme.AmberGrain, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("150M", color = if (!isAuto && currentBitrateMbps == 150) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("200M", color = if (!isAuto && currentBitrateMbps == 200) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

