package com.empathy.ai.domain.util

import android.os.Debug
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 性能监控器
 *
 * 提供应用性能监控功能
 *
 * 参考标准：
 * - [SD-00001] 代码规范和编码标准
 * - [AD-00001] 架构设计文档
 * - [TDD-00004] 联系人画像记忆系统UI架构设计
 *
 * 职责：
 * - 监控内存使用
 * - 记录操作耗时
 * - 检测性能问题
 * - 生成性能报告
 * - 自动降级建议（CR-00009改进）
 *
 * 使用示例：
 * ```kotlin
 * // 监控操作耗时
 * val duration = performanceMonitor.measureOperation("loadData") {
 *     // 执行操作
 * }
 *
 * // 检查内存
 * val memoryInfo = performanceMonitor.checkMemory()
 *
 * // 检查是否需要降级
 * if (performanceMonitor.shouldDegrade()) {
 *     // 切换到简化视图
 * }
 * ```
 */
@Singleton
class PerformanceMonitor @Inject constructor() {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MEMORY_HEALTHY_THRESHOLD_PERCENT = 80
    }
    
    // 监控状态
    private var isMonitoring = false
    
    /**
     * 辅助构造函数，用于兼容旧代码（如 FloatingWindowService）
     * @param context Android Context（当前未使用，保留用于未来扩展）
     */
    @Suppress("UNUSED_PARAMETER")
    constructor(context: android.content.Context) : this()
    
    // ========== 状态 ==========
    
    private val _memoryState = MutableStateFlow(MemoryInfo())
    val memoryState: StateFlow<MemoryInfo> = _memoryState.asStateFlow()
    
    private val _performanceState = MutableStateFlow(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()
    
    // 操作计时器 - 使用 @PublishedApi internal 以支持 inline 函数访问
    @PublishedApi
    internal val operationTimers = ConcurrentHashMap<String, Long>()
    
    // 性能记录
    @PublishedApi
    internal val performanceRecords = ConcurrentHashMap<String, MutableList<Long>>()
    
    // ========== 内存监控 ==========
    
    /**
     * 检查当前内存使用情况
     */
    fun checkMemory(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val availableMemory = maxMemory - usedMemory
        
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        
        val info = MemoryInfo(
            usedMemoryMb = usedMemory,
            maxMemoryMb = maxMemory,
            availableMemoryMb = availableMemory,
            nativeHeapMb = nativeHeap,
            isWarning = usedMemory >= PerformanceMetrics.MEMORY_WARNING_MB,
            isExceeded = usedMemory >= PerformanceMetrics.MAX_MEMORY_MB
        )
        
        _memoryState.value = info
        
        if (info.isExceeded) {
            Log.w(TAG, "内存使用超限: ${usedMemory}MB / ${maxMemory}MB")
        } else if (info.isWarning) {
            Log.w(TAG, "内存使用警告: ${usedMemory}MB / ${maxMemory}MB")
        }
        
        return info
    }
    
    /**
     * 建议进行垃圾回收
     */
    fun suggestGc() {
        val memoryInfo = checkMemory()
        if (memoryInfo.isWarning) {
            System.gc()
            Log.d(TAG, "建议GC已执行")
        }
    }
    
    // ========== 监控生命周期方法（兼容 FloatingWindowService） ==========
    
    /**
     * 启动性能监控
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "性能监控已在运行中")
            return
        }
        isMonitoring = true
        clearRecords()
        Log.d(TAG, "性能监控已启动")
    }
    
    /**
     * 停止性能监控并返回报告
     * @return 性能报告
     */
    fun stopMonitoring(): PerformanceReport {
        isMonitoring = false
        val report = generateReport()
        Log.d(TAG, "性能监控已停止")
        return report
    }
    
    /**
     * 检查内存是否健康
     * @return true 如果内存使用率低于阈值
     */
    fun isMemoryHealthy(): Boolean {
        val memoryInfo = checkMemory()
        return memoryInfo.usagePercent < MEMORY_HEALTHY_THRESHOLD_PERCENT
    }
    
    /**
     * 请求垃圾回收
     */
    fun requestGarbageCollection() {
        Log.d(TAG, "请求垃圾回收")
        System.gc()
        // 等待一小段时间让GC有机会执行
        Thread.sleep(50)
        val memoryInfo = checkMemory()
        Log.d(TAG, "GC后内存使用: ${memoryInfo.usedMemoryMb}MB (${memoryInfo.usagePercent}%)")
    }
    
    // ========== 操作计时 ==========
    
    /**
     * 开始计时
     *
     * @param operationName 操作名称
     */
    fun startTimer(operationName: String) {
        operationTimers[operationName] = System.currentTimeMillis()
    }
    
    /**
     * 结束计时并返回耗时
     *
     * @param operationName 操作名称
     * @return 耗时（毫秒），如果没有开始计时则返回-1
     */
    fun endTimer(operationName: String): Long {
        val startTime = operationTimers.remove(operationName) ?: return -1
        val duration = System.currentTimeMillis() - startTime
        
        recordPerformance(operationName, duration)
        
        return duration
    }
    
    /**
     * 测量操作耗时
     *
     * @param operationName 操作名称
     * @param block 要执行的操作
     * @return 操作结果
     */
    fun <T> measureOperation(operationName: String, block: () -> T): T {
        startTimer(operationName)
        try {
            return block()
        } finally {
            val duration = endTimer(operationName)
            if (duration > PerformanceMetrics.MAX_OPERATION_TIME_MS) {
                Log.w(TAG, "操作耗时过长: $operationName = ${duration}ms")
            }
        }
    }
    
    /**
     * 测量挂起操作耗时
     */
    suspend fun <T> measureSuspendOperation(
        operationName: String,
        block: suspend () -> T
    ): T {
        startTimer(operationName)
        try {
            return block()
        } finally {
            val duration = endTimer(operationName)
            if (duration > PerformanceMetrics.MAX_OPERATION_TIME_MS) {
                Log.w(TAG, "操作耗时过长: $operationName = ${duration}ms")
            }
        }
    }
    
    // ========== 性能记录 ==========
    
    /**
     * 记录性能数据
     */
    @PublishedApi
    internal fun recordPerformance(operationName: String, duration: Long) {
        val records = performanceRecords.getOrPut(operationName) { mutableListOf() }
        records.add(duration)
        
        // 只保留最近100条记录
        if (records.size > 100) {
            records.removeAt(0)
        }
        
        updatePerformanceState()
    }
    
    /**
     * 更新性能状态
     */
    private fun updatePerformanceState() {
        val stats = performanceRecords.mapValues { (_, records) ->
            if (records.isEmpty()) {
                OperationStats()
            } else {
                OperationStats(
                    count = records.size,
                    avgDuration = records.average().toLong(),
                    minDuration = records.minOrNull() ?: 0,
                    maxDuration = records.maxOrNull() ?: 0
                )
            }
        }
        
        _performanceState.value = PerformanceState(operationStats = stats)
    }
    
    // ========== 帧率监控 ==========
    
    private var lastFrameTime = 0L
    private val frameTimesMs = mutableListOf<Long>()
    
    /**
     * 记录帧时间
     */
    fun recordFrameTime() {
        val currentTime = System.nanoTime()
        if (lastFrameTime > 0) {
            val frameTimeMs = (currentTime - lastFrameTime) / 1_000_000
            frameTimesMs.add(frameTimeMs)
            
            // 只保留最近60帧
            if (frameTimesMs.size > 60) {
                frameTimesMs.removeAt(0)
            }
            
            if (frameTimeMs > PerformanceMetrics.FRAME_TIME_WARNING_MS) {
                Log.w(TAG, "帧时间过长: ${frameTimeMs}ms")
            }
        }
        lastFrameTime = currentTime
    }
    
    /**
     * 获取当前帧率
     */
    fun getCurrentFps(): Int {
        if (frameTimesMs.isEmpty()) return 0
        val avgFrameTime = frameTimesMs.average()
        return if (avgFrameTime > 0) (1000 / avgFrameTime).toInt() else 0
    }
    
    // ========== 性能报告 ==========
    
    /**
     * 生成性能报告
     */
    fun generateReport(): PerformanceReport {
        val memoryInfo = checkMemory()
        val fps = getCurrentFps()
        
        return PerformanceReport(
            memoryInfo = memoryInfo,
            currentFps = fps,
            operationStats = _performanceState.value.operationStats,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 清除所有记录
     */
    fun clearRecords() {
        operationTimers.clear()
        performanceRecords.clear()
        frameTimesMs.clear()
        lastFrameTime = 0L
        consecutiveSlowFrames = 0
        _performanceState.value = PerformanceState()
    }
    
    // ========== 性能降级建议（CR-00009改进） ==========
    
    private var consecutiveSlowFrames = 0
    private val SLOW_FRAME_THRESHOLD = 3
    
    /**
     * 检查是否应该降级
     *
     * 降级条件：
     * - 内存使用超过警告阈值
     * - 连续3帧以上帧时间超过阈值
     * - 平均帧率低于最低可接受帧率
     *
     * @return 是否建议降级
     */
    fun shouldDegrade(): Boolean {
        val memoryInfo = checkMemory()
        val fps = getCurrentFps()
        
        // 内存超限
        if (memoryInfo.isExceeded) {
            Log.w(TAG, "建议降级：内存使用超限 ${memoryInfo.usedMemoryMb}MB")
            return true
        }
        
        // 帧率过低
        if (fps > 0 && fps < PerformanceMetrics.MIN_ACCEPTABLE_FPS) {
            Log.w(TAG, "建议降级：帧率过低 ${fps}fps")
            return true
        }
        
        // 连续慢帧
        if (consecutiveSlowFrames >= SLOW_FRAME_THRESHOLD) {
            Log.w(TAG, "建议降级：连续${consecutiveSlowFrames}帧超时")
            return true
        }
        
        return false
    }
    
    /**
     * 记录慢帧
     *
     * @param frameTimeMs 帧时间（毫秒）
     */
    fun recordSlowFrame(frameTimeMs: Long) {
        if (frameTimeMs > PerformanceMetrics.FRAME_TIME_WARNING_MS) {
            consecutiveSlowFrames++
        } else {
            consecutiveSlowFrames = maxOf(0, consecutiveSlowFrames - 1)
        }
    }
    
    /**
     * 获取降级建议
     *
     * @return 降级建议列表
     */
    fun getDegradationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val memoryInfo = checkMemory()
        val fps = getCurrentFps()
        
        if (memoryInfo.isWarning) {
            suggestions.add("减少图片缓存大小")
            suggestions.add("清理不必要的数据")
        }
        
        if (fps > 0 && fps < PerformanceMetrics.TARGET_FPS) {
            suggestions.add("禁用动画效果")
            suggestions.add("使用简化视图")
        }
        
        if (consecutiveSlowFrames > 0) {
            suggestions.add("减少列表项数量")
            suggestions.add("延迟加载图片")
        }
        
        return suggestions
    }
    
    // ========== 内存泄漏检测（CR-00010改进） ==========
    
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private val MEMORY_LEAK_THRESHOLD_MB = 10L
    private val MAX_SNAPSHOTS = 10
    
    /**
     * 内存快照
     */
    data class MemorySnapshot(
        val timestamp: Long,
        val usedMemoryMb: Long,
        val operation: String
    )
    
    /**
     * 监控内存使用变化
     *
     * 在操作前后记录内存使用，检测潜在的内存泄漏
     *
     * @param operation 操作名称
     * @param block 要执行的操作
     * @return 操作结果
     */
    fun <T> monitorMemoryUsage(operation: String, block: () -> T): T {
        val beforeMemory = getUsedMemoryMb()
        
        try {
            return block()
        } finally {
            val afterMemory = getUsedMemoryMb()
            val memoryIncrease = afterMemory - beforeMemory
            
            // 记录内存快照
            recordMemorySnapshot(operation, afterMemory)
            
            // 检测内存泄漏
            if (memoryIncrease > MEMORY_LEAK_THRESHOLD_MB) {
                Log.w(TAG, "潜在内存泄漏: $operation 增加了 ${memoryIncrease}MB")
                onMemoryLeakDetected(operation, memoryIncrease)
            }
        }
    }
    
    /**
     * 获取当前已用内存（MB）
     */
    fun getUsedMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }
    
    /**
     * 记录内存快照
     */
    @PublishedApi
    internal fun recordMemorySnapshot(operation: String, usedMemoryMb: Long) {
        memorySnapshots.add(
            MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                usedMemoryMb = usedMemoryMb,
                operation = operation
            )
        )
        
        // 只保留最近的快照
        if (memorySnapshots.size > MAX_SNAPSHOTS) {
            memorySnapshots.removeAt(0)
        }
    }
    
    /**
     * 内存泄漏检测回调 - 使用 @PublishedApi internal 以支持 inline 函数访问
     * 
     * 直接通过属性赋值设置回调：
     * performanceMonitor.memoryLeakCallback = { operation, increaseMb -> ... }
     */
    @PublishedApi
    internal var memoryLeakCallback: ((String, Long) -> Unit)? = null
    
    /**
     * 内存泄漏检测处理
     */
    @PublishedApi
    internal fun onMemoryLeakDetected(operation: String, increaseMb: Long) {
        memoryLeakCallback?.invoke(operation, increaseMb)
    }
    
    /**
     * 获取内存趋势
     *
     * @return 内存是否持续增长
     */
    fun isMemoryTrendIncreasing(): Boolean {
        if (memorySnapshots.size < 3) return false
        
        val recentSnapshots = memorySnapshots.takeLast(5)
        var increasingCount = 0
        
        for (i in 1 until recentSnapshots.size) {
            if (recentSnapshots[i].usedMemoryMb > recentSnapshots[i - 1].usedMemoryMb) {
                increasingCount++
            }
        }
        
        return increasingCount >= (recentSnapshots.size - 1) * 0.8
    }
    
    // ========== 网络请求监控（CR-00010改进） ==========
    
    @PublishedApi
    internal val networkRequests = ConcurrentHashMap<String, NetworkRequestInfo>()
    @PublishedApi
    internal val NETWORK_TIMEOUT_THRESHOLD_MS = 5000L
    
    /**
     * 网络请求信息
     */
    data class NetworkRequestInfo(
        val url: String,
        val startTime: Long,
        var endTime: Long = 0,
        var success: Boolean = false,
        var errorMessage: String? = null
    ) {
        val duration: Long
            get() = if (endTime > 0) endTime - startTime else System.currentTimeMillis() - startTime
    }
    
    /**
     * 监控网络请求
     *
     * @param url 请求URL
     * @param block 请求操作
     * @return 请求结果
     */
    suspend fun <T> monitorNetworkRequest(
        url: String,
        block: suspend () -> T
    ): T {
        val requestId = "${url}_${System.currentTimeMillis()}"
        val requestInfo = NetworkRequestInfo(url = url, startTime = System.currentTimeMillis())
        networkRequests[requestId] = requestInfo
        
        try {
            val result = block()
            requestInfo.endTime = System.currentTimeMillis()
            requestInfo.success = true
            
            // 检查是否超时
            if (requestInfo.duration > NETWORK_TIMEOUT_THRESHOLD_MS) {
                Log.w(TAG, "网络请求较慢: $url 耗时 ${requestInfo.duration}ms")
            }
            
            return result
        } catch (e: Exception) {
            requestInfo.endTime = System.currentTimeMillis()
            requestInfo.success = false
            requestInfo.errorMessage = e.message
            Log.e(TAG, "网络请求失败: $url - ${e.message}")
            throw e
        } finally {
            // 清理旧的请求记录
            cleanupOldNetworkRequests()
        }
    }
    
    /**
     * 清理旧的网络请求记录
     */
    @PublishedApi
    internal fun cleanupOldNetworkRequests() {
        val cutoffTime = System.currentTimeMillis() - 60000 // 保留最近1分钟的记录
        networkRequests.entries.removeIf { it.value.startTime < cutoffTime }
    }
    
    /**
     * 获取网络请求统计
     */
    fun getNetworkStats(): NetworkStats {
        val requests = networkRequests.values.toList()
        if (requests.isEmpty()) {
            return NetworkStats()
        }
        
        val completedRequests = requests.filter { it.endTime > 0 }
        val successfulRequests = completedRequests.filter { it.success }
        val failedRequests = completedRequests.filter { !it.success }
        
        return NetworkStats(
            totalRequests = requests.size,
            completedRequests = completedRequests.size,
            successfulRequests = successfulRequests.size,
            failedRequests = failedRequests.size,
            avgDurationMs = if (completedRequests.isNotEmpty()) {
                completedRequests.map { it.duration }.average().toLong()
            } else 0,
            slowRequests = completedRequests.count { it.duration > NETWORK_TIMEOUT_THRESHOLD_MS }
        )
    }
    
    /**
     * 网络统计信息
     */
    data class NetworkStats(
        val totalRequests: Int = 0,
        val completedRequests: Int = 0,
        val successfulRequests: Int = 0,
        val failedRequests: Int = 0,
        val avgDurationMs: Long = 0,
        val slowRequests: Int = 0
    ) {
        val successRate: Float
            get() = if (completedRequests > 0) {
                successfulRequests.toFloat() / completedRequests
            } else 0f
    }
}

/**
 * 内存信息
 */
data class MemoryInfo(
    val usedMemoryMb: Long = 0,
    val maxMemoryMb: Long = 0,
    val availableMemoryMb: Long = 0,
    val nativeHeapMb: Long = 0,
    val isWarning: Boolean = false,
    val isExceeded: Boolean = false
) {
    val usagePercent: Int
        get() = if (maxMemoryMb > 0) ((usedMemoryMb * 100) / maxMemoryMb).toInt() else 0
}

/**
 * 操作统计
 */
data class OperationStats(
    val count: Int = 0,
    val avgDuration: Long = 0,
    val minDuration: Long = 0,
    val maxDuration: Long = 0
)

/**
 * 性能状态
 */
data class PerformanceState(
    val operationStats: Map<String, OperationStats> = emptyMap()
)

/**
 * 性能报告
 */
data class PerformanceReport(
    val memoryInfo: MemoryInfo,
    val currentFps: Int,
    val operationStats: Map<String, OperationStats>,
    val timestamp: Long
) {
    override fun toString(): String {
        return buildString {
            appendLine("=== 性能报告 ===")
            appendLine("时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)}")
            appendLine()
            appendLine("内存使用:")
            appendLine("  已用: ${memoryInfo.usedMemoryMb}MB / ${memoryInfo.maxMemoryMb}MB (${memoryInfo.usagePercent}%)")
            appendLine("  可用: ${memoryInfo.availableMemoryMb}MB")
            appendLine("  Native堆: ${memoryInfo.nativeHeapMb}MB")
            appendLine("  状态: ${if (memoryInfo.isExceeded) "超限" else if (memoryInfo.isWarning) "警告" else "正常"}")
            appendLine()
            appendLine("帧率: ${currentFps}fps")
            appendLine()
            if (operationStats.isNotEmpty()) {
                appendLine("操作统计:")
                operationStats.forEach { (name, stats) ->
                    appendLine("  $name: 平均${stats.avgDuration}ms, 最小${stats.minDuration}ms, 最大${stats.maxDuration}ms (${stats.count}次)")
                }
            }
        }
    }
}
