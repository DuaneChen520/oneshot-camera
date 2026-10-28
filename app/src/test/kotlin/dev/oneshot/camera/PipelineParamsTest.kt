package dev.oneshot.camera

import dev.oneshot.data.CameraSettings
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.VideoStabilizationMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for pipeline-isolated parameter generation.
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
    fun stillParams_alwaysContainsEdgeModeHighQuality() {
        val params = StillCaptureParams.build(CameraSettings())
        assertEquals(
            StillCaptureParams.EDGE_MODE_HIGH_QUALITY,
            params[StillCaptureParams.KEY_EDGE_MODE]
        )
    }

    @Test
    fun stillParams_alwaysContainsNoiseReductionHighQuality() {
        val params = StillCaptureParams.build(CameraSettings())
        assertEquals(
            StillCaptureParams.NOISE_REDUCTION_MODE_HIGH_QUALITY,
            params[StillCaptureParams.KEY_NOISE_REDUCTION_MODE]
        )
    }

    @Test
    fun stillParams_alwaysContainsAberrationCorrectionHighQuality() {
        val params = StillCaptureParams.build(CameraSettings())
        assertEquals(
            StillCaptureParams.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY,
            params[StillCaptureParams.KEY_COLOR_CORRECTION_ABERRATION_MODE]
        )
    }

    @Test
    fun stillParams_alwaysContainsShadingHighQuality() {
        val params = StillCaptureParams.build(CameraSettings())
        assertEquals(
            StillCaptureParams.SHADING_MODE_HIGH_QUALITY,
            params[StillCaptureParams.KEY_SHADING_MODE]
        )
    }

    @Test
    fun stillParams_jpegQualityIs100() {
        val params = StillCaptureParams.build(CameraSettings())
        assertEquals(100.toByte(), params[StillCaptureParams.KEY_JPEG_QUALITY])
    }

    @Test
    fun stillParams_neverContainsFpsRangeLock() {
        // The FPS range must NEVER appear in still capture params.
        // This was the root cause of the 1/33s motion blur bug.
        val params = StillCaptureParams.build(CameraSettings())
        assertFalse(
            "FPS range lock MUST NOT appear in still capture params",
            params.containsKey(StillCaptureParams.KEY_AE_TARGET_FPS_RANGE)
        )
    }

    @Test
    fun stillParams_neverContainsFpsRangeLock_regardless_of_fpsPreset() {
        for (preset in FpsPreset.values()) {
            val params = StillCaptureParams.build(CameraSettings(fpsPreset = preset))
            assertFalse(
                "FPS range lock MUST NOT appear in still capture params for preset $preset",
                params.containsKey(StillCaptureParams.KEY_AE_TARGET_FPS_RANGE)
            )
        }
    }

    // =========================================================================
    // VideoCaptureParams — the video pipeline must have FPS lock,
    //                      must NOT have ISP high-quality modes (uses FAST).
    // =========================================================================

    @Test
    fun videoParams_containsFpsRangeLock_30fps() {
        val params = VideoCaptureParams.build(CameraSettings(fpsPreset = FpsPreset.FPS_30))
        val range = params[VideoCaptureParams.KEY_AE_TARGET_FPS_RANGE]
        assertNotNull("Video params must contain FPS range", range)
        val fpsRange = range as FpsRange
        assertEquals(30, fpsRange.lower)
        assertEquals(30, fpsRange.upper)
    }

    @Test
    fun videoParams_containsFpsRangeLock_60fps() {
        val params = VideoCaptureParams.build(CameraSettings(fpsPreset = FpsPreset.FPS_60))
        val range = params[VideoCaptureParams.KEY_AE_TARGET_FPS_RANGE]
        assertNotNull("Video params must contain FPS range", range)
        val fpsRange = range as FpsRange
        assertEquals(60, fpsRange.lower)
        assertEquals(60, fpsRange.upper)
    }

    @Test
    fun videoParams_neverContainsEdgeModeHighQuality() {
        val params = VideoCaptureParams.build(CameraSettings())
        assertFalse(
            "Video must not contain edge mode key (uses HAL default FAST)",
            params.containsKey(StillCaptureParams.KEY_EDGE_MODE)
        )
    }

    @Test
    fun videoParams_neverContainsNoiseReductionHighQuality() {
        val params = VideoCaptureParams.build(CameraSettings())
        assertFalse(
            "Video must not contain noise reduction key",
            params.containsKey(StillCaptureParams.KEY_NOISE_REDUCTION_MODE)
        )
    }

    @Test
    fun videoParams_neverContainsJpegQuality() {
        val params = VideoCaptureParams.build(CameraSettings())
        assertFalse(
            "Video must not contain JPEG quality",
            params.containsKey(StillCaptureParams.KEY_JPEG_QUALITY)
        )
    }

    @Test
    fun videoParams_containsStabilizationSettings_standard() {
        val params = VideoCaptureParams.build(
            CameraSettings(videoStabilization = VideoStabilizationMode.STANDARD)
        )
        assertEquals(
            VideoCaptureParams.VIDEO_STABILIZATION_MODE_OFF,
            params[VideoCaptureParams.KEY_VIDEO_STABILIZATION_MODE]
        )
        assertEquals(
            VideoCaptureParams.OIS_MODE_ON,
            params[VideoCaptureParams.KEY_LENS_OIS_MODE]
        )
    }

    @Test
    fun videoParams_containsStabilizationSettings_enhanced() {
        val params = VideoCaptureParams.build(
            CameraSettings(videoStabilization = VideoStabilizationMode.ENHANCED)
        )
        assertEquals(
            VideoCaptureParams.VIDEO_STABILIZATION_MODE_ON,
            params[VideoCaptureParams.KEY_VIDEO_STABILIZATION_MODE]
        )
        assertEquals(
            VideoCaptureParams.OIS_MODE_ON,
            params[VideoCaptureParams.KEY_LENS_OIS_MODE]
        )
    }

    @Test
    fun videoParams_containsStabilizationSettings_off() {
        val params = VideoCaptureParams.build(
            CameraSettings(videoStabilization = VideoStabilizationMode.OFF)
        )
        assertEquals(
            VideoCaptureParams.VIDEO_STABILIZATION_MODE_OFF,
            params[VideoCaptureParams.KEY_VIDEO_STABILIZATION_MODE]
        )
        assertEquals(
            VideoCaptureParams.OIS_MODE_OFF,
            params[VideoCaptureParams.KEY_LENS_OIS_MODE]
        )
    }

    // =========================================================================
    // Cross-pipeline isolation invariant
    // =========================================================================

    @Test
    fun stillAndVideo_noSharedMutableKeys() {
        val still = StillCaptureParams.build(CameraSettings())
        val video = VideoCaptureParams.build(CameraSettings())

        // FPS range must only be in video, never in still
        assertFalse(still.containsKey(StillCaptureParams.KEY_AE_TARGET_FPS_RANGE))
        assertTrue(video.containsKey(VideoCaptureParams.KEY_AE_TARGET_FPS_RANGE))

        // JPEG quality must only be in still, never in video
        assertTrue(still.containsKey(StillCaptureParams.KEY_JPEG_QUALITY))
        assertFalse(video.containsKey(StillCaptureParams.KEY_JPEG_QUALITY))
    }
}
