package com.empathy.ai.data.learning

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 自适应映射策略
 * 
 * 负责根据历史数据和上下文动态调整字段映射策略
 * 提供智能的字段映射决策支持
 */
class AdaptiveMappingStrategy {
    
    companion object {
        private const val TAG = "AdaptiveMappingStrategy"
        
        // 策略配置
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.9
        private const val STRATEGY_UPDATE_INTERVAL = 10 * 60 * 1000L // 10分钟
        private const val MAX_STRATEGY_HISTORY = 50
        
        // 映射策略权重
        private const val EXACT_MATCH_WEIGHT = 1.0
        private const val LEARNED_MATCH_WEIGHT = 0.8
        private const val SIMILARITY_MATCH_WEIGHT = 0.6
        private const val CONTEXTUAL_MATCH_WEIGHT = 0.7
    }
    
    // 策略缓存
    private val strategyCache = ConcurrentHashMap<String, MappingStrategy>()
    
    // 策略历史
    private val strategyHistory = ConcurrentHashMap<String, MutableList<StrategyHistoryEntry>>()
    
    // 策略统计
    private val strategyStatistics = ConcurrentHashMap<String, StrategyStatistics>()
    
    // 上下文分析器
    private val contextAnalyzer = MappingContextAnalyzer()
    
    // 策略配置
    private val strategyConfig = AtomicReference(StrategyConfig())
    
    // 策略ID生成器
    private val strategyIdGenerator = AtomicLong(0)
    
    // 上次策略更新时间
    private val lastStrategyUpdate = AtomicLong(0)
    
    /**
     * 获取映射策略
     */
    fun getMappingStrategy(
        originalField: String,
        context: MappingContext,
        availableMappings: List<String>
    ): MappingStrategy {
        val strategyKey = generateStrategyKey(originalField, context)
        
        // 检查缓存
        strategyCache[strategyKey]?.let { cachedStrategy ->
            if (isStrategyValid(cachedStrategy, context)) {
                return cachedStrategy
            }
        }
        
        // 生成新策略
        val newStrategy = generateMappingStrategy(originalField, context, availableMappings)
        
        // 缓存策略
        strategyCache[strategyKey] = newStrategy
        
        // 记录策略历史
        recordStrategyHistory(strategyKey, newStrategy, context)
        
        // 更新策略统计
        updateStrategyStatistics(strategyKey, newStrategy)
        
        // 检查是否需要更新全局策略
        checkAndUpdateGlobalStrategies()
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "生成映射策略: $originalField -> ${newStrategy.targetField}, 置信度=${newStrategy.confidence}")
        }
        
        return newStrategy
    }
    
    /**
     * 记录策略执行结果
     */
    fun recordStrategyExecution(
        strategyKey: String,
        success: Boolean,
        executionTimeMs: Long,
        context: MappingContext
    ) {
        val history = strategyHistory[strategyKey]
        if (history != null) {
            synchronized(history) {
                val latestEntry = history.lastOrNull()
                if (latestEntry != null) {
                    val updatedEntry = latestEntry.copy(
                        successCount = if (success) latestEntry.successCount + 1 else latestEntry.successCount,
                        failureCount = if (!success) latestEntry.failureCount + 1 else latestEntry.failureCount,
                        averageExecutionTimeMs = (latestEntry.averageExecutionTimeMs * latestEntry.executionCount + executionTimeMs) / (latestEntry.executionCount + 1),
                        executionCount = latestEntry.executionCount + 1,
                        lastExecutionTime = System.currentTimeMillis()
                    )
                    
                    history[history.lastIndex] = updatedEntry
                }
            }
        }
        
        // 更新统计
        val stats = strategyStatistics[strategyKey]
        if (stats != null) {
            stats.recordExecution(success, executionTimeMs)
        }
        
        // 检查是否需要调整策略
        if (shouldAdjustStrategy(strategyKey)) {
            adjustStrategy(strategyKey, context)
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "记录策略执行: $strategyKey, 成功=$success, 耗时=${executionTimeMs}ms")
        }
    }
    
    /**
     * 获取策略统计
     */
    fun getStrategyStatistics(strategyKey: String): StrategyStatistics? {
        return strategyStatistics[strategyKey]
    }
    
    /**
     * 获取所有策略统计
     */
    fun getAllStrategyStatistics(): Map<String, StrategyStatistics> {
        return strategyStatistics.toMap()
    }
    
    /**
     * 获取最佳策略
     */
    fun getBestStrategies(limit: Int = 10): List<BestStrategy> {
        val bestStrategies = mutableListOf<BestStrategy>()
        
        strategyStatistics.forEach { (strategyKey, stats) ->
            if (stats.executionCount >= 5) { // 至少执行5次
                val score = calculateStrategyScore(stats)
                bestStrategies.add(BestStrategy(
                    strategyKey = strategyKey,
                    score = score,
                    successRate = stats.successRate,
                    averageExecutionTimeMs = stats.averageExecutionTimeMs,
                    executionCount = stats.executionCount
                ))
            }
        }
        
        return bestStrategies
            .sortedByDescending { it.score }
            .take(limit)
    }
    
    /**
     * 更新策略配置
     */
    fun updateStrategyConfig(config: StrategyConfig) {
        strategyConfig.set(config)
        Log.i(TAG, "更新策略配置: $config")
    }
    
    /**
     * 清理过期策略
     */
    fun cleanupExpiredStrategies() {
        val cutoffTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000L // 24小时
        
        // 清理策略缓存
        strategyCache.entries.removeIf { (_, strategy) ->
            strategy.createdTime < cutoffTime
        }
        
        // 清理策略历史
        strategyHistory.values.forEach { history ->
            synchronized(history) {
                history.removeIf { it.lastExecutionTime < cutoffTime }
            }
        }
        
        // 清理策略统计（保留使用频率高的）
        val sortedStats = strategyStatistics.entries
            .sortedByDescending { it.value.executionCount }
            .toMutableList()
        
        if (sortedStats.size > 100) {
            val toRemove = sortedStats.drop(100)
            toRemove.forEach { (key, _) ->
                strategyStatistics.remove(key)
            }
        }
        
        Log.i(TAG, "清理过期策略完成")
    }
    
    /**
     * 重置所有策略
     */
    fun resetAllStrategies() {
        strategyCache.clear()
        strategyHistory.clear()
        strategyStatistics.clear()
        lastStrategyUpdate.set(0)
        
        Log.i(TAG, "重置所有策略完成")
    }
    
    /**
     * 导出策略数据
     */
    fun exportStrategyData(): StrategyDataExport {
        return StrategyDataExport(
            strategies = strategyCache.toMap(),
            history = strategyHistory.flatMap { it.value },
            statistics = strategyStatistics.toMap(),
            exportTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 导入策略数据
     */
    fun importStrategyData(data: StrategyDataExport) {
        strategyCache.clear()
        strategyCache.putAll(data.strategies)
        
        strategyHistory.clear()
        data.history.forEach { entry ->
            strategyHistory.computeIfAbsent(entry.strategyKey) { mutableListOf() }.add(entry)
        }
        
        strategyStatistics.clear()
        strategyStatistics.putAll(data.statistics)
        
        Log.i(TAG, "导入策略数据完成，策略数量: ${data.strategies.size}")
    }
    
    /**
     * 生成策略键
     */
    private fun generateStrategyKey(originalField: String, context: MappingContext): String {
        val config = strategyConfig.get()
        
        return when (config.strategyKeyType) {
            StrategyKeyType.FIELD_ONLY -> originalField
            StrategyKeyType.CONTEXT_AWARE -> "${context.operationType}_${originalField}"
            StrategyKeyType.MODEL_AWARE -> "${context.modelName}_${originalField}"
            StrategyKeyType.FULL_CONTEXT -> "${context.operationType}_${context.modelName}_${originalField}"
        }
    }
    
    /**
     * 判断策略是否有效
     */
    private fun isStrategyValid(strategy: MappingStrategy, context: MappingContext): Boolean {
        val config = strategyConfig.get()
        
        return strategy.createdTime > System.currentTimeMillis() - config.strategyValidityMs &&
               strategy.isCompatibleWithContext(context)
    }
    
    /**
     * 生成映射策略
     */
    private fun generateMappingStrategy(
        originalField: String,
        context: MappingContext,
        availableMappings: List<String>
    ): MappingStrategy {
        val strategyId = generateStrategyId()
        val candidates = mutableListOf<MappingCandidate>()
        
        // 1. 精确匹配候选
        availableMappings.forEach { mapping ->
            if (mapping.equals(originalField, ignoreCase = true)) {
                candidates.add(MappingCandidate(
                    targetField = mapping,
                    confidence = EXACT_MATCH_WEIGHT,
                    source = "exact_match",
                    reasoning = "精确字段名匹配"
                ))
            }
        }
        
        // 2. 学习映射候选
        val learnedMappings = getLearnedMappings(originalField, context)
        learnedMappings.forEach { (targetField, confidence) ->
            candidates.add(MappingCandidate(
                targetField = targetField,
                confidence = confidence * LEARNED_MATCH_WEIGHT,
                source = "learned_mapping",
                reasoning = "基于历史学习数据"
            ))
        }
        
        // 3. 相似度匹配候选
        val similarityMappings = getSimilarityMappings(originalField, availableMappings)
        similarityMappings.forEach { (targetField, similarity) ->
            candidates.add(MappingCandidate(
                targetField = targetField,
                confidence = similarity * SIMILARITY_MATCH_WEIGHT,
                source = "similarity_match",
                reasoning = "基于字段名相似度"
            ))
        }
        
        // 4. 上下文相关候选
        val contextualMappings = getContextualMappings(originalField, context)
        contextualMappings.forEach { (targetField, confidence) ->
            candidates.add(MappingCandidate(
                targetField = targetField,
                confidence = confidence * CONTEXTUAL_MATCH_WEIGHT,
                source = "contextual_mapping",
                reasoning = "基于上下文相关性"
            ))
        }
        
        // 选择最佳候选
        val bestCandidate = candidates
            .sortedByDescending { it.confidence }
            .firstOrNull()
        
        return if (bestCandidate != null && bestCandidate.confidence >= MIN_CONFIDENCE_THRESHOLD) {
            MappingStrategy(
                strategyId = strategyId,
                originalField = originalField,
                targetField = bestCandidate.targetField,
                confidence = bestCandidate.confidence,
                source = bestCandidate.source,
                reasoning = bestCandidate.reasoning,
                createdTime = System.currentTimeMillis(),
                supportedContexts = determineSupportedContexts(originalField, context),
                priority = calculatePriority(bestCandidate.confidence)
            )
        } else {
            // 生成默认策略
            generateDefaultStrategy(strategyId, originalField, context)
        }
    }
    
    /**
     * 记录策略历史
     */
    private fun recordStrategyHistory(
        strategyKey: String,
        strategy: MappingStrategy,
        context: MappingContext
    ) {
        val history = strategyHistory.computeIfAbsent(strategyKey) { mutableListOf() }
        
        synchronized(history) {
            history.add(StrategyHistoryEntry(
                strategyId = strategy.strategyId,
                strategyKey = strategyKey,
                originalField = strategy.originalField,
                targetField = strategy.targetField,
                confidence = strategy.confidence,
                source = strategy.source,
                context = context,
                createdTime = strategy.createdTime,
                successCount = 0,
                failureCount = 0,
                averageExecutionTimeMs = 0L,
                executionCount = 0,
                lastExecutionTime = 0L
            ))
            
            // 保持历史大小
            while (history.size > MAX_STRATEGY_HISTORY) {
                history.removeAt(0)
            }
        }
    }
    
    /**
     * 更新策略统计
     */
    private fun updateStrategyStatistics(strategyKey: String, strategy: MappingStrategy) {
        val stats = strategyStatistics.computeIfAbsent(strategyKey) {
            StrategyStatistics(strategyKey)
        }
        
        stats.recordStrategyCreation(strategy)
    }
    
    /**
     * 检查是否需要更新全局策略
     */
    private fun checkAndUpdateGlobalStrategies() {
        val now = System.currentTimeMillis()
        if (now - lastStrategyUpdate.get() > STRATEGY_UPDATE_INTERVAL) {
            if (lastStrategyUpdate.compareAndSet(lastStrategyUpdate.get(), now)) {
                updateGlobalStrategies()
            }
        }
    }
    
    /**
     * 更新全局策略
     */
    private fun updateGlobalStrategies() {
        // 分析策略使用情况
        val globalInsights = analyzeGlobalStrategyUsage()
        
        // 更新策略配置
        val config = strategyConfig.get()
        val updatedConfig = config.copy(
            confidenceThreshold = adjustConfidenceThreshold(globalInsights),
            enableContextualMapping = shouldEnableContextualMapping(globalInsights),
            enableSimilarityMatching = shouldEnableSimilarityMatching(globalInsights)
        )
        
        strategyConfig.set(updatedConfig)
        
        Log.i(TAG, "更新全局策略配置: $updatedConfig")
    }
    
    /**
     * 判断是否需要调整策略
     */
    private fun shouldAdjustStrategy(strategyKey: String): Boolean {
        val stats = strategyStatistics[strategyKey] ?: return false
        val config = strategyConfig.get()
        
        return stats.executionCount >= 5 && 
               (stats.successRate < config.minSuccessRate || 
                stats.averageExecutionTimeMs > config.maxExecutionTimeMs)
    }
    
    /**
     * 调整策略
     */
    private fun adjustStrategy(strategyKey: String, context: MappingContext) {
        val history = strategyHistory[strategyKey] ?: return
        val stats = strategyStatistics[strategyKey] ?: return
        
        synchronized(history) {
            val latestEntry = history.lastOrNull() ?: return
            
            // 分析失败原因
            val failureReasons = analyzeFailureReasons(history)
            
            // 生成调整建议
            val adjustments = generateStrategyAdjustments(latestEntry, stats, failureReasons)
            
            // 应用调整
            adjustments.forEach { adjustment ->
                applyStrategyAdjustment(strategyKey, adjustment, context)
            }
        }
    }
    
    // 辅助方法
    private fun generateStrategyId(): String {
        return "strategy_${strategyIdGenerator.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    private fun getLearnedMappings(originalField: String, context: MappingContext): Map<String, Double> {
        // 这里应该从FieldMappingLearningEngine获取学习到的映射
        // 为了简化，返回空映射
        return emptyMap()
    }
    
    private fun getSimilarityMappings(originalField: String, availableMappings: List<String>): Map<String, Double> {
        val similarityMappings = mutableMapOf<String, Double>()
        
        availableMappings.forEach { mapping ->
            val similarity = calculateSimilarity(originalField, mapping)
            if (similarity >= 0.6) {
                similarityMappings[mapping] = similarity
            }
        }
        
        return similarityMappings
    }
    
    private fun getContextualMappings(originalField: String, context: MappingContext): Map<String, Double> {
        // 基于上下文分析生成映射建议
        return contextAnalyzer.analyzeContextualMappings(originalField, context)
    }
    
    private fun determineSupportedContexts(
        originalField: String,
        context: MappingContext
    ): Set<String> {
        val supportedContexts = mutableSetOf<String>()
        
        // 基于历史记录确定支持的上下文
        val history = strategyHistory[generateStrategyKey(originalField, context)]
        if (history != null) {
            synchronized(history) {
                history.forEach { entry ->
                    if (entry.successCount > entry.failureCount) {
                        supportedContexts.add(entry.context.operationType)
                    }
                }
            }
        }
        
        return supportedContexts
    }
    
    private fun calculatePriority(confidence: Double): StrategyPriority {
        return when {
            confidence >= HIGH_CONFIDENCE_THRESHOLD -> StrategyPriority.HIGH
            confidence >= MIN_CONFIDENCE_THRESHOLD -> StrategyPriority.MEDIUM
            else -> StrategyPriority.LOW
        }
    }
    
    private fun generateDefaultStrategy(
        strategyId: String,
        originalField: String,
        context: MappingContext
    ): MappingStrategy {
        return MappingStrategy(
            strategyId = strategyId,
            originalField = originalField,
            targetField = originalField, // 默认不映射
            confidence = 0.3,
            source = "default",
            reasoning = "无合适映射，使用默认策略",
            createdTime = System.currentTimeMillis(),
            supportedContexts = setOf(context.operationType),
            priority = StrategyPriority.LOW
        )
    }
    
    private fun calculateStrategyScore(stats: StrategyStatistics): Double {
        val successRateWeight = 0.5
        val executionTimeWeight = 0.3
        val frequencyWeight = 0.2
        
        val successRateScore = stats.successRate
        val executionTimeScore = 1.0 - (stats.averageExecutionTimeMs / 1000.0).coerceAtMost(1.0)
        val frequencyScore = (stats.executionCount / 100.0).coerceAtMost(1.0)
        
        return successRateScore * successRateWeight +
               executionTimeScore * executionTimeWeight +
               frequencyScore * frequencyWeight
    }
    
    private fun calculateSimilarity(str1: String, str2: String): Double {
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
    
    // 占位方法，实际实现需要更复杂的逻辑
    private fun analyzeGlobalStrategyUsage(): GlobalStrategyInsights {
        return GlobalStrategyInsights(
            averageSuccessRate = 0.85,
            averageExecutionTime = 200L,
            mostUsedStrategies = emptyList(),
            leastUsedStrategies = emptyList()
        )
    }
    
    private fun adjustConfidenceThreshold(insights: GlobalStrategyInsights): Double {
        return when {
            insights.averageSuccessRate > 0.9 -> 0.7
            insights.averageSuccessRate > 0.8 -> 0.6
            else -> 0.5
        }
    }
    
    private fun shouldEnableContextualMapping(insights: GlobalStrategyInsights): Boolean {
        return insights.averageSuccessRate < 0.85
    }
    
    private fun shouldEnableSimilarityMatching(insights: GlobalStrategyInsights): Boolean {
        return insights.averageExecutionTime > 300L
    }
    
    private fun analyzeFailureReasons(history: List<StrategyHistoryEntry>): List<String> {
        // 分析失败原因的占位实现
        return listOf("字段名不匹配", "上下文不兼容", "置信度过低")
    }
    
    private fun generateStrategyAdjustments(
        entry: StrategyHistoryEntry,
        stats: StrategyStatistics,
        failureReasons: List<String>
    ): List<StrategyAdjustment> {
        // 生成策略调整的占位实现
        return listOf(
            StrategyAdjustment(
                type = AdjustmentType.INCREASE_CONFIDENCE,
                description = "提高置信度阈值"
            )
        )
    }
    
    private fun applyStrategyAdjustment(
        strategyKey: String,
        adjustment: StrategyAdjustment,
        context: MappingContext
    ) {
        // 应用策略调整的占位实现
        Log.i(TAG, "应用策略调整: ${adjustment.type} - ${adjustment.description}")
    }
    
    // 数据类定义
    data class MappingStrategy(
        val strategyId: String,
        val originalField: String,
        val targetField: String,
        val confidence: Double,
        val source: String,
        val reasoning: String,
        val createdTime: Long,
        val supportedContexts: Set<String>,
        val priority: StrategyPriority
    ) {
        fun isCompatibleWithContext(context: MappingContext): Boolean {
            return supportedContexts.isEmpty() || context.operationType in supportedContexts
        }
    }
    
    data class MappingCandidate(
        val targetField: String,
        val confidence: Double,
        val source: String,
        val reasoning: String
    )
    
    data class StrategyHistoryEntry(
        val strategyId: String,
        val strategyKey: String,
        val originalField: String,
        val targetField: String,
        val confidence: Double,
        val source: String,
        val context: MappingContext,
        val createdTime: Long,
        var successCount: Int,
        var failureCount: Int,
        var averageExecutionTimeMs: Long,
        var executionCount: Int,
        var lastExecutionTime: Long
    )
    
    class StrategyStatistics(
        private val strategyKey: String
    ) {
        var executionCount: Int = 0
        var successCount: Int = 0
        var failureCount: Int = 0
        var totalExecutionTime: Long = 0L
        var minExecutionTime: Long = Long.MAX_VALUE
        var maxExecutionTime: Long = 0L
        var lastUpdated: Long = System.currentTimeMillis()
        
        val successRate: Double
            get() = if (executionCount > 0) successCount.toDouble() / executionCount else 0.0
        
        val averageExecutionTimeMs: Long
            get() = if (executionCount > 0) totalExecutionTime / executionCount else 0L
        
        fun recordExecution(success: Boolean, executionTimeMs: Long) {
            executionCount++
            if (success) {
                successCount++
            } else {
                failureCount++
            }
            
            totalExecutionTime += executionTimeMs
            minExecutionTime = minOf(minExecutionTime, executionTimeMs)
            maxExecutionTime = maxOf(maxExecutionTime, executionTimeMs)
            lastUpdated = System.currentTimeMillis()
        }
        
        fun recordStrategyCreation(strategy: MappingStrategy) {
            // 记录策略创建时的统计信息
            lastUpdated = System.currentTimeMillis()
        }
    }
    
    data class BestStrategy(
        val strategyKey: String,
        val score: Double,
        val successRate: Double,
        val averageExecutionTimeMs: Long,
        val executionCount: Int
    )
    
    data class StrategyConfig(
        val minSuccessRate: Double = 0.8,
        val maxExecutionTimeMs: Long = 500L,
        val strategyValidityMs: Long = 60 * 60 * 1000L, // 1小时
        val strategyKeyType: StrategyKeyType = StrategyKeyType.CONTEXT_AWARE,
        val enableContextualMapping: Boolean = true,
        val enableSimilarityMatching: Boolean = true
    )
    
    data class GlobalStrategyInsights(
        val averageSuccessRate: Double,
        val averageExecutionTime: Long,
        val mostUsedStrategies: List<String>,
        val leastUsedStrategies: List<String>
    )
    
    data class StrategyAdjustment(
        val type: AdjustmentType,
        val description: String
    )
    
    data class StrategyDataExport(
        val strategies: Map<String, MappingStrategy>,
        val history: List<StrategyHistoryEntry>,
        val statistics: Map<String, StrategyStatistics>,
        val exportTimestamp: Long
    )
    
    enum class StrategyPriority {
        HIGH, MEDIUM, LOW
    }
    
    enum class StrategyKeyType {
        FIELD_ONLY, CONTEXT_AWARE, MODEL_AWARE, FULL_CONTEXT
    }
    
    enum class AdjustmentType {
        INCREASE_CONFIDENCE,
        DECREASE_CONFIDENCE,
        CHANGE_SOURCE_WEIGHT,
        ENABLE_CONTEXTUAL_MAPPING,
        DISABLE_SIMILARITY_MATCHING
    }
    
    data class MappingContext(
        val operationType: String,
        val modelName: String,
        val properties: Map<String, Any> = emptyMap()
    )
    
    /**
     * 映射上下文分析器
     */
    private class MappingContextAnalyzer {
        fun analyzeContextualMappings(originalField: String, context: MappingContext): Map<String, Double> {
            val contextualMappings = mutableMapOf<String, Double>()
            
            // 基于操作类型的上下文映射
            when (context.operationType) {
                "analysis" -> {
                    if (originalField.contains("建议")) {
                        contextualMappings["replySuggestion"] = 0.8
                    }
                    if (originalField.contains("分析")) {
                        contextualMappings["strategyAnalysis"] = 0.8
                    }
                }
                "safety_check" -> {
                    if (originalField.contains("安全")) {
                        contextualMappings["isSafe"] = 0.8
                    }
                    if (originalField.contains("风险")) {
                        contextualMappings["triggeredRisks"] = 0.8
                    }
                }
                "extraction" -> {
                    if (originalField.contains("事实")) {
                        contextualMappings["facts"] = 0.8
                    }
                    if (originalField.contains("标签")) {
                        contextualMappings["redTags"] = 0.7
                        contextualMappings["greenTags"] = 0.7
                    }
                }
            }
            
            return contextualMappings
        }
    }
}