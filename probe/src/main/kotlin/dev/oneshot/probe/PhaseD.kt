package dev.oneshot.probe

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Phase D: the open-gate question, answered by actually recording.
 *
 * Phases A and C say a 4:3 stream exists and the HAL admits it. That is not the same thing as
 * the app getting one: CameraX picks the video surface size from its own Quality ladder, and
 * only the finished file proves which size was really written. Each attempt records ~1.5 s and
 * reads the container back, so the evidence is the encoder's input size, not our intention.
 *
 * Tiers, as pre-registered in docs/probe/DECISION-CRITERIA.md:
 *  - D1: public API only — [Recorder.Builder.setAspectRatio] with [AspectRatio.RATIO_4_3].
 *  - D2: D1 plus session parameters (the standard sensor pixel mode, then vendor tags).
 *  - D3: the `@RestrictTo` custom ordered resolutions escape hatch, for a 4:3 size that has no
 *    rung on the Quality ladder at all.
 *
 * The `startGapMs` each attempt measures is also the answer to whether the "recording really
 * began" vibration is worth having.
 */
class PhaseD(
    private val context: Context,
    private val cameraManager: CameraManager,
    private val harness: BindHarness,
    private val onProgress: (String) -> Unit,
    private val outDir: File,
    private val vendorTagBudget: Int,
) {

    fun runFor(provider: ProcessCameraProvider, cameraId: String): JsonObject {
        val chars = runCatching { cameraManager.getCameraCharacteristics(cameraId) }.getOrNull()
            ?: return header(cameraId, "characteristics unavailable")
        val selector = runCatching { Camera2Interop.getCameraSelectorFromCameraId(cameraId) }.getOrNull()
        val info: CameraInfo? = selector?.let { runCatching { provider.getCameraInfo(it) }.getOrNull() }
        if (selector == null || info == null) return header(cameraId, "no selector or camera info")

        val attempts = tierD1() + tierD2(chars) + tierD3(chars)

        return buildJsonObject {
            put("cameraId", cameraId)
            put("openGateCandidates", jsonOf(
                Candidates.forCamera(chars)
                    .filter { it.label.startsWith("OPENGATE") }
                    .map { "${it.recordSize.width}x${it.recordSize.height} ${it.reason}" }
            ))
            put("attemptCount", attempts.size)
            put("attempts", JsonArray(attempts.map { attempt ->
                onProgress("  Phase D: ${attempt.label}")
                recordOnce(provider, selector, info, attempt)
            }))
        }
    }

    private fun tierD1(): List<Attempt> = listOf(
        Attempt("D1-4x3-FHD-SDR", TIER_D1, "public setAspectRatio(4:3) at FHD, no HDR",
            aspect = AspectRatio.RATIO_4_3, quality = Quality.FHD, dynamicRange = DynamicRange.SDR),
        Attempt("D1-4x3-FHD-HLG10", TIER_D1, "PM daily recipe: 4:3 + HLG10, all public API",
            aspect = AspectRatio.RATIO_4_3, quality = Quality.FHD, dynamicRange = DynamicRange.HLG_10_BIT),
        Attempt("D1-4x3-HIGHEST-SDR", TIER_D1, "how far up the public ladder does 4:3 reach",
            aspect = AspectRatio.RATIO_4_3, quality = Quality.HIGHEST, dynamicRange = DynamicRange.SDR),
        Attempt("D1-4x3-QHD-HLG10", TIER_D1, "QHD is the extra rung CameraX 1.7 added",
            aspect = AspectRatio.RATIO_4_3, quality = Quality.QHD, dynamicRange = DynamicRange.HLG_10_BIT),
    )

    private fun tierD2(chars: CameraCharacteristics): List<Attempt> {
        val sensorPixelMode = Attempt(
            label = "D2-SENSOR_PIXEL_MODE", tier = TIER_D2,
            reason = "public 4:3 plus the standard sensor pixel mode session parameter",
            aspect = AspectRatio.RATIO_4_3, quality = Quality.FHD, dynamicRange = DynamicRange.SDR,
            sessionParameters = listOf(CaptureRequest.SENSOR_PIXEL_MODE to 1),
        )
        val vendor = chars.safeSessionKeys()
            .filter { VendorTags.INTERESTING.containsMatchIn(it.name) }
            .take(vendorTagBudget)
            .map { key ->
                Attempt(
                    label = "D2-VENDOR-${key.name}", tier = TIER_D2,
                    reason = "does the HAL accept this vendor session tag at bind time",
                    aspect = AspectRatio.RATIO_4_3, quality = Quality.FHD,
                    dynamicRange = DynamicRange.SDR,
                    sessionParameters = listOf(key to 1),
                )
            }
        return listOf(sensorPixelMode) + vendor
    }

    private fun tierD3(chars: CameraCharacteristics): List<Attempt> =
        Candidates.forCamera(chars)
            .filter { it.label.startsWith("OPENGATE") }
            .take(OPEN_GATE_ATTEMPTS)
            .map { candidate ->
                val size = candidate.recordSize
                Attempt(
                    label = "D3-OPENGATE-${size.width}x${size.height}", tier = TIER_D3,
                    reason = "name the size directly: full-sensor ${"%.4f".format(size.width.toFloat() / size.height)} stream at 30fps",
                    // Deliberately neither aspect ratio nor quality selector: CameraX only keeps
                    // an ordered resolution list while the quality selector is still its default
                    // (VideoCapture.updateCustomOrderedResolutionsByQuality).
                    aspect = AspectRatio.RATIO_DEFAULT,
                    quality = null,
                    dynamicRange = DynamicRange.SDR,
                    orderedResolutions = listOf(size),
                )
            }

    private fun recordOnce(
        provider: ProcessCameraProvider,
        selector: CameraSelector,
        info: CameraInfo,
        attempt: Attempt,
    ): JsonObject {
        val negotiated = Collections.synchronizedList(mutableListOf<String>())
        val built = runCatching { harness.onMain { buildConfig(attempt, negotiated) } }
        val pair = built.getOrNull()

        if (pair == null) {
            return buildJsonObject {
                put("label", attempt.label)
                put("tier", attempt.tier)
                put("reason", attempt.reason)
                put("constructionError", describe(built.exceptionOrNull()))
            }
        }
        val (config, recorder) = pair

        return try {
            buildJsonObject {
                put("label", attempt.label)
                put("tier", attempt.tier)
                put("reason", attempt.reason)
                put("request", requestSummary(attempt))
                put("isSessionConfigSupported", runCatching { harness.onMain { info.isSessionConfigSupported(config) } }.getOrNull())
                val bind = runCatching { harness.bind(provider, selector, config) }
                put("bindError", bind.exceptionOrNull()?.let(::describe))
                if (bind.isSuccess) {
                    harness.settle(SETTLE_AFTER_BIND_MS)
                    put("negotiatedBeforeRecord", jsonOf(negotiated.toList()))
                    val outcome = runCatching { recordShortClip(recorder) }
                    put("recordError", outcome.exceptionOrNull()?.let(::describe))
                    outcome.getOrNull()?.forEach { (key, value) -> put(key, jsonOf(value)) }
                    put("negotiatedAfterRecord", jsonOf(negotiated.toList()))
                }
            }
        } finally {
            harness.releaseAll(provider)
        }
    }

    private fun recordShortClip(recorder: Recorder): Map<String, Any?> {
        val file = File(outDir, "probe-d-${System.currentTimeMillis()}.mp4")
        val events = Collections.synchronizedList(mutableListOf<String>())
        val started = CountDownLatch(1)
        val finalized = CountDownLatch(1)
        var startGapMs = -1L
        var finalizeError: Int? = null

        val startedAt = SystemClock.elapsedRealtime()
        val recording: Recording? = runCatching {
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .start(harness.executor, Consumer<VideoRecordEvent> { event ->
                    val elapsed = SystemClock.elapsedRealtime() - startedAt
                    events += "${event.javaClass.simpleName}@${elapsed}ms"
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            startGapMs = elapsed
                            started.countDown()
                        }
                        is VideoRecordEvent.Finalize -> {
                            finalizeError = event.error
                            finalized.countDown()
                        }
                        else -> Unit
                    }
                })
        }.getOrNull()

        if (recording == null) return mapOf("recordStage" to "prepareRecording or start threw")

        val sawStart = started.await(START_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (sawStart) runCatching { Thread.sleep(RECORD_MS) }
        runCatching { recording.stop() }
        val sawFinalize = finalized.await(FINALIZE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        runCatching { recording.close() }

        return linkedMapOf(
            "startGapMs" to startGapMs,
            "sawStartEvent" to sawStart,
            "sawFinalizeEvent" to sawFinalize,
            "finalizeError" to finalizeError,
            "events" to events.toList(),
            "file" to file.name,
            "media" to readBack(file),
        )
    }

    private fun buildConfig(attempt: Attempt, sink: MutableList<String>): Pair<SessionConfig, Recorder> {
        val recorderBuilder = Recorder.Builder().setExecutor(harness.executor)
        if (attempt.aspect != AspectRatio.RATIO_DEFAULT) {
            recorderBuilder.setAspectRatio(attempt.aspect)
        }
        attempt.quality?.let {
            recorderBuilder.setQualitySelector(QualitySelector.from(it, FallbackStrategy.lowerQualityOrHigherThan(it)))
        }
        val captureBuilder = VideoCapture.Builder(recorderBuilder.build())
            .setDynamicRange(attempt.dynamicRange)
        attempt.orderedResolutions?.let { applyCustomOrderedResolutions(captureBuilder, it) }

        // Build once: the Recorder handed to prepareRecording must be the output of the very
        // VideoCapture instance that gets bound, not a second instance from the same builder.
        val capture = captureBuilder.build()
        val recorder = capture.output

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { request -> observe(request, sink) }
        }
        val useCases: List<UseCase> = listOf(preview, capture)

        val builder = SessionConfig.Builder(useCases)
        if (attempt.sessionParameters.isNotEmpty()) {
            builder.setInterop(Camera2Interop.forSessionConfig { interop ->
                attempt.sessionParameters.forEach { (key, value) ->
                    // A key's declared type is @hide, so which scalar the HAL accepts is probed
                    // rather than assumed. Whatever is accepted still has to survive the bind.
                    val candidates = listOf(value) + SCALAR_PROBE.filterNot { it == value }
                    val accepted = candidates.firstOrNull { candidate ->
                        runCatching { interop.setSessionParameter(unchecked(key), candidate) }.isSuccess
                    }
                    sink += "sessionParam ${key.name} tried=${jsonScalar(candidates)} accepted=${jsonScalar(accepted)}"
                }
            })
        }
        return builder.build() to recorder
    }

    /**
     * `setCustomOrderedResolutions` is `@RestrictTo(LIBRARY_GROUP)`: callable, but not a
     * supported public API. CameraX explicitly honors it as long as no QualitySelector and no
     * groupable feature is set, which is why tier D3 leaves both alone.
     */
    @SuppressLint("RestrictedApi")
    private fun applyCustomOrderedResolutions(builder: VideoCapture.Builder<Recorder>, sizes: List<Size>) {
        runCatching { builder.setCustomOrderedResolutions(sizes) }
    }

    private fun observe(request: SurfaceRequest, sink: MutableList<String>) {
        val size = runCatching { request.resolution }.getOrNull() ?: return
        sink += "preview ${size.width}x${size.height} dr=${runCatching { request.dynamicRange }.getOrNull()}"
        request.provideSurface(harness.newSurface(size.width, size.height), harness.executor) { }
    }

    private fun requestSummary(attempt: Attempt): JsonObject = buildJsonObject {
        put("aspect", aspectName(attempt.aspect))
        put("quality", attempt.quality?.toString())
        put("dynamicRange", attempt.dynamicRange.toString())
        put("orderedResolutions", jsonOf(attempt.orderedResolutions?.map { "${it.width}x${it.height}" }))
        put("sessionParameters", jsonOf(attempt.sessionParameters.map { "${it.first.name}=${it.second}" }))
    }

    private fun readBack(file: File): Map<String, Any?> {
        if (!file.exists() || file.length() == 0L) {
            return mapOf("exists" to file.exists(), "bytes" to file.length(), "error" to "no readable file")
        }
        val pfd = runCatching { ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }.getOrNull()
            ?: return mapOf("bytes" to file.length(), "error" to "could not open fd")

        val extractor = MediaExtractor()
        val out = linkedMapOf<String, Any?>("bytes" to file.length())
        try {
            extractor.setDataSource(pfd.fileDescriptor)
            out["trackCount"] = extractor.trackCount
            out["tracks"] = (0 until extractor.trackCount).map { index ->
                val format = runCatching { extractor.getTrackFormat(index) }.getOrNull()
                if (format == null) {
                    mapOf<String, Any?>("index" to index, "error" to "no format")
                } else {
                    format.keys.associateWith<String, Any?> { key -> formatValue(format, key) } + ("index" to index)
                }
            }
        } catch (t: Throwable) {
            out["extractError"] = describe(t)
        } finally {
            runCatching { extractor.release() }
            runCatching { pfd.close() }
            // The container dump above is the evidence; keeping the clips would fill the device
            // after a couple of dozen attempts.
            runCatching { file.delete() }
        }
        return out
    }

    /** Dump every key the container actually wrote, rather than probing keys we assume exist. */
    private fun formatValue(format: MediaFormat, key: String): Any? =
        runCatching { format.getString(key) }.getOrNull()
            ?: runCatching { format.getNumber(key)?.toString() }.getOrNull()
            ?: runCatching {
                format.getByteBuffer(key)?.let { buffer ->
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    bytes.joinToString("") { "%02x".format(it) }
                }
            }.getOrNull()
            ?: "<unreadable>"

    @Suppress("UNCHECKED_CAST")
    private fun unchecked(key: CaptureRequest.Key<*>): CaptureRequest.Key<Any> = key as CaptureRequest.Key<Any>

    private fun jsonScalar(value: Any?): String = value?.let { "${it}: ${it.javaClass.simpleName}" } ?: "none"

    private fun aspectName(aspect: Int): String = when (aspect) {
        AspectRatio.RATIO_4_3 -> "4:3"
        AspectRatio.RATIO_16_9 -> "16:9"
        else -> "default"
    }

    private fun describe(t: Throwable?): String? =
        t?.let { "${it.javaClass.simpleName}: ${it.message?.take(400)}" }

    private fun header(cameraId: String, reason: String): JsonObject = buildJsonObject {
        put("cameraId", cameraId)
        put("skipped", reason)
    }

    private data class Attempt(
        val label: String,
        val tier: String,
        val reason: String,
        val aspect: Int,
        val quality: Quality?,
        val dynamicRange: DynamicRange,
        val orderedResolutions: List<Size>? = null,
        val sessionParameters: List<Pair<CaptureRequest.Key<*>, Any>> = emptyList(),
    )

    private companion object {
        const val TIER_D1 = "D1"
        const val TIER_D2 = "D2"
        const val TIER_D3 = "D3"
        const val SETTLE_AFTER_BIND_MS = 500L
        const val RECORD_MS = 1_500L
        const val START_TIMEOUT_MS = 10_000L
        const val FINALIZE_TIMEOUT_MS = 20_000L
        const val OPEN_GATE_ATTEMPTS = 3

        /** Scalar types tried, in order, when a vendor tag's declared type is unknown. */
        val SCALAR_PROBE: List<Any> = listOf(1, 1L, 1.0f, true)
    }
}
