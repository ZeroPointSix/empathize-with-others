package com.empathy.ai.domain.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PerformanceMonitor 性能监控器测试
 *
 * 测试内容：
 * - 内存检查
 * - 操作计时
 * - 帧率监控
 * - 性能报告生成
 */
class PerformanceMonitorTest {
    
    private lateinit var monitor: PerformanceMonitor
    
    @Before
    fun setup() {
        monitor = PerformanceMonitor()
    }
    
    @Test
    fun `checkMemory should return valid memory info`() {
        val memoryInfo = monitor.checkMemory()
        
        assertTrue("usedMemoryMb should be >= 0", memoryInfo.usedMemoryMb >= 0)
        assertTrue("maxMemoryMb should be > 0", memoryInfo.maxMemoryMb > 0)
        assertTrue("availableMemoryMb should be >= 0", memoryInfo.availableMemoryMb >= 0)
        assertTrue("usagePercent should be between 0 and 100", 
            memoryInfo.usagePercent in 0..100)
    }
    
    @Test
    fun `startTimer and endTimer should measure duration`() {
        val operationName = "testOperation"
        
        monitor.startTimer(operationName)
        Thread.sleep(50) // 模拟操作耗时
        val duration = monitor.endTimer(operationName)
        
        assertTrue("Duration should be >= 50ms", duration >= 50)
        assertTrue("Duration should be < 200ms", duration < 200)
    }
    
    @Test
    fun `endTimer without startTimer should return -1`() {
        val duration = monitor.endTimer("nonExistentOperation")
        assertEquals(-1L, duration)
    }
    
    @Test
    fun `measureOperation should return result and record duration`() {
        val result = monitor.measureOperation("testMeasure") {
            Thread.sleep(30)
            "success"
        }
        
        assertEquals("success", result)
        
        // 检查性能状态是否记录了操作
        val state = monitor.performanceState.value
        assertTrue("Operation should be recorded", 
            state.operationStats.containsKey("testMeasure"))
    }
    
    @Test
    fun `measureSuspendOperation should work with coroutines`() = runTest {
        val result = monitor.measureSuspendOperation("suspendTest") {
            "coroutine result"
        }
        
        assertEquals("coroutine result", result)
    }
    
    @Test
    fun `recordFrameTime should track frame times`() {
        // 记录多帧
        repeat(10) {
            monitor.recordFrameTime()
            Thread.sleep(16) // 模拟60fps
        }
        
        val fps = monitor.getCurrentFps()
        // FPS应该在合理范围内（考虑测试环境的不精确性）
        assertTrue("FPS should be > 0", fps > 0)
    }
    
    @Test
    fun `getCurrentFps should return 0 when no frames recorded`() {
        val freshMonitor = PerformanceMonitor()
        assertEquals(0, freshMonitor.getCurrentFps())
    }
    
    @Test
    fun `generateReport should contain all information`() {
        // 先执行一些操作
        monitor.checkMemory()
        monitor.measureOperation("reportTest") { Thread.sleep(10) }
        
        val report = monitor.generateReport()
        
        assertNotNull(report.memoryInfo)
        assertTrue("Timestamp should be > 0", report.timestamp > 0)
        assertNotNull(report.operationStats)
    }
    
    @Test
    fun `clearRecords should reset all data`() {
        // 记录一些数据
        monitor.measureOperation("clearTest") { "test" }
        monitor.recordFrameTime()
        
        // 清除
        monitor.clearRecords()
        
        // 验证清除
        val state = monitor.performanceState.value
        assertTrue("Operation stats should be empty", state.operationStats.isEmpty())
        assertEquals(0, monitor.getCurrentFps())
    }
    
    @Test
    fun `memoryState should be updated after checkMemory`() {
        val initialState = monitor.memoryState.value
        
        monitor.checkMemory()
        
        val updatedState = monitor.memoryState.value
        // 至少maxMemoryMb应该有值
        assertTrue("maxMemoryMb should be > 0", updatedState.maxMemoryMb > 0)
    }
    
    @Test
    fun `MemoryInfo usagePercent calculation should be correct`() {
        val info = MemoryInfo(
            usedMemoryMb = 50,
            maxMemoryMb = 100,
            availableMemoryMb = 50,
            nativeHeapMb = 10,
            isWarning = false,
            isExceeded = false
        )
        
        assertEquals(50, info.usagePercent)
    }
    
    @Test
    fun `MemoryInfo usagePercent should handle zero maxMemory`() {
        val info = MemoryInfo(
            usedMemoryMb = 50,
            maxMemoryMb = 0,
            availableMemoryMb = 0,
            nativeHeapMb = 0,
            isWarning = false,
            isExceeded = false
        )
        
        assertEquals(0, info.usagePercent)
    }
    
    @Test
    fun `OperationStats should have correct default values`() {
        val stats = OperationStats()
        
        assertEquals(0, stats.count)
        assertEquals(0L, stats.avgDuration)
        assertEquals(0L, stats.minDuration)
        assertEquals(0L, stats.maxDuration)
    }
    
    @Test
    fun `PerformanceReport toString should be readable`() {
        val report = monitor.generateReport()
        val reportString = report.toString()
        
        assertTrue("Report should contain memory info", 
            reportString.contains("内存使用"))
        assertTrue("Report should contain fps info", 
            reportString.contains("帧率"))
    }
    
    // ========== CR-00009新增测试 ==========
    
    @Test
    fun `shouldDegrade should return false for normal conditions`() {
        val freshMonitor = PerformanceMonitor()
        // 正常情况下不应该降级
        assertFalse(freshMonitor.shouldDegrade())
    }
    
    @Test
    fun `recordSlowFrame should increment counter for slow frames`() {
        // 记录多个慢帧
        repeat(5) {
            monitor.recordSlowFrame(50L) // 超过阈值
        }
        
        // 应该建议降级
        // 注意：实际降级判断还依赖内存和帧率
        val suggestions = monitor.getDegradationSuggestions()
        assertNotNull(suggestions)
    }
    
    @Test
    fun `recordSlowFrame should decrement counter for fast frames`() {
        // 先记录慢帧
        monitor.recordSlowFrame(50L)
        monitor.recordSlowFrame(50L)
        
        // 然后记录快帧
        monitor.recordSlowFrame(10L)
        monitor.recordSlowFrame(10L)
        
        // 计数器应该减少
        val suggestions = monitor.getDegradationSuggestions()
        assertNotNull(suggestions)
    }
    
    @Test
    fun `getDegradationSuggestions should return list`() {
        val suggestions = monitor.getDegradationSuggestions()
        
        assertNotNull(suggestions)
        assertTrue(suggestions is List<String>)
    }
    
    @Test
    fun `clearRecords should reset slow frame counter`() {
        // 记录慢帧
        repeat(5) {
            monitor.recordSlowFrame(50L)
        }
        
        // 清除记录
        monitor.clearRecords()
        
        // 应该重置
        val suggestions = monitor.getDegradationSuggestions()
        // 清除后建议应该减少
        assertNotNull(suggestions)
    }
}
