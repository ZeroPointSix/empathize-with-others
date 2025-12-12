package com.empathy.ai.data.learning

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 字段映射学习引擎
 * 
 * 负责从解析过程中学习新的字段映射关系
 * 基于使用频率和成功率动态优化映射策略
 */
class FieldMappingLearningEngine {
    
    companion object {
        private const val TAG = "FieldMappingLearningEngine"
        
        // 学习配置
        private const val MIN_CONFIDENCE_THRESHOLD = 0.7
        private const val MIN_USAGE_COUNT = 5
        private const val MAX_LEARNED_MAPPINGS = 100
        private const val LEARNING_WINDOW_SIZE = 1000
        
        // 相似度阈值
        private const val SIMILARITY_THRESHOLD = 0.8
        
        // 学习数据保留时间（毫秒）
        private const val LEARNING_DATA_RETENTION = 7 * 24 * 60 * 60 * 1000L // 7天
    }
    
    // 学习到的映射关系
    private val learnedMappings = ConcurrentHashMap<String, LearnedMapping>()
    
    // 映射使用统计
    private val mappingUsageStats = ConcurrentHashMap<String, MappingUsageStats>()
    
    // 学习历史
    private val learningHistory = ConcurrentHashMap<String, MutableList<LearningEvent>>()
    
    // 相似度计算器
    private val similarityCalculator = SimilarityCalculator()
    
    // 学习配置
    private val learningConfig = AtomicReference(LearningConfig())
    
    /**
     * 记录字段映射使用
     */
    fun recordMappingUsage(
        originalField: String,
        mappedField: String,
        context: MappingContext,
        success: Boolean
    ) {
        val mappingKey = "$originalField->$mappedField"
        val timestamp = System.currentTimeMillis()
        
        // 更新使用统计
        val usageStats = mappingUsageStats.computeIfAbsent(mappingKey) {
            MappingUsageStats(originalField, mappedField)
        }
        usageStats.recordUsage(success, timestamp)
        
        // 记录学习事件
        val learningEvent = LearningEvent(
            originalField = originalField,
            mappedField = mappedField,
            context = context,
            success = success,
            timestamp = timestamp
        )
        
        learningHistory.computeIfAbsent(originalField) { mutableListOf() }.add(learningEvent)
        
        // 保持历史大小
        val history = learningHistory[originalField]
        if (history != null && history.size > LEARNING_WINDOW_SIZE) {
            history.removeAt(0)
        }
        
        // 检查是否需要学习新的映射
        if (shouldLearnNewMapping(originalField, mappedField)) {
            learnNewMapping(originalField, mappedField, context)
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "记录映射使用: $originalField -> $mappedField, 成功=$success")
        }
    }
    
    /**
     * 获取学习到的映射建议
     */
    fun getMappingSuggestions(field: String, context: MappingContext): List<MappingSuggestion> {
        val suggestions = mutableListOf<MappingSuggestion>()
        
        // 1. 查找精确匹配的学习映射
        learnedMappings[field]?.let { mapping ->
            if (mapping.isConfident() && mapping.isValidForContext(context)) {
                suggestions.add(MappingSuggestion(
                    targetField = mapping.mappedField,
                    confidence = mapping.confidence,
                    source = "learned_exact",
                    usageCount = mapping.usageCount
                ))
            }
        }
        
        // 2. 查找相似字段的学习映射
        val similarMappings = findSimilarMappings(field, context)
        similarMappings.forEach { (similarField, mapping) ->
            if (mapping.isConfident() && mapping.isValidForContext(context)) {
                val similarity = similarityCalculator.calculateSimilarity(field, similarField)
                suggestions.add(MappingSuggestion(
                    targetField = mapping.mappedField,
                    confidence = mapping.confidence * similarity,
                    source = "learned_similar",
                    usageCount = mapping.usageCount,
                    similarity = similarity
                ))
            }
        }
        
        // 3. 基于使用统计生成建议
        val usageBasedSuggestions = generateUsageBasedSuggestions(field, context)
        suggestions.addAll(usageBasedSuggestions)
        
        // 4. 排序并返回最相关的建议
        return suggestions
            .sortedByDescending { it.confidence }
            .take(learningConfig.get().maxSuggestions)
    }
    
    /**
     * 获取所有学习到的映射
     */
    fun getAllLearnedMappings(): Map<String, LearnedMapping> {
        return learnedMappings.toMap()
    }
    
    /**
     * 添加手动映射
     */
    fun addManualMapping(
        originalField: String,
        mappedField: String,
        context: MappingContext,
        confidence: Double = 1.0
    ) {
        val mapping = LearnedMapping(
            originalField = originalField,
            mappedField = mappedField,
            confidence = confidence,
            usageCount = 0,
            successCount = 0,
            lastUsed = System.currentTimeMillis(),
            source = MappingSource.MANUAL,
            supportedContexts = setOf(context.operationType)
        )
        
        learnedMappings[originalField] = mapping
        
        Log.i(TAG, "添加手动映射: $originalField -> $mappedField, 置信度=$confidence")
    }
    
    /**
     * 移除学习到的映射
     */
    fun removeLearnedMapping(originalField: String) {
        learnedMappings.remove(originalField)
        mappingUsageStats.remove(originalField)
        learningHistory.remove(originalField)
        
        Log.i(TAG, "移除学习映射: $originalField")
    }
    
    /**
     * 更新学习配置
     */
    fun updateLearningConfig(config: LearningConfig) {
        learningConfig.set(config)
        Log.i(TAG, "更新学习配置: $config")
    }
    
    /**
     * 获取学习统计
     */
    fun getLearningStatistics(): LearningStatistics {
        val totalMappings = learnedMappings.size
        val confidentMappings = learnedMappings.values.count { it.isConfident() }
        val manualMappings = learnedMappings.values.count { it.source == MappingSource.MANUAL }
        val autoMappings = totalMappings - manualMappings
        
        val usageStats = mappingUsageStats.values
        val totalUsage = usageStats.sumOf { it.usageCount }
        val totalSuccess = usageStats.sumOf { it.successCount }
        val overallSuccessRate = if (totalUsage > 0) totalSuccess.toDouble() / totalUsage else 0.0
        
        val contextDistribution = learnedMappings.values
            .flatMap { it.supportedContexts }
            .groupingBy { it }
            .mapValues { it.value.size }
        
        return LearningStatistics(
            totalMappings = totalMappings,
            confidentMappings = confidentMappings,
            manualMappings = manualMappings,
            autoMappings = autoMappings,
            totalUsage = totalUsage,
            overallSuccessRate = overallSuccessRate,
            contextDistribution = contextDistribution
        )
    }
    
    /**
     * 清理过期的学习数据
     */
    fun cleanupExpiredLearningData() {
        val cutoffTime = System.currentTimeMillis() - LEARNING_DATA_RETENTION
        
        // 清理学习历史
        learningHistory.values.forEach { history ->
            history.removeIf { it.timestamp < cutoffTime }
        }
        
        // 清理使用统计
        mappingUsageStats.entries.removeIf { (_, stats) ->
            stats.lastUsed < cutoffTime
        }
        
        // 清理学习映射（保留使用频率高的）
        val sortedMappings = learnedMappings.entries
            .sortedByDescending { it.value.usageCount }
            .toMutableList()
        
        if (sortedMappings.size > MAX_LEARNED_MAPPINGS) {
            val toRemove = sortedMappings.drop(MAX_LEARNED_MAPPINGS)
            toRemove.forEach { (field, _) ->
                learnedMappings.remove(field)
            }
        }
        
        Log.i(TAG, "清理过期学习数据完成")
    }
    
    /**
     * 重置学习数据
     */
    fun resetLearningData() {
        learnedMappings.clear()
        mappingUsageStats.clear()
        learningHistory.clear()
        
        Log.i(TAG, "重置学习数据完成")
    }
    
    /**
     * 导出学习数据
     */
    fun exportLearningData(): LearningDataExport {
        return LearningDataExport(
            mappings = learnedMappings.toMap(),
            usageStats = mappingUsageStats.toMap(),
            history = learningHistory.flatMap { it.value },
            exportTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 导入学习数据
     */
    fun importLearningData(data: LearningDataExport) {
        learnedMappings.clear()
        learnedMappings.putAll(data.mappings)
        
        mappingUsageStats.clear()
        mappingUsageStats.putAll(data.usageStats)
        
        learningHistory.clear()
        data.history.forEach { event ->
            learningHistory.computeIfAbsent(event.originalField) { mutableListOf() }.add(event)
        }
        
        Log.i(TAG, "导入学习数据完成，映射数量: ${data.mappings.size}")
    }
    
    /**
     * 判断是否应该学习新的映射
     */
    private fun shouldLearnNewMapping(originalField: String, mappedField: String): Boolean {
        val usageStats = mappingUsageStats["$originalField->$mappedField"]
        if (usageStats == null) {
            return false
        }
        
        val config = learningConfig.get()
        
        return usageStats.usageCount >= config.minUsageCount &&
               usageStats.successRate >= config.minConfidenceThreshold &&
               !learnedMappings.containsKey(originalField)
    }
    
    /**
     * 学习新的映射
     */
    private fun learnNewMapping(
        originalField: String,
        mappedField: String,
        context: MappingContext
    ) {
        val usageStats = mappingUsageStats["$originalField->$mappedField"] ?: return
        
        val mapping = LearnedMapping(
            originalField = originalField,
            mappedField = mappedField,
            confidence = usageStats.successRate,
            usageCount = usageStats.usageCount,
            successCount = usageStats.successCount,
            lastUsed = usageStats.lastUsed,
            source = MappingSource.LEARNED,
            supportedContexts = determineSupportedContexts(originalField, context)
        )
        
        learnedMappings[originalField] = mapping
        
        Log.i(TAG, "学习新映射: $originalField -> $mappedField, 置信度=${mapping.confidence}")
    }
    
    /**
     * 查找相似映射
     */
    private fun findSimilarMappings(
        field: String,
        context: MappingContext
    ): Map<String, LearnedMapping> {
        val similarMappings = mutableMapOf<String, LearnedMapping>()
        
        learnedMappings.forEach { (originalField, mapping) ->
            if (originalField != field && mapping.isValidForContext(context)) {
                val similarity = similarityCalculator.calculateSimilarity(field, originalField)
                if (similarity >= SIMILARITY_THRESHOLD) {
                    similarMappings[originalField] = mapping
                }
            }
        }
        
        return similarMappings
    }
    
    /**
     * 基于使用统计生成建议
     */
    private fun generateUsageBasedSuggestions(
        field: String,
        context: MappingContext
    ): List<MappingSuggestion> {
        val suggestions = mutableListOf<MappingSuggestion>()
        
        mappingUsageStats.forEach { (mappingKey, stats) ->
            val (originalField, mappedField) = mappingKey.split("->", limit = 2)
            
            if (originalField != field && stats.isReliable()) {
                val similarity = similarityCalculator.calculateSimilarity(field, originalField)
                if (similarity >= SIMILARITY_THRESHOLD) {
                    suggestions.add(MappingSuggestion(
                        targetField = mappedField,
                        confidence = stats.successRate * similarity * 0.8, // 降低权重
                        source = "usage_based",
                        usageCount = stats.usageCount,
                        similarity = similarity
                    ))
                }
            }
        }
        
        return suggestions
    }
    
    /**
     * 确定支持的上下文
     */
    private fun determineSupportedContexts(
        originalField: String,
        context: MappingContext
    ): Set<String> {
        val supportedContexts = mutableSetOf<String>()
        
        // 基于历史记录确定支持的上下文
        learningHistory[originalField]?.forEach { event ->
            if (event.success) {
                supportedContexts.add(event.context.operationType)
            }
        }
        
        // 如果没有历史记录，使用当前上下文
        if (supportedContexts.isEmpty()) {
            supportedContexts.add(context.operationType)
        }
        
        return supportedContexts
    }
    
    /**
     * 映射上下文
     */
    data class MappingContext(
        val operationType: String,
        val modelName: String,
        val properties: Map<String, Any> = emptyMap()
    )
    
    /**
     * 学习到的映射
     */
    data class LearnedMapping(
        val originalField: String,
        val mappedField: String,
        val confidence: Double,
        val usageCount: Int,
        val successCount: Int,
        val lastUsed: Long,
        val source: MappingSource,
        val supportedContexts: Set<String>
    ) {
        fun isConfident(): Boolean {
            return confidence >= MIN_CONFIDENCE_THRESHOLD && usageCount >= MIN_USAGE_COUNT
        }
        
        fun isValidForContext(context: MappingContext): Boolean {
            return supportedContexts.isEmpty() || context.operationType in supportedContexts
        }
    }
    
    /**
     * 映射使用统计
     */
    class MappingUsageStats(
        val originalField: String,
        val mappedField: String
    ) {
        var usageCount: Int = 0
            private set
        var successCount: Int = 0
            private set
        var lastUsed: Long = 0
            private set
        
        val successRate: Double
            get() = if (usageCount > 0) successCount.toDouble() / usageCount else 0.0
        
        fun recordUsage(success: Boolean, timestamp: Long) {
            usageCount++
            if (success) {
                successCount++
            }
            lastUsed = timestamp
        }
        
        fun isReliable(): Boolean {
            return usageCount >= MIN_USAGE_COUNT && successRate >= MIN_CONFIDENCE_THRESHOLD
        }
    }
    
    /**
     * 学习事件
     */
    data class LearningEvent(
        val originalField: String,
        val mappedField: String,
        val context: MappingContext,
        val success: Boolean,
        val timestamp: Long
    )
    
    /**
     * 映射建议
     */
    data class MappingSuggestion(
        val targetField: String,
        val confidence: Double,
        val source: String,
        val usageCount: Int,
        val similarity: Double? = null
    )
    
    /**
     * 学习配置
     */
    data class LearningConfig(
        val minConfidenceThreshold: Double = MIN_CONFIDENCE_THRESHOLD,
        val minUsageCount: Int = MIN_USAGE_COUNT,
        val maxSuggestions: Int = 5,
        val enableSimilarityMatching: Boolean = true,
        val enableUsageBasedSuggestions: Boolean = true
    )
    
    /**
     * 学习统计
     */
    data class LearningStatistics(
        val totalMappings: Int,
        val confidentMappings: Int,
        val manualMappings: Int,
        val autoMappings: Int,
        val totalUsage: Long,
        val overallSuccessRate: Double,
        val contextDistribution: Map<String, Int>
    )
    
    /**
     * 学习数据导出
     */
    data class LearningDataExport(
        val mappings: Map<String, LearnedMapping>,
        val usageStats: Map<String, MappingUsageStats>,
        val history: List<LearningEvent>,
        val exportTimestamp: Long
    )
    
    /**
     * 映射来源
     */
    enum class MappingSource {
        MANUAL,   // 手动添加
        LEARNED,  // 自动学习
        DEFAULT    // 默认映射
    }
    
    /**
     * 相似度计算器
     */
    private class SimilarityCalculator {
        fun calculateSimilarity(str1: String, str2: String): Double {
            // 使用编辑距离计算相似度
            val maxLength = maxOf(str1.length, str2.length)
            if (maxLength == 0) return 1.0
            
            val editDistance = calculateEditDistance(str1.lowercase(), str2.lowercase())
            return 1.0 - (editDistance.toDouble() / maxLength)
        }
        
        private fun calculateEditDistance(str1: String, str2: String): Int {
            val len1 = str1.length
            val len2 = str2.length
            
            val dp = Array(len1 + 1) { IntArray(len2 + 1) }
            
            for (i in 0..len1) dp[i][0] = i
            for (j in 0..len2) dp[0][j] = j
            
            for (i in 1..len1) {
                for (j in 1..len2) {
                    val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                }
            }
            
            return dp[len1][len2]
        }
    }
}