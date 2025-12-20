package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * SummaryError 单元测试
 */
class SummaryErrorTest {

    @Test
    fun `NoConversations错误码应该是E001`() {
        assertEquals("E001", SummaryError.NoConversations.code)
    }

    @Test
    fun `NetworkError错误码应该是E002`() {
        assertEquals("E002", SummaryError.NetworkError.code)
    }

    @Test
    fun `ApiError错误码应该是E003`() {
        assertEquals("E003", SummaryError.ApiError.code)
    }

    @Test
    fun `QuotaExceeded错误码应该是E004`() {
        assertEquals("E004", SummaryError.QuotaExceeded.code)
    }

    @Test
    fun `Timeout错误码应该是E005`() {
        assertEquals("E005", SummaryError.Timeout.code)
    }

    @Test
    fun `DatabaseError错误码应该是E006`() {
        assertEquals("E006", SummaryError.DatabaseError.code)
    }

    @Test
    fun `Cancelled错误码应该是E007`() {
        assertEquals("E007", SummaryError.Cancelled.code)
    }

    @Test
    fun `Unknown错误码应该是E999`() {
        val error = SummaryError.Unknown("test error")
        assertEquals("E999", error.code)
    }

    @Test
    fun `NetworkError应该可重试`() {
        assertTrue(SummaryError.NetworkError.isRetryable())
    }

    @Test
    fun `Timeout应该可重试`() {
        assertTrue(SummaryError.Timeout.isRetryable())
    }

    @Test
    fun `ApiError应该可重试`() {
        assertTrue(SummaryError.ApiError.isRetryable())
    }

    @Test
    fun `NoConversations不应该可重试`() {
        assertFalse(SummaryError.NoConversations.isRetryable())
    }

    @Test
    fun `QuotaExceeded不应该可重试`() {
        assertFalse(SummaryError.QuotaExceeded.isRetryable())
    }

    @Test
    fun `DatabaseError不应该可重试`() {
        assertFalse(SummaryError.DatabaseError.isRetryable())
    }

    @Test
    fun `Cancelled不应该可重试`() {
        assertFalse(SummaryError.Cancelled.isRetryable())
    }

    @Test
    fun `Unknown不应该可重试`() {
        val error = SummaryError.Unknown("test")
        assertFalse(error.isRetryable())
    }

    @Test
    fun `NoConversations的userMessage应该正确`() {
        assertEquals("选定时间段内没有对话记录", SummaryError.NoConversations.userMessage)
    }

    @Test
    fun `NetworkError的userMessage应该正确`() {
        assertEquals("网络连接失败，请检查网络", SummaryError.NetworkError.userMessage)
    }

    @Test
    fun `Unknown的userMessage应该是通用提示`() {
        val error = SummaryError.Unknown("specific error")
        assertEquals("操作失败，请稍后重试", error.userMessage)
    }

    @Test
    fun `Unknown的getDebugMessage应该包含原始信息`() {
        val error = SummaryError.Unknown("specific error")
        assertEquals("Unknown error: specific error", error.getDebugMessage())
    }

    @Test
    fun `getSuggestedAction应该返回正确的建议`() {
        assertEquals("请选择包含对话记录的日期范围", SummaryError.NoConversations.getSuggestedAction())
        assertEquals("请检查网络连接后重试", SummaryError.NetworkError.getSuggestedAction())
        assertEquals("请稍后重试，或检查AI服务配置", SummaryError.ApiError.getSuggestedAction())
        assertEquals("请检查API配额或更换服务商", SummaryError.QuotaExceeded.getSuggestedAction())
        assertEquals("请稍后重试", SummaryError.Timeout.getSuggestedAction())
        assertEquals("请重试，如问题持续请重启应用", SummaryError.DatabaseError.getSuggestedAction())
        assertEquals("操作已取消", SummaryError.Cancelled.getSuggestedAction())
        assertEquals("请重试，如问题持续请联系支持", SummaryError.Unknown("test").getSuggestedAction())
    }
}
