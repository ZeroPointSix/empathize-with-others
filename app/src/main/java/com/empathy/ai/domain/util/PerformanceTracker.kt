package com.empathy.ai.domain.util

import android.os.Trace
import android.util.Log

/**
 * 性能追踪工具类
 * 
 * 提供性能监控和追踪功能，帮助识别性能瓶颈
 * 
 * 功能：
 * 1. 使用 Trace API 进行详细追踪
 * 2. 记录操作耗时
 * 3. 当操作超过阈值时记录警告
 * 
 * 使用示例：
 * ```kotlin
 * PerformanceTracker.trace("SearchContacts") {
 *     performSearch(query)
 * }
 * ```
 */
object PerformanceTracker {
    
    private const val TAG = "PerformanceTracker"
    
    /**
     * 是否启用性能追踪
     * 在测试环境中可以禁用以避免 Android API 调用
     */
    var enabled = true
    
    /**
     * 性能指标阈值（毫秒）
     */
    object Thresholds {
        const val SEARCH_OPERATION = 300L      // 搜索操作：300ms
        const val TAG_OPERATION = 500L         // 标签操作：500ms
        const val DATABASE_OPERATION = 100L    // 数据库操作：100ms
        const val UI_RENDER = 16L              // UI 渲染：16ms (60 FPS)
    }
    
    /**
     * 追踪代码块的执行时间
     * 
     * 使用 Trace API 进行系统级追踪，可在 Android Profiler 中查看
     * 
     * @param sectionName 追踪区段名称
     * @param threshold 性能阈值（毫秒），超过此值会记录警告
     * @param block 要追踪的代码块
     * @return 代码块的返回值
     */
    fun <T> trace(
        sectionName: String,
        threshold: Long = 0L,
        block: () -> T
    ): T {
        if (!enabled) {
            return block()
        }
        
        val startTime = System.currentTimeMillis()
        try {
            Trace.beginSection(sectionName)
        } catch (e: Exception) {
            // 在测试环境中可能失败，忽略
        }
        
        return try {
            block()
        } finally {
            try {
                Trace.endSection()
            } catch (e: Exception) {
                // 在测试环境中可能失败，忽略
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            // 如果设置了阈值且超过阈值，记录警告
            try {
                if (threshold > 0 && duration > threshold) {
                    Log.w(TAG, "$sectionName 耗时 ${duration}ms，超过阈值 ${threshold}ms")
                } else {
                    Log.d(TAG, "$sectionName 耗时 ${duration}ms")
                }
            } catch (e: Exception) {
                // 在测试环境中 Log 可能不可用，忽略
                println("$sectionName 耗时 ${duration}ms")
            }
        }
    }
    
    /**
     * 追踪挂起函数的执行时间
     * 
     * @param sectionName 追踪区段名称
     * @param threshold 性能阈值（毫秒）
     * @param block 要追踪的挂起代码块
     * @return 代码块的返回值
     */
    suspend fun <T> traceSuspend(
        sectionName: String,
        threshold: Long = 0L,
        block: suspend () -> T
    ): T {
        if (!enabled) {
            return block()
        }
        
        val startTime = System.currentTimeMillis()
        try {
            Trace.beginSection(sectionName)
        } catch (e: Exception) {
            // 在测试环境中可能失败，忽略
        }
        
        return try {
            block()
        } finally {
            try {
                Trace.endSection()
            } catch (e: Exception) {
                // 在测试环境中可能失败，忽略
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            try {
                if (threshold > 0 && duration > threshold) {
                    Log.w(TAG, "$sectionName 耗时 ${duration}ms，超过阈值 ${threshold}ms")
                } else {
                    Log.d(TAG, "$sectionName 耗时 ${duration}ms")
                }
            } catch (e: Exception) {
                // 在测试环境中 Log 可能不可用，忽略
                println("$sectionName 耗时 ${duration}ms")
            }
        }
    }
    
    /**
     * 记录性能指标
     * 
     * @param operation 操作名称
     * @param duration 操作耗时（毫秒）
     * @param threshold 性能阈值（毫秒）
     */
    fun logMetric(operation: String, duration: Long, threshold: Long) {
        if (!enabled) return
        
        try {
            if (duration > threshold) {
                Log.w(TAG, "性能警告：$operation 耗时 ${duration}ms（阈值：${threshold}ms）")
            } else {
                Log.d(TAG, "性能：$operation 耗时 ${duration}ms")
            }
        } catch (e: Exception) {
            println("性能：$operation 耗时 ${duration}ms")
        }
    }
    
    /**
     * 开始性能追踪
     * 
     * 返回开始时间，用于后续计算耗时
     * 
     * @param operation 操作名称
     * @return 开始时间（毫秒）
     */
    fun start(operation: String): Long {
        val startTime = System.currentTimeMillis()
        if (!enabled) return startTime
        
        try {
            Trace.beginSection(operation)
            Log.d(TAG, "开始：$operation")
        } catch (e: Exception) {
            println("开始：$operation")
        }
        return startTime
    }
    
    /**
     * 结束性能追踪
     * 
     * @param operation 操作名称
     * @param startTime 开始时间（毫秒）
     * @param threshold 性能阈值（毫秒）
     */
    fun end(operation: String, startTime: Long, threshold: Long = 0L) {
        if (!enabled) return
        
        try {
            Trace.endSection()
        } catch (e: Exception) {
            // 忽略
        }
        val duration = System.currentTimeMillis() - startTime
        logMetric(operation, duration, threshold)
    }
}
