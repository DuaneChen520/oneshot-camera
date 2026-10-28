package dev.oneshot.camera

import dev.oneshot.data.CameraSettings
import dev.oneshot.data.VideoStabilizationMode

/**
 * Simple data class representing a frame rate range, used instead of android.util.Range
 * to keep PipelineParams testable in pure JVM unit tests without Android stubs.
 */
data class FpsRange(val lower: Int, val upper: Int)

/**
 * Pure-function parameter builders for camera pipeline isolation.
 *
 * Each builder produces a [Map] of string key → value pairs representing
 * Camera2 CaptureRequest parameters that must be injected exclusively into
 * their respective pipeline (ImageCapture or VideoCapture) via Camera2Interop.Extender.
 *
 * String keys match the Camera2 CaptureRequest.Key names for traceability.
 * The caller (CameraController) maps these back to typed CaptureRequest.Key objects
 * when applying via Camera2Interop.
 *
 * This architecture guarantees that video FPS locks never contaminate the
 * still capture AE engine, and ISP high-quality modes never burden
 * the real-time video encoder.
 */

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

    // Camera2 CaptureRequest mode constants (defined here to be testable without Android stubs)
    const val EDGE_MODE_HIGH_QUALITY = 2
    const val NOISE_REDUCTION_MODE_HIGH_QUALITY = 2
    const val COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY = 2
    const val SHADING_MODE_HIGH_QUALITY = 2

    // Parameter keys (matching Camera2 CaptureRequest.Key names)
    const val KEY_EDGE_MODE = "android.edge.mode"
    const val KEY_NOISE_REDUCTION_MODE = "android.noiseReduction.mode"
    const val KEY_COLOR_CORRECTION_ABERRATION_MODE = "android.colorCorrection.aberrationMode"
    const val KEY_SHADING_MODE = "android.shading.mode"
    const val KEY_JPEG_QUALITY = "android.jpeg.quality"
    const val KEY_AE_TARGET_FPS_RANGE = "android.control.aeTargetFpsRange"

    fun build(settings: CameraSettings): Map<String, Any> {
        val params = mutableMapOf<String, Any>()

        // Hardware ISP high-quality processing chain
        params[KEY_EDGE_MODE] = EDGE_MODE_HIGH_QUALITY
        params[KEY_NOISE_REDUCTION_MODE] = NOISE_REDUCTION_MODE_HIGH_QUALITY
        params[KEY_COLOR_CORRECTION_ABERRATION_MODE] = COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
        params[KEY_SHADING_MODE] = SHADING_MODE_HIGH_QUALITY

        // Maximum JPEG encoding quality — no compression artifact degradation
        params[KEY_JPEG_QUALITY] = 100.toByte()

        // INTENTIONALLY omitted: KEY_AE_TARGET_FPS_RANGE
        // The AE engine must be free to select high-speed shutter (1/200s–1/2000s)
        // to freeze hand tremor on large-sensor cameras.

        return params
    }
}

/**
 * Generates Camera2 capture request parameters for the **video recording** pipeline.
 *
 * Invariants:
 * - ALWAYS includes FPS range lock matching the user's selected frame rate preset.
 * - ALWAYS includes video stabilization mode matching user preference.
 * - NEVER includes ISP HIGH_QUALITY modes (too computationally expensive for real-time encoding;
 *   video uses FAST mode implicitly via HAL defaults).
 * - NEVER includes JPEG_QUALITY (not applicable to video).
 */
object VideoCaptureParams {

    // Video stabilization mode constants
    const val VIDEO_STABILIZATION_MODE_OFF = 0
    const val VIDEO_STABILIZATION_MODE_ON = 1
    const val OIS_MODE_OFF = 0
    const val OIS_MODE_ON = 1

    // Parameter keys
    const val KEY_AE_TARGET_FPS_RANGE = "android.control.aeTargetFpsRange"
    const val KEY_VIDEO_STABILIZATION_MODE = "android.control.videoStabilizationMode"
    const val KEY_LENS_OIS_MODE = "android.lens.opticalStabilizationMode"

    fun build(settings: CameraSettings): Map<String, Any> {
        val params = mutableMapOf<String, Any>()

        // FPS lock — exclusively for the video encoder, never the still capture AE
        val targetFps = settings.fpsPreset.targetFps
        params[KEY_AE_TARGET_FPS_RANGE] = FpsRange(targetFps, targetFps)

        // Video stabilization settings
        when (settings.videoStabilization) {
            VideoStabilizationMode.STANDARD -> {
                params[KEY_VIDEO_STABILIZATION_MODE] = VIDEO_STABILIZATION_MODE_OFF
                params[KEY_LENS_OIS_MODE] = OIS_MODE_ON
            }
            VideoStabilizationMode.ENHANCED -> {
                params[KEY_VIDEO_STABILIZATION_MODE] = VIDEO_STABILIZATION_MODE_ON
                params[KEY_LENS_OIS_MODE] = OIS_MODE_ON
            }
            VideoStabilizationMode.OFF -> {
                params[KEY_VIDEO_STABILIZATION_MODE] = VIDEO_STABILIZATION_MODE_OFF
                params[KEY_LENS_OIS_MODE] = OIS_MODE_OFF
            }
        }

        // INTENTIONALLY omitted: EDGE_MODE, NOISE_REDUCTION_MODE, JPEG_QUALITY
        // Video uses HAL default FAST modes for real-time throughput.

        return params
    }
}
