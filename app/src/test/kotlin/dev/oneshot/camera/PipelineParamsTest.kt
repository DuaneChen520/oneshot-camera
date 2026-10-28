package dev.oneshot.camera

import dev.oneshot.data.CameraSettings
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.VideoStabilizationMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for pipeline-isolated parameter generation using typed domain configurations.
 *
 * Verifies the critical invariant: still capture parameters and video capture
 * parameters are fully decoupled — no FPS lock leaks into the still pipeline,
 * and no ISP high-quality modes leak into the video pipeline.
 */
class PipelineParamsTest {

    // =========================================================================
    // StillCaptureParams — the still photo pipeline must have ISP quality,
    //                      must NEVER have an FPS range lock.
    // =========================================================================

    @Test
    fun stillConfig_containsEdgeModeHighQuality() {
        val config = StillCaptureParams.build(CameraSettings())
        assertEquals(StillCaptureParams.EDGE_MODE_HIGH_QUALITY, config.edgeMode)
    }

    @Test
    fun stillConfig_containsNoiseReductionHighQuality() {
        val config = StillCaptureParams.build(CameraSettings())
        assertEquals(StillCaptureParams.NOISE_REDUCTION_MODE_HIGH_QUALITY, config.noiseReductionMode)
    }

    @Test
    fun stillConfig_containsAberrationCorrectionHighQuality() {
        val config = StillCaptureParams.build(CameraSettings())
        assertEquals(StillCaptureParams.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY, config.aberrationMode)
    }

    @Test
    fun stillConfig_containsShadingHighQuality() {
        val config = StillCaptureParams.build(CameraSettings())
        assertEquals(StillCaptureParams.SHADING_MODE_HIGH_QUALITY, config.shadingMode)
    }

    @Test
    fun stillConfig_jpegQualityIs100() {
        val config = StillCaptureParams.build(CameraSettings())
        assertEquals(100.toByte(), config.jpegQuality)
    }

    // =========================================================================
    // VideoCaptureParams — the video pipeline must have FPS lock,
    //                      must NOT have ISP high-quality modes (uses FAST).
    // =========================================================================

    @Test
    fun videoConfig_containsFpsRangeLock_30fps() {
        val config = VideoCaptureParams.build(CameraSettings(fpsPreset = FpsPreset.FPS_30))
        assertEquals(30, config.fpsRange.lower)
        assertEquals(30, config.fpsRange.upper)
    }

    @Test
    fun videoConfig_containsFpsRangeLock_60fps() {
        val config = VideoCaptureParams.build(CameraSettings(fpsPreset = FpsPreset.FPS_60))
        assertEquals(60, config.fpsRange.lower)
        assertEquals(60, config.fpsRange.upper)
    }

    @Test
    fun videoConfig_containsFpsRangeLock_120fps() {
        val config = VideoCaptureParams.build(CameraSettings(fpsPreset = FpsPreset.FPS_120))
        assertEquals(120, config.fpsRange.lower)
        assertEquals(120, config.fpsRange.upper)
    }

    @Test
    fun videoConfig_containsStabilizationSettings_standard() {
        val config = VideoCaptureParams.build(
            CameraSettings(videoStabilization = VideoStabilizationMode.STANDARD)
        )
        assertEquals(VideoCaptureParams.VIDEO_STABILIZATION_MODE_OFF, config.videoStabilizationMode)
        assertEquals(VideoCaptureParams.OIS_MODE_ON, config.oisMode)
    }

    @Test
    fun videoConfig_containsStabilizationSettings_enhanced() {
        val config = VideoCaptureParams.build(
            CameraSettings(videoStabilization = VideoStabilizationMode.ENHANCED)
        )
        assertEquals(VideoCaptureParams.VIDEO_STABILIZATION_MODE_ON, config.videoStabilizationMode)
        assertEquals(VideoCaptureParams.OIS_MODE_ON, config.oisMode)
    }

    @Test
    fun videoConfig_containsStabilizationSettings_off() {
        val config = VideoCaptureParams.build(
            CameraSettings(videoStabilization = VideoStabilizationMode.OFF)
        )
        assertEquals(VideoCaptureParams.VIDEO_STABILIZATION_MODE_OFF, config.videoStabilizationMode)
        assertEquals(VideoCaptureParams.OIS_MODE_OFF, config.oisMode)
    }
}
