package com.empathy.ai.data.learning

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 解析模式分析器
 * 
 * 负责分析解析过程中的模式
 * 识别常见的问题和成功模式
 */
class ParsingPatternAnalyzer {
    
    companion object {
        private const val TAG = "ParsingPatternAnalyzer"
        
        // 分析配置
        private const val MIN_SAMPLES_FOR_ANALYSIS = 20
        private const val MAX_PATTERN_HISTORY = 1000
        private const val PATTERN_RETENTION_PERIOD = 7 * 24 * 60 * 60 * 1000L // 7天
        
        // 模式类型
        private const val ERROR_PATTERN = "error"
        private const val SUCCESS_PATTERN = "success"
        private const val PERFORMANCE_PATTERN = "performance"
        private const val CONTEXT_PATTERN = "context"
    }
    
    // 解析模式存储
    private val parsingPatterns = ConcurrentHashMap<String, MutableList<ParsingEvent>>()
    
    // 模式分析结果缓存
    private val analysisResults = ConcurrentHashMap<String, PatternAnalysisResult>()
    
    // 模式统计
    private val patternStatistics = ConcurrentHashMap<String, PatternStatistics>()
    
    // 分析配置
    private val analysisConfig = AtomicReference(AnalysisConfig())
    
    // 模式ID生成器
    private val patternIdGenerator = AtomicLong(0)
    
    /**
     * 记录解析事件
     */
    fun recordParsingEvent(event: ParsingEvent) {
        val patternKey = generatePatternKey(event)
        val events = parsingPatterns.computeIfAbsent(patternKey) { mutableListOf() }
        
        synchronized(events) {
            events.add(event)
            
            // 保持历史大小
            while (events.size > MAX_PATTERN_HISTORY) {
                events.removeAt(0)
            }
        }
        
        // 更新统计
        updatePatternStatistics(patternKey, event)
        
        // 检查是否需要重新分析
        if (shouldReanalyze(patternKey)) {
            analyzePattern(patternKey)
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "记录解析事件: 模式=$patternKey, 成功=${event.success}, 耗时=${event.durationMs}ms")
        }
    }
    
    /**
     * 获取模式分析结果
     */
    fun getPatternAnalysis(patternKey: String): PatternAnalysisResult? {
        return analysisResults[patternKey]
    }
    
    /**
     * 获取所有模式分析结果
     */
    fun getAllPatternAnalysis(): Map<String, PatternAnalysisResult> {
        return analysisResults.toMap()
    }
    
    /**
     * 获取模式统计
     */
    fun getPatternStatistics(patternKey: String): PatternStatistics? {
        return patternStatistics[patternKey]
    }
    
    /**
     * 获取所有模式统计
     */
    fun getAllPatternStatistics(): Map<String, PatternStatistics> {
        return patternStatistics.toMap()
    }
    
    /**
     * 获取常见错误模式
     */
    fun getCommonErrorPatterns(limit: Int = 10): List<ErrorPattern> {
        val errorPatterns = mutableListOf<ErrorPattern>()
        
        patternStatistics.forEach { (patternKey, stats) ->
            if (stats.errorRate > 0.1 && stats.totalEvents >= MIN_SAMPLES_FOR_ANALYSIS) {
                val events = parsingPatterns[patternKey] ?: emptyList()
                val errorEvents = events.filter { !it.success }
                
                // 分析错误类型分布
                val errorDistribution = errorEvents
                    .groupBy { it.errorType ?: ErrorType.UNKNOWN }
                    .mapValues { it.value.size }
                
                // 分析常见错误消息
                val commonErrorMessages = errorEvents
                    .mapNotNull { it.errorMessage }
                    .groupBy { it }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)
                    .map { it.first }
                
                errorPatterns.add(ErrorPattern(
                    patternKey = patternKey,
                    errorRate = stats.errorRate,
                    frequency = stats.totalEvents,
                    errorDistribution = errorDistribution,
                    commonErrorMessages = commonErrorMessages,
                    suggestedFixes = generateSuggestedFixes(patternKey, errorEvents)
                ))
            }
        }
        
        return errorPatterns
            .sortedByDescending { it.errorRate * it.frequency }
            .take(limit)
    }
    
    /**
     * 获取性能模式
     */
    fun getPerformancePatterns(limit: Int = 10): List<PerformancePattern> {
        val performancePatterns = mutableListOf<PerformancePattern>()
        
        patternStatistics.forEach { (patternKey, stats) ->
            if (stats.totalEvents >= MIN_SAMPLES_FOR_ANALYSIS) {
                val events = parsingPatterns[patternKey] ?: emptyList()
                
                // 分析性能分布
                val durations = events.map { it.durationMs }
                val avgDuration = durations.average()
                val p95Duration = durations.sorted()[(durations.size * 0.95).toInt()]
                val maxDuration = durations.maxOrNull() ?: 0L
                
                // 分析性能影响因素
                val performanceFactors = analyzePerformanceFactors(patternKey, events)
                
                performancePatterns.add(PerformancePattern(
                    patternKey = patternKey,
                    averageDurationMs = avgDuration.toLong(),
                    p95DurationMs = p95Duration,
                    maxDurationMs = maxDuration,
                    frequency = stats.totalEvents,
                    performanceFactors = performanceFactors,
                    optimizationSuggestions = generateOptimizationSuggestions(patternKey, events)
                ))
            }
        }
        
        return performancePatterns
            .sortedByDescending { it.averageDurationMs }
            .take(limit)
    }
    
    /**
     * 获取上下文模式
     */
    fun getContextPatterns(limit: Int = 10): List<ContextPattern> {
        val contextPatterns = mutableListOf<ContextPattern>()
        
        patternStatistics.forEach { (patternKey, stats) ->
            if (stats.totalEvents >= MIN_SAMPLES_FOR_ANALYSIS) {
                val events = parsingPatterns[patternKey] ?: emptyList()
                
                // 分析上下文分布
                val contextDistribution = events
                    .groupBy { it.context.operationType }
                    .mapValues { it.value.size }
                
                // 分析模型分布
                val modelDistribution = events
                    .groupBy { it.context.modelName }
                    .mapValues { it.value.size }
                
                // 分析数据大小分布
                val dataSizeRanges = events
                    .groupBy { 
                        when {
                            it.dataSize < 1024 -> "< 1KB"
                            it.dataSize < 5120 -> "1-5KB"
                            it.dataSize < 10240 -> "5-10KB"
                            else -> "> 10KB"
                        }
                    }
                    .mapValues { it.value.size }
                
                contextPatterns.add(ContextPattern(
                    patternKey = patternKey,
                    contextDistribution = contextDistribution,
                    modelDistribution = modelDistribution,
                    dataSizeDistribution = dataSizeRanges,
                    frequency = stats.totalEvents,
                    contextInsights = generateContextInsights(patternKey, events)
                ))
            }
        }
        
        return contextPatterns
            .sortedByDescending { it.frequency }
            .take(limit)
    }
    
    /**
     * 更新分析配置
     */
    fun updateAnalysisConfig(config: AnalysisConfig) {
        analysisConfig.set(config)
        Log.i(TAG, "更新分析配置: $config")
    }
    
    /**
     * 手动触发模式分析
     */
    fun triggerPatternAnalysis(patternKey: String) {
        analyzePattern(patternKey)
    }
    
    /**
     * 清理过期模式数据
     */
    fun cleanupExpiredPatterns() {
        val cutoffTime = System.currentTimeMillis() - PATTERN_RETENTION_PERIOD
        
        parsingPatterns.entries.removeIf { (_, events) ->
            synchronized(events) {
                events.removeIf { it.timestamp < cutoffTime }
                events.isEmpty()
            }
        }
        
        analysisResults.entries.removeIf { (_, result) ->
            result.analysisTimestamp < cutoffTime
        }
        
        Log.i(TAG, "清理过期模式数据完成")
    }
    
    /**
     * 重置所有模式数据
     */
    fun resetAllPatterns() {
        parsingPatterns.clear()
        analysisResults.clear()
        patternStatistics.clear()
        
        Log.i(TAG, "重置所有模式数据完成")
    }
    
    /**
     * 导出模式分析数据
     */
    fun exportPatternAnalysisData(): PatternAnalysisExport {
        return PatternAnalysisExport(
            patterns = parsingPatterns.toMap(),
            analysisResults = analysisResults.toMap(),
            statistics = patternStatistics.toMap(),
            exportTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 导入模式分析数据
     */
    fun importPatternAnalysisData(data: PatternAnalysisExport) {
        parsingPatterns.clear()
        parsingPatterns.putAll(data.patterns)
        
        analysisResults.clear()
        analysisResults.putAll(data.analysisResults)
        
        patternStatistics.clear()
        patternStatistics.putAll(data.statistics)
        
        Log.i(TAG, "导入模式分析数据完成，模式数量: ${data.patterns.size}")
    }
    
    /**
     * 生成模式键
     */
    private fun generatePatternKey(event: ParsingEvent): String {
        val config = analysisConfig.get()
        
        return when (config.patternKeyType) {
            PatternKeyType.OPERATION_TYPE -> event.context.operationType
            PatternKeyType.MODEL_NAME -> event.context.modelName
            PatternKeyType.ERROR_TYPE -> event.errorType?.name ?: "success"
            PatternKeyType.COMBINED -> "${event.context.operationType}_${event.context.modelName}_${event.errorType?.name ?: "success"}"
            PatternKeyType.DETAILED -> "${event.context.operationType}_${event.context.modelName}_${event.errorType?.name ?: "success"}_${event.dataSizeRange}"
        }
    }
    
    /**
     * 更新模式统计
     */
    private fun updatePatternStatistics(patternKey: String, event: ParsingEvent) {
        val stats = patternStatistics.computeIfAbsent(patternKey) {
            PatternStatistics()
        }
        
        stats.totalEvents++
        if (event.success) {
            stats.successfulEvents++
        } else {
            stats.failedEvents++
        }
        
        stats.totalDuration += event.durationMs
        stats.minDuration = minOf(stats.minDuration, event.durationMs)
        stats.maxDuration = maxOf(stats.maxDuration, event.durationMs)
        stats.lastUpdated = System.currentTimeMillis()
    }
    
    /**
     * 判断是否需要重新分析
     */
    private fun shouldReanalyze(patternKey: String): Boolean {
        val stats = patternStatistics[patternKey] ?: return false
        val lastAnalysis = analysisResults[patternKey]
        
        val config = analysisConfig.get()
        
        return stats.totalEvents >= config.minSamplesForAnalysis &&
               (lastAnalysis == null || 
                System.currentTimeMillis() - lastAnalysis.analysisTimestamp > config.reanalysisIntervalMs)
    }
    
    /**
     * 分析模式
     */
    private fun analyzePattern(patternKey: String) {
        val events = parsingPatterns[patternKey] ?: return
        val stats = patternStatistics[patternKey] ?: return
        
        synchronized(events) {
            if (events.size < MIN_SAMPLES_FOR_ANALYSIS) {
                return
            }
            
            val successEvents = events.filter { it.success }
            val errorEvents = events.filter { !it.success }
            
            // 分析错误模式
            val errorPatterns = analyzeErrorPatterns(errorEvents)
            
            // 分析性能模式
            val performancePatterns = analyzePerformancePatterns(events)
            
            // 分析上下文模式
            val contextPatterns = analyzeContextPatterns(events)
            
            // 生成建议
            val recommendations = generateRecommendations(patternKey, events, stats)
            
            val result = PatternAnalysisResult(
                patternKey = patternKey,
                totalEvents = stats.totalEvents,
                successRate = stats.successRate,
                averageDurationMs = stats.averageDurationMs,
                errorPatterns = errorPatterns,
                performancePatterns = performancePatterns,
                contextPatterns = contextPatterns,
                recommendations = recommendations,
                analysisTimestamp = System.currentTimeMillis()
            )
            
            analysisResults[patternKey] = result
            
            Log.i(TAG, "完成模式分析: $patternKey, 成功率=${String.format("%.2f%%", stats.successRate * 100)}")
        }
    }
    
    /**
     * 分析错误模式
     */
    private fun analyzeErrorPatterns(errorEvents: List<ParsingEvent>): List<ErrorPatternInfo> {
        val errorPatterns = mutableListOf<ErrorPatternInfo>()
        
        // 按错误类型分组
        val errorsByType = errorEvents.groupBy { it.errorType ?: ErrorType.UNKNOWN }
        
        errorsByType.forEach { (errorType, events) ->
            val frequency = events.size
            val commonMessages = events
                .mapNotNull { it.errorMessage }
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
            
            errorPatterns.add(ErrorPatternInfo(
                errorType = errorType,
                frequency = frequency,
                commonMessages = commonMessages,
                suggestedFixes = generateErrorFixes(errorType, events)
            ))
        }
        
        return errorPatterns
    }
    
    /**
     * 分析性能模式
     */
    private fun analyzePerformancePatterns(events: List<ParsingEvent>): List<PerformancePatternInfo> {
        val performancePatterns = mutableListOf<PerformancePatternInfo>()
        
        val durations = events.map { it.durationMs }
        val avgDuration = durations.average()
        val p95Duration = durations.sorted()[(durations.size * 0.95).toInt()]
        
        // 分析影响性能的因素
        val slowEvents = events.filter { it.durationMs > p95Duration }
        val fastEvents = events.filter { it.durationMs <= avgDuration }
        
        // 分析数据大小对性能的影响
        val dataSizeImpact = analyzeDataSizeImpact(events)
        
        // 分析上下文对性能的影响
        val contextImpact = analyzeContextImpact(events)
        
        performancePatterns.add(PerformancePatternInfo(
            averageDurationMs = avgDuration.toLong(),
            p95DurationMs = p95Duration,
            dataSizeImpact = dataSizeImpact,
            contextImpact = contextImpact,
            optimizationSuggestions = generatePerformanceOptimizations(slowEvents, fastEvents)
        ))
        
        return performancePatterns
    }
    
    /**
     * 分析上下文模式
     */
    private fun analyzeContextPatterns(events: List<ParsingEvent>): List<ContextPatternInfo> {
        val contextPatterns = mutableListOf<ContextPatternInfo>()
        
        // 分析操作类型分布
        val operationTypeDistribution = events
            .groupBy { it.context.operationType }
            .mapValues { it.value.size }
        
        // 分析模型分布
        val modelDistribution = events
            .groupBy { it.context.modelName }
            .mapValues { it.value.size }
        
        // 分析成功率的上下文相关性
        val successRateByContext = events
            .groupBy { it.context.operationType }
            .mapValues { events ->
                val successCount = events.count { it.success }
                successCount.toDouble() / events.size
            }
        
        contextPatterns.add(ContextPatternInfo(
            operationTypeDistribution = operationTypeDistribution,
            modelDistribution = modelDistribution,
            successRateByContext = successRateByContext,
            contextRecommendations = generateContextRecommendations(successRateByContext)
        ))
        
        return contextPatterns
    }
    
    /**
     * 生成建议
     */
    private fun generateRecommendations(
        patternKey: String,
        events: List<ParsingEvent>,
        stats: PatternStatistics
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        val successRate = stats.successRate
        val avgDuration = stats.averageDurationMs
        
        when {
            successRate < 0.8 -> {
                recommendations.add("成功率偏低，建议检查输入数据质量和解析策略")
                recommendations.add("考虑启用更严格的错误处理和降级策略")
            }
            
            avgDuration > 1000 -> {
                recommendations.add("解析耗时偏高，建议优化解析算法或增加缓存")
                recommendations.add("考虑使用更高效的JSON解析库")
            }
            
            stats.failedEvents > 10 -> {
                recommendations.add("失败次数较多，建议分析常见错误原因")
                recommendations.add("考虑添加更多的错误恢复机制")
            }
            
            else -> {
                recommendations.add("解析表现良好，继续保持当前配置")
            }
        }
        
        return recommendations
    }
    
    // 辅助分析方法
    private fun generateSuggestedFixes(patternKey: String, errorEvents: List<ParsingEvent>): List<String> {
        // 基于错误类型生成修复建议
        return errorEvents
            .groupBy { it.errorType }
            .mapNotNull { (errorType, events) ->
                when (errorType) {
                    ErrorType.JSON_SYNTAX_ERROR -> "增强JSON清洗和修复逻辑"
                    ErrorType.FIELD_MAPPING_ERROR -> "扩展字段映射配置"
                    ErrorType.PARSING_LOGIC_ERROR -> "优化解析策略和降级机制"
                    ErrorType.TIMEOUT_ERROR -> "增加超时处理和性能优化"
                    else -> null
                }
            }
            .distinct()
    }
    
    private fun analyzePerformanceFactors(patternKey: String, events: List<ParsingEvent>): Map<String, Double> {
        val factors = mutableMapOf<String, Double>()
        
        // 分析数据大小对性能的影响
        val dataSizeCorrelation = calculateCorrelation(
            events.map { it.dataSize.toDouble() },
            events.map { it.durationMs.toDouble() }
        )
        factors["dataSize"] = dataSizeCorrelation
        
        // 分析模型类型对性能的影响
        val modelPerformance = events
            .groupBy { it.context.modelName }
            .mapValues { events ->
                events.map { it.durationMs }.average()
            }
        
        val avgModelPerformance = modelPerformance.values.average()
        modelPerformance.forEach { (model, performance) ->
            factors["model_$model"] = performance / avgModelPerformance
        }
        
        return factors
    }
    
    private fun generateOptimizationSuggestions(patternKey: String, events: List<ParsingEvent>): List<String> {
        val suggestions = mutableListOf<String>()
        
        val avgDuration = events.map { it.durationMs }.average()
        val slowEvents = events.filter { it.durationMs > avgDuration * 1.5 }
        
        if (slowEvents.isNotEmpty()) {
            suggestions.add("识别并优化慢解析场景")
            suggestions.add("考虑为大数据量实现流式解析")
        }
        
        return suggestions
    }
    
    private fun generateContextInsights(patternKey: String, events: List<ParsingEvent>): List<String> {
        val insights = mutableListOf<String>()
        
        // 分析最成功的上下文组合
        val contextSuccessRates = events
            .groupBy { "${it.context.operationType}_${it.context.modelName}" }
            .mapValues { events ->
                val successCount = events.count { it.success }
                successCount.toDouble() / events.size
            }
        
        val bestContext = contextSuccessRates.maxByOrNull { it.value }
        if (bestContext != null) {
            insights.add("最佳上下文组合: ${bestContext.key} (成功率: ${String.format("%.2f%%", bestContext.value * 100)})")
        }
        
        return insights
    }
    
    private fun analyzeDataSizeImpact(events: List<ParsingEvent>): Map<String, Double> {
        val sizeGroups = events.groupBy {
            when {
                it.dataSize < 1024 -> "small"
                it.dataSize < 5120 -> "medium"
                else -> "large"
            }
        }
        
        return sizeGroups.mapValues { events ->
            events.map { it.durationMs }.average()
        }
    }
    
    private fun analyzeContextImpact(events: List<ParsingEvent>): Map<String, Double> {
        return events
            .groupBy { it.context.operationType }
            .mapValues { events ->
                events.map { it.durationMs }.average()
            }
    }
    
    private fun generateErrorFixes(errorType: ErrorType?, events: List<ParsingEvent>): List<String> {
        return when (errorType) {
            ErrorType.JSON_SYNTAX_ERROR -> listOf(
                "增强JSON清洗和修复逻辑",
                "添加更多格式错误检测",
                "实现更智能的括号匹配"
            )
            
            ErrorType.FIELD_MAPPING_ERROR -> listOf(
                "扩展字段映射配置",
                "启用模糊匹配",
                "实现动态字段学习"
            )
            
            else -> listOf("分析错误日志以确定根本原因")
        }
    }
    
    private fun generatePerformanceOptimizations(
        slowEvents: List<ParsingEvent>,
        fastEvents: List<ParsingEvent>
    ): List<String> {
        val optimizations = mutableListOf<String>()
        
        if (slowEvents.isNotEmpty()) {
            optimizations.add("分析慢解析场景的共同特征")
            optimizations.add("考虑为大数据量实现专门的优化路径")
        }
        
        if (fastEvents.isNotEmpty()) {
            optimizations.add("分析快解析场景的最佳实践")
            optimizations.add("尝试将成功经验应用到其他场景")
        }
        
        return optimizations
    }
    
    private fun generateContextRecommendations(successRateByContext: Map<String, Double>): List<String> {
        val recommendations = mutableListOf<String>()
        
        val bestContext = successRateByContext.maxByOrNull { it.value }
        val worstContext = successRateByContext.minByOrNull { it.value }
        
        if (bestContext != null && worstContext != null) {
            recommendations.add("最佳上下文: ${bestContext.key} (成功率: ${String.format("%.2f%%", bestContext.value * 100)})")
            recommendations.add("最差上下文: ${worstContext.key} (成功率: ${String.format("%.2f%%", worstContext.value * 100)})")
            recommendations.add("考虑将最佳实践应用到最差场景")
        }
        
        return recommendations
    }
    
    private fun calculateCorrelation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.isEmpty()) return 0.0
        
        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { it.first * it.second }
        val sumX2 = x.sumOf { it * it }
        val sumY2 = y.sumOf { it * it }
        
        val numerator = n * sumXY - sumX * sumY
        val denominator = kotlin.math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
        
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
    
    // 数据类定义
    data class ParsingEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val context: ParsingContext,
        val success: Boolean,
        val durationMs: Long,
        val dataSize: Int,
        val errorType: ErrorType? = null,
        val errorMessage: String? = null,
        val dataSizeRange: String = calculateDataSizeRange(dataSize)
    ) {
        companion object {
            private fun calculateDataSizeRange(dataSize: Int): String {
                return when {
                    dataSize < 1024 -> "< 1KB"
                    dataSize < 5120 -> "1-5KB"
                    dataSize < 10240 -> "5-10KB"
                    else -> "> 10KB"
                }
            }
        }
    }
    
    data class PatternStatistics(
        var totalEvents: Int = 0,
        var successfulEvents: Int = 0,
        var failedEvents: Int = 0,
        var totalDuration: Long = 0L,
        var minDuration: Long = Long.MAX_VALUE,
        var maxDuration: Long = 0L,
        var lastUpdated: Long = System.currentTimeMillis()
    ) {
        val successRate: Double
            get() = if (totalEvents > 0) successfulEvents.toDouble() / totalEvents else 0.0
        
        val averageDurationMs: Long
            get() = if (totalEvents > 0) totalDuration / totalEvents else 0L
    }
    
    data class PatternAnalysisResult(
        val patternKey: String,
        val totalEvents: Int,
        val successRate: Double,
        val averageDurationMs: Long,
        val errorPatterns: List<ErrorPatternInfo>,
        val performancePatterns: List<PerformancePatternInfo>,
        val contextPatterns: List<ContextPatternInfo>,
        val recommendations: List<String>,
        val analysisTimestamp: Long
    )
    
    data class ErrorPattern(
        val patternKey: String,
        val errorRate: Double,
        val frequency: Int,
        val errorDistribution: Map<ErrorType, Int>,
        val commonErrorMessages: List<String>,
        val suggestedFixes: List<String>
    )
    
    data class PerformancePattern(
        val patternKey: String,
        val averageDurationMs: Long,
        val p95DurationMs: Long,
        val maxDurationMs: Long,
        val frequency: Int,
        val performanceFactors: Map<String, Double>,
        val optimizationSuggestions: List<String>
    )
    
    data class ContextPattern(
        val patternKey: String,
        val contextDistribution: Map<String, Int>,
        val modelDistribution: Map<String, Int>,
        val dataSizeDistribution: Map<String, Int>,
        val frequency: Int,
        val contextInsights: List<String>
    )
    
    data class ErrorPatternInfo(
        val errorType: ErrorType,
        val frequency: Int,
        val commonMessages: List<String>,
        val suggestedFixes: List<String>
    )
    
    data class PerformancePatternInfo(
        val averageDurationMs: Double,
        val p95DurationMs: Long,
        val dataSizeImpact: Map<String, Double>,
        val contextImpact: Map<String, Double>,
        val optimizationSuggestions: List<String>
    )
    
    data class ContextPatternInfo(
        val operationTypeDistribution: Map<String, Int>,
        val modelDistribution: Map<String, Int>,
        val successRateByContext: Map<String, Double>,
        val contextRecommendations: List<String>
    )
    
    data class AnalysisConfig(
        val minSamplesForAnalysis: Int = MIN_SAMPLES_FOR_ANALYSIS,
        val reanalysisIntervalMs: Long = 60 * 60 * 1000L, // 1小时
        val patternKeyType: PatternKeyType = PatternKeyType.OPERATION_TYPE
    )
    
    data class PatternAnalysisExport(
        val patterns: Map<String, MutableList<ParsingEvent>>,
        val analysisResults: Map<String, PatternAnalysisResult>,
        val statistics: Map<String, PatternStatistics>,
        val exportTimestamp: Long
    )
    
    enum class PatternKeyType {
        OPERATION_TYPE,
        MODEL_NAME,
        ERROR_TYPE,
        COMBINED,
        DETAILED
    }
    
    enum class ErrorType {
        JSON_SYNTAX_ERROR,
        JSON_ENCODING_ERROR,
        FIELD_MAPPING_ERROR,
        PARSING_LOGIC_ERROR,
        TIMEOUT_ERROR,
        MEMORY_ERROR,
        NETWORK_ERROR,
        UNKNOWN
    }
    
    data class ParsingContext(
        val operationType: String,
        val modelName: String,
        val properties: Map<String, Any> = emptyMap()
    )
}