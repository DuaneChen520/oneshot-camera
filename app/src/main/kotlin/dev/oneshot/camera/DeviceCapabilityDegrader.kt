package dev.oneshot.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import android.util.Range
import dev.oneshot.data.AspectRatioMode
import dev.oneshot.data.CameraSettings
import dev.oneshot.data.DynamicRangeMode
import dev.oneshot.data.FpsPreset
import dev.oneshot.data.LensFacingMode
import dev.oneshot.data.QualityPreset

data class DeviceCapabilities(
    val isDcgSupported: Boolean = true,
    val isHlg10Supported: Boolean = true,
    val isUltraHdrSupported: Boolean = true,
    val isOpenGateMaxSupported: Boolean = true,
    val maxSupportedFps: Int = 120,
    val supportedFpsPresets: List<FpsPreset> = FpsPreset.values().toList(),
    val supportedQualities: List<QualityPreset> = QualityPreset.values().toList(),
    val supportedAspectRatios: List<AspectRatioMode> = AspectRatioMode.values().toList(),
)

/**
 * Intelligent hardware capability detection and graceful degradation engine.
 * Inspects device Camera HAL limits and smoothly degrades settings/UI options
 * on mid-range or non-Qualcomm devices to prevent crashes and ensure compatibility.
 */
class DeviceCapabilityDegrader(private val context: Context? = null) {

    /**
     * Resolves device hardware limits for the selected lens facing.
     */
    fun detectCapabilities(lensFacing: LensFacingMode = LensFacingMode.BACK): DeviceCapabilities {
        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return fallbackDefaultCapabilities()

        val targetFacing = when (lensFacing) {
            LensFacingMode.BACK -> CameraCharacteristics.LENS_FACING_BACK
            LensFacingMode.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
        }

        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull()
            chars?.get(CameraCharacteristics.LENS_FACING) == targetFacing
        } ?: cameraManager.cameraIdList.firstOrNull() ?: return fallbackDefaultCapabilities()

        val characteristics = runCatching { cameraManager.getCameraCharacteristics(cameraId) }.getOrNull()
            ?: return fallbackDefaultCapabilities()

        // 1. Qualcomm DCG HDR Hardware vendor tag (0x8003) check
        val isDcgSupported = characteristics.keys.any { key ->
            key.name.contains("EnableHDRDCGMode", ignoreCase = true) ||
            key.name.contains("org.codeaurora.qcamera3", ignoreCase = true)
        }

        // 2. 10-bit HLG10 HDR Profile check (API 33+)
        val isHlg10Supported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val profiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
            profiles != null && profiles.supportedProfiles.contains(DynamicRangeProfiles.HLG10)
        } else {
            false
        }

        // 3. UltraHDR JPEG_R check (API 34+)
        val isUltraHdrSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        // 4. Framerate ranges
        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()
        val maxFps = fpsRanges.maxOfOrNull { it.upper } ?: 30
        val supportedFps = FpsPreset.values().filter { it.targetFps <= maxFps }

        // 5. OpenGate 4:3 4096 max resolution check
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = streamMap?.getOutputSizes(android.graphics.ImageFormat.YUV_420_888) ?: emptyArray()
        val isOpenGateMax = supportedSizes.any { it.width >= 4000 && it.height >= 3000 }

        val supportedQualities = QualityPreset.values().filter { preset ->
            if (preset == QualityPreset.OPENGATE_MAX) isOpenGateMax else true
        }

        return DeviceCapabilities(
            isDcgSupported = isDcgSupported,
            isHlg10Supported = isHlg10Supported,
            isUltraHdrSupported = isUltraHdrSupported,
            isOpenGateMaxSupported = isOpenGateMax,
            maxSupportedFps = maxFps,
            supportedFpsPresets = if (supportedFps.isNotEmpty()) supportedFps else listOf(FpsPreset.FPS_30),
            supportedQualities = if (supportedQualities.isNotEmpty()) supportedQualities else listOf(QualityPreset.FHD),
            supportedAspectRatios = AspectRatioMode.values().toList(),
        )
    }

    /**
     * Sanitizes user camera settings by downgrading any setting that exceeds hardware limits.
     * @return Sanitized [CameraSettings] guaranteed to be compatible with this device.
     */
    fun sanitizeSettings(settings: CameraSettings, capabilities: DeviceCapabilities): CameraSettings {
        var result = settings

        // Fallback DCG if unsupported
        if (result.dcgHdrEnabled && !capabilities.isDcgSupported) {
            result = result.copy(dcgHdrEnabled = false)
        }

        // Fallback HLG10 if unsupported
        if (result.dynamicRange == DynamicRangeMode.HLG10 && !capabilities.isHlg10Supported) {
            result = result.copy(dynamicRange = DynamicRangeMode.SDR)
        }

        // Fallback UltraHDR if unsupported
        if (result.ultraHdrEnabled && !capabilities.isUltraHdrSupported) {
            result = result.copy(ultraHdrEnabled = false)
        }

        // Fallback Quality if OpenGate 4096 unsupported
        if (result.quality == QualityPreset.OPENGATE_MAX && !capabilities.isOpenGateMaxSupported) {
            result = result.copy(quality = QualityPreset.UHD)
        }

        // Fallback FPS if preset exceeds device limit
        if (result.fpsPreset.targetFps > capabilities.maxSupportedFps) {
            val bestFps = capabilities.supportedFpsPresets.maxByOrNull { it.targetFps } ?: FpsPreset.FPS_30
            result = result.copy(fpsPreset = bestFps)
        }

        return result
    }

    private fun fallbackDefaultCapabilities(): DeviceCapabilities {
        return DeviceCapabilities(
            isDcgSupported = false,
            isHlg10Supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            isUltraHdrSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            isOpenGateMaxSupported = false,
            maxSupportedFps = 60,
            supportedFpsPresets = listOf(FpsPreset.FPS_24, FpsPreset.FPS_30, FpsPreset.FPS_60),
            supportedQualities = listOf(QualityPreset.FHD, QualityPreset.QHD, QualityPreset.UHD),
            supportedAspectRatios = AspectRatioMode.values().toList(),
        )
    }
}
