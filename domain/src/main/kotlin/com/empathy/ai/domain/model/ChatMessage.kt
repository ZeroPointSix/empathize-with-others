package com.empathy.ai.domain.model

/**
 * 消息发送者枚举
 */
enum class MessageSender {
    /** 我发的消息 */
    ME,

    /** 对方发的消息 */
    THEM
}

/**
 * 聊天消息 - 最基础的上下文单元
 *
 * 无论是文本消息，还是语音转录/OCR 后的文字，都存储在 content 字段
 *
 * @property id 消息唯一 ID
 * @property content 消息内容: 文本消息或语音转录/OCR 后的文字
 * @property sender 发送者
 * @property timestamp 时间戳 (用于排序，保证上下文顺序正确)
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val sender: MessageSender,
    val timestamp: Long
)
