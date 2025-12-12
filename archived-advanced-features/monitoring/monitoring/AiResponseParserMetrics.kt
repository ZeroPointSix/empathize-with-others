package com.empathy.ai.data.monitoring

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.DoubleAdder
import java.util.concurrent.atomic.LongAdder

/**
 * AI响应解析器性能指标收集器
 * 
 * 负责收集、存储和计算解析过程中的各种性能指标
 * 包括成功率、耗时、吞吐量等关键指标
 */
class AiResponseParserMetrics {
    
    companion object {
        private const val TAG = "AiResponseParserMetrics"
        
        // 指标保留时间（毫秒）
        private const val METRICS_RETENTION_PERIOD = 24 * 60 * 60 * 1000L // 24小时
        
        // 性能阈值配置
        private const val WARNING_THRESHOLD_MS = 500L
        private const val CRITICAL_THRESHOLD_MS = 1000L
        
        // 成功率阈值
        private const val MIN_SUCCESS_RATE = 0.95 // 95%
        private const val CRITICAL_SUCCESS_RATE = 0.80 // 80%
    }
    
    // 基础计数器
    private val totalParseRequests = LongAdder()
    private val successfulParseRequests = LongAdder()
    private val failedParseRequests = LongAdder()
    
    // 耗时统计
    private val totalParseTime = LongAdder()
    private val minParseTime = AtomicLong(Long.MAX_VALUE)
    private val maxParseTime = AtomicLong(0L)
    
    // 按操作类型分类的指标
    private val operationMetrics = ConcurrentHashMap<String, OperationMetrics>()
    
    // 按模型分类的指标
    private val modelMetrics = ConcurrentHashMap<String, ModelMetrics>()
    
    // 按错误类型分类的指标
    private val errorMetrics = ConcurrentHashMap<String, ErrorMetrics>()
    
    // 按时间窗口的指标（用于趋势分析）
    private val timeWindowMetrics = ConcurrentHashMap<Long, TimeWindowMetrics>()
    
    /**
     * 记录解析开始
     */
    fun recordParseStart(operationId: String, operationType: String, modelName: String): ParseSession {
        val session = ParseSession(
            operationId = operationId,
            operationType = operationType,
            modelName = modelName,
            startTime = System.nanoTime()
        )
        
        // 更新总请求数
        totalParseRequests.increment()
        
        // 更新操作类型指标
        operationMetrics.computeIfAbsent(operationType) { OperationMetrics() }.recordStart()
        
        // 更新模型指标
        modelMetrics.computeIfAbsent(modelName) { ModelMetrics() }.recordStart()
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "记录解析开始: ID=$operationId, 类型=$operationType, 模型=$modelName")
        }
        
        return session
    }
    
    /**
     * 记录解析成功
     */
    fun recordParseSuccess(session: ParseSession, dataSize: Int) {
        val endTime = System.nanoTime()
        val duration = endTime - session.startTime
        val durationMs = duration / 1_000_000
        
        // 更新成功计数
        successfulParseRequests.increment()
        
        // 更新耗时统计
        totalParseTime.add(duration)
        updateMinParseTime(durationMs)
        updateMaxParseTime(durationMs)
        
        // 更新操作类型指标
        operationMetrics[session.operationType]?.recordSuccess(durationMs, dataSize)
        
        // 更新模型指标
        modelMetrics[session.modelName]?.recordSuccess(durationMs, dataSize)
        
        // 更新时间窗口指标
        updateTimeWindowMetrics(session.operationType, session.modelName, true, durationMs, dataSize)
        
        // 检查性能阈值
        checkPerformanceThresholds(session.operationType, session.modelName, durationMs)
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "记录解析成功: ID=${session.operationId}, 耗时=${durationMs}ms, 数据大小=${dataSize}B")
        }
    }
    
    /**
     * 记录解析失败
     */
    fun recordParseFailure(session: ParseSession, error: Throwable, dataSize: Int) {
        val endTime = System.nanoTime()
        val duration = endTime - session.startTime
        val durationMs = duration / 1_000_000
        
        // 更新失败计数
        failedParseRequests.increment()
        
        // 更新耗时统计
        totalParseTime.add(duration)
        updateMinParseTime(durationMs)
        updateMaxParseTime(durationMs)
        
        // 更新操作类型指标
        operationMetrics[session.operationType]?.recordFailure(durationMs, dataSize)
        
        // 更新模型指标
        modelMetrics[session.modelName]?.recordFailure(durationMs, dataSize)
        
        // 更新错误指标
        val errorType = error.javaClass.simpleName
        errorMetrics.computeIfAbsent(errorType) { ErrorMetrics() }.recordError()
        
        // 更新时间窗口指标
        updateTimeWindowMetrics(session.operationType, session.modelName, false, durationMs, dataSize)
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "记录解析失败: ID=${session.operationId}, 错误=$errorType, 耗时=${durationMs}ms")
        }
    }
    
    /**
     * 获取总体性能指标
     */
    fun getOverallMetrics(): OverallMetrics {
        val totalRequests = totalParseRequests.sum()
        val successCount = successfulParseRequests.sum()
        val failureCount = failedParseRequests.sum()
        val successRate = if (totalRequests > 0) successCount.toDouble() / totalRequests else 0.0
        
        val totalTime = totalParseTime.sum()
        val avgTimeMs = if (totalRequests > 0) totalTime / totalRequests / 1_000_000 else 0L
        
        return OverallMetrics(
            totalRequests = totalRequests,
            successfulRequests = successCount,
            failedRequests = failureCount,
            successRate = successRate,
            averageParseTimeMs = avgTimeMs,
            minParseTimeMs = minParseTime.get(),
            maxParseTimeMs = maxParseTime.get()
        )
    }
    
    /**
     * 获取操作类型指标
     */
    fun getOperationMetrics(operationType: String): OperationMetrics? {
        return operationMetrics[operationType]
    }
    
    /**
     * 获取所有操作类型指标
     */
    fun getAllOperationMetrics(): Map<String, OperationMetrics> {
        return operationMetrics.toMap()
    }
    
    /**
     * 获取模型指标
     */
    fun getModelMetrics(modelName: String): ModelMetrics? {
        return modelMetrics[modelName]
    }
    
    /**
     * 获取所有模型指标
     */
    fun getAllModelMetrics(): Map<String, ModelMetrics> {
        return modelMetrics.toMap()
    }
    
    /**
     * 获取错误指标
     */
    fun getErrorMetrics(errorType: String): ErrorMetrics? {
        return errorMetrics[errorType]
    }
    
    /**
     * 获取所有错误指标
     */
    fun getAllErrorMetrics(): Map<String, ErrorMetrics> {
        return errorMetrics.toMap()
    }
    
    /**
     * 获取时间窗口指标
     */
    fun getTimeWindowMetrics(timeWindowMs: Long): List<TimeWindowMetrics> {
        val cutoffTime = System.currentTimeMillis() - timeWindowMs
        return timeWindowMetrics.values.filter { it.timestamp >= cutoffTime }.sortedBy { it.timestamp }
    }
    
    /**
     * 获取性能摘要
     */
    fun getPerformanceSummary(): PerformanceSummary {
        val overall = getOverallMetrics()
        val recentMetrics = getTimeWindowMetrics(60 * 60 * 1000) // 最近1小时
        
        val recentSuccessRate = if (recentMetrics.isNotEmpty()) {
            val recentTotal = recentMetrics.sumOf { it.totalRequests }
            val recentSuccess = recentMetrics.sumOf { it.successfulRequests }
            if (recentTotal > 0) recentSuccess.toDouble() / recentTotal else 0.0
        } else {
            overall.successRate
        }
        
        val healthStatus = determineHealthStatus(overall.successRate, overall.averageParseTimeMs)
        
        return PerformanceSummary(
            overallMetrics = overall,
            recentSuccessRate = recentSuccessRate,
            healthStatus = healthStatus,
            topErrors = getTopErrors(5),
            slowestOperations = getSlowestOperations(5)
        )
    }
    
    /**
     * 清理过期指标
     */
    fun cleanupExpiredMetrics() {
        val cutoffTime = System.currentTimeMillis() - METRICS_RETENTION_PERIOD
        
        // 清理时间窗口指标
        timeWindowMetrics.entries.removeIf { it.key < cutoffTime }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "清理过期指标完成")
        }
    }
    
    /**
     * 重置所有指标
     */
    fun resetAllMetrics() {
        totalParseRequests.reset()
        successfulParseRequests.reset()
        failedParseRequests.reset()
        totalParseTime.reset()
        minParseTime.set(Long.MAX_VALUE)
        maxParseTime.set(0L)
        
        operationMetrics.clear()
        modelMetrics.clear()
        errorMetrics.clear()
        timeWindowMetrics.clear()
        
        Log.i(TAG, "重置所有指标完成")
    }
    
    /**
     * 更新最小解析时间
     */
    private fun updateMinParseTime(durationMs: Long) {
        var current = minParseTime.get()
        while (durationMs < current && !minParseTime.compareAndSet(current, durationMs)) {
            current = minParseTime.get()
        }
    }
    
    /**
     * 更新最大解析时间
     */
    private fun updateMaxParseTime(durationMs: Long) {
        var current = maxParseTime.get()
        while (durationMs > current && !maxParseTime.compareAndSet(current, durationMs)) {
            current = maxParseTime.get()
        }
    }
    
    /**
     * 更新时间窗口指标
     */
    private fun updateTimeWindowMetrics(operationType: String, modelName: String, success: Boolean, durationMs: Long, dataSize: Int) {
        val windowKey = System.currentTimeMillis() / (60 * 1000) * (60 * 1000) // 按分钟对齐
        val window = timeWindowMetrics.computeIfAbsent(windowKey) { TimeWindowMetrics(windowKey) }
        
        window.addRequest(operationType, modelName, success, durationMs, dataSize)
    }
    
    /**
     * 检查性能阈值
     */
    private fun checkPerformanceThresholds(operationType: String, modelName: String, durationMs: Long) {
        when {
            durationMs > CRITICAL_THRESHOLD_MS -> {
                Log.w(TAG, "性能警告: $operationType 解析耗时 ${durationMs}ms 超过临界阈值 ${CRITICAL_THRESHOLD_MS}ms")
            }
            durationMs > WARNING_THRESHOLD_MS -> {
                Log.w(TAG, "性能提醒: $operationType 解析耗时 ${durationMs}ms 超过警告阈值 ${WARNING_THRESHOLD_MS}ms")
            }
        }
    }
    
    /**
     * 确定健康状态
     */
    private fun determineHealthStatus(successRate: Double, avgTimeMs: Long): HealthStatus {
        return when {
            successRate >= MIN_SUCCESS_RATE && avgTimeMs <= WARNING_THRESHOLD_MS -> HealthStatus.HEALTHY
            successRate >= CRITICAL_SUCCESS_RATE && avgTimeMs <= CRITICAL_THRESHOLD_MS -> HealthStatus.DEGRADED
            successRate >= CRITICAL_SUCCESS_RATE -> HealthStatus.UNHEALTHY
            else -> HealthStatus.CRITICAL
        }
    }
    
    /**
     * 获取最常见的错误
     */
    private fun getTopErrors(limit: Int): List<ErrorRanking> {
        return errorMetrics.entries
            .map { (errorType, metrics) ->
                ErrorRanking(errorType, metrics.errorCount.get())
            }
            .sortedByDescending { it.count }
            .take(limit)
    }
    
    /**
     * 获取最慢的操作
     */
    private fun getSlowestOperations(limit: Int): List<OperationRanking> {
        return operationMetrics.entries
            .map { (operationType, metrics) ->
                OperationRanking(operationType, metrics.averageParseTimeMs)
            }
            .sortedByDescending { it.averageTimeMs }
            .take(limit)
    }
    
    /**
     * 解析会话
     */
    data class ParseSession(
        val operationId: String,
        val operationType: String,
        val modelName: String,
        val startTime: Long
    )
    
    /**
     * 总体指标
     */
    data class OverallMetrics(
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageParseTimeMs: Long,
        val minParseTimeMs: Long,
        val maxParseTimeMs: Long
    )
    
    /**
     * 操作类型指标
     */
    class OperationMetrics {
        val totalRequests = LongAdder()
        val successfulRequests = LongAdder()
        val failedRequests = LongAdder()
        val totalParseTime = LongAdder()
        val totalDataSize = LongAdder()
        
        fun recordStart() {
            totalRequests.increment()
        }
        
        fun recordSuccess(durationMs: Long, dataSize: Int) {
            successfulRequests.increment()
            totalParseTime.add(durationMs)
            totalDataSize.add(dataSize.toLong())
        }
        
        fun recordFailure(durationMs: Long, dataSize: Int) {
            failedRequests.increment()
            totalParseTime.add(durationMs)
            totalDataSize.add(dataSize.toLong())
        }
        
        fun getMetrics(): OperationMetricsData {
            val total = totalRequests.sum()
            val success = successfulRequests.sum()
            val failure = failedRequests.sum()
            val successRate = if (total > 0) success.toDouble() / total else 0.0
            val avgTimeMs = if (total > 0) totalParseTime.sum() / total else 0L
            val avgDataSize = if (total > 0) totalDataSize.sum() / total else 0L
            
            return OperationMetricsData(total, success, failure, successRate, avgTimeMs, avgDataSize)
        }
        
        val averageParseTimeMs: Long
            get() {
                val total = totalRequests.sum()
                return if (total > 0) totalParseTime.sum() / total else 0L
            }
    }
    
    /**
     * 操作指标数据
     */
    data class OperationMetricsData(
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageParseTimeMs: Long,
        val averageDataSize: Long
    )
    
    /**
     * 模型指标
     */
    class ModelMetrics {
        val totalRequests = LongAdder()
        val successfulRequests = LongAdder()
        val failedRequests = LongAdder()
        val totalParseTime = LongAdder()
        
        fun recordStart() {
            totalRequests.increment()
        }
        
        fun recordSuccess(durationMs: Long, dataSize: Int) {
            successfulRequests.increment()
            totalParseTime.add(durationMs)
        }
        
        fun recordFailure(durationMs: Long, dataSize: Int) {
            failedRequests.increment()
            totalParseTime.add(durationMs)
        }
        
        fun getMetrics(): ModelMetricsData {
            val total = totalRequests.sum()
            val success = successfulRequests.sum()
            val failure = failedRequests.sum()
            val successRate = if (total > 0) success.toDouble() / total else 0.0
            val avgTimeMs = if (total > 0) totalParseTime.sum() / total else 0L
            
            return ModelMetricsData(total, success, failure, successRate, avgTimeMs)
        }
    }
    
    /**
     * 模型指标数据
     */
    data class ModelMetricsData(
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageParseTimeMs: Long
    )
    
    /**
     * 错误指标
     */
    class ErrorMetrics {
        val errorCount = LongAdder()
        
        fun recordError() {
            errorCount.increment()
        }
        
        fun getCount(): Long = errorCount.sum()
    }
    
    /**
     * 时间窗口指标
     */
    class TimeWindowMetrics(val timestamp: Long) {
        val totalRequests = LongAdder()
        val successfulRequests = LongAdder()
        val failedRequests = LongAdder()
        val totalParseTime = LongAdder()
        val totalDataSize = LongAdder()
        
        fun addRequest(operationType: String, modelName: String, success: Boolean, durationMs: Long, dataSize: Int) {
            totalRequests.increment()
            totalParseTime.add(durationMs)
            totalDataSize.add(dataSize.toLong())
            
            if (success) {
                successfulRequests.increment()
            } else {
                failedRequests.increment()
            }
        }
    }
    
    /**
     * 性能摘要
     */
    data class PerformanceSummary(
        val overallMetrics: OverallMetrics,
        val recentSuccessRate: Double,
        val healthStatus: HealthStatus,
        val topErrors: List<ErrorRanking>,
        val slowestOperations: List<OperationRanking>
    )
    
    /**
     * 错误排名
     */
    data class ErrorRanking(
        val errorType: String,
        val count: Long
    )
    
    /**
     * 操作排名
     */
    data class OperationRanking(
        val operationType: String,
        val averageTimeMs: Long
    )
    
    /**
     * 健康状态
     */
    enum class HealthStatus {
        HEALTHY,    // 成功率>95%，平均耗时<500ms
        DEGRADED,   // 成功率>80%，平均耗时<1000ms
        UNHEALTHY,  // 成功率>80%，平均耗时>1000ms
        CRITICAL     // 成功率<80%
    }
}