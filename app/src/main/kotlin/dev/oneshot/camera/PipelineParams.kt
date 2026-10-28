package dev.oneshot.camera

import dev.oneshot.data.CameraSettings
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.VideoStabilizationMode

/**
 * Pure-function parameter builders for camera pipeline isolation.
 *
 * Each builder produces a strongly-typed configuration object representing
 * Camera2 CaptureRequest parameters that must be injected exclusively into
 * their respective pipeline (ImageCapture or VideoCapture) via Camera2Interop.Extender.
 *
 * This architecture guarantees that video FPS locks never contaminate the
 * still capture AE engine, and ISP high-quality modes never burden
 * the real-time video encoder.
 */

/**
 * Strongly-typed frame rate range representation to keep parameter generation
 * testable in pure JVM unit tests without relying on Android framework stubs.
 */
data class FpsRange(val lower: Int, val upper: Int)

/**
 * Strongly-typed configuration for the still photo capture pipeline.
 */
data class StillCaptureConfig(
    val edgeMode: Int = 2, // CaptureRequest.EDGE_MODE_HIGH_QUALITY
    val noiseReductionMode: Int = 2, // CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
    val aberrationMode: Int = 2, // CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
    val shadingMode: Int = 2, // CaptureRequest.SHADING_MODE_HIGH_QUALITY
    val jpegQuality: Byte = 100.toByte(),
)

/**
 * Strongly-typed configuration for the video recording pipeline.
 */
data class VideoCaptureConfig(
    val fpsRange: FpsRange,
    val videoStabilizationMode: Int,
    val oisMode: Int,
)

/**
 * Generates Camera2 capture request parameters for the **still photo** pipeline.
 *
 * Invariants:
 * - ALWAYS includes hardware ISP high-quality processing (edge sharpening, noise reduction,
 *   chromatic aberration correction, lens shading correction).
 * - ALWAYS sets JPEG quality to maximum (100).
 * - NEVER includes FPS range lock (must not constrain AE shutter speed selection).
 */
object StillCaptureParams {

    // Camera2 CaptureRequest mode constants
    const val EDGE_MODE_HIGH_QUALITY = 2
    const val NOISE_REDUCTION_MODE_HIGH_QUALITY = 2
    const val COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY = 2
    const val SHADING_MODE_HIGH_QUALITY = 2

    fun build(settings: CameraSettings = CameraSettings()): StillCaptureConfig {
        return StillCaptureConfig(
            edgeMode = EDGE_MODE_HIGH_QUALITY,
            noiseReductionMode = NOISE_REDUCTION_MODE_HIGH_QUALITY,
            aberrationMode = COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY,
            shadingMode = SHADING_MODE_HIGH_QUALITY,
            jpegQuality = 100.toByte(),
        )
    }
}

/**
 * Generates Camera2 capture request parameters for the **video recording** pipeline.
 *
 * Invariants:
 * - ALWAYS includes FPS range lock matching the user's selected frame rate preset.
 * - ALWAYS includes video stabilization mode matching user preference.
 * - NEVER includes ISP HIGH_QUALITY modes (too computationally expensive for real-time encoding).
 * - NEVER includes JPEG_QUALITY (not applicable to video).
 */
object VideoCaptureParams {

    // Video stabilization mode constants
    const val VIDEO_STABILIZATION_MODE_OFF = 0
    const val VIDEO_STABILIZATION_MODE_ON = 1
    const val OIS_MODE_OFF = 0
    const val OIS_MODE_ON = 1

    fun build(settings: CameraSettings): VideoCaptureConfig {
        val targetFps = settings.fpsPreset.targetFps
        val fpsRange = FpsRange(targetFps, targetFps)

        val (videoStab, ois) = when (settings.videoStabilization) {
            VideoStabilizationMode.STANDARD -> VIDEO_STABILIZATION_MODE_OFF to OIS_MODE_ON
            VideoStabilizationMode.ENHANCED -> VIDEO_STABILIZATION_MODE_ON to OIS_MODE_ON
            VideoStabilizationMode.OFF -> VIDEO_STABILIZATION_MODE_OFF to OIS_MODE_OFF
        }

        return VideoCaptureConfig(
            fpsRange = fpsRange,
            videoStabilizationMode = videoStab,
            oisMode = ois,
        )
    }
}
