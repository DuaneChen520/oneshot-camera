package dev.oneshot.data

import androidx.camera.core.AspectRatio
import androidx.camera.video.Quality
import kotlinx.serialization.Serializable

enum class AspectRatioMode(val label: String, val cameraXRatio: Int, val widthRatio: Float, val heightRatio: Float) {
    RATIO_4_3("4:3 全画幅", AspectRatio.RATIO_4_3, 4f, 3f),
    RATIO_16_9("16:9 标准", AspectRatio.RATIO_16_9, 16f, 9f),
}

enum class DynamicRangeMode(val label: String) {
    SDR("SDR 8-bit"),
    HLG10("HLG 10-bit HDR"),
}

enum class QualityPreset(val label: String, val cameraXQuality: Quality?) {
    FHD("1080p", Quality.FHD),
    QHD("2K 超清", Quality.QHD),
    UHD("4K 满血", Quality.UHD),
    OPENGATE_MAX("4096 极限全开门", null),
}

enum class LensFacingMode(val label: String) {
    BACK("后置主摄"),
    FRONT("前置自拍"),
}

enum class VolumeKeyAction(val label: String) {
    STILL_SHUTTER("拍照快门"),
    VIDEO_SHUTTER("录像快门"),
    MOTION_PHOTO("动态照片快门"),
    ZOOM_IN("镜头放大"),
    ZOOM_OUT("镜头缩小"),
    NONE("无操作"),
}

enum class MotionDurationPreset(
    val label: String,
    val seconds: Float,
    val preRollSeconds: Float,
    val postRollSeconds: Float,
) {
    DUR_1_5S("1.5 秒抓拍", 1.5f, 0.8f, 0.7f),
    DUR_3_0S("3.0 秒完整", 3.0f, 1.5f, 1.5f),
    DUR_5_0S("5.0 秒长景", 5.0f, 2.5f, 2.5f),
}

enum class FpsPreset(val label: String, val targetFps: Int) {
    FPS_24("24 fps 电影", 24),
    FPS_30("30 fps 标准", 30),
    FPS_60("60 fps 平滑", 60),
    FPS_120("120 fps 升格", 120),
}

enum class VideoCodec(val label: String, val mimeType: String) {
    HEVC("HEVC (H.265 高效)", "video/hevc"),
    H264("H.264 (AVC 兼容)", "video/avc"),
}

enum class PhotoFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPEG 标准", ".jpg", "image/jpeg"),
    HEIF("HEIF 高效 (HEIC)", ".heic", "image/heif"),
}

enum class VideoStabilizationMode(val label: String, val shortLabel: String, val description: String) {
    STANDARD("标准 (纯光学 OIS)", "OIS", "100% 全画幅零裁切，无数字畸变"),
    ENHANCED("增强 (光学+电子)", "OIS+EIS", "防抖算法介入，裁切约 10% 画面"),
    OFF("关闭 (三脚架/云台)", "OFF", "消除马达拉扯与共振，光路纯净"),
}

enum class ColorSpacePreset(val label: String) {
    SRGB("sRGB (Rec.709)"),
    DISPLAY_P3("Display P3 广色域"),
    REC_2020("Rec.2020 影视级"),
}

enum class LutPreset(val label: String) {
    TEAL_AND_ORANGE("好莱坞青橙 (Teal & Orange)"),
    KODAK_PORTRA("柯达胶片 (Kodak Portra)"),
    FLAT_LOG("平坦 Log 灰片 (Flat Log)"),
}

enum class HistogramMode(val label: String) {
    OFF("关"),
    LUMA("亮度"),
    RGB("RGB"),
}

@Serializable
data class CustomLut(
    val id: String,
    val name: String,
    val fileName: String,
    val size: Int = 17,
    val importedAtMs: Long = 0L,
)

data class CameraSettings(
    val aspectMode: AspectRatioMode = AspectRatioMode.RATIO_4_3,
    val dynamicRange: DynamicRangeMode = DynamicRangeMode.HLG10,
    val quality: QualityPreset = QualityPreset.FHD,
    val lensFacing: LensFacingMode = LensFacingMode.BACK,
    val dcgHdrEnabled: Boolean = true,
    val evIndex: Int = 0,
    val isAutoWb: Boolean = true,
    val cctKelvin: Int = 5500,
    val tintGm: Int = 0, // Green (-100) to Magenta (+100)
    val zoomRatio: Float = 1.0f,
    val ultraHdrEnabled: Boolean = true,
    val motionDuration: MotionDurationPreset = MotionDurationPreset.DUR_3_0S,
    val volumeKeyUpAction: VolumeKeyAction = VolumeKeyAction.STILL_SHUTTER,
    val volumeKeyDownAction: VolumeKeyAction = VolumeKeyAction.VIDEO_SHUTTER,
    val isAutoShutter: Boolean = true,
    val shutterSpeedNanos: Long = 1_000_000_000L / 60L, // 1/60s default
    val isAutoIso: Boolean = true,
    val isoValue: Int = 100,
    val fpsPreset: FpsPreset = FpsPreset.FPS_30,
    val videoBitrateMbps: Int = 0, // 0 = AUTO (System/CameraX default), >0 = Target Mbps (10..200)
    val videoCodec: VideoCodec = VideoCodec.HEVC,
    val photoFormat: PhotoFormat = PhotoFormat.JPEG,
    val colorSpace: ColorSpacePreset = ColorSpacePreset.REC_2020,
    val lutEnabled: Boolean = false,
    val lutPreset: LutPreset = LutPreset.TEAL_AND_ORANGE,
    val selectedCustomLutId: String? = null,
    val customLuts: List<CustomLut> = emptyList(),
    val lutBurnIn: Boolean = false, // false = preview only, true = burn-in to recorded file
    val isAutoFocus: Boolean = true,
    val focusDistance: Float = 0.0f, // Diopters: 0.0f (infinity) to 10.0f (macro ~10cm)
    val focusPeakingEnabled: Boolean = false,
    val zebraPatternEnabled: Boolean = false,
    val histogramMode: HistogramMode = HistogramMode.OFF,
    val videoStabilization: VideoStabilizationMode = VideoStabilizationMode.STANDARD,
    val screenDimmerEnabled: Boolean = true,
    val hudPresets: ProHudPresets = ProHudPresets(),
    val qMenuSlots: List<QSlotItem> = QMenuDefaults.DEFAULT_SLOTS,
)

object VideoBitrateHelper {
    val KEY_ANCHORS = listOf(12, 30, 60, 100, 150, 200)

    fun getLabel(mbps: Int): String = when (mbps) {
        0 -> "AUTO (自适应)"
        12 -> "12 Mbps (轻量网络)"
        30 -> "30 Mbps (日常标清)"
        60 -> "60 Mbps (4K 标准)"
        100 -> "100 Mbps (电影级 Pro)"
        150 -> "150 Mbps (4:3 全开)"
        200 -> "200 Mbps (满血母带)"
        else -> "$mbps Mbps (自定义)"
    }

    fun estimateStoragePerMinute(mbps: Int): String {
        if (mbps <= 0) return "约 250~350 MB/分"
        val mbPerMin = (mbps.toDouble() * 60.0) / 8.0
        return if (mbPerMin >= 1024.0) {
            "约 %.1f GB/分".format(java.util.Locale.US, mbPerMin / 1024.0)
        } else {
            "约 ${mbPerMin.toInt()} MB/分"
        }
    }
}

@Serializable
data class ProHudPresets(
    val shutterPresets: List<Long> = listOf(
        1_000_000_000L / 1000L,
        1_000_000_000L / 500L,
        1_000_000_000L / 250L,
        1_000_000_000L / 125L,
        1_000_000_000L / 60L
    ),
    val isoPresets: List<Int> = listOf(100, 200, 400, 800, 1600),
    val evPresets: List<Int> = listOf(-2, -1, 1, 2, 4), // indices corresponding to EV values
    val zoomPresets: List<Float> = listOf(0.7f, 1.0f, 2.0f, 5.0f, 10.0f),
    val focusPresets: List<Float> = listOf(0.0f, 0.33f, 1.0f, 2.0f, 10.0f), // Diopters (∞, 3m, 1m, 0.5m, 0.1m)
    val wbPresets: List<Int> = listOf(3200, 4000, 5500, 6500, 7500),
)
