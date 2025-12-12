package com.empathy.ai.domain.util

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * PerformanceMonitor 单元测试
 * 
 * 测试性能监控功能
 */
@RunWith(RobolectricTestRunner::class)
class PerformanceMonitorTest {
    
    private lateinit var context: Context
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        performanceMonitor = PerformanceMonitor(context)
    }
    
    @After
    fun tearDown() {
        performanceMonitor.stopMonitoring()
    }
    
    /**
     * 测试获取内存信息
     */
    @Test
    fun `getMemoryInfo should return valid memory information`() {
        val memoryInfo = performanceMonitor.getMemoryInfo()
        
        // 验证内存信息有效
        assertTrue("已使用内存应大于 0", memoryInfo.usedMemoryMB > 0)
        assertTrue("总内存应大于 0", memoryInfo.totalMemoryMB > 0)
        assertTrue("最大内存应大于 0", memoryInfo.maxMemoryMB > 0)
        assertTrue("已使用内存应小于总内存", memoryInfo.usedMemoryMB <= memoryInfo.totalMemoryMB)
        assertTrue("总内存应小于最大内存", memoryInfo.totalMemoryMB <= memoryInfo.maxMemoryMB)
    }
    
    /**
     * 测试内存健康检查
     */
    @Test
    fun `isMemoryHealthy should return true when memory usage is below threshold`() {
        val isHealthy = performanceMonitor.isMemoryHealthy()
        
        // 在测试环境中，内存使用应该是健康的
        assertTrue("内存使用应该是健康的", isHealthy)
    }
    
    /**
     * 测试启动和停止监控
     */
    @Test
    fun `startMonitoring and stopMonitoring should work correctly`() = runBlocking {
        // 启动监控
        performanceMonitor.startMonitoring()
        
        // 立即停止监控（不等待）
        val report = performanceMonitor.stopMonitoring()
        
        // 验证报告
        assertTrue("持续时间应大于等于 0", report.durationMs >= 0)
        assertTrue("峰值内存应大于等于 0", report.peakMemoryMB >= 0)
        assertTrue("当前内存应大于 0", report.currentMemoryMB > 0)
        assertTrue("总内存应大于 0", report.totalMemoryMB > 0)
    }
    
    /**
     * 测试性能报告格式
     */
    @Test
    fun `PerformanceReport toString should return formatted string`() {
        val report = PerformanceReport(
            durationMs = 1000,
            peakMemoryMB = 100,
            currentMemoryMB = 80,
            totalMemoryMB = 200
        )
        
        val reportString = report.toString()
        
        // 验证报告包含关键信息
        assertTrue("报告应包含持续时间", reportString.contains("1000ms"))
        assertTrue("报告应包含峰值内存", reportString.contains("100MB"))
        assertTrue("报告应包含当前内存", reportString.contains("80MB"))
        assertTrue("报告应包含总内存", reportString.contains("200MB"))
    }
    
    /**
     * 测试垃圾回收请求
     */
    @Test
    fun `requestGarbageCollection should not throw exception`() {
        // 请求垃圾回收不应抛出异常
        performanceMonitor.requestGarbageCollection()
    }
}
