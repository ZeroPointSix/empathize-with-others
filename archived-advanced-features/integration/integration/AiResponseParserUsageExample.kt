package com.empathy.ai.data.integration

import android.content.Context
import android.util.Log
import com.empathy.ai.data.parser.ResponseParserFacade
import com.empathy.ai.data.parser.AiResponseParserFactory
import kotlinx.coroutines.*

/**
 * AI响应解析器使用示例
 * 
 * 展示如何使用新的集成管理器和增强功能
 */
object AiResponseParserUsageExample {
    
    private const val TAG = "AiResponseParserUsageExample"
    
    /**
     * 基本使用示例（向后兼容）
     */
    fun basicUsageExample(): ResponseParserFacade {
        // 创建标准解析器（与之前的使用方式相同）
        val parserFacade = ResponseParserFacade.createDefault()
        
        // 使用解析器
        val json = """{"replySuggestion": "这是建议的回复内容"}"""
        val result = parserFacade.parseAnalysisResult(json, "gpt-4")
        
        result.onSuccess { analysisResult ->
            Log.i(TAG, "解析成功: ${analysisResult.replySuggestion}")
        }.onFailure { error ->
            Log.e(TAG, "解析失败", error)
        }
        
        return parserFacade
    }
    
    /**
     * 增强功能使用示例
     */
    suspend fun enhancedUsageExample(context: Context): ResponseParserFacade {
        // 方法1：使用集成管理器（推荐）
        val integrationManager = AiResponseParserIntegrationManager.getInstance(context)
        
        // 初始化并启动增强功能
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        // 获取增强版解析器
        val enhancedParser = integrationManager.getEnhancedParserFacade()
        
        // 使用解析器
        val json = """{"replySuggestion": "这是建议的回复内容"}"""
        val result = enhancedParser?.parseAnalysisResult(json, "gpt-4")
        
        result?.onSuccess { analysisResult ->
            Log.i(TAG, "增强解析成功: ${analysisResult.replySuggestion}")
        }?.onFailure { error ->
            Log.e(TAG, "增强解析失败", error)
        }
        
        // 获取综合状态报告
        val statusReport = integrationManager.getComprehensiveStatusReport()
        Log.i(TAG, "状态报告: 解析成功=${statusReport.monitoringStatus?.metrics}")
        
        return enhancedParser ?: ResponseParserFacade.createDefault()
    }
    
    /**
     * 快速启动示例
     */
    suspend fun quickStartExample(context: Context): ResponseParserFacade {
        // 快速启动增强版解析器
        val enhancedParser = AiResponseParserIntegrationManager.Utils.quickStartEnhancedParser(context)
        
        // 使用解析器
        val json = """{"replySuggestion": "这是建议的回复内容"}"""
        val result = enhancedParser.parseAnalysisResult(json, "gpt-4")
        
        result.onSuccess { analysisResult ->
            Log.i(TAG, "快速启动解析成功: ${analysisResult.replySuggestion}")
        }.onFailure { error ->
            Log.e(TAG, "快速启动解析失败", error)
        }
        
        return enhancedParser
    }
    
    /**
     * 自定义配置示例
     */
    suspend fun customConfigurationExample(context: Context): ResponseParserFacade {
        // 创建自定义字段映射
        val customMappings = mapOf(
            "replySuggestion" to listOf("回复建议", "建议回复", "话术建议"),
            "strategyAnalysis" to listOf("策略分析", "心理分析", "军师分析")
        )
        
        // 方法1：使用工厂方法创建增强版自定义解析器
        val customEnhancedParser = ResponseParserFacade.createEnhancedWithCustomFieldMapping(
            context = context,
            customMappings = customMappings,
            fuzzyThreshold = 0.8
        )
        
        // 使用解析器
        val json = """{"回复建议": "这是建议的回复内容"}"""
        val result = customEnhancedParser.parseAnalysisResult(json, "gpt-4")
        
        result.onSuccess { analysisResult ->
            Log.i(TAG, "自定义配置解析成功: ${analysisResult.replySuggestion}")
        }.onFailure { error ->
            Log.e(TAG, "自定义配置解析失败", error)
        }
        
        return customEnhancedParser
    }
    
    /**
     * 性能监控示例
     */
    suspend fun performanceMonitoringExample(context: Context) {
        // 启动增强功能
        val integrationManager = AiResponseParserIntegrationManager.getInstance(context)
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = false,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 模拟多次解析
        repeat(10) { i ->
            val json = """{"replySuggestion": "建议回复 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            
            result.onSuccess { analysisResult ->
                Log.d(TAG, "解析 $i 成功: ${analysisResult.replySuggestion}")
            }.onFailure { error ->
                Log.e(TAG, "解析 $i 失败", error)
            }
            
            // 短暂延迟，模拟实际使用
            delay(100)
        }
        
        // 获取性能指标
        val metrics = integrationManager.getObservabilityManager().getMetrics()
        val overallMetrics = metrics.getOverallMetrics()
        
        Log.i(TAG, "性能指标: 总请求数=${overallMetrics.totalRequests}, " +
                "成功率=${overallMetrics.successRate}%, " +
                "平均耗时=${overallMetrics.averageDurationMs}ms")
    }
    
    /**
     * 学习机制示例
     */
    suspend fun learningMechanismExample(context: Context) {
        // 启动增强功能
        val integrationManager = AiResponseParserIntegrationManager.getInstance(context)
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
            """{"建议回复": "另一种中文格式"}""",
            """{"话术建议": "第三种中文格式"}"""
        )
        
        jsonVariants.forEachIndexed { index, json ->
            val result = parser.parseAnalysisResult(json, "gpt-4")
            
            result.onSuccess { analysisResult ->
                Log.d(TAG, "学习示例 $index 成功: ${analysisResult.replySuggestion}")
            }.onFailure { error ->
                Log.e(TAG, "学习示例 $index 失败", error)
            }
            
            delay(50)
        }
        
        // 获取学习统计
        val learningEngine = integrationManager.getObservabilityManager().getFieldMappingLearningEngine()
        val learningStats = learningEngine.getLearningStatistics()
        
        Log.i(TAG, "学习统计: 总映射数=${learningStats.totalMappings}, " +
                "成功学习数=${learningStats.successfulLearning}")
    }
    
    /**
     * 告警系统示例
     */
    suspend fun alertingSystemExample(context: Context) {
        // 启动增强功能
        val integrationManager = AiResponseParserIntegrationManager.getInstance(context)
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
        repeat(100) { i ->
            val json = """{"replySuggestion": "建议回复 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            
            result.onSuccess { analysisResult ->
                if (i % 10 == 0) {
                    Log.d(TAG, "告警示例 $i 成功: ${analysisResult.replySuggestion}")
                }
            }.onFailure { error ->
                Log.e(TAG, "告警示例 $i 失败", error)
            }
            
            // 短暂延迟
            delay(10)
        }
        
        // 获取告警统计
        val alertManager = integrationManager.getObservabilityManager().getAlertManager()
        val alertStats = alertManager.getAlertStatistics()
        
        Log.i(TAG, "告警统计: 总告警数=${alertStats.totalAlerts}, " +
                "活跃告警数=${alertStats.activeAlerts}")
    }
    
    /**
     * 数据导出示例
     */
    suspend fun dataExportExample(context: Context) {
        // 启动增强功能
        val integrationManager = AiResponseParserIntegrationManager.getInstance(context)
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        val parser = integrationManager.getEnhancedParserFacade()!!
        
        // 执行一些解析操作
        repeat(5) { i ->
            val json = """{"replySuggestion": "导出示例 $i"}"""
            val result = parser.parseAnalysisResult(json, "gpt-4")
            
            result.onSuccess { analysisResult ->
                Log.d(TAG, "导出示例 $i 成功: ${analysisResult.replySuggestion}")
            }.onFailure { error ->
                Log.e(TAG, "导出示例 $i 失败", error)
            }
            
            delay(100)
        }
        
        // 导出所有数据
        val exportPath = context.filesDir.absolutePath + "/ai_parser_export.json"
        val exportedData = integrationManager.exportAllData(exportPath)
        
        Log.i(TAG, "数据导出完成: $exportPath")
        Log.d(TAG, "导出数据大小: ${exportedData.length} 字符")
    }
    
    /**
     * 配置更新示例
     */
    suspend fun configurationUpdateExample(context: Context) {
        val integrationManager = AiResponseParserIntegrationManager.getInstance(context)
        
        // 初始配置
        integrationManager.initialize(
            enableEnhancedFeatures = true,
            enableMonitoring = true,
            enableLearning = true,
            enableAlerting = true,
            enableObservability = true
        )
        integrationManager.start()
        
        // 获取初始配置
        val initialConfig = integrationManager.getCurrentConfiguration()
        Log.i(TAG, "初始配置: $initialConfig")
        
        // 运行一段时间
        val parser = integrationManager.getEnhancedParserFacade()!!
        repeat(5) { i ->
            val json = """{"replySuggestion": "配置更新示例 $i"}"""
            parser.parseAnalysisResult(json, "gpt-4")
            delay(100)
        }
        
        // 更新配置 - 禁用学习功能
        integrationManager.updateConfiguration(
            enableLearning = false
        )
        
        // 获取更新后配置
        val updatedConfig = integrationManager.getCurrentConfiguration()
        Log.i(TAG, "更新后配置: $updatedConfig")
        
        // 继续运行
        repeat(5) { i ->
            val json = """{"replySuggestion": "配置更新后示例 $i"}"""
            parser.parseAnalysisResult(json, "gpt-4")
            delay(100)
        }
    }
    
    /**
     * 完整使用示例
     */
    suspend fun completeUsageExample(context: Context) {
        Log.i(TAG, "=== AI响应解析器完整使用示例 ===")
        
        try {
            // 1. 初始化
            val integrationManager = AiResponseParserIntegrationManager.getInstance(context)
            integrationManager.initialize(
                enableEnhancedFeatures = true,
                enableMonitoring = true,
                enableLearning = true,
                enableAlerting = true,
                enableObservability = true
            )
            integrationManager.start()
            
            // 2. 获取解析器
            val parser = integrationManager.getRecommendedParserFacade()
            
            // 3. 执行解析操作
            val testCases = listOf(
                """{"replySuggestion": "标准格式回复"}""",
                """{"回复建议": "中文格式回复"}""",
                """{"strategyAnalysis": "策略分析内容", "riskLevel": "SAFE"}""",
                """{"isSafe": true, "triggeredRisks": [], "suggestion": "安全建议"}""",
                """{"facts": {"name": "张三", "age": "25"}, "redTags": ["敏感话题"], "greenTags": ["友好沟通"]}"""
            )
            
            testCases.forEachIndexed { index, json ->
                when (index) {
                    0, 1 -> {
                        val result = parser.parseAnalysisResult(json, "gpt-4")
                        result.onSuccess { 
                            Log.d(TAG, "解析成功 $index: ${it.replySuggestion}")
                        }.onFailure { 
                            Log.e(TAG, "解析失败 $index", it)
                        }
                    }
                    2 -> {
                        val result = parser.parseAnalysisResult(json, "gpt-4")
                        result.onSuccess { 
                            Log.d(TAG, "解析成功 $index: ${it.strategyAnalysis}")
                        }.onFailure { 
                            Log.e(TAG, "解析失败 $index", it)
                        }
                    }
                    3 -> {
                        val result = parser.parseSafetyCheckResult(json, "gpt-4")
                        result.onSuccess { 
                            Log.d(TAG, "安全检查成功 $index: ${it.isSafe}")
                        }.onFailure { 
                            Log.e(TAG, "安全检查失败 $index", it)
                        }
                    }
                    4 -> {
                        val result = parser.parseExtractedData(json, "gpt-4")
                        result.onSuccess { 
                            Log.d(TAG, "数据提取成功 $index: ${it.facts}")
                        }.onFailure { 
                            Log.e(TAG, "数据提取失败 $index", it)
                        }
                    }
                }
                
                delay(200)
            }
            
            // 4. 获取状态报告
            val statusReport = integrationManager.getComprehensiveStatusReport()
            Log.i(TAG, "=== 状态报告 ===")
            Log.i(TAG, "解析器状态: ${if (statusReport.isStarted) "已启动" else "未启动"}")
            Log.i(TAG, "监控状态: ${statusReport.monitoringStatus != null}")
            Log.i(TAG, "学习状态: ${statusReport.learningStatus != null}")
            Log.i(TAG, "告警状态: ${statusReport.alertingStatus != null}")
            
            // 5. 导出数据
            val exportPath = context.filesDir.absolutePath + "/complete_example_export.json"
            integrationManager.exportAllData(exportPath)
            Log.i(TAG, "数据已导出到: $exportPath")
            
            // 6. 清理
            integrationManager.cleanup()
            
            Log.i(TAG, "=== 完整使用示例完成 ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "完整使用示例失败", e)
        }
    }
}