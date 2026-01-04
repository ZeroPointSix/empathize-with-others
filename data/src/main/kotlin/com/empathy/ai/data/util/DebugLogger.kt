package com.empathy.ai.data.util

import android.util.Log

/**
 * DebugLogger 实现了调试日志工具类
 *
 * 核心价值：解决 Android Logcat 的 4000 字符限制问题
 *
 * 技术背景：
 * - Logcat 单条日志最大 4000 字符
 * - AI 提示词可能长达数万字符
 * - 需要分段输出完整提示词用于调试
 *
 * 业务用途：
 * - 调试时查看 AI 完整提示词
 * - 排查 AI 回复质量问题
 * - 验证提示词模板替换是否正确
 * - 记录 API 请求参数和响应
 *
 * 设计决策：
 * - 分段策略：按 4000 字符分段输出
 * - 开发模式：输出完整内容
 * - 生产模式：只输出前 500 字符
 * - 使用框线格式便于阅读
 *
 * @see AiRepositoryImpl AI仓库（主要使用此工具）
 */
object DebugLogger {

    /**
     * Logcat 单条日志最大长度
     *
     * Android 系统限制，超出会被截断
     */
    const val MAX_LOG_LENGTH = 4000

    /**
     * 生产模式截断长度
     *
     * 只输出前 500 字符，避免日志过多
     */
    private const val TRUNCATE_LENGTH = 500

    /**
     * DEBUG 级别日志
     */
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    /**
     * WARN 级别日志
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    /**
     * ERROR 级别日志
     *
     * @param throwable 可选的异常堆栈
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * INFO 级别日志
     */
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    /**
     * VERBOSE 级别日志
     */
    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }

    /**
     * 记录完整提示词内容
     *
     * 【模式选择】
     * - isDebugMode = true：输出完整内容（开发调试用）
     * - isDebugMode = false：只输出前500字符（生产环境用）
     *
     * @param tag 日志标签
     * @param label 内容标签（如 "PromptContext"、"SystemInstruction"）
     * @param content 完整内容
     * @param isDebugMode 是否开发模式
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

    /**
     * 截断输出（生产模式）
     *
     * @param tag 日志标签
     * @param label 内容标签
     * @param content 完整内容
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
     * 完整输出（开发模式）
     *
     * 【处理逻辑】
     * - 如果内容 ≤ 4000 字符：直接输出
     * - 如果内容 > 4000 字符：分段输出
     *
     * @param tag 日志标签
     * @param label 内容标签
     * @param content 完整内容
     */
    private fun logFull(tag: String, label: String, content: String) {
        Log.d(tag, "========== $label 开始 (总长度: ${content.length}) ==========")
        if (content.length <= MAX_LOG_LENGTH) {
            Log.d(tag, content)
        } else {
            logInSegments(tag, label, content)
        }
        Log.d(tag, "========== $label 结束 ==========")
    }

    /**
     * 分段输出长内容
     *
     * 【分段策略】
     * - 使用 chunked(4000) 按 4000 字符分段
     * - 每段标注序号（1/5、2/5...）
     * - 便于追踪和重组
     *
     * @param tag 日志标签
     * @param label 内容标签
     * @param content 完整内容
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
     * 记录完整的 API 请求信息
     *
     * 【使用场景】
     * - 调试 AI API 调用时
     * - 查看发送的完整请求参数
     * - 排查模型配置问题
     *
     * 【日志格式】
     * - 框线格式便于阅读
     * - 包含所有关键参数
     * - 完整输出 PromptContext 和 SystemInstruction
     *
     * @param tag 日志标签
     * @param method HTTP 方法
     * @param url 完整请求 URL
     * @param model 模型名称
     * @param providerName 服务商名称
     * @param promptContext 提示词上下文（可能很长）
     * @param systemInstruction 系统指令（可选）
     * @param additionalInfo 额外信息（可选）
     * @param temperature 生成温度（可选）
     * @param maxTokens 最大 Token 数（可选）
     */
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
