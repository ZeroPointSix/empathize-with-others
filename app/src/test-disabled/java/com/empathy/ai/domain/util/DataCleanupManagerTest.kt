package com.empathy.ai.domain.util

import com.empathy.ai.data.local.MemoryPreferences
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.repository.FailedTaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * DataCleanupManager数据清理管理器单元测试
 */
class DataCleanupManagerTest {

    private lateinit var conversationRepository: ConversationRepository
    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var failedTaskRepository: FailedTaskRepository
    private lateinit var memoryPreferences: MemoryPreferences
    private lateinit var dataCleanupManager: DataCleanupManager

    @Before
    fun setup() {
        conversationRepository = mockk(relaxed = true)
        dailySummaryRepository = mockk(relaxed = true)
        failedTaskRepository = mockk(relaxed = true)
        memoryPreferences = mockk(relaxed = true)

        dataCleanupManager = DataCleanupManager(
            conversationRepository,
            dailySummaryRepository,
            failedTaskRepository,
            memoryPreferences
        )
    }

    // ==================== setConfig测试 ====================

    @Test
    fun `setConfig正确更新配置`() {
        val newConfig = CleanupConfig(retentionDays = 30, checkIntervalDays = 3)

        dataCleanupManager.setConfig(newConfig)

        assertEquals(newConfig, dataCleanupManager.config)
    }

    @Test
    fun `默认配置为CleanupConfig_DEFAULT`() {
        assertEquals(CleanupConfig.DEFAULT, dataCleanupManager.config)
    }

    // ==================== checkAndCleanup测试 ====================

    @Test
    fun `checkAndCleanup返回null当未达到清理间隔`() = runTest {
        // 设置上次清理日期为昨天
        every { memoryPreferences.getLastCleanupDate() } returns "2025-12-13"
        every { memoryPreferences.getCurrentDateString() } returns "2025-12-14"

        // 默认检查间隔为7天
        val result = dataCleanupManager.checkAndCleanup()

        assertNull(result)
    }

    @Test
    fun `checkAndCleanup执行清理当超过检查间隔`() = runTest {
        // 设置上次清理日期为8天前
        every { memoryPreferences.getLastCleanupDate() } returns "2025-12-06"
        every { memoryPreferences.getCurrentDateString() } returns "2025-12-14"

        // Mock清理操作返回成功
        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns Result.success(5)
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns Result.success(3)
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns Result.success(2)

        val result = dataCleanupManager.checkAndCleanup()

        assertTrue(result != null)
        assertTrue(result!!.success)
        assertEquals(10, result.totalDeleted)
    }

    @Test
    fun `checkAndCleanup执行清理当从未清理过`() = runTest {
        // 从未清理过
        every { memoryPreferences.getLastCleanupDate() } returns ""
        every { memoryPreferences.getCurrentDateString() } returns "2025-12-14"

        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns Result.success(0)
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns Result.success(0)
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns Result.success(0)

        val result = dataCleanupManager.checkAndCleanup()

        assertTrue(result != null)
        assertTrue(result!!.success)
    }

    @Test
    fun `checkAndCleanup成功后更新最后清理日期`() = runTest {
        every { memoryPreferences.getLastCleanupDate() } returns ""
        every { memoryPreferences.getCurrentDateString() } returns "2025-12-14"

        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns Result.success(0)
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns Result.success(0)
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns Result.success(0)

        dataCleanupManager.checkAndCleanup()

        verify { memoryPreferences.setLastCleanupDate("2025-12-14") }
    }

    // ==================== performCleanup测试 ====================

    @Test
    fun `performCleanup返回正确的删除计数`() = runTest {
        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns Result.success(10)
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns Result.success(5)
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns Result.success(3)

        val result = dataCleanupManager.performCleanup()

        assertTrue(result.success)
        assertEquals(10, result.conversationsDeleted)
        assertEquals(5, result.summariesDeleted)
        assertEquals(3, result.failedTasksDeleted)
        assertEquals(18, result.totalDeleted)
    }

    @Test
    fun `performCleanup处理对话清理失败`() = runTest {
        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns
            Result.failure(Exception("清理失败"))
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns Result.success(5)
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns Result.success(3)

        val result = dataCleanupManager.performCleanup()

        // 即使部分失败，整体仍然成功
        assertTrue(result.success)
        assertEquals(0, result.conversationsDeleted)
        assertEquals(5, result.summariesDeleted)
        assertEquals(3, result.failedTasksDeleted)
    }

    @Test
    fun `performCleanup处理总结清理失败`() = runTest {
        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns Result.success(10)
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns
            Result.failure(Exception("清理失败"))
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns Result.success(3)

        val result = dataCleanupManager.performCleanup()

        assertTrue(result.success)
        assertEquals(10, result.conversationsDeleted)
        assertEquals(0, result.summariesDeleted)
        assertEquals(3, result.failedTasksDeleted)
    }

    @Test
    fun `performCleanup处理失败任务清理失败`() = runTest {
        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns Result.success(10)
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns Result.success(5)
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns
            Result.failure(Exception("清理失败"))

        val result = dataCleanupManager.performCleanup()

        assertTrue(result.success)
        assertEquals(10, result.conversationsDeleted)
        assertEquals(5, result.summariesDeleted)
        assertEquals(0, result.failedTasksDeleted)
    }

    @Test
    fun `performCleanup处理异常`() = runTest {
        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } throws
            RuntimeException("数据库异常")

        val result = dataCleanupManager.performCleanup()

        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("数据库异常") == true)
    }

    @Test
    fun `performCleanup使用配置的保留阈值`() = runTest {
        val customConfig = CleanupConfig(retentionDays = 30, failedTaskRetentionDays = 7)
        dataCleanupManager.setConfig(customConfig)

        coEvery { conversationRepository.cleanupOldSummarizedLogs(any()) } returns Result.success(0)
        coEvery { dailySummaryRepository.cleanupOldSummaries(any()) } returns Result.success(0)
        coEvery { failedTaskRepository.cleanupOldTasks(any()) } returns Result.success(0)

        dataCleanupManager.performCleanup()

        // 验证调用了清理方法
        coVerify { conversationRepository.cleanupOldSummarizedLogs(any()) }
        coVerify { dailySummaryRepository.cleanupOldSummaries(any()) }
        coVerify { failedTaskRepository.cleanupOldTasks(any()) }
    }

    // ==================== CleanupResult测试 ====================

    @Test
    fun `CleanupResult_totalDeleted计算正确`() {
        val result = DataCleanupManager.CleanupResult(
            conversationsDeleted = 10,
            summariesDeleted = 5,
            failedTasksDeleted = 3
        )

        assertEquals(18, result.totalDeleted)
    }

    @Test
    fun `CleanupResult默认值正确`() {
        val result = DataCleanupManager.CleanupResult()

        assertEquals(0, result.conversationsDeleted)
        assertEquals(0, result.summariesDeleted)
        assertEquals(0, result.failedTasksDeleted)
        assertEquals(0, result.totalDeleted)
        assertTrue(result.success)
        assertNull(result.errorMessage)
    }

    @Test
    fun `CleanupResult失败状态正确`() {
        val result = DataCleanupManager.CleanupResult(
            success = false,
            errorMessage = "测试错误"
        )

        assertFalse(result.success)
        assertEquals("测试错误", result.errorMessage)
    }
}
