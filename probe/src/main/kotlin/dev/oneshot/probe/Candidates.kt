package dev.oneshot.probe

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.DynamicRangeProfiles
import android.util.Size
import kotlin.math.abs

/** One stream combination worth asking the HAL about. */
data class StreamCandidate(
    val label: String,
    val reason: String,
    /** Preview stream size, or null to omit the preview stream entirely. */
    val previewSize: Size?,
    /** Record stream size and the dynamic range profile to request for it. */
    val recordSize: Size,
    val dynamicRangeProfile: Long,
    val includeStill: Boolean,
)

/**
 * Derives the stream combinations to precheck, from what the HAL actually advertises rather
 * than from a hardcoded list — a device with no FHD PRIVATE stream would otherwise just
 * report "rejected" for reasons unrelated to the question we are asking.
 */
object Candidates {

    private const val NS_30FPS = 33_333_333L
    private const val NS_60FPS = 16_666_667L

    fun forCamera(chars: CameraCharacteristics): List<StreamCandidate> {
        val map = chars.safe(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return emptyList()
        val privateSizes = runCatching { map.getOutputSizes(ImageFormat.PRIVATE) }
            .getOrNull()?.toList() ?: emptyList()
        if (privateSizes.isEmpty()) return emptyList()

        val hlg10Supported = chars.supportedDynamicRangeProfiles().contains(DynamicRangeProfiles.HLG10)
        val out = mutableListOf<StreamCandidate>()

        // The PM's daily recipe: 1080p + 30fps, in SDR always and in HLG10 when advertised.
        findAtAspect(privateSizes, map, 16f / 9f, NS_30FPS, maxWidth = 1920)?.let { fhd ->
            out += StreamCandidate("FHD30-SDR", "PM daily recipe, SDR baseline", fhd, fhd, DynamicRangeProfiles.STANDARD, true)
            if (hlg10Supported) {
                out += StreamCandidate("FHD30-HLG10", "PM daily recipe with 10-bit HLG", fhd, fhd, DynamicRangeProfiles.HLG10, true)
                out += StreamCandidate("FHD30-HLG10-no-still", "HLG10 without a concurrent still stream", fhd, fhd, DynamicRangeProfiles.HLG10, false)
            }
        }

        findAtAspect(privateSizes, map, 16f / 9f, NS_60FPS, maxWidth = 1920)?.let { fhd60 ->
            out += StreamCandidate("FHD60-SDR", "high frame rate sanity check", fhd60, fhd60, DynamicRangeProfiles.STANDARD, false)
        }

        findAtAspect(privateSizes, map, 16f / 9f, NS_30FPS, maxWidth = 2560)?.let { qhd ->
            out += StreamCandidate("QHD30-SDR", "QHD exists in CameraX 1.7 Quality", qhd, qhd, DynamicRangeProfiles.STANDARD, false)
        }

        findAtAspect(privateSizes, map, 16f / 9f, NS_30FPS, maxWidth = 3840)?.let { uhd ->
            out += StreamCandidate("UHD30-SDR", "4K30 baseline", uhd, uhd, DynamicRangeProfiles.STANDARD, false)
            if (hlg10Supported) {
                out += StreamCandidate("UHD30-HLG10", "4K30 with 10-bit HLG", uhd, uhd, DynamicRangeProfiles.HLG10, false)
            }
        }

        // Full-sensor-aspect streams: the open-gate question. These are the candidates whose
        // aspect ratio matches SENSOR_INFO_ACTIVE_ARRAY_SIZE while still sustaining 30fps.
        val activeArray = chars.safe(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        if (activeArray != null) {
            val activeAspect = activeArray.width().toFloat() / activeArray.height()
            privateSizes
                .filter { abs(it.width.toFloat() / it.height - activeAspect) <= 0.02f * activeAspect }
                .filter { runCatching { map.getOutputMinFrameDuration(ImageFormat.PRIVATE, it) }
                    .getOrDefault(Long.MAX_VALUE) in 1..NS_30FPS }
                .sortedByDescending { it.width.toLong() * it.height }
                .take(4)
                .forEach { size ->
                    val aspectLabel = aspectTag(size)
                    out += StreamCandidate(
                        label = "OPENGATE-${size.width}x${size.height}",
                        reason = "aspect matches active array ($aspectLabel), sustains 30fps",
                        previewSize = findAtAspect(privateSizes, map, 16f / 9f, NS_30FPS, maxWidth = 1920),
                        recordSize = size,
                        dynamicRangeProfile = if (hlg10Supported) DynamicRangeProfiles.HLG10 else DynamicRangeProfiles.STANDARD,
                        includeStill = false,
                    )
                }
        }

        // The widest video-capable stream the HAL advertises, whatever its aspect. If open gate
        // is reachable at all it is at or below this.
        privateSizes
            .filter { runCatching { map.getOutputMinFrameDuration(ImageFormat.PRIVATE, it) }
                .getOrDefault(Long.MAX_VALUE) in 1..NS_30FPS }
            .maxByOrNull { it.width.toLong() * it.height }
            ?.let { widest ->
                if (out.none { it.recordSize == widest }) {
                    out += StreamCandidate(
                        label = "WIDEST-${widest.width}x${widest.height}",
                        reason = "largest PRIVATE stream at <=30fps, aspect ${aspectTag(widest)}",
                        previewSize = findAtAspect(privateSizes, map, 16f / 9f, NS_30FPS, maxWidth = 1920),
                        recordSize = widest,
                        dynamicRangeProfile = DynamicRangeProfiles.STANDARD,
                        includeStill = false,
                    )
                }
            }

        return out.distinctBy { it.label }
    }

    internal fun CameraCharacteristics.supportedDynamicRangeProfiles(): Set<Long> {
        val profiles = safe(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
            ?: return setOf(DynamicRangeProfiles.STANDARD)
        // getSupportedProfiles() returns Set<Long>, not a bit field.
        return runCatching { profiles.supportedProfiles }.getOrElse { setOf(DynamicRangeProfiles.STANDARD) }
    }

    /** Largest size at the requested aspect whose frame duration allows the requested fps. */
    private fun findAtAspect(
        sizes: List<Size>,
        map: android.hardware.camera2.params.StreamConfigurationMap,
        aspect: Float,
        maxFrameDurationNs: Long,
        maxWidth: Int,
    ): Size? = sizes
        .filter { abs(it.width.toFloat() / it.height - aspect) < 0.02f }
        .filter { it.width <= maxWidth }
        .filter { runCatching { map.getOutputMinFrameDuration(ImageFormat.PRIVATE, it) }
            .getOrDefault(Long.MAX_VALUE) in 1..maxFrameDurationNs }
        .maxByOrNull { it.width.toLong() * it.height }

    private fun aspectTag(size: Size): String {
        val r = size.width.toFloat() / size.height
        return when {
            abs(r - 4f / 3f) < 0.02f -> "4:3"
            abs(r - 16f / 9f) < 0.02f -> "16:9"
            abs(r - 1f) < 0.02f -> "1:1"
            abs(r - 3f / 2f) < 0.02f -> "3:2"
            abs(r - 2f) < 0.05f -> "2:1"
            else -> "%.3f".format(r)
        }
    }
}

/** Frame duration for the given fps range, used in report annotations. */
