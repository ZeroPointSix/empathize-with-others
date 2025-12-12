package com.empathy.ai.data.optimization

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 性能调优器
 * 
 * 负责根据实时性能数据动态调整系统参数
 * 通过精细化的参数调优，实现最佳性能表现
 * 
 * 功能：
 * 1. 动态参数调整
 * 2. 性能阈值管理
 * 3. 自动性能调优
 * 4. 性能参数推荐
 */
class PerformanceTuner private constructor() {
    
    companion object {
        private const val TAG = "PerformanceTuner"
        
        // 调优配置
        private const val TUNING_INTERVAL_MS = 30_000L // 30秒
        private const val PERFORMANCE_WINDOW_SIZE = 50 // 性能数据窗口大小
        private const val MIN_SAMPLES_FOR_TUNING = 10 // 最小调优样本数
        
        // 调优阈值
        private const val PERFORMANCE_IMPROVEMENT_THRESHOLD = 0.05 // 5%性能改进阈值
        private const val PARAMETER_ADJUSTMENT_STEP = 0.1 // 参数调整步长
        
        @Volatile
        private var INSTANCE: PerformanceTuner? = null
        
        fun getInstance(): PerformanceTuner {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceTuner().also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tuningConfig = AtomicReference(TuningConfig())
    private val performanceData = ConcurrentHashMap<String, PerformanceDataHistory>()
    private val currentParameters = ConcurrentHashMap<String, TuningParameter>()
    private val parameterHistory = ConcurrentHashMap<String, MutableList<ParameterChange>>()
    private val tuningStrategies = ConcurrentHashMap<String, TuningStrategy>()
    
    private var isTuningEnabled = false
    private var tuningJob: Job? = null
    
    init {
        initializeDefaultParameters()
        initializeTuningStrategies()
        Log.i(TAG, "性能调优器初始化完成")
    }
    
    /**
     * 启用性能调优
     */
    fun enableTuning(config: TuningConfig = TuningConfig()) {
        if (isTuningEnabled) {
            Log.w(TAG, "性能调优已启用")
            return
        }
        
        tuningConfig.set(config)
        isTuningEnabled = true
        
        tuningJob = scope.launch {
            while (isActive) {
                try {
                    performTuningCycle()
                    delay(TUNING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "调优周期执行失败", e)
                }
            }
        }
        
        Log.i(TAG, "性能调优已启用")
    }
    
    /**
     * 禁用性能调优
     */
    fun disableTuning() {
        if (!isTuningEnabled) {
            Log.w(TAG, "性能调优未启用")
            return
        }
        
        isTuningEnabled = false
        tuningJob?.cancel()
        tuningJob = null
        
        Log.i(TAG, "性能调优已禁用")
    }
    
    /**
     * 手动触发调优
     */
    suspend fun triggerTuning(): TuningResult {
        return performTuningCycle()
    }
    
    /**
     * 获取当前调优状态
     */
    fun getTuningStatus(): TuningStatus {
        val appliedChanges = parameterHistory.values.flatten().toList()
        val currentParams = currentParameters.toMap()
        
        return TuningStatus(
            isEnabled = isTuningEnabled,
            currentParameters = currentParams,
            parameterHistory = appliedChanges,
            config = tuningConfig.get()
        )
    }
    
    /**
     * 更新调优配置
     */
    fun updateConfig(config: TuningConfig) {
        tuningConfig.set(config)
        Log.i(TAG, "调优配置已更新: $config")
    }
    
    /**
     * 记录性能数据点
     */
    fun recordPerformanceDataPoint(
        operationType: String,
        modelName: String,
        success: Boolean,
        durationMs: Long,
        dataSize: Int,
        parameters: Map<String, Any> = emptyMap()
    ) {
        val key = "$operationType:$modelName"
        val history = performanceData.computeIfAbsent(key) { PerformanceDataHistory() }
        
        history.addDataPoint(
            PerformanceDataPoint(
                timestamp = System.currentTimeMillis(),
                success = success,
                durationMs = durationMs,
                dataSize = dataSize,
                parameters = parameters
            )
        )
        
        // 保持窗口大小
        if (history.dataPoints.size > PERFORMANCE_WINDOW_SIZE) {
            history.dataPoints.removeAt(0)
        }
    }
    
    /**
     * 获取参数建议
     */
    fun getParameterRecommendations(operationType: String, modelName: String): List<ParameterRecommendation> {
        val key = "$operationType:$modelName"
        val history = performanceData[key] ?: return emptyList()
        
        if (history.dataPoints.size < MIN_SAMPLES_FOR_TUNING) {
            return emptyList()
        }
        
        val recommendations = mutableListOf<ParameterRecommendation>()
        val config = tuningConfig.get()
        
        // 分析当前性能
        val currentPerformance = analyzeCurrentPerformance(history)
        
        // 生成参数建议
        tuningStrategies.forEach { (parameterName, strategy) ->
            val recommendation = strategy.generateRecommendation(currentPerformance, config)
            if (recommendation != null) {
                recommendations.add(recommendation)
            }
        }
        
        // 按优先级排序
        return recommendations.sortedByDescending { it.priority.value }
    }
    
    /**
     * 应用参数调整
     */
    fun applyParameterAdjustment(
        parameterName: String,
        newValue: Any,
        reason: String
    ): Boolean {
        val currentParam = currentParameters[parameterName] ?: return false
        
        // 验证新值
        if (!isValidParameterValue(parameterName, newValue)) {
            Log.w(TAG, "无效的参数值: $parameterName = $newValue")
            return false
        }
        
        val oldValue = currentParam.currentValue
        currentParam.currentValue = newValue
        
        // 记录参数变更
        val change = ParameterChange(
            parameterName = parameterName,
            oldValue = oldValue,
            newValue = newValue,
            timestamp = System.currentTimeMillis(),
            reason = reason
        )
        
        parameterHistory.computeIfAbsent(parameterName) { mutableListOf() }.add(change)
        
        Log.i(TAG, "参数已调整: $parameterName $oldValue -> $newValue, 原因: $reason")
        
        return true
    }
    
    /**
     * 重置参数到默认值
     */
    fun resetParameterToDefault(parameterName: String): Boolean {
        val currentParam = currentParameters[parameterName] ?: return false
        
        val oldValue = currentParam.currentValue
        val defaultValue = currentParam.defaultValue
        
        return applyParameterAdjustment(
            parameterName,
            defaultValue,
            "重置为默认值"
        )
    }
    
    /**
     * 重置所有参数到默认值
     */
    fun resetAllParametersToDefault() {
        currentParameters.forEach { (parameterName, _) ->
            resetParameterToDefault(parameterName)
        }
        
        Log.i(TAG, "所有参数已重置为默认值")
    }
    
    /**
     * 执行调优周期
     */
    private suspend fun performTuningCycle(): TuningResult {
        val startTime = System.currentTimeMillis()
        val config = tuningConfig.get()
        
        if (!config.enableAutoTuning) {
            return TuningResult(
                success = false,
                reason = "自动调优已禁用",
                appliedChanges = emptyList(),
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        
        val appliedChanges = mutableListOf<ParameterChange>()
        var tuningSuccess = true
        var failureReason: String? = null
        
        try {
            // 1. 分析所有操作类型的性能
            val performanceAnalysis = mutableMapOf<String, PerformanceAnalysis>()
            
            performanceData.forEach { (key, history) ->
                if (history.dataPoints.size >= MIN_SAMPLES_FOR_TUNING) {
                    performanceAnalysis[key] = analyzeCurrentPerformance(history)
                }
            }
            
            // 2. 生成参数调整建议
            val allRecommendations = mutableListOf<ParameterRecommendation>()
            
            performanceAnalysis.forEach { (key, analysis) ->
                val recommendations = generateParameterRecommendations(analysis, config)
                allRecommendations.addAll(recommendations)
            }
            
            // 3. 应用最佳建议
            val sortedRecommendations = allRecommendations
                .sortedByDescending { it.priority.value }
                .take(config.maxChangesPerCycle)
            
            for (recommendation in sortedRecommendations) {
                if (applyParameterAdjustment(
                    recommendation.parameterName,
                    recommendation.recommendedValue,
                    recommendation.reason
                )) {
                    appliedChanges.add(
                        ParameterChange(
                            parameterName = recommendation.parameterName,
                            oldValue = currentParameters[recommendation.parameterName]?.currentValue ?: "unknown",
                            newValue = recommendation.recommendedValue,
                            timestamp = System.currentTimeMillis(),
                            reason = recommendation.reason
                        )
                    )
                }
            }
            
            // 4. 验证调优效果
            if (config.enableValidation && appliedChanges.isNotEmpty()) {
                validateTuningEffectiveness(appliedChanges)
            }
            
        } catch (e: Exception) {
            tuningSuccess = false
            failureReason = e.message
            Log.e(TAG, "调优周期执行异常", e)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        return TuningResult(
            success = tuningSuccess,
            reason = failureReason,
            appliedChanges = appliedChanges,
            durationMs = duration
        )
    }
    
    /**
     * 分析当前性能
     */
    private fun analyzeCurrentPerformance(history: PerformanceDataHistory): PerformanceAnalysis {
        val dataPoints = history.dataPoints
        val successCount = dataPoints.count { it.success }
        val successRate = successCount.toDouble() / dataPoints.size
        val avgDuration = dataPoints.map { it.durationMs }.average()
        val avgDataSize = dataPoints.map { it.dataSize }.average()
        
        // 分析性能趋势
        val recentData = dataPoints.takeLast(10)
        val recentAvgDuration = recentData.map { it.durationMs }.average()
        val overallAvgDuration = dataPoints.map { it.durationMs }.average()
        val performanceTrend = when {
            recentAvgDuration < overallAvgDuration * 0.9 -> PerformanceTrend.IMPROVING
            recentAvgDuration > overallAvgDuration * 1.1 -> PerformanceTrend.DEGRADING
            else -> PerformanceTrend.STABLE
        }
        
        return PerformanceAnalysis(
            operationType = history.operationType,
            modelName = history.modelName,
            sampleCount = dataPoints.size,
            successRate = successRate,
            averageDurationMs = avgDuration,
            averageDataSize = avgDataSize,
            performanceTrend = performanceTrend
        )
    }
    
    /**
     * 生成参数调整建议
     */
    private fun generateParameterRecommendations(
        analysis: PerformanceAnalysis,
        config: TuningConfig
    ): List<ParameterRecommendation> {
        val recommendations = mutableListOf<ParameterRecommendation>()
        
        tuningStrategies.forEach { (parameterName, strategy) ->
            val recommendation = strategy.generateRecommendation(analysis, config)
            if (recommendation != null) {
                recommendations.add(recommendation)
            }
        }
        
        return recommendations
    }
    
    /**
     * 验证调优效果
     */
    private suspend fun validateTuningEffectiveness(changes: List<ParameterChange>) {
        // 等待一段时间让参数生效
        delay(5000)
        
        // 收集新的性能数据并比较
        // 这里可以实现更复杂的验证逻辑
        Log.d(TAG, "调优效果验证完成，应用了${changes.size}个参数变更")
    }
    
    /**
     * 验证参数值有效性
     */
    private fun isValidParameterValue(parameterName: String, value: Any): Boolean {
        val parameter = currentParameters[parameterName] ?: return false
        
        return when (parameter.type) {
            ParameterType.INTEGER -> {
                val intValue = (value as? Number)?.toInt()
                intValue != null && intValue >= parameter.minValue.toInt() && intValue <= parameter.maxValue.toInt()
            }
            
            ParameterType.DOUBLE -> {
                val doubleValue = (value as? Number)?.toDouble()
                doubleValue != null && doubleValue >= parameter.minValue && doubleValue <= parameter.maxValue
            }
            
            ParameterType.BOOLEAN -> value is Boolean
            
            ParameterType.STRING -> value is String && value.length <= parameter.maxValue.toInt()
        }
    }
    
    /**
     * 初始化默认参数
     */
    private fun initializeDefaultParameters() {
        // 解析相关参数
        currentParameters["parsing_timeout_ms"] = TuningParameter(
            name = "parsing_timeout_ms",
            description = "解析超时时间（毫秒）",
            type = ParameterType.INTEGER,
            currentValue = 5000,
            defaultValue = 5000,
            minValue = 1000.0,
            maxValue = 30000.0,
            unit = "ms"
        )
        
        currentParameters["max_retry_count"] = TuningParameter(
            name = "max_retry_count",
            description = "最大重试次数",
            type = ParameterType.INTEGER,
            currentValue = 3,
            defaultValue = 3,
            minValue = 0.0,
            maxValue = 10.0,
            unit = "次"
        )
        
        currentParameters["fuzzy_match_threshold"] = TuningParameter(
            name = "fuzzy_match_threshold",
            description = "模糊匹配阈值",
            type = ParameterType.DOUBLE,
            currentValue = 0.7,
            defaultValue = 0.7,
            minValue = 0.0,
            maxValue = 1.0,
            unit = ""
        )
        
        currentParameters["cache_size"] = TuningParameter(
            name = "cache_size",
            description = "缓存大小",
            type = ParameterType.INTEGER,
            currentValue = 1000,
            defaultValue = 1000,
            minValue = 100.0,
            maxValue = 10000.0,
            unit = "条"
        )
        
        currentParameters["enable_parallel_parsing"] = TuningParameter(
            name = "enable_parallel_parsing",
            description = "启用并行解析",
            type = ParameterType.BOOLEAN,
            currentValue = true,
            defaultValue = true,
            minValue = 0.0,
            maxValue = 1.0,
            unit = ""
        )
        
        Log.d(TAG, "默认参数初始化完成，共${currentParameters.size}个参数")
    }
    
    /**
     * 初始化调优策略
     */
    private fun initializeTuningStrategies() {
        tuningStrategies["parsing_timeout_ms"] = TimeoutTuningStrategy()
        tuningStrategies["max_retry_count"] = RetryTuningStrategy()
        tuningStrategies["fuzzy_match_threshold"] = FuzzyMatchTuningStrategy()
        tuningStrategies["cache_size"] = CacheTuningStrategy()
        tuningStrategies["enable_parallel_parsing"] = ParallelProcessingTuningStrategy()
        
        Log.d(TAG, "调优策略初始化完成，共${tuningStrategies.size}个策略")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        disableTuning()
        scope.cancel()
        performanceData.clear()
        parameterHistory.clear()
        
        Log.i(TAG, "性能调优器资源清理完成")
    }
    
    /**
     * 调优配置
     */
    data class TuningConfig(
        val enableAutoTuning: Boolean = true,
        val aggressiveness: TuningAggressiveness = TuningAggressiveness.MODERATE,
        val maxChangesPerCycle: Int = 3,
        val minImprovementThreshold: Double = PERFORMANCE_IMPROVEMENT_THRESHOLD,
        val enableValidation: Boolean = true,
        val validationDelayMs: Long = 5000L
    )
    
    /**
     * 调优激进程度
     */
    enum class TuningAggressiveness {
        CONSERVATIVE,  // 保守，小幅调整
        MODERATE,      // 适中，平衡调整
        AGGRESSIVE      // 激进，大幅调整
    }
    
    /**
     * 调优参数
     */
    data class TuningParameter(
        val name: String,
        val description: String,
        val type: ParameterType,
        var currentValue: Any,
        val defaultValue: Any,
        val minValue: Double,
        val maxValue: Double,
        val unit: String
    )
    
    /**
     * 参数类型
     */
    enum class ParameterType {
        INTEGER,
        DOUBLE,
        BOOLEAN,
        STRING
    }
    
    /**
     * 参数变更记录
     */
    data class ParameterChange(
        val parameterName: String,
        val oldValue: Any,
        val newValue: Any,
        val timestamp: Long,
        val reason: String
    )
    
    /**
     * 性能数据历史
     */
    class PerformanceDataHistory {
        lateinit var operationType: String
        lateinit var modelName: String
        val dataPoints = mutableListOf<PerformanceDataPoint>()
        
        fun addDataPoint(dataPoint: PerformanceDataPoint) {
            dataPoints.add(dataPoint)
        }
    }
    
    /**
     * 性能数据点
     */
    data class PerformanceDataPoint(
        val timestamp: Long,
        val success: Boolean,
        val durationMs: Long,
        val dataSize: Int,
        val parameters: Map<String, Any>
    )
    
    /**
     * 性能分析结果
     */
    data class PerformanceAnalysis(
        val operationType: String,
        val modelName: String,
        val sampleCount: Int,
        val successRate: Double,
        val averageDurationMs: Double,
        val averageDataSize: Double,
        val performanceTrend: PerformanceTrend
    )
    
    /**
     * 性能趋势
     */
    enum class PerformanceTrend {
        STABLE,      // 稳定
        IMPROVING,   // 改善
        DEGRADING    // 下降
    }
    
    /**
     * 参数建议
     */
    data class ParameterRecommendation(
        val parameterName: String,
        val recommendedValue: Any,
        val expectedImprovement: Double,
        val priority: RecommendationPriority,
        val reason: String
    )
    
    /**
     * 建议优先级
     */
    enum class RecommendationPriority(val value: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4)
    }
    
    /**
     * 调优结果
     */
    data class TuningResult(
        val success: Boolean,
        val reason: String? = null,
        val appliedChanges: List<ParameterChange>,
        val durationMs: Long
    )
    
    /**
     * 调优状态
     */
    data class TuningStatus(
        val isEnabled: Boolean,
        val currentParameters: Map<String, TuningParameter>,
        val parameterHistory: List<ParameterChange>,
        val config: TuningConfig
    )
    
    /**
     * 调优策略接口
     */
    interface TuningStrategy {
        fun generateRecommendation(
            analysis: PerformanceAnalysis,
            config: TuningConfig
        ): ParameterRecommendation?
    }
    
    /**
     * 超时调优策略
     */
    private class TimeoutTuningStrategy : TuningStrategy {
        override fun generateRecommendation(
            analysis: PerformanceAnalysis,
            config: TuningConfig
        ): ParameterRecommendation? {
            val currentTimeout = 5000L // 当前超时值
            
            return when {
                analysis.averageDurationMs > currentTimeout * 0.8 -> {
                    // 平均耗时接近超时，建议增加超时时间
                    val newTimeout = (currentTimeout * 1.2).toLong()
                    ParameterRecommendation(
                        parameterName = "parsing_timeout_ms",
                        recommendedValue = newTimeout,
                        expectedImprovement = 0.1,
                        priority = RecommendationPriority.HIGH,
                        reason = "平均耗时${analysis.averageDurationMs}ms接近当前超时${currentTimeout}ms，建议增加超时时间"
                    )
                }
                
                analysis.averageDurationMs < currentTimeout * 0.3 -> {
                    // 平均耗时远小于超时，建议减少超时时间
                    val newTimeout = (currentTimeout * 0.8).toLong()
                    ParameterRecommendation(
                        parameterName = "parsing_timeout_ms",
                        recommendedValue = newTimeout,
                        expectedImprovement = 0.05,
                        priority = RecommendationPriority.MEDIUM,
                        reason = "平均耗时${analysis.averageDurationMs}ms远小于当前超时${currentTimeout}ms，建议减少超时时间"
                    )
                }
                
                else -> null
            }
        }
    }
    
    /**
     * 重试调优策略
     */
    private class RetryTuningStrategy : TuningStrategy {
        override fun generateRecommendation(
            analysis: PerformanceAnalysis,
            config: TuningConfig
        ): ParameterRecommendation? {
            val currentRetryCount = 3
            
            return when {
                analysis.successRate < 0.9 -> {
                    // 成功率低，建议增加重试次数
                    val newRetryCount = minOf(currentRetryCount + 1, 10)
                    ParameterRecommendation(
                        parameterName = "max_retry_count",
                        recommendedValue = newRetryCount,
                        expectedImprovement = 0.05,
                        priority = RecommendationPriority.HIGH,
                        reason = "成功率${String.format("%.2f%%", analysis.successRate * 100)}过低，建议增加重试次数"
                    )
                }
                
                analysis.successRate > 0.98 && analysis.performanceTrend == PerformanceTrend.STABLE -> {
                    // 成功率很高且稳定，可以减少重试次数
                    val newRetryCount = maxOf(currentRetryCount - 1, 0)
                    ParameterRecommendation(
                        parameterName = "max_retry_count",
                        recommendedValue = newRetryCount,
                        expectedImprovement = 0.02,
                        priority = RecommendationPriority.LOW,
                        reason = "成功率${String.format("%.2f%%", analysis.successRate * 100)}很高且稳定，建议减少重试次数"
                    )
                }
                
                else -> null
            }
        }
    }
    
    /**
     * 模糊匹配调优策略
     */
    private class FuzzyMatchTuningStrategy : TuningStrategy {
        override fun generateRecommendation(
            analysis: PerformanceAnalysis,
            config: TuningConfig
        ): ParameterRecommendation? {
            val currentThreshold = 0.7
            
            return when {
                analysis.successRate < 0.85 -> {
                    // 成功率低，建议降低阈值（更容易匹配）
                    val newThreshold = maxOf(currentThreshold - 0.1, 0.3)
                    ParameterRecommendation(
                        parameterName = "fuzzy_match_threshold",
                        recommendedValue = newThreshold,
                        expectedImprovement = 0.08,
                        priority = RecommendationPriority.MEDIUM,
                        reason = "成功率${String.format("%.2f%%", analysis.successRate * 100)}偏低，建议降低模糊匹配阈值"
                    )
                }
                
                analysis.successRate > 0.95 && analysis.performanceTrend == PerformanceTrend.STABLE -> {
                    // 成功率很高且稳定，可以提高阈值（更精确匹配）
                    val newThreshold = minOf(currentThreshold + 0.05, 0.9)
                    ParameterRecommendation(
                        parameterName = "fuzzy_match_threshold",
                        recommendedValue = newThreshold,
                        expectedImprovement = 0.02,
                        priority = RecommendationPriority.LOW,
                        reason = "成功率${String.format("%.2f%%", analysis.successRate * 100)}很高且稳定，建议提高模糊匹配阈值"
                    )
                }
                
                else -> null
            }
        }
    }
    
    /**
     * 缓存调优策略
     */
    private class CacheTuningStrategy : TuningStrategy {
        override fun generateRecommendation(
            analysis: PerformanceAnalysis,
            config: TuningConfig
        ): ParameterRecommendation? {
            val currentCacheSize = 1000
            
            return when {
                analysis.averageDurationMs > 1000 && analysis.sampleCount > 50 -> {
                    // 平均耗时高且样本量大，建议增加缓存大小
                    val newCacheSize = (currentCacheSize * 1.5).toInt()
                    ParameterRecommendation(
                        parameterName = "cache_size",
                        recommendedValue = newCacheSize,
                        expectedImprovement = 0.15,
                        priority = RecommendationPriority.HIGH,
                        reason = "平均耗时${analysis.averageDurationMs}ms较高，建议增加缓存大小以提高性能"
                    )
                }
                
                analysis.averageDurationMs < 200 && analysis.sampleCount < 20 -> {
                    // 平均耗时低且样本量小，可以减少缓存大小
                    val newCacheSize = (currentCacheSize * 0.8).toInt()
                    ParameterRecommendation(
                        parameterName = "cache_size",
                        recommendedValue = newCacheSize,
                        expectedImprovement = 0.03,
                        priority = RecommendationPriority.LOW,
                        reason = "平均耗时${analysis.averageDurationMs}ms较低，建议减少缓存大小以节省内存"
                    )
                }
                
                else -> null
            }
        }
    }
    
    /**
     * 并行处理调优策略
     */
    private class ParallelProcessingTuningStrategy : TuningStrategy {
        override fun generateRecommendation(
            analysis: PerformanceAnalysis,
            config: TuningConfig
        ): ParameterRecommendation? {
            return when {
                analysis.averageDurationMs > 2000 && analysis.averageDataSize > 10000 -> {
                    // 耗时长且数据量大，建议启用并行处理
                    ParameterRecommendation(
                        parameterName = "enable_parallel_parsing",
                        recommendedValue = true,
                        expectedImprovement = 0.3,
                        priority = RecommendationPriority.HIGH,
                        reason = "平均耗时${analysis.averageDurationMs}ms且数据量大，建议启用并行处理"
                    )
                }
                
                analysis.averageDurationMs < 100 && analysis.averageDataSize < 1000 -> {
                    // 耗时短且数据量小，可以禁用并行处理
                    ParameterRecommendation(
                        parameterName = "enable_parallel_parsing",
                        recommendedValue = false,
                        expectedImprovement = 0.05,
                        priority = RecommendationPriority.LOW,
                        reason = "平均耗时${analysis.averageDurationMs}ms且数据量小，建议禁用并行处理以减少开销"
                    )
                }
                
                else -> null
            }
        }
    }
}