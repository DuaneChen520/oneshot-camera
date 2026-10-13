package dev.oneshot.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * OneShot Film — 品牌视觉与设计规范 Tokens (BRAND.md 规范)
 *
 * “一次快门，一次成像。”
 * 把摄影还给光学，把色彩留给快门。
 */
object OneShotTheme {
    // 品牌核心调色板 (BRAND.md 6.1)
    val ObsidianBlack = Color(0xFF121212)        // 曜石黑: 底片黑 / 界面底色 / 纯粹取景
    val AmberGrain = Color(0xFFF5A623)           // 底片琥珀黄: 柯达琥珀黄 / 核心快门 / 强调色
    val AmberGrainMuted = Color(0x33F5A623)      // 琥珀黄半透明背景
    val AmberGrainGlow = Color(0x66F5A623)       // 琥珀黄光晕
    val DarkroomRed = Color(0xFFE03E3E)          // 暗房红: 传统暗房安全灯 / 电影机 Tally 录制红灯
    val DarkroomRedMuted = Color(0x33E03E3E)     // 暗房红半透明背景
    val TitaniumGray = Color(0xFF8E8E93)         // 钛金冷灰: 工业钛灰 / 辅助刻度 / 专业标尺
    val TitaniumBorder = Color(0x338E8E93)       // 钛金冷灰极细边框
    val ChassisDark = Color(0xF518181A)          // 机械机身暗底衬
    val ChassisCard = Color(0x552C2C2E)          // 机械卡片微背景
    val FilmSilver = Color(0xFFF2F2F7)           // 胶卷纸白 / 机械银白
    val AutoSkyBlue = Color(0xFF64B5F6)          // AUTO 自动参数标识天蓝
    val HardwareGreen = Color(0xFF00E676)        // 硬件高亮指示绿 (DCG HDR 等)

    // 品牌文字标识规范 (BRAND.md 3.1 & 3.2)
    const val BRAND_NAME = "ONESHOT FILM"
    const val SLOGAN_MAIN = "一次快门，一次成像"
    const val SLOGAN_EN = "One Shutter. The Decisive Frame."
    const val SLOGAN_SUB = "把摄影还给光学，把色彩留给快门。"

    // 字体排版风格
    val MonospaceFont = FontFamily.Monospace
}
