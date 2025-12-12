package com.empathy.ai.data.integration

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import com.empathy.ai.data.parser.ResponseParserFacade
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel

/**
 * AI响应解析器集成测试
 * 
 * 测试整个系统的集成功能
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AiResponseParserIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var integrationManager: AiResponseParserIntegrationManager
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        integrationManager = AiResponseParserIntegrationManager.getInstance(context)
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
        integrationManager.cleanup()
    }
    
    @Test
    fun `test basic integration with enhanced features`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        // 验证配置
        val config = integrationManager.getCurrentConfiguration()
        assertTrue(config.enableEnhancedFeatures)
        assertTrue(config.enableMonitoring)
        assertTrue(config.enableLearning)
        assertTrue(config.enableAlerting)
        assertTrue(config.enableObservability)
        assertTrue(config.isInitialized)
        assertTrue(config.isStarted)
        
        // 获取增强版解析器
        val parser = integrationManager.getEnhancedParserFacade()
        assertNotNull(parser)
        
        // 测试解析功能
        val json = """{"replySuggestion": "这是建议的回复内容"}"""
        val result = parser.parseAnalysisResult(json, "gpt-4")
        
        assertTrue(result.isSuccess)
        assertEquals("这是建议的回复内容", result.getOrNull()?.replySuggestion)
    }
    
    @Test
    fun `test integration with standard parser`() = runBlocking {
        // 初始化标准功能
        integrationManager.initialize(
            enableEnhancedFeatures = false,
            enableMonitoring = false,
            enableLearning = false,
            enableAlerting = false,
            enableObservability = false
        )
        integrationManager.start()
        
        // 验证配置
        val config = integrationManager.getCurrentConfiguration()
        assertFalse(config.enableEnhancedFeatures)
        assertFalse(config.enableMonitoring)
        assertFalse(config.enableLearning)
        assertFalse(config.enableAlerting)
        assertFalse(config.enableObservability)
        assertTrue(config.isInitialized)
        assertTrue(config.isStarted)
        
        // 获取标准解析器
        val parser = integrationManager.getStandardParserFacade()
        assertNotNull(parser)
        
        // 测试解析功能
        val json = """{"replySuggestion": "这是建议的回复内容"}"""
        val result = parser.parseAnalysisResult(json, "gpt-4")
        
        assertTrue(result.isSuccess)
        assertEquals("这是建议的回复内容", result.getOrNull()?.replySuggestion)
    }
    
    @Test
    fun `test recommended parser selection`() = runBlocking {
        // 测试增强功能启用时的推荐解析器
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser1 = integrationManager.getRecommendedParserFacade()
        assertNotNull(parser1)
        
        // 重置并测试增强功能禁用时的推荐解析器
        integrationManager.cleanup()
        integrationManager.initialize(
            enableEnhancedFeatures = false,
            enableMonitoring = false,
            enableLearning = false,
            enableAlerting = false,
            enableObservability = false
        )
        integrationManager.start()
        
        val parser2 = integrationManager.getRecommendedParserFacade()
        assertNotNull(parser2)
    }
    
    @Test
    fun `test configuration update`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        // 更新配置
        integrationManager.updateConfiguration(
            enableLearning = false
        )
        
        // 验证配置更新
        val config = integrationManager.getCurrentConfiguration()
        assertTrue(config.enableEnhancedFeatures)
        assertTrue(config.enableMonitoring)
        assertFalse(config.enableLearning)
        assertTrue(config.enableAlerting)
        assertTrue(config.enableObservability)
    }
    
    @Test
    fun `test comprehensive status report`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        // 执行一些解析操作
        val parser = integrationManager.getEnhancedParserFacade()!!
        repeat(5) { i ->
            val json = """{"replySuggestion": "建议回复 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            assertTrue(result.isSuccess)
        }
        
        // 获取状态报告
        val statusReport = integrationManager.getComprehensiveStatusReport()
        
        assertNotNull(statusReport)
        assertTrue(statusReport.isStarted)
        assertNotNull(statusReport.monitoringStatus)
        assertNotNull(statusReport.learningStatus)
        assertNotNull(statusReport.alertingStatus)
        assertNotNull(statusReport.observabilityStatus)
    }
    
    @Test
    fun `test data export`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        // 执行一些解析操作
        val parser = integrationManager.getEnhancedParserFacade()!!
        repeat(3) { i ->
            val json = """{"replySuggestion": "导出测试 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            assertTrue(result.isSuccess)
        }
        
        // 导出数据
        val exportedData = integrationManager.exportAllData()
        
        assertNotNull(exportedData)
        assertTrue(exportedData.isNotEmpty())
        assertTrue(exportedData.contains("sessions"))
        assertTrue(exportedData.contains("metadata"))
    }
    
    @Test
    fun `test enhanced parser with different data types`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 测试AnalysisResult解析
        val analysisJson = """{"replySuggestion": "分析建议", "strategyAnalysis": "策略分析", "riskLevel": "SAFE"}"""
        val analysisResult = parser.parseAnalysisResult(analysisJson, "gpt-4")
        assertTrue(analysisResult.isSuccess)
        assertEquals("分析建议", analysisResult.getOrNull()?.replySuggestion)
        assertEquals("策略分析", analysisResult.getOrNull()?.strategyAnalysis)
        assertEquals(RiskLevel.SAFE, analysisResult.getOrNull()?.riskLevel)
        
        // 测试SafetyCheckResult解析
        val safetyJson = """{"isSafe": true, "triggeredRisks": [], "suggestion": "安全建议"}"""
        val safetyResult = parser.parseSafetyCheckResult(safetyJson, "gpt-4")
        assertTrue(safetyResult.isSuccess)
        assertTrue(safetyResult.getOrNull()?.isSafe ?: false)
        assertEquals("安全建议", safetyResult.getOrNull()?.suggestion)
        
        // 测试ExtractedData解析
        val extractionJson = """{"facts": {"name": "张三", "age": "25"}, "redTags": ["敏感话题"], "greenTags": ["友好沟通"]}"""
        val extractionResult = parser.parseExtractedData(extractionJson, "gpt-4")
        assertTrue(extractionResult.isSuccess)
        assertEquals("张三", extractionResult.getOrNull()?.facts?.get("name"))
        assertEquals("25", extractionResult.getOrNull()?.facts?.get("age"))
        assertEquals(listOf("敏感话题"), extractionResult.getOrNull()?.redTags)
        assertEquals(listOf("友好沟通"), extractionResult.getOrNull()?.greenTags)
    }
    
    @Test
    fun `test learning mechanism integration`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = false,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 模拟不同格式的JSON，让系统学习字段映射
        val jsonVariants = listOf(
            """{"replySuggestion": "标准格式"}""",
            """{"回复建议": "中文格式"}""",
            """{"建议回复": "另一种中文格式"}"""
        )
        
        jsonVariants.forEach { json ->
            val result = parser.parseAnalysisResult(json, "gpt-4")
            assertTrue(result.isSuccess)
        }
        
        // 获取学习统计
        val learningStats = integrationManager.getObservabilityManager()
            .getFieldMappingLearningEngine()
            .getLearningStatistics()
        
        assertTrue(learningStats.totalMappings > 0)
        assertTrue(learningStats.successfulLearning > 0)
    }
    
    @Test
    fun `test alerting system integration`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = false,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 模拟大量解析请求，可能触发告警
        repeat(20) { i ->
            val json = """{"replySuggestion": "告警测试 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            assertTrue(result.isSuccess)
        }
        
        // 获取告警统计
        val alertStats = integrationManager.getObservabilityManager()
            .getAlertManager()
            .getAlertStatistics()
        
        assertNotNull(alertStats)
    }
    
    @Test
    fun `test observability system integration`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = false,
            enableAlerting = false,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 执行一些解析操作
        repeat(3) { i ->
            val json = """{"replySuggestion": "可观测性测试 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            assertTrue(result.isSuccess)
        }
        
        // 获取诊断统计
        val diagnosticStats = integrationManager.getObservabilityManager()
            .getDiagnosticCollector()
            .getDiagnosticStatistics()
        
        assertTrue(diagnosticStats.totalEntries > 0)
        
        // 获取日志统计
        val loggingStats = integrationManager.getObservabilityManager()
            .getDetailedLogger()
            .getLoggingStatistics()
        
        assertTrue(loggingStats.totalEntries > 0)
    }
    
    @Test
    fun `test performance monitoring integration`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = false,
            enableAlerting = false,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 执行一些解析操作
        repeat(5) { i ->
            val json = """{"replySuggestion": "性能监控测试 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            assertTrue(result.isSuccess)
        }
        
        // 获取性能指标
        val metrics = integrationManager.getObservabilityManager()
            .getMetrics()
            .getOverallMetrics()
        
        assertEquals(5, metrics.totalRequests)
        assertEquals(5, metrics.successfulRequests)
        assertEquals(0, metrics.failedRequests)
        assertTrue(metrics.averageDurationMs > 0.0)
        assertEquals(100.0, metrics.successRate, 0.01)
    }
    
    @Test
    fun `test error handling and recovery`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 测试有效JSON
        val validJson = """{"replySuggestion": "有效内容"}"""
        val validResult = parser.parseAnalysisResult(validJson, "gpt-4")
        assertTrue(validResult.isSuccess)
        assertEquals("有效内容", validResult.getOrNull()?.replySuggestion)
        
        // 测试无效JSON
        val invalidJson = """{"replySuggestion": """ // 不完整的JSON
        val invalidResult = parser.parseAnalysisResult(invalidJson, "gpt-4")
        assertFalse(invalidResult.isSuccess)
        
        // 测试空JSON
        val emptyJson = ""
        val emptyResult = parser.parseAnalysisResult(emptyJson, "gpt-4")
        assertFalse(emptyResult.isSuccess)
        
        // 验证系统仍然正常工作
        val recoveryJson = """{"replySuggestion": "恢复测试"}"""
        val recoveryResult = parser.parseAnalysisResult(recoveryJson, "gpt-4")
        assertTrue(recoveryResult.isSuccess)
        assertEquals("恢复测试", recoveryResult.getOrNull()?.replySuggestion)
    }
    
    @Test
    fun `test concurrent operations`() = runBlocking {
        // 初始化增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 并发执行解析操作
        val threads = (1..5).map { threadId ->
            Thread {
                repeat(10) { i ->
                    val json = """{"replySuggestion": "并发测试 $threadId-$i"}"""
                    val result = parser.parseAnalysisResult(json, "gpt-4")
                    assertTrue(result.isSuccess)
                }
            }.also { it.start() }
        }
        
        // 等待所有线程完成
        threads.forEach { it.join() }
        
        // 验证结果
        val metrics = integrationManager.getObservabilityManager()
            .getMetrics()
            .getOverallMetrics()
        
        assertEquals(50, metrics.totalRequests) // 5 threads * 10 requests
        assertEquals(50, metrics.successfulRequests)
        assertEquals(0, metrics.failedRequests)
        assertEquals(100.0, metrics.successRate, 0.01)
    }
    
    @Test
    fun `test quick start utils`() {
        // 测试快速启动增强版解析器
        val enhancedParser = AiResponseParserIntegrationManager.Utils.quickStartEnhancedParser(context)
        assertNotNull(enhancedParser)
        
        // 测试解析功能
        val json = """{"replySuggestion": "快速启动测试"}"""
        val result = enhancedParser.parseAnalysisResult(json, "gpt-4")
        assertTrue(result.isSuccess)
        assertEquals("快速启动测试", result.getOrNull()?.replySuggestion)
        
        // 清理
        AiResponseParserIntegrationManager.getInstance(context).cleanup()
        
        // 测试快速启动标准解析器
        val standardParser = AiResponseParserIntegrationManager.Utils.quickStartStandardParser(context)
        assertNotNull(standardParser)
        
        // 测试解析功能
        val standardResult = standardParser.parseAnalysisResult(json, "gpt-4")
        assertTrue(standardResult.isSuccess)
        assertEquals("快速启动测试", standardResult.getOrNull()?.replySuggestion)
        
        // 清理
        AiResponseParserIntegrationManager.getInstance(context).cleanup()
    }
    
    @Test
    fun `test enhanced features support check`() {
        // 测试增强功能支持检查
        val isSupported = AiResponseParserIntegrationManager.Utils.isEnhancedFeaturesSupported()
        
        // 在当前环境中应该支持增强功能
        assertTrue(isSupported)
    }
}