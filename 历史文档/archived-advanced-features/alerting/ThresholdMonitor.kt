package com.empathy.ai.data.alerting

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 阈值监控器
 * 
 * 负责监控各种指标阈值
 * 当指标超过或低于阈值时触发告警
 */
class ThresholdMonitor {
    
    companion object {
        private const val TAG = "ThresholdMonitor"
        
        // 默认阈值配置
        private const val DEFAULT_SUCCESS_RATE_THRESHOLD = 0.9
        private const val DEFAULT_ERROR_RATE_THRESHOLD = 0.1
        private const val DEFAULT_AVG_RESPONSE_TIME_THRESHOLD = 500L
        private const val DEFAULT_P95_RESPONSE_TIME_THRESHOLD = 1000L
        private const val DEFAULT_MEMORY_USAGE_THRESHOLD = 0.8
        private const val DEFAULT_CPU_USAGE_THRESHOLD = 0.8
        
        // 监控间隔
        private const val MONITORING_INTERVAL_MS = 30 * 1000L // 30秒
        private const val EVALUATION_WINDOW_SIZE = 100 // 最近100个数据点
    }
    
    // 阈值配置
    private val thresholdConfig = AtomicReference(ThresholdConfig())
    
    // 指标数据存储
    private val metricData = ConcurrentHashMap<String, MutableList<MetricDataPoint>>()
    
    // 阈值状态
    private val thresholdStates = ConcurrentHashMap<String, ThresholdState>()
    
    // 阈值监听器
    private val thresholdListeners = mutableListOf<ThresholdListener>()
    
    // 定期监控任务
    private val scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "ThresholdMonitor-${r.hashCode()}").apply {
            isDaemon = true
        }
    }
    
    // 数据点ID生成器
    private val dataPointIdGenerator = AtomicLong(0)
    
    init {
        // 启动定期监控任务
        startPeriodicMonitoring()
        
        // 初始化默认阈值
        initializeDefaultThresholds()
        
        Log.i(TAG, "ThresholdMonitor 初始化完成")
    }
    
    /**
     * 添加阈值监听器
     */
    fun addThresholdListener(listener: ThresholdListener) {
        synchronized(thresholdListeners) {
            thresholdListeners.add(listener)
        }
        
        Log.i(TAG, "添加阈值监听器")
    }
    
    /**
     * 移除阈值监听器
     */
    fun removeThresholdListener(listener: ThresholdListener) {
        synchronized(thresholdListeners) {
            thresholdListeners.remove(listener)
        }
        
        Log.i(TAG, "移除阈值监听器")
    }
    
    /**
     * 更新阈值配置
     */
    fun updateThresholdConfig(config: ThresholdConfig) {
        thresholdConfig.set(config)
        
        // 重新评估所有阈值
        reevaluateAllThresholds()
        
        Log.i(TAG, "更新阈值配置: $config")
    }
    
    /**
     * 添加指标数据点
     */
    fun addMetricDataPoint(
        metricName: String,
        value: Double,
        timestamp: Long = System.currentTimeMillis(),
        metadata: Map<String, Any> = emptyMap()
    ) {
        val dataPoint = MetricDataPoint(
            id = generateDataPointId(),
            metricName = metricName,
            value = value,
            timestamp = timestamp,
            metadata = metadata
        )
        
        val dataPoints = metricData.computeIfAbsent(metricName) { mutableListOf() }
        synchronized(dataPoints) {
            dataPoints.add(dataPoint)
            
            // 保持窗口大小
            while (dataPoints.size > EVALUATION_WINDOW_SIZE) {
                dataPoints.removeAt(0)
            }
        }
        
        // 立即评估该指标的阈值
        evaluateThreshold(metricName)
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "添加指标数据点: $metricName = $value")
        }
    }
    
    /**
     * 获取指标数据
     */
    fun getMetricData(metricName: String, limit: Int = 100): List<MetricDataPoint> {
        val dataPoints = metricData[metricName]
        return if (dataPoints != null) {
            synchronized(dataPoints) {
                dataPoints.takeLast(limit)
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * 获取所有指标名称
     */
    fun getAllMetricNames(): Set<String> {
        return metricData.keys.toSet()
    }
    
    /**
     * 获取阈值状态
     */
    fun getThresholdState(metricName: String): ThresholdState? {
        return thresholdStates[metricName]
    }
    
    /**
     * 获取所有阈值状态
     */
    fun getAllThresholdStates(): Map<String, ThresholdState> {
        return thresholdStates.toMap()
    }
    
    /**
     * 手动触发阈值评估
     */
    fun triggerThresholdEvaluation(metricName: String? = null) {
        if (metricName != null) {
            evaluateThreshold(metricName)
        } else {
            reevaluateAllThresholds()
        }
    }
    
    /**
     * 重置所有指标数据
     */
    fun resetAllMetricData() {
        metricData.clear()
        thresholdStates.clear()
        
        Log.i(TAG, "重置所有指标数据和阈值状态")
    }
    
    /**
     * 重置特定指标数据
     */
    fun resetMetricData(metricName: String) {
        metricData.remove(metricName)
        thresholdStates.remove(metricName)
        
        Log.i(TAG, "重置指标数据: $metricName")
    }
    
    /**
     * 获取阈值摘要
     */
    fun getThresholdSummary(): ThresholdSummary {
        val totalMetrics = metricData.size
        val activeThresholds = thresholdStates.values.count { it.status != ThresholdStatus.NORMAL }
        val criticalThresholds = thresholdStates.values.count { it.status == ThresholdStatus.CRITICAL }
        val warningThresholds = thresholdStates.values.count { it.status == ThresholdStatus.WARNING }
        
        val thresholdViolations = thresholdStates.values
            .filter { it.status != ThresholdStatus.NORMAL }
            .groupBy { it.metricName }
            .mapValues { it.value.size }
        
        val mostViolatedMetric = thresholdViolations
            .maxByOrNull { it.value }
            ?.key
        
        return ThresholdSummary(
            totalMetrics = totalMetrics,
            activeThresholds = activeThresholds,
            criticalThresholds = criticalThresholds,
            warningThresholds = warningThresholds,
            thresholdViolations = thresholdViolations,
            mostViolatedMetric = mostViolatedMetric,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            // 关闭定期监控任务
            scheduledExecutor.shutdown()
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow()
            }
            
            // 清理资源
            metricData.clear()
            thresholdStates.clear()
            thresholdListeners.clear()
            
            Log.i(TAG, "ThresholdMonitor 资源释放完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }
    
    /**
     * 启动定期监控
     */
    private fun startPeriodicMonitoring() {
        scheduledExecutor.scheduleAtFixedRate({
            try {
                reevaluateAllThresholds()
            } catch (e: Exception) {
                Log.e(TAG, "定期阈值监控失败", e)
            }
        }, MONITORING_INTERVAL_MS, MONITORING_INTERVAL_MS, TimeUnit.MILLISECONDS)
    }
    
    /**
     * 初始化默认阈值
     */
    private fun initializeDefaultThresholds() {
        val config = thresholdConfig.get()
        
        // 成功率阈值
        thresholdStates["success_rate"] = ThresholdState(
            metricName = "success_rate",
            threshold = config.successRateThreshold,
            operator = ThresholdOperator.LESS_THAN,
            status = ThresholdStatus.NORMAL
        )
        
        // 错误率阈值
        thresholdStates["error_rate"] = ThresholdState(
            metricName = "error_rate",
            threshold = config.errorRateThreshold,
            operator = ThresholdOperator.GREATER_THAN,
            status = ThresholdStatus.NORMAL
        )
        
        // 平均响应时间阈值
        thresholdStates["avg_response_time"] = ThresholdState(
            metricName = "avg_response_time",
            threshold = config.avgResponseTimeThreshold.toDouble(),
            operator = ThresholdOperator.GREATER_THAN,
            status = ThresholdStatus.NORMAL
        )
        
        // P95响应时间阈值
        thresholdStates["p95_response_time"] = ThresholdState(
            metricName = "p95_response_time",
            threshold = config.p95ResponseTimeThreshold.toDouble(),
            operator = ThresholdOperator.GREATER_THAN,
            status = ThresholdStatus.NORMAL
        )
        
        // 内存使用阈值
        thresholdStates["memory_usage"] = ThresholdState(
            metricName = "memory_usage",
            threshold = config.memoryUsageThreshold,
            operator = ThresholdOperator.GREATER_THAN,
            status = ThresholdStatus.NORMAL
        )
        
        // CPU使用阈值
        thresholdStates["cpu_usage"] = ThresholdState(
            metricName = "cpu_usage",
            threshold = config.cpuUsageThreshold,
            operator = ThresholdOperator.GREATER_THAN,
            status = ThresholdStatus.NORMAL
        )
    }
    
    /**
     * 评估单个阈值
     */
    private fun evaluateThreshold(metricName: String) {
        val dataPoints = metricData[metricName]
        val thresholdState = thresholdStates[metricName]
        
        if (dataPoints == null || thresholdState == null) {
            return
        }
        
        synchronized(dataPoints) {
            if (dataPoints.isEmpty()) {
                return
            }
            
            val latestValue = dataPoints.last().value
            val oldValue = thresholdState.currentValue
            val oldStatus = thresholdState.status
            
            // 更新当前值
            thresholdState.currentValue = latestValue
            thresholdState.lastEvaluation = System.currentTimeMillis()
            
            // 评估阈值
            val newStatus = evaluateThresholdValue(latestValue, thresholdState)
            
            // 如果状态发生变化，通知监听器
            if (newStatus != oldStatus) {
                thresholdState.status = newStatus
                thresholdState.violationCount++
                thresholdState.lastViolation = System.currentTimeMillis()
                
                notifyThresholdListeners(thresholdState, oldValue, latestValue)
                
                Log.i(TAG, "阈值状态变化: $metricName, $oldStatus -> $newStatus, 值: $latestValue")
            }
        }
    }
    
    /**
     * 重新评估所有阈值
     */
    private fun reevaluateAllThresholds() {
        thresholdStates.values.forEach { thresholdState ->
            evaluateThreshold(thresholdState.metricName)
        }
    }
    
    /**
     * 评估阈值值
     */
    private fun evaluateThresholdValue(
        value: Double,
        thresholdState: ThresholdState
    ): ThresholdStatus {
        return when (thresholdState.operator) {
            ThresholdOperator.GREATER_THAN -> {
                when {
                    value > thresholdState.threshold * 1.5 -> ThresholdStatus.CRITICAL
                    value > thresholdState.threshold -> ThresholdStatus.WARNING
                    else -> ThresholdStatus.NORMAL
                }
            }
            
            ThresholdOperator.LESS_THAN -> {
                when {
                    value < thresholdState.threshold * 0.5 -> ThresholdStatus.CRITICAL
                    value < thresholdState.threshold -> ThresholdStatus.WARNING
                    else -> ThresholdStatus.NORMAL
                }
            }
            
            ThresholdOperator.EQUALS -> {
                when {
                    kotlin.math.abs(value - thresholdState.threshold) < 0.01 -> ThresholdStatus.NORMAL
                    kotlin.math.abs(value - thresholdState.threshold) < 0.1 -> ThresholdStatus.WARNING
                    else -> ThresholdStatus.CRITICAL
                }
            }
        }
    }
    
    /**
     * 通知阈值监听器
     */
    private fun notifyThresholdListeners(
        thresholdState: ThresholdState,
        oldValue: Double,
        newValue: Double
    ) {
        val listeners = synchronized(thresholdListeners) {
            thresholdListeners.toList()
        }
        
        listeners.forEach { listener ->
            try {
                listener.onThresholdViolated(
                    ThresholdViolationEvent(
                        metricName = thresholdState.metricName,
                        threshold = thresholdState.threshold,
                        operator = thresholdState.operator,
                        oldValue = oldValue,
                        newValue = newValue,
                        status = thresholdState.status,
                        violationCount = thresholdState.violationCount,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "阈值监听器异常", e)
            }
        }
    }
    
    /**
     * 生成数据点ID
     */
    private fun generateDataPointId(): String {
        return "dp_${dataPointIdGenerator.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    // 数据类定义
    data class MetricDataPoint(
        val id: String,
        val metricName: String,
        val value: Double,
        val timestamp: Long,
        val metadata: Map<String, Any>
    )
    
    data class ThresholdState(
        val metricName: String,
        var threshold: Double,
        val operator: ThresholdOperator,
        var status: ThresholdStatus = ThresholdStatus.NORMAL,
        var currentValue: Double = 0.0,
        var lastEvaluation: Long = System.currentTimeMillis(),
        var violationCount: Int = 0,
        var lastViolation: Long = 0L
    )
    
    data class ThresholdViolationEvent(
        val metricName: String,
        val threshold: Double,
        val operator: ThresholdOperator,
        val oldValue: Double,
        val newValue: Double,
        val status: ThresholdStatus,
        val violationCount: Int,
        val timestamp: Long
    )
    
    data class ThresholdConfig(
        val successRateThreshold: Double = DEFAULT_SUCCESS_RATE_THRESHOLD,
        val errorRateThreshold: Double = DEFAULT_ERROR_RATE_THRESHOLD,
        val avgResponseTimeThreshold: Long = DEFAULT_AVG_RESPONSE_TIME_THRESHOLD,
        val p95ResponseTimeThreshold: Long = DEFAULT_P95_RESPONSE_TIME_THRESHOLD,
        val memoryUsageThreshold: Double = DEFAULT_MEMORY_USAGE_THRESHOLD,
        val cpuUsageThreshold: Double = DEFAULT_CPU_USAGE_THRESHOLD,
        val enableAutoAdjustment: Boolean = true,
        val adjustmentFactor: Double = 0.1,
        val minDataPoints: Int = 10
    )
    
    data class ThresholdSummary(
        val totalMetrics: Int,
        val activeThresholds: Int,
        val criticalThresholds: Int,
        val warningThresholds: Int,
        val thresholdViolations: Map<String, Int>,
        val mostViolatedMetric: String?,
        val lastUpdated: Long
    )
    
    enum class ThresholdStatus {
        NORMAL,     // 正常状态
        WARNING,    // 警告状态
        CRITICAL    // 严重状态
    }
    
    enum class ThresholdOperator {
        GREATER_THAN,  // 大于阈值
        LESS_THAN,     // 小于阈值
        EQUALS         // 等于阈值
    }
    
    /**
     * 阈值监听器接口
     */
    interface ThresholdListener {
        fun onThresholdViolated(event: ThresholdViolationEvent)
    }
}