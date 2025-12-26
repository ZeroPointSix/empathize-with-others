package com.empathy.ai.presentation.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * 头像淡色系配色方案
 *
 * 提供6种淡色背景+深色文字的配色方案，用于联系人头像显示
 * 使用hashCode取模实现稳定的颜色分配，确保同一联系人始终显示相同颜色
 */
object AvatarColors {

    /**
     * 淡色背景色列表
     */
    private val backgroundColors = listOf(
        Color(0xFFE3F2FD), // 淡蓝色
        Color(0xFFE8F5E9), // 淡绿色
        Color(0xFFFFF3E0), // 淡橙色
        Color(0xFFF3E5F5), // 淡紫色
        Color(0xFFFFEBEE), // 淡红色
        Color(0xFFE0F7FA)  // 淡青色
    )

    /**
     * 深色文字色列表（与背景色一一对应）
     */
    private val textColors = listOf(
        Color(0xFF1565C0), // 深蓝色
        Color(0xFF2E7D32), // 深绿色
        Color(0xFFE65100), // 深橙色
        Color(0xFF7B1FA2), // 深紫色
        Color(0xFFC62828), // 深红色
        Color(0xFF00838F)  // 深青色
    )

    /**
     * 获取颜色配对
     *
     * 根据名称的hashCode获取稳定的颜色配对
     *
     * @param name 联系人名称
     * @return Pair<背景色, 文字色>
     */
    fun getColorPair(name: String): Pair<Color, Color> {
        val index = abs(name.hashCode()) % backgroundColors.size
        return Pair(backgroundColors[index], textColors[index])
    }

    /**
     * 获取背景色
     *
     * @param name 联系人名称
     * @return 背景色
     */
    fun getBackgroundColor(name: String): Color {
        val index = abs(name.hashCode()) % backgroundColors.size
        return backgroundColors[index]
    }

    /**
     * 获取文字色
     *
     * @param name 联系人名称
     * @return 文字色
     */
    fun getTextColor(name: String): Color {
        val index = abs(name.hashCode()) % textColors.size
        return textColors[index]
    }

    /**
     * 获取所有可用的颜色配对数量
     */
    val colorCount: Int = backgroundColors.size
}
