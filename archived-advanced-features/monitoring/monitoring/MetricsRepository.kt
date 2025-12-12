package com.empathy.ai.data.monitoring

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 指标存储库
 * 
 * 负责持久化和检索性能指标数据
 * 提供数据的存储、查询和清理功能
 */
class MetricsRepository private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "MetricsRepository"
        private const val PREFS_NAME = "ai_parser_metrics"
        
        // 指标键前缀
        private const val OVERALL_METRICS_KEY = "overall_metrics"
        private const val OPERATION_METRICS_KEY_PREFIX = "operation_metrics_"
        private const val MODEL_METRICS_KEY_PREFIX = "model_metrics_"
        private const val ERROR_METRICS_KEY_PREFIX = "error_metrics_"
        private const val TIME_WINDOW_METRICS_KEY_PREFIX = "time_window_metrics_"
        
        // 数据保留配置
        private const val MAX_TIME_WINDOWS = 24 * 60 // 24小时，每分钟一个窗口
        private const val MAX_ERROR_TYPES = 100
        private const val MAX_OPERATION_TYPES = 50
        private const val MAX_MODEL_TYPES = 20
        
        @Volatile
        private var INSTANCE: MetricsRepository? = null
        
        fun getInstance(context: Context): MetricsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MetricsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    // 内存缓存
    private val overallMetricsCache = AtomicReference<OverallMetricsData?>()
    private val operationMetricsCache = ConcurrentHashMap<String, OperationMetricsData>()
    private val modelMetricsCache = ConcurrentHashMap<String, ModelMetricsData>()
    private val errorMetricsCache = ConcurrentHashMap<String, ErrorMetricsData>()
    private val timeWindowMetricsCache = ConcurrentHashMap<Long, TimeWindowMetricsData>()
    
    // 定期保存任务
    private val scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "MetricsRepository-Saver").apply {
            isDaemon = true
        }
    }
    
    init {
        // 启动定期保存任务
        scheduledExecutor.scheduleAtFixedRate({
            try {
                saveAllMetricsToStorage()
            } catch (e: Exception) {
                Log.e(TAG, "定期保存指标失败", e)
            }
        }, 1, 1, TimeUnit.MINUTES)
        
        // 从存储加载指标
        loadAllMetricsFromStorage()
        
        Log.i(TAG, "MetricsRepository 初始化完成")
    }
    
    /**
     * 保存总体指标
     */
    fun saveOverallMetrics(metrics: OverallMetricsData) {
        overallMetricsCache.set(metrics)
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存总体指标: 成功率=${String.format("%.2f%%", metrics.successRate * 100)}")
        }
    }
    
    /**
     * 获取总体指标
     */
    fun getOverallMetrics(): OverallMetricsData? {
        return overallMetricsCache.get()
    }
    
    /**
     * 保存操作类型指标
     */
    fun saveOperationMetrics(operationType: String, metrics: OperationMetricsData) {
        operationMetricsCache[operationType] = metrics
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存操作指标: $operationType, 成功率=${String.format("%.2f%%", metrics.successRate * 100)}")
        }
    }
    
    /**
     * 获取操作类型指标
     */
    fun getOperationMetrics(operationType: String): OperationMetricsData? {
        return operationMetricsCache[operationType]
    }
    
    /**
     * 获取所有操作类型指标
     */
    fun getAllOperationMetrics(): Map<String, OperationMetricsData> {
        return operationMetricsCache.toMap()
    }
    
    /**
     * 保存模型指标
     */
    fun saveModelMetrics(modelName: String, metrics: ModelMetricsData) {
        modelMetricsCache[modelName] = metrics
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存模型指标: $modelName, 成功率=${String.format("%.2f%%", metrics.successRate * 100)}")
        }
    }
    
    /**
     * 获取模型指标
     */
    fun getModelMetrics(modelName: String): ModelMetricsData? {
        return modelMetricsCache[modelName]
    }
    
    /**
     * 获取所有模型指标
     */
    fun getAllModelMetrics(): Map<String, ModelMetricsData> {
        return modelMetricsCache.toMap()
    }
    
    /**
     * 保存错误指标
     */
    fun saveErrorMetrics(errorType: String, metrics: ErrorMetricsData) {
        errorMetricsCache[errorType] = metrics
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存错误指标: $errorType, 计数=${metrics.count}")
        }
    }
    
    /**
     * 获取错误指标
     */
    fun getErrorMetrics(errorType: String): ErrorMetricsData? {
        return errorMetricsCache[errorType]
    }
    
    /**
     * 获取所有错误指标
     */
    fun getAllErrorMetrics(): Map<String, ErrorMetricsData> {
        return errorMetricsCache.toMap()
    }
    
    /**
     * 保存时间窗口指标
     */
    fun saveTimeWindowMetrics(timestamp: Long, metrics: TimeWindowMetricsData) {
        timeWindowMetricsCache[timestamp] = metrics
        
        // 限制时间窗口数量
        if (timeWindowMetricsCache.size > MAX_TIME_WINDOWS) {
            val sortedKeys = timeWindowMetricsCache.keys.sorted()
            val keysToRemove = sortedKeys.take(timeWindowMetricsCache.size - MAX_TIME_WINDOWS)
            keysToRemove.forEach { key ->
                timeWindowMetricsCache.remove(key)
            }
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存时间窗口指标: 时间戳=$timestamp, 成功率=${String.format("%.2f%%", metrics.successRate * 100)}")
        }
    }
    
    /**
     * 获取时间窗口指标
     */
    fun getTimeWindowMetrics(timestamp: Long): TimeWindowMetricsData? {
        return timeWindowMetricsCache[timestamp]
    }
    
    /**
     * 获取时间范围内的指标
     */
    fun getTimeWindowMetricsInRange(startTime: Long, endTime: Long): List<TimeWindowMetricsData> {
        return timeWindowMetricsCache.entries
            .filter { (timestamp, _) -> timestamp in startTime..endTime }
            .map { it.value }
            .sortedBy { it.timestamp }
    }
    
    /**
     * 获取最近N个时间窗口的指标
     */
    fun getRecentTimeWindowMetrics(count: Int): List<TimeWindowMetricsData> {
        return timeWindowMetricsCache.values
            .sortedByDescending { it.timestamp }
            .take(count)
    }
    
    /**
     * 清理过期指标
     */
    fun cleanupExpiredMetrics(retentionPeriodMs: Long = 24 * 60 * 60 * 1000L) {
        val cutoffTime = System.currentTimeMillis() - retentionPeriodMs
        
        // 清理时间窗口指标
        timeWindowMetricsCache.entries.removeIf { (timestamp, _) -> timestamp < cutoffTime }
        
        // 清理错误指标（保留最常见的）
        if (errorMetricsCache.size > MAX_ERROR_TYPES) {
            val sortedErrors = errorMetricsCache.entries
                .sortedByDescending { (_, metrics) -> metrics.count }
                .take(MAX_ERROR_TYPES)
                .toMap()
            
            errorMetricsCache.clear()
            errorMetricsCache.putAll(sortedErrors)
        }
        
        // 清理操作类型指标
        if (operationMetricsCache.size > MAX_OPERATION_TYPES) {
            val sortedOperations = operationMetricsCache.entries
                .sortedByDescending { (_, metrics) -> metrics.totalRequests }
                .take(MAX_OPERATION_TYPES)
                .toMap()
            
            operationMetricsCache.clear()
            operationMetricsCache.putAll(sortedOperations)
        }
        
        // 清理模型指标
        if (modelMetricsCache.size > MAX_MODEL_TYPES) {
            val sortedModels = modelMetricsCache.entries
                .sortedByDescending { (_, metrics) -> metrics.totalRequests }
                .take(MAX_MODEL_TYPES)
                .toMap()
            
            modelMetricsCache.clear()
            modelMetricsCache.putAll(sortedModels)
        }
        
        Log.i(TAG, "清理过期指标完成")
    }
    
    /**
     * 导出指标数据
     */
    fun exportMetricsData(): MetricsExportData {
        return MetricsExportData(
            overallMetrics = overallMetricsCache.get(),
            operationMetrics = operationMetricsCache.toMap(),
            modelMetrics = modelMetricsCache.toMap(),
            errorMetrics = errorMetricsCache.toMap(),
            timeWindowMetrics = timeWindowMetricsCache.toMap(),
            exportTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 导入指标数据
     */
    fun importMetricsData(data: MetricsExportData) {
        data.overallMetrics?.let { overallMetricsCache.set(it) }
        
        operationMetricsCache.clear()
        operationMetricsCache.putAll(data.operationMetrics)
        
        modelMetricsCache.clear()
        modelMetricsCache.putAll(data.modelMetrics)
        
        errorMetricsCache.clear()
        errorMetricsCache.putAll(data.errorMetrics)
        
        timeWindowMetricsCache.clear()
        timeWindowMetricsCache.putAll(data.timeWindowMetrics)
        
        Log.i(TAG, "导入指标数据完成")
    }
    
    /**
     * 重置所有指标
     */
    fun resetAllMetrics() {
        overallMetricsCache.set(null)
        operationMetricsCache.clear()
        modelMetricsCache.clear()
        errorMetricsCache.clear()
        timeWindowMetricsCache.clear()
        
        // 清除持久化存储
        sharedPreferences.edit().clear().apply()
        
        Log.i(TAG, "重置所有指标完成")
    }
    
    /**
     * 保存所有指标到存储
     */
    private fun saveAllMetricsToStorage() {
        try {
            val editor = sharedPreferences.edit()
            
            // 保存总体指标
            overallMetricsCache.get()?.let { metrics ->
                val json = getOverallMetricsAdapter().toJson(metrics)
                editor.putString(OVERALL_METRICS_KEY, json)
            }
            
            // 保存操作类型指标
            operationMetricsCache.forEach { (operationType, metrics) ->
                val json = getOperationMetricsAdapter().toJson(metrics)
                editor.putString(OPERATION_METRICS_KEY_PREFIX + operationType, json)
            }
            
            // 保存模型指标
            modelMetricsCache.forEach { (modelName, metrics) ->
                val json = getModelMetricsAdapter().toJson(metrics)
                editor.putString(MODEL_METRICS_KEY_PREFIX + modelName, json)
            }
            
            // 保存错误指标
            errorMetricsCache.forEach { (errorType, metrics) ->
                val json = getErrorMetricsAdapter().toJson(metrics)
                editor.putString(ERROR_METRICS_KEY_PREFIX + errorType, json)
            }
            
            // 保存时间窗口指标（只保存最近的）
            val recentTimeWindows = timeWindowMetricsCache.entries
                .sortedByDescending { it.key }
                .take(60) // 保存最近60个窗口
                .toMap()
                
            recentTimeWindows.forEach { (timestamp, metrics) ->
                val json = getTimeWindowMetricsAdapter().toJson(metrics)
                editor.putString(TIME_WINDOW_METRICS_KEY_PREFIX + timestamp, json)
            }
            
            editor.apply()
            
        } catch (e: Exception) {
            Log.e(TAG, "保存指标到存储失败", e)
        }
    }
    
    /**
     * 从存储加载所有指标
     */
    private fun loadAllMetricsFromStorage() {
        try {
            // 加载总体指标
            val overallJson = sharedPreferences.getString(OVERALL_METRICS_KEY, null)
            overallJson?.let { json ->
                getOverallMetricsAdapter().fromJson(json)?.let { metrics ->
                    overallMetricsCache.set(metrics)
                }
            }
            
            // 加载操作类型指标
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(OPERATION_METRICS_KEY_PREFIX) && value is String) {
                    val operationType = key.removePrefix(OPERATION_METRICS_KEY_PREFIX)
                    getOperationMetricsAdapter().fromJson(value)?.let { metrics ->
                        operationMetricsCache[operationType] = metrics
                    }
                }
            }
            
            // 加载模型指标
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(MODEL_METRICS_KEY_PREFIX) && value is String) {
                    val modelName = key.removePrefix(MODEL_METRICS_KEY_PREFIX)
                    getModelMetricsAdapter().fromJson(value)?.let { metrics ->
                        modelMetricsCache[modelName] = metrics
                    }
                }
            }
            
            // 加载错误指标
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(ERROR_METRICS_KEY_PREFIX) && value is String) {
                    val errorType = key.removePrefix(ERROR_METRICS_KEY_PREFIX)
                    getErrorMetricsAdapter().fromJson(value)?.let { metrics ->
                        errorMetricsCache[errorType] = metrics
                    }
                }
            }
            
            // 加载时间窗口指标
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(TIME_WINDOW_METRICS_KEY_PREFIX) && value is String) {
                    val timestamp = key.removePrefix(TIME_WINDOW_METRICS_KEY_PREFIX).toLongOrNull()
                    if (timestamp != null) {
                        getTimeWindowMetricsAdapter().fromJson(value)?.let { metrics ->
                            timeWindowMetricsCache[timestamp] = metrics
                        }
                    }
                }
            }
            
            Log.i(TAG, "从存储加载指标完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "从存储加载指标失败", e)
        }
    }
    
    // 获取各种类型的JSON适配器
    private fun getOverallMetricsAdapter(): JsonAdapter<OverallMetricsData> {
        return moshi.adapter(OverallMetricsData::class.java)
    }
    
    private fun getOperationMetricsAdapter(): JsonAdapter<OperationMetricsData> {
        return moshi.adapter(OperationMetricsData::class.java)
    }
    
    private fun getModelMetricsAdapter(): JsonAdapter<ModelMetricsData> {
        return moshi.adapter(ModelMetricsData::class.java)
    }
    
    private fun getErrorMetricsAdapter(): JsonAdapter<ErrorMetricsData> {
        return moshi.adapter(ErrorMetricsData::class.java)
    }
    
    private fun getTimeWindowMetricsAdapter(): JsonAdapter<TimeWindowMetricsData> {
        return moshi.adapter(TimeWindowMetricsData::class.java)
    }
    
    private fun getMetricsExportDataAdapter(): JsonAdapter<MetricsExportData> {
        return moshi.adapter(MetricsExportData::class.java)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            // 保存当前指标
            saveAllMetricsToStorage()
            
            // 关闭定期保存任务
            scheduledExecutor.shutdown()
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow()
            }
            
            Log.i(TAG, "MetricsRepository 资源释放完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }
    
    // 数据类定义
    data class OverallMetricsData(
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageParseTimeMs: Long,
        val minParseTimeMs: Long,
        val maxParseTimeMs: Long,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    data class OperationMetricsData(
        val operationType: String,
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageParseTimeMs: Long,
        val averageDataSize: Long,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    data class ModelMetricsData(
        val modelName: String,
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageParseTimeMs: Long,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    data class ErrorMetricsData(
        val errorType: String,
        val count: Long,
        val lastOccurred: Long,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    data class TimeWindowMetricsData(
        val timestamp: Long,
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageParseTimeMs: Long,
        val totalDataSize: Long,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    data class MetricsExportData(
        val overallMetrics: OverallMetricsData?,
        val operationMetrics: Map<String, OperationMetricsData>,
        val modelMetrics: Map<String, ModelMetricsData>,
        val errorMetrics: Map<String, ErrorMetricsData>,
        val timeWindowMetrics: Map<Long, TimeWindowMetricsData>,
        val exportTimestamp: Long
    )
    
    // 用于原子引用的工具类
    private class AtomicReference<T>(initialValue: T? = null) {
        @Volatile
        private var value: T? = initialValue
        
        fun get(): T? = value
        
        fun set(newValue: T?) {
            value = newValue
        }
    }
}