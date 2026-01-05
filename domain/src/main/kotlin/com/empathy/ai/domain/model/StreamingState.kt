package com.empathy.ai.domain.model

/**
 * 流式状态
 *
 * 用于ViewModel和UI之间的状态传递，封装流式响应的各种状态。
 * 与AiStreamChunk不同，StreamingState更关注UI层的状态管理。
 *
 * 业务背景 (FD-00028):
 * - ViewModel需要将流式事件转换为UI可消费的状态
 * - 支持思考过程、文本更新、完成、错误等状态
 * - 便于Compose UI进行状态驱动的渲染
 *
 * 设计决策 (TDD-00028):
 * - 使用密封类确保类型安全和穷尽性检查
 * - 每种状态携带必要的数据供UI渲染
 * - 与AiStreamChunk解耦，便于独立演进
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see AiStreamChunk AI流式响应数据块
 */
sealed class StreamingState {

    /**
     * 流式响应开始
     *
     * 表示AI开始生成响应，UI应显示加载状态。
     *
     * @property messageId 正在生成的消息ID
     */
    data class Started(val messageId: String) : StreamingState()

    /**
     * 思考过程更新
     *
     * 表示收到AI的思考过程内容，UI应更新思考区域。
     *
     * @property content 累积的思考内容
     * @property elapsedMs 已耗时（毫秒）
     */
    data class ThinkingUpdate(
        val content: String,
        val elapsedMs: Long
    ) : StreamingState()

    /**
     * 文本内容更新
     *
     * 表示收到AI的回复内容，UI应更新文本区域。
     *
     * @property content 累积的文本内容
     */
    data class TextUpdate(val content: String) : StreamingState()

    /**
     * 流式响应完成
     *
     * 表示AI响应已完全生成，UI应更新消息状态。
     *
     * @property fullText 完整的响应文本
     * @property usage Token使用统计，可选
     */
    data class Completed(
        val fullText: String,
        val usage: TokenUsage?
    ) : StreamingState()

    /**
     * 流式响应错误
     *
     * 表示流式响应过程中发生错误，UI应显示错误提示。
     *
     * @property error 错误信息
     */
    data class Error(val error: Throwable) : StreamingState()
}
