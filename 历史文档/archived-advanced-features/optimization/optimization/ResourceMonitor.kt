package com.empathy.ai.data.optimization

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.*
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 资源监控器
 * 
 * 负责监控系统资源使用情况，包括CPU、内存、网络等
 * 提供实时资源状态报告和资源使用趋势分析
 * 
 * 功能：
 * 1. 实时资源监控
 * 2. 资源使用趋势分析
 * 3. 资源阈值告警
 * 4. 资源使用优化建议
 */
class ResourceMonitor private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ResourceMonitor"
        
        // 监控配置
        private const val MONITORING_INTERVAL_MS = 10_000L // 10秒
        private const val RESOURCE_HISTORY_SIZE = 144 // 24小时（每10分钟一个点）
        private const val MEMORY_WARNING_THRESHOLD = 0.8 // 80%内存使用率
        private const val CPU_WARNING_THRESHOLD = 0.8 // 80%CPU使用率
        
        @Volatile
        private var INSTANCE: ResourceMonitor? = null
        
        fun getInstance(context: Context): ResourceMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ResourceMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val monitoringConfig = AtomicReference(MonitoringConfig())
    private val resourceHistory = ConcurrentHashMap<ResourceType, ResourceHistory>()
    private val currentMetrics = AtomicReference(ResourceMetrics())
    private val alertThresholds = ConcurrentHashMap<ResourceType, AlertThreshold>()
    private val resourceAlerts = ConcurrentHashMap<ResourceType, MutableList<ResourceAlert>>()
    
    private var isMonitoringEnabled = false
    private var monitoringJob: Job? = null
    
    // 系统服务
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val runtime = Runtime.getRuntime()
    
    init {
        initializeResourceHistories()
        initializeDefaultThresholds()
        Log.i(TAG, "资源监控器初始化完成")
    }
    
    /**
     * 启用资源监控
     */
    fun enableMonitoring(config: MonitoringConfig = MonitoringConfig()) {
        if (isMonitoringEnabled) {
            Log.w(TAG, "资源监控已启用")
            return
        }
        
        monitoringConfig.set(config)
        isMonitoringEnabled = true
        
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    collectResourceMetrics()
                    checkResourceThresholds()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "资源监控周期执行失败", e)
                }
            }
        }
        
        Log.i(TAG, "资源监控已启用")
    }
    
    /**
     * 禁用资源监控
     */
    fun disableMonitoring() {
        if (!isMonitoringEnabled) {
            Log.w(TAG, "资源监控未启用")
            return
        }
        
        isMonitoringEnabled = false
        monitoringJob?.cancel()
        monitoringJob = null
        
        Log.i(TAG, "资源监控已禁用")
    }
    
    /**
     * 获取当前资源指标
     */
    fun getCurrentMetrics(): ResourceMetrics {
        return currentMetrics.get()
    }
    
    /**
     * 获取资源使用历史
     */
    fun getResourceHistory(resourceType: ResourceType, durationMs: Long): List<ResourceDataPoint> {
        val history = resourceHistory[resourceType] ?: return emptyList()
        val cutoffTime = System.currentTimeMillis() - durationMs
        
        return history.dataPoints.filter { it.timestamp >= cutoffTime }
    }
    
    /**
     * 获取资源使用趋势
     */
    fun getResourceTrends(resourceType: ResourceType, durationMs: Long): ResourceTrend {
        val history = getResourceHistory(resourceType, durationMs)
        if (history.size < 2) {
            return ResourceTrend.STABLE
        }
        
        val recentValues = history.takeLast(10).map { it.value }
        val olderValues = history.dropLast(10).takeLast(10).map { it.value }
        
        if (olderValues.isEmpty()) {
            return ResourceTrend.STABLE
        }
        
        val recentAvg = recentValues.average()
        val olderAvg = olderValues.average()
        val changePercent = (recentAvg - olderAvg) / olderAvg
        
        return when {
            changePercent > 0.1 -> ResourceTrend.INCREASING
            changePercent < -0.1 -> ResourceTrend.DECREASING
            else -> ResourceTrend.STABLE
        }
    }
    
    /**
     * 获取资源使用报告
     */
    fun getResourceReport(): ResourceReport {
        val currentMetrics = currentMetrics.get()
        val trends = mutableMapOf<ResourceType, ResourceTrend>()
        val alerts = resourceAlerts.values.flatten()
        
        ResourceType.values().forEach { type ->
            trends[type] = getResourceTrends(type, 60 * 60 * 1000) // 最近1小时
        }
        
        return ResourceReport(
            timestamp = System.currentTimeMillis(),
            currentMetrics = currentMetrics,
            trends = trends,
            recentAlerts = alerts.filter { 
                System.currentTimeMillis() - it.timestamp < 60 * 60 * 1000 
            },
            recommendations = generateResourceRecommendations(currentMetrics, trends)
        )
    }
    
    /**
     * 更新监控配置
     */
    fun updateConfig(config: MonitoringConfig) {
        monitoringConfig.set(config)
        Log.i(TAG, "监控配置已更新: $config")
    }
    
    /**
     * 设置资源告警阈值
     */
    fun setAlertThreshold(resourceType: ResourceType, threshold: AlertThreshold) {
        alertThresholds[resourceType] = threshold
        Log.i(TAG, "资源告警阈值已设置: $resourceType = $threshold")
    }
    
    /**
     * 手动收集资源指标
     */
    fun collectMetrics(): ResourceMetrics {
        return collectResourceMetricsInternal()
    }
    
    /**
     * 收集资源指标
     */
    private suspend fun collectResourceMetrics() {
        val metrics = collectResourceMetricsInternal()
        currentMetrics.set(metrics)
        
        // 更新历史记录
        ResourceType.values().forEach { type ->
            val value = when (type) {
                ResourceType.MEMORY -> metrics.memoryUsagePercent
                ResourceType.CPU -> metrics.cpuUsagePercent
                ResourceType.NETWORK -> metrics.networkUsagePercent
                ResourceType.DISK -> metrics.diskUsagePercent
                ResourceType.BATTERY -> metrics.batteryLevelPercent
            }
            
            val history = resourceHistory[type] ?: return@forEach
            history.addDataPoint(ResourceDataPoint(
                timestamp = System.currentTimeMillis(),
                value = value
            ))
            
            // 保持历史大小
            if (history.dataPoints.size > RESOURCE_HISTORY_SIZE) {
                history.dataPoints.removeAt(0)
            }
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "资源指标收集完成: $metrics")
        }
    }
    
    /**
     * 内部资源指标收集方法
     */
    private fun collectResourceMetricsInternal(): ResourceMetrics {
        // 内存使用情况
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = usedMemory.toDouble() / maxMemory
        
        // 获取系统内存信息
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val systemAvailableMemory = memoryInfo.availMem
        val systemTotalMemory = memoryInfo.totalMem
        val systemMemoryUsagePercent = 1.0 - (systemAvailableMemory.toDouble() / systemTotalMemory)
        
        // CPU使用情况（简化计算）
        val cpuUsagePercent = calculateCpuUsage()
        
        // 网络使用情况（简化计算）
        val networkUsagePercent = calculateNetworkUsage()
        
        // 磁盘使用情况
        val diskUsagePercent = calculateDiskUsage()
        
        // 电池电量
        val batteryLevelPercent = calculateBatteryLevel()
        
        // 垃圾回收信息
        val gcInfo = getGarbageCollectionInfo()
        
        return ResourceMetrics(
            timestamp = System.currentTimeMillis(),
            memoryUsagePercent = memoryUsagePercent,
            systemMemoryUsagePercent = systemMemoryUsagePercent,
            totalMemoryBytes = totalMemory,
            usedMemoryBytes = usedMemory,
            freeMemoryBytes = freeMemory,
            maxMemoryBytes = maxMemory,
            cpuUsagePercent = cpuUsagePercent,
            networkUsagePercent = networkUsagePercent,
            diskUsagePercent = diskUsagePercent,
            batteryLevelPercent = batteryLevelPercent,
            gcInfo = gcInfo
        )
    }
    
    /**
     * 计算CPU使用率
     */
    private fun calculateCpuUsage(): Double {
        return try {
            // 简化的CPU使用率计算
            val loadAverage = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
            val availableProcessors = Runtime.getRuntime().availableProcessors()
            
            if (loadAverage >= 0) {
                minOf(loadAverage / availableProcessors, 1.0)
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.w(TAG, "CPU使用率计算失败", e)
            0.0
        }
    }
    
    /**
     * 计算网络使用率
     */
    private fun calculateNetworkUsage(): Double {
        // 简化的网络使用率计算
        // 在实际应用中，可以使用TrafficStats获取更精确的数据
        return 0.0
    }
    
    /**
     * 计算磁盘使用率
     */
    private fun calculateDiskUsage(): Double {
        return try {
            val dataDir = context.filesDir
            val totalSpace = dataDir.totalSpace
            val freeSpace = dataDir.freeSpace
            val usedSpace = totalSpace - freeSpace
            
            usedSpace.toDouble() / totalSpace
        } catch (e: Exception) {
            Log.w(TAG, "磁盘使用率计算失败", e)
            0.0
        }
    }
    
    /**
     * 计算电池电量
     */
    private fun calculateBatteryLevel(): Double {
        // 简化的电池电量计算
        // 在实际应用中，可以使用BatteryManager获取精确数据
        return 0.8 // 假设80%
    }
    
    /**
     * 获取垃圾回收信息
     */
    private fun getGarbageCollectionInfo(): GarbageCollectionInfo {
        return try {
            val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
            val totalGcCount = gcBeans.sumOf { it.collectionCount }
            val totalGcTime = gcBeans.sumOf { it.collectionTime }
            
            GarbageCollectionInfo(
                totalGcCount = totalGcCount,
                totalGcTimeMs = totalGcTime,
                gcCountPerMinute = if (totalGcTime > 0) (totalGcCount * 60000.0) / totalGcTime else 0.0
            )
        } catch (e: Exception) {
            Log.w(TAG, "垃圾回收信息获取失败", e)
            GarbageCollectionInfo(0, 0, 0.0)
        }
    }
    
    /**
     * 检查资源阈值
     */
    private fun checkResourceThresholds() {
        val metrics = currentMetrics.get()
        val currentTime = System.currentTimeMillis()
        
        ResourceType.values().forEach { type ->
            val threshold = alertThresholds[type] ?: return@forEach
            val currentValue = when (type) {
                ResourceType.MEMORY -> metrics.memoryUsagePercent
                ResourceType.CPU -> metrics.cpuUsagePercent
                ResourceType.NETWORK -> metrics.networkUsagePercent
                ResourceType.DISK -> metrics.diskUsagePercent
                ResourceType.BATTERY -> 1.0 - metrics.batteryLevelPercent // 电池低电量
            }
            
            if (currentValue >= threshold.warningThreshold) {
                val severity = when {
                    currentValue >= threshold.criticalThreshold -> AlertSeverity.CRITICAL
                    currentValue >= threshold.warningThreshold -> AlertSeverity.WARNING
                    else -> AlertSeverity.INFO
                }
                
                val alert = ResourceAlert(
                    type = type,
                    severity = severity,
                    currentValue = currentValue,
                    threshold = threshold.warningThreshold,
                    message = generateAlertMessage(type, currentValue, threshold),
                    timestamp = currentTime
                )
                
                // 记录告警
                resourceAlerts.computeIfAbsent(type) { mutableListOf() }.add(alert)
                
                // 保持告警历史大小
                val alerts = resourceAlerts[type] ?: return@forEach
                if (alerts.size > 100) {
                    alerts.removeAt(0)
                }
                
                Log.w(TAG, "资源告警: ${alert.message}")
            }
        }
    }
    
    /**
     * 生成告警消息
     */
    private fun generateAlertMessage(
        type: ResourceType,
        currentValue: Double,
        threshold: AlertThreshold
    ): String {
        val typeName = when (type) {
            ResourceType.MEMORY -> "内存"
            ResourceType.CPU -> "CPU"
            ResourceType.NETWORK -> "网络"
            ResourceType.DISK -> "磁盘"
            ResourceType.BATTERY -> "电池"
        }
        
        return "${typeName}使用率${String.format("%.1f%%", currentValue * 100)}超过阈值${String.format("%.1f%%", threshold.warningThreshold * 100)}"
    }
    
    /**
     * 生成资源使用建议
     */
    private fun generateResourceRecommendations(
        metrics: ResourceMetrics,
        trends: Map<ResourceType, ResourceTrend>
    ): List<ResourceRecommendation> {
        val recommendations = mutableListOf<ResourceRecommendation>()
        
        // 内存使用建议
        when {
            metrics.memoryUsagePercent > 0.9 -> {
                recommendations.add(ResourceRecommendation(
                    type = ResourceType.MEMORY,
                    priority = RecommendationPriority.CRITICAL,
                    message = "内存使用率过高，建议立即释放内存",
                    action = "释放缓存、清理临时数据、减少内存分配"
                ))
            }
            
            metrics.memoryUsagePercent > 0.8 -> {
                recommendations.add(ResourceRecommendation(
                    type = ResourceType.MEMORY,
                    priority = RecommendationPriority.HIGH,
                    message = "内存使用率较高，建议优化内存使用",
                    action = "检查内存泄漏、优化数据结构、启用内存池"
                ))
            }
            
            trends[ResourceType.MEMORY] == ResourceTrend.INCREASING -> {
                recommendations.add(ResourceRecommendation(
                    type = ResourceType.MEMORY,
                    priority = RecommendationPriority.MEDIUM,
                    message = "内存使用率持续上升，建议监控内存使用趋势",
                    action = "分析内存增长原因、设置内存使用限制"
                ))
            }
        }
        
        // CPU使用建议
        when {
            metrics.cpuUsagePercent > 0.9 -> {
                recommendations.add(ResourceRecommendation(
                    type = ResourceType.CPU,
                    priority = RecommendationPriority.CRITICAL,
                    message = "CPU使用率过高，系统可能过载",
                    action = "优化算法、减少计算量、启用并行处理"
                ))
            }
            
            metrics.cpuUsagePercent > 0.8 -> {
                recommendations.add(ResourceRecommendation(
                    type = ResourceType.CPU,
                    priority = RecommendationPriority.HIGH,
                    message = "CPU使用率较高，建议优化CPU使用",
                    action = "优化热点代码、减少循环复杂度、使用缓存"
                ))
            }
        }
        
        // 垃圾回收建议
        if (metrics.gcInfo.gcCountPerMinute > 10) {
            recommendations.add(ResourceRecommendation(
                type = ResourceType.MEMORY,
                priority = RecommendationPriority.MEDIUM,
                message = "垃圾回收频繁，建议优化内存分配",
                action = "减少临时对象创建、使用对象池、优化内存分配策略"
            ))
        }
        
        return recommendations.sortedByDescending { it.priority.value }
    }
    
    /**
     * 初始化资源历史记录
     */
    private fun initializeResourceHistories() {
        ResourceType.values().forEach { type ->
            resourceHistory[type] = ResourceHistory()
        }
        
        Log.d(TAG, "资源历史记录初始化完成")
    }
    
    /**
     * 初始化默认阈值
     */
    private fun initializeDefaultThresholds() {
        alertThresholds[ResourceType.MEMORY] = AlertThreshold(
            warningThreshold = MEMORY_WARNING_THRESHOLD,
            criticalThreshold = 0.9
        )
        
        alertThresholds[ResourceType.CPU] = AlertThreshold(
            warningThreshold = CPU_WARNING_THRESHOLD,
            criticalThreshold = 0.9
        )
        
        alertThresholds[ResourceType.NETWORK] = AlertThreshold(
            warningThreshold = 0.8,
            criticalThreshold = 0.9
        )
        
        alertThresholds[ResourceType.DISK] = AlertThreshold(
            warningThreshold = 0.85,
            criticalThreshold = 0.95
        )
        
        alertThresholds[ResourceType.BATTERY] = AlertThreshold(
            warningThreshold = 0.2, // 20%电量
            criticalThreshold = 0.1 // 10%电量
        )
        
        Log.d(TAG, "默认资源告警阈值初始化完成")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        disableMonitoring()
        scope.cancel()
        resourceHistory.clear()
        resourceAlerts.clear()
        
        Log.i(TAG, "资源监控器资源清理完成")
    }
    
    /**
     * 监控配置
     */
    data class MonitoringConfig(
        val enableMemoryMonitoring: Boolean = true,
        val enableCpuMonitoring: Boolean = true,
        val enableNetworkMonitoring: Boolean = true,
        val enableDiskMonitoring: Boolean = true,
        val enableBatteryMonitoring: Boolean = true,
        val monitoringIntervalMs: Long = MONITORING_INTERVAL_MS,
        val historyRetentionHours: Int = 24
    )
    
    /**
     * 资源类型
     */
    enum class ResourceType {
        MEMORY,
        CPU,
        NETWORK,
        DISK,
        BATTERY
    }
    
    /**
     * 资源指标
     */
    data class ResourceMetrics(
        val timestamp: Long = 0L,
        val memoryUsagePercent: Double = 0.0,
        val systemMemoryUsagePercent: Double = 0.0,
        val totalMemoryBytes: Long = 0L,
        val usedMemoryBytes: Long = 0L,
        val freeMemoryBytes: Long = 0L,
        val maxMemoryBytes: Long = 0L,
        val cpuUsagePercent: Double = 0.0,
        val networkUsagePercent: Double = 0.0,
        val diskUsagePercent: Double = 0.0,
        val batteryLevelPercent: Double = 0.0,
        val gcInfo: GarbageCollectionInfo = GarbageCollectionInfo()
    )
    
    /**
     * 垃圾回收信息
     */
    data class GarbageCollectionInfo(
        val totalGcCount: Long = 0L,
        val totalGcTimeMs: Long = 0L,
        val gcCountPerMinute: Double = 0.0
    )
    
    /**
     * 资源历史记录
     */
    class ResourceHistory {
        val dataPoints = mutableListOf<ResourceDataPoint>()
        
        fun addDataPoint(dataPoint: ResourceDataPoint) {
            dataPoints.add(dataPoint)
        }
    }
    
    /**
     * 资源数据点
     */
    data class ResourceDataPoint(
        val timestamp: Long,
        val value: Double
    )
    
    /**
     * 资源趋势
     */
    enum class ResourceTrend {
        STABLE,      // 稳定
        INCREASING,  // 上升
        DECREASING   // 下降
    }
    
    /**
     * 告警阈值
     */
    data class AlertThreshold(
        val warningThreshold: Double,
        val criticalThreshold: Double
    )
    
    /**
     * 告警严重程度
     */
    enum class AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
    
    /**
     * 资源告警
     */
    data class ResourceAlert(
        val type: ResourceType,
        val severity: AlertSeverity,
        val currentValue: Double,
        val threshold: Double,
        val message: String,
        val timestamp: Long
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
     * 资源使用建议
     */
    data class ResourceRecommendation(
        val type: ResourceType,
        val priority: RecommendationPriority,
        val message: String,
        val action: String
    )
    
    /**
     * 资源使用报告
     */
    data class ResourceReport(
        val timestamp: Long,
        val currentMetrics: ResourceMetrics,
        val trends: Map<ResourceType, ResourceTrend>,
        val recentAlerts: List<ResourceAlert>,
        val recommendations: List<ResourceRecommendation>
    )
}