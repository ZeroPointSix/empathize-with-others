package com.empathy.ai.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Compose颜色配置
 *
 * 用于UI层的颜色配置，使用Compose Color类型
 */
@Stable
data class ComposeCategoryColor(
    val titleColor: Color,
    val tagBackgroundColor: Color,
    val tagTextColor: Color
)

/**
 * 分类颜色调色板
 *
 * 提供8种Pastel色系颜色，支持深色/浅色模式
 * 符合Material Design 3规范
 */
object CategoryColorPalette {

    /** 颜色数量 */
    const val COLOR_COUNT = 8

    // ==================== 浅色模式颜色 ====================
    
    /** 红色系 - 浅色模式 */
    private val LightRed = ComposeCategoryColor(
        titleColor = Color(0xFFB71C1C),      // 深红色标题
        tagBackgroundColor = Color(0xFFFFCDD2), // 浅红色背景
        tagTextColor = Color(0xFFB71C1C)      // 深红色文字
    )
    
    /** 绿色系 - 浅色模式 */
    private val LightGreen = ComposeCategoryColor(
        titleColor = Color(0xFF1B5E20),      // 深绿色标题
        tagBackgroundColor = Color(0xFFC8E6C9), // 浅绿色背景
        tagTextColor = Color(0xFF1B5E20)      // 深绿色文字
    )
    
    /** 蓝色系 - 浅色模式 */
    private val LightBlue = ComposeCategoryColor(
        titleColor = Color(0xFF0D47A1),      // 深蓝色标题
        tagBackgroundColor = Color(0xFFBBDEFB), // 浅蓝色背景
        tagTextColor = Color(0xFF0D47A1)      // 深蓝色文字
    )
    
    /** 橙色系 - 浅色模式 */
    private val LightOrange = ComposeCategoryColor(
        titleColor = Color(0xFFE65100),      // 深橙色标题
        tagBackgroundColor = Color(0xFFFFE0B2), // 浅橙色背景
        tagTextColor = Color(0xFFE65100)      // 深橙色文字
    )
    
    /** 紫色系 - 浅色模式 */
    private val LightPurple = ComposeCategoryColor(
        titleColor = Color(0xFF4A148C),      // 深紫色标题
        tagBackgroundColor = Color(0xFFE1BEE7), // 浅紫色背景
        tagTextColor = Color(0xFF4A148C)      // 深紫色文字
    )
    
    /** 青色系 - 浅色模式 */
    private val LightCyan = ComposeCategoryColor(
        titleColor = Color(0xFF006064),      // 深青色标题
        tagBackgroundColor = Color(0xFFB2EBF2), // 浅青色背景
        tagTextColor = Color(0xFF006064)      // 深青色文字
    )
    
    /** 粉色系 - 浅色模式 */
    private val LightPink = ComposeCategoryColor(
        titleColor = Color(0xFF880E4F),      // 深粉色标题
        tagBackgroundColor = Color(0xFFF8BBD0), // 浅粉色背景
        tagTextColor = Color(0xFF880E4F)      // 深粉色文字
    )
    
    /** 灰蓝色系 - 浅色模式 */
    private val LightBlueGrey = ComposeCategoryColor(
        titleColor = Color(0xFF37474F),      // 深灰蓝色标题
        tagBackgroundColor = Color(0xFFCFD8DC), // 浅灰蓝色背景
        tagTextColor = Color(0xFF37474F)      // 深灰蓝色文字
    )

    // ==================== 深色模式颜色 ====================
    
    /** 红色系 - 深色模式 */
    private val DarkRed = ComposeCategoryColor(
        titleColor = Color(0xFFEF9A9A),      // 浅红色标题
        tagBackgroundColor = Color(0xFF5D4037), // 深红棕色背景
        tagTextColor = Color(0xFFFFCDD2)      // 浅红色文字
    )
    
    /** 绿色系 - 深色模式 */
    private val DarkGreen = ComposeCategoryColor(
        titleColor = Color(0xFFA5D6A7),      // 浅绿色标题
        tagBackgroundColor = Color(0xFF2E7D32), // 深绿色背景
        tagTextColor = Color(0xFFC8E6C9)      // 浅绿色文字
    )
    
    /** 蓝色系 - 深色模式 */
    private val DarkBlue = ComposeCategoryColor(
        titleColor = Color(0xFF90CAF9),      // 浅蓝色标题
        tagBackgroundColor = Color(0xFF1565C0), // 深蓝色背景
        tagTextColor = Color(0xFFBBDEFB)      // 浅蓝色文字
    )
    
    /** 橙色系 - 深色模式 */
    private val DarkOrange = ComposeCategoryColor(
        titleColor = Color(0xFFFFCC80),      // 浅橙色标题
        tagBackgroundColor = Color(0xFFE65100), // 深橙色背景
        tagTextColor = Color(0xFFFFE0B2)      // 浅橙色文字
    )
    
    /** 紫色系 - 深色模式 */
    private val DarkPurple = ComposeCategoryColor(
        titleColor = Color(0xFFCE93D8),      // 浅紫色标题
        tagBackgroundColor = Color(0xFF6A1B9A), // 深紫色背景
        tagTextColor = Color(0xFFE1BEE7)      // 浅紫色文字
    )
    
    /** 青色系 - 深色模式 */
    private val DarkCyan = ComposeCategoryColor(
        titleColor = Color(0xFF80DEEA),      // 浅青色标题
        tagBackgroundColor = Color(0xFF00838F), // 深青色背景
        tagTextColor = Color(0xFFB2EBF2)      // 浅青色文字
    )
    
    /** 粉色系 - 深色模式 */
    private val DarkPink = ComposeCategoryColor(
        titleColor = Color(0xFFF48FB1),      // 浅粉色标题
        tagBackgroundColor = Color(0xFFAD1457), // 深粉色背景
        tagTextColor = Color(0xFFF8BBD0)      // 浅粉色文字
    )
    
    /** 灰蓝色系 - 深色模式 */
    private val DarkBlueGrey = ComposeCategoryColor(
        titleColor = Color(0xFFB0BEC5),      // 浅灰蓝色标题
        tagBackgroundColor = Color(0xFF455A64), // 深灰蓝色背景
        tagTextColor = Color(0xFFCFD8DC)      // 浅灰蓝色文字
    )

    /** 浅色模式颜色列表 */
    private val lightColors = listOf(
        LightRed, LightGreen, LightBlue, LightOrange,
        LightPurple, LightCyan, LightPink, LightBlueGrey
    )

    /** 深色模式颜色列表 */
    private val darkColors = listOf(
        DarkRed, DarkGreen, DarkBlue, DarkOrange,
        DarkPurple, DarkCyan, DarkPink, DarkBlueGrey
    )

    /**
     * 根据分类名称获取颜色
     *
     * 使用哈希算法确保同一分类始终获得相同颜色
     *
     * @param categoryKey 分类名称
     * @param isDarkMode 是否为深色模式
     * @return 分类颜色配置
     */
    fun getColorForCategory(categoryKey: String, isDarkMode: Boolean): ComposeCategoryColor {
        val colorIndex = abs(categoryKey.hashCode()) % COLOR_COUNT
        return if (isDarkMode) {
            darkColors[colorIndex]
        } else {
            lightColors[colorIndex]
        }
    }

    /**
     * 获取颜色数量
     *
     * @return 可用的颜色数量
     */
    fun getColorCount(): Int = COLOR_COUNT

    /**
     * 根据索引获取颜色
     *
     * @param index 颜色索引
     * @param isDarkMode 是否为深色模式
     * @return 分类颜色配置
     */
    fun getColorByIndex(index: Int, isDarkMode: Boolean): ComposeCategoryColor {
        val safeIndex = abs(index) % COLOR_COUNT
        return if (isDarkMode) {
            darkColors[safeIndex]
        } else {
            lightColors[safeIndex]
        }
    }
}

/**
 * 从Domain层的CategoryColor转换为Compose层的ComposeCategoryColor
 */
fun com.empathy.ai.domain.model.CategoryColor.toComposeColor(): ComposeCategoryColor {
    return ComposeCategoryColor(
        titleColor = Color(titleColor),
        tagBackgroundColor = Color(tagBackgroundColor),
        tagTextColor = Color(tagTextColor)
    )
}
