package dev.oneshot.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.oneshot.camera.CameraController
import dev.oneshot.camera.CapturedMediaType
import dev.oneshot.camera.GalleryLauncher
import dev.oneshot.camera.HapticManager
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.HistogramMode
import dev.oneshot.data.LensFacingMode
import dev.oneshot.data.CustomLut
import dev.oneshot.data.ParameterStore
import dev.oneshot.data.StillResolutionPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import dev.oneshot.ui.theme.OneShotTheme

@Composable
fun ShootingScreen(
    cameraController: CameraController,
    parameterStore: ParameterStore,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Permission Handling
    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true &&
                              permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OneShotTheme.ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = OneShotTheme.BRAND_NAME,
                    color = OneShotTheme.FilmSilver,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = OneShotTheme.MonospaceFont
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = OneShotTheme.SLOGAN_MAIN,
                    color = OneShotTheme.AmberGrain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.padding(2.dp))
                Text(
                    text = OneShotTheme.SLOGAN_SUB,
                    color = OneShotTheme.TitaniumGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.padding(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(permissionsToRequest) },
                    colors = ButtonDefaults.buttonColors(containerColor = OneShotTheme.AmberGrain),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "开启相机与光学传感器权限",
                        color = OneShotTheme.ObsidianBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
        return
    }

    // 2. State Observations
    val settings by parameterStore.settings.collectAsState()
    val zoomRange by cameraController.zoomRange.collectAsState()
    val currentZoom by cameraController.currentZoom.collectAsState()
    val exposureRange by cameraController.exposureRange.collectAsState()
    val exposureStep by cameraController.exposureStep.collectAsState()
    val currentEvIndex by cameraController.currentEvIndex.collectAsState()
    val focusPoint by cameraController.focusPoint.collectAsState()
    val recordingStatus by cameraController.recordingStatus.collectAsState()
    val capabilities by cameraController.capabilities.collectAsState()
    val histogramData by cameraController.histogramData.collectAsState()
    val latestMedia by cameraController.latestMedia.collectAsState()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isQMenuOpen by remember { mutableStateOf(false) }
    var isAbgmDialogOpen by remember { mutableStateOf(false) }
    var isShutterFlashVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var lutToDelete by remember { mutableStateOf<CustomLut?>(null) }

    val lutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    cameraController.customLutManager.importLut(uri, context.contentResolver)
                }
                result.onSuccess { customLut ->
                    parameterStore.addCustomLut(customLut, selectImmediately = true)
                    hapticManager.shutterClick()
                    Toast.makeText(
                        context,
                        "已导入胶卷「${customLut.name}」(${customLut.size}³)",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { error ->
                    hapticManager.tick()
                    Toast.makeText(
                        context,
                        "导入失败: ${error.message ?: "无效的 .cube 文件"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Shutter mirror blackout flash animation
    LaunchedEffect(recordingStatus.shutterAnimationTick) {
        if (recordingStatus.shutterAnimationTick > 0L) {
            isShutterFlashVisible = true
            delay(50)
            isShutterFlashVisible = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraController.unbind()
        }
    }

    // Viewfinder aspect ratio
    val viewportRatio = if (settings.aspectMode == AspectRatioMode.RATIO_4_3) 3f / 4f else 9f / 16f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Center Viewfinder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(viewportRatio)
                    .clipToBounds()
                    .pointerInput(previewViewRef) {
                        detectTapGestures { offset ->
                            val pv = previewViewRef ?: return@detectTapGestures
                            cameraController.setFocusAndMetering(pv.meteringPointFactory, offset.x, offset.y)
                            hapticManager.focusLocked()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomFactor, _ ->
                            cameraController.setZoom(currentZoom * zoomFactor)
                        }
                    }
                    .pointerInput(Unit) {
                        // Downward swipe opens Q-Menu drawer
                        detectDragGestures(
                            onDragStart = { verticalDragAccumulator = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                verticalDragAccumulator += dragAmount.y
                                if (verticalDragAccumulator > 60f) {
                                    isQMenuOpen = true
                                    hapticManager.tick()
                                    verticalDragAccumulator = 0f
                                }
                            }
                        )
                    }
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                            cameraController.bindCamera(lifecycleOwner, surfaceProvider)
                        }
                    },
                    update = { view ->
                        previewViewRef = view
                    }
                )

                // Optical Ground Glass Corner Framing Reticles (BRAND.md 4:3 Open Gate Aesthetics)
                OpticalFramingReticle(
                    modifier = Modifier.fillMaxSize()
                )

                // Mechanical Shutter Flash Reflex Mirror blackout overlay
                AnimatedVisibility(
                    visible = isShutterFlashVisible,
                    enter = fadeIn(animationSpec = tween(10)),
                    exit = fadeOut(animationSpec = tween(60))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                // Tap-to-focus and metering ring indicator
                FocusIndicator(point = focusPoint)

                // Live Optical Histogram & Waveform Scope (Top-Right of Viewfinder)
                LiveHistogramScope(
                    mode = settings.histogramMode,
                    data = histogramData,
                    onCycleMode = {
                        val nextMode = when (settings.histogramMode) {
                            HistogramMode.OFF -> HistogramMode.LUMA
                            HistogramMode.LUMA -> HistogramMode.RGB
                            HistogramMode.RGB -> HistogramMode.OFF
                        }
                        parameterStore.setHistogramMode(nextMode)
                        hapticManager.tick()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                )

                // OLED Screen Dimmer Overlay (Thermal/Power Saving during long recordings)
                ScreenDimmerOverlay(
                    isRecording = recordingStatus.isRecording,
                    enabled = settings.screenDimmerEnabled,
                    dimDelayMillis = 3500L
                )
            }
        }

        // 2. Overlays: Top Control Bar
        TopControlBar(
            settings = settings,
            capabilities = capabilities,
            onToggleAspect = {
                val next = if (settings.aspectMode == AspectRatioMode.RATIO_4_3) AspectRatioMode.RATIO_16_9 else AspectRatioMode.RATIO_4_3
                parameterStore.setAspectRatio(next)
                hapticManager.tick()
            },
            onToggleDynamicRange = {
                val next = if (settings.dynamicRange == DynamicRangeMode.HLG10) DynamicRangeMode.SDR else DynamicRangeMode.HLG10
                parameterStore.setDynamicRange(next)
                hapticManager.tick()
            },
            onToggleFps = {
                val supported = capabilities.supportedFpsPresets
                val currentIndex = supported.indexOf(settings.fpsPreset)
                val nextIndex = (if (currentIndex >= 0) currentIndex + 1 else 0) % supported.size
                val nextFps = supported[nextIndex]
                parameterStore.setFpsPreset(nextFps)
                hapticManager.tick()
            },
            onToggleDcg = {
                parameterStore.setDcgHdr(!settings.dcgHdrEnabled)
                hapticManager.tick()
            },
            onToggleMotionPhotoLive = {
                val next = !settings.motionPhotoLiveEnabled
                parameterStore.setMotionPhotoLive(next)
                hapticManager.tick()
            },
            onOpenQMenu = {
                isQMenuOpen = true
                hapticManager.tick()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Overlays: Bottom Pro HUD + 3-Shutter Cluster
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // In-place parameter gestures HUD (Shutter, ISO, EV, Zoom, Focus, WB)
            ProHud(
                settings = settings,
                zoomRange = zoomRange,
                currentZoom = currentZoom,
                onZoomChange = { cameraController.setZoom(it) },
                exposureRange = exposureRange,
                exposureStep = exposureStep,
                currentEvIndex = currentEvIndex,
                onEvChange = { cameraController.setEv(it) },
                onToggleAutoShutter = { parameterStore.setAutoShutter(it) },
                onShutterSpeedChange = { parameterStore.setShutterSpeedNanos(it) },
                onToggleAutoIso = { parameterStore.setAutoIso(it) },
                onIsoChange = { parameterStore.setIsoValue(it) },
                onToggleAutoFocus = { cameraController.setAutoFocus(it) },
                onFocusDistanceChange = { cameraController.setManualFocus(it) },
                onToggleAutoWb = { parameterStore.setAutoWb(it) },
                onCctChange = { parameterStore.setCctKelvin(it) },
                onUpdateHudPresets = { parameterStore.updateHudPresets(it) },
                onOpenWbDialog = { isAbgmDialogOpen = true },
                hapticManager = hapticManager
            )

            // Three-Shutter Cluster Bar
            ShutterCluster(
                status = recordingStatus,
                onTakeStillPhoto = { cameraController.takeStillPhoto() },
                onCaptureMotionPhoto = { cameraController.captureMotionPhoto() },
                onStartRecording = {
                    hapticManager.tick()
                    cameraController.startRecording()
                },
                onStopRecording = {
                    hapticManager.tick()
                    cameraController.stopRecording()
                },
                onFlipCamera = {
                    val next = if (settings.lensFacing == LensFacingMode.BACK) LensFacingMode.FRONT else LensFacingMode.BACK
                    parameterStore.setLensFacing(next)
                    hapticManager.tick()
                },
                onOpenGallery = {
                    hapticManager.tick()
                    val targetUri = latestMedia?.uri ?: recordingStatus.savedUri
                    val isVideo = latestMedia?.isVideo ?: (recordingStatus.lastMediaType == CapturedMediaType.VIDEO)
                    GalleryLauncher.openGallery(context, targetUri, isVideo)
                },
                lensFacing = settings.lensFacing,
                latestMedia = latestMedia
            )
        }

        // 4. Overlays: Q-Menu Drawer
        QuickSettingsDrawer(
            isOpen = isQMenuOpen,
            settings = settings,
            capabilities = capabilities,
            onClose = { isQMenuOpen = false },
            onSetAspect = { parameterStore.setAspectRatio(it) },
            onSetQuality = { parameterStore.setQuality(it) },
            onSetStillResolution = { parameterStore.setStillResolution(it) },
            onSetFps = { parameterStore.setFpsPreset(it) },
            onSetVideoBitrate = { parameterStore.setVideoBitrateMbps(it) },
            onSetVideoCodec = { parameterStore.setVideoCodec(it) },
            onSetPhotoFormat = { parameterStore.setPhotoFormat(it) },
            onSetVideoStabilization = { parameterStore.setVideoStabilization(it) },
            onToggleScreenDimmer = { parameterStore.setScreenDimmerEnabled(it) },
            onSetDynamicRange = { parameterStore.setDynamicRange(it) },
            onToggleDcg = { parameterStore.setDcgHdr(it) },
            onToggleUltraHdr = { parameterStore.setUltraHdr(it) },
            onSetMotionDuration = { parameterStore.setMotionDuration(it) },
            onSetVolumeUp = { parameterStore.setVolumeKeyUpAction(it) },
            onSetVolumeDown = { parameterStore.setVolumeKeyDownAction(it) },
            onSetColorSpace = { parameterStore.setColorSpace(it) },
            onToggleLut = { parameterStore.setLutEnabled(it) },
            onSetLutPreset = { parameterStore.setLutPreset(it) },
            onSelectCustomLut = { parameterStore.selectCustomLut(it) },
            onImportLutClick = { lutPickerLauncher.launch(arrayOf("*/*")) },
            onDeleteCustomLut = { lutToDelete = it },
            onToggleLutBurnIn = { parameterStore.setLutBurnIn(it) },
            onToggleFocusPeaking = { parameterStore.setFocusPeaking(it) },
            onToggleZebraPattern = { parameterStore.setZebraPattern(it) },
            onSetHistogramMode = { parameterStore.setHistogramMode(it) },
            onSetQMenuSlots = { parameterStore.setQMenuSlots(it) },
            hapticManager = hapticManager,
            modifier = Modifier.fillMaxSize()
        )

        // 5. Overlays: ABGM 2D White Balance Dialog
        if (isAbgmDialogOpen) {
            AbgmTintDialog(
                isAuto = settings.isAutoWb,
                cctKelvin = settings.cctKelvin,
                tintGm = settings.tintGm,
                onSetAuto = { parameterStore.setAutoWb(true) },
                onSetWb = { k, t ->
                    parameterStore.setAutoWb(false)
                    parameterStore.setCctKelvin(k)
                    parameterStore.setTintGm(t)
                },
                onDismiss = { isAbgmDialogOpen = false },
                hapticManager = hapticManager
            )
        }

        // 6. Overlays: Custom LUT Delete Confirmation Dialog
        if (lutToDelete != null) {
            val targetLut = lutToDelete!!
            AlertDialog(
                onDismissRequest = { lutToDelete = null },
                title = {
                    Text(
                        text = "移除胶卷预设",
                        color = OneShotTheme.FilmSilver,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OneShotTheme.MonospaceFont
                    )
                },
                text = {
                    Text(
                        text = "确定从机身胶卷仓中移除「${targetLut.name}」(${targetLut.size}³) 吗？\n文件将从应用存储中删除。",
                        color = OneShotTheme.TitaniumGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            cameraController.customLutManager.deleteLut(targetLut.id, targetLut.fileName)
                        }
                        parameterStore.removeCustomLut(targetLut.id)
                        hapticManager.tick()
                        lutToDelete = null
                        Toast.makeText(context, "已移除胶卷「${targetLut.name}」", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("确认移除", color = OneShotTheme.DarkroomRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { lutToDelete = null }) {
                        Text("取消", color = OneShotTheme.TitaniumGray)
                    }
                },
                containerColor = OneShotTheme.ObsidianBlack,
            )
        }
    }
}

/**
 * Optical Ground Glass Framing Reticle (BRAND.md 4:3 Open Gate & Cinema Aesthetics)
 * Features:
 * - 4 Precision ground glass corner brackets
 * - Center split-image rangefinder micro-ring & fine crosshair
 * - Rule of thirds subtle optical tick marks
 */
@Composable
private fun OpticalFramingReticle(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val margin = 14.dp.toPx()
        val cornerLen = 16.dp.toPx()
        val color = OneShotTheme.FilmSilver.copy(alpha = 0.38f)
        val stroke = 1.0.dp.toPx()

        // 1. Four Ground Glass Corner Brackets
        // Top-Left corner ⸢
        drawLine(color, Offset(margin, margin), Offset(margin + cornerLen, margin), strokeWidth = stroke)
        drawLine(color, Offset(margin, margin), Offset(margin, margin + cornerLen), strokeWidth = stroke)

        // Top-Right corner ⸣
        drawLine(color, Offset(w - margin, margin), Offset(w - margin - cornerLen, margin), strokeWidth = stroke)
        drawLine(color, Offset(w - margin, margin), Offset(w - margin, margin + cornerLen), strokeWidth = stroke)

        // Bottom-Left corner ⸤
        drawLine(color, Offset(margin, h - margin), Offset(margin + cornerLen, h - margin), strokeWidth = stroke)
        drawLine(color, Offset(margin, h - margin), Offset(margin, h - margin - cornerLen), strokeWidth = stroke)

        // Bottom-Right corner ⸥
        drawLine(color, Offset(w - margin, h - margin), Offset(w - margin - cornerLen, h - margin), strokeWidth = stroke)
        drawLine(color, Offset(w - margin, h - margin), Offset(w - margin, h - margin - cornerLen), strokeWidth = stroke)

        // 2. Rule of Thirds Micro-Ticks (Ultra-fine 0.75px tick marks)
        val thirdX1 = w / 3f
        val thirdX2 = w * 2f / 3f
        val thirdY1 = h / 3f
        val thirdY2 = h * 2f / 3f
        val tickSize = 4.dp.toPx()
        val tickColor = OneShotTheme.TitaniumGray.copy(alpha = 0.22f)

        // 4 intersection micro crosses
        listOf(
            Offset(thirdX1, thirdY1),
            Offset(thirdX2, thirdY1),
            Offset(thirdX1, thirdY2),
            Offset(thirdX2, thirdY2)
        ).forEach { pt ->
            drawLine(tickColor, Offset(pt.x - tickSize, pt.y), Offset(pt.x + tickSize, pt.y), strokeWidth = 0.75f)
            drawLine(tickColor, Offset(pt.x, pt.y - tickSize), Offset(pt.x, pt.y + tickSize), strokeWidth = 0.75f)
        }

        // 3. Center Split-Image Rangefinder Micro-Ring & Crosshair
        val cx = w / 2f
        val cy = h / 2f
        val ringR = 14.dp.toPx()
        val centerCrossLen = 6.dp.toPx()

        // Center micro ring (Leica M / Hasselblad ground glass cue)
        drawCircle(
            color = OneShotTheme.TitaniumGray.copy(alpha = 0.28f),
            radius = ringR,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.85f)
        )

        // Crosshairs outside the ring
        drawLine(tickColor, Offset(cx - ringR - centerCrossLen, cy), Offset(cx - ringR, cy), strokeWidth = 0.85f)
        drawLine(tickColor, Offset(cx + ringR, cy), Offset(cx + ringR + centerCrossLen, cy), strokeWidth = 0.85f)
        drawLine(tickColor, Offset(cx, cy - ringR - centerCrossLen), Offset(cx, cy - ringR), strokeWidth = 0.85f)
        drawLine(tickColor, Offset(cx, cy + ringR), Offset(cx, cy + ringR + centerCrossLen), strokeWidth = 0.85f)

        // Center micro dot
        drawCircle(
            color = OneShotTheme.AmberGrain.copy(alpha = 0.6f),
            radius = 1.5.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}
