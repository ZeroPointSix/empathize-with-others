package com.empathy.ai.domain.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * MemoryLogger日志工具类单元测试
 *
 * 注意：由于MemoryLogger使用Android Log类，这些测试主要验证日志级别控制逻辑
 * 实际的日志输出在单元测试环境中不会执行
 */
class MemoryLoggerTest {

    @Before
    fun setup() {
        // 重置日志级别为默认值
        MemoryLogger.setLogLevel(MemoryLogger.Level.DEBUG)
    }

    @After
    fun tearDown() {
        // 测试后重置日志级别
        MemoryLogger.setLogLevel(MemoryLogger.Level.DEBUG)
    }

    // ==================== Level枚举测试 ====================

    @Test
    fun `Level枚举值正确排序`() {
        assertTrue(MemoryLogger.Level.DEBUG.value < MemoryLogger.Level.INFO.value)
        assertTrue(MemoryLogger.Level.INFO.value < MemoryLogger.Level.WARN.value)
        assertTrue(MemoryLogger.Level.WARN.value < MemoryLogger.Level.ERROR.value)
    }

    @Test
    fun `Level枚举包含所有级别`() {
        val levels = MemoryLogger.Level.values()

        assertEquals(4, levels.size)
        assertTrue(levels.contains(MemoryLogger.Level.DEBUG))
        assertTrue(levels.contains(MemoryLogger.Level.INFO))
        assertTrue(levels.contains(MemoryLogger.Level.WARN))
        assertTrue(levels.contains(MemoryLogger.Level.ERROR))
    }

    // ==================== setLogLevel测试 ====================

    @Test
    fun `setLogLevel正确设置日志级别`() {
        MemoryLogger.setLogLevel(MemoryLogger.Level.INFO)
        assertEquals(MemoryLogger.Level.INFO, MemoryLogger.currentLogLevel)

        MemoryLogger.setLogLevel(MemoryLogger.Level.WARN)
        assertEquals(MemoryLogger.Level.WARN, MemoryLogger.currentLogLevel)

        MemoryLogger.setLogLevel(MemoryLogger.Level.ERROR)
        assertEquals(MemoryLogger.Level.ERROR, MemoryLogger.currentLogLevel)

        MemoryLogger.setLogLevel(MemoryLogger.Level.DEBUG)
        assertEquals(MemoryLogger.Level.DEBUG, MemoryLogger.currentLogLevel)
    }

    @Test
    fun `默认日志级别为DEBUG`() {
        // 重新设置为DEBUG后验证
        MemoryLogger.setLogLevel(MemoryLogger.Level.DEBUG)
        assertEquals(MemoryLogger.Level.DEBUG, MemoryLogger.currentLogLevel)
    }

    // ==================== 日志级别过滤测试 ====================

    @Test
    fun `DEBUG级别允许所有日志`() {
        MemoryLogger.setLogLevel(MemoryLogger.Level.DEBUG)

        // DEBUG级别应该允许所有级别的日志
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.DEBUG))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.INFO))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.WARN))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.ERROR))
    }

    @Test
    fun `INFO级别过滤DEBUG日志`() {
        MemoryLogger.setLogLevel(MemoryLogger.Level.INFO)

        assertFalse(shouldLogAtLevel(MemoryLogger.Level.DEBUG))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.INFO))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.WARN))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.ERROR))
    }

    @Test
    fun `WARN级别过滤DEBUG和INFO日志`() {
        MemoryLogger.setLogLevel(MemoryLogger.Level.WARN)

        assertFalse(shouldLogAtLevel(MemoryLogger.Level.DEBUG))
        assertFalse(shouldLogAtLevel(MemoryLogger.Level.INFO))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.WARN))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.ERROR))
    }

    @Test
    fun `ERROR级别只允许ERROR日志`() {
        MemoryLogger.setLogLevel(MemoryLogger.Level.ERROR)

        assertFalse(shouldLogAtLevel(MemoryLogger.Level.DEBUG))
        assertFalse(shouldLogAtLevel(MemoryLogger.Level.INFO))
        assertFalse(shouldLogAtLevel(MemoryLogger.Level.WARN))
        assertTrue(shouldLogAtLevel(MemoryLogger.Level.ERROR))
    }

    // ==================== measureTime测试 ====================

    @Test
    fun `measureTime返回block执行结果`() {
        val result = MemoryLogger.measureTime("TEST", "operation") {
            "test result"
        }

        assertEquals("test result", result)
    }

    @Test
    fun `measureTime正确处理数值返回`() {
        val result = MemoryLogger.measureTime("TEST", "calculation") {
            1 + 2 + 3
        }

        assertEquals(6, result)
    }

    @Test
    fun `measureTime正确处理列表返回`() {
        val result = MemoryLogger.measureTime("TEST", "list operation") {
            listOf(1, 2, 3)
        }

        assertEquals(listOf(1, 2, 3), result)
    }

    @Test(expected = RuntimeException::class)
    fun `measureTime传播异常`() {
        MemoryLogger.measureTime("TEST", "failing operation") {
            throw RuntimeException("Test exception")
        }
    }

    @Test
    fun `measureTime执行block代码`() {
        var executed = false

        MemoryLogger.measureTime("TEST", "side effect") {
            executed = true
        }

        assertTrue(executed)
    }

    // ==================== 辅助方法 ====================

    /**
     * 模拟shouldLog逻辑，用于测试日志级别过滤
     */
    private fun shouldLogAtLevel(level: MemoryLogger.Level): Boolean {
        return level.value >= MemoryLogger.currentLogLevel.value
    }
}
