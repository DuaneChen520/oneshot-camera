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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.DeviceCapabilities
import dev.oneshot.camera.HapticManager
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.ColorSpacePreset
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.LutPreset
import dev.oneshot.data.MotionDurationPreset
import dev.oneshot.data.QualityPreset
import dev.oneshot.data.VolumeKeyAction

@Composable
fun QuickSettingsDrawer(
    isOpen: Boolean,
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onClose: () -> Unit,
    onSetAspect: (AspectRatioMode) -> Unit,
    onSetQuality: (QualityPreset) -> Unit,
    onSetFps: (FpsPreset) -> Unit,
    onSetDynamicRange: (DynamicRangeMode) -> Unit,
    onToggleDcg: (Boolean) -> Unit,
    onToggleUltraHdr: (Boolean) -> Unit,
    onSetMotionDuration: (MotionDurationPreset) -> Unit,
    onSetVolumeUp: (VolumeKeyAction) -> Unit,
    onSetVolumeDown: (VolumeKeyAction) -> Unit,
    onSetColorSpace: (ColorSpacePreset) -> Unit,
    onToggleLut: (Boolean) -> Unit,
    onSetLutPreset: (LutPreset) -> Unit,
    onToggleLutBurnIn: (Boolean) -> Unit,
    onToggleFocusPeaking: (Boolean) -> Unit,
    onToggleZebraPattern: (Boolean) -> Unit,
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
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xF5141414))
                .border(width = 1.dp, color = Color(0x44FFFFFF), shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with close pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Q 快速设定抽屉 (Quick Settings)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable {
                                hapticManager.tick()
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = "收起 ✕", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

                // Section 2: Video Resolution & Quality Ladder
                DrawerSectionTitle("视频画质天梯 (Resolution)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LutPreset.values().forEach { preset ->
                        DrawerTile(
                            title = preset.label,
                            isSelected = settings.lutEnabled && settings.lutPreset == preset,
                            onClick = {
                                onSetLutPreset(preset)
                                if (!settings.lutEnabled) onToggleLut(true)
                                hapticManager.tick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 8: Professional Visual Assist Tools (M5 Focus Peaking & Zebra)
                DrawerSectionTitle("专业摄影辅助工具 (Focus Peaking & Zebra Pattern)")
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

                Spacer(modifier = Modifier.height(12.dp))

                // Section 9: Hardware Volume Keys Customization
                DrawerSectionTitle("硬件音量键自定义映射 (Volume Buttons)")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "音量上键 [+] :", color = Color.LightGray, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        Text(text = "音量下键 [-] :", color = Color.LightGray, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        color = Color(0xAAFFFFFF),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
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
            .background(if (isSelected) Color(0x33FFD54F) else Color(0x22FFFFFF))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFFFFD54F) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color(0xFFFFD54F) else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
