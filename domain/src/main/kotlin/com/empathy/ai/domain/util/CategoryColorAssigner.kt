package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.CategoryColor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 分类颜色分配器
 *
 * 使用哈希算法为分类分配一致的颜色，确保同一分类始终获得相同颜色
 * 纯Kotlin实现，无Android依赖
 */
@Singleton
class CategoryColorAssigner @Inject constructor() {

    companion object {
        /** 颜色数量 */
        private const val COLOR_COUNT = 8

        // ==================== 浅色模式颜色 ====================
        // 红色系
        private val LIGHT_RED = CategoryColor(
            titleColor = 0xFFB71C1C,      // 深红色标题
            tagBackgroundColor = 0xFFFFCDD2, // 浅红色背景
            tagTextColor = 0xFFB71C1C      // 深红色文字
        )
        // 绿色系
        private val LIGHT_GREEN = CategoryColor(
            titleColor = 0xFF1B5E20,      // 深绿色标题
            tagBackgroundColor = 0xFFC8E6C9, // 浅绿色背景
            tagTextColor = 0xFF1B5E20      // 深绿色文字
        )
        // 蓝色系
        private val LIGHT_BLUE = CategoryColor(
            titleColor = 0xFF0D47A1,      // 深蓝色标题
            tagBackgroundColor = 0xFFBBDEFB, // 浅蓝色背景
            tagTextColor = 0xFF0D47A1      // 深蓝色文字
        )
        // 橙色系
        private val LIGHT_ORANGE = CategoryColor(
            titleColor = 0xFFE65100,      // 深橙色标题
            tagBackgroundColor = 0xFFFFE0B2, // 浅橙色背景
            tagTextColor = 0xFFE65100      // 深橙色文字
        )
        // 紫色系
        private val LIGHT_PURPLE = CategoryColor(
            titleColor = 0xFF4A148C,      // 深紫色标题
            tagBackgroundColor = 0xFFE1BEE7, // 浅紫色背景
            tagTextColor = 0xFF4A148C      // 深紫色文字
        )
        // 青色系
        private val LIGHT_CYAN = CategoryColor(
            titleColor = 0xFF006064,      // 深青色标题
            tagBackgroundColor = 0xFFB2EBF2, // 浅青色背景
            tagTextColor = 0xFF006064      // 深青色文字
        )
        // 粉色系
        private val LIGHT_PINK = CategoryColor(
            titleColor = 0xFF880E4F,      // 深粉色标题
            tagBackgroundColor = 0xFFF8BBD9, // 浅粉色背景
            tagTextColor = 0xFF880E4F      // 深粉色文字
        )
        // 灰蓝色系
        private val LIGHT_BLUE_GREY = CategoryColor(
            titleColor = 0xFF37474F,      // 深灰蓝色标题
            tagBackgroundColor = 0xFFCFD8DC, // 浅灰蓝色背景
            tagTextColor = 0xFF37474F      // 深灰蓝色文字
        )

        // ==================== 深色模式颜色 ====================
        // 红色系
        private val DARK_RED = CategoryColor(
            titleColor = 0xFFEF9A9A,      // 浅红色标题
            tagBackgroundColor = 0xFF5D4037, // 深红棕色背景
            tagTextColor = 0xFFFFCDD2      // 浅红色文字
        )
        // 绿色系
        private val DARK_GREEN = CategoryColor(
            titleColor = 0xFFA5D6A7,      // 浅绿色标题
            tagBackgroundColor = 0xFF2E7D32, // 深绿色背景
            tagTextColor = 0xFFC8E6C9      // 浅绿色文字
        )
        // 蓝色系
        private val DARK_BLUE = CategoryColor(
            titleColor = 0xFF90CAF9,      // 浅蓝色标题
            tagBackgroundColor = 0xFF1565C0, // 深蓝色背景
            tagTextColor = 0xFFBBDEFB      // 浅蓝色文字
        )
        // 橙色系
        private val DARK_ORANGE = CategoryColor(
            titleColor = 0xFFFFCC80,      // 浅橙色标题
            tagBackgroundColor = 0xFFE65100, // 深橙色背景
            tagTextColor = 0xFFFFE0B2      // 浅橙色文字
        )
        // 紫色系
        private val DARK_PURPLE = CategoryColor(
            titleColor = 0xFFCE93D8,      // 浅紫色标题
            tagBackgroundColor = 0xFF6A1B9A, // 深紫色背景
            tagTextColor = 0xFFE1BEE7      // 浅紫色文字
        )
        // 青色系
        private val DARK_CYAN = CategoryColor(
            titleColor = 0xFF80DEEA,      // 浅青色标题
            tagBackgroundColor = 0xFF00838F, // 深青色背景
            tagTextColor = 0xFFB2EBF2      // 浅青色文字
        )
        // 粉色系
        private val DARK_PINK = CategoryColor(
            titleColor = 0xFFF48FB1,      // 浅粉色标题
            tagBackgroundColor = 0xFFAD1457, // 深粉色背景
            tagTextColor = 0xFFF8BBD9      // 浅粉色文字
        )
        // 灰蓝色系
        private val DARK_BLUE_GREY = CategoryColor(
            titleColor = 0xFFB0BEC5,      // 浅灰蓝色标题
            tagBackgroundColor = 0xFF455A64, // 深灰蓝色背景
            tagTextColor = 0xFFCFD8DC      // 浅灰蓝色文字
        )

        private val LIGHT_COLORS = listOf(
            LIGHT_RED, LIGHT_GREEN, LIGHT_BLUE, LIGHT_ORANGE,
            LIGHT_PURPLE, LIGHT_CYAN, LIGHT_PINK, LIGHT_BLUE_GREY
        )

        private val DARK_COLORS = listOf(
            DARK_RED, DARK_GREEN, DARK_BLUE, DARK_ORANGE,
            DARK_PURPLE, DARK_CYAN, DARK_PINK, DARK_BLUE_GREY
        )
    }

    /**
     * 为分类分配颜色
     *
     * 使用哈希算法确保同一分类始终获得相同颜色
     *
     * @param categoryKey 分类名称
     * @param isDarkMode 是否为深色模式
     * @return 分类颜色配置
     */
    fun assignColor(categoryKey: String, isDarkMode: Boolean): CategoryColor {
        val colorIndex = abs(categoryKey.hashCode()) % COLOR_COUNT
        return if (isDarkMode) {
            DARK_COLORS[colorIndex]
        } else {
            LIGHT_COLORS[colorIndex]
        }
    }

    /**
     * 获取颜色数量
     *
     * @return 可用的颜色数量
     */
    fun getColorCount(): Int = COLOR_COUNT
}
