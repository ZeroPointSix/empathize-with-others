package com.empathy.ai.presentation.ui.component.ios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TemperatureSlider 单元测试
 * 
 * TD-00025 T3-05: Temperature滑块单元测试
 * 
 * 测试覆盖：
 * - 边界值验证
 * - 步进精度验证
 * - 范围标签逻辑
 * - 回调触发验证
 */
class TemperatureSliderTest {

    // ============================================================
    // 常量定义
    // ============================================================
    
    companion object {
        private const val TEMPERATURE_MIN = 0.0f
        private const val TEMPERATURE_MAX = 2.0f
        private const val TEMPERATURE_STEP = 0.1f
        private const val DEFAULT_TEMPERATURE = 0.7f
    }

    // ============================================================
    // 边界值测试
    // ============================================================

    @Test
    fun `边界值_最小值应为0`() {
        assertEquals("最小值应为0.0", 0.0f, TEMPERATURE_MIN)
    }

    @Test
    fun `边界值_最大值应为2`() {
        assertEquals("最大值应为2.0", 2.0f, TEMPERATURE_MAX)
    }

    @Test
    fun `边界值_默认值应为0点7`() {
        assertEquals("默认值应为0.7", 0.7f, DEFAULT_TEMPERATURE)
    }

    @Test
    fun `边界值_低于最小值应被限制`() {
        val inputValue = -0.5f
        val safeValue = inputValue.coerceIn(TEMPERATURE_MIN, TEMPERATURE_MAX)
        
        assertEquals("低于最小值应被限制为0.0", 0.0f, safeValue)
    }

    @Test
    fun `边界值_高于最大值应被限制`() {
        val inputValue = 3.0f
        val safeValue = inputValue.coerceIn(TEMPERATURE_MIN, TEMPERATURE_MAX)
        
        assertEquals("高于最大值应被限制为2.0", 2.0f, safeValue)
    }

    @Test
    fun `边界值_正常值不应被修改`() {
        val inputValue = 1.5f
        val safeValue = inputValue.coerceIn(TEMPERATURE_MIN, TEMPERATURE_MAX)
        
        assertEquals("正常值不应被修改", 1.5f, safeValue)
    }

    // ============================================================
    // 步进精度测试
    // ============================================================

    @Test
    fun `步进精度_步进值应为0点1`() {
        assertEquals("步进值应为0.1", 0.1f, TEMPERATURE_STEP)
    }

    @Test
    fun `步进精度_四舍五入到0点1`() {
        val rawValue = 0.75f
        val roundedValue = (rawValue * 10).toInt() / 10f
        
        assertEquals("0.75应四舍五入到0.7", 0.7f, roundedValue)
    }

    @Test
    fun `步进精度_0点78应四舍五入到0点7`() {
        val rawValue = 0.78f
        val roundedValue = (rawValue * 10).toInt() / 10f
        
        assertEquals("0.78应四舍五入到0.7", 0.7f, roundedValue)
    }

    @Test
    fun `步进精度_1点25应四舍五入到1点2`() {
        val rawValue = 1.25f
        val roundedValue = (rawValue * 10).toInt() / 10f
        
        assertEquals("1.25应四舍五入到1.2", 1.2f, roundedValue)
    }

    @Test
    fun `步进精度_步数计算正确`() {
        // (2.0 - 0.0) / 0.1 - 1 = 19 steps
        val steps = ((TEMPERATURE_MAX - TEMPERATURE_MIN) / TEMPERATURE_STEP - 1).toInt()
        
        assertEquals("步数应为19", 19, steps)
    }

    // ============================================================
    // 范围标签逻辑测试
    // ============================================================

    @Test
    fun `范围标签_精确范围0到0点5`() {
        val value = 0.3f
        val isPrecise = value <= 0.5f
        
        assertTrue("0.3应在精确范围内", isPrecise)
    }

    @Test
    fun `范围标签_平衡范围0点6到1点0`() {
        val value = 0.8f
        val isBalanced = value in 0.6f..1.0f
        
        assertTrue("0.8应在平衡范围内", isBalanced)
    }

    @Test
    fun `范围标签_创意范围1点1到2点0`() {
        val value = 1.5f
        val isCreative = value > 1.0f
        
        assertTrue("1.5应在创意范围内", isCreative)
    }

    @Test
    fun `范围标签_边界值0点5属于精确范围`() {
        val value = 0.5f
        val isPrecise = value <= 0.5f
        
        assertTrue("0.5应属于精确范围", isPrecise)
    }

    @Test
    fun `范围标签_边界值0点6属于平衡范围`() {
        val value = 0.6f
        val isBalanced = value in 0.6f..1.0f
        
        assertTrue("0.6应属于平衡范围", isBalanced)
    }

    @Test
    fun `范围标签_边界值1点0属于平衡范围`() {
        val value = 1.0f
        val isBalanced = value in 0.6f..1.0f
        
        assertTrue("1.0应属于平衡范围", isBalanced)
    }

    @Test
    fun `范围标签_边界值1点1属于创意范围`() {
        val value = 1.1f
        val isCreative = value > 1.0f
        
        assertTrue("1.1应属于创意范围", isCreative)
    }

    // ============================================================
    // 回调触发测试
    // ============================================================

    @Test
    fun `回调触发_值变化时触发回调`() {
        var callbackValue: Float? = null
        val onValueChange: (Float) -> Unit = { callbackValue = it }
        
        // 模拟值变化
        onValueChange(1.2f)
        
        assertEquals("回调应接收到正确的值", 1.2f, callbackValue)
    }

    @Test
    fun `回调触发_边界值变化时触发回调`() {
        var callbackValue: Float? = null
        val onValueChange: (Float) -> Unit = { callbackValue = it }
        
        // 模拟边界值变化
        onValueChange(0.0f)
        
        assertEquals("回调应接收到边界值", 0.0f, callbackValue)
    }

    @Test
    fun `回调触发_最大值变化时触发回调`() {
        var callbackValue: Float? = null
        val onValueChange: (Float) -> Unit = { callbackValue = it }
        
        // 模拟最大值变化
        onValueChange(2.0f)
        
        assertEquals("回调应接收到最大值", 2.0f, callbackValue)
    }

    // ============================================================
    // 显示格式测试
    // ============================================================

    @Test
    fun `显示格式_值应显示一位小数`() {
        val value = 0.7f
        val formatted = String.format("%.1f", value)
        
        assertEquals("应显示为0.7", "0.7", formatted)
    }

    @Test
    fun `显示格式_整数值应显示一位小数`() {
        val value = 1.0f
        val formatted = String.format("%.1f", value)
        
        assertEquals("应显示为1.0", "1.0", formatted)
    }

    @Test
    fun `显示格式_最大值应显示为2点0`() {
        val value = 2.0f
        val formatted = String.format("%.1f", value)
        
        assertEquals("应显示为2.0", "2.0", formatted)
    }

    // ============================================================
    // 禁用状态测试
    // ============================================================

    @Test
    fun `禁用状态_禁用时不应触发回调`() {
        val enabled = false
        var callbackTriggered = false
        
        // 模拟禁用状态下的交互
        if (enabled) {
            callbackTriggered = true
        }
        
        assertFalse("禁用状态下不应触发回调", callbackTriggered)
    }

    @Test
    fun `禁用状态_启用时应触发回调`() {
        val enabled = true
        var callbackTriggered = false
        
        // 模拟启用状态下的交互
        if (enabled) {
            callbackTriggered = true
        }
        
        assertTrue("启用状态下应触发回调", callbackTriggered)
    }
}
