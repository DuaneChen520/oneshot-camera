package dev.oneshot.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dev.oneshot.camera.gl.CustomLutManager
import dev.oneshot.camera.gl.OneshotCameraEffect
import dev.oneshot.camera.gl.OneshotGlProcessor
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.LensFacingMode
import dev.oneshot.data.ParameterStore
import dev.oneshot.data.QualityPreset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class CameraController(
    private val context: Context,
    private val parameterStore: ParameterStore,
    private val hapticManager: HapticManager,
    val customLutManager: CustomLutManager = CustomLutManager(context),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null
    private var activeLifecycleOwner: LifecycleOwner? = null
    private var activeSurfaceProvider: Preview.SurfaceProvider? = null

    private var activeImageCapture: ImageCapture? = null
    private var activeVideoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var glProcessor: OneshotGlProcessor? = null
    private var activeCameraEffect: OneshotCameraEffect? = null

    // State flows for UI
    private val _zoomRange = MutableStateFlow(0.6f..10.0f)
    val zoomRange: StateFlow<ClosedFloatingPointRange<Float>> = _zoomRange.asStateFlow()

    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoom: StateFlow<Float> = _currentZoom.asStateFlow()

    private val _exposureRange = MutableStateFlow(-4..4)
    val exposureRange: StateFlow<IntRange> = _exposureRange.asStateFlow()

    private val _exposureStep = MutableStateFlow(0.5f)
    val exposureStep: StateFlow<Float> = _exposureStep.asStateFlow()

    private val _currentEvIndex = MutableStateFlow(0)
    val currentEvIndex: StateFlow<Int> = _currentEvIndex.asStateFlow()

    private val _focusPoint = MutableStateFlow<PointF?>(null)
    val focusPoint: StateFlow<PointF?> = _focusPoint.asStateFlow()

    private val _recordingStatus = MutableStateFlow(RecordingStatus())
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

    private val capabilityDegrader = DeviceCapabilityDegrader(context)
    private val _capabilities = MutableStateFlow(capabilityDegrader.detectCapabilities())
    val capabilities: StateFlow<DeviceCapabilities> = _capabilities.asStateFlow()

    val galleryThumbnailManager = GalleryThumbnailManager(context)
    val latestMedia: StateFlow<MediaThumbnail?> = galleryThumbnailManager.latestMedia

    private val _emptyHistogram = MutableStateFlow<dev.oneshot.camera.gl.HistogramData?>(null)
    val histogramData: StateFlow<dev.oneshot.camera.gl.HistogramData?>
        get() = glProcessor?.histogramData ?: _emptyHistogram

    init {
        scope.launch {
            parameterStore.settings.collectLatest { settings ->
                if (activeLifecycleOwner != null && activeSurfaceProvider != null) {
                    rebindCamera(activeLifecycleOwner!!, activeSurfaceProvider!!, settings)
                }
            }
        }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        activeLifecycleOwner = lifecycleOwner
        activeSurfaceProvider = surfaceProvider
        val currentSettings = parameterStore.settings.value

        if (cameraProvider != null) {
            rebindCamera(lifecycleOwner, surfaceProvider, currentSettings)
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                cameraProvider = future.get()
                rebindCamera(lifecycleOwner, surfaceProvider, currentSettings)
            }, mainExecutor)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun rebindCamera(
        owner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        settings: CameraSettings,
    ) {
        val provider = cameraProvider ?: return
        if (_recordingStatus.value.isRecording || _recordingStatus.value.isMotionCapturing) {
            // Avoid disrupting active captures
            applyInFlightParameters(settings)
            return
        }

        provider.unbindAll()

        val detectedCaps = capabilityDegrader.detectCapabilities(settings.lensFacing)
        _capabilities.value = detectedCaps
        val effectiveSettings = capabilityDegrader.sanitizeSettings(settings, detectedCaps)

        val selector = when (effectiveSettings.lensFacing) {
            LensFacingMode.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            LensFacingMode.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        // 1. Build Preview
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(surfaceProvider)
        }

        // 2. Build ImageCapture (M2 High-spec Stills)
        val imageCaptureBuilder = ImageCapture.Builder()
            .setTargetAspectRatio(effectiveSettings.aspectMode.cameraXRatio)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && effectiveSettings.ultraHdrEnabled) {
            runCatching {
                imageCaptureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
            }
        }
        val imageCapture = imageCaptureBuilder.build()
        activeImageCapture = imageCapture

        // 3. Build Recorder and VideoCapture
        val recorderBuilder = Recorder.Builder()
            .setAspectRatio(effectiveSettings.aspectMode.cameraXRatio)
            .setExecutor(mainExecutor)

        if (effectiveSettings.quality != QualityPreset.OPENGATE_MAX && effectiveSettings.quality.cameraXQuality != null) {
            recorderBuilder.setQualitySelector(
                QualitySelector.from(
                    effectiveSettings.quality.cameraXQuality,
                    FallbackStrategy.lowerQualityOrHigherThan(effectiveSettings.quality.cameraXQuality)
                )
            )
        }

        val dynamicRange = when (effectiveSettings.dynamicRange) {
            DynamicRangeMode.HLG10 -> DynamicRange.HLG_10_BIT
            DynamicRangeMode.SDR -> DynamicRange.SDR
        }

        val videoCaptureBuilder = VideoCapture.Builder(recorderBuilder.build())
            .setDynamicRange(dynamicRange)

        if (effectiveSettings.quality == QualityPreset.OPENGATE_MAX) {
            runCatching {
                videoCaptureBuilder.setCustomOrderedResolutions(listOf(Size(4096, 3072), Size(2880, 2160)))
            }
        }

        val videoCapture = videoCaptureBuilder.build()
        activeVideoCapture = videoCapture

        // 4. Session Parameters (DCG HDR vendor tag 0x8003)
        val useCases: MutableList<UseCase> = mutableListOf(preview, imageCapture, videoCapture)
        val sessionConfigBuilder = SessionConfig.Builder(useCases)

        if (effectiveSettings.dcgHdrEnabled && effectiveSettings.lensFacing == LensFacingMode.BACK) {
            sessionConfigBuilder.setInterop(Camera2Interop.forSessionConfig { interop ->
                runCatching {
                    interop.setSessionParameter(
                        CaptureRequest.Key("org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode", Int::class.java),
                        1
                    )
                }
            })
        }

        // Bind use cases to lifecycle with graceful fallback
        val hasEffect = effectiveSettings.lutEnabled ||
                        effectiveSettings.focusPeakingEnabled ||
                        effectiveSettings.zebraPatternEnabled ||
                        effectiveSettings.histogramMode != dev.oneshot.data.HistogramMode.OFF
        if (hasEffect) {
            val processor = glProcessor ?: OneshotGlProcessor(customLutManager).also { glProcessor = it }
            processor.updateSettings(effectiveSettings)

            val targets = if (effectiveSettings.lutBurnIn) {
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE
            } else {
                CameraEffect.PREVIEW
            }

            val effect = OneshotCameraEffect(targets, mainExecutor, processor)
            activeCameraEffect = effect

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .addUseCase(videoCapture)
                .addEffect(effect)
                .build()

            runCatching {
                val camera = provider.bindToLifecycle(owner, selector, useCaseGroup)
                boundCamera = camera
                updateCameraCapabilities(camera)
                applyInFlightParameters(settings)
            }.onFailure {
                val fallbackGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(videoCapture)
                    .addEffect(effect)
                    .build()
                runCatching {
                    val camera = provider.bindToLifecycle(owner, selector, fallbackGroup)
                    boundCamera = camera
                    updateCameraCapabilities(camera)
                    applyInFlightParameters(settings)
                }
            }
        } else {
            runCatching {
                val camera = provider.bindToLifecycle(owner, selector, preview, imageCapture, videoCapture)
                boundCamera = camera
                updateCameraCapabilities(camera)
                applyInFlightParameters(settings)
            }.onFailure {
                // Fallback: bind preview and videoCapture only if 3 concurrent use cases fail
                runCatching {
                    val camera = provider.bindToLifecycle(owner, selector, preview, videoCapture)
                    boundCamera = camera
                    updateCameraCapabilities(camera)
                    applyInFlightParameters(settings)
                }
            }
        }
    }

    private fun updateCameraCapabilities(camera: Camera) {
        val info = camera.cameraInfo
        val zoomState = info.zoomState.value
        if (zoomState != null) {
            _zoomRange.value = zoomState.minZoomRatio..zoomState.maxZoomRatio
            _currentZoom.value = zoomState.zoomRatio
        }

        val expState = info.exposureState
        _exposureRange.value = expState.exposureCompensationRange.lower..expState.exposureCompensationRange.upper
        _exposureStep.value = expState.exposureCompensationStep.toFloat()
        _currentEvIndex.value = expState.exposureCompensationIndex
    }

    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(_zoomRange.value.start, _zoomRange.value.endInclusive)
        _currentZoom.value = clamped
        parameterStore.setZoomRatio(clamped)
        boundCamera?.cameraControl?.setZoomRatio(clamped)
        hapticManager.tick()
    }

    fun setEv(evIndex: Int) {
        val clamped = evIndex.coerceIn(_exposureRange.value.first, _exposureRange.value.last)
        _currentEvIndex.value = clamped
        parameterStore.setEvIndex(clamped)
        boundCamera?.cameraControl?.setExposureCompensationIndex(clamped)
        hapticManager.tick()
    }

    fun setFocusAndMetering(factory: MeteringPointFactory, x: Float, y: Float) {
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        _focusPoint.value = PointF(x, y)
        boundCamera?.cameraControl?.startFocusAndMetering(action)
        hapticManager.tick()
        scope.launch {
            delay(3000)
            if (_focusPoint.value?.x == x && _focusPoint.value?.y == y) {
                _focusPoint.value = null
            }
        }
    }

    fun applyInFlightParameters(settings: CameraSettings) {
        val control = boundCamera?.cameraControl ?: return
        val info = boundCamera?.cameraInfo ?: return

        // 1. Zoom
        control.setZoomRatio(settings.zoomRatio.coerceIn(
            info.zoomState.value?.minZoomRatio ?: 0.6f,
            info.zoomState.value?.maxZoomRatio ?: 10.0f
        ))

        // 2. EV
        control.setExposureCompensationIndex(settings.evIndex.coerceIn(
            info.exposureState.exposureCompensationRange.lower,
            info.exposureState.exposureCompensationRange.upper
        ))

        val optionsBuilder = CaptureRequestOptions.Builder()

        // 3. Target Frame Rate
        val targetFps = settings.fpsPreset.targetFps
        optionsBuilder.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            Range(targetFps, targetFps)
        )

        // 4. White Balance & CCT
        if (settings.isAutoWb) {
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
        } else {
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF
            )
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CaptureRequest.COLOR_CORRECTION_MODE_FAST
            )
            if (Build.VERSION.SDK_INT >= 36) {
                runCatching {
                    optionsBuilder.setCaptureRequestOption(
                        CaptureRequest.Key("android.colorCorrection.colorTemperature", Int::class.java),
                        settings.cctKelvin
                    )
                }
            }
        }

        // 5. Manual Exposure Time (Shutter Speed) & Manual ISO
        if (!settings.isAutoShutter || !settings.isAutoIso) {
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )
            if (!settings.isAutoShutter) {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    settings.shutterSpeedNanos
                )
            }
            if (!settings.isAutoIso) {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY,
                    settings.isoValue
                )
            }
        } else {
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )
        }

        Camera2CameraControl.from(control).setCaptureRequestOptions(optionsBuilder.build())
        glProcessor?.updateSettings(settings)
    }

    // =========================================================================
    // M2: STILL PHOTOGRAPHY PIPELINE
    // =========================================================================

    fun takeStillPhoto() {
        val imageCapture = activeImageCapture ?: return
        if (_recordingStatus.value.isStillCapturing || _recordingStatus.value.isMotionCapturing) return

        hapticManager.shutterClick()
        _recordingStatus.value = _recordingStatus.value.copy(
            isStillCapturing = true,
            shutterAnimationTick = SystemClock.uptimeMillis()
        )

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "IMG_${stamp}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/OneShot")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    outputFileResults.savedUri?.let { uri ->
                        galleryThumbnailManager.updateWithCapturedUri(uri, false, displayName)
                    }
                    _recordingStatus.value = _recordingStatus.value.copy(
                        isStillCapturing = false,
                        savedUri = outputFileResults.savedUri,
                        lastMediaType = CapturedMediaType.PHOTO,
                        userNotice = "照片已保存至相册 (DCIM/OneShot)",
                        lastError = null,
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    _recordingStatus.value = _recordingStatus.value.copy(
                        isStillCapturing = false,
                        lastError = "拍照失败: ${exception.message}",
                    )
                }
            }
        )
    }

    // =========================================================================
    // M2: MOTION PHOTO PIPELINE
    // =========================================================================

    fun captureMotionPhoto() {
        val imageCapture = activeImageCapture ?: return
        val videoCapture = activeVideoCapture ?: return
        if (_recordingStatus.value.isStillCapturing || _recordingStatus.value.isMotionCapturing || _recordingStatus.value.isRecording) return

        hapticManager.shutterClick()
        _recordingStatus.value = _recordingStatus.value.copy(
            isMotionCapturing = true,
            shutterAnimationTick = SystemClock.uptimeMillis()
        )

        val durationSeconds = parameterStore.settings.value.motionDuration.seconds
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tempJpeg = File(context.cacheDir, "motion_still_${stamp}.jpg")
        val tempMp4 = File(context.cacheDir, "motion_vid_${stamp}.mp4")

        scope.launch {
            val jpegDeferred = CompletableDeferred<Boolean>()
            val videoDeferred = CompletableDeferred<Boolean>()

            // 1. Capture still photo to temp file
            val jpegOptions = ImageCapture.OutputFileOptions.Builder(tempJpeg).build()
            imageCapture.takePicture(
                jpegOptions,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        jpegDeferred.complete(true)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        jpegDeferred.complete(false)
                    }
                }
            )

            // 2. Capture micro video snippet to temp file
            val videoOptions = FileOutputOptions.Builder(tempMp4).build()
            val pending = videoCapture.output.prepareRecording(context, videoOptions)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pending.withAudioEnabled()
            }

            var motionRec: Recording? = null
            motionRec = pending.start(mainExecutor) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    videoDeferred.complete(!event.hasError())
                }
            }

            // Record for requested duration, then stop
            delay((durationSeconds * 1000).toLong())
            motionRec.stop()

            // Await both completion
            val jpegSuccess = jpegDeferred.await()
            val videoSuccess = videoDeferred.await()

            if (jpegSuccess && videoSuccess) {
                val motionUri = withContext(Dispatchers.IO) {
                    MotionPhotoWriter.createMotionPhoto(
                        context = context,
                        jpegFile = tempJpeg,
                        mp4File = tempMp4,
                        displayName = "IMG_${stamp}_MOTION.jpg"
                    )
                }

                // Clean up temp files
                tempJpeg.delete()
                tempMp4.delete()

                if (motionUri != null) {
                    hapticManager.motionPhotoCaptured()
                    galleryThumbnailManager.updateWithCapturedUri(motionUri, false, "IMG_${stamp}_MOTION.jpg")
                    _recordingStatus.value = _recordingStatus.value.copy(
                        isMotionCapturing = false,
                        savedUri = motionUri,
                        lastMediaType = CapturedMediaType.MOTION_PHOTO,
                        userNotice = "动态照片已保存至相册 (DCIM/OneShot)",
                        lastError = null,
                    )
                } else {
                    _recordingStatus.value = _recordingStatus.value.copy(
                        isMotionCapturing = false,
                        lastError = "动态照片生成失败",
                    )
                }
            } else {
                tempJpeg.delete()
                tempMp4.delete()
                _recordingStatus.value = _recordingStatus.value.copy(
                    isMotionCapturing = false,
                    lastError = "动态照片捕获异常",
                )
            }
        }
    }

    // =========================================================================
    // M1/M2: VIDEO RECORDING PIPELINE WITH TWO-STAGE HAPTICS
    // =========================================================================

    fun startRecording() {
        val videoCapture = activeVideoCapture ?: return
        if (_recordingStatus.value.isRecording || _recordingStatus.value.isMotionCapturing) return

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val aspectTag = if (parameterStore.settings.value.aspectMode == AspectRatioMode.RATIO_4_3) "4x3" else "16x9"
        val displayName = "VID_${stamp}_${aspectTag}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/OneShot")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        // Two-stage haptic: Stage 1 button press
        hapticManager.recordButtonPress()
        RecordingForegroundService.start(context)

        val pendingRecording = videoCapture.output.prepareRecording(context, outputOptions)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pendingRecording.withAudioEnabled()
        }

        activeRecording = pendingRecording
            .start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // Two-stage haptic: Stage 2 HAL confirmation vibration
                        hapticManager.recordingStartConfirm()
                        _recordingStatus.value = _recordingStatus.value.copy(
                            isRecording = true,
                            isPaused = false,
                            durationSeconds = 0,
                            recordedBytes = 0,
                        )
                    }
                    is VideoRecordEvent.Status -> {
                        val duration = TimeUnit.NANOSECONDS.toSeconds(event.recordingStats.recordedDurationNanos)
                        val bytes = event.recordingStats.numBytesRecorded
                        _recordingStatus.value = _recordingStatus.value.copy(
                            durationSeconds = duration,
                            recordedBytes = bytes,
                        )
                    }
                    is VideoRecordEvent.Finalize -> {
                        RecordingForegroundService.stop(context)
                        val uri = event.outputResults.outputUri
                        val error = if (event.hasError()) "录制异常 (code=${event.error})" else null
                        if (!event.hasError() && uri != Uri.EMPTY) {
                            galleryThumbnailManager.updateWithCapturedUri(uri, true)
                        }
                        _recordingStatus.value = _recordingStatus.value.copy(
                            isRecording = false,
                            isPaused = false,
                            savedUri = if (!event.hasError()) uri else null,
                            lastMediaType = CapturedMediaType.VIDEO,
                            lastError = error,
                            userNotice = if (!event.hasError()) "视频已保存至相册 (DCIM/OneShot)" else null,
                        )
                    }
                    is VideoRecordEvent.Pause -> {
                        _recordingStatus.value = _recordingStatus.value.copy(isPaused = true)
                    }
                    is VideoRecordEvent.Resume -> {
                        _recordingStatus.value = _recordingStatus.value.copy(isPaused = false)
                    }
                }
            }
    }

    fun pauseRecording() {
        activeRecording?.pause()
    }

    fun resumeRecording() {
        activeRecording?.resume()
    }

    fun stopRecording() {
        hapticManager.recordingStop()
        RecordingForegroundService.stop(context)
        activeRecording?.stop()
        activeRecording = null
    }

    fun clearUserNotice() {
        _recordingStatus.value = _recordingStatus.value.copy(userNotice = null)
    }

    fun unbind() {
        stopRecording()
        RecordingForegroundService.stop(context)
        glProcessor?.release()
        glProcessor = null
        activeCameraEffect = null
        cameraProvider?.unbindAll()
        boundCamera = null
        activeLifecycleOwner = null
        activeSurfaceProvider = null
    }
}
