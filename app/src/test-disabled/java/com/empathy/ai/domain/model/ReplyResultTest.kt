package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * ReplyResult 单元测试
 *
 * @see ReplyResult
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
class ReplyResultTest {

    @Test
    fun `getCopyableText 返回建议的回复`() {
        val result = ReplyResult(
            suggestedReply = "好的，我知道了",
            strategyNote = "简短回应表示理解"
        )

        assertEquals("好的，我知道了", result.getCopyableText())
    }

    @Test
    fun `getDisplayContent 无策略说明时只返回回复`() {
        val result = ReplyResult(
            suggestedReply = "好的，我知道了",
            strategyNote = null
        )

        assertEquals("好的，我知道了", result.getDisplayContent())
    }

    @Test
    fun `getDisplayContent 有策略说明时附加说明`() {
        val result = ReplyResult(
            suggestedReply = "好的，我知道了",
            strategyNote = "简短回应表示理解"
        )

        val expected = "好的，我知道了\n\n💡 策略说明：简短回应表示理解"
        assertEquals(expected, result.getDisplayContent())
    }

    @Test
    fun `getDisplayContent strategyNote为空字符串时不附加说明`() {
        val result = ReplyResult(
            suggestedReply = "好的，我知道了",
            strategyNote = ""
        )

        assertEquals("好的，我知道了", result.getDisplayContent())
    }

    @Test
    fun `getDisplayContent strategyNote只有空格时不附加说明`() {
        val result = ReplyResult(
            suggestedReply = "好的，我知道了",
            strategyNote = "   "
        )

        assertEquals("好的，我知道了", result.getDisplayContent())
    }

    @Test
    fun `默认值测试`() {
        val result = ReplyResult(suggestedReply = "测试回复")

        assertNull(result.strategyNote)
    }

    @Test
    fun `data class equals 测试`() {
        val result1 = ReplyResult(
            suggestedReply = "回复",
            strategyNote = "策略"
        )
        val result2 = ReplyResult(
            suggestedReply = "回复",
            strategyNote = "策略"
        )

        assertEquals(result1, result2)
    }

    @Test
    fun `data class copy 测试`() {
        val original = ReplyResult(
            suggestedReply = "原始回复",
            strategyNote = null
        )
        val copied = original.copy(strategyNote = "新策略")

        assertEquals("原始回复", copied.suggestedReply)
        assertEquals("新策略", copied.strategyNote)
    }

    @Test
    fun `长文本回复测试`() {
        val longReply = "这是一段很长的回复内容，" +
            "包含了很多信息，" +
            "用于测试长文本的处理情况。"
        val result = ReplyResult(
            suggestedReply = longReply,
            strategyNote = "详细回复以表达诚意"
        )

        assertEquals(longReply, result.getCopyableText())
    }
}
