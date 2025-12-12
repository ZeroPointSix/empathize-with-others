package com.empathy.ai.data.monitoring

import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AI响应解析器指标测试
 * 
 * 测试性能指标收集和计算功能
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AiResponseParserMetricsTest {
    
    private lateinit var metrics: AiResponseParserMetrics
    
    @Before
    fun setUp() {
        metrics = AiResponseParserMetrics.getInstance()
        // 重置指标状态
        metrics.resetMetrics()
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
        metrics.resetMetrics()
    }
    
    @Test
    fun `test recordParsingResult - success case`() {
        // 记录成功的解析结果
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        
        val overallMetrics = metrics.getOverallMetrics()
        
        assertEquals(1, overallMetrics.totalRequests)
        assertEquals(1, overallMetrics.successfulRequests)
        assertEquals(0, overallMetrics.failedRequests)
        assertEquals(100.0, overallMetrics.averageDurationMs, 0.01)
        assertEquals(100.0, overallMetrics.successRate, 0.01)
    }
    
    @Test
    fun `test recordParsingResult - failure case`() {
        // 记录失败的解析结果
        metrics.recordParsingResult("analysis", "gpt-4", false, 200)
        
        val overallMetrics = metrics.getOverallMetrics()
        
        assertEquals(1, overallMetrics.totalRequests)
        assertEquals(0, overallMetrics.successfulRequests)
        assertEquals(1, overallMetrics.failedRequests)
        assertEquals(200.0, overallMetrics.averageDurationMs, 0.01)
        assertEquals(0.0, overallMetrics.successRate, 0.01)
    }
    
    @Test
    fun `test recordParsingResult - multiple records`() {
        // 记录多个解析结果
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        metrics.recordParsingResult("analysis", "gpt-4", false, 200)
        metrics.recordParsingResult("safety_check", "gpt-3.5", true, 150)
        
        val overallMetrics = metrics.getOverallMetrics()
        
        assertEquals(3, overallMetrics.totalRequests)
        assertEquals(2, overallMetrics.successfulRequests)
        assertEquals(1, overallMetrics.failedRequests)
        assertEquals(150.0, overallMetrics.averageDurationMs, 0.01) // (100+200+150)/3
        assertEquals(66.67, overallMetrics.successRate, 0.01) // 2/3 * 100
    }
    
    @Test
    fun `test getOperationTypeMetrics - specific operation`() {
        // 记录特定操作类型的解析结果
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        metrics.recordParsingResult("analysis", "gpt-4", true, 120)
        metrics.recordParsingResult("safety_check", "gpt-4", false, 80)
        
        val analysisMetrics = metrics.getOperationTypeMetrics("analysis")
        
        assertEquals(2, analysisMetrics.totalRequests)
        assertEquals(2, analysisMetrics.successfulRequests)
        assertEquals(0, analysisMetrics.failedRequests)
        assertEquals(110.0, analysisMetrics.averageDurationMs, 0.01) // (100+120)/2
        assertEquals(100.0, analysisMetrics.successRate, 0.01)
    }
    
    @Test
    fun `test getModelMetrics - specific model`() {
        // 记录特定模型的解析结果
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        metrics.recordParsingResult("safety_check", "gpt-4", false, 80)
        metrics.recordParsingResult("analysis", "gpt-3.5", true, 120)
        
        val gpt4Metrics = metrics.getModelMetrics("gpt-4")
        
        assertEquals(2, gpt4Metrics.totalRequests)
        assertEquals(1, gpt4Metrics.successfulRequests)
        assertEquals(1, gpt4Metrics.failedRequests)
        assertEquals(90.0, gpt4Metrics.averageDurationMs, 0.01) // (100+80)/2
        assertEquals(50.0, gpt4Metrics.successRate, 0.01) // 1/2 * 100
    }
    
    @Test
    fun `test getErrorTypeMetrics - error recording`() {
        // 记录错误类型的解析结果
        metrics.recordParsingResult("analysis", "gpt-4", false, 100, "JsonSyntaxError")
        metrics.recordParsingResult("analysis", "gpt-4", false, 120, "JsonSyntaxError")
        metrics.recordParsingResult("safety_check", "gpt-4", false, 80, "ValidationError")
        
        val jsonErrorMetrics = metrics.getErrorTypeMetrics("JsonSyntaxError")
        
        assertEquals(2, jsonErrorMetrics.count)
        assertEquals(110.0, jsonErrorMetrics.averageDuration, 0.01) // (100+120)/2
    }
    
    @Test
    fun `test getPerformanceSummary`() {
        // 记录多个解析结果
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        metrics.recordParsingResult("analysis", "gpt-4", true, 120)
        metrics.recordParsingResult("analysis", "gpt-4", false, 80)
        metrics.recordParsingResult("safety_check", "gpt-4", true, 90)
        metrics.recordParsingResult("extraction", "gpt-4", true, 110)
        
        val summary = metrics.getPerformanceSummary()
        
        assertEquals(5, summary.totalRequests)
        assertEquals(4, summary.successfulRequests)
        assertEquals(1, summary.failedRequests)
        assertEquals(100.0, summary.averageDurationMs, 0.01) // (100+120+80+90+110)/5
        assertEquals(80.0, summary.successRate, 0.01) // 4/5 * 100
        
        // 检查操作类型分布
        assertEquals(3, summary.operationTypeDistribution["analysis"])
        assertEquals(1, summary.operationTypeDistribution["safety_check"])
        assertEquals(1, summary.operationTypeDistribution["extraction"])
        
        // 检查模型分布
        assertEquals(5, summary.modelDistribution["gpt-4"])
    }
    
    @Test
    fun `test getHealthStatus - healthy`() {
        // 记录健康的解析结果
        repeat(10) {
            metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        }
        
        val healthStatus = metrics.getHealthStatus()
        
        assertEquals("HEALTHY", healthStatus.status)
        assertTrue(healthStatus.isHealthy)
        assertTrue(healthStatus.successRate >= 95.0)
        assertTrue(healthStatus.averageDurationMs <= 1000.0)
    }
    
    @Test
    fun `test getHealthStatus - unhealthy`() {
        // 记录不健康的解析结果
        repeat(10) {
            metrics.recordParsingResult("analysis", "gpt-4", false, 2000)
        }
        
        val healthStatus = metrics.getHealthStatus()
        
        assertEquals("UNHEALTHY", healthStatus.status)
        assertFalse(healthStatus.isHealthy)
        assertTrue(healthStatus.successRate < 50.0)
        assertTrue(healthStatus.averageDurationMs > 1000.0)
    }
    
    @Test
    fun `test getHealthStatus - degraded`() {
        // 记录降级的解析结果
        repeat(5) {
            metrics.recordParsingResult("analysis", "gpt-4", true, 1500)
        }
        repeat(5) {
            metrics.recordParsingResult("analysis", "gpt-4", false, 500)
        }
        
        val healthStatus = metrics.getHealthStatus()
        
        assertEquals("DEGRADED", healthStatus.status)
        assertFalse(healthStatus.isHealthy)
        assertTrue(healthStatus.successRate < 95.0)
        assertTrue(healthStatus.averageDurationMs > 1000.0)
    }
    
    @Test
    fun `test resetMetrics`() {
        // 记录一些解析结果
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        metrics.recordParsingResult("analysis", "gpt-4", false, 200)
        
        // 重置指标
        metrics.resetMetrics()
        
        val overallMetrics = metrics.getOverallMetrics()
        
        assertEquals(0, overallMetrics.totalRequests)
        assertEquals(0, overallMetrics.successfulRequests)
        assertEquals(0, overallMetrics.failedRequests)
        assertEquals(0.0, overallMetrics.averageDurationMs, 0.01)
        assertEquals(0.0, overallMetrics.successRate, 0.01)
    }
    
    @Test
    fun `test getMetricsTimeRange`() {
        // 记录不同时间的解析结果
        val startTime = System.currentTimeMillis()
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        
        // 等待一小段时间
        Thread.sleep(10)
        
        val midTime = System.currentTimeMillis()
        metrics.recordParsingResult("analysis", "gpt-4", false, 200)
        
        // 等待一小段时间
        Thread.sleep(10)
        
        val endTime = System.currentTimeMillis()
        metrics.recordParsingResult("analysis", "gpt-4", true, 150)
        
        // 获取时间范围内的指标
        val rangeMetrics = metrics.getMetricsTimeRange(startTime, midTime)
        
        assertEquals(1, rangeMetrics.totalRequests)
        assertEquals(1, rangeMetrics.successfulRequests)
        assertEquals(0, rangeMetrics.failedRequests)
        assertEquals(100.0, rangeMetrics.averageDurationMs, 0.01)
        assertEquals(100.0, rangeMetrics.successRate, 0.01)
    }
    
    @Test
    fun `test concurrent metric recording`() {
        // 测试并发记录指标
        val threads = (1..10).map { threadId ->
            Thread {
                repeat(10) {
                    metrics.recordParsingResult("analysis", "gpt-4", true, 100)
                }
            }.also { it.start() }
        }
        
        // 等待所有线程完成
        threads.forEach { it.join() }
        
        val overallMetrics = metrics.getOverallMetrics()
        
        assertEquals(100, overallMetrics.totalRequests) // 10 threads * 10 records
        assertEquals(100, overallMetrics.successfulRequests)
        assertEquals(0, overallMetrics.failedRequests)
        assertEquals(100.0, overallMetrics.averageDurationMs, 0.01)
        assertEquals(100.0, overallMetrics.successRate, 0.01)
    }
    
    @Test
    fun `test metric data consistency`() {
        // 记录解析结果
        metrics.recordParsingResult("analysis", "gpt-4", true, 100)
        metrics.recordParsingResult("analysis", "gpt-4", false, 200, "TestError")
        
        // 检查各种指标的一致性
        val overallMetrics = metrics.getOverallMetrics()
        val operationMetrics = metrics.getOperationTypeMetrics("analysis")
        val modelMetrics = metrics.getModelMetrics("gpt-4")
        val errorMetrics = metrics.getErrorTypeMetrics("TestError")
        
        // 总体指标应该等于操作类型指标的总和
        assertEquals(overallMetrics.totalRequests, operationMetrics.totalRequests)
        assertEquals(overallMetrics.successfulRequests, operationMetrics.successfulRequests)
        assertEquals(overallMetrics.failedRequests, operationMetrics.failedRequests)
        
        // 总体指标应该等于模型指标的总和
        assertEquals(overallMetrics.totalRequests, modelMetrics.totalRequests)
        assertEquals(overallMetrics.successfulRequests, modelMetrics.successfulRequests)
        assertEquals(overallMetrics.failedRequests, modelMetrics.failedRequests)
        
        // 错误指标应该反映记录的错误
        assertEquals(1, errorMetrics.count)
    }
}