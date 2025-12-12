package com.empathy.ai.data.improvement

import android.util.Log
import com.empathy.ai.data.monitoring.AiResponseParserMetrics
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 性能回归检测器
 * 
 * 负责监控系统性能变化，及时发现性能回归
 * 通过统计分析算法，识别显著的性能下降
 * 
 * 功能：
 * 1. 实时性能监控
 * 2. 回归检测算法
 * 3. 回归告警机制
 * 4. 回归分析报告
 */
class PerformanceRegressionDetector private constructor(
    private val metrics: AiResponseParserMetrics
) {
    
    companion object {
        private const val TAG = "PerformanceRegressionDetector"
        
        // 检测配置
        private const val DETECTION_INTERVAL_MS = 30_000L // 30秒
        private const val BASELINE_WINDOW_SIZE = 100 // 基线窗口大小
        private const val DETECTION_WINDOW_SIZE = 20 // 检测窗口大小
        private const val MIN_SAMPLES_FOR_DETECTION = 30 // 最小检测样本数
        
        // 回归阈值
        private const val SUCCESS_RATE_REGRESSION_THRESHOLD = -0.05 // -5%成功率回归阈值
        private const val PERFORMANCE_REGRESSION_THRESHOLD = -0.1 // -10%性能回归阈值
        
        @Volatile
        private var INSTANCE: PerformanceRegressionDetector? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): PerformanceRegressionDetector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceRegressionDetector(metrics).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val detectionConfig = AtomicReference(DetectionConfig())
    private val performanceHistory = ConcurrentHashMap<String, PerformanceHistory>()
    private val baselineMetrics = AtomicReference<BaselineMetrics?>()
    private val regressionHistory = ConcurrentHashMap<String, MutableList<PerformanceRegression>>()
    private val detectionAlgorithms = ConcurrentHashMap<String, DetectionAlgorithm>()
    
    private var isDetectionEnabled = false
    private var detectionJob: Job? = null
    private val regressionCounter = AtomicLong(0)
    
    init {
        initializeDetectionAlgorithms()
        Log.i(TAG, "性能回归检测器初始化完成")
    }
    
    /**
     * 启用回归检测
     */
    fun enableDetection(config: DetectionConfig = DetectionConfig()) {
        if (isDetectionEnabled) {
            Log.w(TAG, "回归检测已启用")
            return
        }
        
        detectionConfig.set(config)
        isDetectionEnabled = true
        
        // 初始化基线
        initializeBaseline()
        
        detectionJob = scope.launch {
            while (isActive) {
                try {
                    performDetectionCycle()
                    delay(DETECTION_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "检测周期执行失败", e)
                }
            }
        }
        
        Log.i(TAG, "性能回归检测已启用")
    }
    
    /**
     * 禁用回归检测
     */
    fun disableDetection() {
        if (!isDetectionEnabled) {
            Log.w(TAG, "回归检测未启用")
            return
        }
        
        isDetectionEnabled = false
        detectionJob?.cancel()
        detectionJob = null
        
        Log.i(TAG, "性能回归检测已禁用")
    }
    
    /**
     * 手动触发检测
     */
    suspend fun triggerDetection(): DetectionResult {
        return performDetectionCycle()
    }
    
    /**
     * 获取回归状态
     */
    fun getRegressionStatus(): RegressionStatus {
        val currentBaseline = baselineMetrics.get()
        val recentRegressions = getRecentRegressions(10)
        val detectionAlgorithmsStatus = detectionAlgorithms.values.map { it.getStatus() }
        
        return RegressionStatus(
            isEnabled = isDetectionEnabled,
            baselineMetrics = currentBaseline,
            recentRegressions = recentRegressions,
            detectionAlgorithmsStatus = detectionAlgorithmsStatus,
            config = detectionConfig.get()
        )
    }
    
    /**
     * 更新检测配置
     */
    fun updateConfig(config: DetectionConfig) {
        detectionConfig.set(config)
        Log.i(TAG, "检测配置已更新: $config")
    }
    
    /**
     * 获取回归分析
     */
    fun analyzeRegressions(): RegressionAnalysis {
        val allRegressions = regressionHistory.values.flatten()
        val recentRegressions = getRecentRegressions(50)
        
        // 按类型分组
        val regressionsByType = recentRegressions.groupBy { it.type }
        
        // 计算回归统计
        val totalRegressions = allRegressions.size
        val regressionRate = if (totalRegressions > 0) {
            val timeSpan = System.currentTimeMillis() - (allRegressions.firstOrNull()?.timestamp ?: System.currentTimeMillis())
            val daysSpan = timeSpan / (24 * 60 * 60 * 1000)
            if (daysSpan > 0) totalRegressions.toDouble() / daysSpan else 0.0
        } else 0.0
        
        val averageSeverity = recentRegressions.map { it.severity.value }.average()
        val mostCommonType = regressionsByType.maxByOrNull { it.value.size }?.key
        
        // 计算回归趋势
        val regressionTrend = calculateRegressionTrend(recentRegressions)
        
        return RegressionAnalysis(
            totalRegressions = totalRegressions,
            regressionRate = regressionRate,
            averageSeverity = averageSeverity,
            mostCommonType = mostCommonType,
            regressionsByType = regressionsByType,
            regressionTrend = regressionTrend,
            recommendations = generateRegressionRecommendations(regressionsByType)
        )
    }
    
    /**
     * 检测性能回归
     */
    suspend fun detectRegressions(systemState: Any? = null): List<PerformanceRegression> {
        if (!isDetectionEnabled) {
            return emptyList()
        }
        
        val detectedRegressions = mutableListOf<PerformanceRegression>()
        val config = detectionConfig.get()
        
        // 1. 收集当前性能指标
        val currentMetrics = collectCurrentMetrics()
        
        // 2. 更新性能历史
        updatePerformanceHistory(currentMetrics)
        
        // 3. 获取基线指标
        val baseline = baselineMetrics.get()
        if (baseline == null) {
            Log.w(TAG, "基线指标未建立，无法检测回归")
            return emptyList()
        }
        
        // 4. 应用检测算法
        detectionAlgorithms.values.forEach { algorithm ->
            if (algorithm.isEnabled(config)) {
                val regressions = algorithm.detectRegressions(currentMetrics, baseline, config)
                detectedRegressions.addAll(regressions)
            }
        }
        
        // 5. 过滤和验证回归
        val validRegressions = detectedRegressions.filter { regression ->
            isValidRegression(regression, config)
        }
        
        // 6. 记录回归
        validRegressions.forEach { regression ->
            recordRegression(regression)
        }
        
        // 7. 更新基线（如果需要）
        if (config.enableBaselineUpdate && shouldUpdateBaseline(validRegressions, config)) {
            updateBaseline(currentMetrics)
        }
        
        return validRegressions
    }
    
    /**
     * 收集当前性能指标
     */
    private fun collectCurrentMetrics(): CurrentMetrics {
        val overallMetrics = metrics.getOverallMetrics()
        val performanceSummary = metrics.getPerformanceSummary()
        
        return CurrentMetrics(
            timestamp = System.currentTimeMillis(),
            successRate = overallMetrics.successRate,
            averageResponseTime = overallMetrics.averageParseTimeMs,
            throughput = calculateThroughput(overallMetrics),
            errorRate = 1.0 - overallMetrics.successRate,
            resourceUtilization = calculateResourceUtilization()
        )
    }
    
    /**
     * 更新性能历史
     */
    private fun updatePerformanceHistory(currentMetrics: CurrentMetrics) {
        val history = performanceHistory.computeIfAbsent("overall") { PerformanceHistory() }
        history.addMetrics(currentMetrics)
        
        // 保持历史大小
        if (history.metrics.size > BASELINE_WINDOW_SIZE) {
            history.metrics.removeAt(0)
        }
    }
    
    /**
     * 初始化基线
     */
    private fun initializeBaseline() {
        val history = performanceHistory["overall"]
        if (history != null && history.metrics.size >= MIN_SAMPLES_FOR_DETECTION) {
            val baseline = calculateBaseline(history.metrics)
            baselineMetrics.set(baseline)
            
            Log.i(TAG, "基线指标已建立: $baseline")
        }
    }
    
    /**
     * 计算基线
     */
    private fun calculateBaseline(metrics: List<CurrentMetrics>): BaselineMetrics {
        val successRateBaseline = metrics.map { it.successRate }.average()
        val responseTimeBaseline = metrics.map { it.averageResponseTime }.average()
        val throughputBaseline = metrics.map { it.throughput }.average()
        val errorRateBaseline = metrics.map { it.errorRate }.average()
        val resourceUtilizationBaseline = metrics.map { it.resourceUtilization }.average()
        
        return BaselineMetrics(
            timestamp = System.currentTimeMillis(),
            sampleCount = metrics.size,
            successRateBaseline = successRateBaseline,
            responseTimeBaseline = responseTimeBaseline,
            throughputBaseline = throughputBaseline,
            errorRateBaseline = errorRateBaseline,
            resourceUtilizationBaseline = resourceUtilizationBaseline
        )
    }
    
    /**
     * 更新基线
     */
    private fun updateBaseline(currentMetrics: CurrentMetrics) {
        val history = performanceHistory["overall"] ?: return
        val newBaseline = calculateBaseline(history.metrics)
        baselineMetrics.set(newBaseline)
        
        Log.i(TAG, "基线指标已更新: $newBaseline")
    }
    
    /**
     * 验证回归有效性
     */
    private fun isValidRegression(
        regression: PerformanceRegression,
        config: DetectionConfig
    ): Boolean {
        // 1. 检查回归幅度是否超过阈值
        when (regression.type) {
            RegressionType.SUCCESS_RATE -> {
                if (regression.regressionPercent > SUCCESS_RATE_REGRESSION_THRESHOLD) {
                    return false
                }
            }
            
            RegressionType.PERFORMANCE -> {
                if (regression.regressionPercent > PERFORMANCE_REGRESSION_THRESHOLD) {
                    return false
                }
            }
        }
        
        // 2. 检查回归持续时间
        val minDuration = config.minRegressionDurationMs
        if (System.currentTimeMillis() - regression.timestamp < minDuration) {
            return false
        }
        
        // 3. 检查回归置信度
        if (regression.confidence < config.minConfidenceThreshold) {
            return false
        }
        
        return true
    }
    
    /**
     * 判断是否应该更新基线
     */
    private fun shouldUpdateBaseline(
        regressions: List<PerformanceRegression>,
        config: DetectionConfig
    ): Boolean {
        if (regressions.isEmpty()) {
            return false
        }
        
        // 如果没有严重回归且基线较旧，则更新基线
        val currentBaseline = baselineMetrics.get()
        if (currentBaseline != null) {
            val baselineAge = System.currentTimeMillis() - currentBaseline.timestamp
            val maxBaselineAge = config.maxBaselineAgeMs
            
            val hasSevereRegression = regressions.any { it.severity == RegressionSeverity.SEVERE }
            
            return baselineAge > maxBaselineAge && !hasSevereRegression
        }
        
        return false
    }
    
    /**
     * 记录回归
     */
    private fun recordRegression(regression: PerformanceRegression) {
        val regressionId = "reg_${regressionCounter.incrementAndGet()}_${System.currentTimeMillis()}"
        val regressionWithId = regression.copy(id = regressionId)
        
        val history = regressionHistory.computeIfAbsent(regression.type.name) { mutableListOf() }
        history.add(regressionWithId)
        
        // 保持历史大小
        if (history.size > 1000) {
            history.removeAt(0)
        }
        
        Log.w(TAG, "检测到性能回归: $regressionWithId")
    }
    
    /**
     * 获取最近的回归
     */
    private fun getRecentRegressions(count: Int): List<PerformanceRegression> {
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 最近24小时
        
        return regressionHistory.values.flatten()
            .filter { it.timestamp >= cutoffTime }
            .sortedByDescending { it.timestamp }
            .take(count)
    }
    
    /**
     * 计算吞吐量
     */
    private fun calculateThroughput(overallMetrics: AiResponseParserMetrics.OverallMetrics): Double {
        // 简化的吞吐量计算：每秒处理的请求数
        return overallMetrics.totalRequests.toDouble() / (overallMetrics.averageParseTimeMs / 1000.0)
    }
    
    /**
     * 计算资源利用率
     */
    private fun calculateResourceUtilization(): Double {
        // 简化的资源利用率计算
        // 在实际应用中，这里可以从ResourceMonitor获取详细数据
        return 0.5 // 假设50%利用率
    }
    
    /**
     * 计算回归趋势
     */
    private fun calculateRegressionTrend(recentRegressions: List<PerformanceRegression>): RegressionTrend {
        if (recentRegressions.size < 2) {
            return RegressionTrend.STABLE
        }
        
        val timeSpan = recentRegressions.last().timestamp - recentRegressions.first().timestamp
        val daysSpan = timeSpan / (24 * 60 * 60 * 1000)
        
        if (daysSpan <= 0) {
            return RegressionTrend.STABLE
        }
        
        val regressionRate = recentRegressions.size.toDouble() / daysSpan
        
        return when {
            regressionRate > 1.0 -> RegressionTrend.INCREASING
            regressionRate < 0.1 -> RegressionTrend.DECREASING
            else -> RegressionTrend.STABLE
        }
    }
    
    /**
     * 生成回归建议
     */
    private fun generateRegressionRecommendations(
        regressionsByType: Map<RegressionType, List<PerformanceRegression>>
    ): List<RegressionRecommendation> {
        val recommendations = mutableListOf<RegressionRecommendation>()
        
        regressionsByType.forEach { (type, regressions) ->
            val recentRegressions = regressions.takeLast(5)
            
            when (type) {
                RegressionType.SUCCESS_RATE -> {
                    recommendations.add(RegressionRecommendation(
                        type = RegressionRecommendationType.IMPROVE_ERROR_HANDLING,
                        priority = RecommendationPriority.HIGH,
                        description = "成功率频繁回归，建议改进错误处理机制",
                        action = "增强错误处理和重试策略"
                    ))
                }
                
                RegressionType.PERFORMANCE -> {
                    recommendations.add(RegressionRecommendation(
                        type = RegressionRecommendationType.OPTIMIZE_PERFORMANCE,
                        priority = RecommendationPriority.MEDIUM,
                        description = "性能频繁回归，建议优化解析算法",
                        action = "优化热点代码和算法效率"
                    ))
                }
                
                RegressionType.MEMORY -> {
                    recommendations.add(RegressionRecommendation(
                        type = RegressionRecommendationType.OPTIMIZE_MEMORY_USAGE,
                        priority = RecommendationPriority.MEDIUM,
                        description = "内存使用频繁回归，建议优化内存管理",
                        action = "优化内存分配和垃圾回收策略"
                    ))
                }
            }
        }
        
        return recommendations.sortedByDescending { it.priority.value }
    }
    
    /**
     * 执行检测周期
     */
    private suspend fun performDetectionCycle(): DetectionResult {
        val startTime = System.currentTimeMillis()
        val config = detectionConfig.get()
        
        if (!config.enableAutomaticDetection) {
            return DetectionResult(
                success = false,
                reason = "自动检测已禁用",
                detectedRegressions = emptyList(),
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        
        val detectedRegressions = mutableListOf<PerformanceRegression>()
        var detectionSuccess = true
        var failureReason: String? = null
        
        try {
            // 1. 收集当前指标
            val currentMetrics = collectCurrentMetrics()
            
            // 2. 更新历史
            updatePerformanceHistory(currentMetrics)
            
            // 3. 检测回归
            val regressions = detectRegressions()
            detectedRegressions.addAll(regressions)
            
            // 4. 更新基线
            if (config.enableBaselineUpdate && shouldUpdateBaseline(regressions, config)) {
                updateBaseline(currentMetrics)
            }
            
        } catch (e: Exception) {
            detectionSuccess = false
            failureReason = e.message
            Log.e(TAG, "检测周期执行异常", e)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        return DetectionResult(
            success = detectionSuccess,
            reason = failureReason,
            detectedRegressions = detectedRegressions,
            durationMs = duration
        )
    }
    
    /**
     * 初始化检测算法
     */
    private fun initializeDetectionAlgorithms() {
        detectionAlgorithms["statistical"] = StatisticalDetectionAlgorithm()
        detectionAlgorithms["trend"] = TrendDetectionAlgorithm()
        detectionAlgorithms["threshold"] = ThresholdDetectionAlgorithm()
        
        Log.d(TAG, "检测算法初始化完成，共${detectionAlgorithms.size}个算法")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        disableDetection()
        scope.cancel()
        performanceHistory.clear()
        regressionHistory.clear()
        
        Log.i(TAG, "性能回归检测器资源清理完成")
    }
    
    /**
     * 检测配置
     */
    data class DetectionConfig(
        val enableAutomaticDetection: Boolean = true,
        val enableBaselineUpdate: Boolean = true,
        val minRegressionDurationMs: Long = 60_000L, // 1分钟
        val maxBaselineAgeMs: Long = 7 * 24 * 60 * 60 * 1000L, // 7天
        val minConfidenceThreshold: Double = 0.8,
        val statisticalThreshold: Double = 2.0, // 2个标准差
        val trendThreshold: Double = 0.05, // 5%趋势阈值
        val enableMultipleAlgorithms: Boolean = true
    )
    
    /**
     * 回归类型
     */
    enum class RegressionType {
        SUCCESS_RATE,
        PERFORMANCE,
        MEMORY,
        CPU,
        ERROR_RATE,
        THROUGHPUT
    }
    
    /**
     * 回归严重程度
     */
    enum class RegressionSeverity(val value: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        SEVERE(4)
    }
    
    /**
     * 当前指标
     */
    data class CurrentMetrics(
        val timestamp: Long,
        val successRate: Double,
        val averageResponseTime: Double,
        val throughput: Double,
        val errorRate: Double,
        val resourceUtilization: Double
    )
    
    /**
     * 基线指标
     */
    data class BaselineMetrics(
        val timestamp: Long,
        val sampleCount: Int,
        val successRateBaseline: Double,
        val responseTimeBaseline: Double,
        val throughputBaseline: Double,
        val errorRateBaseline: Double,
        val resourceUtilizationBaseline: Double
    )
    
    /**
     * 性能历史
     */
    class PerformanceHistory {
        val metrics = mutableListOf<CurrentMetrics>()
        
        fun addMetrics(metric: CurrentMetrics) {
            metrics.add(metric)
        }
    }
    
    /**
     * 性能回归
     */
    data class PerformanceRegression(
        val id: String = "",
        val type: RegressionType,
        val severity: RegressionSeverity,
        val timestamp: Long,
        val regressionPercent: Double,
        val confidence: Double,
        val description: String,
        val currentValue: Double,
        val baselineValue: Double,
        val affectedMetrics: List<String>
    )
    
    /**
     * 回归趋势
     */
    enum class RegressionTrend {
        STABLE,      // 稳定
        INCREASING,  // 增加
        DECREASING   // 减少
    }
    
    /**
     * 回归状态
     */
    data class RegressionStatus(
        val isEnabled: Boolean,
        val baselineMetrics: BaselineMetrics?,
        val recentRegressions: List<PerformanceRegression>,
        val detectionAlgorithmsStatus: List<AlgorithmStatus>,
        val config: DetectionConfig
    )
    
    /**
     * 回归分析
     */
    data class RegressionAnalysis(
        val totalRegressions: Int,
        val regressionRate: Double,
        val averageSeverity: Double,
        val mostCommonType: RegressionType?,
        val regressionsByType: Map<RegressionType, List<PerformanceRegression>>,
        val regressionTrend: RegressionTrend,
        val recommendations: List<RegressionRecommendation>
    )
    
    /**
     * 回归建议类型
     */
    enum class RegressionRecommendationType {
        IMPROVE_ERROR_HANDLING,
        OPTIMIZE_PERFORMANCE,
        OPTIMIZE_MEMORY_USAGE,
        UPDATE_BASELINE,
        ADJUST_THRESHOLDS
    }
    
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
     * 回归建议
     */
    data class RegressionRecommendation(
        val type: RegressionRecommendationType,
        val priority: RecommendationPriority,
        val description: String,
        val action: String
    )
    
    /**
     * 检测结果
     */
    data class DetectionResult(
        val success: Boolean,
        val reason: String? = null,
        val detectedRegressions: List<PerformanceRegression>,
        val durationMs: Long
    )
    
    /**
     * 算法状态
     */
    data class AlgorithmStatus(
        val name: String,
        val isEnabled: Boolean,
        val lastExecutionTime: Long,
        val lastDetectionCount: Int
    )
    
    /**
     * 检测算法接口
     */
    interface DetectionAlgorithm {
        fun isEnabled(config: DetectionConfig): Boolean
        fun detectRegressions(
            currentMetrics: CurrentMetrics,
            baseline: BaselineMetrics,
            config: DetectionConfig
        ): List<PerformanceRegression>
        fun getStatus(): AlgorithmStatus
    }
    
    /**
     * 统计检测算法
     */
    private class StatisticalDetectionAlgorithm : DetectionAlgorithm {
        private var lastExecutionTime = 0L
        private var lastDetectionCount = 0
        
        override fun isEnabled(config: DetectionConfig): Boolean {
            return config.enableMultipleAlgorithms
        }
        
        override fun detectRegressions(
            currentMetrics: CurrentMetrics,
            baseline: BaselineMetrics,
            config: DetectionConfig
        ): List<PerformanceRegression> {
            val regressions = mutableListOf<PerformanceRegression>()
            
            // 成功率回归检测
            val successRateDiff = currentMetrics.successRate - baseline.successRateBaseline
            val successRateStdDev = calculateStandardDeviation(listOf(currentMetrics.successRate, baseline.successRateBaseline))
            
            if (abs(successRateDiff) > config.statisticalThreshold * successRateStdDev) {
                regressions.add(PerformanceRegression(
                    type = RegressionType.SUCCESS_RATE,
                    severity = determineSeverity(abs(successRateDiff)),
                    timestamp = currentMetrics.timestamp,
                    regressionPercent = successRateDiff / baseline.successRateBaseline,
                    confidence = calculateConfidence(abs(successRateDiff), successRateStdDev),
                    description = "成功率统计回归: ${String.format("%.2f%%", successRateDiff * 100)}",
                    currentValue = currentMetrics.successRate,
                    baselineValue = baseline.successRateBaseline,
                    affectedMetrics = listOf("successRate")
                ))
            }
            
            // 性能回归检测
            val responseTimeDiff = currentMetrics.averageResponseTime - baseline.responseTimeBaseline
            val responseTimeStdDev = calculateStandardDeviation(listOf(currentMetrics.averageResponseTime, baseline.responseTimeBaseline))
            
            if (responseTimeDiff > config.statisticalThreshold * responseTimeStdDev) {
                regressions.add(PerformanceRegression(
                    type = RegressionType.PERFORMANCE,
                    severity = determineSeverity(responseTimeDiff / baseline.responseTimeBaseline),
                    timestamp = currentMetrics.timestamp,
                    regressionPercent = responseTimeDiff / baseline.responseTimeBaseline,
                    confidence = calculateConfidence(responseTimeDiff, responseTimeStdDev),
                    description = "响应时间统计回归: ${String.format("%.0fms", responseTimeDiff)}",
                    currentValue = currentMetrics.averageResponseTime,
                    baselineValue = baseline.responseTimeBaseline,
                    affectedMetrics = listOf("averageResponseTime")
                ))
            }
            
            lastExecutionTime = System.currentTimeMillis()
            lastDetectionCount = regressions.size
            
            return regressions
        }
        
        override fun getStatus(): AlgorithmStatus {
            return AlgorithmStatus(
                name = "statistical",
                isEnabled = true,
                lastExecutionTime = lastExecutionTime,
                lastDetectionCount = lastDetectionCount
            )
        }
        
        private fun calculateStandardDeviation(values: List<Double>): Double {
            val mean = values.average()
            val variance = values.map { (it - mean) * (it - mean) }.average()
            return kotlin.math.sqrt(variance)
        }
        
        private fun determineSeverity(deviation: Double): RegressionSeverity {
            return when {
                deviation > 0.2 -> RegressionSeverity.SEVERE
                deviation > 0.1 -> RegressionSeverity.HIGH
                deviation > 0.05 -> RegressionSeverity.MEDIUM
                else -> RegressionSeverity.LOW
            }
        }
        
        private fun calculateConfidence(deviation: Double, stdDev: Double): Double {
            return if (stdDev > 0) minOf(deviation / stdDev, 1.0) else 0.0
        }
    }
    
    /**
     * 趋势检测算法
     */
    private class TrendDetectionAlgorithm : DetectionAlgorithm {
        private var lastExecutionTime = 0L
        private var lastDetectionCount = 0
        
        override fun isEnabled(config: DetectionConfig): Boolean {
            return config.enableMultipleAlgorithms
        }
        
        override fun detectRegressions(
            currentMetrics: CurrentMetrics,
            baseline: BaselineMetrics,
            config: DetectionConfig
        ): List<PerformanceRegression> {
            // 简化的趋势检测
            // 在实际应用中，这里需要更多的历史数据来计算趋势
            return emptyList()
        }
        
        override fun getStatus(): AlgorithmStatus {
            return AlgorithmStatus(
                name = "trend",
                isEnabled = true,
                lastExecutionTime = lastExecutionTime,
                lastDetectionCount = lastDetectionCount
            )
        }
    }
    
    /**
     * 阈值检测算法
     */
    private class ThresholdDetectionAlgorithm : DetectionAlgorithm {
        private var lastExecutionTime = 0L
        private var lastDetectionCount = 0
        
        override fun isEnabled(config: DetectionConfig): Boolean {
            return config.enableMultipleAlgorithms
        }
        
        override fun detectRegressions(
            currentMetrics: CurrentMetrics,
            baseline: BaselineMetrics,
            config: DetectionConfig
        ): List<PerformanceRegression> {
            val regressions = mutableListOf<PerformanceRegression>()
            
            // 成功率阈值检测
            val successRateRegression = currentMetrics.successRate - baseline.successRateBaseline
            if (successRateRegression < -config.trendThreshold) {
                regressions.add(PerformanceRegression(
                    type = RegressionType.SUCCESS_RATE,
                    severity = RegressionSeverity.MEDIUM,
                    timestamp = currentMetrics.timestamp,
                    regressionPercent = successRateRegression / baseline.successRateBaseline,
                    confidence = 0.9,
                    description = "成功率阈值回归: ${String.format("%.2f%%", successRateRegression * 100)}",
                    currentValue = currentMetrics.successRate,
                    baselineValue = baseline.successRateBaseline,
                    affectedMetrics = listOf("successRate")
                ))
            }
            
            // 性能阈值检测
            val responseTimeRegression = currentMetrics.averageResponseTime - baseline.responseTimeBaseline
            if (responseTimeRegression > baseline.responseTimeBaseline * config.trendThreshold) {
                regressions.add(PerformanceRegression(
                    type = RegressionType.PERFORMANCE,
                    severity = RegressionSeverity.MEDIUM,
                    timestamp = currentMetrics.timestamp,
                    regressionPercent = responseTimeRegression / baseline.responseTimeBaseline,
                    confidence = 0.9,
                    description = "响应时间阈值回归: ${String.format("%.0fms", responseTimeRegression)}",
                    currentValue = currentMetrics.averageResponseTime,
                    baselineValue = baseline.responseTimeBaseline,
                    affectedMetrics = listOf("averageResponseTime")
                ))
            }
            
            lastExecutionTime = System.currentTimeMillis()
            lastDetectionCount = regressions.size
            
            return regressions
        }
        
        override fun getStatus(): AlgorithmStatus {
            return AlgorithmStatus(
                name = "threshold",
                isEnabled = true,
                lastExecutionTime = lastExecutionTime,
                lastDetectionCount = lastDetectionCount
            )
        }
    }
}