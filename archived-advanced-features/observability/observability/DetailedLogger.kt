package com.empathy.ai.data.observability

import android.content.Context
import android.util.Log
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
 * 详细日志记录器
 * 
 * 提供结构化的详细日志记录功能
 * 支持多种日志级别、分类和持久化存储
 */
class DetailedLogger private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DetailedLogger"
        
        // 日志文件目录
        private const val LOG_DIR = "logs"
        
        // 日志保留时间（毫秒）
        private const val LOG_RETENTION_TIME = 7 * 24 * 60 * 60 * 1000L // 7天
        
        // 最大日志条目数
        private const val MAX_LOG_ENTRIES = 10000
        
        // 日志文件大小限制（字节）
        private const val MAX_LOG_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        
        @Volatile
        private var INSTANCE: DetailedLogger? = null
        
        fun getInstance(context: Context): DetailedLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DetailedLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    
    // 日志存储
    private val logEntries = CopyOnWriteArrayList<LogEntry>()
    private val logFiles = ConcurrentHashMap<String, LogFile>()
    
    // 统计信息
    private val totalLogEntries = AtomicLong(0)
    private val lastLogTime = AtomicLong(0)
    
    // 配置
    private var logLevel = LogLevel.INFO
    private var enableFileLogging = true
    private var enableConsoleLogging = true
    private var enableStructuredLogging = true
    
    // 监听器
    private val logListeners = CopyOnWriteArrayList<LogListener>()
    
    init {
        // 确保日志目录存在
        ensureLogDirectoryExists()
        
        // 启动定期清理任务
        startPeriodicCleanup()
        
        // 启动日志文件轮转任务
        startLogFileRotation()
        
        Log.i(TAG, "DetailedLogger 初始化完成")
    }
    
    /**
     * 记录调试日志
     */
    fun debug(
        category: LogCategory,
        message: String,
        data: Map<String, Any> = emptyMap(),
        tags: Set<String> = emptySet(),
        throwable: Throwable? = null
    ) {
        log(LogLevel.DEBUG, category, message, data, tags, throwable)
    }
    
    /**
     * 记录信息日志
     */
    fun info(
        category: LogCategory,
        message: String,
        data: Map<String, Any> = emptyMap(),
        tags: Set<String> = emptySet(),
        throwable: Throwable? = null
    ) {
        log(LogLevel.INFO, category, message, data, tags, throwable)
    }
    
    /**
     * 记录警告日志
     */
    fun warning(
        category: LogCategory,
        message: String,
        data: Map<String, Any> = emptyMap(),
        tags: Set<String> = emptySet(),
        throwable: Throwable? = null
    ) {
        log(LogLevel.WARNING, category, message, data, tags, throwable)
    }
    
    /**
     * 记录错误日志
     */
    fun error(
        category: LogCategory,
        message: String,
        data: Map<String, Any> = emptyMap(),
        tags: Set<String> = emptySet(),
        throwable: Throwable? = null
    ) {
        log(LogLevel.ERROR, category, message, data, tags, throwable)
    }
    
    /**
     * 记录致命错误日志
     */
    fun fatal(
        category: LogCategory,
        message: String,
        data: Map<String, Any> = emptyMap(),
        tags: Set<String> = emptySet(),
        throwable: Throwable? = null
    ) {
        log(LogLevel.FATAL, category, message, data, tags, throwable)
    }
    
    /**
     * 记录解析器日志
     */
    fun logParserEvent(
        operationType: String,
        requestId: String? = null,
        success: Boolean,
        duration: Long,
        inputSize: Int? = null,
        outputSize: Int? = null,
        errorMessage: String? = null
    ) {
        val level = if (success) LogLevel.INFO else LogLevel.ERROR
        val message = if (success) {
            "解析操作成功: $operationType"
        } else {
            "解析操作失败: $operationType"
        }
        
        val data = mutableMapOf<String, Any>(
            "operationType" to operationType,
            "success" to success,
            "duration" to duration
        )
        
        if (requestId != null) {
            data["requestId"] = requestId
        }
        
        if (inputSize != null) {
            data["inputSize"] = inputSize
        }
        
        if (outputSize != null) {
            data["outputSize"] = outputSize
        }
        
        if (errorMessage != null) {
            data["errorMessage"] = errorMessage
        }
        
        val tags = setOf("parser", operationType.lowercase())
        
        log(level, LogCategory.PARSER, message, data, tags)
    }
    
    /**
     * 记录性能日志
     */
    fun logPerformanceEvent(
        operation: String,
        metrics: Map<String, Number>,
        thresholds: Map<String, Number> = emptyMap()
    ) {
        val violations = mutableListOf<String>()
        
        thresholds.forEach { (metric, threshold) ->
            val value = metrics[metric]
            if (value != null && value.toDouble() > threshold.toDouble()) {
                violations.add("$metric 超过阈值: $value > $threshold")
            }
        }
        
        val level = if (violations.isNotEmpty()) LogLevel.WARNING else LogLevel.INFO
        val message = if (violations.isNotEmpty()) {
            "性能警告: $operation - ${violations.joinToString(", ")}"
        } else {
            "性能正常: $operation"
        }
        
        val data = mutableMapOf<String, Any>(
            "operation" to operation,
            "metrics" to metrics
        )
        
        if (violations.isNotEmpty()) {
            data["violations"] = violations
        }
        
        if (thresholds.isNotEmpty()) {
            data["thresholds"] to thresholds
        }
        
        val tags = setOf("performance", operation.lowercase())
        
        log(level, LogCategory.PERFORMANCE, message, data, tags)
    }
    
    /**
     * 记录学习事件日志
     */
    fun logLearningEvent(
        eventType: String,
        details: Map<String, Any>,
        success: Boolean = true
    ) {
        val level = if (success) LogLevel.INFO else LogLevel.WARNING
        val message = "学习事件: $eventType"
        
        val data = mutableMapOf<String, Any>(
            "eventType" to eventType,
            "details" to details,
            "success" to success
        )
        
        val tags = setOf("learning", eventType.lowercase())
        
        log(level, LogCategory.LEARNING, message, data, tags)
    }
    
    /**
     * 记录告警事件日志
     */
    fun logAlertEvent(
        alertType: String,
        alertLevel: String,
        message: String,
        ruleId: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val level = when (alertLevel.uppercase()) {
            "CRITICAL" -> LogLevel.ERROR
            "WARNING" -> LogLevel.WARNING
            else -> LogLevel.INFO
        }
        
        val data = mutableMapOf<String, Any>(
            "alertType" to alertType,
            "alertLevel" to alertLevel,
            "message" to message
        )
        
        if (ruleId != null) {
            data["ruleId"] = ruleId
        }
        
        if (metadata.isNotEmpty()) {
            data.putAll(metadata)
        }
        
        val tags = setOf("alert", alertType.lowercase())
        
        log(level, LogCategory.ALERTING, "告警触发: $alertType", data, tags)
    }
    
    /**
     * 通用日志记录方法
     */
    private fun log(
        level: LogLevel,
        category: LogCategory,
        message: String,
        data: Map<String, Any> = emptyMap(),
        tags: Set<String> = emptySet(),
        throwable: Throwable? = null
    ) {
        // 检查日志级别
        if (level.ordinal < logLevel.ordinal) {
            return
        }
        
        val timestamp = System.currentTimeMillis()
        val threadName = Thread.currentThread().name
        
        val logEntry = LogEntry(
            id = generateLogId(),
            timestamp = timestamp,
            level = level,
            category = category,
            message = message,
            data = data,
            tags = tags,
            threadName = threadName,
            throwable = throwable
        )
        
        // 添加到内存存储
        addLogEntry(logEntry)
        
        // 控制台日志
        if (enableConsoleLogging) {
            logToConsole(logEntry)
        }
        
        // 文件日志
        if (enableFileLogging) {
            logToFile(logEntry)
        }
        
        // 通知监听器
        notifyLogListeners(logEntry)
    }
    
    /**
     * 获取日志条目
     */
    fun getLogEntries(
        level: LogLevel? = null,
        category: LogCategory? = null,
        tags: Set<String> = emptySet(),
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int = 100
    ): List<LogEntry> {
        return logEntries.asSequence()
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
     * 获取日志统计
     */
    fun getLoggingStatistics(): LoggingStatistics {
        val now = System.currentTimeMillis()
        val last24Hours = now - 24 * 60 * 60 * 1000L
        val last7Days = now - 7 * 24 * 60 * 60 * 1000L
        
        val recentEntries = logEntries.filter { it.timestamp > last24Hours }
        val weeklyEntries = logEntries.filter { it.timestamp > last7Days }
        
        val levelDistribution = logEntries.groupBy { it.level }.mapValues { it.value.size }
        val categoryDistribution = logEntries.groupBy { it.category }.mapValues { it.value.size }
        
        return LoggingStatistics(
            totalEntries = logEntries.size,
            totalLogEntries = totalLogEntries.get(),
            last24HoursEntries = recentEntries.size,
            last7DaysEntries = weeklyEntries.size,
            levelDistribution = levelDistribution,
            categoryDistribution = categoryDistribution,
            lastLogTime = lastLogTime.get(),
            lastUpdated = now
        )
    }
    
    /**
     * 导出日志
     */
    suspend fun exportLogs(
        outputPath: String? = null,
        level: LogLevel? = null,
        category: LogCategory? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): String = withContext(Dispatchers.IO) {
        val entries = getLogEntries(level, category, emptySet(), startTime, endTime, Int.MAX_VALUE)
        
        val exportData = mapOf(
            "logs" to entries.map { it.toMap() },
            "metadata" to mapOf(
                "exportedAt" to System.currentTimeMillis(),
                "totalEntries" to entries.size,
                "version" to "1.0"
            )
        )
        
        if (outputPath != null) {
            // 导出到文件
            FileWriter(File(outputPath)).use { writer ->
                writer.write(JSONObject(exportData).toString(2))
            }
            outputPath
        } else {
            // 返回JSON字符串
            JSONObject(exportData).toString(2)
        }
    }
    
    /**
     * 清理旧日志
     */
    fun cleanupOldLogs() {
        val cutoffTime = System.currentTimeMillis() - LOG_RETENTION_TIME
        
        // 清理内存中的日志
        val removedEntries = logEntries.removeAll { it.timestamp < cutoffTime }
        
        // 清理日志文件
        val logDir = File(context.filesDir, LOG_DIR)
        if (logDir.exists()) {
            logDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                    logFiles.remove(file.name)
                }
            }
        }
        
        Log.i(TAG, "清理完成: 移除 $removedEntries 个日志条目")
    }
    
    /**
     * 设置日志级别
     */
    fun setLogLevel(level: LogLevel) {
        logLevel = level
        Log.i(TAG, "设置日志级别: $level")
    }
    
    /**
     * 启用/禁用文件日志
     */
    fun setFileLoggingEnabled(enabled: Boolean) {
        enableFileLogging = enabled
        Log.i(TAG, "文件日志: ${if (enabled) "启用" else "禁用"}")
    }
    
    /**
     * 启用/禁用控制台日志
     */
    fun setConsoleLoggingEnabled(enabled: Boolean) {
        enableConsoleLogging = enabled
        Log.i(TAG, "控制台日志: ${if (enabled) "启用" else "禁用"}")
    }
    
    /**
     * 添加日志监听器
     */
    fun addLogListener(listener: LogListener) {
        logListeners.add(listener)
    }
    
    /**
     * 移除日志监听器
     */
    fun removeLogListener(listener: LogListener) {
        logListeners.remove(listener)
    }
    
    /**
     * 添加日志条目
     */
    private fun addLogEntry(entry: LogEntry) {
        synchronized(logEntries) {
            logEntries.add(entry)
            
            // 保持最大条目数限制
            while (logEntries.size > MAX_LOG_ENTRIES) {
                logEntries.removeAt(0)
            }
        }
        
        totalLogEntries.incrementAndGet()
        lastLogTime.set(System.currentTimeMillis())
    }
    
    /**
     * 记录到控制台
     */
    private fun logToConsole(entry: LogEntry) {
        val tag = "AI_PARSER_${entry.category.name}"
        val message = formatConsoleMessage(entry)
        
        when (entry.level) {
            LogLevel.DEBUG -> Log.d(tag, message, entry.throwable)
            LogLevel.INFO -> Log.i(tag, message, entry.throwable)
            LogLevel.WARNING -> Log.w(tag, message, entry.throwable)
            LogLevel.ERROR -> Log.e(tag, message, entry.throwable)
            LogLevel.FATAL -> Log.wtf(tag, message, entry.throwable)
        }
    }
    
    /**
     * 记录到文件
     */
    private fun logToFile(entry: LogEntry) {
        scope.launch {
            try {
                val fileName = getLogFileName(entry.timestamp)
                val logFile = getOrCreateLogFile(fileName)
                
                val logLine = formatFileLogLine(entry)
                
                FileWriter(logFile.file, true).use { writer ->
                    writer.write(logLine)
                    writer.write("\n")
                }
                
                // 更新文件大小
                logFile.size += logLine.length + 1
                
            } catch (e: Exception) {
                Log.e(TAG, "写入日志文件失败", e)
            }
        }
    }
    
    /**
     * 格式化控制台消息
     */
    private fun formatConsoleMessage(entry: LogEntry): String {
        val timestamp = dateFormat.format(Date(entry.timestamp))
        val builder = StringBuilder()
        
        builder.append("[$timestamp] ")
        builder.append("[${entry.level.name}] ")
        builder.append("[${entry.threadName}] ")
        builder.append(entry.message)
        
        if (entry.data.isNotEmpty()) {
            builder.append(" | Data: ${entry.data}")
        }
        
        if (entry.tags.isNotEmpty()) {
            builder.append(" | Tags: ${entry.tags.joinToString(", ")}")
        }
        
        return builder.toString()
    }
    
    /**
     * 格式化文件日志行
     */
    private fun formatFileLogLine(entry: LogEntry): String {
        val logData = mutableMapOf<String, Any>(
            "timestamp" to entry.timestamp,
            "level" to entry.level.name,
            "category" to entry.category.name,
            "message" to entry.message,
            "threadName" to entry.threadName
        )
        
        if (entry.data.isNotEmpty()) {
            logData["data"] = entry.data
        }
        
        if (entry.tags.isNotEmpty()) {
            logData["tags"] = entry.tags.toList()
        }
        
        if (entry.throwable != null) {
            logData["throwable"] = entry.throwable.stackTraceToString()
        }
        
        return JSONObject(logData).toString()
    }
    
    /**
     * 获取日志文件名
     */
    private fun getLogFileName(timestamp: Long): String {
        return "ai_parser_${fileDateFormat.format(Date(timestamp))}.jsonl"
    }
    
    /**
     * 获取或创建日志文件
     */
    private fun getOrCreateLogFile(fileName: String): LogFile {
        return logFiles.getOrPut(fileName) {
            val file = File(context.filesDir, "$LOG_DIR/$fileName")
            LogFile(file, if (file.exists()) file.length() else 0L)
        }
    }
    
    /**
     * 确保日志目录存在
     */
    private fun ensureLogDirectoryExists() {
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }
    
    /**
     * 启动定期清理任务
     */
    private fun startPeriodicCleanup() {
        scope.launch {
            while (isActive) {
                try {
                    cleanupOldLogs()
                    delay(24 * 60 * 60 * 1000L) // 每24小时清理一次
                } catch (e: Exception) {
                    Log.e(TAG, "定期清理任务异常", e)
                    delay(60 * 60 * 1000L) // 错误后等待1小时再重试
                }
            }
        }
    }
    
    /**
     * 启动日志文件轮转任务
     */
    private fun startLogFileRotation() {
        scope.launch {
            while (isActive) {
                try {
                    checkAndRotateLogFiles()
                    delay(60 * 60 * 1000L) // 每小时检查一次
                } catch (e: Exception) {
                    Log.e(TAG, "日志文件轮转任务异常", e)
                    delay(60 * 60 * 1000L) // 错误后等待1小时再重试
                }
            }
        }
    }
    
    /**
     * 检查并轮转日志文件
     */
    private fun checkAndRotateLogFiles() {
        logFiles.values.forEach { logFile ->
            if (logFile.size > MAX_LOG_FILE_SIZE) {
                // 文件过大，进行轮转
                val originalFile = logFile.file
                val rotatedFile = File(originalFile.parent, "${originalFile.name}.old")
                
                originalFile.renameTo(rotatedFile)
                logFiles.remove(originalFile.name)
                
                Log.i(TAG, "日志文件轮转: ${originalFile.name}")
            }
        }
    }
    
    /**
     * 通知日志监听器
     */
    private fun notifyLogListeners(entry: LogEntry) {
        logListeners.forEach { it.onLogEntry(entry) }
    }
    
    /**
     * 生成日志ID
     */
    private fun generateLogId(): String {
        return "log_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    // 数据类定义
    data class LogEntry(
        val id: String,
        val timestamp: Long,
        val level: LogLevel,
        val category: LogCategory,
        val message: String,
        val data: Map<String, Any>,
        val tags: Set<String>,
        val threadName: String,
        val throwable: Throwable?
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "timestamp" to timestamp,
                "level" to level.name,
                "category" to category.name,
                "message" to message,
                "data" to data,
                "tags" to tags.toList(),
                "threadName" to threadName,
                "throwable" to (throwable?.stackTraceToString() ?: null)
            )
        }
    }
    
    data class LogFile(
        val file: File,
        var size: Long
    )
    
    data class LoggingStatistics(
        val totalEntries: Int,
        val totalLogEntries: Long,
        val last24HoursEntries: Int,
        val last7DaysEntries: Int,
        val levelDistribution: Map<LogLevel, Int>,
        val categoryDistribution: Map<LogCategory, Int>,
        val lastLogTime: Long,
        val lastUpdated: Long
    )
    
    enum class LogLevel {
        DEBUG,   // 调试信息
        INFO,    // 一般信息
        WARNING, // 警告信息
        ERROR,   // 错误信息
        FATAL    // 致命错误
    }
    
    enum class LogCategory {
        SYSTEM,     // 系统相关
        PARSER,     // 解析器相关
        PERFORMANCE, // 性能相关
        LEARNING,   // 学习相关
        ALERTING,   // 告警相关
        NETWORK,    // 网络相关
        DATABASE,   // 数据库相关
        USER        // 用户相关
    }
    
    /**
     * 日志监听器接口
     */
    interface LogListener {
        fun onLogEntry(entry: LogEntry) {}
    }
}