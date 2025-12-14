package com.empathy.ai.domain.util

import android.util.Log

/**
 * 记忆系统日志工具类
 *
 * 提供统一的日志记录接口，便于调试和监控
 * 支持动态调整日志级别，生产环境可设置为INFO或WARN减少日志输出
 */
object MemoryLogger {

    private const val TAG_PREFIX = "MEMORY_SYSTEM"

    /**
     * 日志级别枚举
     * 数值越大，级别越高
     */
    enum class Level(val value: Int) {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3)
    }

    /**
     * 当前日志级别，默认为DEBUG
     * 只有级别 >= currentLogLevel 的日志才会输出
     */
    @Volatile
    var currentLogLevel: Level = Level.DEBUG
        private set

    /**
     * 设置日志级别
     * 生产环境建议设置为INFO或WARN
     */
    fun setLogLevel(level: Level) {
        currentLogLevel = level
    }

    /**
     * 检查是否应该输出指定级别的日志
     */
    private fun shouldLog(level: Level): Boolean {
        return level.value >= currentLogLevel.value
    }

    /**
     * 记录调试日志
     */
    fun debug(tag: String, message: String) {
        if (shouldLog(Level.DEBUG)) {
            Log.d(formatTag(tag), message)
        }
    }

    /**
     * 记录信息日志
     */
    fun info(tag: String, message: String) {
        if (shouldLog(Level.INFO)) {
            Log.i(formatTag(tag), message)
        }
    }

    /**
     * 记录警告日志
     */
    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.WARN)) {
            if (throwable != null) {
                Log.w(formatTag(tag), message, throwable)
            } else {
                Log.w(formatTag(tag), message)
            }
        }
    }

    /**
     * 记录错误日志
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.ERROR)) {
            if (throwable != null) {
                Log.e(formatTag(tag), message, throwable)
            } else {
                Log.e(formatTag(tag), message)
            }
        }
    }

    /**
     * 记录性能日志（DEBUG级别）
     */
    fun performance(tag: String, operation: String, durationMs: Long) {
        if (shouldLog(Level.DEBUG)) {
            Log.d(formatTag(tag), "[$operation] 耗时: ${durationMs}ms")
        }
    }

    /**
     * 记录操作开始（DEBUG级别）
     */
    fun operationStart(tag: String, operation: String) {
        if (shouldLog(Level.DEBUG)) {
            Log.d(formatTag(tag), "[$operation] 开始执行")
        }
    }

    /**
     * 记录操作结束
     */
    fun operationEnd(tag: String, operation: String, success: Boolean, details: String? = null) {
        val status = if (success) "成功" else "失败"
        val message = if (details != null) {
            "[$operation] 执行$status: $details"
        } else {
            "[$operation] 执行$status"
        }
        if (success) {
            debug(tag, message)
        } else {
            warn(tag, message)
        }
    }

    /**
     * 记录AI调用（INFO级别）
     */
    fun aiCall(tag: String, operation: String, contactId: String, success: Boolean, responseTime: Long? = null) {
        if (shouldLog(Level.INFO)) {
            val timeInfo = if (responseTime != null) ", 响应时间: ${responseTime}ms" else ""
            val status = if (success) "成功" else "失败"
            Log.i(formatTag(tag), "[AI调用] $operation - 联系人: $contactId, 状态: $status$timeInfo")
        }
    }

    /**
     * 记录数据库操作（DEBUG级别）
     */
    fun dbOperation(tag: String, operation: String, table: String, count: Int) {
        if (shouldLog(Level.DEBUG)) {
            Log.d(formatTag(tag), "[数据库] $operation - 表: $table, 影响行数: $count")
        }
    }

    /**
     * 记录内存使用情况（DEBUG级别）
     */
    fun memoryUsage(tag: String) {
        if (shouldLog(Level.DEBUG)) {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            Log.d(formatTag(tag), "[内存] 已使用: ${usedMemory}MB / 最大: ${maxMemory}MB")
        }
    }

    /**
     * 格式化标签
     */
    private fun formatTag(tag: String): String {
        return "$TAG_PREFIX:$tag"
    }

    /**
     * 测量操作耗时
     */
    inline fun <T> measureTime(tag: String, operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block().also {
                val duration = System.currentTimeMillis() - startTime
                performance(tag, operation, duration)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            error(tag, "[$operation] 执行异常，耗时: ${duration}ms", e)
            throw e
        }
    }
}
