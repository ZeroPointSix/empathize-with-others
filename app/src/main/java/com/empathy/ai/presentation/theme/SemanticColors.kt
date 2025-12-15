package com.empathy.ai.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 语义化颜色扩展
 *
 * 提供业务相关的语义化颜色，支持深色/浅色模式自动切换
 * 解决硬编码颜色值问题，提高主题一致性和可维护性
 *
 * 使用方式：
 * ```kotlin
 * val colors = LocalSemanticColors.current
 * Box(modifier = Modifier.background(colors.riskRed))
 * ```
 */
@Immutable
data class SemanticColors(
    // ========== 标签颜色 ==========
    
    /** 雷区标签颜色 - 红色系 */
    val riskRed: Color,
    
    /** 策略标签颜色 - 绿色系 */
    val strategyGreen: Color,
    
    // ========== 状态颜色 ==========
    
    /** 成功状态 - 绿色 */
    val success: Color,
    
    /** 警告状态 - 橙色 */
    val warning: Color,
    
    /** 错误状态 - 红色 */
    val error: Color,
    
    /** 信息状态 - 蓝色 */
    val info: Color,
    
    /** 不可用状态 - 灰色 */
    val disabled: Color,
    
    // ========== 情感颜色 ==========
    
    /** 甜蜜情绪 - 粉色 */
    val emotionSweet: Color,
    
    /** 冲突情绪 - 红色 */
    val emotionConflict: Color,
    
    /** 礼物情绪 - 金色 */
    val emotionGift: Color,
    
    /** 约会情绪 - 粉紫色 */
    val emotionDate: Color,
    
    /** 深度对话 - 蓝色 */
    val emotionDeepTalk: Color,
    
    /** 中性情绪 - 灰色 */
    val emotionNeutral: Color
)

/**
 * 浅色模式语义化颜色
 */
val LightSemanticColors = SemanticColors(
    // 标签颜色
    riskRed = Color(0xFFE53935),
    strategyGreen = Color(0xFF43A047),
    
    // 状态颜色
    success = Color(0xFF43A047),
    warning = Color(0xFFFF9800),
    error = Color(0xFFE53935),
    info = Color(0xFF2196F3),
    disabled = Color(0xFF9E9E9E),
    
    // 情感颜色
    emotionSweet = Color(0xFFE91E63),
    emotionConflict = Color(0xFFF44336),
    emotionGift = Color(0xFFFFB300),
    emotionDate = Color(0xFF9C27B0),
    emotionDeepTalk = Color(0xFF3F51B5),
    emotionNeutral = Color(0xFF9E9E9E)
)

/**
 * 深色模式语义化颜色
 */
val DarkSemanticColors = SemanticColors(
    // 标签颜色 - 深色模式下稍微提亮
    riskRed = Color(0xFFEF5350),
    strategyGreen = Color(0xFF66BB6A),
    
    // 状态颜色
    success = Color(0xFF66BB6A),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFEF5350),
    info = Color(0xFF64B5F6),
    disabled = Color(0xFFBDBDBD),
    
    // 情感颜色
    emotionSweet = Color(0xFFF48FB1),
    emotionConflict = Color(0xFFEF5350),
    emotionGift = Color(0xFFFFD54F),
    emotionDate = Color(0xFFCE93D8),
    emotionDeepTalk = Color(0xFF7986CB),
    emotionNeutral = Color(0xFFBDBDBD)
)

/**
 * CompositionLocal 用于提供语义化颜色
 */
val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

/**
 * 扩展函数：根据标签类型获取颜色
 */
@Composable
fun SemanticColors.getTagColor(isRisk: Boolean): Color {
    return if (isRisk) riskRed else strategyGreen
}

/**
 * 扩展函数：根据情绪类型获取颜色
 */
fun SemanticColors.getEmotionColor(emotionType: com.empathy.ai.domain.model.EmotionType): Color {
    return when (emotionType) {
        com.empathy.ai.domain.model.EmotionType.SWEET -> emotionSweet
        com.empathy.ai.domain.model.EmotionType.CONFLICT -> emotionConflict
        com.empathy.ai.domain.model.EmotionType.GIFT -> emotionGift
        com.empathy.ai.domain.model.EmotionType.DATE -> emotionDate
        com.empathy.ai.domain.model.EmotionType.DEEP_TALK -> emotionDeepTalk
        com.empathy.ai.domain.model.EmotionType.NEUTRAL -> emotionNeutral
    }
}
