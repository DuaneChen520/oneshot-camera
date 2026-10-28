package dev.oneshot.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.DeviceCapabilities
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.LutPreset
import dev.oneshot.ui.theme.CinemaIcon
import dev.oneshot.ui.theme.OneShotIcons
import dev.oneshot.ui.theme.OneShotTheme

/**
 * OneShot Film Industrial Top Control Bar.
 *
 * Adheres strictly to BRAND.md cinema optical ergonomics:
 * - Upper brand header with 35mm physical film canister status badge.
 * - Lower precision tactical matrix: Exactly 6 fixed-dimension (48dp x 36dp) industrial cells.
 * - Eliminates layout jitter and dynamic text resizing. Uses sharp cinema vector iconography
 *   and hardware status diodes (Amber / Green).
 */
@Composable
fun TopControlBar(
    settings: CameraSettings,
    capabilities: DeviceCapabilities,
    onToggleAspect: () -> Unit,
    onToggleDynamicRange: () -> Unit,
    onToggleFps: () -> Unit,
    onToggleDcg: () -> Unit,
    onToggleMotionPhotoLive: () -> Unit,
    onOpenQMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OneShotTheme.ObsidianBlack.copy(alpha = 0.96f),
                        OneShotTheme.ObsidianBlack.copy(alpha = 0.75f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .displayCutoutPadding()
            // 物理安全出血区：水平内缩 16dp 避开屏幕顶角圆弧，且自然避让居中前置摄像头挖孔
            .padding(horizontal = 16.dp, vertical = 6.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Uniform 6-Slot Industrial Tactile Matrix (Fixed 48dp x 36dp per Cell)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Aspect Ratio Cell (4:3 Open Gate vs 16:9 Scope)
            TactileIconCell(
                iconRes = OneShotIcons.AspectRatioOpenGate,
                label = if (settings.aspectMode == AspectRatioMode.RATIO_4_3) "4:3" else "16:9",
                active = settings.aspectMode == AspectRatioMode.RATIO_4_3,
                activeColor = OneShotTheme.AmberGrain,
                showDiode = settings.aspectMode == AspectRatioMode.RATIO_4_3,
                onClick = onToggleAspect
            )

            // 2. Dynamic Range Cell (HLG 10-bit vs SDR)
            TactileIconCell(
                iconRes = OneShotIcons.DynamicRangeHlg,
                label = if (settings.dynamicRange == DynamicRangeMode.HLG10) "10-bit" else "SDR",
                active = settings.dynamicRange == DynamicRangeMode.HLG10,
                activeColor = OneShotTheme.AmberGrain,
                showDiode = settings.dynamicRange == DynamicRangeMode.HLG10,
                onClick = if (capabilities.isHlg10Supported) onToggleDynamicRange else { {} }
            )

            // 3. FPS Shutter Angle Cell (24 / 30 / 60 / 120)
            TactileIconCell(
                iconRes = OneShotIcons.FpsShutterAngle,
                label = "${settings.fpsPreset.targetFps}",
                active = settings.fpsPreset == FpsPreset.FPS_24,
                activeColor = if (settings.fpsPreset == FpsPreset.FPS_24) OneShotTheme.AmberGrain else OneShotTheme.FilmSilver,
                showDiode = false,
                onClick = onToggleFps
            )

            // 4. DCG Hardware HDR Cell
            TactileIconCell(
                iconRes = OneShotIcons.DcgDualGain,
                label = "DCG",
                active = settings.dcgHdrEnabled && capabilities.isDcgSupported,
                activeColor = OneShotTheme.HardwareGreen,
                showDiode = settings.dcgHdrEnabled && capabilities.isDcgSupported,
                onClick = if (capabilities.isDcgSupported) onToggleDcg else { {} }
            )

            // 5. Motion Photo LIVE Pre-Roll Buffer Cell
            val isLiveActive = settings.motionPhotoLiveEnabled
            val liveTransition = rememberInfiniteTransition(label = "liveDiodePulse")
            val livePulse by liveTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "livePulse"
            )

            TactileIconCell(
                iconRes = OneShotIcons.MotionPhotoLive,
                label = "LIVE",
                active = isLiveActive,
                activeColor = OneShotTheme.AmberGrain,
                showDiode = isLiveActive,
                diodeScale = if (isLiveActive) livePulse else 1.0f,
                onClick = onToggleMotionPhotoLive
            )

            // 6. Q-Menu Matrix Cell (Sliding Quick Settings Drawer)
            TactileIconCell(
                iconRes = OneShotIcons.QMenuMatrix,
                label = "Q",
                active = true,
                activeColor = OneShotTheme.AmberGrain,
                showDiode = false,
                isSpecialQ = true,
                onClick = onOpenQMenu
            )
        }
    }
}

/**
 * Fixed-dimension Industrial Tactile Button Cell (48dp x 36dp).
 *
 * Guarantees zero layout shifts regardless of state changes.
 */
@Composable
private fun TactileIconCell(
    @DrawableRes iconRes: Int,
    label: String,
    active: Boolean,
    activeColor: Color,
    showDiode: Boolean = false,
    diodeScale: Float = 1.0f,
    isSpecialQ: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isSpecialQ -> OneShotTheme.AmberGrain.copy(alpha = 0.75f)
        active -> activeColor.copy(alpha = 0.75f)
        else -> OneShotTheme.ChassisBorder
    }

    val backgroundColor = when {
        isSpecialQ -> OneShotTheme.AmberGrainMuted.copy(alpha = 0.55f)
        active -> OneShotTheme.ChassisMilled.copy(alpha = 0.95f)
        else -> OneShotTheme.ChassisDark.copy(alpha = 0.80f)
    }

    val contentColor = when {
        isSpecialQ -> OneShotTheme.AmberGrain
        active -> activeColor
        else -> OneShotTheme.TitaniumGray
    }

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (active || isSpecialQ) 0.85.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            // High-precision Cinema Vector Icon
            CinemaIcon(
                resId = iconRes,
                contentDescription = label,
                tint = contentColor,
                size = 14.dp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Precision Compact Monospace Tag
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                fontFamily = OneShotTheme.MonospaceFont,
                color = contentColor,
                maxLines = 1
            )
        }

        // Hardware Diode Indicator at top right
        if (showDiode && active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(4.dp)
                    .scale(diodeScale)
                    .clip(CircleShape)
                    .background(activeColor)
                    .border(0.5.dp, activeColor.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}
