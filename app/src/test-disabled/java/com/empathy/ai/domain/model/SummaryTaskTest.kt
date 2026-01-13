package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * SummaryTask 单元测试
 */
class SummaryTaskTest {

    @Test
    fun `创建有效的任务应该成功`() {
        val task = SummaryTask(
            contactId = "test-contact",
            startDate = "2025-12-01",
            endDate = "2025-12-10"
        )
        assertEquals("test-contact", task.contactId)
        assertEquals("2025-12-01", task.startDate)
        assertEquals("2025-12-10", task.endDate)
        assertEquals(SummaryTaskStatus.IDLE, task.status)
        assertEquals(0f, task.progress, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `contactId为空应该抛出异常`() {
        SummaryTask(contactId = "", startDate = "2025-12-01", endDate = "2025-12-10")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `startDate格式错误应该抛出异常`() {
        SummaryTask(contactId = "test", startDate = "2025/12/01", endDate = "2025-12-10")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `endDate格式错误应该抛出异常`() {
        SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "12-10-2025")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `progress超出范围应该抛出异常`() {
        SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10", progress = 1.5f)
    }


    @Test
    fun `getDayCount应该正确计算天数`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        assertEquals(10, task.getDayCount())
    }

    @Test
    fun `getDateRange应该返回正确的DateRange对象`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        val range = task.getDateRange()
        assertEquals("2025-12-01", range.startDate)
        assertEquals("2025-12-10", range.endDate)
    }

    @Test
    fun `withProgress应该正确更新进度`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        val updated = task.withProgress(0.5f, "处理中...")
        assertEquals(0.5f, updated.progress, 0.001f)
        assertEquals("处理中...", updated.currentStep)
    }

    @Test
    fun `withProgress应该限制进度在0到1之间`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        val tooHigh = task.withProgress(1.5f, "test")
        assertEquals(1f, tooHigh.progress, 0.001f)
        val tooLow = task.withProgress(-0.5f, "test")
        assertEquals(0f, tooLow.progress, 0.001f)
    }

    @Test
    fun `markStarted应该设置正确的状态`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        val started = task.markStarted()
        assertEquals(SummaryTaskStatus.FETCHING_DATA, started.status)
        assertNotNull(started.startedAt)
    }

    @Test
    fun `markSuccess应该设置正确的状态`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        val success = task.markSuccess()
        assertEquals(SummaryTaskStatus.SUCCESS, success.status)
        assertEquals(1f, success.progress, 0.001f)
        assertNotNull(success.completedAt)
    }

    @Test
    fun `markFailed应该设置正确的状态和错误`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        val failed = task.markFailed(SummaryError.NetworkError)
        assertEquals(SummaryTaskStatus.FAILED, failed.status)
        assertEquals(SummaryError.NetworkError, failed.error)
        assertNotNull(failed.completedAt)
    }

    @Test
    fun `markCancelled应该设置正确的状态`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        val cancelled = task.markCancelled()
        assertEquals(SummaryTaskStatus.CANCELLED, cancelled.status)
        assertEquals(SummaryError.Cancelled, cancelled.error)
        assertNotNull(cancelled.completedAt)
    }

    @Test
    fun `canRetry对于可重试错误应该返回true`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
            .markFailed(SummaryError.NetworkError)
        assertTrue(task.canRetry())
    }

    @Test
    fun `canRetry对于不可重试错误应该返回false`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
            .markFailed(SummaryError.NoConversations)
        assertFalse(task.canRetry())
    }

    @Test
    fun `create工厂方法应该正确创建任务`() {
        val range = DateRange("2025-12-01", "2025-12-10")
        val task = SummaryTask.create("test-contact", range)
        assertEquals("test-contact", task.contactId)
        assertEquals("2025-12-01", task.startDate)
        assertEquals("2025-12-10", task.endDate)
    }

    @Test
    fun `getDuration未开始时应该返回null`() {
        val task = SummaryTask(contactId = "test", startDate = "2025-12-01", endDate = "2025-12-10")
        assertNull(task.getDuration())
    }

    @Test
    fun `getDuration已完成时应该返回正确时长`() {
        val now = System.currentTimeMillis()
        val task = SummaryTask(
            contactId = "test",
            startDate = "2025-12-01",
            endDate = "2025-12-10",
            startedAt = now - 5000,
            completedAt = now
        )
        assertEquals(5000L, task.getDuration())
    }
}
