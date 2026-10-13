package dev.oneshot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSettingsTest {

    @Test
    fun testDefaultCameraSettings() {
        val settings = CameraSettings()
        assertEquals(AspectRatioMode.RATIO_4_3, settings.aspectMode)
        assertEquals(DynamicRangeMode.HLG10, settings.dynamicRange)
        assertEquals(QualityPreset.FHD, settings.quality)
        assertEquals(LensFacingMode.BACK, settings.lensFacing)
        assertTrue(settings.dcgHdrEnabled)
        assertEquals(0, settings.evIndex)
        assertTrue(settings.isAutoWb)
        assertEquals(5500, settings.cctKelvin)
        assertEquals(1.0f, settings.zoomRatio)
        assertTrue(settings.ultraHdrEnabled)
        assertEquals(MotionDurationPreset.DUR_1_5S, settings.motionDuration)
        assertEquals(VolumeKeyAction.STILL_SHUTTER, settings.volumeKeyUpAction)
        assertEquals(VolumeKeyAction.VIDEO_SHUTTER, settings.volumeKeyDownAction)
        assertTrue(settings.isAutoShutter)
        assertTrue(settings.isAutoIso)
        assertEquals(FpsPreset.FPS_30, settings.fpsPreset)
        assertEquals(ColorSpacePreset.REC_2020, settings.colorSpace)
        assertFalse(settings.lutEnabled)
        assertEquals(LutPreset.TEAL_AND_ORANGE, settings.lutPreset)
        assertEquals(null, settings.selectedCustomLutId)
        assertTrue(settings.customLuts.isEmpty())
        assertFalse(settings.lutBurnIn)
        assertTrue(settings.isAutoFocus)
        assertEquals(0.0f, settings.focusDistance, 0.001f)
        assertFalse(settings.focusPeakingEnabled)
        assertFalse(settings.zebraPatternEnabled)
        assertEquals(HistogramMode.OFF, settings.histogramMode)
    }

    @Test
    fun testCustomLutSerialization() {
        val original = CustomLut(
            id = "lut_12345",
            name = "Fujifilm Superia 400",
            fileName = "lut_12345.cube",
            size = 33,
            importedAtMs = 1718000000000L
        )
        val json = kotlinx.serialization.json.Json.encodeToString(CustomLut.serializer(), original)
        val decoded = kotlinx.serialization.json.Json.decodeFromString(CustomLut.serializer(), json)
        assertEquals(original, decoded)
    }

    @Test
    fun testAspectRatioValues() {
        assertEquals(4f, AspectRatioMode.RATIO_4_3.widthRatio)
        assertEquals(3f, AspectRatioMode.RATIO_4_3.heightRatio)
        assertEquals(16f, AspectRatioMode.RATIO_16_9.widthRatio)
        assertEquals(9f, AspectRatioMode.RATIO_16_9.heightRatio)
    }

    @Test
    fun testMotionDurationPresets() {
        assertEquals(1.5f, MotionDurationPreset.DUR_1_5S.seconds)
        assertEquals(3.0f, MotionDurationPreset.DUR_3_0S.seconds)
    }

    @Test
    fun testFpsPresets() {
        assertEquals(24, FpsPreset.FPS_24.targetFps)
        assertEquals(30, FpsPreset.FPS_30.targetFps)
        assertEquals(60, FpsPreset.FPS_60.targetFps)
        assertEquals(120, FpsPreset.FPS_120.targetFps)
    }
}
