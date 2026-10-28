package dev.oneshot.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import dev.oneshot.data.MotionDurationPreset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance Rolling Segment Pre-Roll Buffer Engine for Motion Photos (Live Photos).
 *
 * Implements Google & Apple style pre-roll capture:
 * - When [motionPhotoLiveEnabled] is true, continuously maintains a background rolling MP4 snippet
 *   in cache memory without burdening the persistent storage.
 * - When the shutter fires at T0, captures the pristine still frame while video continues recording
 *   for the configured post-roll duration.
 * - Uses [VideoTrimmer] for zero-transcode container remuxing at sync-frame GOP boundaries (~30ms),
 *   extracting [T0 - preRoll, T0 + postRoll] with presentation timestamp centered at T0.
 */
class MotionPhotoPreRollEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStillSavedForThumbnail: (Uri, String) -> Unit,
    private val onMotionPhotoComplete: (Uri?, String) -> Unit,
    private val onError: (String) -> Unit,
) {

    private enum class State { IDLE, ROLLING, ROTATING, CAPTURING, FINALIZING }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor = Dispatchers.IO.asExecutor()

    private var state = State.IDLE
    private var isEnabled = false
    private var activeVideoCapture: VideoCapture<Recorder>? = null
    private var activeImageCapture: ImageCapture? = null

    private var activeRecording: Recording? = null
    private var currentFile: File? = null
    private var currentDurationMs = 0L
    private var fileToggleIndex = 0

    // Capture in flight state
    private var pivotMs = 0L
    private var targetEndMs = 0L
    private var capturedStillFile: File? = null
    private var stillSavedDeferred: CompletableDeferred<Boolean>? = null
    private var pendingStamp: String = ""

    companion object {
        private const val ROLL_SEGMENT_DURATION_MS = 6000L // 6 seconds segment rotation
        private const val ROLL_FILE_PREFIX = "oneshot_live_roll_"
    }

    init {
        cleanupOldSegments()
    }

    private fun cleanupOldSegments() {
        runCatching {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(ROLL_FILE_PREFIX) || file.name.startsWith("oneshot_trim_")) {
                    file.delete()
                }
            }
        }
    }

    val isRolling: Boolean
        get() = state == State.ROLLING || state == State.ROTATING

    val isBusyCapturing: Boolean
        get() = state == State.CAPTURING || state == State.FINALIZING

    fun updateUseCases(videoCapture: VideoCapture<Recorder>?, imageCapture: ImageCapture?) {
        activeVideoCapture = videoCapture
        activeImageCapture = imageCapture
        if (isEnabled && !isBusyCapturing && videoCapture != null) {
            if (state == State.IDLE) {
                startRollingSegment()
            }
        }
    }

    fun setLiveEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        isEnabled = enabled
        if (enabled) {
            if (state == State.IDLE && activeVideoCapture != null) {
                startRollingSegment()
            }
        } else {
            if (!isBusyCapturing) {
                stopRollingInternal()
            }
        }
    }

    fun stopRolling() {
        isEnabled = false
        stopRollingInternal()
    }

    private fun stopRollingInternal() {
        mainHandler.removeCallbacksAndMessages(null)
        val rec = activeRecording
        activeRecording = null
        state = State.IDLE
        runCatching { rec?.stop() }
        currentFile?.delete()
        currentFile = null
        currentDurationMs = 0L
    }

    @SuppressLint("MissingPermission")
    private fun startRollingSegment() {
        val vc = activeVideoCapture ?: return
        if (state == State.CAPTURING || state == State.FINALIZING || !isEnabled) return

        val file = File(context.cacheDir, "${ROLL_FILE_PREFIX}${fileToggleIndex}.mp4")
        fileToggleIndex = (fileToggleIndex + 1) % 2
        file.delete()

        currentFile = file
        currentDurationMs = 0L

        try {
            val fileOptions = FileOutputOptions.Builder(file).build()
            val pending = vc.output.prepareRecording(context, fileOptions)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pending.withAudioEnabled()
            }

            state = State.ROLLING
            activeRecording = pending.start(ioExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Status -> {
                        val duration = event.recordingStats.recordedDurationNanos / 1_000_000L
                        currentDurationMs = duration

                        if (state == State.ROLLING && duration >= ROLL_SEGMENT_DURATION_MS) {
                            // Rotate segment smoothly
                            state = State.ROTATING
                            mainHandler.post {
                                activeRecording?.stop()
                            }
                        } else if (state == State.CAPTURING && duration >= targetEndMs) {
                            state = State.FINALIZING
                            mainHandler.post {
                                activeRecording?.stop()
                            }
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        handleRecordFinalize(file, !event.hasError() && file.exists() && file.length() > 0)
                    }
                }
            }
        } catch (e: Exception) {
            state = State.IDLE
            activeRecording = null
            file.delete()
        }
    }

    private fun handleRecordFinalize(recordedFile: File, success: Boolean) {
        val prevState = state
        activeRecording = null

        if (prevState == State.ROTATING) {
            recordedFile.delete()
            state = State.IDLE
            if (isEnabled) {
                mainHandler.postDelayed({ startRollingSegment() }, 100)
            }
        } else if (prevState == State.FINALIZING || prevState == State.CAPTURING) {
            state = State.IDLE
            val stillFile = capturedStillFile
            val deferred = stillSavedDeferred
            val pivot = pivotMs
            val stamp = pendingStamp

            scope.launch {
                val stillOk = deferred?.await() ?: false
                if (stillOk && success && stillFile != null && stillFile.exists() && recordedFile.exists()) {
                    processAndPublishMotionPhoto(recordedFile, stillFile, pivot, stamp)
                } else {
                    recordedFile.delete()
                    stillFile?.delete()
                    withContext(Dispatchers.Main) {
                        onError("动态照片预摄生成失败")
                    }
                }
                // Restart rolling if still enabled
                if (isEnabled) {
                    mainHandler.postDelayed({ startRollingSegment() }, 200)
                }
            }
        } else {
            // Unexpected stop or cancelled
            recordedFile.delete()
            state = State.IDLE
            if (isEnabled) {
                mainHandler.postDelayed({ startRollingSegment() }, 300)
            }
        }
    }

    /**
     * Intercepts shutter tap to capture pre-roll + post-roll snippet.
     * Returns true if pre-roll capture was engaged, false if engine was not rolling.
     */
    fun captureLivePhoto(
        durationPreset: MotionDurationPreset,
        stamp: String,
        tempStillFile: File,
    ): Boolean {
        val ic = activeImageCapture ?: return false
        if (state != State.ROLLING || activeRecording == null) return false

        state = State.CAPTURING
        pivotMs = currentDurationMs
        val postRollMs = (durationPreset.postRollSeconds * 1000).toLong()
        targetEndMs = pivotMs + postRollMs
        capturedStillFile = tempStillFile
        pendingStamp = stamp

        val stillDeferred = CompletableDeferred<Boolean>()
        stillSavedDeferred = stillDeferred

        // 1. Concurrently capture pristine high-resolution still image
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempStillFile).build()
        ic.takePicture(
            outputOptions,
            ioExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    stillDeferred.complete(true)
                }

                override fun onError(exception: ImageCaptureException) {
                    stillDeferred.complete(false)
                }
            }
        )

        // Safety fallback: if video doesn't finalize within target + 4s, force stop
        mainHandler.postDelayed({
            if (state == State.CAPTURING) {
                state = State.FINALIZING
                runCatching { activeRecording?.stop() }
            }
        }, postRollMs + 4000L)

        return true
    }

    private suspend fun processAndPublishMotionPhoto(
        sourceVideo: File,
        stillJpeg: File,
        pivot: Long,
        stamp: String,
    ) = withContext(Dispatchers.IO) {
        val trimmedMp4 = File(context.cacheDir, "oneshot_trim_${System.currentTimeMillis()}.mp4")
        try {
            // Pre-roll window: up to 1.5s prior to pivot
            val preRollUs = 1_500_000L
            val targetStartUs = maxOf(0L, (pivot * 1000L) - preRollUs)
            val totalSpanUs = 3_000_000L // 3.0s standard Motion Photo duration

            val baseUs = VideoTrimmer.trimMp4(
                srcFile = sourceVideo,
                dstFile = trimmedMp4,
                startUs = targetStartUs,
                durationUs = totalSpanUs,
            )

            val presentationTimestampUs = if (baseUs >= 0) {
                maxOf(0L, (pivot * 1000L) - baseUs)
            } else {
                0L
            }

            val finalVideo = if (baseUs >= 0 && trimmedMp4.exists() && trimmedMp4.length() > 0) {
                trimmedMp4
            } else {
                sourceVideo
            }

            val displayName = "IMG_${stamp}_MOTION.jpg"
            val uri = MotionPhotoWriter.createMotionPhoto(
                context = context,
                jpegFile = stillJpeg,
                mp4File = finalVideo,
                displayName = displayName,
                presentationTimestampUs = presentationTimestampUs,
            )

            withContext(Dispatchers.Main) {
                if (uri != null) {
                    onStillSavedForThumbnail(uri, displayName)
                    onMotionPhotoComplete(uri, "带 1.5s 预摄的动态照片已保存至相册 (DCIM/OneShot)")
                } else {
                    onError("动态照片封装写盘失败")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError("处理动态照片异常: ${e.message}")
            }
        } finally {
            sourceVideo.delete()
            stillJpeg.delete()
            trimmedMp4.delete()
        }
    }
}
