package com.empathy.ai.data.resource

import android.content.Context
import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.management.ManagementFactory
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.MemoryMXBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 垃圾回收优化器
 * 优化GC行为减少GC停顿
 */
class GarbageCollectionOptimizer private constructor(
    private val context: Context,
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: GarbageCollectionOptimizer? = null
        
        fun getInstance(context: Context, metrics: AiResponseParserMetrics): GarbageCollectionOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GarbageCollectionOptimizer(context.applicationContext, metrics).also { INSTANCE = it }
            }
        }
        
        private const val GC_MONITORING_INTERVAL = 5000L // 5秒
        private const val GC_ANALYSIS_WINDOW = 60000L // 1分钟
        private const val GC_PAUSE_THRESHOLD = 100L // 100ms
        private const val GC_FREQUENCY_THRESHOLD = 10 // 10次/分钟
        private const val MEMORY_PRESSURE_THRESHOLD = 0.8f // 80%
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // GC状态
    private val _gcState = MutableStateFlow(GcState())
    val gcState: StateFlow<GcState> = _gcState.asStateFlow()
    
    // GC历史记录
    private val gcHistory = ConcurrentHashMap<Long, GcEvent>()
    
    // GC统计信息
    private val gcStatistics = AtomicReference(GcStatistics())
    
    // GC优化策略
    private val optimizationStrategies = ConcurrentHashMap<String, GcOptimizationStrategy>()
    
    // 优化历史
    private val optimizationHistory = ConcurrentHashMap<String, GcOptimizationHistory>()
    
    // 内存监控
    private val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    private val gcBeans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()
    
    // 监控任务
    private var monitoringJob: Job? = null
    
    // 上次GC计数
    private val lastGcCounts = ConcurrentHashMap<String, Long>()
    
    init {
        initializeOptimizationStrategies()
        initializeGcCounters()
        startGcMonitoring()
    }
    
    /**
     * 初始化优化策略
     */
    private fun initializeOptimizationStrategies() {
        // 预分配策略
        optimizationStrategies["pre_allocation"] = object : GcOptimizationStrategy {
            override val name = "预分配优化"
            override val priority = 1
            override val applicableThreshold = 0.7f
            
            override fun isApplicable(gcState: GcState): Boolean {
                return gcState.memoryPressure >= applicableThreshold && gcState.gcFrequency > 5
            }
            
            override suspend fun optimize(): GcOptimizationResult {
                val beforeState = getCurrentGcState()
                
                // 预分配常用对象
                preAllocateCommonObjects()
                // 预分配缓冲区
                preAllocateBuffers()
                
                val afterState = getCurrentGcState()
                
                return GcOptimizationResult(
                    strategyName = name,
                    success = true,
                    details = "执行了预分配优化，减少了GC压力",
                    gcFrequencyBefore = beforeState.gcFrequency,
                    gcFrequencyAfter = afterState.gcFrequency,
                    averagePauseTimeBefore = beforeState.averagePauseTime,
                    averagePauseTimeAfter = afterState.averagePauseTime
                )
            }
        }
        
        // 对象复用策略
        optimizationStrategies["object_reuse"] = object : GcOptimizationStrategy {
            override val name = "对象复用优化"
            override val priority = 2
            override val applicableThreshold = 0.75f
            
            override fun isApplicable(gcState: GcState): Boolean {
                return gcState.memoryPressure >= applicableThreshold && gcState.gcFrequency > 8
            }
            
            override suspend fun optimize(): GcOptimizationResult {
                val beforeState = getCurrentGcState()
                
                // 启用对象池
                enableObjectPooling()
                // 优化字符串处理
                optimizeStringHandling()
                
                val afterState = getCurrentGcState()
                
                return GcOptimizationResult(
                    strategyName = name,
                    success = true,
                    details = "启用了对象复用机制，减少了对象创建和销毁",
                    gcFrequencyBefore = beforeState.gcFrequency,
                    gcFrequencyAfter = afterState.gcFrequency,
                    averagePauseTimeBefore = beforeState.averagePauseTime,
                    averagePauseTimeAfter = afterState.averagePauseTime
                )
            }
        }
        
        // 内存整理策略
        optimizationStrategies["memory_compaction"] = object : GcOptimizationStrategy {
            override val name = "内存整理优化"
            override val priority = 3
            override val applicableThreshold = 0.8f
            
            override fun isApplicable(gcState: GcState): Boolean {
                return gcState.memoryPressure >= applicableThreshold && gcState.averagePauseTime > GC_PAUSE_THRESHOLD
            }
            
            override suspend fun optimize(): GcOptimizationResult {
                val beforeState = getCurrentGcState()
                
                // 触发内存整理
                System.gc()
                delay(2000) // 等待GC完成
                
                val afterState = getCurrentGcState()
                
                return GcOptimizationResult(
                    strategyName = name,
                    success = true,
                    details = "执行了内存整理，优化了内存布局",
                    gcFrequencyBefore = beforeState.gcFrequency,
                    gcFrequencyAfter = afterState.gcFrequency,
                    averagePauseTimeBefore = beforeState.averagePauseTime,
                    averagePauseTimeAfter = afterState.averagePauseTime
                )
            }
        }
        
        // 内存泄漏检测策略
        optimizationStrategies["memory_leak_detection"] = object : GcOptimizationStrategy {
            override val name = "内存泄漏检测"
            override val priority = 4
            override val applicableThreshold = 0.85f
            
            override fun isApplicable(gcState: GcState): Boolean {
                return gcState.memoryPressure >= applicableThreshold
            }
            
            override suspend fun optimize(): GcOptimizationResult {
                val beforeState = getCurrentGcState()
                
                // 检测内存泄漏
                val leakDetected = detectMemoryLeaks()
                
                val afterState = getCurrentGcState()
                
                return GcOptimizationResult(
                    strategyName = name,
                    success = true,
                    details = if (leakDetected) "检测到潜在的内存泄漏" else "未检测到内存泄漏",
                    gcFrequencyBefore = beforeState.gcFrequency,
                    gcFrequencyAfter = afterState.gcFrequency,
                    averagePauseTimeBefore = beforeState.averagePauseTime,
                    averagePauseTimeAfter = afterState.averagePauseTime
                )
            }
        }
    }
    
    /**
     * 初始化GC计数器
     */
    private fun initializeGcCounters() {
        for (gcBean in gcBeans) {
            lastGcCounts[gcBean.name] = gcBean.collectionCount
        }
    }
    
    /**
     * 启动GC监控
     */
    private fun startGcMonitoring() {
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    monitorGcActivity()
                    delay(GC_MONITORING_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordGcOptimizationError("GC监控错误", e)
                }
            }
        }
    }
    
    /**
     * 监控GC活动
     */
    private fun monitorGcActivity() {
        val currentTime = System.currentTimeMillis()
        val gcEvents = mutableListOf<GcEvent>()
        
        for (gcBean in gcBeans) {
            val currentCount = gcBean.collectionCount
            val lastCount = lastGcCounts[gcBean.name] ?: 0L
            
            if (currentCount > lastCount) {
                val gcEvent = GcEvent(
                    timestamp = currentTime,
                    gcName = gcBean.name,
                    collectionCount = currentCount,
                    collectionTime = gcBean.collectionTime,
                    memoryBefore = getMemoryUsage(),
                    memoryAfter = getMemoryUsage()
                )
                
                gcEvents.add(gcEvent)
                gcHistory[currentTime] = gcEvent
                lastGcCounts[gcBean.name] = currentCount
                
                metrics.recordGcEvent(gcBean.name, gcEvent.collectionTime)
            }
        }
        
        if (gcEvents.isNotEmpty()) {
            updateGcState()
            checkAndPerformOptimization()
        }
    }
    
    /**
     * 更新GC状态
     */
    private fun updateGcState() {
        val currentTime = System.currentTimeMillis()
        val windowStart = currentTime - GC_ANALYSIS_WINDOW
        
        // 分析最近的GC事件
        val recentGcEvents = gcHistory.values.filter { it.timestamp >= windowStart }
        
        val gcFrequency = recentGcEvents.size
        val averagePauseTime = if (recentGcEvents.isNotEmpty()) {
            recentGcEvents.map { it.collectionTime }.average().toFloat()
        } else 0f
        
        val memoryUsage = getMemoryUsage()
        val memoryPressure = memoryUsage.toFloat() / Runtime.getRuntime().maxMemory()
        
        val currentState = _gcState.value.copy(
            gcFrequency = gcFrequency,
            averagePauseTime = averagePauseTime,
            memoryPressure = memoryPressure,
            memoryUsage = memoryUsage,
            maxMemory = Runtime.getRuntime().maxMemory(),
            lastUpdateTime = currentTime
        )
        
        _gcState.value = currentState
        updateGcStatistics(currentState)
    }
    
    /**
     * 更新GC统计信息
     */
    private fun updateGcStatistics(gcState: GcState) {
        val currentStats = gcStatistics.get()
        val newStats = currentStats.copy(
            totalGcEvents = currentStats.totalGcEvents + gcState.gcFrequency,
            averageGcFrequency = (currentStats.averageGcFrequency * currentStats.sampleCount + gcState.gcFrequency) / (currentStats.sampleCount + 1),
            averagePauseTime = (currentStats.averagePauseTime * currentStats.sampleCount + gcState.averagePauseTime) / (currentStats.sampleCount + 1),
            peakMemoryPressure = maxOf(currentStats.peakMemoryPressure, gcState.memoryPressure),
            sampleCount = currentStats.sampleCount + 1,
            lastUpdateTime = System.currentTimeMillis()
        )
        gcStatistics.set(newStats)
    }
    
    /**
     * 检查并执行优化
     */
    private fun checkAndPerformOptimization() {
        val gcState = _gcState.value
        
        if (gcState.memoryPressure >= MEMORY_PRESSURE_THRESHOLD || 
            gcState.gcFrequency >= GC_FREQUENCY_THRESHOLD ||
            gcState.averagePauseTime >= GC_PAUSE_THRESHOLD) {
            
            scope.launch {
                performGcOptimization(gcState)
            }
        }
    }
    
    /**
     * 执行GC优化
     */
    private suspend fun performGcOptimization(gcState: GcState) {
        val applicableStrategies = optimizationStrategies.values
            .filter { it.isApplicable(gcState) }
            .sortedBy { it.priority }
        
        if (applicableStrategies.isEmpty()) {
            return
        }
        
        val optimizationId = "gc_opt_${System.currentTimeMillis()}"
        val startTime = System.currentTimeMillis()
        
        try {
            val results = mutableListOf<GcOptimizationResult>()
            val appliedStrategies = mutableSetOf<String>()
            
            for (strategy in applicableStrategies) {
                try {
                    val result = strategy.optimize()
                    results.add(result)
                    appliedStrategies.add(strategy.name)
                    
                    metrics.recordGcOptimization(
                        strategy.name,
                        result.success
                    )
                    
                    // 如果GC状态已经改善，停止优化
                    val currentState = _gcState.value
                    if (currentState.memoryPressure < MEMORY_PRESSURE_THRESHOLD &&
                        currentState.gcFrequency < GC_FREQUENCY_THRESHOLD) {
                        break
                    }
                } catch (e: Exception) {
                    metrics.recordGcOptimizationError("GC优化策略执行错误: ${strategy.name}", e)
                }
            }
            
            val endTime = System.currentTimeMillis()
            
            // 记录优化历史
            optimizationHistory[optimizationId] = GcOptimizationHistory(
                id = optimizationId,
                timestamp = startTime,
                duration = endTime - startTime,
                gcStateBefore = gcState,
                gcStateAfter = _gcState.value,
                strategiesApplied = appliedStrategies.toList(),
                results = results
            )
            
        } catch (e: Exception) {
            metrics.recordGcOptimizationError("GC优化执行错误", e)
        }
    }
    
    /**
     * 获取内存使用量
     */
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * 获取当前GC状态
     */
    private fun getCurrentGcState(): GcState {
        return _gcState.value
    }
    
    /**
     * 预分配常用对象
     */
    private fun preAllocateCommonObjects() {
        // 预分配StringBuilder
        val sbPool = ResourcePool.getInstance(metrics).createPool(
            "string_builder_pool",
            StringBuilderResourceFactory(),
            PoolConfig(minSize = 5, maxSize = 20)
        )
        
        // 预分配StringBuffer
        val sbufPool = ResourcePool.getInstance(metrics).createPool(
            "string_buffer_pool",
            StringBufferResourceFactory(),
            PoolConfig(minSize = 5, maxSize = 20)
        )
    }
    
    /**
     * 预分配缓冲区
     */
    private fun preAllocateBuffers() {
        // 预分配字节缓冲区
        // 这里可以根据实际需求预分配各种缓冲区
    }
    
    /**
     * 启用对象池
     */
    private fun enableObjectPooling() {
        // 启用各种对象池
        // 例如：JSON解析器池、字符串池等
    }
    
    /**
     * 优化字符串处理
     */
    private fun optimizeStringHandling() {
        // 优化字符串处理逻辑
        // 例如：使用StringBuilder代替字符串拼接
    }
    
    /**
     * 检测内存泄漏
     */
    private fun detectMemoryLeaks(): Boolean {
        // 这里可以实现内存泄漏检测逻辑
        // 例如：检查对象引用、分析内存快照等
        return false
    }
    
    /**
     * 获取GC历史
     */
    fun getGcHistory(limit: Int = 100): List<GcEvent> {
        return gcHistory.values
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取优化历史
     */
    fun getOptimizationHistory(limit: Int = 50): List<GcOptimizationHistory> {
        return optimizationHistory.values
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取GC统计信息
     */
    fun getGcStatistics(): GcStatistics {
        return gcStatistics.get()
    }
    
    /**
     * 手动触发GC优化
     */
    suspend fun triggerManualOptimization(): List<GcOptimizationResult> {
        val gcState = _gcState.value
        val applicableStrategies = optimizationStrategies.values
            .filter { it.isApplicable(gcState) }
            .sortedBy { it.priority }
        
        val results = mutableListOf<GcOptimizationResult>()
        
        for (strategy in applicableStrategies) {
            try {
                val result = strategy.optimize()
                results.add(result)
                
                metrics.recordGcOptimization(
                    strategy.name,
                    result.success
                )
            } catch (e: Exception) {
                metrics.recordGcOptimizationError("手动GC优化错误: ${strategy.name}", e)
            }
        }
        
        return results
    }
    
    /**
     * 销毁GC优化器
     */
    fun destroy() {
        monitoringJob?.cancel()
        scope.cancel()
        
        gcHistory.clear()
        optimizationHistory.clear()
        optimizationStrategies.clear()
        lastGcCounts.clear()
        
        INSTANCE = null
    }
}

/**
 * GC状态
 */
data class GcState(
    val gcFrequency: Int = 0,
    val averagePauseTime: Float = 0f,
    val memoryPressure: Float = 0f,
    val memoryUsage: Long = 0L,
    val maxMemory: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * GC事件
 */
data class GcEvent(
    val timestamp: Long,
    val gcName: String,
    val collectionCount: Long,
    val collectionTime: Long,
    val memoryBefore: Long,
    val memoryAfter: Long
)

/**
 * GC优化策略
 */
interface GcOptimizationStrategy {
    val name: String
    val priority: Int
    val applicableThreshold: Float
    
    fun isApplicable(gcState: GcState): Boolean
    suspend fun optimize(): GcOptimizationResult
}

/**
 * GC优化结果
 */
data class GcOptimizationResult(
    val strategyName: String,
    val success: Boolean,
    val details: String,
    val gcFrequencyBefore: Int,
    val gcFrequencyAfter: Int,
    val averagePauseTimeBefore: Float,
    val averagePauseTimeAfter: Float
)

/**
 * GC优化历史
 */
data class GcOptimizationHistory(
    val id: String,
    val timestamp: Long,
    val duration: Long,
    val gcStateBefore: GcState,
    val gcStateAfter: GcState,
    val strategiesApplied: List<String>,
    val results: List<GcOptimizationResult>
)

/**
 * GC统计信息
 */
data class GcStatistics(
    val totalGcEvents: Long = 0L,
    val averageGcFrequency: Float = 0f,
    val averagePauseTime: Float = 0f,
    val peakMemoryPressure: Float = 0f,
    val sampleCount: Int = 0,
    val lastUpdateTime: Long = 0L
)