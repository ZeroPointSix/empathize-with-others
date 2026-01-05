package com.empathy.ai.domain.model

/**
 * AI流式响应数据块
 *
 * 参考Cherry Studio的ChunkType设计，定义统一的流式事件类型。
 * 用于处理SSE流式响应，支持文本增量、思考过程、完成状态等事件。
 *
 * 业务背景 (FD-00028):
 * - 流式响应是现代AI应用的标配，提升用户体验
 * - 支持DeepSeek R1等模型的思考过程展示
 * - 统一的Chunk协议便于扩展新的内容类型
 *
 * 设计决策 (TDD-00028):
 * - 使用密封类确保类型安全和穷尽性检查
 * - Throwable是Kotlin标准库类，在:domain模块中可以使用
 * - 每种Chunk类型对应不同的UI渲染逻辑
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see RESEARCH-00004 Cherry项目AI对话实现深度分析报告
 */
sealed class AiStreamChunk {

    /**
     * 响应开始
     *
     * 表示SSE连接已建立，AI开始生成响应。
     * UI层收到此事件后应显示加载状态。
     */
    data object Started : AiStreamChunk()

    /**
     * 文本增量
     *
     * 表示收到一段文本内容，需要追加到现有内容后面。
     * 这是最常见的Chunk类型，用于实现打字机效果。
     *
     * @property text 增量文本内容
     */
    data class TextDelta(val text: String) : AiStreamChunk()

    /**
     * 思考过程增量（DeepSeek R1等模型）
     *
     * 表示收到AI的思考过程内容，用于展示AI的推理过程。
     * 仅部分模型支持此功能（如DeepSeek R1）。
     *
     * @property text 思考过程文本
     * @property thinkingMs 思考耗时（毫秒），可选
     */
    data class ThinkingDelta(
        val text: String,
        val thinkingMs: Long? = null
    ) : AiStreamChunk()

    /**
     * 思考完成
     *
     * 表示AI的思考过程已结束，即将开始输出正式回复。
     * UI层收到此事件后可以折叠思考过程区域。
     *
     * @property fullThinking 完整的思考过程文本
     * @property totalMs 总思考耗时（毫秒）
     */
    data class ThinkingComplete(
        val fullThinking: String,
        val totalMs: Long
    ) : AiStreamChunk()

    /**
     * 响应完成
     *
     * 表示AI响应已完全生成，流式传输结束。
     * UI层收到此事件后应更新消息状态为SUCCESS。
     *
     * @property fullText 完整的响应文本
     * @property usage Token使用统计，可选
     */
    data class Complete(
        val fullText: String,
        val usage: TokenUsage? = null
    ) : AiStreamChunk()

    /**
     * 错误
     *
     * 表示流式响应过程中发生错误。
     * UI层收到此事件后应显示错误提示并支持重试。
     *
     * @property error 错误信息
     */
    data class Error(val error: Throwable) : AiStreamChunk()
}

/**
 * Token使用统计
 *
 * 记录AI请求的Token消耗情况，用于用量统计和成本控制。
 *
 * @property promptTokens 提示词Token数
 * @property completionTokens 生成内容Token数
 * @property totalTokens 总Token数
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
