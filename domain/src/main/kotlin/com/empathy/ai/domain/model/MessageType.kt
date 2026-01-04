package com.empathy.ai.domain.model

/**
 * AI军师对话消息类型枚举
 *
 * 用于区分对话中的用户消息和AI回复消息
 */
enum class MessageType {
    /** 用户发送的消息 */
    USER,
    
    /** AI军师的回复消息 */
    AI
}
