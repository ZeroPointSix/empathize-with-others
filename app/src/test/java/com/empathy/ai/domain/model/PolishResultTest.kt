package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PolishResult 单元测试
 *
 * @see PolishResult
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
class PolishResultTest {

    @Test
    fun `getCopyableText 返回优化后的文本`() {
        val result = PolishResult(
            polishedText = "优化后的文本",
            hasRisk = false,
            riskWarning = null
        )

        assertEquals("优化后的文本", result.getCopyableText())
    }

    @Test
    fun `getDisplayContent 无风险时只返回文本`() {
        val result = PolishResult(
            polishedText = "优化后的文本",
            hasRisk = false,
            riskWarning = null
        )

        assertEquals("优化后的文本", result.getDisplayContent())
    }

    @Test
    fun `getDisplayContent 有风险时附加风险提示`() {
        val result = PolishResult(
            polishedText = "优化后的文本",
            hasRisk = true,
            riskWarning = "这句话可能会引起误解"
        )

        val expected = "优化后的文本\n\n⚠️ 风险提示：这句话可能会引起误解"
        assertEquals(expected, result.getDisplayContent())
    }

    @Test
    fun `getDisplayContent hasRisk为true但riskWarning为空时不附加提示`() {
        val result = PolishResult(
            polishedText = "优化后的文本",
            hasRisk = true,
            riskWarning = ""
        )

        assertEquals("优化后的文本", result.getDisplayContent())
    }

    @Test
    fun `getDisplayContent hasRisk为true但riskWarning为null时不附加提示`() {
        val result = PolishResult(
            polishedText = "优化后的文本",
            hasRisk = true,
            riskWarning = null
        )

        assertEquals("优化后的文本", result.getDisplayContent())
    }

    @Test
    fun `默认值测试`() {
        val result = PolishResult(polishedText = "测试")

        assertFalse(result.hasRisk)
        assertEquals(null, result.riskWarning)
    }

    @Test
    fun `data class equals 测试`() {
        val result1 = PolishResult(
            polishedText = "文本",
            hasRisk = true,
            riskWarning = "警告"
        )
        val result2 = PolishResult(
            polishedText = "文本",
            hasRisk = true,
            riskWarning = "警告"
        )

        assertEquals(result1, result2)
    }

    @Test
    fun `data class copy 测试`() {
        val original = PolishResult(
            polishedText = "原始文本",
            hasRisk = false,
            riskWarning = null
        )
        val copied = original.copy(hasRisk = true, riskWarning = "新警告")

        assertEquals("原始文本", copied.polishedText)
        assertTrue(copied.hasRisk)
        assertEquals("新警告", copied.riskWarning)
    }
}
