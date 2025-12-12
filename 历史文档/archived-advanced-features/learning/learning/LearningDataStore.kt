package com.empathy.ai.data.learning

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
 * 学习数据存储
 * 
 * 负责持久化和检索学习相关的数据
 * 提供数据的存储、查询和清理功能
 */
class LearningDataStore private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "LearningDataStore"
        private const val PREFS_NAME = "ai_parser_learning"
        
        // 数据键前缀
        private const val FIELD_MAPPING_KEY_PREFIX = "field_mapping_"
        private const val PATTERN_ANALYSIS_KEY_PREFIX = "pattern_analysis_"
        private const val STRATEGY_HISTORY_KEY_PREFIX = "strategy_history_"
        private const val LEARNING_CONFIG_KEY = "learning_config"
        private const val LEARNING_STATS_KEY = "learning_stats"
        
        // 数据保留配置
        private const val MAX_FIELD_MAPPINGS = 200
        private const val MAX_PATTERN_ANALYSES = 100
        private const val MAX_STRATEGY_HISTORIES = 50
        private const val DATA_RETENTION_DAYS = 30
        
        @Volatile
        private var INSTANCE: LearningDataStore? = null
        
        fun getInstance(context: Context): LearningDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LearningDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    // 内存缓存
    private val fieldMappingCache = ConcurrentHashMap<String, FieldMappingData>()
    private val patternAnalysisCache = ConcurrentHashMap<String, PatternAnalysisData>()
    private val strategyHistoryCache = ConcurrentHashMap<String, MutableList<StrategyHistoryEntry>>()
    private val learningConfigCache = AtomicReference<LearningConfig?>()
    private val learningStatsCache = AtomicReference<LearningStats?>()
    
    // 定期保存任务
    private val scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "LearningDataStore-Saver").apply {
            isDaemon = true
        }
    }
    
    init {
        // 启动定期保存任务
        scheduledExecutor.scheduleAtFixedRate({
            try {
                saveAllDataToStorage()
            } catch (e: Exception) {
                Log.e(TAG, "定期保存学习数据失败", e)
            }
        }, 5, 5, TimeUnit.MINUTES)
        
        // 从存储加载数据
        loadAllDataFromStorage()
        
        Log.i(TAG, "LearningDataStore 初始化完成")
    }
    
    /**
     * 保存字段映射数据
     */
    fun saveFieldMapping(key: String, data: FieldMappingData) {
        fieldMappingCache[key] = data
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存字段映射: $key, 目标字段=${data.targetField}, 置信度=${data.confidence}")
        }
    }
    
    /**
     * 获取字段映射数据
     */
    fun getFieldMapping(key: String): FieldMappingData? {
        return fieldMappingCache[key]
    }
    
    /**
     * 获取所有字段映射数据
     */
    fun getAllFieldMappings(): Map<String, FieldMappingData> {
        return fieldMappingCache.toMap()
    }
    
    /**
     * 删除字段映射数据
     */
    fun removeFieldMapping(key: String) {
        fieldMappingCache.remove(key)
        
        Log.i(TAG, "删除字段映射: $key")
    }
    
    /**
     * 保存模式分析数据
     */
    fun savePatternAnalysis(key: String, data: PatternAnalysisData) {
        patternAnalysisCache[key] = data
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存模式分析: $key, 成功率=${data.successRate}")
        }
    }
    
    /**
     * 获取模式分析数据
     */
    fun getPatternAnalysis(key: String): PatternAnalysisData? {
        return patternAnalysisCache[key]
    }
    
    /**
     * 获取所有模式分析数据
     */
    fun getAllPatternAnalyses(): Map<String, PatternAnalysisData> {
        return patternAnalysisCache.toMap()
    }
    
    /**
     * 删除模式分析数据
     */
    fun removePatternAnalysis(key: String) {
        patternAnalysisCache.remove(key)
        
        Log.i(TAG, "删除模式分析: $key")
    }
    
    /**
     * 保存策略历史记录
     */
    fun saveStrategyHistory(key: String, entry: StrategyHistoryEntry) {
        val history = strategyHistoryCache.computeIfAbsent(key) { mutableListOf() }
        
        synchronized(history) {
            history.add(entry)
            
            // 保持历史大小
            while (history.size > MAX_STRATEGY_HISTORIES) {
                history.removeAt(0)
            }
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "保存策略历史: $key, 成功=${entry.success}")
        }
    }
    
    /**
     * 获取策略历史记录
     */
    fun getStrategyHistory(key: String): List<StrategyHistoryEntry> {
        return strategyHistoryCache[key]?.toList() ?: emptyList()
    }
    
    /**
     * 获取所有策略历史记录
     */
    fun getAllStrategyHistories(): Map<String, List<StrategyHistoryEntry>> {
        return strategyHistoryCache.mapValues { it.toList() }.toMap()
    }
    
    /**
     * 删除策略历史记录
     */
    fun removeStrategyHistory(key: String) {
        strategyHistoryCache.remove(key)
        
        Log.i(TAG, "删除策略历史: $key")
    }
    
    /**
     * 保存学习配置
     */
    fun saveLearningConfig(config: LearningConfig) {
        learningConfigCache.set(config)
        
        Log.i(TAG, "保存学习配置: $config")
    }
    
    /**
     * 获取学习配置
     */
    fun getLearningConfig(): LearningConfig? {
        return learningConfigCache.get()
    }
    
    /**
     * 保存学习统计
     */
    fun saveLearningStats(stats: LearningStats) {
        learningStatsCache.set(stats)
        
        Log.i(TAG, "保存学习统计: 总映射=${stats.totalFieldMappings}, 成功率=${stats.overallSuccessRate}")
    }
    
    /**
     * 获取学习统计
     */
    fun getLearningStats(): LearningStats? {
        return learningStatsCache.get()
    }
    
    /**
     * 清理过期数据
     */
    fun cleanupExpiredData() {
        val cutoffTime = System.currentTimeMillis() - (DATA_RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        
        // 清理字段映射
        fieldMappingCache.entries.removeIf { (_, data) ->
            data.lastUsed < cutoffTime
        }
        
        // 限制字段映射数量
        if (fieldMappingCache.size > MAX_FIELD_MAPPINGS) {
            val sortedMappings = fieldMappingCache.entries
                .sortedByDescending { it.value.lastUsed }
                .toMutableList()
            
            val toRemove = sortedMappings.drop(MAX_FIELD_MAPPINGS)
            toRemove.forEach { (key, _) ->
                fieldMappingCache.remove(key)
            }
        }
        
        // 清理模式分析
        patternAnalysisCache.entries.removeIf { (_, data) ->
            data.analysisTimestamp < cutoffTime
        }
        
        // 限制模式分析数量
        if (patternAnalysisCache.size > MAX_PATTERN_ANALYSES) {
            val sortedAnalyses = patternAnalysisCache.entries
                .sortedByDescending { it.value.analysisTimestamp }
                .toMutableList()
            
            val toRemove = sortedAnalyses.drop(MAX_PATTERN_ANALYSES)
            toRemove.forEach { (key, _) ->
                patternAnalysisCache.remove(key)
            }
        }
        
        // 清理策略历史
        strategyHistoryCache.values.forEach { history ->
            synchronized(history) {
                history.removeIf { it.timestamp < cutoffTime }
            }
        }
        
        Log.i(TAG, "清理过期学习数据完成")
    }
    
    /**
     * 重置所有学习数据
     */
    fun resetAllLearningData() {
        fieldMappingCache.clear()
        patternAnalysisCache.clear()
        strategyHistoryCache.clear()
        learningConfigCache.set(null)
        learningStatsCache.set(null)
        
        // 清除持久化存储
        sharedPreferences.edit().clear().apply()
        
        Log.i(TAG, "重置所有学习数据完成")
    }
    
    /**
     * 导出学习数据
     */
    fun exportLearningData(): LearningDataExport {
        return LearningDataExport(
            fieldMappings = fieldMappingCache.toMap(),
            patternAnalyses = patternAnalysisCache.toMap(),
            strategyHistories = strategyHistoryCache.mapValues { it.toList() }.toMap(),
            learningConfig = learningConfigCache.get(),
            learningStats = learningStatsCache.get(),
            exportTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 导入学习数据
     */
    fun importLearningData(data: LearningDataExport) {
        fieldMappingCache.clear()
        fieldMappingCache.putAll(data.fieldMappings)
        
        patternAnalysisCache.clear()
        patternAnalysisCache.putAll(data.patternAnalyses)
        
        strategyHistoryCache.clear()
        data.strategyHistories.forEach { (key, history) ->
            strategyHistoryCache[key] = history.toMutableList()
        }
        
        learningConfigCache.set(data.learningConfig)
        learningStatsCache.set(data.learningStats)
        
        Log.i(TAG, "导入学习数据完成，字段映射数量: ${data.fieldMappings.size}")
    }
    
    /**
     * 保存所有数据到存储
     */
    private fun saveAllDataToStorage() {
        try {
            val editor = sharedPreferences.edit()
            
            // 保存字段映射
            fieldMappingCache.forEach { (key, data) ->
                val json = getFieldMappingAdapter().toJson(data)
                editor.putString(FIELD_MAPPING_KEY_PREFIX + key, json)
            }
            
            // 保存模式分析
            patternAnalysisCache.forEach { (key, data) ->
                val json = getPatternAnalysisAdapter().toJson(data)
                editor.putString(PATTERN_ANALYSIS_KEY_PREFIX + key, json)
            }
            
            // 保存策略历史（只保存最近的）
            strategyHistoryCache.forEach { (key, history) ->
                synchronized(history) {
                    val recentHistory = history.takeLast(20) // 只保存最近20条记录
                    val json = getStrategyHistoryListAdapter().toJson(recentHistory)
                    editor.putString(STRATEGY_HISTORY_KEY_PREFIX + key, json)
                }
            }
            
            // 保存学习配置
            learningConfigCache.get()?.let { config ->
                val json = getLearningConfigAdapter().toJson(config)
                editor.putString(LEARNING_CONFIG_KEY, json)
            }
            
            // 保存学习统计
            learningStatsCache.get()?.let { stats ->
                val json = getLearningStatsAdapter().toJson(stats)
                editor.putString(LEARNING_STATS_KEY, json)
            }
            
            editor.apply()
            
        } catch (e: Exception) {
            Log.e(TAG, "保存学习数据到存储失败", e)
        }
    }
    
    /**
     * 从存储加载所有数据
     */
    private fun loadAllDataFromStorage() {
        try {
            // 加载字段映射
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(FIELD_MAPPING_KEY_PREFIX) && value is String) {
                    val mappingKey = key.removePrefix(FIELD_MAPPING_KEY_PREFIX)
                    getFieldMappingAdapter().fromJson(value)?.let { data ->
                        fieldMappingCache[mappingKey] = data
                    }
                }
            }
            
            // 加载模式分析
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(PATTERN_ANALYSIS_KEY_PREFIX) && value is String) {
                    val analysisKey = key.removePrefix(PATTERN_ANALYSIS_KEY_PREFIX)
                    getPatternAnalysisAdapter().fromJson(value)?.let { data ->
                        patternAnalysisCache[analysisKey] = data
                    }
                }
            }
            
            // 加载策略历史
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(STRATEGY_HISTORY_KEY_PREFIX) && value is String) {
                    val historyKey = key.removePrefix(STRATEGY_HISTORY_KEY_PREFIX)
                    getStrategyHistoryListAdapter().fromJson(value)?.let { history ->
                        strategyHistoryCache[historyKey] = history.toMutableList()
                    }
                }
            }
            
            // 加载学习配置
            val configJson = sharedPreferences.getString(LEARNING_CONFIG_KEY, null)
            configJson?.let { json ->
                getLearningConfigAdapter().fromJson(json)?.let { config ->
                    learningConfigCache.set(config)
                }
            }
            
            // 加载学习统计
            val statsJson = sharedPreferences.getString(LEARNING_STATS_KEY, null)
            statsJson?.let { json ->
                getLearningStatsAdapter().fromJson(json)?.let { stats ->
                    learningStatsCache.set(stats)
                }
            }
            
            Log.i(TAG, "从存储加载学习数据完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "从存储加载学习数据失败", e)
        }
    }
    
    // 获取各种类型的JSON适配器
    private fun getFieldMappingAdapter(): JsonAdapter<FieldMappingData> {
        return moshi.adapter(FieldMappingData::class.java)
    }
    
    private fun getPatternAnalysisAdapter(): JsonAdapter<PatternAnalysisData> {
        return moshi.adapter(PatternAnalysisData::class.java)
    }
    
    private fun getStrategyHistoryListAdapter(): JsonAdapter<List<StrategyHistoryEntry>> {
        return moshi.adapter(Types.newParameterizedType(
            List::class.java,
            StrategyHistoryEntry::class.java
        ))
    }
    
    private fun getLearningConfigAdapter(): JsonAdapter<LearningConfig> {
        return moshi.adapter(LearningConfig::class.java)
    }
    
    private fun getLearningStatsAdapter(): JsonAdapter<LearningStats> {
        return moshi.adapter(LearningStats::class.java)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            // 保存当前数据
            saveAllDataToStorage()
            
            // 关闭定期保存任务
            scheduledExecutor.shutdown()
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow()
            }
            
            Log.i(TAG, "LearningDataStore 资源释放完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }
    
    // 数据类定义
    data class FieldMappingData(
        val originalField: String,
        val targetField: String,
        val confidence: Double,
        val usageCount: Int,
        val successCount: Int,
        val lastUsed: Long,
        val source: String,
        val supportedContexts: Set<String>
    )
    
    data class PatternAnalysisData(
        val patternKey: String,
        val totalEvents: Int,
        val successRate: Double,
        val averageDurationMs: Long,
        val errorPatterns: Map<String, Int>,
        val recommendations: List<String>,
        val analysisTimestamp: Long
    )
    
    data class StrategyHistoryEntry(
        val strategyId: String,
        val strategyKey: String,
        val originalField: String,
        val targetField: String,
        val confidence: Double,
        val success: Boolean,
        val executionTimeMs: Long,
        val context: Map<String, String>,
        val timestamp: Long
    )
    
    data class LearningConfig(
        val enableFieldLearning: Boolean = true,
        val enablePatternAnalysis: Boolean = true,
        val enableStrategyOptimization: Boolean = true,
        val minConfidenceThreshold: Double = 0.7,
        val maxLearningEntries: Int = 100,
        val dataRetentionDays: Int = 30,
        val autoCleanupEnabled: Boolean = true
    )
    
    data class LearningStats(
        val totalFieldMappings: Int,
        val confidentMappings: Int,
        val totalPatternAnalyses: Int,
        val successfulStrategies: Int,
        val totalStrategyExecutions: Int,
        val overallSuccessRate: Double,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    data class LearningDataExport(
        val fieldMappings: Map<String, FieldMappingData>,
        val patternAnalyses: Map<String, PatternAnalysisData>,
        val strategyHistories: Map<String, List<StrategyHistoryEntry>>,
        val learningConfig: LearningConfig?,
        val learningStats: LearningStats?,
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