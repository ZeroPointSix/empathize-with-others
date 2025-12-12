package com.empathy.ai.data.observability

import android.content.Context
import android.util.Log
import com.empathy.ai.data.alerting.AlertManager
import com.empathy.ai.data.alerting.AlertRuleEngine
import com.empathy.ai.data.alerting.NotificationService
import com.empathy.ai.data.alerting.ThresholdMonitor
import com.empathy.ai.data.learning.FieldMappingLearningEngine
import com.empathy.ai.data.learning.LearningDataStore
import com.empathy.ai.data.learning.ParsingPatternAnalyzer
import com.empathy.ai.data.monitoring.AiResponseParserMetrics
import com.empathy.ai.data.monitoring.HealthCheckSystem
import com.empathy.ai.data.monitoring.MetricsRepository
import com.empathy.ai.data.monitoring.ParsingPerformanceTracker
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 可观测性管理器
 * 
 * 统一管理和协调所有可观测性组件
 * 提供统一的接口和配置管理
 */
class ObservabilityManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ObservabilityManager"
        
        @Volatile
        private var INSTANCE: ObservabilityManager? = null
        
        fun getInstance(context: Context): ObservabilityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ObservabilityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isInitialized = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)
    
    // 组件引用
    private lateinit var metrics: AiResponseParserMetrics
    private lateinit var performanceTracker: ParsingPerformanceTracker
    private lateinit var healthCheckSystem: HealthCheckSystem
    private lateinit var metricsRepository: MetricsRepository
    
    private lateinit var fieldMappingLearningEngine: FieldMappingLearningEngine
    private lateinit var parsingPatternAnalyzer: ParsingPatternAnalyzer
    private lateinit var learningDataStore: LearningDataStore
    
    private lateinit var alertManager: AlertManager
    private lateinit var thresholdMonitor: ThresholdMonitor
    private lateinit var notificationService: NotificationService
    private lateinit var alertRuleEngine: AlertRuleEngine
    
    private lateinit var diagnosticCollector: DiagnosticCollector
    private lateinit var parsingTracer: ParsingTracer
    private lateinit var detailedLogger: DetailedLogger
    
    // 配置
    private val configuration = ObservabilityConfiguration()
    
    // 监听器
    private val observabilityListeners = CopyOnWriteArrayList<ObservabilityListener>()
    
    init {
        Log.i(TAG, "ObservabilityManager 初始化完成")
    }
    
    /**
     * 初始化所有组件
     */
    fun initialize() {
        if (isInitialized.get()) {
            Log.w(TAG, "ObservabilityManager 已初始化")
            return
        }
        
        try {
            // 初始化性能监控组件
            initializeMonitoringComponents()
            
            // 初始化学习机制组件
            initializeLearningComponents()
            
            // 初始化告警系统组件
            initializeAlertingComponents()
            
            // 初始化可观测性组件
            initializeObservabilityComponents()
            
            // 设置组件间的关联
            setupComponentInteractions()
            
            isInitialized.set(true)
            
            Log.i(TAG, "所有可观测性组件初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            throw e
        }
    }
    
    /**
     * 启动所有组件
     */
    fun start() {
        if (!isInitialized.get()) {
            Log.e(TAG, "组件未初始化，无法启动")
            return
        }
        
        if (isStarted.get()) {
            Log.w(TAG, "ObservabilityManager 已启动")
            return
        }
        
        try {
            // 启动性能监控组件
            if (configuration.enableMonitoring) {
                performanceTracker.startTracking()
                healthCheckSystem.startHealthChecks()
            }
            
            // 启动学习机制组件
            if (configuration.enableLearning) {
                fieldMappingLearningEngine.startLearning()
                parsingPatternAnalyzer.startAnalysis()
            }
            
            // 启动告警系统组件
            if (configuration.enableAlerting) {
                alertManager.start()
                thresholdMonitor.startMonitoring()
                alertRuleEngine.start()
            }
            
            // 启动可观测性组件
            if (configuration.enableObservability) {
                // 这些组件通常不需要显式启动，但可以在这里执行一些初始化操作
                diagnosticCollector.createSystemSnapshot(
                    name = "系统启动",
                    description = "系统启动时的状态快照"
                )
            }
            
            isStarted.set(true)
            
            Log.i(TAG, "所有可观测性组件已启动")
            
            // 通知监听器
            notifyObservabilityStarted()
            
        } catch (e: Exception) {
            Log.e(TAG, "启动失败", e)
            throw e
        }
    }
    
    /**
     * 停止所有组件
     */
    fun stop() {
        if (!isStarted.get()) {
            Log.w(TAG, "ObservabilityManager 未启动")
            return
        }
        
        try {
            // 停止告警系统组件
            if (configuration.enableAlerting) {
                alertRuleEngine.stop()
                thresholdMonitor.stopMonitoring()
                alertManager.stop()
            }
            
            // 停止学习机制组件
            if (configuration.enableLearning) {
                parsingPatternAnalyzer.stopAnalysis()
                fieldMappingLearningEngine.stopLearning()
            }
            
            // 停止性能监控组件
            if (configuration.enableMonitoring) {
                healthCheckSystem.stopHealthChecks()
                performanceTracker.stopTracking()
            }
            
            // 创建系统关闭快照
            if (configuration.enableObservability) {
                diagnosticCollector.createSystemSnapshot(
                    name = "系统关闭",
                    description = "系统关闭时的状态快照"
                )
            }
            
            isStarted.set(false)
            
            Log.i(TAG, "所有可观测性组件已停止")
            
            // 通知监听器
            notifyObservabilityStopped()
            
        } catch (e: Exception) {
            Log.e(TAG, "停止失败", e)
            throw e
        }
    }
    
    /**
     * 获取综合状态报告
     */
    fun getComprehensiveStatusReport(): ComprehensiveStatusReport {
        if (!isInitialized.get()) {
            throw IllegalStateException("组件未初始化")
        }
        
        return scope.async {
            val monitoringStatus = if (configuration.enableMonitoring) {
                MonitoringStatus(
                    metrics = metrics.getOverallMetrics(),
                    healthReport = healthCheckSystem.getHealthReport(),
                    performanceStatistics = performanceTracker.getPerformanceStatistics()
                )
            } else {
                null
            }
            
            val learningStatus = if (configuration.enableLearning) {
                LearningStatus(
                    learningStatistics = fieldMappingLearningEngine.getLearningStatistics(),
                    patternAnalysisStatistics = parsingPatternAnalyzer.getAnalysisStatistics(),
                    learningDataStatistics = learningDataStore.getLearningDataStatistics()
                )
            } else {
                null
            }
            
            val alertingStatus = if (configuration.enableAlerting) {
                AlertingStatus(
                    alertStatistics = alertManager.getAlertStatistics(),
                    thresholdStatistics = thresholdMonitor.getThresholdStatistics(),
                    ruleEngineStatistics = alertRuleEngine.getRuleStatistics()
                )
            } else {
                null
            }
            
            val observabilityStatus = if (configuration.enableObservability) {
                ObservabilityStatus(
                    diagnosticStatistics = diagnosticCollector.getDiagnosticStatistics(),
                    traceStatistics = parsingTracer.getTraceStatistics(),
                    loggingStatistics = detailedLogger.getLoggingStatistics()
                )
            } else {
                null
            }
            
            ComprehensiveStatusReport(
                timestamp = System.currentTimeMillis(),
                isStarted = isStarted.get(),
                configuration = configuration,
                monitoringStatus = monitoringStatus,
                learningStatus = learningStatus,
                alertingStatus = alertingStatus,
                observabilityStatus = observabilityStatus
            )
        }.runBlocking()
    }
    
    /**
     * 导出所有数据
     */
    suspend fun exportAllData(
        outputPath: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) {
            throw IllegalStateException("组件未初始化")
        }
        
        val exportData = mutableMapOf<String, Any>()
        
        // 导出性能监控数据
        if (configuration.enableMonitoring) {
            exportData["metrics"] = metricsRepository.exportMetrics()
            exportData["healthReports"] = healthCheckSystem.getHealthHistory()
        }
        
        // 导出学习数据
        if (configuration.enableLearning) {
            exportData["learningData"] = learningDataStore.exportLearningData()
            exportData["fieldMappings"] = fieldMappingLearningEngine.exportFieldMappings()
            exportData["patterns"] = parsingPatternAnalyzer.exportPatterns()
        }
        
        // 导出告警数据
        if (configuration.enableAlerting) {
            exportData["alerts"] = alertManager.exportAlerts()
            exportData["rules"] = alertRuleEngine.getAllRules().map { it.toMap() }
        }
        
        // 导出可观测性数据
        if (configuration.enableObservability) {
            exportData["diagnostics"] = diagnosticCollector.exportDiagnostics()
            exportData["traces"] = parsingTracer.exportTraceData()
            exportData["logs"] = detailedLogger.exportLogs()
        }
        
        // 添加元数据
        exportData["metadata"] = mapOf(
            "exportedAt" to System.currentTimeMillis(),
            "configuration" to configuration.toMap(),
            "version" to "1.0"
        )
        
        JSONObject(exportData).toString(2)
    }
    
    /**
     * 更新配置
     */
    fun updateConfiguration(newConfiguration: ObservabilityConfiguration) {
        val oldConfiguration = configuration.copy()
        
        // 应用新配置
        configuration.apply {
            enableMonitoring = newConfiguration.enableMonitoring
            enableLearning = newConfiguration.enableLearning
            enableAlerting = newConfiguration.enableAlerting
            enableObservability = newConfiguration.enableObservability
            
            monitoringConfiguration = newConfiguration.monitoringConfiguration
            learningConfiguration = newConfiguration.learningConfiguration
            alertingConfiguration = newConfiguration.alertingConfiguration
            observabilityConfiguration = newConfiguration.observabilityConfiguration
        }
        
        // 如果系统已启动，需要重启组件以应用新配置
        if (isStarted.get()) {
            restartComponentsWithNewConfiguration(oldConfiguration, configuration)
        }
        
        Log.i(TAG, "配置已更新")
        
        // 通知监听器
        notifyConfigurationUpdated(oldConfiguration, configuration)
    }
    
    /**
     * 获取配置
     */
    fun getConfiguration(): ObservabilityConfiguration {
        return configuration.copy()
    }
    
    /**
     * 添加可观测性监听器
     */
    fun addObservabilityListener(listener: ObservabilityListener) {
        observabilityListeners.add(listener)
    }
    
    /**
     * 移除可观测性监听器
     */
    fun removeObservabilityListener(listener: ObservabilityListener) {
        observabilityListeners.remove(listener)
    }
    
    /**
     * 获取组件引用
     */
    fun getMetrics(): AiResponseParserMetrics = metrics
    fun getPerformanceTracker(): ParsingPerformanceTracker = performanceTracker
    fun getHealthCheckSystem(): HealthCheckSystem = healthCheckSystem
    fun getFieldMappingLearningEngine(): FieldMappingLearningEngine = fieldMappingLearningEngine
    fun getParsingPatternAnalyzer(): ParsingPatternAnalyzer = parsingPatternAnalyzer
    fun getAlertManager(): AlertManager = alertManager
    fun getDiagnosticCollector(): DiagnosticCollector = diagnosticCollector
    fun getParsingTracer(): ParsingTracer = parsingTracer
    fun getDetailedLogger(): DetailedLogger = detailedLogger
    
    /**
     * 初始化性能监控组件
     */
    private fun initializeMonitoringComponents() {
        metrics = AiResponseParserMetrics.getInstance()
        performanceTracker = ParsingPerformanceTracker.getInstance()
        healthCheckSystem = HealthCheckSystem.getInstance()
        metricsRepository = MetricsRepository.getInstance(context)
        
        Log.d(TAG, "性能监控组件初始化完成")
    }
    
    /**
     * 初始化学习机制组件
     */
    private fun initializeLearningComponents() {
        fieldMappingLearningEngine = FieldMappingLearningEngine.getInstance()
        parsingPatternAnalyzer = ParsingPatternAnalyzer.getInstance()
        learningDataStore = LearningDataStore.getInstance(context)
        
        Log.d(TAG, "学习机制组件初始化完成")
    }
    
    /**
     * 初始化告警系统组件
     */
    private fun initializeAlertingComponents() {
        alertManager = AlertManager.getInstance(context)
        thresholdMonitor = ThresholdMonitor.getInstance()
        notificationService = NotificationService.getInstance(context)
        alertRuleEngine = AlertRuleEngine.getInstance()
        
        Log.d(TAG, "告警系统组件初始化完成")
    }
    
    /**
     * 初始化可观测性组件
     */
    private fun initializeObservabilityComponents() {
        diagnosticCollector = DiagnosticCollector.getInstance(context)
        parsingTracer = ParsingTracer.getInstance()
        detailedLogger = DetailedLogger.getInstance(context)
        
        Log.d(TAG, "可观测性组件初始化完成")
    }
    
    /**
     * 设置组件间的关联
     */
    private fun setupComponentInteractions() {
        // 设置告警规则引擎监听器
        alertRuleEngine.addRuleListener(object : AlertRuleEngine.AlertRuleListener {
            override fun onAlertTriggered(alert: AlertRuleEngine.Alert) {
                // 当规则触发时，发送通知
                notificationService.sendAlertNotification(
                    type = alert.type,
                    level = alert.level,
                    title = alert.title,
                    message = alert.message,
                    metadata = alert.metadata
                )
                
                // 记录诊断信息
                diagnosticCollector.collectSystemDiagnostics(
                    level = DiagnosticCollector.DiagnosticLevel.WARNING,
                    category = DiagnosticCollector.DiagnosticCategory.SYSTEM,
                    title = "告警触发",
                    message = "规则 ${alert.ruleName} 触发了告警",
                    data = mapOf(
                        "alertId" to alert.id,
                        "ruleId" to alert.ruleId,
                        "alertType" to alert.type.name,
                        "alertLevel" to alert.level.name
                    ),
                    tags = setOf("alert", "rule-triggered")
                )
            }
        })
        
        // 设置性能跟踪器监听器
        performanceTracker.addPerformanceListener(object : ParsingPerformanceTracker.PerformanceListener {
            override fun onPerformanceThresholdExceeded(
                operationType: String,
                metric: String,
                value: Double,
                threshold: Double
            ) {
                // 当性能阈值超过时，记录诊断信息
                diagnosticCollector.collectPerformanceDiagnostics(
                    operation = operationType,
                    metrics = mapOf(metric to value),
                    thresholds = mapOf(metric to threshold)
                )
            }
        })
        
        Log.d(TAG, "组件间关联设置完成")
    }
    
    /**
     * 使用新配置重启组件
     */
    private fun restartComponentsWithNewConfiguration(
        oldConfiguration: ObservabilityConfiguration,
        newConfiguration: ObservabilityConfiguration
    ) {
        // 比较配置变化，只重启受影响的组件
        
        // 监控配置变化
        if (oldConfiguration.enableMonitoring != newConfiguration.enableMonitoring ||
            oldConfiguration.monitoringConfiguration != newConfiguration.monitoringConfiguration) {
            
            if (newConfiguration.enableMonitoring) {
                performanceTracker.startTracking()
                healthCheckSystem.startHealthChecks()
            } else {
                performanceTracker.stopTracking()
                healthCheckSystem.stopHealthChecks()
            }
        }
        
        // 学习配置变化
        if (oldConfiguration.enableLearning != newConfiguration.enableLearning ||
            oldConfiguration.learningConfiguration != newConfiguration.learningConfiguration) {
            
            if (newConfiguration.enableLearning) {
                fieldMappingLearningEngine.startLearning()
                parsingPatternAnalyzer.startAnalysis()
            } else {
                fieldMappingLearningEngine.stopLearning()
                parsingPatternAnalyzer.stopAnalysis()
            }
        }
        
        // 告警配置变化
        if (oldConfiguration.enableAlerting != newConfiguration.enableAlerting ||
            oldConfiguration.alertingConfiguration != newConfiguration.alertingConfiguration) {
            
            if (newConfiguration.enableAlerting) {
                alertManager.start()
                thresholdMonitor.startMonitoring()
                alertRuleEngine.start()
            } else {
                alertRuleEngine.stop()
                thresholdMonitor.stopMonitoring()
                alertManager.stop()
            }
        }
        
        Log.d(TAG, "组件配置更新完成")
    }
    
    /**
     * 通知可观测性系统启动
     */
    private fun notifyObservabilityStarted() {
        observabilityListeners.forEach { it.onObservabilityStarted() }
    }
    
    /**
     * 通知可观测性系统停止
     */
    private fun notifyObservabilityStopped() {
        observabilityListeners.forEach { it.onObservabilityStopped() }
    }
    
    /**
     * 通知配置更新
     */
    private fun notifyConfigurationUpdated(
        oldConfiguration: ObservabilityConfiguration,
        newConfiguration: ObservabilityConfiguration
    ) {
        observabilityListeners.forEach { it.onConfigurationUpdated(oldConfiguration, newConfiguration) }
    }
    
    // 数据类定义
    data class ObservabilityConfiguration(
        val enableMonitoring: Boolean = true,
        val enableLearning: Boolean = true,
        val enableAlerting: Boolean = true,
        val enableObservability: Boolean = true,
        val monitoringConfiguration: MonitoringConfiguration = MonitoringConfiguration(),
        val learningConfiguration: LearningConfiguration = LearningConfiguration(),
        val alertingConfiguration: AlertingConfiguration = AlertingConfiguration(),
        val observabilityConfiguration: ObservabilityComponentConfiguration = ObservabilityComponentConfiguration()
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "enableMonitoring" to enableMonitoring,
                "enableLearning" to enableLearning,
                "enableAlerting" to enableAlerting,
                "enableObservability" to enableObservability,
                "monitoringConfiguration" to monitoringConfiguration.toMap(),
                "learningConfiguration" to learningConfiguration.toMap(),
                "alertingConfiguration" to alertingConfiguration.toMap(),
                "observabilityConfiguration" to observabilityConfiguration.toMap()
            )
        }
    }
    
    data class MonitoringConfiguration(
        val trackingInterval: Long = 30_000L, // 30秒
        val healthCheckInterval: Long = 60_000L, // 1分钟
        val metricsRetentionTime: Long = 7 * 24 * 60 * 60 * 1000L // 7天
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "trackingInterval" to trackingInterval,
                "healthCheckInterval" to healthCheckInterval,
                "metricsRetentionTime" to metricsRetentionTime
            )
        }
    }
    
    data class LearningConfiguration(
        val learningInterval: Long = 60_000L, // 1分钟
        val analysisInterval: Long = 5 * 60_000L, // 5分钟
        val dataRetentionTime: Long = 30 * 24 * 60 * 60 * 1000L // 30天
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "learningInterval" to learningInterval,
                "analysisInterval" to analysisInterval,
                "dataRetentionTime" to dataRetentionTime
            )
        }
    }
    
    data class AlertingConfiguration(
        val evaluationInterval: Long = 30_000L, // 30秒
        val notificationCooldown: Long = 5 * 60_000L, // 5分钟
        val alertRetentionTime: Long = 7 * 24 * 60 * 60 * 1000L // 7天
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "evaluationInterval" to evaluationInterval,
                "notificationCooldown" to notificationCooldown,
                "alertRetentionTime" to alertRetentionTime
            )
        }
    }
    
    data class ObservabilityComponentConfiguration(
        val diagnosticRetentionTime: Long = 7 * 24 * 60 * 60 * 1000L, // 7天
        val traceRetentionTime: Long = 24 * 60 * 60 * 1000L, // 1天
        val logRetentionTime: Long = 7 * 24 * 60 * 60 * 1000L // 7天
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "diagnosticRetentionTime" to diagnosticRetentionTime,
                "traceRetentionTime" to traceRetentionTime,
                "logRetentionTime" to logRetentionTime
            )
        }
    }
    
    data class ComprehensiveStatusReport(
        val timestamp: Long,
        val isStarted: Boolean,
        val configuration: ObservabilityConfiguration,
        val monitoringStatus: MonitoringStatus?,
        val learningStatus: LearningStatus?,
        val alertingStatus: AlertingStatus?,
        val observabilityStatus: ObservabilityStatus?
    )
    
    data class MonitoringStatus(
        val metrics: Any,
        val healthReport: Any,
        val performanceStatistics: Any
    )
    
    data class LearningStatus(
        val learningStatistics: Any,
        val patternAnalysisStatistics: Any,
        val learningDataStatistics: Any
    )
    
    data class AlertingStatus(
        val alertStatistics: Any,
        val thresholdStatistics: Any,
        val ruleEngineStatistics: Any
    )
    
    data class ObservabilityStatus(
        val diagnosticStatistics: Any,
        val traceStatistics: Any,
        val loggingStatistics: Any
    )
    
    /**
     * 可观测性监听器接口
     */
    interface ObservabilityListener {
        fun onObservabilityStarted() {}
        fun onObservabilityStopped() {}
        fun onConfigurationUpdated(
            oldConfiguration: ObservabilityConfiguration,
            newConfiguration: ObservabilityConfiguration
        ) {}
    }
}