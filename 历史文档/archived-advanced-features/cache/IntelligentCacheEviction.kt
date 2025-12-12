package com.empathy.ai.data.cache

import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 智能缓存淘汰器
 * 基于多种策略和机器学习的智能缓存淘汰算法
 */
class IntelligentCacheEviction private constructor(
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: IntelligentCacheEviction? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): IntelligentCacheEviction {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IntelligentCacheEviction(metrics).also { INSTANCE = it }
            }
        }
        
        private const val ANALYSIS_INTERVAL = 60000L // 1分钟
        private const val LEARNING_WINDOW = 3600000L // 1小时
        private const val MIN_SAMPLES_FOR_PREDICTION = 100
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 淘汰策略注册表
    private val evictionStrategies = ConcurrentHashMap<String, EvictionStrategy>()
    
    // 访问历史
    private val accessHistory = ConcurrentHashMap<String, MutableList<AccessRecord>>()
    
    // 淘汰统计
    private val evictionStats = AtomicReference(EvictionStatistics())
    
    // 淘汰状态
    private val _evictionState = MutableStateFlow(EvictionState())
    val evictionState: StateFlow<EvictionState> = _evictionState.asStateFlow()
    
    // 机器学习模型
    private val mlModel = AtomicReference<AccessPredictionModel?>(null)
    
    // 分析任务
    private var analysisJob: Job? = null
    
    init {
        initializeEvictionStrategies()
        startAnalysisTask()
    }
    
    /**
     * 初始化淘汰策略
     */
    private fun initializeEvictionStrategies() {
        // LRU策略
        evictionStrategies["lru"] = object : EvictionStrategy {
            override val name = "LRU"
            override val priority = 1
            
            override fun selectCandidates(
                entries: Map<String, CacheEntryInfo>,
                count: Int
            ): List<String> {
                return entries.entries
                    .sortedBy { it.value.lastAccessTime }
                    .take(count)
                    .map { it.key }
            }
        }
        
        // LFU策略
        evictionStrategies["lfu"] = object : EvictionStrategy {
            override val name = "LFU"
            override val priority = 2
            
            override fun selectCandidates(
                entries: Map<String, CacheEntryInfo>,
                count: Int
            ): List<String> {
                return entries.entries
                    .sortedBy { it.value.accessCount }
                    .take(count)
                    .map { it.key }
            }
        }
        
        // 大小优先策略
        evictionStrategies["size"] = object : EvictionStrategy {
            override val name = "Size"
            override val priority = 3
            
            override fun selectCandidates(
                entries: Map<String, CacheEntryInfo>,
                count: Int
            ): List<String> {
                return entries.entries
                    .sortedByDescending { it.value.size }
                    .take(count)
                    .map { it.key }
            }
        }
        
        // 过期时间优先策略
        evictionStrategies["expire_time"] = object : EvictionStrategy {
            override val name = "ExpireTime"
            override val priority = 4
            
            override fun selectCandidates(
                entries: Map<String, CacheEntryInfo>,
                count: Int
            ): List<String> {
                val currentTime = System.currentTimeMillis()
                return entries.entries
                    .filter { it.value.expireTime > 0 }
                    .sortedBy { it.value.expireTime }
                    .take(count)
                    .map { it.key }
            }
        }
        
        // 成本效益策略
        evictionStrategies["cost_benefit"] = object : EvictionStrategy {
            override val name = "CostBenefit"
            override val priority = 5
            
            override fun selectCandidates(
                entries: Map<String, CacheEntryInfo>,
                count: Int
            ): List<String> {
                return entries.entries
                    .sortedBy { calculateCostBenefit(it.value) }
                    .take(count)
                    .map { it.key }
            }
            
            private fun calculateCostBenefit(entry: CacheEntryInfo): Float {
                val currentTime = System.currentTimeMillis()
                val age = currentTime - entry.createTime
                val accessFrequency = if (age > 0) entry.accessCount.toFloat() / age.toFloat() else 0f
                val sizeCost = entry.size.toFloat()
                
                // 成本效益 = 访问频率 / 大小成本
                return if (sizeCost > 0) accessFrequency / sizeCost else 0f
            }
        }
        
        // 机器学习预测策略
        evictionStrategies["ml_prediction"] = object : EvictionStrategy {
            override val name = "MLPrediction"
            override val priority = 6
            
            override fun selectCandidates(
                entries: Map<String, CacheEntryInfo>,
                count: Int
            ): List<String> {
                val model = mlModel.get()
                if (model == null || !model.isTrained()) {
                    // 回退到LRU策略
                    return evictionStrategies["lru"]?.selectCandidates(entries, count) ?: emptyList()
                }
                
                return entries.entries
                    .sortedBy { model.predictAccessProbability(it.key) }
                    .take(count)
                    .map { it.key }
            }
        }
    }
    
    /**
     * 启动分析任务
     */
    private fun startAnalysisTask() {
        analysisJob = scope.launch {
            while (isActive) {
                try {
                    analyzeAccessPatterns()
                    updateEvictionState()
                    delay(ANALYSIS_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordCacheEvictionError("智能淘汰分析错误", e)
                }
            }
        }
    }
    
    /**
     * 记录访问
     */
    fun recordAccess(key: String, entryInfo: CacheEntryInfo) {
        val currentTime = System.currentTimeMillis()
        val record = AccessRecord(
            timestamp = currentTime,
            key = key,
            accessType = AccessType.READ
        )
        
        val history = accessHistory.getOrPut(key) { mutableListOf() }
        history.add(record)
        
        // 清理过期的访问记录
        cleanupOldAccessRecords(key, currentTime)
        
        metrics.recordCacheAccess(key, currentTime)
    }
    
    /**
     * 清理过期的访问记录
     */
    private fun cleanupOldAccessRecords(key: String, currentTime: Long) {
        val history = accessHistory[key] ?: return
        val cutoffTime = currentTime - LEARNING_WINDOW
        
        history.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * 分析访问模式
     */
    private fun analyzeAccessPatterns() {
        val currentTime = System.currentTimeMillis()
        val allAccessRecords = mutableListOf<AccessRecord>()
        
        for (history in accessHistory.values) {
            allAccessRecords.addAll(history)
        }
        
        if (allAccessRecords.size >= MIN_SAMPLES_FOR_PREDICTION) {
            // 训练机器学习模型
            val model = trainAccessPredictionModel(allAccessRecords)
            mlModel.set(model)
            
            metrics.recordCacheEvictionMlModelUpdate(allAccessRecords.size)
        }
    }
    
    /**
     * 训练访问预测模型
     */
    private fun trainAccessPredictionModel(records: List<AccessRecord>): AccessPredictionModel {
        // 简化的机器学习模型实现
        // 实际应用中可以使用更复杂的算法
        
        val accessFrequency = ConcurrentHashMap<String, Int>()
        val timePatterns = ConcurrentHashMap<String, MutableList<Long>>()
        
        for (record in records) {
            accessFrequency.merge(record.key, 1) { a, b -> a + b }
            
            val timePattern = timePatterns.getOrPut(record.key) { mutableListOf() }
            timePattern.add(record.timestamp)
        }
        
        return SimpleAccessPredictionModel(accessFrequency, timePatterns)
    }
    
    /**
     * 智能选择淘汰候选
     */
    fun selectEvictionCandidates(
        entries: Map<String, CacheEntryInfo>,
        count: Int,
        strategy: String = "ml_prediction"
    ): List<String> {
        val evictionStrategy = evictionStrategies[strategy]
            ?: evictionStrategies["lru"]!!
        
        val candidates = evictionStrategy.selectCandidates(entries, count)
        
        metrics.recordCacheEvictionCandidates(strategy, candidates.size)
        
        return candidates
    }
    
    /**
     * 混合策略选择淘汰候选
     */
    fun selectHybridEvictionCandidates(
        entries: Map<String, CacheEntryInfo>,
        count: Int,
        strategyWeights: Map<String, Float> = getDefaultStrategyWeights()
    ): List<String> {
        val candidateScores = mutableMapOf<String, Float>()
        
        for ((strategyName, weight) in strategyWeights) {
            val strategy = evictionStrategies[strategyName] ?: continue
            val candidates = strategy.selectCandidates(entries, count * 2) // 获取更多候选
            
            for ((index, key) in candidates.withIndex()) {
                val score = weight * (1f - index.toFloat() / candidates.size)
                candidateScores.merge(key, score) { a, b -> a + b }
            }
        }
        
        return candidateScores.entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key }
    }
    
    /**
     * 获取默认策略权重
     */
    private fun getDefaultStrategyWeights(): Map<String, Float> {
        return mapOf(
            "lru" to 0.3f,
            "lfu" to 0.2f,
            "size" to 0.2f,
            "cost_benefit" to 0.2f,
            "ml_prediction" to 0.1f
        )
    }
    
    /**
     * 更新淘汰状态
     */
    private fun updateEvictionState() {
        val model = mlModel.get()
        val totalAccessRecords = accessHistory.values.sumOf { it.size }
        val uniqueKeys = accessHistory.size
        
        val currentState = _evictionState.value.copy(
            mlModelTrained = model?.isTrained() ?: false,
            totalAccessRecords = totalAccessRecords,
            uniqueKeys = uniqueKeys,
            activeStrategies = evictionStrategies.keys.toList(),
            lastUpdateTime = System.currentTimeMillis()
        )
        _evictionState.value = currentState
        
        updateEvictionStatistics()
    }
    
    /**
     * 更新淘汰统计信息
     */
    private fun updateEvictionStatistics() {
        val currentStats = evictionStats.get()
        val newStats = currentStats.copy(
            totalAnalyses = currentStats.totalAnalyses + 1,
            lastUpdateTime = System.currentTimeMillis()
        )
        evictionStats.set(newStats)
    }
    
    /**
     * 记录淘汰事件
     */
    fun recordEviction(key: String, strategy: String, reason: String) {
        val currentStats = evictionStats.get()
        val newStats = currentStats.copy(
            totalEvictions = currentStats.totalEvictions + 1,
            lastEvictionTime = System.currentTimeMillis()
        )
        evictionStats.set(newStats)
        
        metrics.recordCacheEviction(key, strategy, reason)
    }
    
    /**
     * 获取访问历史
     */
    fun getAccessHistory(key: String, limit: Int = 100): List<AccessRecord> {
        return accessHistory[key]?.takeLast(limit) ?: emptyList()
    }
    
    /**
     * 获取所有访问历史
     */
    fun getAllAccessHistory(limit: Int = 1000): List<AccessRecord> {
        return accessHistory.values
            .flatten()
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取淘汰统计信息
     */
    fun getEvictionStatistics(): EvictionStatistics {
        return evictionStats.get()
    }
    
    /**
     * 获取淘汰状态
     */
    fun getEvictionState(): EvictionState {
        return _evictionState.value
    }
    
    /**
     * 获取可用的淘汰策略
     */
    fun getAvailableStrategies(): List<String> {
        return evictionStrategies.keys.toList()
    }
    
    /**
     * 预测访问概率
     */
    fun predictAccessProbability(key: String): Float {
        val model = mlModel.get()
        return model?.predictAccessProbability(key) ?: 0f
    }
    
    /**
     * 销毁智能淘汰器
     */
    fun destroy() {
        analysisJob?.cancel()
        scope.cancel()
        
        accessHistory.clear()
        evictionStrategies.clear()
        mlModel.set(null)
        
        INSTANCE = null
    }
}

/**
 * 淘汰策略接口
 */
interface EvictionStrategy {
    val name: String
    val priority: Int
    
    fun selectCandidates(
        entries: Map<String, CacheEntryInfo>,
        count: Int
    ): List<String>
}

/**
 * 缓存条目信息
 */
data class CacheEntryInfo(
    val key: String,
    val size: Long,
    val createTime: Long,
    val lastAccessTime: Long,
    val expireTime: Long,
    val accessCount: Long,
    val hitCount: Long,
    val missCount: Long
)

/**
 * 访问记录
 */
data class AccessRecord(
    val timestamp: Long,
    val key: String,
    val accessType: AccessType
)

/**
 * 访问类型
 */
enum class AccessType {
    READ,
    WRITE,
    DELETE
}

/**
 * 淘汰状态
 */
data class EvictionState(
    val mlModelTrained: Boolean = false,
    val totalAccessRecords: Int = 0,
    val uniqueKeys: Int = 0,
    val activeStrategies: List<String> = emptyList(),
    val lastUpdateTime: Long = 0L
)

/**
 * 淘汰统计信息
 */
data class EvictionStatistics(
    val totalAnalyses: Long = 0L,
    val totalEvictions: Long = 0L,
    val lastEvictionTime: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * 访问预测模型接口
 */
interface AccessPredictionModel {
    fun isTrained(): Boolean
    fun predictAccessProbability(key: String): Float
}

/**
 * 简单的访问预测模型实现
 */
class SimpleAccessPredictionModel(
    private val accessFrequency: Map<String, Int>,
    private val timePatterns: Map<String, List<Long>>
) : AccessPredictionModel {
    
    private val totalAccesses = accessFrequency.values.sum()
    
    override fun isTrained(): Boolean {
        return totalAccesses > 0
    }
    
    override fun predictAccessProbability(key: String): Float {
        val frequency = accessFrequency[key] ?: 0
        val patterns = timePatterns[key] ?: emptyList()
        
        if (totalAccesses == 0) return 0f
        
        // 基于频率的简单预测
        val frequencyScore = frequency.toFloat() / totalAccesses.toFloat()
        
        // 基于时间模式的简单预测
        val timeScore = calculateTimeScore(patterns)
        
        // 综合评分
        return (frequencyScore + timeScore) / 2f
    }
    
    private fun calculateTimeScore(patterns: List<Long>): Float {
        if (patterns.isEmpty()) return 0f
        
        val currentTime = System.currentTimeMillis()
        val recentPatterns = patterns.filter { 
            currentTime - it < 3600000L // 最近1小时
        }
        
        return if (patterns.isNotEmpty()) {
            recentPatterns.size.toFloat() / patterns.size.toFloat()
        } else 0f
    }
}

/**
 * 淘汰策略工厂
 */
object EvictionStrategyFactory {
    
    /**
     * 创建自适应淘汰策略
     */
    fun createAdaptiveStrategy(
        metrics: AiResponseParserMetrics,
        learningRate: Float = 0.1f
    ): EvictionStrategy {
        return AdaptiveEvictionStrategy(metrics, learningRate)
    }
    
    /**
     * 创建基于时间的淘汰策略
     */
    fun createTimeBasedStrategy(
        timeWeight: Float = 0.5f,
        frequencyWeight: Float = 0.5f
    ): EvictionStrategy {
        return TimeBasedEvictionStrategy(timeWeight, frequencyWeight)
    }
}

/**
 * 自适应淘汰策略
 */
class AdaptiveEvictionStrategy(
    private val metrics: AiResponseParserMetrics,
    private val learningRate: Float
) : EvictionStrategy {
    
    override val name = "Adaptive"
    override val priority = 10
    
    private val strategyPerformance = mutableMapOf<String, Float>()
    private val baseStrategies = listOf("lru", "lfu", "size", "cost_benefit")
    
    override fun selectCandidates(
        entries: Map<String, CacheEntryInfo>,
        count: Int
    ): List<String> {
        // 根据性能选择最佳策略
        val bestStrategy = strategyPerformance.maxByOrNull { it.value }?.key ?: "lru"
        
        // 这里应该使用实际的淘汰策略实现
        // 简化实现，使用LRU
        return entries.entries
            .sortedBy { it.value.lastAccessTime }
            .take(count)
            .map { it.key }
    }
    
    /**
     * 更新策略性能
     */
    fun updateStrategyPerformance(strategy: String, performance: Float) {
        val currentPerformance = strategyPerformance.getOrDefault(strategy, 0.5f)
        val updatedPerformance = currentPerformance + learningRate * (performance - currentPerformance)
        strategyPerformance[strategy] = updatedPerformance
    }
}

/**
 * 基于时间的淘汰策略
 */
class TimeBasedEvictionStrategy(
    private val timeWeight: Float,
    private val frequencyWeight: Float
) : EvictionStrategy {
    
    override val name = "TimeBased"
    override val priority = 7
    
    override fun selectCandidates(
        entries: Map<String, CacheEntryInfo>,
        count: Int
    ): List<String> {
        val currentTime = System.currentTimeMillis()
        
        return entries.entries
            .sortedBy { entry ->
                val age = currentTime - entry.value.lastAccessTime
                val frequency = entry.value.accessCount.toFloat()
                
                // 综合时间和频率的评分
                timeWeight * age - frequencyWeight * frequency
            }
            .take(count)
            .map { it.key }
    }
}