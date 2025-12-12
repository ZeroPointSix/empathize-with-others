package com.empathy.ai.domain.util

import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 性能监控工具
 * 
 * 职责：
 * - 监控内存使用情况
 * - 检测内存泄漏
 * - 提供性能指标
 * 
 * 使用方式：
 * ```
 * val monitor = PerformanceMonitor(context)
 * monitor.startMonitoring()
 * // ... 执行操作
 * monitor.stopMonitoring()
 * ```
 */
class PerformanceMonitor(private val context: Context) {
    
    private var monitoringJob: Job? = null
    private var startTime: Long = 0
    private var peakMemoryUsage: Long = 0
    
    /**
     * 开始监控
     * 
     * 每隔 5 秒检查一次内存使用情况
     */
    fun startMonitoring() {
        startTime = System.currentTimeMillis()
        peakMemoryUsage = 0
        
        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val memoryInfo = getMemoryInfo()
                
                // 更新峰值内存使用
                if (memoryInfo.usedMemoryMB > peakMemoryUsage) {
                    peakMemoryUsage = memoryInfo.usedMemoryMB
                }
                
                // 检查内存使用是否超过阈值（150MB）
                if (memoryInfo.usedMemoryMB > MEMORY_THRESHOLD_MB) {
                    Log.w(TAG, "内存使用超过阈值: ${memoryInfo.usedMemoryMB}MB")
                    onMemoryWarning(memoryInfo)
                }
                
                // 记录内存使用情况
                Log.d(TAG, "内存使用: ${memoryInfo.usedMemoryMB}MB / ${memoryInfo.totalMemoryMB}MB")
                
                // 等待 5 秒
                delay(MONITORING_INTERVAL_MS)
            }
        }
    }
    
    /**
     * 停止监控
     * 
     * @return 性能报告
     */
    fun stopMonitoring(): PerformanceReport {
        monitoringJob?.cancel()
        monitoringJob = null
        
        val duration = System.currentTimeMillis() - startTime
        val currentMemory = getMemoryInfo()
        
        return PerformanceReport(
            durationMs = duration,
            peakMemoryMB = peakMemoryUsage,
            currentMemoryMB = currentMemory.usedMemoryMB,
            totalMemoryMB = currentMemory.totalMemoryMB
        )
    }
    
    /**
     * 获取当前内存信息
     * 
     * @return 内存信息
     */
    fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val totalMemory = runtime.totalMemory()
        val maxMemory = runtime.maxMemory()
        
        // 获取 Native 内存使用（可选）
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()
        
        return MemoryInfo(
            usedMemoryMB = usedMemory / (1024 * 1024),
            totalMemoryMB = totalMemory / (1024 * 1024),
            maxMemoryMB = maxMemory / (1024 * 1024),
            nativeHeapMB = nativeHeapAllocated / (1024 * 1024)
        )
    }
    
    /**
     * 检查内存使用是否健康
     * 
     * @return true 如果内存使用正常，false 如果超过阈值
     */
    fun isMemoryHealthy(): Boolean {
        val memoryInfo = getMemoryInfo()
        return memoryInfo.usedMemoryMB < MEMORY_THRESHOLD_MB
    }
    
    /**
     * 触发垃圾回收
     * 
     * 注意：这只是建议 JVM 进行垃圾回收，不保证立即执行
     */
    fun requestGarbageCollection() {
        Log.d(TAG, "请求垃圾回收")
        System.gc()
    }
    
    /**
     * 内存警告回调
     * 
     * 当内存使用超过阈值时调用
     * 
     * @param memoryInfo 内存信息
     */
    private fun onMemoryWarning(memoryInfo: MemoryInfo) {
        Log.w(TAG, "内存警告: 使用 ${memoryInfo.usedMemoryMB}MB，超过阈值 ${MEMORY_THRESHOLD_MB}MB")
        
        // 建议进行垃圾回收
        requestGarbageCollection()
    }
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        
        /**
         * 内存使用阈值（MB）
         * 
         * 根据需求 6.1，内存占用应低于 150MB
         */
        private const val MEMORY_THRESHOLD_MB = 150L
        
        /**
         * 监控间隔（毫秒）
         */
        private const val MONITORING_INTERVAL_MS = 5000L
    }
}

/**
 * 内存信息
 * 
 * @property usedMemoryMB 已使用内存（MB）
 * @property totalMemoryMB 总内存（MB）
 * @property maxMemoryMB 最大内存（MB）
 * @property nativeHeapMB Native 堆内存（MB）
 */
data class MemoryInfo(
    val usedMemoryMB: Long,
    val totalMemoryMB: Long,
    val maxMemoryMB: Long,
    val nativeHeapMB: Long
)

/**
 * 性能报告
 * 
 * @property durationMs 持续时间（毫秒）
 * @property peakMemoryMB 峰值内存使用（MB）
 * @property currentMemoryMB 当前内存使用（MB）
 * @property totalMemoryMB 总内存（MB）
 */
data class PerformanceReport(
    val durationMs: Long,
    val peakMemoryMB: Long,
    val currentMemoryMB: Long,
    val totalMemoryMB: Long
) {
    override fun toString(): String {
        return """
            性能报告:
            - 持续时间: ${durationMs}ms
            - 峰值内存: ${peakMemoryMB}MB
            - 当前内存: ${currentMemoryMB}MB
            - 总内存: ${totalMemoryMB}MB
        """.trimIndent()
    }
}
