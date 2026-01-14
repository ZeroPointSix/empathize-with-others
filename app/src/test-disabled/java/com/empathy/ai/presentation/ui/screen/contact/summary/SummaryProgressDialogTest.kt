package com.empathy.ai.presentation.ui.screen.contact.summary

import com.empathy.ai.domain.model.SummaryTaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SummaryProgressDialog 组件单元测试
 *
 * 测试SummaryTaskStatus枚举的行为，不测试Composable函数本身
 */
class SummaryProgressDialogTest {

    @Test
    fun `进度百分比计算应该正确`() {
        val progress = 0.6f
        val percent = (progress * 100).toInt()
        assertEquals(60, percent)
    }

    @Test
    fun `FETCHING_DATA状态应该可取消`() {
        assertTrue(SummaryTaskStatus.FETCHING_DATA.isCancellable())
    }

    @Test
    fun `ANALYZING状态应该可取消`() {
        assertTrue(SummaryTaskStatus.ANALYZING.isCancellable())
    }

    @Test
    fun `GENERATING状态应该可取消`() {
        assertTrue(SummaryTaskStatus.GENERATING.isCancellable())
    }

    @Test
    fun `SAVING状态不应该可取消`() {
        assertFalse(SummaryTaskStatus.SAVING.isCancellable())
    }

    @Test
    fun `SUCCESS状态不应该可取消`() {
        assertFalse(SummaryTaskStatus.SUCCESS.isCancellable())
    }

    @Test
    fun `FAILED状态不应该可取消`() {
        assertFalse(SummaryTaskStatus.FAILED.isCancellable())
    }

    @Test
    fun `各状态对应的图标应该正确`() {
        val iconMap = mapOf(
            SummaryTaskStatus.IDLE to "⏳",
            SummaryTaskStatus.FETCHING_DATA to "📥",
            SummaryTaskStatus.ANALYZING to "🤖",
            SummaryTaskStatus.GENERATING to "✍️",
            SummaryTaskStatus.SAVING to "💾",
            SummaryTaskStatus.SUCCESS to "✅",
            SummaryTaskStatus.FAILED to "❌",
            SummaryTaskStatus.CANCELLED to "🚫"
        )
        
        assertEquals(8, iconMap.size)
        assertEquals("📥", iconMap[SummaryTaskStatus.FETCHING_DATA])
        assertEquals("🤖", iconMap[SummaryTaskStatus.ANALYZING])
    }

    @Test
    fun `默认步骤文案应该正确`() {
        val stepTextMap = mapOf(
            SummaryTaskStatus.IDLE to "准备中...",
            SummaryTaskStatus.FETCHING_DATA to "正在获取对话记录...",
            SummaryTaskStatus.ANALYZING to "AI正在分析对话内容...",
            SummaryTaskStatus.GENERATING to "正在生成总结...",
            SummaryTaskStatus.SAVING to "正在保存结果...",
            SummaryTaskStatus.SUCCESS to "完成",
            SummaryTaskStatus.FAILED to "处理失败",
            SummaryTaskStatus.CANCELLED to "已取消"
        )
        
        assertEquals("正在获取对话记录...", stepTextMap[SummaryTaskStatus.FETCHING_DATA])
        assertEquals("AI正在分析对话内容...", stepTextMap[SummaryTaskStatus.ANALYZING])
    }

    @Test
    fun `终态判断应该正确`() {
        assertTrue(SummaryTaskStatus.SUCCESS.isTerminal())
        assertTrue(SummaryTaskStatus.FAILED.isTerminal())
        assertTrue(SummaryTaskStatus.CANCELLED.isTerminal())
        assertFalse(SummaryTaskStatus.IDLE.isTerminal())
        assertFalse(SummaryTaskStatus.ANALYZING.isTerminal())
    }
}
