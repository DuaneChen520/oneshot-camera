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
        assertEquals(0, settings.videoBitrateMbps)
        assertEquals(VideoCodec.HEVC, settings.videoCodec)
        assertEquals(PhotoFormat.JPEG, settings.photoFormat)
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
        assertEquals(5, settings.hudPresets.shutterPresets.size)
        assertEquals(5, settings.hudPresets.isoPresets.size)
        assertEquals(5, settings.hudPresets.evPresets.size)
        assertEquals(5, settings.hudPresets.zoomPresets.size)
        assertEquals(5, settings.hudPresets.focusPresets.size)
        assertEquals(5, settings.hudPresets.wbPresets.size)
    }

    @Test
    fun testVideoBitrateSettingsAndHelper() {
        val settings = CameraSettings()
        assertEquals(0, settings.videoBitrateMbps)

        assertTrue(VideoBitrateHelper.KEY_ANCHORS.contains(12))
        assertTrue(VideoBitrateHelper.KEY_ANCHORS.contains(30))
        assertTrue(VideoBitrateHelper.KEY_ANCHORS.contains(60))
        assertTrue(VideoBitrateHelper.KEY_ANCHORS.contains(100))
        assertTrue(VideoBitrateHelper.KEY_ANCHORS.contains(150))
        assertTrue(VideoBitrateHelper.KEY_ANCHORS.contains(200))

        assertEquals("AUTO (自适应)", VideoBitrateHelper.getLabel(0))
        assertEquals("12 Mbps (轻量网络)", VideoBitrateHelper.getLabel(12))
        assertEquals("100 Mbps (电影级 Pro)", VideoBitrateHelper.getLabel(100))
        assertEquals("45 Mbps (自定义)", VideoBitrateHelper.getLabel(45))

        assertTrue(VideoBitrateHelper.estimateStoragePerMinute(0).contains("250~350"))
        assertEquals("约 90 MB/分", VideoBitrateHelper.estimateStoragePerMinute(12))
        assertEquals("约 750 MB/分", VideoBitrateHelper.estimateStoragePerMinute(100))
    }

    @Test
    fun testVideoCodecAndPhotoFormatEnums() {
        assertEquals("video/hevc", VideoCodec.HEVC.mimeType)
        assertEquals("video/avc", VideoCodec.H264.mimeType)
        assertEquals(".jpg", PhotoFormat.JPEG.extension)
        assertEquals(".heic", PhotoFormat.HEIF.extension)
        assertEquals("image/heif", PhotoFormat.HEIF.mimeType)
    }

    @Test
    fun testHudPresetsSerialization() {
        val presets = ProHudPresets()
        val json = kotlinx.serialization.json.Json.encodeToString(ProHudPresets.serializer(), presets)
        val decoded = kotlinx.serialization.json.Json.decodeFromString(ProHudPresets.serializer(), json)
        assertEquals(presets, decoded)
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
