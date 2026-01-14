package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * SummaryType 单元测试
 */
class SummaryTypeTest {

    @Test
    fun `DAILY类型的displayName应该是单日总结`() {
        assertEquals("单日总结", SummaryType.DAILY.displayName)
    }

    @Test
    fun `CUSTOM_RANGE类型的displayName应该是范围总结`() {
        assertEquals("范围总结", SummaryType.CUSTOM_RANGE.displayName)
    }

    @Test
    fun `应该有两种总结类型`() {
        assertEquals(2, SummaryType.entries.size)
    }

    @Test
    fun `valueOf应该正确解析DAILY`() {
        assertEquals(SummaryType.DAILY, SummaryType.valueOf("DAILY"))
    }

    @Test
    fun `valueOf应该正确解析CUSTOM_RANGE`() {
        assertEquals(SummaryType.CUSTOM_RANGE, SummaryType.valueOf("CUSTOM_RANGE"))
    }
}
