package com.empathy.ai.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 马卡龙色系标签配色
 * 用于画像库页面的标签胶囊
 * 
 * @see TDD-00020 2.3 马卡龙色系标签
 */
object MacaronTagColors {
    /** 粉色系 - 背景色 to 文字色 */
    val Pink = Color(0xFFFFF0F3) to Color(0xFFC41E3A)
    
    /** 黄色系 */
    val Yellow = Color(0xFFFFF8E1) to Color(0xFFB8860B)
    
    /** 青色系 */
    val Cyan = Color(0xFFE0F7FA) to Color(0xFF00838F)
    
    /** 紫色系 */
    val Purple = Color(0xFFF3E5F5) to Color(0xFF7B1FA2)
    
    /** 绿色系 */
    val Green = Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    
    /** 蓝色系 */
    val Blue = Color(0xFFE3F2FD) to Color(0xFF1565C0)
    
    private val colorPairs = listOf(Pink, Yellow, Cyan, Purple, Green, Blue)
    
    /**
     * 根据标签名获取颜色对
     * 使用hashCode确保同一标签名始终返回相同颜色
     * 
     * @param tagName 标签名称
     * @return Pair<背景色, 文字色>
     */
    fun getColorPair(tagName: String): Pair<Color, Color> {
        val index = tagName.hashCode().let { 
            if (it < 0) -it else it 
        } % colorPairs.size
        return colorPairs[index]
    }
    
    /**
     * 根据索引获取颜色对
     * @param index 索引值
     * @return Pair<背景色, 文字色>
     */
    fun getColorPairByIndex(index: Int): Pair<Color, Color> {
        return colorPairs[index % colorPairs.size]
    }
}
