package com.empathy.ai.domain.util

import android.util.Log
import com.empathy.ai.BuildConfig

/**
 * 调试日志工具类
 *
 * 用于在调试模式下输出完整的提示词内容，
 * 在 Release 模式下截取输出以保护隐私和性能。
 *
 * 功能：
 * - Debug 模式：输出完整内容，超长文本自动分段
 * - Release 模式：截取前 500 字符输出
 *
 * @see SR-00001 模型列表自动获取与调试日志优化
 */
object DebugLogger {

    /**
     * Android Log 单条日志最大长度
     * 实际限制约 4000 字符，这里使用保守值
     */
    const val MAX_LOG_LENGTH = 4000

    /**
     * Release 模式下的截取长度
     */
    private const val TRUNCATE_LENGTH = 500

    /**
     * 输出完整的提示词日志
     *
     * @param tag 日志标签
     * @param label 内容标签（如 "PromptContext"、"SystemInstruction"）
     * @param content 要输出的内容
     * @param isDebugMode 是否为调试模式，默认使用 BuildConfig.DEBUG
     */
    fun logFullPrompt(
        tag: String,
        label: String,
        content: String,
        isDebugMode: Boolean = BuildConfig.DEBUG
    ) {
        if (!isDebugMode) {
            // Release 模式：截取输出
            logTruncated(tag, label, content)
            return
        }

        // Debug 模式：完整输出
        logFull(tag, label, content)
    }

    /**
     * 截取输出（Release 模式）
     */
    private fun logTruncated(tag: String, label: String, content: String) {
        val truncated = if (content.length > TRUNCATE_LENGTH) {
            content.take(TRUNCATE_LENGTH)
        } else {
            content
        }
        Log.d(tag, "$label (前${TRUNCATE_LENGTH}字符): $truncated")
    }

    /**
     * 完整输出（Debug 模式）
     */
    private fun logFull(tag: String, label: String, content: String) {
        // 输出开始标记
        Log.d(tag, "========== $label 开始 (总长度: ${content.length}) ==========")

        if (content.length <= MAX_LOG_LENGTH) {
            // 内容不超过限制，直接输出
            Log.d(tag, content)
        } else {
            // 内容超过限制，分段输出
            logInSegments(tag, label, content)
        }

        // 输出结束标记
        Log.d(tag, "========== $label 结束 ==========")
    }

    /**
     * 分段输出长文本
     */
    private fun logInSegments(tag: String, label: String, content: String) {
        val segments = content.chunked(MAX_LOG_LENGTH)
        val totalSegments = segments.size

        segments.forEachIndexed { index, segment ->
            val segmentNumber = index + 1
            Log.d(tag, "[$label 第 $segmentNumber/$totalSegments 段]\n$segment")
        }
    }

    /**
     * 输出 API 请求详情日志
     *
     * 专门用于 AiRepositoryImpl 中的 API 请求日志
     *
     * @param tag 日志标签
     * @param method API 方法名（如 "analyzeChat"、"checkDraftSafety"）
     * @param url 请求 URL
     * @param model 模型名称
     * @param providerName 服务商名称
     * @param promptContext 提示词上下文
     * @param systemInstruction 系统指令（可选）
     * @param additionalInfo 额外信息（可选）
     */
    fun logApiRequest(
        tag: String,
        method: String,
        url: String,
        model: String,
        providerName: String,
        promptContext: String,
        systemInstruction: String? = null,
        additionalInfo: Map<String, Any>? = null
    ) {
        val isDebug = BuildConfig.DEBUG

        Log.d(tag, "=== API请求详情 ($method) ===")
        Log.d(tag, "URL: $url")
        Log.d(tag, "Model: $model")
        Log.d(tag, "Provider: $providerName")

        // 输出额外信息
        additionalInfo?.forEach { (key, value) ->
            Log.d(tag, "$key: $value")
        }

        // 输出提示词上下文
        Log.d(tag, "PromptContext长度: ${promptContext.length} 字符")
        logFullPrompt(tag, "PromptContext", promptContext, isDebug)

        // 输出系统指令（如果有）
        systemInstruction?.let {
            Log.d(tag, "SystemInstruction长度: ${it.length} 字符")
            logFullPrompt(tag, "SystemInstruction", it, isDebug)
        }
    }
}
