package dev.oneshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset

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
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Aspect Ratio Pill (4:3 OpenGate vs 16:9)
            PillButton(
                text = settings.aspectMode.label,
                active = settings.aspectMode == AspectRatioMode.RATIO_4_3,
                activeColor = Color(0xFF00E5FF),
                onClick = onToggleAspect
            )

            // 2. Dynamic Range Pill (HLG 10-bit vs SDR)
            PillButton(
                text = if (settings.dynamicRange == DynamicRangeMode.HLG10) "10-bit HLG" else "SDR",
                active = settings.dynamicRange == DynamicRangeMode.HLG10,
                activeColor = Color(0xFFFFB300),
                onClick = if (capabilities.isHlg10Supported) onToggleDynamicRange else { {} }
            )

            // 3. FPS Toggle Pill (cycles through hardware supported presets)
            PillButton(
                text = "${settings.fpsPreset.targetFps} fps",
                active = true,
                activeColor = Color(0xFF81D4FA),
                onClick = onToggleFps
            )

            // 4. DCG Hardware HDR Pill (Smoothly hidden if device lacks Qualcomm DCG tag 0x8003)
            if (capabilities.isDcgSupported) {
                PillButton(
                    text = "DCG HDR",
                    active = settings.dcgHdrEnabled,
                    activeColor = Color(0xFF00E676),
                    onClick = onToggleDcg
                )
            }

            // 5. Q-Menu Pill (Sliding Quick Settings Drawer)
            PillButton(
                text = "Q 菜单",
                active = true,
                activeColor = Color(0xFFFFD54F),
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
            .clip(CircleShape)
            .background(Color(0x55000000))
            .border(
                width = 1.dp,
                color = if (active) activeColor else Color(0x66FFFFFF),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) activeColor else Color.White
        )
    }
}
