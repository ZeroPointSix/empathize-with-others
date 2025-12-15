package com.empathy.ai.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 关系分数颜色映射
 *
 * 根据关系分数（0-100）提供对应的情感化渐变色
 * 用于动态背景、卡片装饰等情感化设计元素
 *
 * 色彩心理学映射：
 * - Excellent (81-100): 暖橙/粉色 - 传递亲密、温暖、甜蜜的情感
 * - Good (61-80): 柔和黄绿 - 传递友好、舒适、积极的情感
 * - Normal (31-60): 中性蓝灰 - 传递平和、稳定、中立的情感
 * - Poor (0-30): 冷蓝/深灰 - 传递冷淡、疏离、需要关注的情感
 */
object RelationshipColors {
    /**
     * 优秀关系 (81-100分)
     * 暖橙色/粉色渐变 - 亲密、温暖
     */
    val Excellent = listOf(
        Color(0xFFFF6B9D), // 粉色
        Color(0xFFFFA06B)  // 暖橙色
    )
    
    /**
     * 良好关系 (61-80分)
     * 柔和黄绿渐变 - 友好、舒适
     */
    val Good = listOf(
        Color(0xFFFFC371), // 柔和黄色
        Color(0xFF7FD8BE)  // 柔和绿色
    )
    
    /**
     * 一般关系 (31-60分)
     * 中性蓝灰渐变 - 平和、稳定
     */
    val Normal = listOf(
        Color(0xFF89B5E0), // 中性蓝色
        Color(0xFFB8C5D6)  // 中性灰色
    )
    
    /**
     * 冷淡关系 (0-30分)
     * 冷蓝/深灰渐变 - 冷淡、疏离
     */
    val Poor = listOf(
        Color(0xFF6B8CAE), // 冷蓝色
        Color(0xFF8B9DAF)  // 深灰色
    )
    
    /**
     * 根据关系分数获取对应的颜色列表
     *
     * @param score 关系分数 (0-100)
     * @return 对应的渐变色列表
     */
    fun getColorsByScore(score: Int): List<Color> {
        return when {
            score >= 81 -> Excellent
            score >= 61 -> Good
            score >= 31 -> Normal
            else -> Poor
        }
    }
}
