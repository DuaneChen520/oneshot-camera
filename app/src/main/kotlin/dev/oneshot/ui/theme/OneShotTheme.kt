package dev.oneshot.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * OneShot Film — 品牌视觉与设计规范 Tokens (BRAND.md 规范)
 *
 * “一次快门，一次成像。”
 * 把摄影还给光学，把色彩留给快门。
 *
 * 机械机身 · 暗房红光 · 柯达琥珀 · 钛金冷灰 · 电影机 Tally
 */
object OneShotTheme {
    // 品牌核心调色板 (BRAND.md 6.1) - 机械与光化学色彩
    val ObsidianBlack = Color(0xFF0C0C0E)        // 曜石黑: 底片暗房黑 / 纯粹取景底色
    val ChassisDark = Color(0xFF141417)          // 机械机身暗底衬 (阳极氧化铝黑)
    val ChassisMilled = Color(0xFF1C1C20)        // 滚花转盘铣削金属底
    val ChassisCard = Color(0xFF222227)          // 机械卡片微背景 (缎面钛金属)
    val ChassisHighlight = Color(0xFF2C2C33)     // 机械倒角高光反光
    val ChassisBorder = Color(0x388E8E93)        // 钛金冷灰极细发丝纹边框
    val ChassisBorderActive = Color(0x80F5A623)  // 旋钮激活金黄微光边框

    val AmberGrain = Color(0xFFF5A623)           // 底片琥珀黄: 柯达琥珀黄 / 核心快门 / 强调色
    val AmberGrainMuted = Color(0x26F5A623)      // 琥珀黄半透明背景
    val AmberGrainGlow = Color(0x66F5A623)       // 琥珀黄光晕
    val AmberGrainDeep = Color(0xFFD48810)       // 琥珀黄深色刻度

    val DarkroomRed = Color(0xFFE03E3E)          // 暗房红: 传统暗房安全灯 / 电影机 Tally 录制红灯
    val DarkroomRedMuted = Color(0x26E03E3E)     // 暗房红半透明背景
    val DarkroomRedGlow = Color(0x66E03E3E)      // 暗房红呼吸光晕

    val TitaniumGray = Color(0xFF8E8E93)         // 钛金冷灰: 工业钛灰 / 辅助刻度 / 专业标尺
    val TitaniumDark = Color(0xFF48484A)         // 深钛灰: 未激活转盘凹槽刻线
    val TitaniumBorder = Color(0x338E8E93)       // 钛金冷灰极细边框
    val TitaniumLight = Color(0xFFC7C7CC)        // 浅钛灰: 次级参数激光刻字

    val FilmSilver = Color(0xFFF4F4F6)           // 胶卷纸白 / 机械银白刻字
    val AutoSkyBlue = Color(0xFF64B5F6)          // AUTO 自动参数标识天蓝 (冷调平衡)
    val HardwareGreen = Color(0xFF00E676)        // 硬件高亮指示绿 (DCG HDR / AF锁焦 / 180°开角)
    val Cine800TCyan = Color(0xFF00E5FF)         // 800T 钨丝灯光晕青

    // 品牌文字标识规范 (BRAND.md 3.1 & 3.2)
    const val BRAND_NAME = "ONESHOT FILM"
    const val SLOGAN_MAIN = "一次快门，一次成像"
    const val SLOGAN_EN = "One Shutter. The Decisive Frame."
    const val SLOGAN_SUB = "把摄影还给光学，把色彩留给快门。"

    // 字体排版风格
    val MonospaceFont = FontFamily.Monospace
}

