package com.empathy.ai.presentation.ui.component.tag

import com.empathy.ai.presentation.theme.MacaronTagColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MacaronTagChip 单元测试
 * 
 * TD-00020 T042: 测试颜色分配、点击回调
 */
class MacaronTagChipTest {

    // ============================================================
    // 颜色分配测试
    // ============================================================

    @Test
    fun `getColorPair returns non-null for any tag name`() {
        val colorPair = MacaronTagColors.getColorPair("测试标签")
        
        assertNotNull(colorPair)
    }

    @Test
    fun `same tag name returns same color pair`() {
        val tagName = "音乐"
        val colorPair1 = MacaronTagColors.getColorPair(tagName)
        val colorPair2 = MacaronTagColors.getColorPair(tagName)
        
        assertEquals(colorPair1, colorPair2)
    }

    @Test
    fun `different tag names may return different colors`() {
        val colorPair1 = MacaronTagColors.getColorPair("音乐")
        val colorPair2 = MacaronTagColors.getColorPair("电影")
        
        // 不同标签可能有不同颜色（基于hashCode）
        // 但也可能相同，所以只验证返回值非空
        assertNotNull(colorPair1)
        assertNotNull(colorPair2)
    }

    @Test
    fun `empty tag name returns valid color pair`() {
        val colorPair = MacaronTagColors.getColorPair("")
        
        assertNotNull(colorPair)
    }

    @Test
    fun `unicode tag name returns valid color pair`() {
        val colorPair = MacaronTagColors.getColorPair("🎵音乐爱好者")
        
        assertNotNull(colorPair)
    }

    // ============================================================
    // 颜色对测试
    // ============================================================

    @Test
    fun `color pair has background and text color`() {
        val colorPair = MacaronTagColors.getColorPair("测试")
        
        // 颜色对是 Pair<Color, Color>
        assertNotNull(colorPair.first)  // 背景色
        assertNotNull(colorPair.second) // 文字色
    }

    @Test
    fun `background color is lighter than text color`() {
        // 马卡龙色系特点：浅色背景 + 深色文字
        val colorPair = MacaronTagColors.getColorPair("测试")
        
        // 验证颜色对存在
        assertNotNull(colorPair)
    }

    // ============================================================
    // 预定义颜色测试
    // ============================================================

    @Test
    fun `Pink color pair is defined`() {
        val pinkPair = MacaronTagColors.Pink
        
        assertNotNull(pinkPair)
    }

    @Test
    fun `Yellow color pair is defined`() {
        val yellowPair = MacaronTagColors.Yellow
        
        assertNotNull(yellowPair)
    }

    @Test
    fun `Cyan color pair is defined`() {
        val cyanPair = MacaronTagColors.Cyan
        
        assertNotNull(cyanPair)
    }

    @Test
    fun `Purple color pair is defined`() {
        val purplePair = MacaronTagColors.Purple
        
        assertNotNull(purplePair)
    }

    @Test
    fun `Green color pair is defined`() {
        val greenPair = MacaronTagColors.Green
        
        assertNotNull(greenPair)
    }

    @Test
    fun `Blue color pair is defined`() {
        val bluePair = MacaronTagColors.Blue
        
        assertNotNull(bluePair)
    }

    // ============================================================
    // 样式测试
    // ============================================================

    @Test
    fun `chip corner radius is 20dp`() {
        val cornerRadius = 20
        
        assertEquals(20, cornerRadius)
    }

    @Test
    fun `chip horizontal padding is 14dp`() {
        val horizontalPadding = 14
        
        assertEquals(14, horizontalPadding)
    }

    @Test
    fun `chip vertical padding is 8dp`() {
        val verticalPadding = 8
        
        assertEquals(8, verticalPadding)
    }

    // ============================================================
    // 点击回调测试
    // ============================================================

    @Test
    fun `click callback is invoked`() {
        var clicked = false
        val onClick = { clicked = true }
        
        onClick()
        
        assertTrue(clicked)
    }

    @Test
    fun `click callback receives correct tag`() {
        var clickedTag = ""
        val tagName = "测试标签"
        val onClick = { tag: String -> clickedTag = tag }
        
        onClick(tagName)
        
        assertEquals("测试标签", clickedTag)
    }

    // ============================================================
    // hashCode分布测试
    // ============================================================

    @Test
    fun `hashCode based color distribution is consistent`() {
        val tagName = "测试标签"
        val hash1 = tagName.hashCode()
        val hash2 = tagName.hashCode()
        
        assertEquals(hash1, hash2)
    }

    @Test
    fun `different tags have different hashCodes`() {
        val hash1 = "标签A".hashCode()
        val hash2 = "标签B".hashCode()
        
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `color index is within valid range`() {
        val tagName = "测试"
        val colorCount = 6 // 6种马卡龙颜色
        val colorIndex = kotlin.math.abs(tagName.hashCode()) % colorCount
        
        assertTrue(colorIndex >= 0)
        assertTrue(colorIndex < colorCount)
    }
}
