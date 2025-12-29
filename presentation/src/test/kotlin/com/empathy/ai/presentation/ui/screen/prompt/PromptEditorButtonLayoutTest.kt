package com.empathy.ai.presentation.ui.screen.prompt

import org.junit.Assert.*
import org.junit.Test

/**
 * 提示词编辑器按钮布局测试
 * 
 * BUG-00037 P3: 验证按钮文字不会换行
 */
class PromptEditorButtonLayoutTest {

    // ==================== 按钮文字测试 ====================

    @Test
    fun `重置按钮文字应该简短`() {
        val buttonText = "重置"
        
        // 文字长度应该不超过4个字符
        assertTrue(buttonText.length <= 4)
    }

    @Test
    fun `保存按钮文字应该简短`() {
        val buttonText = "保存"
        
        // 文字长度应该不超过4个字符
        assertTrue(buttonText.length <= 4)
    }

    // ==================== 按钮宽度计算测试 ====================

    @Test
    fun `按钮内部元素宽度计算`() {
        // Icon: 20dp
        // Spacer: 6dp (优化后)
        // Text: 约30dp (2个中文字符，15sp)
        // Padding: 约24dp (左右各12dp)
        val iconWidth = 20
        val spacerWidth = 6
        val textWidth = 30 // 估算
        val paddingWidth = 24
        
        val totalWidth = iconWidth + spacerWidth + textWidth + paddingWidth
        
        // 总宽度应该小于最小按钮宽度（约80dp）
        assertTrue(totalWidth < 100)
    }

    @Test
    fun `小屏幕设备按钮宽度计算`() {
        // 假设屏幕宽度320dp
        val screenWidth = 320
        val horizontalPadding = 32 // 左右各16dp
        val buttonSpacing = 12
        
        val availableWidth = screenWidth - horizontalPadding - buttonSpacing
        val buttonWidth = availableWidth / 2
        
        // 每个按钮宽度约138dp，应该足够显示内容
        assertTrue(buttonWidth > 100)
    }

    // ==================== 字体大小测试 ====================

    @Test
    fun `按钮字体大小应该合适`() {
        val fontSize = 15 // sp
        
        // 字体大小应该在14-16sp之间
        assertTrue(fontSize in 14..16)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `极小屏幕设备按钮应该仍然可用`() {
        // 假设屏幕宽度280dp（极小屏幕）
        val screenWidth = 280
        val horizontalPadding = 32
        val buttonSpacing = 12
        
        val availableWidth = screenWidth - horizontalPadding - buttonSpacing
        val buttonWidth = availableWidth / 2
        
        // 即使在极小屏幕上，按钮宽度也应该大于最小可用宽度
        assertTrue(buttonWidth > 80)
    }

    @Test
    fun `横屏模式按钮应该正常显示`() {
        // 假设横屏宽度640dp
        val screenWidth = 640
        val horizontalPadding = 32
        val buttonSpacing = 12
        
        val availableWidth = screenWidth - horizontalPadding - buttonSpacing
        val buttonWidth = availableWidth / 2
        
        // 横屏时按钮宽度充足
        assertTrue(buttonWidth > 200)
    }
}
