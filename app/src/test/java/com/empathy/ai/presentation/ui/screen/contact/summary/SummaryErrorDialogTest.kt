package com.empathy.ai.presentation.ui.screen.contact.summary

import com.empathy.ai.domain.model.SummaryError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SummaryErrorDialog 组件单元测试
 *
 * 测试错误模型的行为，不测试Composable函数本身
 */
class SummaryErrorDialogTest {

    @Test
    fun `网络错误应该可重试`() {
        assertTrue(SummaryError.NetworkError.isRetryable())
    }

    @Test
    fun `超时错误应该可重试`() {
        assertTrue(SummaryError.Timeout.isRetryable())
    }

    @Test
    fun `API错误应该可重试`() {
        assertTrue(SummaryError.ApiError.isRetryable())
    }

    @Test
    fun `无对话数据错误不应该可重试`() {
        assertFalse(SummaryError.NoConversations.isRetryable())
    }

    @Test
    fun `配额不足错误不应该可重试`() {
        assertFalse(SummaryError.QuotaExceeded.isRetryable())
    }

    @Test
    fun `数据库错误不应该可重试`() {
        assertFalse(SummaryError.DatabaseError.isRetryable())
    }

    @Test
    fun `取消错误不应该可重试`() {
        assertFalse(SummaryError.Cancelled.isRetryable())
    }

    @Test
    fun `错误用户提示应该正确`() {
        assertEquals("选定时间段内没有对话记录", SummaryError.NoConversations.userMessage)
        assertEquals("网络连接失败，请检查网络", SummaryError.NetworkError.userMessage)
        assertEquals("AI服务暂时不可用", SummaryError.ApiError.userMessage)
        assertEquals("API调用次数已用完", SummaryError.QuotaExceeded.userMessage)
        assertEquals("处理超时，请稍后重试", SummaryError.Timeout.userMessage)
        assertEquals("保存失败，请重试", SummaryError.DatabaseError.userMessage)
        assertEquals("已取消", SummaryError.Cancelled.userMessage)
    }

    @Test
    fun `错误建议操作应该正确`() {
        assertEquals("请选择包含对话记录的日期范围", SummaryError.NoConversations.getSuggestedAction())
        assertEquals("请检查网络连接后重试", SummaryError.NetworkError.getSuggestedAction())
        assertEquals("请稍后重试，或检查AI服务配置", SummaryError.ApiError.getSuggestedAction())
        assertEquals("请检查API配额或更换服务商", SummaryError.QuotaExceeded.getSuggestedAction())
        assertEquals("请稍后重试", SummaryError.Timeout.getSuggestedAction())
    }

    @Test
    fun `错误码应该正确`() {
        assertEquals("E001", SummaryError.NoConversations.code)
        assertEquals("E002", SummaryError.NetworkError.code)
        assertEquals("E003", SummaryError.ApiError.code)
        assertEquals("E004", SummaryError.QuotaExceeded.code)
        assertEquals("E005", SummaryError.Timeout.code)
        assertEquals("E006", SummaryError.DatabaseError.code)
        assertEquals("E007", SummaryError.Cancelled.code)
    }

    @Test
    fun `Unknown错误应该包含原始消息`() {
        val errorMessage = "测试错误消息"
        val error = SummaryError.Unknown(errorMessage)
        assertEquals("E999", error.code)
        assertEquals("操作失败，请稍后重试", error.userMessage)
        assertEquals("Unknown error: $errorMessage", error.getDebugMessage())
    }
}
