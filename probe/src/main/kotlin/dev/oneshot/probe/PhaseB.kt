package dev.oneshot.probe

import android.util.Range
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoCapabilities
import androidx.core.util.Consumer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.Executor

/**
 * Phase B: what CameraX itself believes about this camera, and which session shapes it will
 * actually bind.
 *
 * Phase A dumps the HAL's own claims and phase C asks the HAL to admit a stream combination.
 * Neither answers "will CameraX build a session out of it", which is the question the app
 * architecture depends on — CameraX inserts its own preview stream, picks its own sizes, and
 * may refuse an effect or a frame-rate range regardless of what the HAL allows.
 */
class PhaseB(
    private val harness: BindHarness,
    private val onProgress: (String) -> Unit,
) {

    fun runFor(provider: ProcessCameraProvider, cameraId: String): JsonObject {
        val selector = runCatching { Camera2Interop.getCameraSelectorFromCameraId(cameraId) }.getOrNull()
            ?: return skipped(cameraId, "no CameraSelector for cameraId")

        val info = runCatching { provider.getCameraInfo(selector) }
            .getOrNull() ?: return skipped(cameraId, "getCameraInfo threw for this selector")

        return buildJsonObject {
            put("cameraId", cameraId)
            put("cameraInfo", describeInfo(info))
            put("videoCapabilities", videoCapabilities(info))
            put("bindMatrix", JsonArray(SHAPES.map { attempt(provider, selector, info, it) }))
        }
    }

    private fun describeInfo(info: CameraInfo): JsonObject = buildJsonObject {
        put("lensFacing", info.lensFacing)
        put("implementationType", info.implementationType)
        put("sensorRotationDegrees", info.sensorRotationDegrees)
        put("hasFlashUnit", info.hasFlashUnit())
        put("isLogicalMultiCameraSupported", info.isLogicalMultiCameraSupported)
        put("isZslSupported", info.isZslSupported)
        put("isPrivateReprocessingSupported", info.isPrivateReprocessingSupported)
        put("physicalCameraIds", jsonOf(info.physicalCameraInfos.map {
            runCatching { Camera2Interop.getCameraId(it) }.getOrNull()
        }))
        val zoom = runCatching { info.zoomState.value }.getOrNull()
        if (zoom != null) {
            put("zoomRatioRange", jsonOf(listOf(zoom.minZoomRatio, zoom.maxZoomRatio)))
        }
        put("supportedFrameRateRangesNoConfig", jsonOf(
            runCatching { info.supportedFrameRateRanges }.getOrNull()
                ?.map { "${it.lower}-${it.upper}" }
        ))

        val asked = listOf(DynamicRange.SDR, DynamicRange.HLG_10_BIT, DynamicRange.HDR10_10_BIT,
            DynamicRange.HDR10_PLUS_10_BIT, DynamicRange.HDR_UNSPECIFIED_10_BIT)
        val answered = runCatching { info.querySupportedDynamicRanges(asked.toSet()) }.getOrNull().orEmpty()
        put("dynamicRangesAsked", jsonOf(asked.map { it.toString() }))
        put("dynamicRangesSupported", jsonOf(answered.map { it.toString() }))
        put("dynamicRangesRefused", jsonOf(asked.filter { it !in answered }.map { it.toString() }))
    }

    private fun videoCapabilities(info: CameraInfo): JsonObject = buildJsonObject {
        val recorder = runCatching { Recorder.Builder().build() }.getOrNull()
        put("qualityLadder", jsonOf(Quality.getSortedQualities().map { quality ->
            mapOf(
                "quality" to quality.toString(),
                "isQualitySupported" to runCatching { QualitySelector.isQualitySupported(info, quality) }.getOrNull(),
                "resolution" to runCatching { QualitySelector.getResolution(info, quality) }
                    .getOrNull()?.let { "${it.width}x${it.height}" },
            )
        }))
        put("byCapabilitiesSource", jsonOf(
            listOf(
                "camcorderProfile" to Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                "codecCapabilities" to Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
            ).map { (name, source) ->
                val attempt = runCatching { recorder?.getMediaCapabilities(info, source) }
                mapOf(
                    "source" to name,
                    "error" to attempt.exceptionOrNull()?.let(::describe),
                    "capabilities" to attempt.getOrNull()?.let { describeCapabilities(it) },
                )
            }
        ))
    }

    private fun describeCapabilities(caps: VideoCapabilities): Map<String, Any?> = mapOf(
        "dynamicRanges" to caps.supportedDynamicRanges.map { it.toString() },
        "stabilizationSupported" to runCatching { caps.isStabilizationSupported }.getOrNull(),
        "byDynamicRange" to caps.supportedDynamicRanges.map { range ->
            val qualities = runCatching { caps.getSupportedQualities(range) }.getOrNull().orEmpty()
            mapOf(
                "dynamicRange" to range.toString(),
                "qualities" to qualities.map { it.toString() },
                "resolutions" to qualities.map { quality ->
                    "${quality}=" + runCatching { caps.getResolution(quality, range) }
                        .getOrNull()?.let { "${it.width}x${it.height}" }
                },
            )
        },
    )

    private fun attempt(
        provider: ProcessCameraProvider,
        selector: CameraSelector,
        info: CameraInfo,
        shape: Shape,
    ): JsonObject {
        onProgress("  Phase B: ${shape.label}")
        val negotiated = mutableListOf<String>()
        val built = runCatching { harness.onMain { buildSessionConfig(shape, negotiated) } }
        val config = built.getOrNull() ?: return buildJsonObject {
            put("label", shape.label)
            put("reason", shape.reason)
            put("constructionError", describe(built.exceptionOrNull()))
        }

        try {
            return buildJsonObject {
                put("label", shape.label)
                put("reason", shape.reason)
                put("shape", describeShape(shape))
                put("isSessionConfigSupported", runCatching { harness.onMain { info.isSessionConfigSupported(config) } }.getOrNull())
                val bound = runCatching { harness.bind(provider, selector, config) }
                put("bindError", bound.exceptionOrNull()?.let(::describe))
                put("bindCameraLensFacing", bound.getOrNull()?.cameraInfo?.lensFacing)
                put("isBound", runCatching { harness.onMain { provider.isBound(config) } }.getOrNull())
                harness.settle(SETTLE_MS)
                put("negotiated", jsonOf(negotiated))
                put("supportedFrameRateRangesForConfig", jsonOf(
                    runCatching { harness.onMain { info.getSupportedFrameRateRanges(config) } }.getOrNull()
                        ?.map { "${it.lower}-${it.upper}" }
                ))
            }
        } finally {
            harness.releaseAll(provider)
        }
    }

    private fun buildSessionConfig(shape: Shape, sink: MutableList<String>): SessionConfig {
        val useCases = mutableListOf<UseCase>()

        if (shape.preview) {
            useCases += Preview.Builder().build().apply {
                setSurfaceProvider { request -> surfaceFor(request, sink, "preview") }
            }
        }
        if (shape.video) {
            useCases += videoUseCase(shape, sink)
        }
        if (shape.withImageCapture) {
            useCases += ImageCapture.Builder().build()
        }

        val builder = SessionConfig.Builder(useCases)
        shape.frameRateRange?.let { builder.setFrameRateRange(it) }
        if (shape.effectTargets != 0) {
            builder.addEffect(NoOpEffect(shape.effectTargets, harness.executor, sink, harness))
        }
        return builder.build()
    }

    private fun videoUseCase(shape: Shape, sink: MutableList<String>): VideoCapture<Recorder> {
        val recorderBuilder = Recorder.Builder()
            .setAspectRatio(shape.aspect)
            .setExecutor(harness.executor)
        shape.quality?.let {
            recorderBuilder.setQualitySelector(QualitySelector.from(it, FallbackStrategy.lowerQualityOrHigherThan(it)))
        }
        val captureBuilder = VideoCapture.Builder(recorderBuilder.build())
            .setDynamicRange(shape.dynamicRange)
        if (shape.streamUseCaseInterop) {
            captureBuilder.setInterop(Camera2Interop.forUseCase { it.setStreamUseCase(VIDEO_RECORD_USE_CASE) })
        }
        sink += "video built: aspect=${aspectName(shape.aspect)} quality=${shape.quality} dr=${shape.dynamicRange}"
        return captureBuilder.build()
    }

    private fun surfaceFor(request: SurfaceRequest, sink: MutableList<String>, tag: String) {
        val size = runCatching { request.resolution }.getOrNull() ?: return
        sink += "$tag request ${size.width}x${size.height} dr=${runCatching { request.dynamicRange }.getOrNull()}" +
            " sessionType=${runCatching { request.sessionType }.getOrNull()}" +
            " fps=${runCatching { request.expectedFrameRate }.getOrNull()}"
        request.provideSurface(harness.newSurface(size.width, size.height), harness.executor) { result ->
            sink += "$tag surface result=${result.resultCode}"
        }
    }

    private fun describeShape(shape: Shape): JsonObject = buildJsonObject {
        put("preview", shape.preview)
        put("video", shape.video)
        put("aspect", aspectName(shape.aspect))
        put("quality", shape.quality?.toString())
        put("dynamicRange", shape.dynamicRange.toString())
        put("frameRateRange", shape.frameRateRange?.let { "${it.lower}-${it.upper}" })
        put("effectTargets", shape.effectTargets)
        put("withImageCapture", shape.withImageCapture)
        put("streamUseCaseInterop", shape.streamUseCaseInterop)
    }

    private fun skipped(cameraId: String, reason: String): JsonObject = buildJsonObject {
        put("cameraId", cameraId)
        put("skipped", reason)
    }

    private fun aspectName(aspect: Int): String = when (aspect) {
        AspectRatio.RATIO_4_3 -> "4:3"
        AspectRatio.RATIO_16_9 -> "16:9"
        else -> "default"
    }

    private fun describe(t: Throwable?): String? =
        t?.let { "${it.javaClass.simpleName}: ${it.message?.take(400)}" }

    /** One session shape to try. Described rather than encoded so the report stays readable. */
    private data class Shape(
        val label: String,
        val reason: String,
        val preview: Boolean = true,
        val video: Boolean = true,
        val aspect: Int = AspectRatio.RATIO_16_9,
        val quality: Quality? = Quality.FHD,
        val dynamicRange: DynamicRange = DynamicRange.SDR,
        val frameRateRange: Range<Int>? = null,
        val effectTargets: Int = 0,
        val withImageCapture: Boolean = false,
        val streamUseCaseInterop: Boolean = false,
    )

    internal class NoOpEffect(
        targets: Int,
        executor: Executor,
        sink: MutableList<String>,
        harness: BindHarness,
    ) : CameraEffect(
        targets,
        executor,
        ProbeSurfaceProcessor(sink, harness),
        Consumer<Throwable> { t -> sink += "effectError ${t.javaClass.simpleName}: ${t.message}" },
    )

    private companion object {
        const val SETTLE_MS = 700L
        const val VIDEO_RECORD_USE_CASE = 3L

        val SHAPES = listOf(
            Shape("PREVIEW-ONLY", "does a preview-only session bind at all", video = false),
            Shape("16x9-FHD-SDR", "PM daily recipe in the CameraX default shape"),
            Shape("4x3-FHD-SDR", "open-gate tier D1 without HDR", aspect = AspectRatio.RATIO_4_3),
            Shape("4x3-FHD-HLG10", "open-gate tier D1 with 10-bit HLG",
                aspect = AspectRatio.RATIO_4_3, dynamicRange = DynamicRange.HLG_10_BIT),
            Shape("16x9-FHD-HLG10", "HLG10 in the ordinary shape, as the control",
                dynamicRange = DynamicRange.HLG_10_BIT),
            Shape("PREVIEW+VIDEO+IMAGE", "concurrent still stream", withImageCapture = true),
            Shape("EFFECT-PREVIEW", "can a CameraEffect be added at all",
                effectTargets = CameraEffect.PREVIEW),
            Shape("EFFECT-PREVIEW+VIDEO", "the LUT burn-in question",
                effectTargets = CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE),
            Shape("EFFECT-4x3-HLG10", "LUT + open gate + HLG10 together",
                aspect = AspectRatio.RATIO_4_3, dynamicRange = DynamicRange.HLG_10_BIT,
                effectTargets = CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE),
            Shape("FPS-RANGE-30-30", "does CameraX accept a locked 30fps range",
                frameRateRange = Range(30, 30)),
            Shape("STREAM-USE-CASE-INTEROP", "public 1.7 use-case interop path",
                streamUseCaseInterop = true),
        )
    }
}

/**
 * Records what CameraX negotiates for an effect path instead of rendering anything.
 *
 * Nothing drains the texture we hand back, so the stream stalls after a few frames. That is
 * deliberate: phase B asks "is this shape accepted", not "does the GL chain work".
 */
private class ProbeSurfaceProcessor(
    private val sink: MutableList<String>,
    private val harness: BindHarness,
) : SurfaceProcessor {

    override fun onInputSurface(request: SurfaceRequest) {
        val size = runCatching { request.resolution }.getOrNull() ?: return
        sink += "effect in ${size.width}x${size.height} dr=${runCatching { request.dynamicRange }.getOrNull()}"
        request.provideSurface(harness.newSurface(size.width, size.height), harness.executor) { }
    }

    override fun onOutputSurface(output: SurfaceOutput) {
        val size = runCatching { output.size }.getOrNull()
        sink += "effect out ${size?.let { "${it.width}x${it.height}" }}" +
            " fmt=${runCatching { output.format }.getOrNull()}" +
            " targets=${runCatching { output.targets }.getOrNull()}"
    }
}
