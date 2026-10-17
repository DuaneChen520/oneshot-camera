package dev.oneshot.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import androidx.compose.ui.graphics.StrokeJoin
import dev.oneshot.camera.MediaThumbnail
import dev.oneshot.camera.RecordingStatus
import dev.oneshot.data.LensFacingMode
import dev.oneshot.ui.theme.OneShotTheme

@Composable
fun ShutterCluster(
    status: RecordingStatus,
    onTakeStillPhoto: () -> Unit,
    onCaptureMotionPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    lensFacing: LensFacingMode = LensFacingMode.BACK,
    latestMedia: MediaThumbnail? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // 物理安全出血区：左右内缩 22dp 远离屏幕四角 190px 大圆弧，底部抬高 18dp 远离手势导航小白条
            .padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video Recording Status HUD (Tally Pulse + Timecode + Real-time File size)
        AnimatedVisibility(
            visible = status.isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val minutes = status.durationSeconds / 60
            val seconds = status.durationSeconds % 60
            val sizeMb = status.recordedBytes.toFloat() / (1024 * 1024)

            Row(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(OneShotTheme.ChassisDark.copy(alpha = 0.96f))
                    .border(width = 1.dp, color = OneShotTheme.DarkroomRed.copy(alpha = 0.8f), shape = CapsuleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val transition = rememberInfiniteTransition(label = "tallyPulse")
                val pulse by transition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotPulse"
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(OneShotTheme.DarkroomRed)
                        .border(0.5.dp, OneShotTheme.DarkroomRedGlow, CircleShape)
                )
                Text(
                    text = "REC %02d:%02d".format(minutes, seconds),
                    color = OneShotTheme.FilmSilver,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OneShotTheme.MonospaceFont
                )
                Text(
                    text = "|  %.1f MB".format(sizeMb),
                    color = OneShotTheme.TitaniumGray,
                    fontSize = 11.sp,
                    fontFamily = OneShotTheme.MonospaceFont
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Three-Shutter Bar Row (Physical Cluster Architecture)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Far Left: Skeuomorphic Optical Inspection Squircle Hatch (Gallery Jump)
            GallerySquircleOpticalHatch(
                latestMedia = latestMedia,
                enabled = !status.isRecording && !status.isMotionCapturing,
                onClick = onOpenGallery
            )

            // Left Shutter: Motion Photo (Live Emulsion Shutter with concentric rings)
            MotionPhotoShutterButton(
                isCapturing = status.isMotionCapturing,
                enabled = !status.isMotionCapturing,
                onClick = onCaptureMotionPhoto
            )

            // Center Shutter: Decisive Frame Still Photography (Precision Mechanical Shutter Assembly)
            MechanicalStillShutterButton(
                isCapturing = status.isStillCapturing,
                enabled = !status.isMotionCapturing,
                onClick = onTakeStillPhoto
            )

            // Right Shutter: Video Recording (Darkroom Red Tally Shutter)
            CinemaVideoShutterButton(
                isRecording = status.isRecording,
                enabled = !status.isMotionCapturing,
                onClick = {
                    if (status.isRecording) onStopRecording() else onStartRecording()
                }
            )

            // Far Right: Authentic Mechanical Slide Switch (Left Landscape <-> Right Portrait)
            MechanicalSquircleSlideSwitch(
                lensFacing = lensFacing,
                enabled = !status.isRecording && !status.isMotionCapturing,
                onClick = onFlipCamera
            )
        }
    }
}

/**
 * Continuous-Curvature Superellipse (Squircle) Shape.
 * Implements Kenya Hara's Xiaomi Alive formula: |x/a|^n + |y/b|^n = 1 with n = 3.0.
 * Eliminates harsh robotic straight-to-arc joints with completely continuous G2 curvature.
 */
class SquircleShape(private val n: Float = 3.0f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val a = size.width / 2f
        val b = size.height / 2f
        val path = buildSquirclePath(cx = a, cy = b, a = a, b = b, n = n)
        return Outline.Generic(path)
    }
}

private fun buildSquirclePath(
    cx: Float,
    cy: Float,
    a: Float,
    b: Float,
    n: Float = 3.0f,
    steps: Int = 72
): Path {
    val path = Path()
    val exponent = 2f / n
    for (i in 0 until steps) {
        val theta = (i * 2.0 * Math.PI / steps).toFloat()
        val cosT = cos(theta)
        val sinT = sin(theta)
        val x = cx + a * cosT.sign * abs(cosT).pow(exponent)
        val y = cy + b * sinT.sign * abs(sinT).pow(exponent)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Far Left: Skeuomorphic Optical Viewfinder Hatch (Gallery Jump Button).
 * Recreates the authentic optical viewfinder feel of classic Fujifilm point-and-shoot cameras:
 * - Organic Continuous-Curvature Superellipse (Xiaomi Alive n = 3.0)
 * - Pure Z-axis mechanical travel: perspective scale depression (1.0 -> 0.95), ZERO unnatural Y-axis translation
 * - Fujifilm Viewfinder Optical Aberration:
 *   - Crystal clear central transmission
 *   - Four-corner diffuse light scattering & soft peripheral lens haze (milky-cyan optical dispersion)
 *   - Natural optical corner vignetting & light falloff
 *   - Completely removed artificial glare streaks & perimeter wireframe strokes
 * - Standby: Deep obsidian optical cavity with 6-blade aperture diaphragm
 * - Instant micro-switch tactile snap on touch-down (HapticFeedbackConstants.CONFIRM)
 */
@Composable
private fun GallerySquircleOpticalHatch(
    latestMedia: MediaThumbnail?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // Instant tactile micro-switch haptic snap on touch-down
    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    // Pure Z-Axis mechanical borehole depression: 0.0f (rest) -> 1.0f (recessed into housing)
    val depression by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 750f
        ),
        label = "galleryHatchDepression"
    )

    // True Z-axis perspective compression & light occlusion (NO Y-axis translation)
    val zScale = 1.0f - (0.05f * depression)
    val lightOcclusion = 1.0f - (0.18f * depression)

    Box(
        modifier = modifier
            .size(36.dp)
            .alpha(if (enabled) 1.0f else 0.35f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // MOVING INNER OPTICAL VIEWFINDER ELEMENT (Scheme 2: 36dp compact optical viewport)
        // Direct glass viewport embedded into chassis - NO exterior wireframe or outer line frame
        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(zScale)
                .clip(SquircleShape(n = 3.0f))
                .background(Color(0xFF0A090D)),
            contentAlignment = Alignment.Center
        ) {
            if (latestMedia?.bitmap != null) {
                // 2.1 Pure uncropped photographic media
                Image(
                    bitmap = latestMedia.bitmap.asImageBitmap(),
                    contentDescription = "最近底片 (光学检视视窗)",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (latestMedia.isVideo) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            color = OneShotTheme.FilmSilver,
                            fontSize = 8.sp
                        )
                    }
                }
            } else {
                // 2.2 Standby Empty State: Deep Optical Obsidian Chamber with Aperture Diaphragm
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val r = w * 0.22f
                    val irisColor = Color(0x66C7C7CC).copy(alpha = 0.45f * lightOcclusion)

                    // 6-Blade Precision Aperture Diaphragm
                    val bladeCount = 6
                    for (i in 0 until bladeCount) {
                        val angleDeg = i * (360f / bladeCount)
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val nextRad = Math.toRadians((angleDeg + 60f).toDouble())
                        val x1 = cx + (r * cos(rad)).toFloat()
                        val y1 = cy + (r * sin(rad)).toFloat()
                        val x2 = cx + (r * 0.5f * cos(nextRad)).toFloat()
                        val y2 = cy + (r * 0.5f * sin(nextRad)).toFloat()
                        drawLine(
                            color = irisColor,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 1.0.dp.toPx()
                        )
                    }
                    drawCircle(
                        color = Color(0x22FFFFFF),
                        radius = r * 0.35f,
                        center = Offset(cx, cy)
                    )
                }
            }

            // 2.3 FUJIFILM VIEWFINDER OPTICAL DISPERSION & CORNER DIFFUSE SCATTERING
            // (Recreates the vintage meniscus viewfinder lens: clear center + soft corner haze + peripheral falloff)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val maxDim = maxOf(w, h)

                // Pass 1: Fuji Optical Vignetting & Corner Light Falloff
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to Color.Transparent,
                        0.42f to Color.Transparent,
                        0.72f to Color(0x35000000).copy(alpha = 0.25f + 0.15f * depression),
                        0.88f to Color(0x6508080E).copy(alpha = 0.55f + 0.15f * depression),
                        1.00f to Color(0x95040408).copy(alpha = 0.80f + 0.15f * depression),
                        center = Offset(cx, cy),
                        radius = maxDim * 0.72f
                    )
                )

                // Pass 2: Four-Corner Diffuse Light Scattering & Defocus Haze (Meniscus Acrylic/Glass Dispersion)
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to Color.Transparent,
                        0.48f to Color.Transparent,
                        0.74f to Color(0x1F00E5FF).copy(alpha = 0.16f * lightOcclusion), // Soft cyan dispersion haze
                        0.88f to Color(0x1CE0F7FA).copy(alpha = 0.20f * lightOcclusion), // Milky refractive scattering
                        1.00f to Color(0x14A855F7).copy(alpha = 0.12f * lightOcclusion), // Subtle magenta chromatic edge fringe
                        center = Offset(cx, cy),
                        radius = maxDim * 0.70f
                    )
                )

                // Pass 3: Subtle Asymmetric Ambient Skylight Sheen
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f * lightOcclusion),
                            Color.Transparent,
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.75f, h * 0.75f)
                    )
                )
            }

            // 2.4 DIRECT GLASS ELEMENT HIGHLIGHT CONTOUR (Authentic Beveled Optical Lens Glint)
            // No exterior frame - the highlight contour is integrated directly onto the glass element perimeter
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val strokeW = 1.0.dp.toPx()
                val lensPath = buildSquirclePath(
                    cx = cx,
                    cy = cy,
                    a = cx - strokeW / 2f,
                    b = cy - strokeW / 2f,
                    n = 3.0f
                )

                // Lens Edge Chamfer Glint: top-left catches ambient key light, bottom-right softly fades
                drawPath(
                    path = lensPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f * lightOcclusion),
                            Color.White.copy(alpha = 0.18f * lightOcclusion),
                            Color(0x18FFFFFF),
                            Color(0x30000000)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    ),
                    style = Stroke(width = strokeW)
                )
            }
        }
    }
}

/**
 * Far Right: Classic Point-and-Shoot Mechanical Slide Switch.
 * Authentic camera body mechanism inspired by Contax T2, Olympus Mju, and Leica Minilux slide selectors:
 * - Scheme 2 Pyramid Architecture:
 *   - 36dp compact layout footprint strictly matching left gallery hatch (36dp)
 *   - Purely planar horizontal mechanism: operates along X-axis (-10dp to +10dp), strictly ZERO Z-axis scale or depression
 * - Upper Stencil Stage (Dual Mode Icons & Diffuse LED Glow):
 *   - Left: Solid Chiseled Alpine Twin-Peaks & Sun (Landscape / Rear Camera, Cool Cyan/White LED glow)
 *   - Right: Classical Solid Cameo Bust (Portrait / Front Camera, Warm Amber LED glow)
 *   - Inactive icon stays as a solid matte-steel laser engraving (Color 0xFF9492A2)
 * - Lower Recessed Slide Bay (Precision Machined Channel, 36dp x 13.5dp):
 *   - Full 36dp horizontal chassis span matching gallery hatch width
 *   - Mechanical detent travel stops at left (-10dp) and right (+10dp)
 * - Solid Metal Slider Plunger (14dp x 11dp, Cold-Rolled Satin Titanium):
 *   - 4 precision-machined vertical gripping flutes (knurled anti-slip ridges)
 *   - Central beveled index datum line & pointing arrowhead pip
 *   - Crisp Ball-Detent Snap (dampingRatio = 1.0f, stiffness = 1400f): ZERO bounce or oscillation
 *   - Tactile ball-detent confirmation click on switch lock (HapticFeedbackConstants.CONFIRM)
 */
@Composable
private fun MechanicalSquircleSlideSwitch(
    lensFacing: LensFacingMode,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape = lensFacing == LensFacingMode.BACK
    // Left (-10dp) for Landscape; Right (+10dp) for Portrait
    val targetOffset = if (isLandscape) -10.0f else 10.0f

    // Heavy mechanical ball-detent snap: dampingRatio = 1.0f (strictly critically damped), zero bounce or overshoot
    val animatedOffsetDp by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = 1400f
        ),
        label = "mechanicalSliderOffset"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current

    // Ball-detent mechanical snap haptic feedback when switched to new lens facing
    LaunchedEffect(lensFacing) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    // Layout footprint is strictly 36dp, perfectly matching GallerySquircleOpticalHatch (Scheme 2)
    // Note: Planar mechanical slide switch operates purely along horizontal X-axis, strictly ZERO Z-axis scale or depression
    Box(
        modifier = modifier
            .size(36.dp)
            .alpha(if (enabled) 1.0f else 0.35f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // =====================================================================
            // 1. UPPER STENCIL STAGE: DUAL MODE ICONS & DIFFUSE LED BACKLIGHT
            // Laser-etched directly onto chassis, vertically centered in 36dp bounds
            // =====================================================================
            val iconY = cy - 8.8.dp.toPx()
            val landX = cx - 10.0.dp.toPx() // Left stop alignment (-10dp)
            val portX = cx + 10.0.dp.toPx() // Right stop alignment (+10dp)
            val yBase = iconY + 2.2.dp.toPx() // Shared ground baseline for both icons

                // Unselected laser-engraving titanium color (high visibility solid matte metal)
                val unselectedEngravingColor = Color(0xFF9492A2)
                val unselectedShadeColor = Color(0xFF767484)

                // ---------------------------------------------------------------------
                // (A) LEFT: Landscape Mode (Alpine Mountain Range & Decisive Sun Pip)
                // Total bounding box: Width 8.4dp, Height ~5.8dp
                // ---------------------------------------------------------------------
                val leftPeakLit = Path().apply {
                    moveTo(landX - 4.2.dp.toPx(), yBase)
                    lineTo(landX - 1.2.dp.toPx(), iconY - 2.4.dp.toPx())
                    lineTo(landX - 0.2.dp.toPx(), yBase)
                    close()
                }
                val leftPeakShaded = Path().apply {
                    moveTo(landX - 1.2.dp.toPx(), iconY - 2.4.dp.toPx())
                    lineTo(landX + 0.6.dp.toPx(), yBase)
                    lineTo(landX - 0.2.dp.toPx(), yBase)
                    close()
                }
                val rightPeak = Path().apply {
                    moveTo(landX + 0.5.dp.toPx(), yBase)
                    lineTo(landX + 2.4.dp.toPx(), iconY - 1.1.dp.toPx())
                    lineTo(landX + 4.2.dp.toPx(), yBase)
                    close()
                }

                if (isLandscape) {
                    // Physical cool-cyan/white diffused LED backlight glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.00f to Color(0x65A5F3FC),
                            0.45f to Color(0x2806B6D4),
                            0.78f to Color(0x0C00E5FF),
                            1.00f to Color.Transparent,
                            center = Offset(landX, iconY),
                            radius = 8.0.dp.toPx()
                        ),
                        radius = 8.0.dp.toPx(),
                        center = Offset(landX, iconY)
                    )

                    val sunlitColor = Color.White.copy(alpha = 0.98f)
                    val shadeColor = Color(0xFFD6DBE4).copy(alpha = 0.76f)
                    val subPeakColor = Color.White.copy(alpha = 0.92f)

                    drawPath(path = leftPeakLit, color = sunlitColor)
                    drawPath(path = leftPeakShaded, color = shadeColor)
                    drawPath(path = rightPeak, color = subPeakColor)

                    // Sun / aperture pip
                    drawCircle(
                        color = sunlitColor,
                        radius = 0.85.dp.toPx(),
                        center = Offset(landX + 2.5.dp.toPx(), iconY - 2.6.dp.toPx())
                    )
                    // Baseline grounding bar
                    drawLine(
                        color = sunlitColor.copy(alpha = 0.85f),
                        start = Offset(landX - 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        end = Offset(landX + 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        strokeWidth = 0.65.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    // Inactive state: Solid, crisp laser-etched titanium engraving (NO glow, highly visible)
                    drawPath(path = leftPeakLit, color = unselectedEngravingColor)
                    drawPath(path = leftPeakShaded, color = unselectedShadeColor)
                    drawPath(path = rightPeak, color = unselectedEngravingColor)

                    drawCircle(
                        color = unselectedEngravingColor,
                        radius = 0.85.dp.toPx(),
                        center = Offset(landX + 2.5.dp.toPx(), iconY - 2.6.dp.toPx())
                    )
                    drawLine(
                        color = unselectedShadeColor,
                        start = Offset(landX - 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        end = Offset(landX + 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        strokeWidth = 0.65.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // ---------------------------------------------------------------------
                // (B) RIGHT: Portrait Mode (Classical Cameo Silhouette Bust)
                // Total bounding box: Width 8.4dp (identical to landscape), Height ~5.8dp
                // Head: center (portX, iconY - 1.35dp), radius 1.65dp (diameter 3.3dp)
                // Shoulders: from portX - 4.2dp to portX + 4.2dp, resting on yBase
                // ---------------------------------------------------------------------
                val headCenterY = iconY - 1.35.dp.toPx()
                val headRadius = 1.65.dp.toPx()

                val bustSilhouette = Path().apply {
                    val neckY = iconY + 0.65.dp.toPx()
                    moveTo(portX - 1.2.dp.toPx(), neckY)
                    cubicTo(
                        portX - 2.4.dp.toPx(), iconY + 0.85.dp.toPx(),
                        portX - 3.7.dp.toPx(), iconY + 1.5.dp.toPx(),
                        portX - 4.2.dp.toPx(), yBase
                    )
                    lineTo(portX + 4.2.dp.toPx(), yBase)
                    cubicTo(
                        portX + 3.7.dp.toPx(), iconY + 1.5.dp.toPx(),
                        portX + 2.4.dp.toPx(), iconY + 0.85.dp.toPx(),
                        portX + 1.2.dp.toPx(), neckY
                    )
                    close()
                }

                if (!isLandscape) {
                    // Physical warm-amber diffused LED backlight glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.00f to Color(0x80FDE68A),
                            0.45f to Color(0x38F59E0B),
                            0.78f to Color(0x12D97706),
                            1.00f to Color.Transparent,
                            center = Offset(portX, iconY),
                            radius = 8.0.dp.toPx()
                        ),
                        radius = 8.0.dp.toPx(),
                        center = Offset(portX, iconY)
                    )

                    val portGlowColor = OneShotTheme.AmberGrain.copy(alpha = 0.98f)
                    // Head: solid full circular cameo
                    drawCircle(
                        color = portGlowColor,
                        radius = headRadius,
                        center = Offset(portX, headCenterY)
                    )
                    // Shoulders / bust silhouette
                    drawPath(
                        path = bustSilhouette,
                        color = portGlowColor.copy(alpha = 0.94f)
                    )
                    // Baseline grounding bar (matching Landscape)
                    drawLine(
                        color = portGlowColor.copy(alpha = 0.85f),
                        start = Offset(portX - 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        end = Offset(portX + 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        strokeWidth = 0.65.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    // Inactive state: Solid, crisp laser-etched titanium engraving (NO glow, perfectly visible)
                    // Head is completely solid and easily recognizable!
                    drawCircle(
                        color = unselectedEngravingColor,
                        radius = headRadius,
                        center = Offset(portX, headCenterY)
                    )
                    drawPath(
                        path = bustSilhouette,
                        color = unselectedEngravingColor
                    )
                    drawLine(
                        color = unselectedShadeColor,
                        start = Offset(portX - 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        end = Offset(portX + 4.2.dp.toPx(), yBase + 0.4.dp.toPx()),
                        strokeWidth = 0.65.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

            // =====================================================================
            // 2. LOWER RECESSED SLIDE BAY (Machined Horizontal Channel)
            // =====================================================================
            val trackY = cy + 4.5.dp.toPx()
            val trackW = 36.0.dp.toPx()
            val trackH = 13.5.dp.toPx()
            val trackCorner = 3.0.dp.toPx()

            val trackLeft = cx - trackW / 2f
            val trackTop = trackY - trackH / 2f
            val trackRect = Rect(trackLeft, trackTop, trackLeft + trackW, trackTop + trackH)

            // 2.1 Deep Machined Channel Cavity (Dark Moat Well)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF040406), Color(0xFF0A090F), Color(0xFF100F15)),
                    start = Offset(trackLeft, trackTop),
                    end = Offset(trackLeft, trackTop + trackH)
                ),
                topLeft = trackRect.topLeft,
                size = trackRect.size,
                cornerRadius = CornerRadius(trackCorner, trackCorner)
            )

            // 2.2 Channel Upper Wall Drop Shadow
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x90000000), Color(0x30000000), Color.Transparent),
                    startY = trackTop,
                    endY = trackTop + 3.0.dp.toPx()
                ),
                topLeft = trackRect.topLeft,
                size = trackRect.size,
                cornerRadius = CornerRadius(trackCorner, trackCorner)
            )

            // 2.3 Channel Bottom Rail Specular Edge
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x20FFFFFF).copy(alpha = 0.15f)),
                    startY = trackTop + trackH - 1.5.dp.toPx(),
                    endY = trackTop + trackH
                ),
                topLeft = trackRect.topLeft,
                size = trackRect.size,
                cornerRadius = CornerRadius(trackCorner, trackCorner)
            )

            // 2.4 Machined Slide Rail Track Lines (Horizontal Guide Rails)
            val railY1 = trackY - 2.8.dp.toPx()
            val railY2 = trackY + 2.8.dp.toPx()
            drawLine(
                color = Color(0x18FFFFFF),
                start = Offset(trackLeft + 2.dp.toPx(), railY1),
                end = Offset(trackLeft + trackW - 2.dp.toPx(), railY1),
                strokeWidth = 0.5.dp.toPx()
            )
            drawLine(
                color = Color(0x18FFFFFF),
                start = Offset(trackLeft + 2.dp.toPx(), railY2),
                end = Offset(trackLeft + trackW - 2.dp.toPx(), railY2),
                strokeWidth = 0.5.dp.toPx()
            )

            // 2.5 Ball-Detent Mechanical Travel Stops (Left & Right indents)
            drawCircle(color = Color(0xFF060509), radius = 1.0.dp.toPx(), center = Offset(landX, trackY))
            drawCircle(color = Color(0xFF060509), radius = 1.0.dp.toPx(), center = Offset(portX, trackY))
        }

        // =====================================================================
        // 3. SOLID METAL SLIDER PLUNGER (Cold-Rolled Satin Titanium Block)
        // Slides horizontally between -10dp (Left) and +10dp (Right)
        // =====================================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val trackY = cy + 4.5.dp.toPx()

                val sliderOffsetPx = animatedOffsetDp.dp.toPx()
                val knobCenterX = cx + sliderOffsetPx
                val knobCenterY = trackY

                val knobW = 14.0.dp.toPx()
                val knobH = 11.0.dp.toPx()
                val knobCorner = 2.0.dp.toPx()
                val knobLeft = knobCenterX - knobW / 2f
                val knobTop = knobCenterY - knobH / 2f

                // 2.3.1 Slider Ambient Drop Shadow onto Track Floor
                val shadowOffset = 1.0.dp.toPx()
                drawRoundRect(
                    color = Color(0x80000000),
                    topLeft = Offset(knobLeft + shadowOffset, knobTop + shadowOffset * 1.2f),
                    size = Size(knobW, knobH),
                    cornerRadius = CornerRadius(knobCorner, knobCorner)
                )

                // 2.3.2 Solid Titanium Plunger Body
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF8C8A9A),
                            Color(0xFFAFAEC0),
                            Color(0xFF706E7E),
                            Color(0xFF565463)
                        ),
                        start = Offset(knobLeft, knobTop),
                        end = Offset(knobLeft + knobW, knobTop + knobH)
                    ),
                    topLeft = Offset(knobLeft, knobTop),
                    size = Size(knobW, knobH),
                    cornerRadius = CornerRadius(knobCorner, knobCorner)
                )

                // 2.3.3 CNC Diamond Chamfer Perimeter Bevel
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.20f),
                            Color(0x30000000),
                            Color(0x60000000)
                        ),
                        start = Offset(knobLeft, knobTop),
                        end = Offset(knobLeft + knobW, knobTop + knobH)
                    ),
                    topLeft = Offset(knobLeft, knobTop),
                    size = Size(knobW, knobH),
                    cornerRadius = CornerRadius(knobCorner, knobCorner),
                    style = Stroke(width = 0.65.dp.toPx())
                )

                // 2.3.4 4 Precision-Machined Vertical Gripping Flutes (Knurled Anti-Slip Ribs)
                val fluteSpacing = knobW / 5f
                for (i in 1..4) {
                    val fluteX = knobLeft + i * fluteSpacing
                    // Shadow groove
                    drawLine(
                        color = Color(0x65000000),
                        start = Offset(fluteX, knobTop + 1.8.dp.toPx()),
                        end = Offset(fluteX, knobTop + knobH - 1.8.dp.toPx()),
                        strokeWidth = 0.7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Specular ridge line
                    drawLine(
                        color = Color.White.copy(alpha = 0.50f),
                        start = Offset(fluteX - 0.35.dp.toPx(), knobTop + 1.8.dp.toPx()),
                        end = Offset(fluteX - 0.35.dp.toPx(), knobTop + knobH - 1.8.dp.toPx()),
                        strokeWidth = 0.35.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 2.3.5 Triangular Index Pip at Top Center of Slider (Pointing Up to Mode Stencil)
                val arrowTipY = knobTop - 1.6.dp.toPx()
                val arrowBaseY = knobTop + 0.3.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(knobCenterX, arrowTipY)
                    lineTo(knobCenterX - 1.6.dp.toPx(), arrowBaseY)
                    lineTo(knobCenterX + 1.6.dp.toPx(), arrowBaseY)
                    close()
                }

                val pipColor = if (isLandscape) OneShotTheme.FilmSilver else OneShotTheme.AmberGrain
                drawPath(path = arrowPath, color = pipColor.copy(alpha = 0.98f))

                // Illuminated Central Index Pip on Slider Face
                drawCircle(
                    color = pipColor.copy(alpha = 1.0f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(knobCenterX, knobCenterY)
                )
            }
        }
    }


/**
 * Left Shutter: Authentic Skeuomorphic Live Photo Actuator.
 * Features the internationally recognized Live Photo trinity symbol:
 * - Solid core decisive frame pip
 * - Continuous orbital audio-visual time groove
 * - 12-segment dotted frame halo
 * Laser-etched into an ergonomic concave dark-titanium mechanical plunger.
 * In standby: tactile cold-silver machined lines.
 * During capture: transforms into rich glowing warm Amber Grain (OneShot film aesthetic).
 * Pure Z-axis mechanical borehole depression with dynamic shadow.
 */
@Composable
private fun MotionPhotoShutterButton(
    isCapturing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // Touch-Down Instant Haptic Snap: Trigger authentic mechanical microswitch detent the exact millisecond finger contacts glass
    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    val depression by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 750f
        ),
        label = "motionDepression"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .alpha(if (enabled) 1.0f else 0.35f)
            .clickable(
                enabled = enabled && !isCapturing,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val totalRadius = w / 2f - 1.dp.toPx()

            // 1. FIXED DARK-TITANIUM MACHINE COLLAR (Rigid chassis, no scaling)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E2C33), Color(0xFF1B1A20), Color(0xFF100F14)),
                    center = Offset(cx, cy),
                    radius = totalRadius
                ),
                radius = totalRadius,
                center = Offset(cx, cy)
            )

            // Precision Outer Collar 45° Diamond Chamfer (Top-left natural gleam, bottom clean dark shade)
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color(0xFF5A5862),
                    0.25f to Color(0xFF222026), // Bottom is dark shade, zero bottom line
                    0.50f to Color(0xFF6C6A76),
                    0.65f to Color(0xFFB0AEB8), // Top-left gentle glint
                    0.80f to Color(0xFF7E7C88),
                    1.00f to Color(0xFF5A5862),
                    center = Offset(cx, cy)
                ),
                radius = totalRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.85.dp.toPx())
            )

            // 2. RIGID BOREHOLE WELL & RECESS LIGHTING
            val boreOuterRadius = totalRadius - 2.8.dp.toPx()
            val moatWidth = 1.4.dp.toPx()
            val dishRadius = boreOuterRadius - moatWidth

            // 2.1 Borehole cavity dark shadow (annular drop moat - strictly constant width)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF08070A), Color(0xFF020103)),
                    center = Offset(cx, cy),
                    radius = boreOuterRadius
                ),
                radius = boreOuterRadius,
                center = Offset(cx, cy)
            )

            // 3. RIGID MOVING CONCAVE TITANIUM PLUNGER DISH (100% Constant Physical Dimensions)
            // Light occlusion factor: subtle 15% step-down (restrained, elegant optical shift)
            val lightOcclusion = 1.0f - (0.15f * depression)
            val lightShiftY = 2.0.dp.toPx() * depression

            // 3.1 Dish Concave Body Shading (Upper rim in back-shade, lower basin collecting light)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF484650), // Upper rim back-shade
                        Color(0xFF646270),
                        Color(0xFF908E9E),
                        Color(0xFFB4B2C2)  // Basin collecting specular light
                    ),
                    start = Offset(cx - dishRadius * 0.7f, cy - dishRadius * 0.9f + lightShiftY),
                    end = Offset(cx + dishRadius * 0.4f, cy + dishRadius * 0.8f + lightShiftY)
                ),
                radius = dishRadius,
                center = Offset(cx, cy)
            )

            // 3.2 Perimeter 45° Polished Bevel Rim (With Specular Occlusion on the upper rim!)
            val topBevelAlpha = (1.0f - 0.70f * depression)
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color.White.copy(alpha = 0.20f * lightOcclusion),
                    0.25f to Color.Transparent, // Bottom clean, no extra line
                    0.50f to Color.White.copy(alpha = 0.25f * lightOcclusion),
                    0.65f to Color(0xFFEDEBF6).copy(alpha = 0.75f * lightOcclusion * topBevelAlpha), // Top-left specular highlight dims
                    0.80f to Color.White.copy(alpha = 0.35f * lightOcclusion * topBevelAlpha),
                    1.00f to Color.White.copy(alpha = 0.20f * lightOcclusion),
                    center = Offset(cx, cy)
                ),
                radius = dishRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx())
            )

            // 4. THE ICONIC LIVE PHOTO CONCENTRIC TRINITY (Laser Debossed Machine Symbol)
            // Color State: Cold Film Silver (Standby) -> Pure Warm Amber Grain (Capturing)
            val symbolColor = if (isCapturing) {
                OneShotTheme.AmberGrain
            } else {
                Color(0xFFDCDAE6).copy(alpha = 0.90f * lightOcclusion)
            }
            val symbolShadow = Color(0x38000000)

            // 4.1 Layer 1: Central Decisive Frame Pip (Solid core)
            val rCore = dishRadius * 0.16f
            // Subtle deboss shadow
            drawCircle(
                color = symbolShadow,
                radius = rCore,
                center = Offset(cx, cy - 0.5.dp.toPx())
            )
            // Core Pip
            drawCircle(
                color = symbolColor,
                radius = rCore,
                center = Offset(cx, cy)
            )

            // 4.2 Layer 2: Breathable 12-Segment Dotted Motion Frame Halo (Single outer dashed ring)
            // Removed crowded solid middle circle per leadership direction to maximize dish metal breathing room
            val rOuter = dishRadius * 0.60f
            val segments = 12
            val sweepAngle = 18f
            val gapAngle = 12f
            for (i in 0 until segments) {
                val startAngle = i * (sweepAngle + gapAngle)
                // Debossed shadow arc
                drawArc(
                    color = symbolShadow,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - rOuter, cy - rOuter - 0.5.dp.toPx()),
                    size = Size(rOuter * 2f, rOuter * 2f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
                // Front symbol arc
                drawArc(
                    color = symbolColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - rOuter, cy - rOuter),
                    size = Size(rOuter * 2f, rOuter * 2f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Center Shutter: The Decisive Frame Mechanical Shutter Button (Leica M Concave Edition).
 * Recreates the authentic mechanical rangefinder shutter assembly with true Z-axis physics:
 * - Fixed Machine Collar:
 *   - Outer 88-tooth CNC cold-steel collar & diamond chamfer are 100% rigid on the camera body (NO incorrect outer scaling)
 *   - Remains completely stationary during press
 * - Authentic Z-Axis Plunger Depression (Borehole Occlusion):
 *   - Orthographic top-down perspective: NO unnatural sideways/downward Y-axis slippage
 *   - Pure vertical plunger travel into the metal borehole sleeve
 *   - Dynamic cast shadow: As the button sinks into the bore, the sleeve walls cast a deep 3.5dp annular occlusion shadow and upper crescent shadow
 *   - Subtle optical perspective compression on the inner dish (scale 1.0 -> 0.96)
 *   - Light attenuation: Specular reflections naturally dim as the surface recesses below the housing rim
 * - Micro M3 Threaded Cable Release Socket:
 *   - Strict concentric alignment with warm vintage brass bushing glint and fine internal threads
 */
@Composable
private fun MechanicalStillShutterButton(
    isCapturing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // Instant tactile micro-switch haptic snap on touch-down
    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    // Authentic mechanical travel depth: 0.0f (rest / flush) -> 1.0f (fully bottomed out in sleeve)
    val depression by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = 0.65f, // Tight mechanical resistance & spring return
            stiffness = 750f      // Crisp tactile trigger
        ),
        label = "mechanicalDepression"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            // Outer collar is fixed to chassis — NO scale modifier on root container!
            .alpha(if (enabled) 1.0f else 0.35f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val totalRadius = w / 2f - 1.dp.toPx()

            // =========================================================================
            // PART 1: RIGID MACHINE COLLAR (Completely fixed to camera body)
            // =========================================================================

            // 1.1 Cold Steel Outer Collar Base (Matte gunmetal chassis)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E2E38), Color(0xFF1B1B22), Color(0xFF101014)),
                    center = Offset(cx, cy),
                    radius = totalRadius
                ),
                radius = totalRadius,
                center = Offset(cx, cy)
            )

            // 1.2 88-Tooth High-Density Precision Knurled Grip (Micro-teeth replacing coarse cogs)
            val teethCount = 88
            val knurlR1 = totalRadius - 2.8.dp.toPx()
            val knurlR2 = totalRadius - 0.4.dp.toPx()
            for (i in 0 until teethCount) {
                val angleDeg = (i * 360f / teethCount)
                val rad = Math.toRadians(angleDeg.toDouble())
                val cosVal = cos(rad).toFloat()
                val sinVal = sin(rad).toFloat()
                // Directional lighting from top-left (-135°): highlights on top-left, muted on bottom-right
                val lightVal = ((-cosVal - sinVal) / 2.8f + 0.5f).coerceIn(0.2f, 1.0f)
                val toothColor = Color(0xFFA2A2B4).copy(alpha = 0.38f * lightVal + 0.12f)
                drawLine(
                    color = toothColor,
                    start = Offset(cx + knurlR1 * cosVal, cy + knurlR1 * sinVal),
                    end = Offset(cx + knurlR2 * cosVal, cy + knurlR2 * sinVal),
                    strokeWidth = 0.75.dp.toPx()
                )
            }

            // 1.3 Outer Collar 45° Diamond Chamfer (Top-left natural gleam, bottom clean dark shade)
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color(0xFF5E5E6C),
                    0.25f to Color(0xFF24242C), // Bottom dark shade, zero bright line
                    0.50f to Color(0xFF727280),
                    0.65f to Color(0xFFB8B8C6), // Top-left glint
                    0.80f to Color(0xFF888898),
                    1.00f to Color(0xFF5E5E6C),
                    center = Offset(cx, cy)
                ),
                radius = totalRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.9.dp.toPx())
            )

            // 1.4 Precision-Milled Borehole Sleeve Rim
            val boreOuterRadius = totalRadius - 3.2.dp.toPx()
            drawCircle(
                color = Color(0xFF3C3C46),
                radius = boreOuterRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx())
            )

            // =========================================================================
            // PART 2: DYNAMIC BOREHOLE WELL & RECESS LIGHTING (Z-Axis travel)
            // =========================================================================

            // Rigid mechanical tolerance clearance (Constant 1.8dp width, zero expansion)
            val moatWidth = 1.8.dp.toPx()
            val dishRadius = boreOuterRadius - moatWidth

            // 2.1 Borehole cavity dark shadow (annular drop moat - strictly constant width)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF040406),
                        Color(0xFF020203)
                    ),
                    center = Offset(cx, cy),
                    radius = boreOuterRadius
                ),
                radius = boreOuterRadius,
                center = Offset(cx, cy)
            )

            // =========================================================================
            // PART 3: MOVING CONCAVE SOFT-RELEASE PLUNGER DISH (Top-down concentric, Rigid)
            // =========================================================================

            // Ambient light occlusion factor: subtle 15% step-down (restrained, elegant optical shift)
            val lightOcclusion = 1.0f - (0.15f * depression)
            val lightShiftY = 2.4.dp.toPx() * depression
            val glintRotationDeg = depression * 4.5f

            // 3.1 Dish Concave Body Shading (Upper rim in back-shade, lower basin collecting light)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF666672), // Upper rim shade
                        Color(0xFF8E8E9C), // Intermediate curve
                        Color(0xFFC8C8D6), // Basin receiving specular glow
                        Color(0xFFE8E8F2)  // Bottom interior curvature
                    ),
                    start = Offset(cx - dishRadius * 0.7f, cy - dishRadius * 0.9f + lightShiftY),
                    end = Offset(cx + dishRadius * 0.4f, cy + dishRadius * 0.8f + lightShiftY)
                ),
                radius = dishRadius,
                center = Offset(cx, cy)
            )

            // 3.2 Anisotropic Brushed Metal Sheen & Micro CD Lathe Rings (with subtle glint rotation)
            rotate(degrees = glintRotationDeg, pivot = Offset(cx, cy)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.32f * lightOcclusion),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.26f * lightOcclusion),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.32f * lightOcclusion)
                        ),
                        center = Offset(cx, cy)
                    ),
                    radius = dishRadius - 0.5.dp.toPx(),
                    center = Offset(cx, cy)
                )

                // 3.3 Micro CD Lathe Rings (Fine concentric turning grooves)
                val latheRings = 7
                for (k in 1..latheRings) {
                    val ringFrac = 0.28f + 0.62f * (k.toFloat() / latheRings)
                    val ringR = dishRadius * ringFrac
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f * lightOcclusion),
                                Color(0x06000000),
                                Color.White.copy(alpha = 0.22f * lightOcclusion),
                                Color(0x06000000),
                                Color.White.copy(alpha = 0.18f * lightOcclusion)
                            ),
                            center = Offset(cx, cy)
                        ),
                        radius = ringR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                }
            }

            // 3.4 Perimeter 45° Polished Bevel (Specular occlusion on top rim as it recesses, bottom clean)
            val topBevelAlpha = (1.0f - 0.70f * depression)
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color.White.copy(alpha = 0.22f * lightOcclusion),
                    0.25f to Color.Transparent, // Bottom clean, no extra line
                    0.50f to Color.White.copy(alpha = 0.28f * lightOcclusion),
                    0.65f to Color.White.copy(alpha = 0.80f * lightOcclusion * topBevelAlpha), // Top-left specular highlight dims
                    0.80f to Color.White.copy(alpha = 0.40f * lightOcclusion * topBevelAlpha),
                    1.00f to Color.White.copy(alpha = 0.22f * lightOcclusion),
                    center = Offset(cx, cy)
                ),
                radius = dishRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.85.dp.toPx())
            )

            // =========================================================================
            // PART 4: MICRO M3 THREADED CABLE RELEASE SOCKET (Concentric on Plunger)
            // =========================================================================
            val socketRadius = dishRadius * 0.18f

            // 4.1 Warm Vintage Brass Bushing Outer Rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD278), Color(0xFFC48E32), Color(0xFF6B4B14)),
                    start = Offset(cx - socketRadius, cy - socketRadius),
                    end = Offset(cx + socketRadius, cy + socketRadius)
                ),
                radius = socketRadius,
                center = Offset(cx, cy)
            )

            // 4.2 Deep Steel Cavity Moat
            val socketInnerR = socketRadius * 0.82f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF26221D), Color(0xFF090807)),
                    center = Offset(cx, cy),
                    radius = socketInnerR
                ),
                radius = socketInnerR,
                center = Offset(cx, cy)
            )

            // 4.3 Fine Internal Mechanical Threads (Glint lines)
            drawCircle(
                color = Color(0xFFDEB065).copy(alpha = 0.65f * lightOcclusion),
                radius = socketInnerR * 0.68f,
                center = Offset(cx, cy),
                style = Stroke(width = 0.45.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF888072).copy(alpha = 0.5f * lightOcclusion),
                radius = socketInnerR * 0.42f,
                center = Offset(cx, cy),
                style = Stroke(width = 0.4.dp.toPx())
            )
            // 4.4 Central Plunger Core Pit
            drawCircle(
                color = Color(0xFF020202),
                radius = socketInnerR * 0.22f,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Right Shutter: ARRI/RED Cinema Video Fresnel Tally Switch.
 * Authentic movie-camera skeuomorphic engineering:

 * Right Shutter: ARRI/RED Dedicated Cinema Video REC Switch.
 * Authentic movie-camera skeuomorphic engineering:
 * - Rigid Protective Metal Guard Collar (Raised dark titanium bezel preventing accidental triggers, 100% fixed)
 * - Deep Recessed Borehole Sleeve with dynamic occlusion shadow on depression
 * - Dedicated Cinema Moat Halo Glow:
 *   - In standby: dark mechanical tolerance moat
 *   - During recording: a razor-thin, deep-crimson Tally Halo breathes from within the tolerance gap between plunger and collar
 * - Solid Gunmetal Actuator Plunger:
 *   - True physical metal actuator (NOT a flat glowing red bulb!)
 *   - Precision CNC 45° polished perimeter chamfer
 *   - Pure Z-axis mechanical borehole depression with spring damping
 * - Recessed Crimson Enamel Dot:
 *   - Precision debossed optical red jewel inset into the center of the metal button
 *   - Standby: rich deep vintage darkroom red
 *   - Recording: activated vivid cinema crimson with specular glint (NO unnatural white square block)
 */
@Composable
private fun CinemaVideoShutterButton(
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // Instant tactile micro-switch haptic snap on touch-down
    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    // Authentic mechanical travel depth: 0.0f (rest / flush) -> 1.0f (fully bottomed out in guard)
    val depression by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = 0.65f, // Firm cinema switch resistance
            stiffness = 750f
        ),
        label = "videoDepression"
    )

    val transition = rememberInfiniteTransition(label = "tallyBreath")
    val tallyGlow by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tallyGlow"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            // Rigid guard collar is fixed to chassis — NO scale modifier on root container!
            .alpha(if (enabled) 1.0f else 0.35f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val totalRadius = w / 2f - 1.dp.toPx()

            // 1. FIXED PROTECTIVE BEZEL GUARD (Cold-rolled cinema steel chassis)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2C2A30), Color(0xFF18171C), Color(0xFF0E0D12)),
                    center = Offset(cx, cy),
                    radius = totalRadius
                ),
                radius = totalRadius,
                center = Offset(cx, cy)
            )

            // 45° CNC Precision Chamfer Glint on Guard Outer Rim (Top-left natural gleam, bottom clean dark shade)
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color(0xFF5A5862),
                    0.25f to Color(0xFF201E24), // Bottom dark shade, zero bright line
                    0.50f to Color(0xFF6C6A74),
                    0.65f to Color(0xFFB0AEB8), // Top-left glint
                    0.80f to Color(0xFF7E7C86),
                    1.00f to Color(0xFF5A5862),
                    center = Offset(cx, cy)
                ),
                radius = totalRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.85.dp.toPx())
            )

            // 2. RIGID BOREHOLE WELL & RECESS LIGHTING (Zero scale, rigid physical gap)
            val boreOuterRadius = totalRadius - 2.8.dp.toPx()
            val moatWidth = 1.4.dp.toPx()
            val dishRadius = boreOuterRadius - moatWidth

            // 2.1 Borehole cavity dark shadow (annular drop moat - strictly constant width)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0A080C), Color(0xFF020204)),
                    center = Offset(cx, cy),
                    radius = boreOuterRadius
                ),
                radius = boreOuterRadius,
                center = Offset(cx, cy)
            )

            // 2.3 THE REQUESTED MOAT TALLY HALO: Breathes from inside the tolerance gap during recording
            // Authentic two-pass optical emission: soft trench ambient spill + intense ruby core filament
            if (isRecording) {
                val haloRadius = (boreOuterRadius + dishRadius) / 2f
                // Pass 1: Soft deep-crimson optical diffuse bloom in the mechanical slit
                drawCircle(
                    color = Color(0xFFB71C1C).copy(alpha = tallyGlow * 0.50f),
                    radius = haloRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.4.dp.toPx())
                )
                // Pass 2: High-transmission optical ruby core
                drawCircle(
                    color = Color(0xFFFF2D55).copy(alpha = tallyGlow * 0.95f),
                    radius = haloRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 0.9.dp.toPx())
                )
            }

            // 3. SOLID GUNMETAL ACTUATOR PLUNGER (True physical metal button, 100% Constant Dimensions)
            val lightOcclusion = 1.0f - (0.15f * depression)
            val lightShiftY = 1.8.dp.toPx() * depression

            // 3.1 Gunmetal Plunger Body Shading
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF44424C), // Upper rim shade
                        Color(0xFF5E5C68), // Intermediate curve
                        Color(0xFF888694), // Basin receiving specular light
                        Color(0xFFA8A6B6)  // Cold-steel interior reflection
                    ),
                    start = Offset(cx - dishRadius * 0.7f, cy - dishRadius * 0.9f + lightShiftY),
                    end = Offset(cx + dishRadius * 0.4f, cy + dishRadius * 0.8f + lightShiftY)
                ),
                radius = dishRadius,
                center = Offset(cx, cy)
            )

            // 3.2 Perimeter 45° Polished Bevel Rim (With Specular Occlusion on the upper rim!)
            val topBevelAlpha = (1.0f - 0.70f * depression)
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color.White.copy(alpha = 0.20f * lightOcclusion),
                    0.25f to Color.Transparent, // Bottom clean, no extra line
                    0.50f to Color.White.copy(alpha = 0.25f * lightOcclusion),
                    0.65f to Color(0xFFEDEBF6).copy(alpha = 0.75f * lightOcclusion * topBevelAlpha),
                    0.80f to Color.White.copy(alpha = 0.35f * lightOcclusion * topBevelAlpha),
                    1.00f to Color.White.copy(alpha = 0.20f * lightOcclusion),
                    center = Offset(cx, cy)
                ),
                radius = dishRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx())
            )

            // 4. RECESSED CRIMSON ENAMEL DOT (True physical camera record inset)
            val dotRadius = dishRadius * 0.38f

            // 4.1 Debossed Chamfer Well Shadow in the metal surface (Precision micro-bore step)
            drawCircle(
                color = Color.Black.copy(alpha = 0.80f),
                radius = dotRadius + 0.5.dp.toPx(),
                center = Offset(cx, cy - 0.5.dp.toPx())
            )

            // 4.2 Enamel Core: Pure authentic industrial debossed paint filling
            // Standby: Deep vintage darkroom red with natural directional gradient
            // Recording: Vivid cinema crimson with rich optical saturation (No artificial white dots!)
            val dotBrush = if (isRecording) {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF3042),
                        Color(0xFFE50914),
                        Color(0xFF8A000A)
                    ),
                    center = Offset(cx - dotRadius * 0.20f, cy - dotRadius * 0.20f),
                    radius = dotRadius
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFBD1822).copy(alpha = lightOcclusion),
                        Color(0xFF900C15).copy(alpha = lightOcclusion),
                        Color(0xFF4E0308).copy(alpha = lightOcclusion)
                    ),
                    center = Offset(cx - dotRadius * 0.20f, cy - dotRadius * 0.20f),
                    radius = dotRadius
                )
            }
            drawCircle(
                brush = dotBrush,
                radius = dotRadius,
                center = Offset(cx, cy)
            )
        }
    }
}

private val CapsuleShape = RoundedCornerShape(50)


