package com.empathy.ai.presentation.util

import android.util.Log

/**
 * 调试日志工具类（Presentation层）
 *
 * 用于在调试模式下输出日志信息
 * 这是presentation模块的本地实现，避免依赖data层
 */
object DebugLogger {

    const val MAX_LOG_LENGTH = 4000
    private const val TRUNCATE_LENGTH = 500

    /**
     * Debug级别日志
     */
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    /**
     * Warning级别日志
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    /**
     * Error级别日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Info级别日志
     */
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    /**
     * Verbose级别日志
     */
    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }

    /**
     * 输出完整的提示词日志
     */
    fun logFullPrompt(
        tag: String,
        label: String,
        content: String,
        isDebugMode: Boolean = true
    ) {
        if (!isDebugMode) {
            logTruncated(tag, label, content)
            return
        }
        logFull(tag, label, content)
    }

    private fun logTruncated(tag: String, label: String, content: String) {
        val truncated = if (content.length > TRUNCATE_LENGTH) {
            content.take(TRUNCATE_LENGTH)
        } else {
            content
        }
        Log.d(tag, "$label (前${TRUNCATE_LENGTH}字符): $truncated")
    }

    private fun logFull(tag: String, label: String, content: String) {
        Log.d(tag, "========== $label 开始 (总长度: ${content.length}) ==========")
        if (content.length <= MAX_LOG_LENGTH) {
            Log.d(tag, content)
        } else {
            logInSegments(tag, label, content)
        }
        Log.d(tag, "========== $label 结束 ==========")
    }

    private fun logInSegments(tag: String, label: String, content: String) {
        val segments = content.chunked(MAX_LOG_LENGTH)
        val totalSegments = segments.size
        segments.forEachIndexed { index, segment ->
            val segmentNumber = index + 1
            Log.d(tag, "[$label 第 $segmentNumber/$totalSegments 段]\n$segment")
        }
    }
}
