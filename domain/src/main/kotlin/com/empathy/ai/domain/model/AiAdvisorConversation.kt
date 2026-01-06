package com.empathy.ai.domain.model

import java.util.UUID

/**
 * AI军师对话记录
 *
 * 存储用户与AI军师的对话内容，支持消息状态跟踪和重试功能。
 * 与主对话系统(PRD-00007/PRD-00008)分离，提供独立的深度分析空间。
 *
 * 业务背景 (PRD-00026):
 * - AI军师定位为"赛前赛后复盘助手"，而非实时对话分析
 * - 每个联系人的对话历史独立存储，支持多会话管理
 * - 对话数据用于构建上下文，理解用户的问题意图
 *
 * 设计决策 (TDD-00026):
 * - 使用UUID作为主键，避免分布式环境下的ID冲突
 * - 分离sendStatus字段支持消息重试机制，提升用户体验
 * - 默认SUCCESS状态，避免空值判断的复杂性
 *
 * 任务追踪 (FD-00026/T003): 数据层实现 - 对话记录模型
 *
 * @property id 对话ID（UUID）
 * @property contactId 联系人ID，关联ContactProfile
 * @property sessionId 会话ID，支持同一联系人的多会话隔离
 * @property messageType 消息类型：USER或AI
 * @property content 消息内容，AI军师的分析回复或用户的问题
 * @property timestamp 时间戳，用于对话历史排序
 * @property createdAt 创建时间，用于数据管理和过期清理
 * @property sendStatus 发送状态（用于UI显示），支持PENDING/SUCCESS/FAILED
 * @property relatedUserMessageId 关联的用户消息ID（BUG-048-V4修复：用于重新生成时获取原始用户输入）
 */
data class AiAdvisorConversation(
    val id: String,
    val contactId: String,
    val sessionId: String,
    val messageType: MessageType,
    val content: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val sendStatus: SendStatus = SendStatus.SUCCESS,
    val relatedUserMessageId: String? = null // BUG-048-V4: 关联的用户消息ID
) {
    companion object {
        /**
         * 创建用户消息
         *
         * 业务规则 (PRD-00026/AC-003):
         * - 用户消息默认状态为PENDING，等待AI回复
         * - AI回复成功后，状态由UI层更新为SUCCESS
         * - 如果AI调用失败，消息状态保持FAILED供用户重试
         *
         * 设计权衡 (TDD-00026):
         * - 默认PENDING状态，因为用户消息总是先发送再等待AI响应
         * - 与AI消息的默认SUCCESS状态区分，反映不同的生命周期
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
         * 业务规则 (PRD-00026/AC-003):
         * - AI消息默认状态为SUCCESS，因为AI响应是异步的
         * - AI消息的content由AI服务返回，可能包含分析和建议
         * - AI军师的回复应保持客观中立（PRD-00026/2.2）
         *
         * 设计权衡 (TDD-00026):
         * - 默认SUCCESS状态，AI消息一旦创建就认为有效
         * - 与用户消息的默认PENDING状态区分，反映不同的生命周期
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
