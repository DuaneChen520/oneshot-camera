package dev.oneshot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.data.CameraSettings
import kotlin.math.roundToInt

@Composable
fun ProControlsPanel(
    settings: CameraSettings,
    zoomRange: ClosedFloatingPointRange<Float>,
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    exposureRange: IntRange,
    exposureStep: Float,
    currentEvIndex: Int,
    onEvChange: (Int) -> Unit,
    onToggleAwb: (Boolean) -> Unit,
    onCctChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf<ProTab?>(ProTab.ZOOM) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab selector bar
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x991E1E1E))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF37474F) else Color.Transparent)
                        .clickable { activeTab = if (isSelected) null else tab }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val label = when (tab) {
                        ProTab.ZOOM -> "%.1fx".format(currentZoom)
                        ProTab.EV -> {
                            val evVal = currentEvIndex * exposureStep
                            if (evVal > 0) "+%.1f EV".format(evVal) else "%.1f EV".format(evVal)
                        }
                        ProTab.WB -> if (settings.isAutoWb) "AWB" else "${settings.cctKelvin}K"
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFFFD54F) else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content
        AnimatedVisibility(
            visible = activeTab != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xBB121212))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when (activeTab) {
                    ProTab.ZOOM -> ZoomControl(
                        zoomRange = zoomRange,
                        currentZoom = currentZoom,
                        onZoomChange = onZoomChange
                    )
                    ProTab.EV -> ExposureControl(
                        exposureRange = exposureRange,
                        exposureStep = exposureStep,
                        currentEvIndex = currentEvIndex,
                        onEvChange = onEvChange
                    )
                    ProTab.WB -> WhiteBalanceControl(
                        isAuto = settings.isAutoWb,
                        kelvin = settings.cctKelvin,
                        onToggleAwb = onToggleAwb,
                        onCctChange = onCctChange
                    )
                    null -> Unit
                }
            }
        }
    }
}

enum class ProTab { ZOOM, EV, WB }

@Composable
private fun ZoomControl(
    zoomRange: ClosedFloatingPointRange<Float>,
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
) {
    val presets = listOf(0.6f, 1.0f, 2.0f, 5.0f, 10.0f)
        .filter { it in zoomRange.start..zoomRange.endInclusive }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            presets.forEach { preset ->
                val active = (currentZoom - preset).let { it in -0.05f..0.05f }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (active) Color(0xFFFFD54F) else Color(0x33FFFFFF))
                        .clickable { onZoomChange(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (preset < 1f) "%.1f".format(preset) else "${preset.toInt()}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.Black else Color.White
                    )
                }
            }
        }

        Slider(
            value = currentZoom,
            onValueChange = onZoomChange,
            valueRange = zoomRange.start..zoomRange.endInclusive,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD54F),
                activeTrackColor = Color(0xFFFFD54F)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ExposureControl(
    exposureRange: IntRange,
    exposureStep: Float,
    currentEvIndex: Int,
    onEvChange: (Int) -> Unit,
) {
    val evValue = currentEvIndex * exposureStep
    val evText = if (evValue > 0) "+%.2f EV".format(evValue) else "%.2f EV".format(evValue)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "曝光补偿: $evText",
            fontSize = 12.sp,
            color = Color.LightGray
        )
        Slider(
            value = currentEvIndex.toFloat(),
            onValueChange = { onEvChange(it.roundToInt()) },
            valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
            steps = (exposureRange.last - exposureRange.first) - 1,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WhiteBalanceControl(
    isAuto: Boolean,
    kelvin: Int,
    onToggleAwb: (Boolean) -> Unit,
    onCctChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAuto) "自动白平衡 (AWB)" else "手动色温: ${kelvin}K",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAuto) Color(0xFFFFB300) else Color(0x33FFFFFF))
                    .clickable { onToggleAwb(!isAuto) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isAuto) "已锁定 AWB" else "切换为自动",
                    fontSize = 11.sp,
                    color = if (isAuto) Color.Black else Color.White
                )
            }
        }

        if (!isAuto) {
            Slider(
                value = kelvin.toFloat(),
                onValueChange = { onCctChange(it.roundToInt()) },
                valueRange = 2000f..10000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9100),
                    activeTrackColor = Color(0xFFFF9100)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
