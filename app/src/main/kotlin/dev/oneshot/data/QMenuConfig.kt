package dev.oneshot.data

import kotlinx.serialization.Serializable

@Serializable
enum class QSlotItem(
    val label: String,
    val icon: String,
    val description: String,
) {
    ASPECT_RATIO("画幅比例", "📐", "切换 4:3 与 16:9 全开门"),
    DYNAMIC_RANGE("高动态范围", "⚡", "切换 10-bit HLG 与 SDR"),
    FPS_PRESET("录制帧率", "⏱️", "切换 24/30/60/120 电影帧率"),
    FILM_STOCK("胶卷色彩", "🎞️", "切换 3D LUT 胶卷色彩配方"),
    DCG_HDR("高通 DCG", "💎", "开关 Dual Conversion Gain 硬件级 HDR"),
    PHOTO_FORMAT("照片格式", "📷", "切换 JPEG 与 HEIF 高效格式"),
    MOTION_DURATION("动态照片", "🎬", "切换 1.5s / 3.0s 动态抓拍时长"),
    FOCUS_PEAKING("峰值对焦", "🎯", "开关取景器高反差边缘荧光辅助"),
    ZEBRA_PATTERN("斑马纹", "🦓", "开关过曝高光对角条纹告警"),
    VIDEO_CODEC("视频编码", "📼", "切换 HEVC 与 H.264 编码"),
    HISTOGRAM("示波器", "📊", "切换关闭 / 亮度直方图 / RGB 示波器"),
    LUT_BURN_IN("烧录模式", "🔥", "切换直出写盘与屏幕监看模式"),
    QUALITY("视频画质", "📺", "切换 1080p / 2K / 4K / 4096"),
    COLOR_SPACE("色彩空间", "🎨", "切换 sRGB / Display P3 / Rec.2020"),
    ULTRA_HDR("UltraHDR", "✨", "开关 Android 14+ UltraHDR 静态高光"),
}

object QMenuDefaults {
    val DEFAULT_SLOTS: List<QSlotItem> = listOf(
        QSlotItem.ASPECT_RATIO,
        QSlotItem.DYNAMIC_RANGE,
        QSlotItem.FPS_PRESET,
        QSlotItem.FILM_STOCK,
        QSlotItem.DCG_HDR,
        QSlotItem.FOCUS_PEAKING,
    )
    const val MAX_SLOTS = 6
}
