package com.empathy.ai.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 情绪类型颜色系统
 * 用于事实流页面的情绪节点渐变背景
 * 
 * @see TDD-00020 2.2 情绪类型颜色系统
 */
object EmotionColors {
    /** 甜蜜 - 粉红渐变 */
    val Sweet = listOf(Color(0xFFFFB6C1), Color(0xFFFF69B4))
    
    /** 冲突 - 橙红渐变 */
    val Conflict = listOf(Color(0xFFFFA07A), Color(0xFFFF6347))
    
    /** 中性 - 蓝灰渐变 */
    val Neutral = listOf(Color(0xFFB0C4DE), Color(0xFF87CEEB))
    
    /** 礼物 - 金黄渐变 */
    val Gift = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
    
    /** 约会 - 紫粉渐变 */
    val Date = listOf(Color(0xFFDDA0DD), Color(0xFFBA55D3))
    
    /** 深谈 - 青绿渐变 */
    val DeepTalk = listOf(Color(0xFF98D8C8), Color(0xFF20B2AA))
    
    /**
     * 根据情绪类型获取渐变色列表
     * @param emotionType 情绪类型
     * @return 渐变色列表 [起始色, 结束色]
     */
    fun getGradient(emotionType: EmotionType): List<Color> {
        return when (emotionType) {
            EmotionType.SWEET -> Sweet
            EmotionType.CONFLICT -> Conflict
            EmotionType.NEUTRAL -> Neutral
            EmotionType.GIFT -> Gift
            EmotionType.DATE -> Date
            EmotionType.DEEP_TALK -> DeepTalk
        }
    }
    
    /**
     * 根据情绪类型获取线性渐变Brush
     * @param emotionType 情绪类型
     * @return 线性渐变Brush
     */
    fun getLinearGradientBrush(emotionType: EmotionType): Brush {
        return Brush.linearGradient(colors = getGradient(emotionType))
    }
}

/**
 * 情绪类型枚举
 * 用于事实流页面的情绪分类
 */
enum class EmotionType(
    val emoji: String,
    val displayName: String
) {
    SWEET("❤️", "甜蜜"),
    CONFLICT("⛈️", "冲突"),
    NEUTRAL("💭", "中性"),
    GIFT("🎁", "礼物"),
    DATE("🍽️", "约会"),
    DEEP_TALK("💬", "深谈")
}
