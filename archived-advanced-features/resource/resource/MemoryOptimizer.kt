package com.empathy.ai.data.resource

import android.content.Context
import android.os.Debug
import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 内存优化器
 * 负责优化内存使用和防止内存泄漏
 */
class MemoryOptimizer private constructor(
    private val context: Context,
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: MemoryOptimizer? = null
        
        fun getInstance(context: Context, metrics: AiResponseParserMetrics): MemoryOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryOptimizer(context.applicationContext, metrics).also { INSTANCE = it }
            }
        }
        
        private const val MEMORY_CHECK_INTERVAL = 5000L // 5秒
        private const val MEMORY_WARNING_THRESHOLD = 0.8 // 80%内存使用率
        private const val MEMORY_CRITICAL_THRESHOLD = 0.9 // 90%内存使用率
        private const val WEAK_REF_CLEANUP_INTERVAL = 10000L // 10秒
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 内存状态
    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    // 内存历史
    private val memoryHistory = ConcurrentHashMap<Long, MemorySnapshot>()
    
    // 弱引用注册表
    private val weakReferences = ConcurrentHashMap<String, MutableList<WeakReference<*>>>()
    
    // 内存优化策略
    private val optimizationStrategies = ConcurrentHashMap<String, MemoryOptimizationStrategy>()
    
    // 优化历史
    private val optimizationHistory = ConcurrentHashMap<String, MemoryOptimizationHistory>()
    
    // 当前优化
    private val currentOptimizations = AtomicReference<Set<String>>(emptySet())
    
    // 内存警告通道
    private val memoryWarnings = Channel<MemoryWarning>(Channel.UNLIMITED)
    
    // 内存统计
    private val memoryStats = AtomicReference(MemoryStatistics())
    
    // 优化任务
    private var optimizationJob: Job? = null
    private var cleanupJob: Job? = null
    
    init {
        initializeOptimizationStrategies()
        startMemoryMonitoring()
        startWeakReferenceCleanup()
    }
    
    /**
     * 初始化优化策略
     */
    private fun initializeOptimizationStrategies() {
        // 缓存清理策略
        optimizationStrategies["cache_cleanup"] = object : MemoryOptimizationStrategy {
            override val name = "缓存清理"
            override val priority = 1
            override val applicableThreshold = 0.7f
            
            override fun isApplicable(memoryState: MemoryState): Boolean {
                return memoryState.memoryUsageRatio >= applicableThreshold
            }
            
            override suspend fun optimize(): MemoryOptimizationResult {
                val beforeMemory = getCurrentMemoryUsage()
                
                // 清理LRU缓存
                clearLruCache()
                // 清理图片缓存
                clearImageCache()
                // 清理其他临时缓存
                clearTemporaryCache()
                
                val afterMemory = getCurrentMemoryUsage()
                val freedMemory = beforeMemory - afterMemory
                
                return MemoryOptimizationResult(
                    strategyName = name,
                    freedMemoryBytes = freedMemory,
                    success = freedMemory > 0,
                    details = "清理了各种缓存，释放了 ${formatBytes(freedMemory)} 内存"
                )
            }
        }
        
        // 对象池清理策略
        optimizationStrategies["object_pool_cleanup"] = object : MemoryOptimizationStrategy {
            override val name = "对象池清理"
            override val priority = 2
            override val applicableThreshold = 0.75f
            
            override fun isApplicable(memoryState: MemoryState): Boolean {
                return memoryState.memoryUsageRatio >= applicableThreshold
            }
            
            override suspend fun optimize(): MemoryOptimizationResult {
                val beforeMemory = getCurrentMemoryUsage()
                
                // 清理对象池
                clearObjectPools()
                
                val afterMemory = getCurrentMemoryUsage()
                val freedMemory = beforeMemory - afterMemory
                
                return MemoryOptimizationResult(
                    strategyName = name,
                    freedMemoryBytes = freedMemory,
                    success = freedMemory > 0,
                    details = "清理了对象池，释放了 ${formatBytes(freedMemory)} 内存"
                )
            }
        }
        
        // 弱引用清理策略
        optimizationStrategies["weak_ref_cleanup"] = object : MemoryOptimizationStrategy {
            override val name = "弱引用清理"
            override val priority = 3
            override val applicableThreshold = 0.8f
            
            override fun isApplicable(memoryState: MemoryState): Boolean {
                return memoryState.memoryUsageRatio >= applicableThreshold
            }
            
            override suspend fun optimize(): MemoryOptimizationResult {
                val beforeMemory = getCurrentMemoryUsage()
                
                // 清理弱引用
                val cleanedCount = cleanupWeakReferences()
                
                val afterMemory = getCurrentMemoryUsage()
                val freedMemory = beforeMemory - afterMemory
                
                return MemoryOptimizationResult(
                    strategyName = name,
                    freedMemoryBytes = freedMemory,
                    success = cleanedCount > 0,
                    details = "清理了 $cleanedCount 个弱引用，释放了 ${formatBytes(freedMemory)} 内存"
                )
            }
        }
        
        // 强制垃圾回收策略
        optimizationStrategies["force_gc"] = object : MemoryOptimizationStrategy {
            override val name = "强制垃圾回收"
            override val priority = 4
            override val applicableThreshold = 0.85f
            
            override fun isApplicable(memoryState: MemoryState): Boolean {
                return memoryState.memoryUsageRatio >= applicableThreshold
            }
            
            override suspend fun optimize(): MemoryOptimizationResult {
                val beforeMemory = getCurrentMemoryUsage()
                
                // 强制垃圾回收
                System.gc()
                // 等待GC完成
                delay(1000)
                
                val afterMemory = getCurrentMemoryUsage()
                val freedMemory = beforeMemory - afterMemory
                
                return MemoryOptimizationResult(
                    strategyName = name,
                    freedMemoryBytes = freedMemory,
                    success = freedMemory > 0,
                    details = "强制垃圾回收，释放了 ${formatBytes(freedMemory)} 内存"
                )
            }
        }
    }
    
    /**
     * 启动内存监控
     */
    private fun startMemoryMonitoring() {
        optimizationJob = scope.launch {
            while (isActive) {
                try {
                    checkMemoryState()
                    delay(MEMORY_CHECK_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordMemoryOptimizationError("内存监控错误", e)
                }
            }
        }
    }
    
    /**
     * 启动弱引用清理
     */
    private fun startWeakReferenceCleanup() {
        cleanupJob = scope.launch {
            while (isActive) {
                try {
                    cleanupWeakReferences()
                    delay(WEAK_REF_CLEANUP_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordMemoryOptimizationError("弱引用清理错误", e)
                }
            }
        }
    }
    
    /**
     * 检查内存状态
     */
    private fun checkMemoryState() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        val memoryUsageRatio = usedMemory.toFloat() / maxMemory.toFloat()
        
        // 获取内存信息
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            maxMemory = maxMemory,
            memoryUsageRatio = memoryUsageRatio,
            nativeHeapSize = memoryInfo.nativePss * 1024L,
            nativeHeapAllocated = memoryInfo.nativePrivateDirty * 1024L,
            dalvikHeapSize = memoryInfo.dalvikPss * 1024L,
            dalvikHeapAllocated = memoryInfo.dalvikPrivateDirty * 1024L
        )
        
        // 更新内存历史
        memoryHistory[snapshot.timestamp] = snapshot
        
        // 更新当前状态
        val currentState = _memoryState.value.copy(
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            maxMemory = maxMemory,
            memoryUsageRatio = memoryUsageRatio,
            lastCheckTime = snapshot.timestamp
        )
        _memoryState.value = currentState
        
        // 更新统计信息
        updateMemoryStatistics(currentState)
        
        // 检查是否需要优化
        checkAndPerformOptimization(currentState)
        
        // 发送内存警告
        checkMemoryWarnings(currentState)
    }
    
    /**
     * 检查并执行优化
     */
    private fun checkAndPerformOptimization(memoryState: MemoryState) {
        if (memoryState.memoryUsageRatio >= MEMORY_WARNING_THRESHOLD) {
            scope.launch {
                performMemoryOptimization(memoryState)
            }
        }
    }
    
    /**
     * 执行内存优化
     */
    private suspend fun performMemoryOptimization(memoryState: MemoryState) {
        val applicableStrategies = optimizationStrategies.values
            .filter { it.isApplicable(memoryState) }
            .sortedBy { it.priority }
        
        if (applicableStrategies.isEmpty()) {
            return
        }
        
        val optimizationId = "opt_${System.currentTimeMillis()}"
        val startTime = System.currentTimeMillis()
        
        try {
            val results = mutableListOf<MemoryOptimizationResult>()
            val appliedStrategies = mutableSetOf<String>()
            
            for (strategy in applicableStrategies) {
                try {
                    val result = strategy.optimize()
                    results.add(result)
                    appliedStrategies.add(strategy.name)
                    
                    metrics.recordMemoryOptimization(
                        strategy.name,
                        result.freedMemoryBytes,
                        result.success
                    )
                    
                    // 如果内存使用率已经降到安全水平，停止优化
                    val currentState = _memoryState.value
                    if (currentState.memoryUsageRatio < MEMORY_WARNING_THRESHOLD) {
                        break
                    }
                } catch (e: Exception) {
                    metrics.recordMemoryOptimizationError("优化策略执行错误: ${strategy.name}", e)
                }
            }
            
            val endTime = System.currentTimeMillis()
            val totalFreedMemory = results.sumOf { it.freedMemoryBytes }
            
            // 记录优化历史
            optimizationHistory[optimizationId] = MemoryOptimizationHistory(
                id = optimizationId,
                timestamp = startTime,
                duration = endTime - startTime,
                memoryUsageBefore = memoryState.memoryUsageRatio,
                memoryUsageAfter = _memoryState.value.memoryUsageRatio,
                strategiesApplied = appliedStrategies.toList(),
                totalFreedMemory = totalFreedMemory,
                results = results
            )
            
            currentOptimizations.set(appliedStrategies)
            
        } catch (e: Exception) {
            metrics.recordMemoryOptimizationError("内存优化执行错误", e)
        }
    }
    
    /**
     * 检查内存警告
     */
    private fun checkMemoryWarnings(memoryState: MemoryState) {
        when {
            memoryState.memoryUsageRatio >= MEMORY_CRITICAL_THRESHOLD -> {
                val warning = MemoryWarning(
                    level = MemoryWarningLevel.CRITICAL,
                    message = "内存使用率达到危险水平: ${String.format("%.1f%%", memoryState.memoryUsageRatio * 100)}",
                    memoryUsageRatio = memoryState.memoryUsageRatio,
                    timestamp = System.currentTimeMillis()
                )
                
                scope.launch {
                    memoryWarnings.send(warning)
                }
                
                metrics.recordMemoryWarning(MemoryWarningLevel.CRITICAL, memoryState.memoryUsageRatio)
            }
            
            memoryState.memoryUsageRatio >= MEMORY_WARNING_THRESHOLD -> {
                val warning = MemoryWarning(
                    level = MemoryWarningLevel.WARNING,
                    message = "内存使用率较高: ${String.format("%.1f%%", memoryState.memoryUsageRatio * 100)}",
                    memoryUsageRatio = memoryState.memoryUsageRatio,
                    timestamp = System.currentTimeMillis()
                )
                
                scope.launch {
                    memoryWarnings.send(warning)
                }
                
                metrics.recordMemoryWarning(MemoryWarningLevel.WARNING, memoryState.memoryUsageRatio)
            }
        }
    }
    
    /**
     * 更新内存统计信息
     */
    private fun updateMemoryStatistics(memoryState: MemoryState) {
        val currentStats = memoryStats.get()
        val newStats = currentStats.copy(
            totalChecks = currentStats.totalChecks + 1,
            averageMemoryUsage = (currentStats.averageMemoryUsage * currentStats.totalChecks + memoryState.memoryUsageRatio) / (currentStats.totalChecks + 1),
            peakMemoryUsage = maxOf(currentStats.peakMemoryUsage, memoryState.memoryUsageRatio),
            lastUpdateTime = System.currentTimeMillis()
        )
        memoryStats.set(newStats)
    }
    
    /**
     * 清理LRU缓存
     */
    private fun clearLruCache() {
        // 这里应该调用实际的缓存清理逻辑
        // 例如：AiResponseParserCache.clearLruCache()
    }
    
    /**
     * 清理图片缓存
     */
    private fun clearImageCache() {
        // 这里应该调用实际的图片缓存清理逻辑
    }
    
    /**
     * 清理临时缓存
     */
    private fun clearTemporaryCache() {
        // 这里应该调用实际的临时缓存清理逻辑
    }
    
    /**
     * 清理对象池
     */
    private fun clearObjectPools() {
        // 这里应该调用实际的对象池清理逻辑
        // 例如：ResourcePool.clearAllPools()
    }
    
    /**
     * 清理弱引用
     */
    private fun cleanupWeakReferences(): Int {
        var cleanedCount = 0
        
        for ((key, refs) in weakReferences) {
            val iterator = refs.iterator()
            while (iterator.hasNext()) {
                val ref = iterator.next()
                if (ref.get() == null) {
                    iterator.remove()
                    cleanedCount++
                }
            }
        }
        
        return cleanedCount
    }
    
    /**
     * 获取当前内存使用量
     */
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * 格式化字节数
     */
    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
    
    /**
     * 注册弱引用
     */
    fun registerWeakReference(key: String, ref: WeakReference<*>) {
        val refs = weakReferences.getOrPut(key) { mutableListOf() }
        refs.add(ref)
    }
    
    /**
     * 获取内存警告流
     */
    fun getMemoryWarningFlow() = memoryWarnings
    
    /**
     * 获取内存历史
     */
    fun getMemoryHistory(limit: Int = 100): List<MemorySnapshot> {
        return memoryHistory.values
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取优化历史
     */
    fun getOptimizationHistory(limit: Int = 50): List<MemoryOptimizationHistory> {
        return optimizationHistory.values
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取内存统计信息
     */
    fun getMemoryStatistics(): MemoryStatistics {
        return memoryStats.get()
    }
    
    /**
     * 手动触发内存优化
     */
    suspend fun triggerManualOptimization(): List<MemoryOptimizationResult> {
        val memoryState = _memoryState.value
        val applicableStrategies = optimizationStrategies.values
            .filter { it.isApplicable(memoryState) }
            .sortedBy { it.priority }
        
        val results = mutableListOf<MemoryOptimizationResult>()
        
        for (strategy in applicableStrategies) {
            try {
                val result = strategy.optimize()
                results.add(result)
                
                metrics.recordMemoryOptimization(
                    strategy.name,
                    result.freedMemoryBytes,
                    result.success
                )
            } catch (e: Exception) {
                metrics.recordMemoryOptimizationError("手动优化错误: ${strategy.name}", e)
            }
        }
        
        return results
    }
    
    /**
     * 销毁内存优化器
     */
    fun destroy() {
        optimizationJob?.cancel()
        cleanupJob?.cancel()
        scope.cancel()
        
        weakReferences.clear()
        memoryHistory.clear()
        optimizationHistory.clear()
        optimizationStrategies.clear()
        
        INSTANCE = null
    }
}

/**
 * 内存状态
 */
data class MemoryState(
    val totalMemory: Long = 0L,
    val usedMemory: Long = 0L,
    val freeMemory: Long = 0L,
    val maxMemory: Long = 0L,
    val memoryUsageRatio: Float = 0f,
    val lastCheckTime: Long = 0L
)

/**
 * 内存快照
 */
data class MemorySnapshot(
    val timestamp: Long,
    val totalMemory: Long,
    val usedMemory: Long,
    val freeMemory: Long,
    val maxMemory: Long,
    val memoryUsageRatio: Float,
    val nativeHeapSize: Long,
    val nativeHeapAllocated: Long,
    val dalvikHeapSize: Long,
    val dalvikHeapAllocated: Long
)

/**
 * 内存优化策略
 */
interface MemoryOptimizationStrategy {
    val name: String
    val priority: Int
    val applicableThreshold: Float
    
    fun isApplicable(memoryState: MemoryState): Boolean
    suspend fun optimize(): MemoryOptimizationResult
}

/**
 * 内存优化结果
 */
data class MemoryOptimizationResult(
    val strategyName: String,
    val freedMemoryBytes: Long,
    val success: Boolean,
    val details: String
)

/**
 * 内存优化历史
 */
data class MemoryOptimizationHistory(
    val id: String,
    val timestamp: Long,
    val duration: Long,
    val memoryUsageBefore: Float,
    val memoryUsageAfter: Float,
    val strategiesApplied: List<String>,
    val totalFreedMemory: Long,
    val results: List<MemoryOptimizationResult>
)

/**
 * 内存警告
 */
data class MemoryWarning(
    val level: MemoryWarningLevel,
    val message: String,
    val memoryUsageRatio: Float,
    val timestamp: Long
)

/**
 * 内存警告级别
 */
enum class MemoryWarningLevel {
    WARNING,
    CRITICAL
}

/**
 * 内存统计信息
 */
data class MemoryStatistics(
    val totalChecks: Long = 0L,
    val averageMemoryUsage: Float = 0f,
    val peakMemoryUsage: Float = 0f,
    val lastUpdateTime: Long = 0L
)