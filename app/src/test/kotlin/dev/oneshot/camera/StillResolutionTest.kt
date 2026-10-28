package dev.oneshot.camera

import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.QualityPreset
import dev.oneshot.data.StillResolutionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StillResolutionTest {

    @Test
    fun testStillResolutionPresets() {
        val res50 = StillResolutionPreset.RES_50MP
        val res12 = StillResolutionPreset.RES_12MP

        assertEquals("50MP", res50.shortLabel)
        assertEquals("12MP", res12.shortLabel)
        assertTrue(res50.label.contains("50MP"))
        assertTrue(res12.label.contains("12MP"))
        assertEquals("8192×6144", res50.approxMegaPixels)
        assertEquals("4096×3072", res12.approxMegaPixels)
    }

    @Test
    fun testSettingsDefaultStillResolutionIs12MP() {
        val settings = CameraSettings()
        assertEquals(StillResolutionPreset.RES_12MP, settings.stillResolution)
        assertFalse(settings.motionPhotoLiveEnabled)
    }

    @Test
    fun testStillResolutionDecoupledFromVideoQuality() {
        val s1 = CameraSettings(stillResolution = StillResolutionPreset.RES_50MP, quality = QualityPreset.FHD)
        val s2 = CameraSettings(stillResolution = StillResolutionPreset.RES_12MP, quality = QualityPreset.OPENGATE_MAX)

        // Verifies still resolution and video quality vary completely independently
        assertEquals(StillResolutionPreset.RES_50MP, s1.stillResolution)
        assertEquals(QualityPreset.FHD, s1.quality)

        assertEquals(StillResolutionPreset.RES_12MP, s2.stillResolution)
        assertEquals(QualityPreset.OPENGATE_MAX, s2.quality)

        assertNotEquals(s1.stillResolution, s2.stillResolution)
        assertNotEquals(s1.quality, s2.quality)
    }
}
