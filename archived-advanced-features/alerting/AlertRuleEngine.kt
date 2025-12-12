package com.empathy.ai.data.alerting

import android.util.Log
import com.empathy.ai.data.monitoring.AiResponseParserMetrics
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

/**
 * 告警规则引擎
 * 
 * 负责管理和执行告警规则
 * 支持动态规则配置和复杂条件判断
 */
class AlertRuleEngine private constructor() {
    companion object {
        private const val TAG = "AlertRuleEngine"
        
        // 默认评估间隔（毫秒）
        private const val DEFAULT_EVALUATION_INTERVAL = 30_000L // 30秒
        
        // 规则冷却时间（毫秒）
        private const val RULE_COOLDOWN_TIME = 5 * 60_000L // 5分钟
        
        @Volatile
        private var INSTANCE: AlertRuleEngine? = null
        
        fun getInstance(): AlertRuleEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlertRuleEngine().also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var evaluationJob: Job? = null
    
    // 规则存储
    private val alertRules = ConcurrentHashMap<String, AlertRule>()
    private val activeRules = CopyOnWriteArrayList<String>()
    
    // 规则执行状态
    private val ruleExecutionHistory = ConcurrentHashMap<String, MutableList<RuleExecutionRecord>>()
    private val ruleCooldowns = ConcurrentHashMap<String, Long>()
    private val ruleCounters = ConcurrentHashMap<String, AtomicLong>()
    
    // 监听器
    private val ruleListeners = CopyOnWriteArrayList<AlertRuleListener>()
    
    // 配置
    private var evaluationInterval = DEFAULT_EVALUATION_INTERVAL
    private var isRunning = false
    
    init {
        // 初始化默认规则
        initializeDefaultRules()
        
        Log.i(TAG, "AlertRuleEngine 初始化完成")
    }
    
    /**
     * 启动规则引擎
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "规则引擎已在运行")
            return
        }
        
        isRunning = true
        evaluationJob = scope.launch {
            while (isActive) {
                try {
                    evaluateActiveRules()
                    delay(evaluationInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "规则评估异常", e)
                    delay(5000) // 错误后等待5秒再重试
                }
            }
        }
        
        Log.i(TAG, "规则引擎已启动，评估间隔: ${evaluationInterval}ms")
    }
    
    /**
     * 停止规则引擎
     */
    fun stop() {
        if (!isRunning) {
            Log.w(TAG, "规则引擎未运行")
            return
        }
        
        isRunning = false
        evaluationJob?.cancel()
        
        Log.i(TAG, "规则引擎已停止")
    }
    
    /**
     * 添加告警规则
     */
    fun addRule(rule: AlertRule) {
        alertRules[rule.id] = rule
        
        // 如果规则启用，添加到活动规则列表
        if (rule.enabled) {
            activeRules.add(rule.id)
        }
        
        // 初始化规则计数器
        ruleCounters[rule.id] = AtomicLong(0)
        
        Log.i(TAG, "添加告警规则: ${rule.name} (${rule.id})")
        
        // 通知监听器
        notifyRuleAdded(rule)
    }
    
    /**
     * 更新告警规则
     */
    fun updateRule(ruleId: String, rule: AlertRule) {
        val oldRule = alertRules[ruleId]
        if (oldRule == null) {
            Log.w(TAG, "规则不存在，无法更新: $ruleId")
            return
        }
        
        alertRules[ruleId] = rule
        
        // 更新活动规则列表
        if (rule.enabled) {
            if (!activeRules.contains(ruleId)) {
                activeRules.add(ruleId)
            }
        } else {
            activeRules.remove(ruleId)
        }
        
        Log.i(TAG, "更新告警规则: ${rule.name} ($ruleId)")
        
        // 通知监听器
        notifyRuleUpdated(oldRule, rule)
    }
    
    /**
     * 删除告警规则
     */
    fun removeRule(ruleId: String) {
        val rule = alertRules.remove(ruleId)
        if (rule == null) {
            Log.w(TAG, "规则不存在，无法删除: $ruleId")
            return
        }
        
        // 从活动规则列表中移除
        activeRules.remove(ruleId)
        
        // 清理相关数据
        ruleExecutionHistory.remove(ruleId)
        ruleCooldowns.remove(ruleId)
        ruleCounters.remove(ruleId)
        
        Log.i(TAG, "删除告警规则: ${rule.name} ($ruleId)")
        
        // 通知监听器
        notifyRuleRemoved(rule)
    }
    
    /**
     * 启用/禁用规则
     */
    fun enableRule(ruleId: String, enabled: Boolean) {
        val rule = alertRules[ruleId]
        if (rule == null) {
            Log.w(TAG, "规则不存在: $ruleId")
            return
        }
        
        val updatedRule = rule.copy(enabled = enabled)
        updateRule(ruleId, updatedRule)
        
        Log.i(TAG, "${if (enabled) "启用" else "禁用"}规则: ${rule.name} ($ruleId)")
    }
    
    /**
     * 手动评估规则
     */
    fun evaluateRule(ruleId: String, metrics: AiResponseParserMetrics): RuleEvaluationResult? {
        val rule = alertRules[ruleId]
        if (rule == null) {
            Log.w(TAG, "规则不存在: $ruleId")
            return null
        }
        
        if (!rule.enabled) {
            Log.d(TAG, "规则已禁用: $ruleId")
            return null
        }
        
        return evaluateRuleInternal(rule, metrics)
    }
    
    /**
     * 获取所有规则
     */
    fun getAllRules(): List<AlertRule> {
        return alertRules.values.toList()
    }
    
    /**
     * 获取活动规则
     */
    fun getActiveRules(): List<AlertRule> {
        return activeRules.mapNotNull { alertRules[it] }
    }
    
    /**
     * 获取规则执行历史
     */
    fun getRuleExecutionHistory(ruleId: String, limit: Int = 50): List<RuleExecutionRecord> {
        return ruleExecutionHistory[ruleId]?.takeLast(limit) ?: emptyList()
    }
    
    /**
     * 获取规则统计
     */
    fun getRuleStatistics(): RuleEngineStatistics {
        val totalRules = alertRules.size
        val enabledRules = alertRules.values.count { it.enabled }
        val activeRulesCount = activeRules.size
        
        val ruleExecutionCounts = ruleCounters.mapValues { it.value.get() }
        val totalEvaluations = ruleExecutionCounts.values.sum()
        
        val recentExecutions = ruleExecutionHistory.values.flatten()
            .filter { it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000L }
        
        val recent24HEvaluations = recentExecutions.size
        val recent24HAlerts = recentExecutions.count { it.triggered }
        
        return RuleEngineStatistics(
            totalRules = totalRules,
            enabledRules = enabledRules,
            activeRules = activeRulesCount,
            totalEvaluations = totalEvaluations,
            recent24HEvaluations = recent24HEvaluations,
            recent24HAlerts = recent24HAlerts,
            ruleExecutionCounts = ruleExecutionCounts,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * 设置评估间隔
     */
    fun setEvaluationInterval(interval: Long) {
        evaluationInterval = max(5000, interval) // 最小5秒
        Log.i(TAG, "设置评估间隔: ${evaluationInterval}ms")
    }
    
    /**
     * 添加规则监听器
     */
    fun addRuleListener(listener: AlertRuleListener) {
        ruleListeners.add(listener)
    }
    
    /**
     * 移除规则监听器
     */
    fun removeRuleListener(listener: AlertRuleListener) {
        ruleListeners.remove(listener)
    }
    
    /**
     * 评估所有活动规则
     */
    private suspend fun evaluateActiveRules() {
        val metrics = AiResponseParserMetrics.getInstance()
        
        for (ruleId in activeRules) {
            val rule = alertRules[ruleId] ?: continue
            
            try {
                val result = evaluateRuleInternal(rule, metrics)
                recordRuleExecution(rule, result)
                
                if (result.triggered) {
                    handleRuleTriggered(rule, result)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "评估规则失败: ${rule.name} ($ruleId)", e)
                recordRuleExecution(rule, RuleEvaluationResult(
                    ruleId = ruleId,
                    triggered = false,
                    errorMessage = e.message
                ))
            }
        }
    }
    
    /**
     * 内部规则评估方法
     */
    private fun evaluateRuleInternal(rule: AlertRule, metrics: AiResponseParserMetrics): RuleEvaluationResult {
        val ruleId = rule.id
        
        // 检查冷却时间
        val currentTime = System.currentTimeMillis()
        val cooldownEnd = ruleCooldowns[ruleId] ?: 0L
        if (currentTime < cooldownEnd) {
            return RuleEvaluationResult(
                ruleId = ruleId,
                triggered = false,
                reason = "规则在冷却期内"
            )
        }
        
        // 评估条件
        val conditionResults = mutableListOf<ConditionResult>()
        var allConditionsMet = true
        
        for (condition in rule.conditions) {
            val result = evaluateCondition(condition, metrics)
            conditionResults.add(result)
            
            if (!result.met) {
                allConditionsMet = false
                
                // 如果是AND逻辑，可以提前退出
                if (rule.conditionLogic == ConditionLogic.AND) {
                    break
                }
            } else if (rule.conditionLogic == ConditionLogic.OR) {
                // 如果是OR逻辑，可以提前退出
                allConditionsMet = true
                break
            }
        }
        
        val triggered = allConditionsMet && conditionResults.isNotEmpty()
        
        return RuleEvaluationResult(
            ruleId = ruleId,
            triggered = triggered,
            conditionResults = conditionResults,
            reason = if (triggered) "所有条件满足" else "条件不满足",
            evaluatedAt = currentTime
        )
    }
    
    /**
     * 评估单个条件
     */
    private fun evaluateCondition(condition: AlertCondition, metrics: AiResponseParserMetrics): ConditionResult {
        val metricValue = getMetricValue(condition.metricName, metrics)
        val actualValue = metricValue ?: 0.0
        
        val met = when (condition.operator) {
            ConditionOperator.GREATER_THAN -> actualValue > condition.threshold
            ConditionOperator.GREATER_THAN_OR_EQUAL -> actualValue >= condition.threshold
            ConditionOperator.LESS_THAN -> actualValue < condition.threshold
            ConditionOperator.LESS_THAN_OR_EQUAL -> actualValue <= condition.threshold
            ConditionOperator.EQUAL -> actualValue == condition.threshold
            ConditionOperator.NOT_EQUAL -> actualValue != condition.threshold
        }
        
        return ConditionResult(
            condition = condition,
            actualValue = actualValue,
            threshold = condition.threshold,
            met = met
        )
    }
    
    /**
     * 获取指标值
     */
    private fun getMetricValue(metricName: String, metrics: AiResponseParserMetrics): Double? {
        return when (metricName) {
            "success_rate" -> metrics.getOverallMetrics().successRate.toDouble()
            "total_requests" -> metrics.getOverallMetrics().totalRequests.toDouble()
            "error_rate" -> metrics.getOverallMetrics().errorRate.toDouble()
            "average_duration" -> metrics.getOverallMetrics().averageDurationMs.toDouble()
            "p95_duration" -> metrics.getOverallMetrics().p95DurationMs.toDouble()
            "p99_duration" -> metrics.getOverallMetrics().p99DurationMs.toDouble()
            "cache_hit_rate" -> metrics.getOverallMetrics().cacheHitRate.toDouble()
            "memory_usage" -> metrics.getOverallMetrics().memoryUsageMB.toDouble()
            "cpu_usage" -> metrics.getOverallMetrics().cpuUsagePercent.toDouble()
            else -> null
        }
    }
    
    /**
     * 处理规则触发
     */
    private fun handleRuleTriggered(rule: AlertRule, result: RuleEvaluationResult) {
        val ruleId = rule.id
        
        // 更新计数器
        ruleCounters[ruleId]?.incrementAndGet()
        
        // 设置冷却时间
        ruleCooldowns[ruleId] = System.currentTimeMillis() + rule.cooldownTime
        
        // 创建告警
        val alert = Alert(
            id = "alert_${ruleId}_${System.currentTimeMillis()}",
            ruleId = ruleId,
            ruleName = rule.name,
            type = rule.alertType,
            level = rule.alertLevel,
            title = rule.alertTitle ?: "告警: ${rule.name}",
            message = rule.alertMessage ?: "规则 ${rule.name} 被触发",
            metadata = mapOf(
                "ruleId" to ruleId,
                "ruleName" to rule.name,
                "evaluationResult" to result.toString(),
                "timestamp" to System.currentTimeMillis()
            ),
            triggeredAt = result.evaluatedAt
        )
        
        // 通知监听器
        notifyAlertTriggered(alert)
        
        Log.i(TAG, "规则触发: ${rule.name} ($ruleId)")
    }
    
    /**
     * 记录规则执行
     */
    private fun recordRuleExecution(rule: AlertRule, result: RuleEvaluationResult) {
        val ruleId = rule.id
        val record = RuleExecutionRecord(
            ruleId = ruleId,
            ruleName = rule.name,
            triggered = result.triggered,
            reason = result.reason,
            errorMessage = result.errorMessage,
            timestamp = result.evaluatedAt
        )
        
        val history = ruleExecutionHistory.getOrPut(ruleId) { mutableListOf() }
        synchronized(history) {
            history.add(record)
            
            // 保持历史大小
            while (history.size > 100) {
                history.removeAt(0)
            }
        }
    }
    
    /**
     * 初始化默认规则
     */
    private fun initializeDefaultRules() {
        // 成功率低告警
        addRule(AlertRule(
            id = "success_rate_low",
            name = "成功率低告警",
            description = "当解析成功率低于阈值时触发告警",
            enabled = true,
            conditions = listOf(
                AlertCondition(
                    metricName = "success_rate",
                    operator = ConditionOperator.LESS_THAN,
                    threshold = 90.0
                )
            ),
            conditionLogic = ConditionLogic.AND,
            alertType = AlertType.SUCCESS_RATE_LOW,
            alertLevel = AlertLevel.WARNING,
            alertTitle = "AI解析成功率低",
            alertMessage = "解析成功率低于90%，请检查系统状态",
            cooldownTime = RULE_COOLDOWN_TIME
        ))
        
        // 性能下降告警
        addRule(AlertRule(
            id = "performance_degraded",
            name = "性能下降告警",
            description = "当平均响应时间超过阈值时触发告警",
            enabled = true,
            conditions = listOf(
                AlertCondition(
                    metricName = "average_duration",
                    operator = ConditionOperator.GREATER_THAN,
                    threshold = 5000.0
                )
            ),
            conditionLogic = ConditionLogic.AND,
            alertType = AlertType.PERFORMANCE_DEGRADED,
            alertLevel = AlertLevel.WARNING,
            alertTitle = "AI解析性能下降",
            alertMessage = "平均响应时间超过5秒，请检查系统负载",
            cooldownTime = RULE_COOLDOWN_TIME
        ))
        
        // 错误率高告警
        addRule(AlertRule(
            id = "error_rate_high",
            name = "错误率高告警",
            description = "当错误率超过阈值时触发告警",
            enabled = true,
            conditions = listOf(
                AlertCondition(
                    metricName = "error_rate",
                    operator = ConditionOperator.GREATER_THAN,
                    threshold = 10.0
                )
            ),
            conditionLogic = ConditionLogic.AND,
            alertType = AlertType.ERROR_RATE_HIGH,
            alertLevel = AlertLevel.WARNING,
            alertTitle = "AI解析错误率高",
            alertMessage = "错误率超过10%，请检查系统状态",
            cooldownTime = RULE_COOLDOWN_TIME
        ))
        
        Log.i(TAG, "初始化默认告警规则完成")
    }
    
    /**
     * 通知规则添加
     */
    private fun notifyRuleAdded(rule: AlertRule) {
        ruleListeners.forEach { it.onRuleAdded(rule) }
    }
    
    /**
     * 通知规则更新
     */
    private fun notifyRuleUpdated(oldRule: AlertRule, newRule: AlertRule) {
        ruleListeners.forEach { it.onRuleUpdated(oldRule, newRule) }
    }
    
    /**
     * 通知规则删除
     */
    private fun notifyRuleRemoved(rule: AlertRule) {
        ruleListeners.forEach { it.onRuleRemoved(rule) }
    }
    
    /**
     * 通知告警触发
     */
    private fun notifyAlertTriggered(alert: Alert) {
        ruleListeners.forEach { it.onAlertTriggered(alert) }
    }
    
    // 数据类定义
    data class AlertRule(
        val id: String,
        val name: String,
        val description: String,
        val enabled: Boolean,
        val conditions: List<AlertCondition>,
        val conditionLogic: ConditionLogic = ConditionLogic.AND,
        val alertType: AlertType,
        val alertLevel: AlertLevel,
        val alertTitle: String? = null,
        val alertMessage: String? = null,
        val cooldownTime: Long = RULE_COOLDOWN_TIME,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class AlertCondition(
        val metricName: String,
        val operator: ConditionOperator,
        val threshold: Double,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class RuleEvaluationResult(
        val ruleId: String,
        val triggered: Boolean,
        val conditionResults: List<ConditionResult> = emptyList(),
        val reason: String,
        val errorMessage: String? = null,
        val evaluatedAt: Long = System.currentTimeMillis()
    )
    
    data class ConditionResult(
        val condition: AlertCondition,
        val actualValue: Double,
        val threshold: Double,
        val met: Boolean
    )
    
    data class RuleExecutionRecord(
        val ruleId: String,
        val ruleName: String,
        val triggered: Boolean,
        val reason: String,
        val errorMessage: String? = null,
        val timestamp: Long
    )
    
    data class RuleEngineStatistics(
        val totalRules: Int,
        val enabledRules: Int,
        val activeRules: Int,
        val totalEvaluations: Long,
        val recent24HEvaluations: Int,
        val recent24HAlerts: Int,
        val ruleExecutionCounts: Map<String, Long>,
        val lastUpdated: Long
    )
    
    data class Alert(
        val id: String,
        val ruleId: String,
        val ruleName: String,
        val type: AlertType,
        val level: AlertLevel,
        val title: String,
        val message: String,
        val metadata: Map<String, Any>,
        val triggeredAt: Long,
        val resolvedAt: Long? = null,
        val resolvedBy: String? = null
    )
    
    enum class ConditionLogic {
        AND,  // 所有条件都必须满足
        OR    // 任一条件满足即可
    }
    
    enum class ConditionOperator {
        GREATER_THAN,              // 大于
        GREATER_THAN_OR_EQUAL,     // 大于等于
        LESS_THAN,                 // 小于
        LESS_THAN_OR_EQUAL,        // 小于等于
        EQUAL,                     // 等于
        NOT_EQUAL                  // 不等于
    }
    
    /**
     * 规则监听器接口
     */
    interface AlertRuleListener {
        fun onRuleAdded(rule: AlertRule) {}
        fun onRuleUpdated(oldRule: AlertRule, newRule: AlertRule) {}
        fun onRuleRemoved(rule: AlertRule) {}
        fun onAlertTriggered(alert: Alert) {}
    }
}