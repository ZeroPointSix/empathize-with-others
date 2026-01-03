package com.empathy.ai.data.util

import android.util.Log

/**
 * 调试日志工具类
 *
 * 用于在调试模式下输出完整的提示词内容
 */
object DebugLogger {

    const val MAX_LOG_LENGTH = 4000
    private const val TRUNCATE_LENGTH = 500

    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }

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

    fun logApiRequest(
        tag: String,
        method: String,
        url: String,
        model: String,
        providerName: String,
        promptContext: String,
        systemInstruction: String? = null,
        additionalInfo: Map<String, Any>? = null,
        temperature: Float? = null,
        maxTokens: Int? = null
    ) {
        Log.d(tag, "╔══════════════════════════════════════════════════════════════")
        Log.d(tag, "║ 🚀 API请求详情 ($method)")
        Log.d(tag, "╠══════════════════════════════════════════════════════════════")
        Log.d(tag, "║ 📍 URL: $url")
        Log.d(tag, "║ 🤖 Model: $model")
        Log.d(tag, "║ 🏢 Provider: $providerName")
        Log.d(tag, "╠══════════════════════════════════════════════════════════════")
        Log.d(tag, "║ ⚙️ 高级参数配置:")
        Log.d(tag, "║    🌡️ Temperature: ${temperature ?: "默认(0.7)"}")
        Log.d(tag, "║    📊 MaxTokens: ${maxTokens ?: "未设置(无限制)"}")
        Log.d(tag, "╠══════════════════════════════════════════════════════════════")
        additionalInfo?.forEach { (key, value) ->
            Log.d(tag, "║ 📌 $key: $value")
        }
        Log.d(tag, "║ 📝 PromptContext长度: ${promptContext.length} 字符")
        Log.d(tag, "╚══════════════════════════════════════════════════════════════")
        logFullPrompt(tag, "PromptContext", promptContext, true)
        systemInstruction?.let {
            Log.d(tag, "SystemInstruction长度: ${it.length} 字符")
            logFullPrompt(tag, "SystemInstruction", it, true)
        }
    }
}
