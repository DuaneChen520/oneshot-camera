package dev.oneshot.camera

import android.content.Context
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.QualityPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DeviceCapabilityDegraderTest {

    @Test
    fun testFlagshipProfileDoesNotDegradeSettings() {
        val degrader = DeviceCapabilityDegrader()
        val flagshipCaps = DeviceCapabilities(
            isDcgSupported = true,
            isHlg10Supported = true,
            isUltraHdrSupported = true,
            isOpenGateMaxSupported = true,
            maxSupportedFps = 120,
            supportedFpsPresets = FpsPreset.values().toList(),
            supportedQualities = QualityPreset.values().toList(),
        )

        val fullSettings = CameraSettings(
            dynamicRange = DynamicRangeMode.HLG10,
            dcgHdrEnabled = true,
            ultraHdrEnabled = true,
            quality = QualityPreset.OPENGATE_MAX,
            fpsPreset = FpsPreset.FPS_120
        )

        val sanitized = degrader.sanitizeSettings(fullSettings, flagshipCaps)
        assertEquals(DynamicRangeMode.HLG10, sanitized.dynamicRange)
        assertTrue(sanitized.dcgHdrEnabled)
        assertTrue(sanitized.ultraHdrEnabled)
        assertEquals(QualityPreset.OPENGATE_MAX, sanitized.quality)
        assertEquals(FpsPreset.FPS_120, sanitized.fpsPreset)
    }

    @Test
    fun testMidrangeProfileGracefullyDegradesSettings() {
        val degrader = DeviceCapabilityDegrader()
        val midRangeCaps = DeviceCapabilities(
            isDcgSupported = false,
            isHlg10Supported = false,
            isUltraHdrSupported = false,
            isOpenGateMaxSupported = false,
            maxSupportedFps = 30,
            supportedFpsPresets = listOf(FpsPreset.FPS_24, FpsPreset.FPS_30),
            supportedQualities = listOf(QualityPreset.FHD, QualityPreset.QHD, QualityPreset.UHD),
        )

        val ambitiousSettings = CameraSettings(
            dynamicRange = DynamicRangeMode.HLG10,
            dcgHdrEnabled = true,
            ultraHdrEnabled = true,
            quality = QualityPreset.OPENGATE_MAX,
            fpsPreset = FpsPreset.FPS_120
        )

        val sanitized = degrader.sanitizeSettings(ambitiousSettings, midRangeCaps)

        // 1. DCG HDR degraded to false
        assertFalse("DCG should be disabled on non-Qualcomm hardware", sanitized.dcgHdrEnabled)

        // 2. Dynamic range degraded to SDR
        assertEquals("Dynamic range should fall back to SDR", DynamicRangeMode.SDR, sanitized.dynamicRange)

        // 3. UltraHDR degraded to false
        assertFalse("UltraHDR should fall back to false", sanitized.ultraHdrEnabled)

        // 4. OpenGate 4096 degraded to UHD 4K
        assertEquals("Quality should fall back to UHD", QualityPreset.UHD, sanitized.quality)

        // 5. FPS 120 degraded to max supported 30fps
        assertEquals("FPS should fall back to device maximum", FpsPreset.FPS_30, sanitized.fpsPreset)
    }
}
