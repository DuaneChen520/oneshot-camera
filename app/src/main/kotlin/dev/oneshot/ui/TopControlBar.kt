package dev.oneshot.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.DeviceCapabilities
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.LutPreset
import dev.oneshot.ui.theme.OneShotTheme

@Composable
fun TopControlBar(
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onToggleAspect: () -> Unit,
    onToggleDynamicRange: () -> Unit,
    onToggleFps: () -> Unit,
    onToggleDcg: () -> Unit,
    onOpenQMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OneShotTheme.ObsidianBlack.copy(alpha = 0.95f),
                        OneShotTheme.ObsidianBlack.copy(alpha = 0.70f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .displayCutoutPadding()
            // 物理安全出血区：水平内缩 18dp 避开屏幕顶角圆弧，且自然避让居中前置摄像头挖孔
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        // 1. Upper Brand Header & Loaded Film Roll Indicator (BRAND.md 规范)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Name & Decisive Philosophy Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "amberDiodePulse")
                val diodeScale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "diodeScale"
                )

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .scale(diodeScale)
                        .clip(CircleShape)
                        .background(OneShotTheme.AmberGrain)
                        .border(0.5.dp, OneShotTheme.AmberGrainGlow, CircleShape)
                )
                Text(
                    text = OneShotTheme.BRAND_NAME,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = OneShotTheme.FilmSilver,
                    fontFamily = OneShotTheme.MonospaceFont
                )
            }

            // Current Film Stock Roll Badge (Canister style with metal spool & accent color)
            val (filmBrandName, canisterColor) = if (settings.lutEnabled) {
                if (settings.selectedCustomLutId != null) {
                    val custom = settings.customLuts.find { it.id == settings.selectedCustomLutId }
                    (custom?.name ?: "CUSTOM").take(12).uppercase() to OneShotTheme.AmberGrain
                } else {
                    when (settings.lutPreset) {
                        LutPreset.KODAK_PORTRA -> "PORTRA 400" to OneShotTheme.AmberGrain
                        LutPreset.TEAL_AND_ORANGE -> "CINE 250" to OneShotTheme.Cine800TCyan
                        LutPreset.FLAT_LOG -> "CINELOG" to OneShotTheme.TitaniumLight
                    }
                }
            } else {
                "DIRECT OPTICS" to OneShotTheme.FilmSilver
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(OneShotTheme.ChassisCard.copy(alpha = 0.85f))
                    .border(
                        width = 0.75.dp,
                        color = if (settings.lutEnabled) canisterColor.copy(alpha = 0.6f) else OneShotTheme.ChassisBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable(onClick = onOpenQMenu)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Miniature 35mm Canister Icon
                Canvas(modifier = Modifier.size(11.dp)) {
                    val w = size.width
                    val h = size.height
                    // Canister body
                    drawRoundRect(
                        color = canisterColor,
                        topLeft = Offset(0f, h * 0.18f),
                        size = Size(w * 0.75f, h * 0.82f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Spool nub at top
                    drawRect(
                        color = OneShotTheme.FilmSilver,
                        topLeft = Offset(w * 0.22f, 0f),
                        size = Size(w * 0.32f, h * 0.22f)
                    )
                    // Film strip tail
                    drawRect(
                        color = canisterColor.copy(alpha = 0.7f),
                        topLeft = Offset(w * 0.75f, h * 0.35f),
                        size = Size(w * 0.25f, h * 0.45f)
                    )
                }

                Text(
                    text = "FILM: $filmBrandName",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OneShotTheme.MonospaceFont,
                    color = if (settings.lutEnabled) canisterColor else OneShotTheme.TitaniumGray
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // 2. Precision Camera Control Quick Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Aspect Ratio Pill (4:3 Open Gate vs 16:9 Scope)
            TactilePillButton(
                text = if (settings.aspectMode == AspectRatioMode.RATIO_4_3) "4:3 OPEN" else "16:9 SCOPE",
                active = settings.aspectMode == AspectRatioMode.RATIO_4_3,
                activeColor = OneShotTheme.AmberGrain,
                showDiode = true,
                onClick = onToggleAspect
            )

            // 2. Dynamic Range Pill (HLG 10-bit vs SDR)
            TactilePillButton(
                text = if (settings.dynamicRange == DynamicRangeMode.HLG10) "10-bit HLG" else "SDR",
                active = settings.dynamicRange == DynamicRangeMode.HLG10,
                activeColor = OneShotTheme.AmberGrain,
                showDiode = settings.dynamicRange == DynamicRangeMode.HLG10,
                onClick = if (capabilities.isHlg10Supported) onToggleDynamicRange else { {} }
            )

            // 3. FPS Toggle Pill (cycles through hardware supported presets)
            TactilePillButton(
                text = "${settings.fpsPreset.targetFps} FPS",
                active = settings.fpsPreset == FpsPreset.FPS_24,
                activeColor = if (settings.fpsPreset == FpsPreset.FPS_24) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                showDiode = false,
                onClick = onToggleFps
            )

            // 4. DCG Hardware HDR Pill (Smoothly hidden if device lacks Qualcomm DCG tag 0x8003)
            if (capabilities.isDcgSupported) {
                TactilePillButton(
                    text = "DCG HDR",
                    active = settings.dcgHdrEnabled,
                    activeColor = OneShotTheme.HardwareGreen,
                    showDiode = settings.dcgHdrEnabled,
                    onClick = onToggleDcg
                )
            }

            // 5. Q-Menu Pill (Sliding Quick Settings Drawer)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(OneShotTheme.AmberGrainMuted)
                    .border(
                        width = 0.75.dp,
                        color = OneShotTheme.AmberGrain.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(7.dp)
                    )
                    .clickable(onClick = onOpenQMenu)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Q 设定",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OneShotTheme.MonospaceFont,
                        color = OneShotTheme.AmberGrain
                    )
                    Text(
                        text = "▾",
                        fontSize = 9.sp,
                        color = OneShotTheme.AmberGrain
                    )
                }
            }
        }
    }
}

@Composable
fun TactilePillButton(
    text: String,
    active: Boolean,
    activeColor: Color,
    showDiode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (active) OneShotTheme.ChassisMilled.copy(alpha = 0.9f)
                else OneShotTheme.ChassisDark.copy(alpha = 0.75f)
            )
            .border(
                width = if (active) 0.85.dp else 0.5.dp,
                color = if (active) activeColor.copy(alpha = 0.7f) else OneShotTheme.ChassisBorder,
                shape = RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showDiode && active) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            }
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                fontFamily = OneShotTheme.MonospaceFont,
                color = if (active) activeColor else OneShotTheme.TitaniumLight
            )
        }
    }
}

