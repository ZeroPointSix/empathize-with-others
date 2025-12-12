package com.empathy.ai.data.benchmark

import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 持续性能监控器
 * 实时监控系统性能并生成性能报告
 */
class ContinuousPerformanceMonitor private constructor(
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: ContinuousPerformanceMonitor? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): ContinuousPerformanceMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContinuousPerformanceMonitor(metrics).also { INSTANCE = it }
            }
        }
        
        private const val MONITORING_INTERVAL = 5000L // 5秒
        private const val REPORT_INTERVAL = 60000L // 1分钟
        private const val HISTORY_SIZE = 1000
        private const val PERFORMANCE_DEGRADATION_THRESHOLD = 10.0 // 10%性能下降
        private const val MEMORY_PRESSURE_THRESHOLD = 0.8 // 80%内存使用率
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 性能监控器注册表
    private val performanceMonitors = ConcurrentHashMap<String, PerformanceMonitor>()
    
    // 性能历史数据
    private val performanceHistory = ConcurrentHashMap<String, MutableList<PerformanceSnapshot>>()
    
    // 性能警报
    private val performanceAlerts = mutableListOf<PerformanceAlert>()
    
    // 监控状态
    private val _monitoringState = MutableStateFlow(MonitoringState())
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()
    
    // 监控统计
    private val monitoringStats = AtomicReference(MonitoringStatistics())
    
    // 监控任务
    private var monitoringJob: Job? = null
    private var reportingJob: Job? = null
    
    init {
        initializeBuiltInMonitors()
        startMonitoring()
    }
    
    /**
     * 初始化内置监控器
     */
    private fun initializeBuiltInMonitors() {
        // CPU监控器
        registerPerformanceMonitor("cpu", object : PerformanceMonitor {
            override val name = "CPU监控器"
            override val description = "监控CPU使用率和性能"
            override val category = MonitorCategory.SYSTEM
            
            override fun collectMetrics(): PerformanceMetrics {
                val cpuUsage = getCpuUsage()
                val loadAverage = getLoadAverage()
                
                return PerformanceMetrics(
                    timestamp = System.currentTimeMillis(),
                    values = mapOf(
                        "cpu_usage" to cpuUsage,
                        "load_average" to loadAverage
                    ),
                    status = if (cpuUsage > 80.0) MetricsStatus.CRITICAL else 
                             if (cpuUsage > 60.0) MetricsStatus.WARNING else MetricsStatus.NORMAL
                )
            }
            
            private fun getCpuUsage(): Double {
                // 这里应该获取实际的CPU使用率
                // 简化实现
                return (Math.random() * 100).toDouble()
            }
            
            private fun getLoadAverage(): Double {
                // 这里应该获取实际的系统负载
                return (Math.random() * 4).toDouble()
            }
        })
        
        // 内存监控器
        registerPerformanceMonitor("memory", object : PerformanceMonitor {
            override val name = "内存监控器"
            override val description = "监控内存使用情况"
            override val category = MonitorCategory.SYSTEM
            
            override fun collectMetrics(): PerformanceMetrics {
                val runtime = Runtime.getRuntime()
                val totalMemory = runtime.totalMemory()
                val freeMemory = runtime.freeMemory()
                val usedMemory = totalMemory - freeMemory
                val maxMemory = runtime.maxMemory()
                val memoryUsageRatio = usedMemory.toDouble() / maxMemory.toDouble()
                
                return PerformanceMetrics(
                    timestamp = System.currentTimeMillis(),
                    values = mapOf(
                        "total_memory" to totalMemory,
                        "used_memory" to usedMemory,
                        "free_memory" to freeMemory,
                        "max_memory" to maxMemory,
                        "memory_usage_ratio" to memoryUsageRatio
                    ),
                    status = if (memoryUsageRatio > MEMORY_PRESSURE_THRESHOLD) MetricsStatus.CRITICAL else 
                             if (memoryUsageRatio > 0.6) MetricsStatus.WARNING else MetricsStatus.NORMAL
                )
            }
        })
        
        // 网络监控器
        registerPerformanceMonitor("network", object : PerformanceMonitor {
            override val name = "网络监控器"
            override val description = "监控网络性能和连接状态"
            override val category = MonitorCategory.NETWORK
            
            override fun collectMetrics(): PerformanceMetrics {
                val networkLatency = measureNetworkLatency()
                val connectionCount = getActiveConnectionCount()
                val throughput = measureNetworkThroughput()
                
                return PerformanceMetrics(
                    timestamp = System.currentTimeMillis(),
                    values = mapOf(
                        "network_latency" to networkLatency,
                        "connection_count" to connectionCount,
                        "throughput" to throughput
                    ),
                    status = if (networkLatency > 1000) MetricsStatus.CRITICAL else 
                             if (networkLatency > 500) MetricsStatus.WARNING else MetricsStatus.NORMAL
                )
            }
            
            private fun measureNetworkLatency(): Long {
                // 这里应该测量实际的网络延迟
                return (Math.random() * 2000).toLong()
            }
            
            private fun getActiveConnectionCount(): Int {
                // 这里应该获取实际的连接数
                return (Math.random() * 100).toInt()
            }
            
            private fun measureNetworkThroughput(): Double {
                // 这里应该测量实际的网络吞吐量
                return Math.random() * 1000
            }
        })
        
        // 应用性能监控器
        registerPerformanceMonitor("application", object : PerformanceMonitor {
            override val name = "应用性能监控器"
            override val description = "监控应用级别的性能指标"
            override val category = MonitorCategory.APPLICATION
            
            override fun collectMetrics(): PerformanceMetrics {
                val responseTime = measureAverageResponseTime()
                val requestRate = measureRequestRate()
                val errorRate = measureErrorRate()
                val cacheHitRate = measureCacheHitRate()
                
                return PerformanceMetrics(
                    timestamp = System.currentTimeMillis(),
                    values = mapOf(
                        "response_time" to responseTime,
                        "request_rate" to requestRate,
                        "error_rate" to errorRate,
                        "cache_hit_rate" to cacheHitRate
                    ),
                    status = if (errorRate > 5.0) MetricsStatus.CRITICAL else 
                             if (responseTime > 1000 || errorRate > 1.0) MetricsStatus.WARNING else MetricsStatus.NORMAL
                )
            }
            
            private fun measureAverageResponseTime(): Double {
                // 这里应该测量实际的平均响应时间
                return Math.random() * 2000
            }
            
            private fun measureRequestRate(): Double {
                // 这里应该测量实际的请求率
                return Math.random() * 1000
            }
            
            private fun measureErrorRate(): Double {
                // 这里应该测量实际的错误率
                return Math.random() * 10
            }
            
            private fun measureCacheHitRate(): Double {
                // 这里应该测量实际的缓存命中率
                return Math.random() * 100
            }
        })
    }
    
    /**
     * 启动监控
     */
    private fun startMonitoring() {
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    collectPerformanceMetrics()
                    checkPerformanceAlerts()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordPerformanceMonitoringError("性能监控错误", e)
                }
            }
        }
        
        reportingJob = scope.launch {
            while (isActive) {
                try {
                    generatePerformanceReport()
                    delay(REPORT_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordPerformanceMonitoringError("性能报告生成错误", e)
                }
            }
        }
    }
    
    /**
     * 注册性能监控器
     */
    fun registerPerformanceMonitor(id: String, monitor: PerformanceMonitor) {
        performanceMonitors[id] = monitor
        performanceHistory[id] = mutableListOf()
        metrics.recordPerformanceMonitorRegistered(id, monitor.name)
    }
    
    /**
     * 收集性能指标
     */
    private fun collectPerformanceMetrics() {
        val currentTime = System.currentTimeMillis()
        
        for ((id, monitor) in performanceMonitors) {
            try {
                val metrics = monitor.collectMetrics()
                val snapshot = PerformanceSnapshot(
                    timestamp = currentTime,
                    metrics = metrics
                )
                
                // 添加到历史记录
                val history = performanceHistory[id] ?: mutableListOf()
                history.add(snapshot)
                
                // 限制历史记录大小
                if (history.size > HISTORY_SIZE) {
                    history.removeAt(0)
                }
                
                performanceHistory[id] = history
                
                metrics.recordPerformanceMetricsCollected(id, metrics.status)
            } catch (e: Exception) {
                metrics.recordPerformanceMonitoringError("监控器错误: $id", e)
            }
        }
        
        updateMonitoringState()
    }
    
    /**
     * 检查性能警报
     */
    private fun checkPerformanceAlerts() {
        val currentTime = System.currentTimeMillis()
        val newAlerts = mutableListOf<PerformanceAlert>()
        
        for ((id, history) in performanceHistory) {
            if (history.size < 10) continue // 需要足够的历史数据
            
            val recentSnapshots = history.takeLast(10)
            val latestSnapshot = recentSnapshots.last()
            
            // 检查关键指标状态
            if (latestSnapshot.metrics.status == MetricsStatus.CRITICAL) {
                newAlerts.add(PerformanceAlert(
                    id = "alert_${currentTime}_${id}",
                    monitorId = id,
                    monitorName = performanceMonitors[id]?.name ?: id,
                    severity = AlertSeverity.CRITICAL,
                    message = "检测到严重性能问题: ${latestSnapshot.metrics.values}",
                    timestamp = currentTime,
                    metrics = latestSnapshot.metrics
                ))
            } else if (latestSnapshot.metrics.status == MetricsStatus.WARNING) {
                newAlerts.add(PerformanceAlert(
                    id = "alert_${currentTime}_${id}",
                    monitorId = id,
                    monitorName = performanceMonitors[id]?.name ?: id,
                    severity = AlertSeverity.WARNING,
                    message = "检测到性能警告: ${latestSnapshot.metrics.values}",
                    timestamp = currentTime,
                    metrics = latestSnapshot.metrics
                ))
            }
            
            // 检查性能趋势
            val trendAlert = checkPerformanceTrend(id, recentSnapshots)
            if (trendAlert != null) {
                newAlerts.add(trendAlert)
            }
        }
        
        // 添加新警报
        performanceAlerts.addAll(newAlerts)
        
        // 限制警报数量
        if (performanceAlerts.size > 100) {
            performanceAlerts.subList(0, performanceAlerts.size - 100).clear()
        }
        
        // 记录警报
        for (alert in newAlerts) {
            metrics.recordPerformanceAlert(alert.monitorId, alert.severity.name, alert.message)
        }
    }
    
    /**
     * 检查性能趋势
     */
    private fun checkPerformanceTrend(
        monitorId: String,
        snapshots: List<PerformanceSnapshot>
    ): PerformanceAlert? {
        if (snapshots.size < 5) return null
        
        val monitor = performanceMonitors[monitorId] ?: return null
        
        // 根据监控器类型检查不同的趋势
        return when (monitor.category) {
            MonitorCategory.SYSTEM -> checkSystemPerformanceTrend(monitorId, snapshots)
            MonitorCategory.NETWORK -> checkNetworkPerformanceTrend(monitorId, snapshots)
            MonitorCategory.APPLICATION -> checkApplicationPerformanceTrend(monitorId, snapshots)
        }
    }
    
    /**
     * 检查系统性能趋势
     */
    private fun checkSystemPerformanceTrend(
        monitorId: String,
        snapshots: List<PerformanceSnapshot>
    ): PerformanceAlert? {
        val cpuValues = snapshots.mapNotNull { it.metrics.values["cpu_usage"] as? Double }
        val memoryValues = snapshots.mapNotNull { it.metrics.values["memory_usage_ratio"] as? Double }
        
        // 检查CPU使用率趋势
        if (cpuValues.size >= 5) {
            val recentAvg = cpuValues.takeLast(3).average()
            val previousAvg = cpuValues.dropLast(3).takeLast(3).average()
            
            if (recentAvg > previousAvg * 1.2) { // 20%增长
                return PerformanceAlert(
                    id = "trend_${System.currentTimeMillis()}_${monitorId}",
                    monitorId = monitorId,
                    monitorName = performanceMonitors[monitorId]?.name ?: monitorId,
                    severity = AlertSeverity.WARNING,
                    message = "CPU使用率呈上升趋势: ${String.format("%.1f%%", recentAvg)}",
                    timestamp = System.currentTimeMillis(),
                    metrics = snapshots.last().metrics
                )
            }
        }
        
        // 检查内存使用率趋势
        if (memoryValues.size >= 5) {
            val recentAvg = memoryValues.takeLast(3).average()
            val previousAvg = memoryValues.dropLast(3).takeLast(3).average()
            
            if (recentAvg > previousAvg * 1.2) { // 20%增长
                return PerformanceAlert(
                    id = "trend_${System.currentTimeMillis()}_${monitorId}",
                    monitorId = monitorId,
                    monitorName = performanceMonitors[monitorId]?.name ?: monitorId,
                    severity = AlertSeverity.WARNING,
                    message = "内存使用率呈上升趋势: ${String.format("%.1f%%", recentAvg * 100)}",
                    timestamp = System.currentTimeMillis(),
                    metrics = snapshots.last().metrics
                )
            }
        }
        
        return null
    }
    
    /**
     * 检查网络性能趋势
     */
    private fun checkNetworkPerformanceTrend(
        monitorId: String,
        snapshots: List<PerformanceSnapshot>
    ): PerformanceAlert? {
        val latencyValues = snapshots.mapNotNull { it.metrics.values["network_latency"] as? Double }
        
        if (latencyValues.size >= 5) {
            val recentAvg = latencyValues.takeLast(3).average()
            val previousAvg = latencyValues.dropLast(3).takeLast(3).average()
            
            if (recentAvg > previousAvg * 1.5) { // 50%增长
                return PerformanceAlert(
                    id = "trend_${System.currentTimeMillis()}_${monitorId}",
                    monitorId = monitorId,
                    monitorName = performanceMonitors[monitorId]?.name ?: monitorId,
                    severity = AlertSeverity.WARNING,
                    message = "网络延迟呈上升趋势: ${String.format("%.1fms", recentAvg)}",
                    timestamp = System.currentTimeMillis(),
                    metrics = snapshots.last().metrics
                )
            }
        }
        
        return null
    }
    
    /**
     * 检查应用性能趋势
     */
    private fun checkApplicationPerformanceTrend(
        monitorId: String,
        snapshots: List<PerformanceSnapshot>
    ): PerformanceAlert? {
        val responseTimeValues = snapshots.mapNotNull { it.metrics.values["response_time"] as? Double }
        val errorRateValues = snapshots.mapNotNull { it.metrics.values["error_rate"] as? Double }
        
        // 检查响应时间趋势
        if (responseTimeValues.size >= 5) {
            val recentAvg = responseTimeValues.takeLast(3).average()
            val previousAvg = responseTimeValues.dropLast(3).takeLast(3).average()
            
            if (recentAvg > previousAvg * 1.3) { // 30%增长
                return PerformanceAlert(
                    id = "trend_${System.currentTimeMillis()}_${monitorId}",
                    monitorId = monitorId,
                    monitorName = performanceMonitors[monitorId]?.name ?: monitorId,
                    severity = AlertSeverity.WARNING,
                    message = "响应时间呈上升趋势: ${String.format("%.1fms", recentAvg)}",
                    timestamp = System.currentTimeMillis(),
                    metrics = snapshots.last().metrics
                )
            }
        }
        
        // 检查错误率趋势
        if (errorRateValues.size >= 5) {
            val recentAvg = errorRateValues.takeLast(3).average()
            val previousAvg = errorRateValues.dropLast(3).takeLast(3).average()
            
            if (recentAvg > previousAvg * 2.0) { // 100%增长
                return PerformanceAlert(
                    id = "trend_${System.currentTimeMillis()}_${monitorId}",
                    monitorId = monitorId,
                    monitorName = performanceMonitors[monitorId]?.name ?: monitorId,
                    severity = AlertSeverity.CRITICAL,
                    message = "错误率呈上升趋势: ${String.format("%.1f%%", recentAvg)}",
                    timestamp = System.currentTimeMillis(),
                    metrics = snapshots.last().metrics
                )
            }
        }
        
        return null
    }
    
    /**
     * 生成性能报告
     */
    private fun generatePerformanceReport() {
        val currentTime = System.currentTimeMillis()
        val report = PerformanceReport(
            timestamp = currentTime,
            period = REPORT_INTERVAL,
            monitors = performanceMonitors.keys.toList(),
            snapshots = getCurrentSnapshots(),
            alerts = getRecentAlerts(),
            summary = generatePerformanceSummary()
        )
        
        metrics.recordPerformanceReportGenerated(report.monitors.size, report.alerts.size)
    }
    
    /**
     * 获取当前快照
     */
    private fun getCurrentSnapshots(): Map<String, PerformanceSnapshot> {
        val snapshots = mutableMapOf<String, PerformanceSnapshot>()
        
        for ((id, history) in performanceHistory) {
            if (history.isNotEmpty()) {
                snapshots[id] = history.last()
            }
        }
        
        return snapshots
    }
    
    /**
     * 获取最近的警报
     */
    private fun getRecentAlerts(): List<PerformanceAlert> {
        val cutoffTime = System.currentTimeMillis() - REPORT_INTERVAL
        return performanceAlerts.filter { it.timestamp >= cutoffTime }
    }
    
    /**
     * 生成性能摘要
     */
    private fun generatePerformanceSummary(): PerformanceSummary {
        val snapshots = getCurrentSnapshots()
        val totalMonitors = snapshots.size
        val healthyMonitors = snapshots.values.count { it.metrics.status == MetricsStatus.NORMAL }
        val warningMonitors = snapshots.values.count { it.metrics.status == MetricsStatus.WARNING }
        val criticalMonitors = snapshots.values.count { it.metrics.status == MetricsStatus.CRITICAL }
        
        return PerformanceSummary(
            totalMonitors = totalMonitors,
            healthyMonitors = healthyMonitors,
            warningMonitors = warningMonitors,
            criticalMonitors = criticalMonitors,
            overallHealth = if (criticalMonitors > 0) HealthStatus.CRITICAL else
                          if (warningMonitors > 0) HealthStatus.WARNING else HealthStatus.HEALTHY
        )
    }
    
    /**
     * 更新监控状态
     */
    private fun updateMonitoringState() {
        val currentState = _monitoringState.value
        val newState = currentState.copy(
            activeMonitors = performanceMonitors.keys.toList(),
            lastUpdateTime = System.currentTimeMillis()
        )
        _monitoringState.value = newState
        
        updateMonitoringStatistics()
    }
    
    /**
     * 更新监控统计信息
     */
    private fun updateMonitoringStatistics() {
        val currentStats = monitoringStats.get()
        val newStats = currentStats.copy(
            totalCollections = currentStats.totalCollections + 1,
            lastUpdateTime = System.currentTimeMillis()
        )
        monitoringStats.set(newStats)
    }
    
    /**
     * 获取性能历史
     */
    fun getPerformanceHistory(monitorId: String, limit: Int = 100): List<PerformanceSnapshot> {
        return performanceHistory[monitorId]?.takeLast(limit) ?: emptyList()
    }
    
    /**
     * 获取所有性能历史
     */
    fun getAllPerformanceHistory(limit: Int = 100): Map<String, List<PerformanceSnapshot>> {
        val result = mutableMapOf<String, List<PerformanceSnapshot>>()
        
        for ((id, history) in performanceHistory) {
            result[id] = history.takeLast(limit)
        }
        
        return result
    }
    
    /**
     * 获取性能警报
     */
    fun getPerformanceAlerts(limit: Int = 50): List<PerformanceAlert> {
        return performanceAlerts.takeLast(limit)
    }
    
    /**
     * 获取监控统计信息
     */
    fun getMonitoringStatistics(): MonitoringStatistics {
        return monitoringStats.get()
    }
    
    /**
     * 获取监控状态
     */
    fun getMonitoringState(): MonitoringState {
        return _monitoringState.value
    }
    
    /**
     * 获取可用的监控器
     */
    fun getAvailableMonitors(): List<MonitorInfo> {
        return performanceMonitors.entries.map { (id, monitor) ->
            MonitorInfo(
                id = id,
                name = monitor.name,
                description = monitor.description,
                category = monitor.category,
                hasHistory = performanceHistory[id]?.isNotEmpty() == true
            )
        }
    }
    
    /**
     * 清除性能警报
     */
    fun clearPerformanceAlerts() {
        performanceAlerts.clear()
        metrics.recordPerformanceAlertsCleared()
    }
    
    /**
     * 手动触发性能检查
     */
    suspend fun triggerPerformanceCheck() {
        collectPerformanceMetrics()
        checkPerformanceAlerts()
        generatePerformanceReport()
    }
    
    /**
     * 销毁持续性能监控器
     */
    fun destroy() {
        monitoringJob?.cancel()
        reportingJob?.cancel()
        scope.cancel()
        
        performanceMonitors.clear()
        performanceHistory.clear()
        performanceAlerts.clear()
        
        INSTANCE = null
    }
}

/**
 * 性能监控器接口
 */
interface PerformanceMonitor {
    val name: String
    val description: String
    val category: MonitorCategory
    
    fun collectMetrics(): PerformanceMetrics
}

/**
 * 性能指标
 */
data class PerformanceMetrics(
    val timestamp: Long,
    val values: Map<String, Any>,
    val status: MetricsStatus
)

/**
 * 性能快照
 */
data class PerformanceSnapshot(
    val timestamp: Long,
    val metrics: PerformanceMetrics
)

/**
 * 性能警报
 */
data class PerformanceAlert(
    val id: String,
    val monitorId: String,
    val monitorName: String,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Long,
    val metrics: PerformanceMetrics
)

/**
 * 性能报告
 */
data class PerformanceReport(
    val timestamp: Long,
    val period: Long,
    val monitors: List<String>,
    val snapshots: Map<String, PerformanceSnapshot>,
    val alerts: List<PerformanceAlert>,
    val summary: PerformanceSummary
)

/**
 * 性能摘要
 */
data class PerformanceSummary(
    val totalMonitors: Int,
    val healthyMonitors: Int,
    val warningMonitors: Int,
    val criticalMonitors: Int,
    val overallHealth: HealthStatus
)

/**
 * 监控状态
 */
data class MonitoringState(
    val activeMonitors: List<String> = emptyList(),
    val lastUpdateTime: Long = 0L
)

/**
 * 监控统计信息
 */
data class MonitoringStatistics(
    val totalCollections: Long = 0L,
    val totalAlerts: Long = 0L,
    val totalReports: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * 监控器信息
 */
data class MonitorInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: MonitorCategory,
    val hasHistory: Boolean
)

/**
 * 监控器类别
 */
enum class MonitorCategory {
    SYSTEM,
    NETWORK,
    APPLICATION,
    DATABASE,
    CACHE
}

/**
 * 指标状态
 */
enum class MetricsStatus {
    NORMAL,
    WARNING,
    CRITICAL
}

/**
 * 警报严重程度
 */
enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * 健康状态
 */
enum class HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL
}

/**
 * 性能监控工厂
 */
object PerformanceMonitorFactory {
    
    /**
     * 创建自定义性能监控器
     */
    fun createCustomMonitor(
        name: String,
        description: String,
        category: MonitorCategory,
        metricsCollector: () -> PerformanceMetrics
    ): PerformanceMonitor {
        return object : PerformanceMonitor {
            override val name = name
            override val description = description
            override val category = category
            
            override fun collectMetrics(): PerformanceMetrics {
                return metricsCollector()
            }
        }
    }
    
    /**
     * 创建阈值监控器
     */
    fun createThresholdMonitor(
        name: String,
        description: String,
        category: MonitorCategory,
        metricName: String,
        metricProvider: () -> Double,
        warningThreshold: Double,
        criticalThreshold: Double
    ): PerformanceMonitor {
        return object : PerformanceMonitor {
            override val name = name
            override val description = description
            override val category = category
            
            override fun collectMetrics(): PerformanceMetrics {
                val value = metricProvider()
                val status = when {
                    value >= criticalThreshold -> MetricsStatus.CRITICAL
                    value >= warningThreshold -> MetricsStatus.WARNING
                    else -> MetricsStatus.NORMAL
                }
                
                return PerformanceMetrics(
                    timestamp = System.currentTimeMillis(),
                    values = mapOf(metricName to value),
                    status = status
                )
            }
        }
    }
}