package com.empathy.ai.presentation.ui.component.persona

import androidx.compose.ui.graphics.Color
import com.empathy.ai.presentation.theme.TagCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * ModernPersonaTab组件测试
 * 
 * 测试覆盖：
 * - 莫兰迪配色系统
 * - 文件夹样式配置
 * - 数据模型
 */
class ModernPersonaTabTest {

    // ============================================================
    // 莫兰迪配色测试
    // ============================================================

    @Test
    fun `莫兰迪配色_兴趣爱好分类返回淡橙色系`() {
        val colors = MorandiTagColors.getColors(TagCategory.INTERESTS)
        
        // 背景色应该是极淡橙色
        assertEquals(Color(0xFFFFF8F0), colors.backgroundColor)
        // 文字色应该是深橙色
        assertEquals(Color(0xFFEA580C), colors.textColor)
    }

    @Test
    fun `莫兰迪配色_工作信息分类返回淡蓝色系`() {
        val colors = MorandiTagColors.getColors(TagCategory.WORK)
        
        assertEquals(Color(0xFFF0F9FF), colors.backgroundColor)
        assertEquals(Color(0xFF1D4ED8), colors.textColor)
    }

    @Test
    fun `莫兰迪配色_沟通策略分类返回淡绿色系`() {
        val colors = MorandiTagColors.getColors(TagCategory.STRATEGY)
        
        assertEquals(Color(0xFFF0FDF4), colors.backgroundColor)
        assertEquals(Color(0xFF059669), colors.textColor)
    }

    @Test
    fun `莫兰迪配色_雷区标签分类返回淡红色系`() {
        val colors = MorandiTagColors.getColors(TagCategory.RISK)
        
        assertEquals(Color(0xFFFEF2F2), colors.backgroundColor)
        assertEquals(Color(0xFFDC2626), colors.textColor)
    }

    @Test
    fun `莫兰迪配色_所有分类都有对应配色`() {
        TagCategory.values().forEach { category ->
            val colors = MorandiTagColors.getColors(category)
            assertNotNull("分类 ${category.name} 应该有配色", colors)
            assertNotNull("分类 ${category.name} 应该有背景色", colors.backgroundColor)
            assertNotNull("分类 ${category.name} 应该有文字色", colors.textColor)
        }
    }

    // ============================================================
    // 文件夹样式测试
    // ============================================================

    @Test
    fun `文件夹样式_兴趣爱好使用橙色背景`() {
        val style = ModernFolderStyle.getStyle(TagCategory.INTERESTS)
        
        assertEquals(Color(0xFFF97316), style.iconBackground)
        assertNotNull(style.icon)
    }

    @Test
    fun `文件夹样式_工作信息使用蓝色背景`() {
        val style = ModernFolderStyle.getStyle(TagCategory.WORK)
        
        assertEquals(Color(0xFF3B82F6), style.iconBackground)
        assertNotNull(style.icon)
    }

    @Test
    fun `文件夹样式_沟通策略使用绿色背景`() {
        val style = ModernFolderStyle.getStyle(TagCategory.STRATEGY)
        
        assertEquals(Color(0xFF10B981), style.iconBackground)
        assertNotNull(style.icon)
    }

    @Test
    fun `文件夹样式_雷区标签使用红色背景`() {
        val style = ModernFolderStyle.getStyle(TagCategory.RISK)
        
        assertEquals(Color(0xFFEF4444), style.iconBackground)
        assertNotNull(style.icon)
    }

    @Test
    fun `文件夹样式_所有分类都有对应样式`() {
        TagCategory.values().forEach { category ->
            val style = ModernFolderStyle.getStyle(category)
            assertNotNull("分类 ${category.name} 应该有样式", style)
            assertNotNull("分类 ${category.name} 应该有图标", style.icon)
            assertNotNull("分类 ${category.name} 应该有图标背景色", style.iconBackground)
        }
    }

    // ============================================================
    // 数据模型测试
    // ============================================================

    @Test
    fun `PersonaCategoryData_正确存储分类和标签`() {
        val data = PersonaCategoryData(
            category = TagCategory.INTERESTS,
            tags = listOf("喜欢旅行", "爱看电影")
        )
        
        assertEquals(TagCategory.INTERESTS, data.category)
        assertEquals(2, data.tags.size)
        assertEquals("喜欢旅行", data.tags[0])
        assertEquals("爱看电影", data.tags[1])
    }

    @Test
    fun `PersonaCategoryData_支持空标签列表`() {
        val data = PersonaCategoryData(
            category = TagCategory.WORK,
            tags = emptyList()
        )
        
        assertEquals(TagCategory.WORK, data.category)
        assertEquals(0, data.tags.size)
    }

    @Test
    fun `PersonaCategoryData_copy方法正确工作`() {
        val original = PersonaCategoryData(
            category = TagCategory.INTERESTS,
            tags = listOf("喜欢旅行", "爱看电影", "健身达人")
        )
        
        val filtered = original.copy(
            tags = original.tags.filter { it.contains("旅行") }
        )
        
        assertEquals(TagCategory.INTERESTS, filtered.category)
        assertEquals(1, filtered.tags.size)
        assertEquals("喜欢旅行", filtered.tags[0])
    }

    // ============================================================
    // 配色对比度测试（确保可读性）
    // ============================================================

    @Test
    fun `莫兰迪配色_背景色和文字色有足够对比度`() {
        TagCategory.values().forEach { category ->
            val colors = MorandiTagColors.getColors(category)
            
            // 背景色应该是浅色（alpha通道高，RGB值高）
            val bgLuminance = calculateLuminance(colors.backgroundColor)
            // 文字色应该是深色（RGB值低）
            val textLuminance = calculateLuminance(colors.textColor)
            
            // 背景应该比文字亮
            assert(bgLuminance > textLuminance) {
                "分类 ${category.name} 的背景色应该比文字色亮"
            }
        }
    }

    /**
     * 计算颜色的相对亮度（简化版）
     */
    private fun calculateLuminance(color: Color): Float {
        return 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    }
}
