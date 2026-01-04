package com.empathy.ai.domain.model

/**
 * 消息发送者枚举
 *
 * 用于区分用户自己发的消息和对方发的消息。
 * 这是对话上下文构建的基础，AI分析需要知道每条消息的来源。
 */
enum class MessageSender {
    /** 我发的消息 - 用于构建"我"的语境 */
    ME,

    /** 对方发的消息 - 用于构建"对方"的语境 */
    THEM
}

/**
 * 聊天消息 - 最基础的上下文单元
 *
 * 无论是文本消息，还是语音转录/OCR后的文字，都存储在content字段。
 * 这是PromptBuilder构建提示词的原材料。
 *
 * 业务背景 (PRD-00001):
 * - ChatMessage是构建对话上下文的基础单元
 * - 区分ME和THEM，AI需要理解"谁在说什么"
 * - timestamp用于排序，保证上下文顺序正确
 * - 用于实时分析和历史回顾两种场景
 *
 * 设计决策 (TDD-00001):
 * - 简洁的data class设计，只包含必要字段
 * - sender使用枚举而非布尔值，提升可读性
 * - timestamp使用Long（毫秒），支持精确排序
 *
 * 任务追踪: FD-00001/对话上下文构建
 *
 * @property id 消息唯一ID，用于LazyColumn的key
 * @property content 消息内容: 文本消息或语音转录/OCR后的文字
 * @property sender 发送者，区分ME和THEM
 * @property timestamp 时间戳，用于排序，保证上下文顺序正确
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val sender: MessageSender,
    val timestamp: Long
)
