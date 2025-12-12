package com.empathy.ai.data.observability

import android.content.Context
import android.util.Log
import com.empathy.ai.data.monitoring.AiResponseParserMetrics
import com.empathy.ai.data.monitoring.HealthCheckSystem
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * 诊断信息收集器
 * 
 * 负责收集系统运行时的各种诊断信息
 * 提供详细的系统状态和问题诊断数据
 */
class DiagnosticCollector private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DiagnosticCollector"
        
        // 诊断信息保留时间（毫秒）
        private const val DIAGNOSTIC_RETENTION_TIME = 7 * 24 * 60 * 60 * 1000L // 7天
        
        // 最大诊断条目数
        private const val MAX_DIAGNOSTIC_ENTRIES = 1000
        
        // 诊断文件目录
        private const val DIAGNOSTIC_DIR = "diagnostics"
        
        @Volatile
        private var INSTANCE: DiagnosticCollector? = null
        
        fun getInstance(context: Context): DiagnosticCollector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiagnosticCollector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    // 诊断信息存储
    private val diagnosticEntries = CopyOnWriteArrayList<DiagnosticEntry>()
    private val systemSnapshots = ConcurrentHashMap<String, SystemSnapshot>()
    private val errorReports = ConcurrentHashMap<String, ErrorReport>()
    
    // 统计信息
    private val totalCollectedEntries = AtomicLong(0)
    private val lastCollectionTime = AtomicLong(0)
    
    // 监听器
    private val diagnosticListeners = CopyOnWriteArrayList<DiagnosticListener>()
    
    init {
        // 确保诊断目录存在
        ensureDiagnosticDirectoryExists()
        
        // 启动定期清理任务
        startPeriodicCleanup()
        
        Log.i(TAG, "DiagnosticCollector 初始化完成")
    }
    
    /**
     * 收集系统诊断信息
     */
    fun collectSystemDiagnostics(
        level: DiagnosticLevel = DiagnosticLevel.INFO,
        category: DiagnosticCategory = DiagnosticCategory.SYSTEM,
        title: String,
        message: String,
        data: Map<String, Any> = emptyMap(),
        tags: Set<String> = emptySet()
    ) {
        val entry = DiagnosticEntry(
            id = generateDiagnosticId(),
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            title = title,
            message = message,
            data = data,
            tags = tags,
            threadName = Thread.currentThread().name,
            stackTrace = Thread.currentThread().stackTrace.slice(1..5)
        )
        
        // 添加到内存存储
        addDiagnosticEntry(entry)
        
        // 通知监听器
        notifyDiagnosticCollected(entry)
        
        // 如果是错误级别，创建错误报告
        if (level == DiagnosticLevel.ERROR || level == DiagnosticLevel.CRITICAL) {
            createErrorReport(entry)
        }
        
        Log.d(TAG, "收集诊断信息: $title")
    }
    
    /**
     * 收集解析器诊断信息
     */
    fun collectParserDiagnostics(
        operationType: String,
        requestId: String? = null,
        success: Boolean,
        duration: Long,
        errorMessage: String? = null,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val level = if (success) DiagnosticLevel.INFO else DiagnosticLevel.ERROR
        val category = DiagnosticCategory.PARSER
        
        val data = mutableMapOf<String, Any>(
            "operationType" to operationType,
            "success" to success,
            "duration" to duration
        )
        
        if (requestId != null) {
            data["requestId"] = requestId
        }
        
        if (errorMessage != null) {
            data["errorMessage"] = errorMessage
        }
        
        data.putAll(additionalData)
        
        val title = if (success) {
            "解析操作成功: $operationType"
        } else {
            "解析操作失败: $operationType"
        }
        
        val message = if (success) {
            "操作 $operationType 在 ${duration}ms 内成功完成"
        } else {
            "操作 $operationType 在 ${duration}ms 后失败: $errorMessage"
        }
        
        collectSystemDiagnostics(
            level = level,
            category = category,
            title = title,
            message = message,
            data = data,
            tags = setOf("parser", operationType.lowercase())
        )
    }
    
    /**
     * 收集性能诊断信息
     */
    fun collectPerformanceDiagnostics(
        operation: String,
        metrics: Map<String, Any>,
        thresholds: Map<String, Double> = emptyMap()
    ) {
        val violations = mutableListOf<String>()
        
        thresholds.forEach { (metric, threshold) ->
            val value = metrics[metric]
            if (value is Number && value.toDouble() > threshold) {
                violations.add("$metric 超过阈值: ${value.toDouble()} > $threshold")
            }
        }
        
        val level = if (violations.isNotEmpty()) {
            DiagnosticLevel.WARNING
        } else {
            DiagnosticLevel.INFO
        }
        
        val title = "性能诊断: $operation"
        val message = if (violations.isNotEmpty()) {
            "检测到性能问题: ${violations.joinToString(", ")}"
        } else {
            "性能正常"
        }
        
        val data = mutableMapOf<String, Any>(
            "operation" to operation,
            "metrics" to metrics,
            "thresholds" to thresholds
        )
        
        if (violations.isNotEmpty()) {
            data["violations"] = violations
        }
        
        collectSystemDiagnostics(
            level = level,
            category = DiagnosticCategory.PERFORMANCE,
            title = title,
            message = message,
            data = data,
            tags = setOf("performance", operation.lowercase())
        )
    }
    
    /**
     * 创建系统快照
     */
    fun createSystemSnapshot(
        name: String,
        description: String? = null,
        includeMetrics: Boolean = true,
        includeHealth: Boolean = true,
        includeMemory: Boolean = true,
        includeThreads: Boolean = true
    ): SystemSnapshot {
        val snapshotId = generateSnapshotId()
        val timestamp = System.currentTimeMillis()
        
        val data = mutableMapOf<String, Any>()
        
        // 收集系统指标
        if (includeMetrics) {
            try {
                val metrics = AiResponseParserMetrics.getInstance()
                data["metrics"] = metrics.getOverallMetrics()
            } catch (e: Exception) {
                Log.w(TAG, "收集指标失败", e)
                data["metrics_error"] = e.message ?: "未知错误"
            }
        }
        
        // 收集健康状态
        if (includeHealth) {
            try {
                val healthSystem = HealthCheckSystem.getInstance()
                data["health"] = healthSystem.getHealthReport()
            } catch (e: Exception) {
                Log.w(TAG, "收集健康状态失败", e)
                data["health_error"] = e.message ?: "未知错误"
            }
        }
        
        // 收集内存信息
        if (includeMemory) {
            try {
                val runtime = Runtime.getRuntime()
                val memoryInfo = mapOf(
                    "totalMemory" to runtime.totalMemory(),
                    "freeMemory" to runtime.freeMemory(),
                    "usedMemory" to (runtime.totalMemory() - runtime.freeMemory()),
                    "maxMemory" to runtime.maxMemory(),
                    "memoryUsagePercent" to ((runtime.totalMemory() - runtime.freeMemory()).toDouble() / runtime.maxMemory() * 100)
                )
                data["memory"] = memoryInfo
            } catch (e: Exception) {
                Log.w(TAG, "收集内存信息失败", e)
                data["memory_error"] = e.message ?: "未知错误"
            }
        }
        
        // 收集线程信息
        if (includeThreads) {
            try {
                val threadCount = Thread.activeCount()
                val threadInfo = mapOf(
                    "activeCount" to threadCount,
                    "currentThread" to Thread.currentThread().name
                )
                data["threads"] = threadInfo
            } catch (e: Exception) {
                Log.w(TAG, "收集线程信息失败", e)
                data["threads_error"] = e.message ?: "未知错误"
            }
        }
        
        val snapshot = SystemSnapshot(
            id = snapshotId,
            name = name,
            description = description,
            timestamp = timestamp,
            data = data
        )
        
        // 保存快照
        systemSnapshots[snapshotId] = snapshot
        
        // 记录诊断信息
        collectSystemDiagnostics(
            level = DiagnosticLevel.INFO,
            category = DiagnosticCategory.SYSTEM,
            title = "创建系统快照: $name",
            message = "系统快照已创建，包含 ${data.keys.size} 个数据项",
            data = mapOf(
                "snapshotId" to snapshotId,
                "dataItems" to data.keys.toList()
            ),
            tags = setOf("snapshot", name.lowercase())
        )
        
        Log.i(TAG, "创建系统快照: $name ($snapshotId)")
        
        return snapshot
    }
    
    /**
     * 获取诊断信息
     */
    fun getDiagnosticEntries(
        level: DiagnosticLevel? = null,
        category: DiagnosticCategory? = null,
        tags: Set<String> = emptySet(),
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int = 100
    ): List<DiagnosticEntry> {
        return diagnosticEntries.asSequence()
            .filter { entry ->
                // 过滤级别
                level?.let { entry.level == it } ?: true
            }
            .filter { entry ->
                // 过滤类别
                category?.let { entry.category == it } ?: true
            }
            .filter { entry ->
                // 过滤标签
                if (tags.isEmpty()) true
                else tags.any { tag -> entry.tags.contains(tag) }
            }
            .filter { entry ->
                // 过滤时间范围
                val afterStart = startTime?.let { entry.timestamp >= it } ?: true
                val beforeEnd = endTime?.let { entry.timestamp <= it } ?: true
                afterStart && beforeEnd
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
            .toList()
    }
    
    /**
     * 获取系统快照
     */
    fun getSystemSnapshot(snapshotId: String): SystemSnapshot? {
        return systemSnapshots[snapshotId]
    }
    
    /**
     * 获取所有系统快照
     */
    fun getAllSystemSnapshots(): List<SystemSnapshot> {
        return systemSnapshots.values.sortedByDescending { it.timestamp }
    }
    
    /**
     * 获取错误报告
     */
    fun getErrorReport(errorId: String): ErrorReport? {
        return errorReports[errorId]
    }
    
    /**
     * 获取所有错误报告
     */
    fun getAllErrorReports(): List<ErrorReport> {
        return errorReports.values.sortedByDescending { it.timestamp }
    }
    
    /**
     * 导出诊断信息
     */
    suspend fun exportDiagnostics(
        outputPath: String? = null,
        includeSnapshots: Boolean = true,
        includeErrorReports: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val defaultPath = File(context.filesDir, "$DIAGNOSTIC_DIR/diagnostics_$timestamp.json")
        val file = outputPath?.let { File(it) } ?: defaultPath
        
        try {
            val exportData = mutableMapOf<String, Any>()
            
            // 导出诊断条目
            exportData["diagnosticEntries"] = diagnosticEntries.map { it.toMap() }
            
            // 导出系统快照
            if (includeSnapshots) {
                exportData["systemSnapshots"] = systemSnapshots.values.map { it.toMap() }
            }
            
            // 导出错误报告
            if (includeErrorReports) {
                exportData["errorReports"] = errorReports.values.map { it.toMap() }
            }
            
            // 导出元数据
            exportData["metadata"] = mapOf(
                "exportedAt" to System.currentTimeMillis(),
                "totalEntries" to diagnosticEntries.size,
                "totalSnapshots" to systemSnapshots.size,
                "totalErrorReports" to errorReports.size,
                "version" to "1.0"
            )
            
            // 写入文件
            FileWriter(file).use { writer ->
                writer.write(JSONObject(exportData).toString(2))
            }
            
            Log.i(TAG, "诊断信息已导出到: ${file.absolutePath}")
            
            file.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "导出诊断信息失败", e)
            throw e
        }
    }
    
    /**
     * 清理旧的诊断信息
     */
    fun cleanupOldDiagnostics() {
        val cutoffTime = System.currentTimeMillis() - DIAGNOSTIC_RETENTION_TIME
        
        // 清理诊断条目
        val removedEntries = diagnosticEntries.removeAll { it.timestamp < cutoffTime }
        
        // 清理系统快照
        val removedSnapshots = systemSnapshots.values.removeAll { it.timestamp < cutoffTime }
        
        // 清理错误报告
        val removedErrorReports = errorReports.values.removeAll { it.timestamp < cutoffTime }
        
        Log.i(TAG, "清理完成: 移除 $removedEntries 个诊断条目, $removedSnapshots 个快照, $removedErrorReports 个错误报告")
    }
    
    /**
     * 获取诊断统计
     */
    fun getDiagnosticStatistics(): DiagnosticStatistics {
        val now = System.currentTimeMillis()
        val last24Hours = now - 24 * 60 * 60 * 1000L
        val last7Days = now - 7 * 24 * 60 * 60 * 1000L
        
        val recentEntries = diagnosticEntries.filter { it.timestamp > last24Hours }
        val weeklyEntries = diagnosticEntries.filter { it.timestamp > last7Days }
        
        val levelDistribution = diagnosticEntries.groupBy { it.level }.mapValues { it.value.size }
        val categoryDistribution = diagnosticEntries.groupBy { it.category }.mapValues { it.value.size }
        
        return DiagnosticStatistics(
            totalEntries = diagnosticEntries.size,
            totalSnapshots = systemSnapshots.size,
            totalErrorReports = errorReports.size,
            last24HoursEntries = recentEntries.size,
            last7DaysEntries = weeklyEntries.size,
            levelDistribution = levelDistribution,
            categoryDistribution = categoryDistribution,
            lastCollectionTime = lastCollectionTime.get(),
            lastUpdated = now
        )
    }
    
    /**
     * 添加诊断监听器
     */
    fun addDiagnosticListener(listener: DiagnosticListener) {
        diagnosticListeners.add(listener)
    }
    
    /**
     * 移除诊断监听器
     */
    fun removeDiagnosticListener(listener: DiagnosticListener) {
        diagnosticListeners.remove(listener)
    }
    
    /**
     * 添加诊断条目
     */
    private fun addDiagnosticEntry(entry: DiagnosticEntry) {
        synchronized(diagnosticEntries) {
            diagnosticEntries.add(entry)
            
            // 保持最大条目数限制
            while (diagnosticEntries.size > MAX_DIAGNOSTIC_ENTRIES) {
                diagnosticEntries.removeAt(0)
            }
        }
        
        totalCollectedEntries.incrementAndGet()
        lastCollectionTime.set(System.currentTimeMillis())
    }
    
    /**
     * 创建错误报告
     */
    private fun createErrorReport(entry: DiagnosticEntry) {
        val errorId = generateErrorId()
        val errorReport = ErrorReport(
            id = errorId,
            diagnosticEntryId = entry.id,
            timestamp = entry.timestamp,
            level = entry.level,
            title = entry.title,
            message = entry.message,
            data = entry.data,
            stackTrace = entry.stackTrace,
            threadName = entry.threadName,
            tags = entry.tags
        )
        
        errorReports[errorId] = errorReport
        
        Log.w(TAG, "创建错误报告: $errorId")
    }
    
    /**
     * 确保诊断目录存在
     */
    private fun ensureDiagnosticDirectoryExists() {
        val diagnosticDir = File(context.filesDir, DIAGNOSTIC_DIR)
        if (!diagnosticDir.exists()) {
            diagnosticDir.mkdirs()
        }
    }
    
    /**
     * 启动定期清理任务
     */
    private fun startPeriodicCleanup() {
        scope.launch {
            while (isActive) {
                try {
                    cleanupOldDiagnostics()
                    delay(24 * 60 * 60 * 1000L) // 每24小时清理一次
                } catch (e: Exception) {
                    Log.e(TAG, "定期清理任务异常", e)
                    delay(60 * 60 * 1000L) // 错误后等待1小时再重试
                }
            }
        }
    }
    
    /**
     * 通知诊断信息收集
     */
    private fun notifyDiagnosticCollected(entry: DiagnosticEntry) {
        diagnosticListeners.forEach { it.onDiagnosticCollected(entry) }
    }
    
    /**
     * 生成诊断ID
     */
    private fun generateDiagnosticId(): String {
        return "diag_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 生成快照ID
     */
    private fun generateSnapshotId(): String {
        return "snap_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 生成错误ID
     */
    private fun generateErrorId(): String {
        return "err_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    // 数据类定义
    data class DiagnosticEntry(
        val id: String,
        val timestamp: Long,
        val level: DiagnosticLevel,
        val category: DiagnosticCategory,
        val title: String,
        val message: String,
        val data: Map<String, Any>,
        val tags: Set<String>,
        val threadName: String,
        val stackTrace: Array<StackTraceElement>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "timestamp" to timestamp,
                "level" to level.name,
                "category" to category.name,
                "title" to title,
                "message" to message,
                "data" to data,
                "tags" to tags.toList(),
                "threadName" to threadName,
                "stackTrace" to stackTrace.map { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
            )
        }
    }
    
    data class SystemSnapshot(
        val id: String,
        val name: String,
        val description: String?,
        val timestamp: Long,
        val data: Map<String, Any>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "name" to name,
                "description" to description,
                "timestamp" to timestamp,
                "data" to data
            )
        }
    }
    
    data class ErrorReport(
        val id: String,
        val diagnosticEntryId: String,
        val timestamp: Long,
        val level: DiagnosticLevel,
        val title: String,
        val message: String,
        val data: Map<String, Any>,
        val stackTrace: Array<StackTraceElement>,
        val threadName: String,
        val tags: Set<String>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "diagnosticEntryId" to diagnosticEntryId,
                "timestamp" to timestamp,
                "level" to level.name,
                "title" to title,
                "message" to message,
                "data" to data,
                "stackTrace" to stackTrace.map { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" },
                "threadName" to threadName,
                "tags" to tags.toList()
            )
        }
    }
    
    data class DiagnosticStatistics(
        val totalEntries: Int,
        val totalSnapshots: Int,
        val totalErrorReports: Int,
        val last24HoursEntries: Int,
        val last7DaysEntries: Int,
        val levelDistribution: Map<DiagnosticLevel, Int>,
        val categoryDistribution: Map<DiagnosticCategory, Int>,
        val lastCollectionTime: Long,
        val lastUpdated: Long
    )
    
    enum class DiagnosticLevel {
        DEBUG,    // 调试信息
        INFO,     // 一般信息
        WARNING,  // 警告信息
        ERROR,    // 错误信息
        CRITICAL  // 严重错误
    }
    
    enum class DiagnosticCategory {
        SYSTEM,     // 系统相关
        PARSER,     // 解析器相关
        PERFORMANCE, // 性能相关
        MEMORY,     // 内存相关
        NETWORK,    // 网络相关
        SECURITY,   // 安全相关
        USER        // 用户相关
    }
    
    /**
     * 诊断监听器接口
     */
    interface DiagnosticListener {
        fun onDiagnosticCollected(entry: DiagnosticEntry) {}
    }
}