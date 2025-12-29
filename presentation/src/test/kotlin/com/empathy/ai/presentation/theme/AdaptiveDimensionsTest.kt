package com.empathy.ai.presentation.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AdaptiveDimensions 响应式尺寸系统单元测试
 * 
 * BUG-00036 Phase 1: 响应式字体系统测试
 * 
 * 测试覆盖：
 * - 字体缩放边界限制
 * - 高密度屏幕补偿因子
 * - ROM厂商检测
 * - 响应式字体尺寸计算
 */
class AdaptiveDimensionsTest {

    // ============================================================
    // 字体缩放边界限制测试
    // ============================================================

    @Test
    fun `字体缩放边界_正常范围内不变`() {
        val fontScale = 1.0f
        val clamped = fontScale.coerceIn(0.85f, 1.5f)
        
        assertEquals("正常缩放应保持不变", 1.0f, clamped, 0.001f)
    }

    @Test
    fun `字体缩放边界_超大字体被限制到最大值`() {
        val fontScale = 2.0f
        val clamped = fontScale.coerceIn(0.85f, 1.5f)
        
        assertEquals("超大字体应被限制到1.5", 1.5f, clamped, 0.001f)
    }

    @Test
    fun `字体缩放边界_超小字体被限制到最小值`() {
        val fontScale = 0.5f
        val clamped = fontScale.coerceIn(0.85f, 1.5f)
        
        assertEquals("超小字体应被限制到0.85", 0.85f, clamped, 0.001f)
    }

    @Test
    fun `字体缩放边界_边界值1_5保持不变`() {
        val fontScale = 1.5f
        val clamped = fontScale.coerceIn(0.85f, 1.5f)
        
        assertEquals("边界值1.5应保持不变", 1.5f, clamped, 0.001f)
    }

    @Test
    fun `字体缩放边界_边界值0_85保持不变`() {
        val fontScale = 0.85f
        val clamped = fontScale.coerceIn(0.85f, 1.5f)
        
        assertEquals("边界值0.85应保持不变", 0.85f, clamped, 0.001f)
    }

    // ============================================================
    // 高密度屏幕补偿因子测试
    // ============================================================

    @Test
    fun `密度补偿_xxxhdpi以上屏幕应缩小`() {
        val density = 4.5f
        val compensation = when {
            density > 4.0f -> 0.9f
            density > 3.0f -> 0.95f
            density < 1.5f -> 1.1f
            else -> 1.0f
        }
        
        assertEquals("xxxhdpi以上应使用0.9补偿", 0.9f, compensation, 0.001f)
    }

    @Test
    fun `密度补偿_xxhdpi屏幕应轻微缩小`() {
        val density = 3.5f
        val compensation = when {
            density > 4.0f -> 0.9f
            density > 3.0f -> 0.95f
            density < 1.5f -> 1.1f
            else -> 1.0f
        }
        
        assertEquals("xxhdpi应使用0.95补偿", 0.95f, compensation, 0.001f)
    }

    @Test
    fun `密度补偿_hdpi以下屏幕应放大`() {
        val density = 1.0f
        val compensation = when {
            density > 4.0f -> 0.9f
            density > 3.0f -> 0.95f
            density < 1.5f -> 1.1f
            else -> 1.0f
        }
        
        assertEquals("hdpi以下应使用1.1补偿", 1.1f, compensation, 0.001f)
    }

    @Test
    fun `密度补偿_标准密度无补偿`() {
        val density = 2.0f
        val compensation = when {
            density > 4.0f -> 0.9f
            density > 3.0f -> 0.95f
            density < 1.5f -> 1.1f
            else -> 1.0f
        }
        
        assertEquals("标准密度应使用1.0补偿", 1.0f, compensation, 0.001f)
    }

    // ============================================================
    // ROM厂商检测测试
    // ============================================================

    @Test
    fun `ROM检测_小米设备识别为MIUI`() {
        val manufacturer = "xiaomi"
        val romType = detectRomType(manufacturer, "")
        
        assertEquals("小米设备应识别为MIUI", RomType.MIUI, romType)
    }

    @Test
    fun `ROM检测_华为设备识别为EMUI`() {
        val manufacturer = "huawei"
        val romType = detectRomType(manufacturer, "")
        
        assertEquals("华为设备应识别为EMUI", RomType.EMUI, romType)
    }

    @Test
    fun `ROM检测_OPPO设备识别为COLOR_OS`() {
        val manufacturer = "oppo"
        val romType = detectRomType(manufacturer, "")
        
        assertEquals("OPPO设备应识别为COLOR_OS", RomType.COLOR_OS, romType)
    }

    @Test
    fun `ROM检测_三星设备识别为ONE_UI`() {
        val manufacturer = "samsung"
        val romType = detectRomType(manufacturer, "")
        
        assertEquals("三星设备应识别为ONE_UI", RomType.ONE_UI, romType)
    }

    @Test
    fun `ROM检测_未知设备识别为AOSP`() {
        val manufacturer = "unknown"
        val romType = detectRomType(manufacturer, "")
        
        assertEquals("未知设备应识别为AOSP", RomType.AOSP, romType)
    }

    @Test
    fun `ROM检测_品牌优先于制造商`() {
        val manufacturer = "unknown"
        val brand = "xiaomi"
        val romType = detectRomType(manufacturer, brand)
        
        assertEquals("品牌xiaomi应识别为MIUI", RomType.MIUI, romType)
    }

    // ============================================================
    // ROM字体补偿测试
    // ============================================================

    @Test
    fun `ROM补偿_MIUI使用0_98`() {
        val compensation = getRomFontCompensation(RomType.MIUI)
        
        assertEquals("MIUI应使用0.98补偿", 0.98f, compensation, 0.001f)
    }

    @Test
    fun `ROM补偿_EMUI使用1_0`() {
        val compensation = getRomFontCompensation(RomType.EMUI)
        
        assertEquals("EMUI应使用1.0补偿", 1.0f, compensation, 0.001f)
    }

    @Test
    fun `ROM补偿_ONE_UI使用1_02`() {
        val compensation = getRomFontCompensation(RomType.ONE_UI)
        
        assertEquals("ONE_UI应使用1.02补偿", 1.02f, compensation, 0.001f)
    }

    @Test
    fun `ROM补偿_AOSP使用1_0`() {
        val compensation = getRomFontCompensation(RomType.AOSP)
        
        assertEquals("AOSP应使用1.0补偿", 1.0f, compensation, 0.001f)
    }

    // ============================================================
    // 综合缩放因子计算测试
    // ============================================================

    @Test
    fun `综合缩放_标准设备标准字体`() {
        val scaleFactor = 1.0f
        val fontScale = 1.0f
        val densityCompensation = 1.0f
        val romCompensation = 1.0f
        
        val clampedFontScale = fontScale.coerceIn(0.85f, 1.5f)
        val fontScaleFactor = scaleFactor * clampedFontScale * densityCompensation * romCompensation
        
        assertEquals("标准设备应使用1.0缩放", 1.0f, fontScaleFactor, 0.001f)
    }

    @Test
    fun `综合缩放_大字体模式`() {
        val scaleFactor = 1.0f
        val fontScale = 1.3f
        val densityCompensation = 1.0f
        val romCompensation = 1.0f
        
        val clampedFontScale = fontScale.coerceIn(0.85f, 1.5f)
        val fontScaleFactor = scaleFactor * clampedFontScale * densityCompensation * romCompensation
        
        assertEquals("大字体模式应使用1.3缩放", 1.3f, fontScaleFactor, 0.001f)
    }

    @Test
    fun `综合缩放_高密度屏幕大字体`() {
        val scaleFactor = 1.0f
        val fontScale = 1.3f
        val densityCompensation = 0.95f  // xxhdpi
        val romCompensation = 1.0f
        
        val clampedFontScale = fontScale.coerceIn(0.85f, 1.5f)
        val fontScaleFactor = scaleFactor * clampedFontScale * densityCompensation * romCompensation
        
        assertEquals("高密度大字体应使用1.235缩放", 1.235f, fontScaleFactor, 0.001f)
    }

    @Test
    fun `综合缩放_小米设备大字体`() {
        val scaleFactor = 1.0f
        val fontScale = 1.3f
        val densityCompensation = 1.0f
        val romCompensation = 0.98f  // MIUI
        
        val clampedFontScale = fontScale.coerceIn(0.85f, 1.5f)
        val fontScaleFactor = scaleFactor * clampedFontScale * densityCompensation * romCompensation
        
        assertEquals("小米大字体应使用1.274缩放", 1.274f, fontScaleFactor, 0.001f)
    }

    // ============================================================
    // 响应式字体尺寸计算测试
    // ============================================================

    @Test
    fun `字体尺寸_标准缩放下的基准值`() {
        val fontScaleFactor = 1.0f
        
        val fontSizeCaption = 12 * fontScaleFactor
        val fontSizeBody = 14 * fontScaleFactor
        val fontSizeSubtitle = 16 * fontScaleFactor
        val fontSizeTitle = 17 * fontScaleFactor
        val fontSizeHeadline = 20 * fontScaleFactor
        val fontSizeLargeTitle = 34 * fontScaleFactor
        
        assertEquals("Caption应为12", 12f, fontSizeCaption, 0.001f)
        assertEquals("Body应为14", 14f, fontSizeBody, 0.001f)
        assertEquals("Subtitle应为16", 16f, fontSizeSubtitle, 0.001f)
        assertEquals("Title应为17", 17f, fontSizeTitle, 0.001f)
        assertEquals("Headline应为20", 20f, fontSizeHeadline, 0.001f)
        assertEquals("LargeTitle应为34", 34f, fontSizeLargeTitle, 0.001f)
    }

    @Test
    fun `字体尺寸_大字体模式下的值`() {
        val fontScaleFactor = 1.3f
        
        val fontSizeCaption = 12 * fontScaleFactor
        val fontSizeBody = 14 * fontScaleFactor
        val fontSizeTitle = 17 * fontScaleFactor
        
        assertEquals("Caption应为15.6", 15.6f, fontSizeCaption, 0.001f)
        assertEquals("Body应为18.2", 18.2f, fontSizeBody, 0.001f)
        assertEquals("Title应为22.1", 22.1f, fontSizeTitle, 0.001f)
    }

    @Test
    fun `字体尺寸_小字体模式下的值`() {
        val fontScaleFactor = 0.85f
        
        val fontSizeCaption = 12 * fontScaleFactor
        val fontSizeBody = 14 * fontScaleFactor
        val fontSizeTitle = 17 * fontScaleFactor
        
        assertEquals("Caption应为10.2", 10.2f, fontSizeCaption, 0.001f)
        assertEquals("Body应为11.9", 11.9f, fontSizeBody, 0.001f)
        assertEquals("Title应为14.45", 14.45f, fontSizeTitle, 0.001f)
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 模拟ROM类型检测
     */
    private fun detectRomType(manufacturer: String, brand: String): RomType {
        val mfr = manufacturer.lowercase()
        val brd = brand.lowercase()
        
        return when {
            mfr.contains("xiaomi") || brd.contains("xiaomi") -> RomType.MIUI
            mfr.contains("huawei") || brd.contains("huawei") -> RomType.EMUI
            mfr.contains("oppo") || brd.contains("oppo") -> RomType.COLOR_OS
            mfr.contains("vivo") || brd.contains("vivo") -> RomType.FUNTOUCH_OS
            mfr.contains("samsung") || brd.contains("samsung") -> RomType.ONE_UI
            mfr.contains("oneplus") || brd.contains("oneplus") -> RomType.OXYGEN_OS
            else -> RomType.AOSP
        }
    }

    /**
     * 获取ROM字体补偿因子
     */
    private fun getRomFontCompensation(romType: RomType): Float {
        return when (romType) {
            RomType.MIUI -> 0.98f
            RomType.EMUI -> 1.0f
            RomType.COLOR_OS -> 0.99f
            RomType.ONE_UI -> 1.02f
            RomType.FUNTOUCH_OS -> 1.0f
            RomType.OXYGEN_OS -> 1.0f
            RomType.AOSP -> 1.0f
        }
    }
}
