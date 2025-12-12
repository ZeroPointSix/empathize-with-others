package com.empathy.ai.data.monitoring

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 健康检查系统
 * 
 * 负责监控解析器的健康状态
 * 定期检查关键指标并生成健康报告
 */
class HealthCheckSystem {
    
    companion object {
        private const val TAG = "HealthCheckSystem"
        
        // 健康检查配置
        private const val CHECK_INTERVAL_MS = 60_000L // 1分钟
        private const val RECENT_WINDOW_SIZE = 100 // 最近100次请求
        private const val MIN_SAMPLES_FOR_HEALTH = 10 // 最少需要10个样本
        
        // 健康阈值
        private const val HEALTHY_SUCCESS_RATE = 0.95 // 95%
        private const val DEGRADED_SUCCESS_RATE = 0.90 // 90%
        private const val CRITICAL_SUCCESS_RATE = 0.80 // 80%
        
        private const val HEALTHY_AVG_RESPONSE_TIME = 500L // 500ms
        private const val DEGRADED_AVG_RESPONSE_TIME = 1000L // 1000ms
        private const val CRITICAL_AVG_RESPONSE_TIME = 2000L // 2000ms
        
        private const val HEALTHY_ERROR_RATE = 0.05 // 5%
        private const val CRITICAL_ERROR_RATE = 0.20 // 20%
    }
    
    // 最近的解析结果
    private val recentResults = ConcurrentLinkedQueue<HealthCheckResult>()
    
    // 当前健康状态
    private val currentHealthStatus = AtomicReference(HealthStatus.UNKNOWN)
    
    // 上次检查时间
    private val lastCheckTime = AtomicLong(0)
    
    // 健康检查监听器
    private val healthCheckListeners = mutableListOf<HealthCheckListener>()
    
    // 组件健康状态
    private val componentHealth = mutableMapOf<String, ComponentHealth>()
    
    /**
     * 记录解析结果
     */
    fun recordParsingResult(result: HealthCheckResult) {
        recentResults.offer(result)
        
        // 保持窗口大小
        while (recentResults.size > RECENT_WINDOW_SIZE) {
            recentResults.poll()
        }
        
        // 更新组件健康状态
        updateComponentHealth(result)
        
        // 检查是否需要更新整体健康状态
        val now = System.currentTimeMillis()
        if (now - lastCheckTime.get() > CHECK_INTERVAL_MS) {
            if (lastCheckTime.compareAndSet(lastCheckTime.get(), now)) {
                updateOverallHealthStatus()
            }
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "记录解析结果: 成功=${result.success}, 耗时=${result.durationMs}ms, 错误=${result.errorType?.name}")
        }
    }
    
    /**
     * 获取当前健康状态
     */
    fun getCurrentHealthStatus(): HealthStatus {
        return currentHealthStatus.get()
    }
    
    /**
     * 获取详细健康报告
     */
    fun getHealthReport(): HealthReport {
        val results = recentResults.toList()
        val overallStatus = currentHealthStatus.get()
        
        val totalRequests = results.size
        val successfulRequests = results.count { it.success }
        val failedRequests = totalRequests - successfulRequests
        val successRate = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0
        
        val avgDuration = if (results.isNotEmpty()) {
            results.map { it.durationMs }.average().toLong()
        } else {
            0L
        }
        
        val errorDistribution = results
            .filter { !it.success }
            .groupBy { it.errorType ?: ErrorType.UNKNOWN }
            .mapValues { it.value.size }
        
        val componentStatus = componentHealth.toMap()
        
        return HealthReport(
            timestamp = System.currentTimeMillis(),
            overallStatus = overallStatus,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            successRate = successRate,
            averageDurationMs = avgDuration,
            errorDistribution = errorDistribution,
            componentStatus = componentStatus,
            recommendations = generateRecommendations(overallStatus, successRate, avgDuration)
        )
    }
    
    /**
     * 添加健康检查监听器
     */
    fun addHealthCheckListener(listener: HealthCheckListener) {
        synchronized(healthCheckListeners) {
            healthCheckListeners.add(listener)
        }
    }
    
    /**
     * 移除健康检查监听器
     */
    fun removeHealthCheckListener(listener: HealthCheckListener) {
        synchronized(healthCheckListeners) {
            healthCheckListeners.remove(listener)
        }
    }
    
    /**
     * 手动触发健康检查
     */
    fun triggerHealthCheck(): HealthStatus {
        updateOverallHealthStatus()
        return currentHealthStatus.get()
    }
    
    /**
     * 重置健康状态
     */
    fun resetHealthStatus() {
        recentResults.clear()
        componentHealth.clear()
        currentHealthStatus.set(HealthStatus.UNKNOWN)
        lastCheckTime.set(0)
        
        Log.i(TAG, "重置健康状态完成")
    }
    
    /**
     * 更新组件健康状态
     */
    private fun updateComponentHealth(result: HealthCheckResult) {
        result.components.forEach { (componentName, componentResult) ->
            val health = componentHealth.getOrPut(componentName) {
                ComponentHealth(componentName)
            }
            
            health.recordResult(componentResult)
        }
    }
    
    /**
     * 更新整体健康状态
     */
    private fun updateOverallHealthStatus() {
        val results = recentResults.toList()
        
        if (results.size < MIN_SAMPLES_FOR_HEALTH) {
            currentHealthStatus.set(HealthStatus.UNKNOWN)
            return
        }
        
        val totalRequests = results.size
        val successfulRequests = results.count { it.success }
        val successRate = successfulRequests.toDouble() / totalRequests
        
        val avgDuration = results.map { it.durationMs }.average()
        
        val errorRate = (totalRequests - successfulRequests).toDouble() / totalRequests
        
        val newStatus = determineHealthStatus(successRate, avgDuration, errorRate)
        val oldStatus = currentHealthStatus.get()
        
        if (newStatus != oldStatus) {
            currentHealthStatus.set(newStatus)
            notifyHealthStatusChange(oldStatus, newStatus)
            
            Log.i(TAG, "健康状态变更: $oldStatus -> $newStatus")
            Log.i(TAG, "详细指标: 成功率=${String.format("%.2f%%", successRate * 100)}, 平均耗时=${String.format("%.2f", avgDuration)}ms, 错误率=${String.format("%.2f%%", errorRate * 100)}")
        }
    }
    
    /**
     * 确定健康状态
     */
    private fun determineHealthStatus(successRate: Double, avgDuration: Double, errorRate: Double): HealthStatus {
        return when {
            successRate >= HEALTHY_SUCCESS_RATE && 
            avgDuration <= HEALTHY_AVG_RESPONSE_TIME && 
            errorRate <= HEALTHY_ERROR_RATE -> HealthStatus.HEALTHY
            
            successRate >= DEGRADED_SUCCESS_RATE && 
            avgDuration <= DEGRADED_AVG_RESPONSE_TIME && 
            errorRate < CRITICAL_ERROR_RATE -> HealthStatus.DEGRADED
            
            successRate >= CRITICAL_SUCCESS_RATE -> HealthStatus.UNHEALTHY
            
            else -> HealthStatus.CRITICAL
        }
    }
    
    /**
     * 生成建议
     */
    private fun generateRecommendations(status: HealthStatus, successRate: Double, avgDuration: Double): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (status) {
            HealthStatus.HEALTHY -> {
                recommendations.add("系统运行正常，继续保持当前配置")
            }
            
            HealthStatus.DEGRADED -> {
                if (successRate < HEALTHY_SUCCESS_RATE) {
                    recommendations.add("解析成功率偏低，建议检查输入数据质量和解析策略")
                }
                if (avgDuration > HEALTHY_AVG_RESPONSE_TIME) {
                    recommendations.add("解析耗时偏高，建议优化解析算法或增加缓存")
                }
                recommendations.add("考虑启用更详细的日志记录以诊断问题")
            }
            
            HealthStatus.UNHEALTHY -> {
                recommendations.add("系统性能下降，建议立即检查错误日志")
                recommendations.add("考虑临时启用更保守的解析策略")
                recommendations.add("检查系统资源使用情况（CPU、内存）")
            }
            
            HealthStatus.CRITICAL -> {
                recommendations.add("系统处于临界状态，建议立即采取行动")
                recommendations.add("考虑切换到备用解析方案")
                recommendations.add("联系技术支持团队进行紧急干预")
            }
            
            HealthStatus.UNKNOWN -> {
                recommendations.add("系统运行时间不足，需要更多数据来评估健康状态")
            }
        }
        
        return recommendations
    }
    
    /**
     * 通知健康状态变更
     */
    private fun notifyHealthStatusChange(oldStatus: HealthStatus, newStatus: HealthStatus) {
        val listeners = synchronized(healthCheckListeners) {
            healthCheckListeners.toList()
        }
        
        listeners.forEach { listener ->
            try {
                listener.onHealthStatusChanged(oldStatus, newStatus)
            } catch (e: Exception) {
                Log.e(TAG, "健康状态监听器异常", e)
            }
        }
    }
    
    /**
     * 组件健康状态
     */
    class ComponentHealth(val componentName: String) {
        private val results = mutableListOf<ComponentResult>()
        private val maxResults = 50
        
        fun recordResult(result: ComponentResult) {
            synchronized(results) {
                results.add(result)
                
                // 保持结果数量在限制内
                while (results.size > maxResults) {
                    results.removeAt(0)
                }
            }
        }
        
        fun getHealthStatus(): ComponentHealthStatus {
            synchronized(results) {
                if (results.isEmpty()) {
                    return ComponentHealthStatus.UNKNOWN
                }
                
                val successCount = results.count { it.success }
                val successRate = successCount.toDouble() / results.size
                val avgDuration = results.map { it.durationMs }.average()
                
                return when {
                    successRate >= 0.95 && avgDuration <= 100 -> ComponentHealthStatus.HEALTHY
                    successRate >= 0.90 && avgDuration <= 200 -> ComponentHealthStatus.DEGRADED
                    successRate >= 0.80 -> ComponentHealthStatus.UNHEALTHY
                    else -> ComponentHealthStatus.CRITICAL
                }
            }
        }
        
        fun getStatistics(): ComponentStatistics {
            synchronized(results) {
                if (results.isEmpty()) {
                    return ComponentStatistics(0, 0, 0.0, 0L, 0L, 0L)
                }
                
                val totalRequests = results.size
                val successfulRequests = results.count { it.success }
                val successRate = successfulRequests.toDouble() / totalRequests
                val avgDuration = results.map { it.durationMs }.average().toLong()
                val minDuration = results.minOfOrNull { it.durationMs } ?: 0L
                val maxDuration = results.maxOfOrNull { it.durationMs } ?: 0L
                
                return ComponentStatistics(
                    totalRequests = totalRequests,
                    successfulRequests = successfulRequests,
                    successRate = successRate,
                    averageDurationMs = avgDuration,
                    minDurationMs = minDuration,
                    maxDurationMs = maxDuration
                )
            }
        }
    }
    
    /**
     * 健康检查结果
     */
    data class HealthCheckResult(
        val timestamp: Long = System.currentTimeMillis(),
        val success: Boolean,
        val durationMs: Long,
        val dataSize: Int = 0,
        val errorType: ErrorType? = null,
        val components: Map<String, ComponentResult> = emptyMap()
    )
    
    /**
     * 组件结果
     */
    data class ComponentResult(
        val success: Boolean,
        val durationMs: Long,
        val errorType: ErrorType? = null
    )
    
    /**
     * 健康报告
     */
    data class HealthReport(
        val timestamp: Long,
        val overallStatus: HealthStatus,
        val totalRequests: Int,
        val successfulRequests: Int,
        val failedRequests: Int,
        val successRate: Double,
        val averageDurationMs: Long,
        val errorDistribution: Map<ErrorType, Int>,
        val componentStatus: Map<String, ComponentHealth>,
        val recommendations: List<String>
    )
    
    /**
     * 组件统计
     */
    data class ComponentStatistics(
        val totalRequests: Int,
        val successfulRequests: Int,
        val successRate: Double,
        val averageDurationMs: Long,
        val minDurationMs: Long,
        val maxDurationMs: Long
    )
    
    /**
     * 健康状态
     */
    enum class HealthStatus {
        HEALTHY,    // 成功率>95%，平均耗时<500ms
        DEGRADED,   // 成功率>90%，平均耗时<1000ms
        UNHEALTHY,  // 成功率>80%
        CRITICAL,    // 成功率<80%
        UNKNOWN      // 数据不足
    }
    
    /**
     * 组件健康状态
     */
    enum class ComponentHealthStatus {
        HEALTHY,    // 组件运行正常
        DEGRADED,   // 组件性能下降
        UNHEALTHY,  // 组件有问题
        UNKNOWN      // 数据不足
    }
    
    /**
     * 错误类型
     */
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
    
    /**
     * 健康检查监听器
     */
    interface HealthCheckListener {
        fun onHealthStatusChanged(oldStatus: HealthStatus, newStatus: HealthStatus)
    }
}