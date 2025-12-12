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
 * 缓存预热器
 * 智能预测和预热热点数据
 */
class CacheWarmer private constructor(
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: CacheWarmer? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): CacheWarmer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CacheWarmer(metrics).also { INSTANCE = it }
            }
        }
        
        private const val ANALYSIS_INTERVAL = 300000L // 5分钟
        private const val WARMUP_BATCH_SIZE = 10
        private const val WARMUP_INTERVAL = 1000L // 1秒
        private const val MIN_ACCESS_COUNT = 3
        private const val PREDICTION_WINDOW = 3600000L // 1小时
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 预热任务队列
    private val warmupQueue = PriorityBlockingQueue<WarmupTask>()
    
    // 访问模式分析器
    private val accessPatternAnalyzer = AccessPatternAnalyzer()
    
    // 预热策略
    private val warmupStrategies = ConcurrentHashMap<String, WarmupStrategy>()
    
    // 预热历史
    private val warmupHistory = ConcurrentHashMap<String, WarmupHistory>()
    
    // 预热状态
    private val _warmupState = MutableStateFlow(WarmupState())
    val warmupState: StateFlow<WarmupState> = _warmupState.asStateFlow()
    
    // 预热统计
    private val warmupStats = AtomicReference(WarmupStatistics())
    
    // 缓存注册表
    private val cacheRegistry = ConcurrentHashMap<String, Any>() // 实际应该是Cache实例
    
    // 分析任务
    private var analysisJob: Job? = null
    
    // 预热任务
    private var warmupJob: Job? = null
    
    init {
        initializeWarmupStrategies()
        startAnalysisTask()
        startWarmupTask()
    }
    
    /**
     * 初始化预热策略
     */
    private fun initializeWarmupStrategies() {
        // 基于访问频率的预热策略
        warmupStrategies["frequency"] = object : WarmupStrategy {
            override val name = "FrequencyBased"
            override val priority = 1
            
            override fun selectWarmupCandidates(
                accessPatterns: Map<String, AccessPattern>,
                count: Int
            ): List<WarmupCandidate> {
                return accessPatterns.entries
                    .filter { it.value.accessCount >= MIN_ACCESS_COUNT }
                    .sortedByDescending { it.value.accessCount }
                    .take(count)
                    .map { (key, pattern) ->
                        WarmupCandidate(
                            key = key,
                            priority = pattern.accessCount.toFloat(),
                            strategy = name,
                            reason = "高访问频率: ${pattern.accessCount}"
                        )
                    }
            }
        }
        
        // 基于时间模式的预热策略
        warmupStrategies["time_pattern"] = object : WarmupStrategy {
            override val name = "TimePatternBased"
            override val priority = 2
            
            override fun selectWarmupCandidates(
                accessPatterns: Map<String, AccessPattern>,
                count: Int
            ): List<WarmupCandidate> {
                val currentTime = System.currentTimeMillis()
                return accessPatterns.entries
                    .filter { hasTimePattern(it.value, currentTime) }
                    .sortedByDescending { calculateTimePatternScore(it.value, currentTime) }
                    .take(count)
                    .map { (key, pattern) ->
                        WarmupCandidate(
                            key = key,
                            priority = calculateTimePatternScore(pattern, currentTime),
                            strategy = name,
                            reason = "时间模式匹配"
                        )
                    }
            }
            
            private fun hasTimePattern(pattern: AccessPattern, currentTime: Long): Boolean {
                // 检查是否有周期性访问模式
                return pattern.accessTimes.size >= 3
            }
            
            private fun calculateTimePatternScore(pattern: AccessPattern, currentTime: Long): Float {
                // 简化的时间模式评分
                val recentAccesses = pattern.accessTimes.filter { 
                    currentTime - it < PREDICTION_WINDOW 
                }
                return recentAccesses.size.toFloat()
            }
        }
        
        // 基于关联关系的预热策略
        warmupStrategies["association"] = object : WarmupStrategy {
            override val name = "AssociationBased"
            override val priority = 3
            
            override fun selectWarmupCandidates(
                accessPatterns: Map<String, AccessPattern>,
                count: Int
            ): List<WarmupCandidate> {
                val candidates = mutableListOf<WarmupCandidate>()
                
                // 分析关联关系
                val associations = analyzeAssociations(accessPatterns)
                
                for ((key, associatedKeys) in associations) {
                    for (associatedKey in associatedKeys.take(3)) {
                        candidates.add(
                            WarmupCandidate(
                                key = associatedKey,
                                priority = 0.5f,
                                strategy = name,
                                reason = "与 $key 关联"
                            )
                        )
                    }
                }
                
                return candidates.take(count)
            }
            
            private fun analyzeAssociations(
                patterns: Map<String, AccessPattern>
            ): Map<String, List<String>> {
                // 简化的关联分析
                val associations = mutableMapOf<String, MutableList<String>>()
                
                for ((key, pattern) in patterns) {
                    for (accessTime in pattern.accessTimes) {
                        // 查找在同一时间窗口内访问的其他键
                        val associatedKeys = patterns.entries
                            .filter { it.key != key && it.value.accessTimes.any { 
                                Math.abs(it - accessTime) < 60000L // 1分钟内
                            }}
                            .map { it.key }
                        
                        if (associatedKeys.isNotEmpty()) {
                            associations.getOrPut(key) { mutableListOf() }.addAll(associatedKeys)
                        }
                    }
                }
                
                return associations
            }
        }
        
        // 基于机器学习的预热策略
        warmupStrategies["ml_prediction"] = object : WarmupStrategy {
            override val name = "MLPredictionBased"
            override val priority = 4
            
            override fun selectWarmupCandidates(
                accessPatterns: Map<String, AccessPattern>,
                count: Int
            ): List<WarmupCandidate> {
                // 简化的机器学习预测
                return accessPatterns.entries
                    .sortedByDescending { predictAccessProbability(it.value) }
                    .take(count)
                    .map { (key, pattern) ->
                        WarmupCandidate(
                            key = key,
                            priority = predictAccessProbability(pattern),
                            strategy = name,
                            reason = "机器学习预测"
                        )
                    }
            }
            
            private fun predictAccessProbability(pattern: AccessPattern): Float {
                val currentTime = System.currentTimeMillis()
                val recentAccesses = pattern.accessTimes.filter { 
                    currentTime - it < PREDICTION_WINDOW 
                }
                
                if (recentAccesses.isEmpty()) return 0f
                
                // 简单的线性预测
                val timeSpan = recentAccesses.maxOrNull()!! - recentAccesses.minOrNull()!!
                val accessRate = if (timeSpan > 0) recentAccesses.size.toFloat() / timeSpan.toFloat() else 0f
                
                return minOf(accessRate * 1000f, 1f) // 归一化到0-1
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
                    generateWarmupTasks()
                    delay(ANALYSIS_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordCacheWarmupError("访问模式分析错误", e)
                }
            }
        }
    }
    
    /**
     * 启动预热任务
     */
    private fun startWarmupTask() {
        warmupJob = scope.launch {
            while (isActive) {
                try {
                    processWarmupQueue()
                    delay(WARMUP_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordCacheWarmupError("预热任务处理错误", e)
                }
            }
        }
    }
    
    /**
     * 记录访问
     */
    fun recordAccess(key: String, cacheName: String = "default") {
        accessPatternAnalyzer.recordAccess(key, cacheName)
        metrics.recordCacheWarmupAccess(key, cacheName)
    }
    
    /**
     * 分析访问模式
     */
    private fun analyzeAccessPatterns() {
        val patterns = accessPatternAnalyzer.getAccessPatterns()
        
        // 更新预热状态
        val currentState = _warmupState.value.copy(
            totalAccessPatterns = patterns.size,
            lastAnalysisTime = System.currentTimeMillis()
        )
        _warmupState.value = currentState
    }
    
    /**
     * 生成预热任务
     */
    private fun generateWarmupTasks() {
        val patterns = accessPatternAnalyzer.getAccessPatterns()
        
        for (strategy in warmupStrategies.values) {
            val candidates = strategy.selectWarmupCandidates(patterns, WARMUP_BATCH_SIZE)
            
            for (candidate in candidates) {
                val task = WarmupTask(
                    id = "warmup_${System.currentTimeMillis()}_${candidate.key}",
                    key = candidate.key,
                    cacheName = "default", // 可以从访问模式中获取
                    priority = candidate.priority,
                    strategy = candidate.strategy,
                    reason = candidate.reason,
                    createTime = System.currentTimeMillis()
                )
                
                warmupQueue.offer(task)
            }
        }
    }
    
    /**
     * 处理预热队列
     */
    private suspend fun processWarmupQueue() {
        val tasksToProcess = mutableListOf<WarmupTask>()
        
        // 取出高优先级任务
        repeat(WARMUP_BATCH_SIZE) {
            val task = warmupQueue.poll()
            if (task != null) {
                tasksToProcess.add(task)
            } else {
                return@repeat
            }
        }
        
        if (tasksToProcess.isEmpty()) return
        
        for (task in tasksToProcess) {
            try {
                executeWarmupTask(task)
            } catch (e: Exception) {
                metrics.recordCacheWarmupError("预热任务执行错误", e)
                recordWarmupFailure(task, e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 执行预热任务
     */
    private suspend fun executeWarmupTask(task: WarmupTask) {
        val startTime = System.currentTimeMillis()
        
        // 这里应该调用实际的缓存预热逻辑
        // 例如：从数据源加载数据到缓存
        val success = performActualWarmup(task.key, task.cacheName)
        
        val duration = System.currentTimeMillis() - startTime
        
        if (success) {
            recordWarmupSuccess(task, duration)
        } else {
            recordWarmupFailure(task, "预热失败")
        }
    }
    
    /**
     * 执行实际的预热操作
     */
    private suspend fun performActualWarmup(key: String, cacheName: String): Boolean {
        // 这里应该实现实际的预热逻辑
        // 例如：从数据库或网络加载数据
        
        // 模拟预热操作
        delay(100) // 模拟加载时间
        
        // 这里应该将数据加载到指定的缓存中
        // cacheRegistry[cacheName]?.put(key, data)
        
        return true
    }
    
    /**
     * 记录预热成功
     */
    private fun recordWarmupSuccess(task: WarmupTask, duration: Long) {
        val history = WarmupHistory(
            taskId = task.id,
            key = task.key,
            cacheName = task.cacheName,
            strategy = task.strategy,
            success = true,
            duration = duration,
            reason = task.reason,
            timestamp = System.currentTimeMillis()
        )
        
        warmupHistory[task.id] = history
        
        val currentStats = warmupStats.get()
        val newStats = currentStats.copy(
            totalWarmups = currentStats.totalWarmups + 1,
            successfulWarmups = currentStats.successfulWarmups + 1,
            averageWarmupTime = (currentStats.averageWarmupTime * currentStats.totalWarmups + duration) / (currentStats.totalWarmups + 1),
            lastWarmupTime = System.currentTimeMillis()
        )
        warmupStats.set(newStats)
        
        metrics.recordCacheWarmupSuccess(task.key, task.strategy, duration)
    }
    
    /**
     * 记录预热失败
     */
    private fun recordWarmupFailure(task: WarmupTask, error: String) {
        val history = WarmupHistory(
            taskId = task.id,
            key = task.key,
            cacheName = task.cacheName,
            strategy = task.strategy,
            success = false,
            duration = 0,
            reason = "${task.reason} - 失败: $error",
            timestamp = System.currentTimeMillis()
        )
        
        warmupHistory[task.id] = history
        
        val currentStats = warmupStats.get()
        val newStats = currentStats.copy(
            totalWarmups = currentStats.totalWarmups + 1,
            failedWarmups = currentStats.failedWarmups + 1,
            lastWarmupTime = System.currentTimeMillis()
        )
        warmupStats.set(newStats)
        
        metrics.recordCacheWarmupFailure(task.key, task.strategy, error)
    }
    
    /**
     * 手动添加预热任务
     */
    fun addWarmupTask(
        key: String,
        cacheName: String = "default",
        priority: Float = 1.0f,
        strategy: String = "manual",
        reason: String = "手动预热"
    ) {
        val task = WarmupTask(
            id = "manual_warmup_${System.currentTimeMillis()}_${key}",
            key = key,
            cacheName = cacheName,
            priority = priority,
            strategy = strategy,
            reason = reason,
            createTime = System.currentTimeMillis()
        )
        
        warmupQueue.offer(task)
        metrics.recordCacheWarmupManualTask(key, cacheName)
    }
    
    /**
     * 批量添加预热任务
     */
    fun addWarmupTasks(
        keys: List<String>,
        cacheName: String = "default",
        priority: Float = 1.0f,
        strategy: String = "manual_batch",
        reason: String = "批量手动预热"
    ) {
        for (key in keys) {
            addWarmupTask(key, cacheName, priority, strategy, reason)
        }
    }
    
    /**
     * 注册缓存
     */
    fun registerCache(name: String, cache: Any) {
        cacheRegistry[name] = cache
    }
    
    /**
     * 获取预热历史
     */
    fun getWarmupHistory(limit: Int = 100): List<WarmupHistory> {
        return warmupHistory.values
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取预热统计信息
     */
    fun getWarmupStatistics(): WarmupStatistics {
        return warmupStats.get()
    }
    
    /**
     * 获取预热状态
     */
    fun getWarmupState(): WarmupState {
        return _warmupState.value
    }
    
    /**
     * 获取可用的预热策略
     */
    fun getAvailableStrategies(): List<String> {
        return warmupStrategies.keys.toList()
    }
    
    /**
     * 清空预热队列
     */
    fun clearWarmupQueue() {
        warmupQueue.clear()
        metrics.recordCacheWarmupQueueCleared()
    }
    
    /**
     * 销毁缓存预热器
     */
    fun destroy() {
        analysisJob?.cancel()
        warmupJob?.cancel()
        scope.cancel()
        
        warmupQueue.clear()
        warmupHistory.clear()
        warmupStrategies.clear()
        cacheRegistry.clear()
        
        INSTANCE = null
    }
}

/**
 * 预热策略接口
 */
interface WarmupStrategy {
    val name: String
    val priority: Int
    
    fun selectWarmupCandidates(
        accessPatterns: Map<String, AccessPattern>,
        count: Int
    ): List<WarmupCandidate>
}

/**
 * 预热候选
 */
data class WarmupCandidate(
    val key: String,
    val priority: Float,
    val strategy: String,
    val reason: String
)

/**
 * 预热任务
 */
data class WarmupTask(
    val id: String,
    val key: String,
    val cacheName: String,
    val priority: Float,
    val strategy: String,
    val reason: String,
    val createTime: Long
) : Comparable<WarmupTask> {
    override fun compareTo(other: WarmupTask): Int {
        return other.priority.compareTo(this.priority) // 高优先级在前
    }
}

/**
 * 预热历史
 */
data class WarmupHistory(
    val taskId: String,
    val key: String,
    val cacheName: String,
    val strategy: String,
    val success: Boolean,
    val duration: Long,
    val reason: String,
    val timestamp: Long
)

/**
 * 预热状态
 */
data class WarmupState(
    val totalAccessPatterns: Int = 0,
    val queueSize: Int = 0,
    val lastAnalysisTime: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * 预热统计信息
 */
data class WarmupStatistics(
    val totalWarmups: Long = 0L,
    val successfulWarmups: Long = 0L,
    val failedWarmups: Long = 0L,
    val averageWarmupTime: Long = 0L,
    val lastWarmupTime: Long = 0L
) {
    val successRate: Float
        get() = if (totalWarmups > 0) successfulWarmups.toFloat() / totalWarmups.toFloat() else 0f
}

/**
 * 访问模式分析器
 */
class AccessPatternAnalyzer {
    private val accessPatterns = ConcurrentHashMap<String, AccessPattern>()
    
    fun recordAccess(key: String, cacheName: String = "default") {
        val currentTime = System.currentTimeMillis()
        val fullKey = "$cacheName:$key"
        
        val pattern = accessPatterns.getOrPut(fullKey) {
            AccessPattern(key = key, cacheName = cacheName)
        }
        
        pattern.recordAccess(currentTime)
    }
    
    fun getAccessPatterns(): Map<String, AccessPattern> {
        return accessPatterns.toMap()
    }
}

/**
 * 访问模式
 */
data class AccessPattern(
    val key: String,
    val cacheName: String,
    var accessCount: Long = 0L,
    val accessTimes: MutableList<Long> = mutableListOf(),
    var lastAccessTime: Long = 0L
) {
    fun recordAccess(timestamp: Long) {
        accessCount++
        accessTimes.add(timestamp)
        lastAccessTime = timestamp
        
        // 限制访问记录数量
        if (accessTimes.size > 1000) {
            accessTimes.removeAt(0)
        }
    }
}

/**
 * 缓存预热器工厂
 */
object CacheWarmerFactory {
    
    /**
     * 创建智能缓存预热器
     */
    fun createIntelligentWarmer(metrics: AiResponseParserMetrics): CacheWarmer {
        return CacheWarmer.getInstance(metrics)
    }
    
    /**
     * 创建简单的缓存预热器
     */
    fun createSimpleWarmer(metrics: AiResponseParserMetrics): SimpleCacheWarmer {
        return SimpleCacheWarmer(metrics)
    }
}

/**
 * 简单的缓存预热器实现
 */
class SimpleCacheWarmer(
    private val metrics: AiResponseParserMetrics
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * 预热指定的键列表
     */
    suspend fun warmUpKeys(
        keys: List<String>,
        cacheName: String = "default",
        valueProvider: suspend (String) -> Any?
    ) {
        for (key in keys) {
            try {
                val value = valueProvider(key)
                if (value != null) {
                    // 这里应该将值放入缓存
                    metrics.recordCacheWarmupSuccess(key, "simple", 0)
                }
            } catch (e: Exception) {
                metrics.recordCacheWarmupFailure(key, "simple", e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 销毁预热器
     */
    fun destroy() {
        scope.cancel()
    }
}