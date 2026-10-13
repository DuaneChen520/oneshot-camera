package dev.oneshot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.gl.HistogramData
import dev.oneshot.data.HistogramMode
import dev.oneshot.ui.theme.OneShotTheme

/**
 * Optical Live Histogram / Waveform Scope (BRAND.md Mechanical Aesthetics)
 * Displays 64-bin real-time luminance or RGB spectral distributions.
 */
@Composable
fun LiveHistogramScope(
    mode: HistogramMode,
    data: HistogramData?,
    onCycleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = mode != HistogramMode.OFF,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val hist = data ?: HistogramData.empty()

        Box(
            modifier = Modifier
                .width(130.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OneShotTheme.ChassisDark.copy(alpha = 0.82f))
                .border(width = 1.dp, color = OneShotTheme.TitaniumBorder, shape = RoundedCornerShape(8.dp))
                .clickable(onClick = onCycleMode)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Mode Tag & Clipping Warnings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HIST · ${mode.name}",
                        color = if (mode == HistogramMode.RGB) OneShotTheme.FilmSilver else OneShotTheme.AmberGrain,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OneShotTheme.MonospaceFont
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (hist.isShadowClipped) {
                            Text(
                                text = "◀ SHDW",
                                color = OneShotTheme.DarkroomRed,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = OneShotTheme.MonospaceFont
                            )
                        }
                        if (hist.isHighlightClipped) {
                            Text(
                                text = "CLIP ▶",
                                color = OneShotTheme.DarkroomRed,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = OneShotTheme.MonospaceFont
                            )
                        }
                        if (!hist.isShadowClipped && !hist.isHighlightClipped) {
                            Text(
                                text = "${(hist.meanLuma / 2.55f).toInt()}%",
                                color = OneShotTheme.TitaniumGray,
                                fontSize = 8.sp,
                                fontFamily = OneShotTheme.MonospaceFont
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Canvas Scope Visualization
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Grid Reticle (Dashed vertical lines for 25%, 50%, 75% EV zones)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                    val gridColor = Color(0x338E8E93)
                    for (step in 1..3) {
                        val x = w * (step / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // 2. Draw Curves
                    if (mode == HistogramMode.LUMA) {
                        drawLumaHistogram(hist.lumaBins, w, h)
                    } else if (mode == HistogramMode.RGB) {
                        drawChannelCurve(hist.rBins, Color(0x99FF4B4B), Color(0x33FF4B4B), w, h)
                        drawChannelCurve(hist.gBins, Color(0x994CAF50), Color(0x334CAF50), w, h)
                        drawChannelCurve(hist.bBins, Color(0x99448AFF), Color(0x33448AFF), w, h)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLumaHistogram(bins: FloatArray, w: Float, h: Float) {
    if (bins.isEmpty()) return
    val binWidth = w / bins.size.toFloat()

    val path = Path()
    path.moveTo(0f, h)

    for (i in bins.indices) {
        val x = i * binWidth + binWidth / 2f
        val y = h - (bins[i] * (h - 2f)).coerceIn(0f, h)
        path.lineTo(x, y)
    }
    path.lineTo(w, h)
    path.close()

    // Fill with Amber gradient
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                OneShotTheme.AmberGrain.copy(alpha = 0.65f),
                OneShotTheme.AmberGrain.copy(alpha = 0.12f)
            ),
            startY = 0f,
            endY = h
        ),
        style = Fill
    )

    // Stroke Outline
    val strokePath = Path()
    for (i in bins.indices) {
        val x = i * binWidth + binWidth / 2f
        val y = h - (bins[i] * (h - 2f)).coerceIn(0f, h)
        if (i == 0) strokePath.moveTo(x, y) else strokePath.lineTo(x, y)
    }
    drawPath(
        path = strokePath,
        color = OneShotTheme.AmberGrain,
        style = Stroke(width = 1.25f)
    )
}

private fun DrawScope.drawChannelCurve(
    bins: FloatArray,
    strokeColor: Color,
    fillColor: Color,
    w: Float,
    h: Float,
) {
    if (bins.isEmpty()) return
    val binWidth = w / bins.size.toFloat()

    val fillPath = Path()
    fillPath.moveTo(0f, h)
    for (i in bins.indices) {
        val x = i * binWidth + binWidth / 2f
        val y = h - (bins[i] * (h - 2f)).coerceIn(0f, h)
        fillPath.lineTo(x, y)
    }
    fillPath.lineTo(w, h)
    fillPath.close()

    drawPath(path = fillPath, color = fillColor, style = Fill)

    val strokePath = Path()
    for (i in bins.indices) {
        val x = i * binWidth + binWidth / 2f
        val y = h - (bins[i] * (h - 2f)).coerceIn(0f, h)
        if (i == 0) strokePath.moveTo(x, y) else strokePath.lineTo(x, y)
    }
    drawPath(path = strokePath, color = strokeColor, style = Stroke(width = 1.1f))
}
