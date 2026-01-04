package com.empathy.ai.domain.model

import java.util.UUID

/**
 * AI军师对话记录
 *
 * 存储用户与AI军师的对话内容，支持消息状态跟踪和重试功能。
 *
 * @property id 对话ID（UUID）
 * @property contactId 联系人ID
 * @property sessionId 会话ID
 * @property messageType 消息类型：USER或AI
 * @property content 消息内容
 * @property timestamp 时间戳
 * @property createdAt 创建时间
 * @property sendStatus 发送状态（用于UI显示）
 */
data class AiAdvisorConversation(
    val id: String,
    val contactId: String,
    val sessionId: String,
    val messageType: MessageType,
    val content: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val sendStatus: SendStatus = SendStatus.SUCCESS
) {
    companion object {
        /**
         * 创建用户消息
         *
         * @param sessionId 会话ID
         * @param contactId 联系人ID
         * @param content 消息内容
         * @param sendStatus 发送状态，默认为PENDING
         * @return 新创建的用户消息
         */
        fun createUserMessage(
            sessionId: String,
            contactId: String,
            content: String,
            sendStatus: SendStatus = SendStatus.PENDING
        ): AiAdvisorConversation {
            val now = System.currentTimeMillis()
            return AiAdvisorConversation(
                id = UUID.randomUUID().toString(),
                contactId = contactId,
                sessionId = sessionId,
                messageType = MessageType.USER,
                content = content,
                timestamp = now,
                createdAt = now,
                sendStatus = sendStatus
            )
        }

        /**
         * 创建AI消息
         *
         * @param sessionId 会话ID
         * @param contactId 联系人ID
         * @param content 消息内容
         * @param sendStatus 发送状态，默认为SUCCESS
         * @return 新创建的AI消息
         */
        fun createAiMessage(
            sessionId: String,
            contactId: String,
            content: String,
            sendStatus: SendStatus = SendStatus.SUCCESS
        ): AiAdvisorConversation {
            val now = System.currentTimeMillis()
            return AiAdvisorConversation(
                id = UUID.randomUUID().toString(),
                contactId = contactId,
                sessionId = sessionId,
                messageType = MessageType.AI,
                content = content,
                timestamp = now,
                createdAt = now,
                sendStatus = sendStatus
            )
        }
    }
}
