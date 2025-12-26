package com.empathy.ai.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 分类卡片左侧色条颜色
 * 用于画像库页面的分类卡片
 * 
 * @see TDD-00020 2.4 分类卡片色条颜色
 */
object CategoryBarColors {
    /** 兴趣爱好 - 橙色 */
    val Interests = Color(0xFFF97316)
    
    /** 工作信息 - 蓝色 */
    val Work = Color(0xFF3B82F6)
    
    /** 沟通策略 - 绿色 */
    val Strategy = Color(0xFF10B981)
    
    /** 雷区标签 - 红色 */
    val Risk = Color(0xFFEF4444)
    
    /**
     * 根据分类类型获取色条颜色
     * @param category 标签分类
     * @return 对应的色条颜色
     */
    fun getBarColor(category: TagCategory): Color {
        return when (category) {
            TagCategory.INTERESTS -> Interests
            TagCategory.WORK -> Work
            TagCategory.STRATEGY -> Strategy
            TagCategory.RISK -> Risk
        }
    }
}

/**
 * 标签分类枚举
 * 用于画像库页面的分类卡片
 */
enum class TagCategory(val displayName: String) {
    INTERESTS("兴趣爱好"),
    WORK("工作信息"),
    STRATEGY("沟通策略"),
    RISK("雷区标签")
}
