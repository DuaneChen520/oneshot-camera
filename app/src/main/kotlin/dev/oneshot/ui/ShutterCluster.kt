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
import kotlin.math.cos
import kotlin.math.sin
import dev.oneshot.camera.MediaThumbnail
import dev.oneshot.camera.RecordingStatus
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
            // Far Left: Optical Viewfinder (OVF) Gallery Window (No Bulky Borders)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OneShotTheme.ChassisDark)
                    .border(
                        width = 1.dp,
                        color = if (latestMedia?.bitmap != null) OneShotTheme.AmberGrain.copy(alpha = 0.8f) else OneShotTheme.ChassisBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        enabled = !status.isRecording && !status.isMotionCapturing,
                        onClick = onOpenGallery
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (latestMedia?.bitmap != null) {
                    Image(
                        bitmap = latestMedia.bitmap.asImageBitmap(),
                        contentDescription = "最近底片 (取景器视窗)",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Viewfinder Etched Frameline Overlay on top of preview
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 1.dp.toPx()
                        val mark = 6.dp.toPx()
                        val pad = 4.dp.toPx()
                        val color = Color.White.copy(alpha = 0.8f)
                        val w = size.width
                        val h = size.height

                        // Top-Left corner
                        drawLine(color, Offset(pad, pad), Offset(pad + mark, pad), stroke)
                        drawLine(color, Offset(pad, pad), Offset(pad, pad + mark), stroke)
                        // Top-Right corner
                        drawLine(color, Offset(w - pad - mark, pad), Offset(w - pad, pad), stroke)
                        drawLine(color, Offset(w - pad, pad), Offset(w - pad, pad + mark), stroke)
                        // Bottom-Left corner
                        drawLine(color, Offset(pad, h - pad), Offset(pad + mark, h - pad), stroke)
                        drawLine(color, Offset(pad, h - pad), Offset(pad, h - pad - mark), stroke)
                        // Bottom-Right corner
                        drawLine(color, Offset(w - pad - mark, h - pad), Offset(w - pad, h - pad), stroke)
                        drawLine(color, Offset(w - pad, h - pad), Offset(w - pad, h - pad - mark), stroke)
                    }
                    if (latestMedia.isVideo) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶",
                                color = OneShotTheme.FilmSilver,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else {
                    // Optical Viewfinder Reticle & Brightlines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 1.dp.toPx()
                        val mark = 6.dp.toPx()
                        val pad = 5.dp.toPx()
                        val color = OneShotTheme.FilmSilver.copy(alpha = 0.85f)
                        val w = size.width
                        val h = size.height

                        // Corner Framelines
                        drawLine(color, Offset(pad, pad), Offset(pad + mark, pad), stroke)
                        drawLine(color, Offset(pad, pad), Offset(pad, pad + mark), stroke)
                        drawLine(color, Offset(w - pad - mark, pad), Offset(w - pad, pad), stroke)
                        drawLine(color, Offset(w - pad, pad), Offset(w - pad, pad + mark), stroke)
                        drawLine(color, Offset(pad, h - pad), Offset(pad + mark, h - pad), stroke)
                        drawLine(color, Offset(pad, h - pad), Offset(pad, h - pad - mark), stroke)
                        drawLine(color, Offset(w - pad - mark, h - pad), Offset(w - pad, h - pad), stroke)
                        drawLine(color, Offset(w - pad, h - pad), Offset(w - pad, h - pad - mark), stroke)

                        // Center Rangefinder Crosshair
                        val cx = w / 2f
                        val cy = h / 2f
                        val reticle = 3.dp.toPx()
                        drawLine(color.copy(alpha = 0.6f), Offset(cx - reticle, cy), Offset(cx + reticle, cy), stroke * 0.8f)
                        drawLine(color.copy(alpha = 0.6f), Offset(cx, cy - reticle), Offset(cx, cy + reticle), stroke * 0.8f)

                        // Top-Right Phosphor Green Ready Dot
                        drawCircle(
                            color = Color(0xFF10B981),
                            radius = 2.dp.toPx(),
                            center = Offset(w - pad - 2.dp.toPx(), pad + 2.dp.toPx())
                        )
                    }
                }
            }

            // Left Shutter: Motion Photo (Live Emulsion Shutter with concentric rings)
            MotionPhotoShutterButton(
                isCapturing = status.isMotionCapturing,
                enabled = !status.isRecording && !status.isStillCapturing,
                onClick = onCaptureMotionPhoto
            )

            // Center Shutter: Decisive Frame Still Photography (Precision Mechanical Shutter Assembly)
            MechanicalStillShutterButton(
                isCapturing = status.isStillCapturing,
                enabled = !status.isRecording && !status.isMotionCapturing,
                onClick = onTakeStillPhoto
            )

            // Right Shutter: Video Recording (Darkroom Red Tally Shutter)
            CinemaVideoShutterButton(
                isRecording = status.isRecording,
                enabled = !status.isStillCapturing && !status.isMotionCapturing,
                onClick = {
                    if (status.isRecording) onStopRecording() else onStartRecording()
                }
            )

            // Far Right: Skeuomorphic PL Cinema Lens Mount Flip Button
            LensMountFlipButton(
                enabled = !status.isRecording && !status.isMotionCapturing,
                onClick = onFlipCamera
            )
        }
    }
}

/**
 * Far Right: Skeuomorphic PL Cinema Lens Mount Flip Button.
 * Replicates the Arri/Cooke PL positive lock bayonet flange:
 * - Machined titanium-steel base with 3 locking lugs at 120°
 * - 4 precision Torx fastening screws
 * - Anti-reflective coated optical glass core (magenta / cyan iridescent reflection)
 * - Tactile 180° rotation detent on tap
 */
@Composable
private fun LensMountFlipButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var targetRotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "plMountRotation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "plMountScale"
    )

    Box(
        modifier = modifier
            .size(50.dp)
            .scale(scale)
            .alpha(if (enabled) 1.0f else 0.35f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    targetRotation += 180f
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(animatedRotation)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val baseRadius = w * 0.44f

            // 1. Dark titanium outer flange base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E2C33), Color(0xFF141318)),
                    center = Offset(cx, cy),
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF4A4652),
                radius = baseRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 2. Three Stainless Steel Locking Lugs at 120° intervals (-90°, 30°, 150°)
            val lugColor = Color(0xFFC0C0C8)
            val lugBorder = Color(0xFF5E5E68)
            val lugAngles = listOf(-90f, 30f, 150f)
            for (angleDeg in lugAngles) {
                val rad = Math.toRadians(angleDeg.toDouble())
                val lugDist = baseRadius * 0.98f
                val lx = cx + (lugDist * cos(rad)).toFloat()
                val ly = cy + (lugDist * sin(rad)).toFloat()

                rotate(degrees = angleDeg + 90f, pivot = Offset(lx, ly)) {
                    val lugW = 9.dp.toPx()
                    val lugH = 3.5.dp.toPx()
                    drawRoundRect(
                        color = lugColor,
                        topLeft = Offset(lx - lugW / 2, ly - lugH / 2),
                        size = Size(lugW, lugH),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                    drawRoundRect(
                        color = lugBorder,
                        topLeft = Offset(lx - lugW / 2, ly - lugH / 2),
                        size = Size(lugW, lugH),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                        style = Stroke(0.6.dp.toPx())
                    )
                }
            }

            // 3. Inner Machined Flange Face
            val innerRadius = w * 0.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1F1E24), Color(0xFF0F0E12)),
                    center = Offset(cx, cy),
                    radius = innerRadius
                ),
                radius = innerRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF38363F),
                radius = innerRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx())
            )

            // 4. Four Precision Torx Screw Heads at 45°, 135°, 225°, 315°
            val screwAngles = listOf(45f, 135f, 225f, 315f)
            val screwDist = innerRadius * 0.82f
            for (angleDeg in screwAngles) {
                val rad = Math.toRadians(angleDeg.toDouble())
                val sx = cx + (screwDist * cos(rad)).toFloat()
                val sy = cy + (screwDist * sin(rad)).toFloat()
                drawCircle(
                    color = Color(0xFF787884),
                    radius = 1.3.dp.toPx(),
                    center = Offset(sx, sy)
                )
                drawLine(
                    color = Color(0xFF28282E),
                    start = Offset(sx - 0.8.dp.toPx(), sy),
                    end = Offset(sx + 0.8.dp.toPx(), sy),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // 5. Optical Multi-Coated Lens Core (Magenta/Cyan/Amber AR coating sheen)
            val lensRadius = w * 0.24f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x99A855F7),
                        Color(0x6606B6D4),
                        Color(0x33F59E0B)
                    ),
                    start = Offset(cx - lensRadius, cy - lensRadius),
                    end = Offset(cx + lensRadius, cy + lensRadius)
                ),
                radius = lensRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0x66FFFFFF),
                radius = lensRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.75.dp.toPx())
            )

            // Specular Glare crescent
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x88FFFFFF), Color(0x00FFFFFF)),
                    start = Offset(cx - lensRadius * 0.7f, cy - lensRadius * 0.7f),
                    end = Offset(cx + lensRadius * 0.3f, cy + lensRadius * 0.3f)
                ),
                start = Offset(cx - lensRadius * 0.6f, cy - lensRadius * 0.6f),
                end = Offset(cx + lensRadius * 0.4f, cy - lensRadius * 0.2f),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // Center HUD Label: "PL" and curved flip arrow
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "↺",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "PL",
                color = OneShotTheme.AmberGrain,
                fontSize = 6.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = OneShotTheme.MonospaceFont,
                letterSpacing = 0.5.sp
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
            .size(54.dp)
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
            .size(54.dp)
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


