package com.empathy.ai.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CleanupConfig配置类单元测试
 */
class CleanupConfigTest {

    // ==================== 构造函数测试 ====================

    @Test
    fun `默认配置使用MemoryConstants中的值`() {
        val config = CleanupConfig()

        assertEquals(MemoryConstants.DATA_RETENTION_DAYS, config.retentionDays)
        assertEquals(MemoryConstants.CLEANUP_INTERVAL_DAYS, config.checkIntervalDays)
        assertEquals(MemoryConstants.MAX_TASK_RETRIES, config.maxRetryCount)
        assertEquals(MemoryConstants.MAX_RETRY_DAYS, config.failedTaskRetentionDays)
    }

    @Test
    fun `自定义配置正确设置`() {
        val config = CleanupConfig(
            retentionDays = 30,
            checkIntervalDays = 3,
            maxRetryCount = 5,
            failedTaskRetentionDays = 7
        )

        assertEquals(30, config.retentionDays)
        assertEquals(3, config.checkIntervalDays)
        assertEquals(5, config.maxRetryCount)
        assertEquals(7, config.failedTaskRetentionDays)
    }

    // ==================== 预设配置测试 ====================

    @Test
    fun `DEFAULT配置使用默认值`() {
        val config = CleanupConfig.DEFAULT

        assertEquals(MemoryConstants.DATA_RETENTION_DAYS, config.retentionDays)
        assertEquals(MemoryConstants.CLEANUP_INTERVAL_DAYS, config.checkIntervalDays)
    }

    @Test
    fun `AGGRESSIVE配置使用激进值`() {
        val config = CleanupConfig.AGGRESSIVE

        assertEquals(30, config.retentionDays)
        assertEquals(3, config.checkIntervalDays)
        assertEquals(3, config.failedTaskRetentionDays)
    }

    @Test
    fun `CONSERVATIVE配置使用保守值`() {
        val config = CleanupConfig.CONSERVATIVE

        assertEquals(180, config.retentionDays)
        assertEquals(14, config.checkIntervalDays)
        assertEquals(14, config.failedTaskRetentionDays)
    }

    // ==================== getRetentionThreshold 测试 ====================

    @Test
    fun `getRetentionThreshold计算正确的阈值`() {
        val config = CleanupConfig(retentionDays = 90)
        val now = 1000000000000L // 固定时间戳

        val threshold = config.getRetentionThreshold(now)

        val expectedThreshold = now - 90 * MemoryConstants.ONE_DAY_MILLIS
        assertEquals(expectedThreshold, threshold)
    }

    @Test
    fun `getRetentionThreshold使用当前时间当未提供参数`() {
        val config = CleanupConfig(retentionDays = 90)
        val before = System.currentTimeMillis()

        val threshold = config.getRetentionThreshold()

        val after = System.currentTimeMillis()
        val expectedMin = before - 90 * MemoryConstants.ONE_DAY_MILLIS
        val expectedMax = after - 90 * MemoryConstants.ONE_DAY_MILLIS

        assertTrue(threshold >= expectedMin)
        assertTrue(threshold <= expectedMax)
    }

    // ==================== getFailedTaskThreshold 测试 ====================

    @Test
    fun `getFailedTaskThreshold计算正确的阈值`() {
        val config = CleanupConfig(failedTaskRetentionDays = 7)
        val now = 1000000000000L

        val threshold = config.getFailedTaskThreshold(now)

        val expectedThreshold = now - 7 * MemoryConstants.ONE_DAY_MILLIS
        assertEquals(expectedThreshold, threshold)
    }

    @Test
    fun `getFailedTaskThreshold使用当前时间当未提供参数`() {
        val config = CleanupConfig(failedTaskRetentionDays = 7)
        val before = System.currentTimeMillis()

        val threshold = config.getFailedTaskThreshold()

        val after = System.currentTimeMillis()
        val expectedMin = before - 7 * MemoryConstants.ONE_DAY_MILLIS
        val expectedMax = after - 7 * MemoryConstants.ONE_DAY_MILLIS

        assertTrue(threshold >= expectedMin)
        assertTrue(threshold <= expectedMax)
    }

    // ==================== shouldCleanup 测试 ====================

    @Test
    fun `shouldCleanup返回true当lastCleanupDate为空`() {
        val config = CleanupConfig(checkIntervalDays = 7)

        assertTrue(config.shouldCleanup("", "2025-12-14"))
    }

    @Test
    fun `shouldCleanup返回true当lastCleanupDate为空白`() {
        val config = CleanupConfig(checkIntervalDays = 7)

        assertTrue(config.shouldCleanup("   ", "2025-12-14"))
    }

    @Test
    fun `shouldCleanup返回true当超过检查间隔`() {
        val config = CleanupConfig(checkIntervalDays = 7)

        // 距离上次清理8天
        assertTrue(config.shouldCleanup("2025-12-06", "2025-12-14"))
    }

    @Test
    fun `shouldCleanup返回true当刚好达到检查间隔`() {
        val config = CleanupConfig(checkIntervalDays = 7)

        // 距离上次清理刚好7天
        assertTrue(config.shouldCleanup("2025-12-07", "2025-12-14"))
    }

    @Test
    fun `shouldCleanup返回false当未达到检查间隔`() {
        val config = CleanupConfig(checkIntervalDays = 7)

        // 距离上次清理6天
        assertFalse(config.shouldCleanup("2025-12-08", "2025-12-14"))
    }

    @Test
    fun `shouldCleanup返回true当日期解析失败`() {
        val config = CleanupConfig(checkIntervalDays = 7)

        // 无效日期格式
        assertTrue(config.shouldCleanup("invalid-date", "2025-12-14"))
    }

    @Test
    fun `shouldCleanup返回true当当前日期解析失败`() {
        val config = CleanupConfig(checkIntervalDays = 7)

        // 当前日期无效
        assertTrue(config.shouldCleanup("2025-12-07", "invalid"))
    }

    // ==================== data class 测试 ====================

    @Test
    fun `copy方法正确复制配置`() {
        val original = CleanupConfig(retentionDays = 90)
        val copied = original.copy(retentionDays = 30)

        assertEquals(30, copied.retentionDays)
        assertEquals(original.checkIntervalDays, copied.checkIntervalDays)
    }

    @Test
    fun `equals方法正确比较配置`() {
        val config1 = CleanupConfig(retentionDays = 90)
        val config2 = CleanupConfig(retentionDays = 90)
        val config3 = CleanupConfig(retentionDays = 30)

        assertEquals(config1, config2)
        assertFalse(config1 == config3)
    }
}
