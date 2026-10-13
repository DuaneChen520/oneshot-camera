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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.oneshot.camera.HapticManager
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.LensFacingMode
import dev.oneshot.data.ParameterStore
import kotlinx.coroutines.delay

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
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "OneShot 专业相机需要相机与麦克风权限",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    onClick = { permissionLauncher.launch(permissionsToRequest) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                ) {
                    Text("授予相机与录音权限", color = Color.Black, fontWeight = FontWeight.Bold)
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

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isQMenuOpen by remember { mutableStateOf(false) }
    var isAbgmDialogOpen by remember { mutableStateOf(false) }
    var isShutterFlashVisible by remember { mutableStateOf(false) }

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
                            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                            cameraController.bindCamera(lifecycleOwner, surfaceProvider)
                        }
                    },
                    update = { view ->
                        previewViewRef = view
                    }
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

                // OLED Screen Dimmer during Video Recording
                ScreenDimmerOverlay(
                    isRecording = recordingStatus.isRecording,
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
            // In-place parameter gestures HUD (ZOOM, EV, Shutter Speed, ISO, WB)
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
                onToggleAutoWb = { parameterStore.setAutoWb(it) },
                onCctChange = { parameterStore.setCctKelvin(it) },
                onOpenWbDialog = { isAbgmDialogOpen = true },
                hapticManager = hapticManager
            )

            // Three-Shutter Cluster Bar
            ShutterCluster(
                status = recordingStatus,
                onTakeStillPhoto = { cameraController.takeStillPhoto() },
                onCaptureMotionPhoto = { cameraController.captureMotionPhoto() },
                onStartRecording = { cameraController.startRecording() },
                onStopRecording = { cameraController.stopRecording() },
                onFlipCamera = {
                    val next = if (settings.lensFacing == LensFacingMode.BACK) LensFacingMode.FRONT else LensFacingMode.BACK
                    parameterStore.setLensFacing(next)
                    hapticManager.tick()
                },
                onOpenGallery = {
                    runCatching {
                        val intent = if (recordingStatus.savedUri != null) {
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(recordingStatus.savedUri, "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        } else {
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                            }
                        }
                        context.startActivity(intent)
                    }
                }
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
            onSetFps = { parameterStore.setFpsPreset(it) },
            onSetDynamicRange = { parameterStore.setDynamicRange(it) },
            onToggleDcg = { parameterStore.setDcgHdr(it) },
            onToggleUltraHdr = { parameterStore.setUltraHdr(it) },
            onSetMotionDuration = { parameterStore.setMotionDuration(it) },
            onSetVolumeUp = { parameterStore.setVolumeKeyUpAction(it) },
            onSetVolumeDown = { parameterStore.setVolumeKeyDownAction(it) },
            onSetColorSpace = { parameterStore.setColorSpace(it) },
            onToggleLut = { parameterStore.setLutEnabled(it) },
            onSetLutPreset = { parameterStore.setLutPreset(it) },
            onToggleLutBurnIn = { parameterStore.setLutBurnIn(it) },
            onToggleFocusPeaking = { parameterStore.setFocusPeaking(it) },
            onToggleZebraPattern = { parameterStore.setZebraPattern(it) },
            hapticManager = hapticManager,
            modifier = Modifier.align(Alignment.TopCenter)
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
    }
}
