package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TimeFlowMarker 单元测试
 *
 * 测试时间流逝标记的显示格式
 */
class TimeFlowMarkerTest {

    // ==================== DateChange 测试 ====================

    @Test
    fun `DateChange显示格式正确`() {
        val marker = TimeFlowMarker.DateChange("2025-12-16")
        val result = marker.toDisplayString()

        assertEquals("--- [2025-12-16] ---", result)
    }

    @Test
    fun `DateChange包含日期`() {
        val date = "2025-01-01"
        val marker = TimeFlowMarker.DateChange(date)
        val result = marker.toDisplayString()

        assertTrue(result.contains(date))
    }

    // ==================== ShortGap 测试 ====================

    @Test
    fun `ShortGap分钟显示格式正确`() {
        val marker = TimeFlowMarker.ShortGap(30)
        val result = marker.toDisplayString()

        assertEquals("--- (对话暂停了 30 分钟) ---", result)
    }

    @Test
    fun `ShortGap小于60分钟显示分钟`() {
        val marker = TimeFlowMarker.ShortGap(45)
        val result = marker.toDisplayString()

        assertTrue(result.contains("45 分钟"))
        assertTrue(!result.contains("小时"))
    }

    @Test
    fun `ShortGap等于60分钟显示1小时`() {
        val marker = TimeFlowMarker.ShortGap(60)
        val result = marker.toDisplayString()

        assertEquals("--- (对话暂停了 1 小时) ---", result)
    }

    @Test
    fun `ShortGap大于60分钟显示小时`() {
        val marker = TimeFlowMarker.ShortGap(90)
        val result = marker.toDisplayString()

        // 90分钟 = 1小时（整除）
        assertEquals("--- (对话暂停了 1 小时) ---", result)
    }

    @Test
    fun `ShortGap120分钟显示2小时`() {
        val marker = TimeFlowMarker.ShortGap(120)
        val result = marker.toDisplayString()

        assertEquals("--- (对话暂停了 2 小时) ---", result)
    }

    // ==================== LongGap 测试 ====================

    @Test
    fun `LongGap显示格式正确`() {
        val marker = TimeFlowMarker.LongGap(6)
        val result = marker.toDisplayString()

        assertEquals("--- (对话暂停了 6 小时，注意对方可能的情绪变化) ---", result)
    }

    @Test
    fun `LongGap包含情绪提示`() {
        val marker = TimeFlowMarker.LongGap(4)
        val result = marker.toDisplayString()

        assertTrue(result.contains("注意对方可能的情绪变化"))
    }

    @Test
    fun `LongGap包含小时数`() {
        val hours = 8
        val marker = TimeFlowMarker.LongGap(hours)
        val result = marker.toDisplayString()

        assertTrue(result.contains("$hours 小时"))
    }

    // ==================== None 测试 ====================

    @Test
    fun `None返回空字符串`() {
        val marker = TimeFlowMarker.None
        val result = marker.toDisplayString()

        assertEquals("", result)
    }

    @Test
    fun `None是单例对象`() {
        val marker1 = TimeFlowMarker.None
        val marker2 = TimeFlowMarker.None

        assertTrue(marker1 === marker2)
    }

    // ==================== 常量测试 ====================

    @Test
    fun `时间单位常量正确`() {
        assertEquals("分钟", TimeFlowMarker.UNIT_MINUTE)
        assertEquals("小时", TimeFlowMarker.UNIT_HOUR)
    }

    @Test
    fun `模板常量包含占位符`() {
        assertTrue(TimeFlowMarker.TEMPLATE_DATE_CHANGE.contains("%s"))
        assertTrue(TimeFlowMarker.TEMPLATE_SHORT_GAP.contains("%s"))
        assertTrue(TimeFlowMarker.TEMPLATE_LONG_GAP.contains("%d"))
    }
}
