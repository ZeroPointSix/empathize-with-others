package com.empathy.ai.presentation.ui.component.persona

import androidx.compose.ui.graphics.Color
import com.empathy.ai.presentation.theme.TagCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * SmartFolderCard组件单元测试
 * 
 * 测试智能文件夹卡片的配置和样式
 */
class SmartFolderCardTest {

    @Test
    fun `SmartFolderConfig应该为每个分类返回正确的样式`() {
        // Given & When
        val interestsStyle = SmartFolderConfig.getStyle(TagCategory.INTERESTS)
        val workStyle = SmartFolderConfig.getStyle(TagCategory.WORK)
        val strategyStyle = SmartFolderConfig.getStyle(TagCategory.STRATEGY)
        val riskStyle = SmartFolderConfig.getStyle(TagCategory.RISK)
        
        // Then
        assertNotNull(interestsStyle)
        assertNotNull(workStyle)
        assertNotNull(strategyStyle)
        assertNotNull(riskStyle)
    }
    
    @Test
    fun `兴趣爱好分类应该使用橙色系配色`() {
        // Given & When
        val style = SmartFolderConfig.INTERESTS
        
        // Then - 验证是橙色系
        assertEquals(Color(0xFFFFF3E0), style.iconBackgroundColor)
        assertEquals(Color(0xFFF97316), style.iconColor)
        assertEquals(Color(0xFFFFF8F0), style.tagBackgroundColor)
        assertEquals(Color(0xFFEA580C), style.tagTextColor)
    }
    
    @Test
    fun `工作信息分类应该使用蓝色系配色`() {
        // Given & When
        val style = SmartFolderConfig.WORK
        
        // Then - 验证是蓝色系
        assertEquals(Color(0xFFE3F2FD), style.iconBackgroundColor)
        assertEquals(Color(0xFF3B82F6), style.iconColor)
        assertEquals(Color(0xFFF0F9FF), style.tagBackgroundColor)
        assertEquals(Color(0xFF1D4ED8), style.tagTextColor)
    }
    
    @Test
    fun `沟通策略分类应该使用绿色系配色`() {
        // Given & When
        val style = SmartFolderConfig.STRATEGY
        
        // Then - 验证是绿色系
        assertEquals(Color(0xFFE8F5E9), style.iconBackgroundColor)
        assertEquals(Color(0xFF10B981), style.iconColor)
        assertEquals(Color(0xFFF0FDF4), style.tagBackgroundColor)
        assertEquals(Color(0xFF059669), style.tagTextColor)
    }
    
    @Test
    fun `雷区标签分类应该使用红色系配色`() {
        // Given & When
        val style = SmartFolderConfig.RISK
        
        // Then - 验证是红色系
        assertEquals(Color(0xFFFFEBEE), style.iconBackgroundColor)
        assertEquals(Color(0xFFEF4444), style.iconColor)
        assertEquals(Color(0xFFFEF2F2), style.tagBackgroundColor)
        assertEquals(Color(0xFFDC2626), style.tagTextColor)
    }
    
    @Test
    fun `FolderStyle应该包含所有必要的颜色属性`() {
        // Given
        val style = FolderStyle(
            icon = SmartFolderConfig.INTERESTS.icon,
            iconBackgroundColor = Color.White,
            iconColor = Color.Black,
            tagBackgroundColor = Color.LightGray,
            tagTextColor = Color.DarkGray
        )
        
        // Then
        assertNotNull(style.icon)
        assertEquals(Color.White, style.iconBackgroundColor)
        assertEquals(Color.Black, style.iconColor)
        assertEquals(Color.LightGray, style.tagBackgroundColor)
        assertEquals(Color.DarkGray, style.tagTextColor)
    }
    
    @Test
    fun `每个分类的图标应该不为空`() {
        // Given & When & Then
        assertNotNull(SmartFolderConfig.INTERESTS.icon)
        assertNotNull(SmartFolderConfig.WORK.icon)
        assertNotNull(SmartFolderConfig.STRATEGY.icon)
        assertNotNull(SmartFolderConfig.RISK.icon)
    }
    
    @Test
    fun `性格特点配置应该使用青色系配色`() {
        // Given & When
        val style = SmartFolderConfig.PERSONALITY
        
        // Then - 验证是青色系
        assertEquals(Color(0xFFE0F7FA), style.iconBackgroundColor)
        assertEquals(Color(0xFF0891B2), style.iconColor)
        assertEquals(Color(0xFFF0FDFA), style.tagBackgroundColor)
        assertEquals(Color(0xFF0D9488), style.tagTextColor)
    }
}
