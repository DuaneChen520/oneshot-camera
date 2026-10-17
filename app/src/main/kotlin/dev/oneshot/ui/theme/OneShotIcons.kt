package dev.oneshot.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.oneshot.R
import dev.oneshot.data.QSlotItem

/**
 * OneShot Film — 胶片电影机专业矢量图标系统 (Cinema Camera Iconography Suite)
 *
 * 遵循 BRAND.md 工业机械与光化学设计规范：
 * - 纯矢量 24x24 工业绘图栅格对齐；
 * - 涵盖光学取景、35mm 胶片感光、旋转叶片快门、10-bit HLG 对数、高通 DCG 双增益及专业监看工具；
 * - 深度贴合 Jetpack Compose 原生矢量渲染体系。
 */
object OneShotIcons {
    // 1. 光学取景与机械卡口 (Optical & Chassis)
    @DrawableRes val AspectRatioOpenGate = R.drawable.ic_aspect_ratio_open_gate
    @DrawableRes val LensMountFlip = R.drawable.ic_lens_mount_flip
    @DrawableRes val FramingReticleCross = R.drawable.ic_framing_reticle_cross

    // 2. 胶片感光与色彩科学 (Film & Emulsion)
    @DrawableRes val FilmCartridge35mm = R.drawable.ic_film_cartridge_35mm
    @DrawableRes val FilmStockLut = R.drawable.ic_film_stock_lut
    @DrawableRes val LutBurnIn = R.drawable.ic_lut_burn_in
    @DrawableRes val ColorSpaceGamut = R.drawable.ic_color_space_gamut

    // 3. 快门与传感器硬件 (Shutter & Sensor)
    @DrawableRes val FpsShutterAngle = R.drawable.ic_fps_shutter_angle
    @DrawableRes val DynamicRangeHlg = R.drawable.ic_dynamic_range_hlg
    @DrawableRes val DcgDualGain = R.drawable.ic_dcg_dual_gain
    @DrawableRes val ShutterMechanical = R.drawable.ic_shutter_mechanical

    // 4. 导演级专业监看辅助 (Monitor & Assist)
    @DrawableRes val FocusPeaking = R.drawable.ic_focus_peaking
    @DrawableRes val ZebraStripes = R.drawable.ic_zebra_stripes
    @DrawableRes val HistogramParade = R.drawable.ic_histogram_parade
    @DrawableRes val ThermalMonitor = R.drawable.ic_thermal_monitor

    // 5. 编码录制与存储介质 (Codec & Storage)
    @DrawableRes val MotionPhotoLive = R.drawable.ic_motion_photo_live
    @DrawableRes val VideoCodecCinema = R.drawable.ic_video_codec_cinema
    @DrawableRes val PhotoFormatRaw = R.drawable.ic_photo_format_raw
    @DrawableRes val GalleryFilmFrame = R.drawable.ic_gallery_film_frame
    @DrawableRes val CinemaReelMagazine = R.drawable.ic_cinema_reel_magazine
    @DrawableRes val VideoQuality4k = R.drawable.ic_video_quality_4k
    @DrawableRes val QMenuMatrix = R.drawable.ic_q_menu_matrix
    @DrawableRes val UltraHdrSpark = R.drawable.ic_ultra_hdr_spark

    /**
     * 将 QMenu 插槽项直接映射为胶片电影机矢量图标资源
     */
    @DrawableRes
    fun getSlotIconRes(item: QSlotItem): Int = when (item) {
        QSlotItem.ASPECT_RATIO -> AspectRatioOpenGate
        QSlotItem.DYNAMIC_RANGE -> DynamicRangeHlg
        QSlotItem.FPS_PRESET -> FpsShutterAngle
        QSlotItem.FILM_STOCK -> FilmStockLut
        QSlotItem.DCG_HDR -> DcgDualGain
        QSlotItem.PHOTO_FORMAT -> PhotoFormatRaw
        QSlotItem.MOTION_DURATION -> MotionPhotoLive
        QSlotItem.FOCUS_PEAKING -> FocusPeaking
        QSlotItem.ZEBRA_PATTERN -> ZebraStripes
        QSlotItem.VIDEO_CODEC -> VideoCodecCinema
        QSlotItem.HISTOGRAM -> HistogramParade
        QSlotItem.LUT_BURN_IN -> LutBurnIn
        QSlotItem.QUALITY -> VideoQuality4k
        QSlotItem.COLOR_SPACE -> ColorSpaceGamut
        QSlotItem.ULTRA_HDR -> UltraHdrSpark
    }
}

/**
 * 胶片电影机统一矢量图标 Compose 组件
 */
@Composable
fun CinemaIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = OneShotTheme.FilmSilver,
    size: Dp = 16.dp,
) {
    Icon(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

/**
 * 便捷属性：直接获取 QSlotItem 对应的电影机矢量资源
 */
val QSlotItem.iconRes: Int
    get() = OneShotIcons.getSlotIconRes(this)
