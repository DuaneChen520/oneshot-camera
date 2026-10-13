package dev.oneshot.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .padding(horizontal = 14.dp, vertical = 8.dp)
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
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(OneShotTheme.AmberGrain)
                )
                Text(
                    text = OneShotTheme.BRAND_NAME,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = OneShotTheme.FilmSilver,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "· ${OneShotTheme.SLOGAN_MAIN}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = OneShotTheme.TitaniumGray
                )
            }

            // Current Film Stock Roll Badge (Clickable shortcut to switch LUT)
            val filmStockLabel = if (settings.lutEnabled) {
                if (settings.selectedCustomLutId != null) {
                    val custom = settings.customLuts.find { it.id == settings.selectedCustomLutId }
                    val name = custom?.name ?: "CUSTOM"
                    "FILM: ${name.take(14).uppercase()}"
                } else {
                    when (settings.lutPreset) {
                        LutPreset.KODAK_PORTRA -> "FILM: PORTRA 400"
                        LutPreset.TEAL_AND_ORANGE -> "FILM: CINE 250"
                        LutPreset.FLAT_LOG -> "FILM: CINELOG"
                    }
                }
            } else {
                "FILM: DIRECT OPTICS"
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33222224))
                    .border(width = 0.5.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpenQMenu)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = filmStockLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (settings.lutEnabled) OneShotTheme.AmberGrain else OneShotTheme.TitaniumGray
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Precision Camera Control Quick Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Aspect Ratio Pill (4:3 Open Gate vs 16:9)
            PillButton(
                text = if (settings.aspectMode == AspectRatioMode.RATIO_4_3) "4:3 OPEN GATE" else "16:9",
                active = settings.aspectMode == AspectRatioMode.RATIO_4_3,
                activeColor = OneShotTheme.AmberGrain,
                onClick = onToggleAspect
            )

            // 2. Dynamic Range Pill (HLG 10-bit vs SDR)
            PillButton(
                text = if (settings.dynamicRange == DynamicRangeMode.HLG10) "10-bit HLG" else "SDR",
                active = settings.dynamicRange == DynamicRangeMode.HLG10,
                activeColor = OneShotTheme.AmberGrain,
                onClick = if (capabilities.isHlg10Supported) onToggleDynamicRange else { {} }
            )

            // 3. FPS Toggle Pill (cycles through hardware supported presets)
            PillButton(
                text = "${settings.fpsPreset.targetFps} FPS",
                active = true,
                activeColor = OneShotTheme.FilmSilver,
                onClick = onToggleFps
            )

            // 4. DCG Hardware HDR Pill (Smoothly hidden if device lacks Qualcomm DCG tag 0x8003)
            if (capabilities.isDcgSupported) {
                PillButton(
                    text = "DCG HDR",
                    active = settings.dcgHdrEnabled,
                    activeColor = OneShotTheme.HardwareGreen,
                    onClick = onToggleDcg
                )
            }

            // 5. Q-Menu Pill (Sliding Quick Settings Drawer)
            PillButton(
                text = "Q 设定",
                active = true,
                activeColor = OneShotTheme.AmberGrain,
                onClick = onOpenQMenu
            )
        }
    }
}

@Composable
fun PillButton(
    text: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x66141416))
            .border(
                width = 0.75.dp,
                color = if (active) activeColor else OneShotTheme.TitaniumBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (active) activeColor else OneShotTheme.TitaniumGray
        )
    }
}
