package com.empathy.ai.data.alerting

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 告警管理器
 * 
 * 负责管理告警的生成、评估、分发和处理
 * 提供灵活的告警规则和通知机制
 */
class AlertManager {
    
    companion object {
        private const val TAG = "AlertManager"
        
        // 告警配置
        private const val MAX_ACTIVE_ALERTS = 100
        private const val MAX_ALERT_HISTORY = 1000
        private const val ALERT_COOLDOWN_PERIOD = 5 * 60 * 1000L // 5分钟
        private const val ALERT_RETENTION_PERIOD = 7 * 24 * 60 * 60 * 1000L // 7天
        
        // 告警级别
        private const val CRITICAL_ALERT_THRESHOLD = 0.8
        private const val WARNING_ALERT_THRESHOLD = 0.6
    }
    
    // 活跃告警
    private val activeAlerts = ConcurrentHashMap<String, Alert>()
    
    // 告警历史
    private val alertHistory = CopyOnWriteArrayList<Alert>()
    
    // 告警规则
    private val alertRules = ConcurrentHashMap<String, AlertRule>()
    
    // 告警监听器
    private val alertListeners = CopyOnWriteArrayList<AlertListener>()
    
    // 告警抑制器
    private val alertSuppressors = ConcurrentHashMap<String, AlertSuppressor>()
    
    // 定期任务执行器
    private val scheduledExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { r ->
        Thread(r, "AlertManager-${r.hashCode()}").apply {
            isDaemon = true
        }
    }
    
    // 告警ID生成器
    private val alertIdGenerator = java.util.concurrent.atomic.AtomicLong(0)
    
    init {
        // 启动定期清理任务
        scheduledExecutor.scheduleAtFixedRate({
            try {
                cleanupExpiredAlerts()
                evaluateAlertRules()
            } catch (e: Exception) {
                Log.e(TAG, "定期告警任务失败", e)
            }
        }, 1, 1, TimeUnit.MINUTES)
        
        // 初始化默认告警规则
        initializeDefaultAlertRules()
        
        Log.i(TAG, "AlertManager 初始化完成")
    }
    
    /**
     * 添加告警监听器
     */
    fun addAlertListener(listener: AlertListener) {
        alertListeners.add(listener)
        Log.i(TAG, "添加告警监听器")
    }
    
    /**
     * 移除告警监听器
     */
    fun removeAlertListener(listener: AlertListener) {
        alertListeners.remove(listener)
        Log.i(TAG, "移除告警监听器")
    }
    
    /**
     * 添加告警规则
     */
    fun addAlertRule(rule: AlertRule) {
        alertRules[rule.id] = rule
        Log.i(TAG, "添加告警规则: ${rule.id}")
    }
    
    /**
     * 移除告警规则
     */
    fun removeAlertRule(ruleId: String) {
        alertRules.remove(ruleId)
        Log.i(TAG, "移除告警规则: $ruleId")
    }
    
    /**
     * 添加告警抑制器
     */
    fun addAlertSuppressor(suppressor: AlertSuppressor) {
        alertSuppressors[suppressor.id] = suppressor
        Log.i(TAG, "添加告警抑制器: ${suppressor.id}")
    }
    
    /**
     * 移除告警抑制器
     */
    fun removeAlertSuppressor(suppressorId: String) {
        alertSuppressors.remove(suppressorId)
        Log.i(TAG, "移除告警抑制器: $suppressorId")
    }
    
    /**
     * 手动触发告警
     */
    fun triggerAlert(
        type: AlertType,
        level: AlertLevel,
        title: String,
        message: String,
        source: String = "manual",
        metadata: Map<String, Any> = emptyMap()
    ): String {
        val alertId = generateAlertId()
        val alert = Alert(
            id = alertId,
            type = type,
            level = level,
            title = title,
            message = message,
            source = source,
            timestamp = System.currentTimeMillis(),
            metadata = metadata,
            status = AlertStatus.ACTIVE
        )
        
        // 检查告警抑制
        if (shouldSuppressAlert(alert)) {
            Log.d(TAG, "告警被抑制: $alertId")
            return alertId
        }
        
        // 添加到活跃告警
        activeAlerts[alertId] = alert
        
        // 添加到历史记录
        synchronized(alertHistory) {
            alertHistory.add(alert)
            
            // 保持历史大小
            while (alertHistory.size > MAX_ALERT_HISTORY) {
                alertHistory.removeAt(0)
            }
        }
        
        // 通知监听器
        notifyAlertListeners(alert)
        
        // 记录日志
        logAlert(alert)
        
        return alertId
    }
    
    /**
     * 解除告警
     */
    fun resolveAlert(alertId: String, resolution: String = "手动解决") {
        val alert = activeAlerts[alertId]
        if (alert != null) {
            val resolvedAlert = alert.copy(
                status = AlertStatus.RESOLVED,
                resolvedAt = System.currentTimeMillis(),
                resolution = resolution
            )
            
            activeAlerts[alertId] = resolvedAlert
            
            // 通知监听器
            notifyAlertListeners(resolvedAlert)
            
            Log.i(TAG, "告警已解决: $alertId, 解决方案: $resolution")
        }
    }
    
    /**
     * 获取活跃告警
     */
    fun getActiveAlerts(): List<Alert> {
        return activeAlerts.values.toList()
    }
    
    /**
     * 获取活跃告警数量
     */
    fun getActiveAlertCount(): Int {
        return activeAlerts.size
    }
    
    /**
     * 获取告警历史
     */
    fun getAlertHistory(limit: Int = 100): List<Alert> {
        return synchronized(alertHistory) {
            alertHistory.takeLast(limit)
        }
    }
    
    /**
     * 获取告警统计
     */
    fun getAlertStatistics(): AlertStatistics {
        val allAlerts = synchronized(alertHistory) {
            alertHistory.toList() + activeAlerts.values.toList()
        }
        
        val totalAlerts = allAlerts.size
        val activeAlerts = allAlerts.count { it.status == AlertStatus.ACTIVE }
        val resolvedAlerts = allAlerts.count { it.status == AlertStatus.RESOLVED }
        val suppressedAlerts = allAlerts.count { it.status == AlertStatus.SUPPRESSED }
        
        // 按类型分组
        val alertsByType = allAlerts.groupBy { it.type }
        val typeDistribution = alertsByType.mapValues { it.value.size }
        
        // 按级别分组
        val alertsByLevel = allAlerts.groupBy { it.level }
        val levelDistribution = alertsByLevel.mapValues { it.value.size }
        
        // 按来源分组
        val alertsBySource = allAlerts.groupBy { it.source }
        val sourceDistribution = alertsBySource.mapValues { it.value.size }
        
        // 计算平均解决时间
        val resolvedAlertsWithTime = allAlerts.filter { it.status == AlertStatus.RESOLVED && it.resolvedAt != null }
        val avgResolutionTime = if (resolvedAlertsWithTime.isNotEmpty()) {
            resolvedAlertsWithTime.map { it.resolvedAt!! - it.timestamp }.average()
        } else {
            0.0
        }
        
        return AlertStatistics(
            totalAlerts = totalAlerts,
            activeAlerts = activeAlerts,
            resolvedAlerts = resolvedAlerts,
            suppressedAlerts = suppressedAlerts,
            typeDistribution = typeDistribution,
            levelDistribution = levelDistribution,
            sourceDistribution = sourceDistribution,
            averageResolutionTimeMs = avgResolutionTime.toLong()
        )
    }
    
    /**
     * 评估告警规则
     */
    fun evaluateAlertRules() {
        alertRules.values.forEach { rule ->
            try {
                if (rule.enabled) {
                    val evaluation = rule.evaluate()
                    if (evaluation.shouldTrigger) {
                        triggerAlert(
                            type = rule.alertType,
                            level = rule.alertLevel,
                            title = rule.generateTitle(evaluation),
                            message = rule.generateMessage(evaluation),
                            source = "rule:${rule.id}",
                            metadata = mapOf(
                                "ruleId" to rule.id,
                                "evaluation" to evaluation.metadata
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "评估告警规则失败: ${rule.id}", e)
            }
        }
    }
    
    /**
     * 清理过期告警
     */
    fun cleanupExpiredAlerts() {
        val now = System.currentTimeMillis()
        
        // 清理活跃告警
        activeAlerts.entries.removeIf { (_, alert) ->
            val shouldRemove = when (alert.status) {
                AlertStatus.ACTIVE -> now - alert.timestamp > ALERT_RETENTION_PERIOD
                AlertStatus.RESOLVED -> alert.resolvedAt != null && now - alert.resolvedAt!! > ALERT_RETENTION_PERIOD
                AlertStatus.SUPPRESSED -> now - alert.timestamp > ALERT_RETENTION_PERIOD
            }
            
            if (shouldRemove) {
                Log.d(TAG, "清理过期告警: ${alert.id}")
            }
            
            shouldRemove
        }
        
        // 清理历史记录
        synchronized(alertHistory) {
            alertHistory.removeIf { now - it.timestamp > ALERT_RETENTION_PERIOD }
        }
        
        // 限制活跃告警数量
        if (activeAlerts.size > MAX_ACTIVE_ALERTS) {
            val sortedAlerts = activeAlerts.values
                .sortedBy { it.timestamp }
                .toMutableList()
            
            val toRemove = sortedAlerts.drop(MAX_ACTIVE_ALERTS)
            toRemove.forEach { alert ->
                activeAlerts.remove(alert.id)
                Log.d(TAG, "清理多余告警: ${alert.id}")
            }
        }
    }
    
    /**
     * 重置所有告警
     */
    fun resetAllAlerts() {
        activeAlerts.clear()
        alertHistory.clear()
        
        Log.i(TAG, "重置所有告警完成")
    }
    
    /**
     * 获取告警摘要
     */
    fun getAlertSummary(): AlertSummary {
        val activeAlerts = getActiveAlerts()
        val criticalAlerts = activeAlerts.count { it.level == AlertLevel.CRITICAL }
        val warningAlerts = activeAlerts.count { it.level == AlertLevel.WARNING }
        val infoAlerts = activeAlerts.count { it.level == AlertLevel.INFO }
        
        val recentAlerts = getAlertHistory(10)
        val topAlertTypes = recentAlerts
            .groupBy { it.type }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
        
        return AlertSummary(
            totalActiveAlerts = activeAlerts.size,
            criticalAlerts = criticalAlerts,
            warningAlerts = warningAlerts,
            infoAlerts = infoAlerts,
            topAlertTypes = topAlertTypes,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * 生成告警ID
     */
    private fun generateAlertId(): String {
        return "alert_${alertIdGenerator.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    /**
     * 判断是否应该抑制告警
     */
    private fun shouldSuppressAlert(alert: Alert): Boolean {
        // 检查抑制器
        for (suppressor in alertSuppressors.values) {
            if (suppressor.shouldSuppress(alert)) {
                return true
            }
        }
        
        // 检查冷却期
        val similarAlerts = activeAlerts.values.filter { existingAlert ->
            existingAlert.type == alert.type &&
            existingAlert.source == alert.source &&
            existingAlert.status == AlertStatus.ACTIVE &&
            System.currentTimeMillis() - existingAlert.timestamp < ALERT_COOLDOWN_PERIOD
        }
        
        return similarAlerts.isNotEmpty()
    }
    
    /**
     * 通知告警监听器
     */
    private fun notifyAlertListeners(alert: Alert) {
        alertListeners.forEach { listener ->
            try {
                listener.onAlert(alert)
            } catch (e: Exception) {
                Log.e(TAG, "告警监听器异常", e)
            }
        }
    }
    
    /**
     * 记录告警日志
     */
    private fun logAlert(alert: Alert) {
        when (alert.level) {
            AlertLevel.CRITICAL -> Log.e(TAG, "🚨 CRITICAL告警: [${alert.id}] ${alert.title} - ${alert.message}")
            AlertLevel.WARNING -> Log.w(TAG, "⚠️ WARNING告警: [${alert.id}] ${alert.title} - ${alert.message}")
            AlertLevel.INFO -> Log.i(TAG, "ℹ️ INFO告警: [${alert.id}] ${alert.title} - ${alert.message}")
        }
    }
    
    /**
     * 初始化默认告警规则
     */
    private fun initializeDefaultAlertRules() {
        // 成功率告警规则
        addAlertRule(SuccessRateAlertRule())
        
        // 性能告警规则
        addAlertRule(PerformanceAlertRule())
        
        // 错误率告警规则
        addAlertRule(ErrorRateAlertRule())
        
        // 资源使用告警规则
        addAlertRule(ResourceUsageAlertRule())
        
        Log.i(TAG, "初始化默认告警规则完成")
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            // 关闭定期任务
            scheduledExecutor.shutdown()
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow()
            }
            
            // 清理资源
            activeAlerts.clear()
            alertHistory.clear()
            alertRules.clear()
            alertListeners.clear()
            alertSuppressors.clear()
            
            Log.i(TAG, "AlertManager 资源释放完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }
    
    // 数据类定义
    data class Alert(
        val id: String,
        val type: AlertType,
        val level: AlertLevel,
        val title: String,
        val message: String,
        val source: String,
        val timestamp: Long,
        val metadata: Map<String, Any>,
        var status: AlertStatus = AlertStatus.ACTIVE,
        var resolvedAt: Long? = null,
        var resolution: String? = null
    )
    
    data class AlertStatistics(
        val totalAlerts: Int,
        val activeAlerts: Int,
        val resolvedAlerts: Int,
        val suppressedAlerts: Int,
        val typeDistribution: Map<AlertType, Int>,
        val levelDistribution: Map<AlertLevel, Int>,
        val sourceDistribution: Map<String, Int>,
        val averageResolutionTimeMs: Long
    )
    
    data class AlertSummary(
        val totalActiveAlerts: Int,
        val criticalAlerts: Int,
        val warningAlerts: Int,
        val infoAlerts: Int,
        val topAlertTypes: List<Pair<AlertType, Int>>,
        val lastUpdated: Long
    )
    
    data class AlertEvaluation(
        val shouldTrigger: Boolean,
        val metadata: Map<String, Any>
    )
    
    /**
     * 告警类型
     */
    enum class AlertType {
        SUCCESS_RATE_LOW,
        PERFORMANCE_DEGRADED,
        ERROR_RATE_HIGH,
        RESOURCE_USAGE_HIGH,
        MEMORY_LEAK,
        CONNECTION_FAILURE,
        DATA_CORRUPTION,
        SYSTEM_ERROR,
        CUSTOM
    }
    
    /**
     * 告警级别
     */
    enum class AlertLevel {
        INFO,      // 信息性告警
        WARNING,   // 警告性告警
        CRITICAL   // 严重告警
    }
    
    /**
     * 告警状态
     */
    enum class AlertStatus {
        ACTIVE,     // 活跃状态
        RESOLVED,   // 已解决
        SUPPRESSED  // 被抑制
    }
    
    /**
     * 告警监听器接口
     */
    interface AlertListener {
        fun onAlert(alert: Alert)
    }
    
    /**
     * 告警规则接口
     */
    interface AlertRule {
        val id: String
        val alertType: AlertType
        val alertLevel: AlertLevel
        val enabled: Boolean
        
        fun evaluate(): AlertEvaluation
        fun generateTitle(evaluation: AlertEvaluation): String
        fun generateMessage(evaluation: AlertEvaluation): String
    }
    
    /**
     * 告警抑制器接口
     */
    interface AlertSuppressor {
        val id: String
        fun shouldSuppress(alert: Alert): Boolean
    }
    
    /**
     * 成功率告警规则
     */
    private class SuccessRateAlertRule : AlertRule {
        override val id = "success_rate_low"
        override val alertType = AlertType.SUCCESS_RATE_LOW
        override val alertLevel = AlertLevel.WARNING
        override var enabled = true
        
        override fun evaluate(): AlertEvaluation {
            // 这里应该从监控系统获取成功率数据
            // 为了简化，返回不触发
            return AlertEvaluation(false, emptyMap())
        }
        
        override fun generateTitle(evaluation: AlertEvaluation): String {
            return "解析成功率过低"
        }
        
        override fun generateMessage(evaluation: AlertEvaluation): String {
            return "当前解析成功率低于阈值，建议检查解析策略和数据质量"
        }
    }
    
    /**
     * 性能告警规则
     */
    private class PerformanceAlertRule : AlertRule {
        override val id = "performance_degraded"
        override val alertType = AlertType.PERFORMANCE_DEGRADED
        override val alertLevel = AlertLevel.WARNING
        override var enabled = true
        
        override fun evaluate(): AlertEvaluation {
            // 这里应该从监控系统获取性能数据
            // 为了简化，返回不触发
            return AlertEvaluation(false, emptyMap())
        }
        
        override fun generateTitle(evaluation: AlertEvaluation): String {
            return "解析性能下降"
        }
        
        override fun generateMessage(evaluation: AlertEvaluation): String {
            return "解析平均耗时超过阈值，建议优化解析算法或增加缓存"
        }
    }
    
    /**
     * 错误率告警规则
     */
    private class ErrorRateAlertRule : AlertRule {
        override val id = "error_rate_high"
        override val alertType = AlertType.ERROR_RATE_HIGH
        override val alertLevel = AlertLevel.CRITICAL
        override var enabled = true
        
        override fun evaluate(): AlertEvaluation {
            // 这里应该从监控系统获取错误率数据
            // 为了简化，返回不触发
            return AlertEvaluation(false, emptyMap())
        }
        
        override fun generateTitle(evaluation: AlertEvaluation): String {
            return "解析错误率过高"
        }
        
        override fun generateMessage(evaluation: AlertEvaluation): String {
            return "解析错误率超过阈值，建议检查输入数据和错误处理逻辑"
        }
    }
    
    /**
     * 资源使用告警规则
     */
    private class ResourceUsageAlertRule : AlertRule {
        override val id = "resource_usage_high"
        override val alertType = AlertType.RESOURCE_USAGE_HIGH
        override val alertLevel = AlertLevel.WARNING
        override var enabled = true
        
        override fun evaluate(): AlertEvaluation {
            // 这里应该从监控系统获取资源使用数据
            // 为了简化，返回不触发
            return AlertEvaluation(false, emptyMap())
        }
        
        override fun generateTitle(evaluation: AlertEvaluation): String {
            return "资源使用过高"
        }
        
        override fun generateMessage(evaluation: AlertEvaluation): String {
            return "内存或CPU使用率超过阈值，建议优化资源使用或增加资源限制"
        }
    }
}