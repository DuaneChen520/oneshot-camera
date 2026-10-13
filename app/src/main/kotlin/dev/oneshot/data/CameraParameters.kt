package dev.oneshot.data

import androidx.camera.core.AspectRatio
import androidx.camera.video.Quality

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

enum class MotionDurationPreset(val label: String, val seconds: Float) {
    DUR_1_5S("1.5 秒抓拍", 1.5f),
    DUR_3_0S("3.0 秒完整", 3.0f),
}

enum class FpsPreset(val label: String, val targetFps: Int) {
    FPS_24("24 fps 电影", 24),
    FPS_30("30 fps 标准", 30),
    FPS_60("60 fps 平滑", 60),
    FPS_120("120 fps 升格", 120),
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
    val motionDuration: MotionDurationPreset = MotionDurationPreset.DUR_1_5S,
    val volumeKeyUpAction: VolumeKeyAction = VolumeKeyAction.STILL_SHUTTER,
    val volumeKeyDownAction: VolumeKeyAction = VolumeKeyAction.VIDEO_SHUTTER,
    val isAutoShutter: Boolean = true,
    val shutterSpeedNanos: Long = 1_000_000_000L / 60L, // 1/60s default
    val isAutoIso: Boolean = true,
    val isoValue: Int = 100,
    val fpsPreset: FpsPreset = FpsPreset.FPS_30,
    val colorSpace: ColorSpacePreset = ColorSpacePreset.REC_2020,
    val lutEnabled: Boolean = false,
    val lutPreset: LutPreset = LutPreset.TEAL_AND_ORANGE,
    val lutBurnIn: Boolean = false, // false = preview only, true = burn-in to recorded file
    val focusPeakingEnabled: Boolean = false,
    val zebraPatternEnabled: Boolean = false,
)
