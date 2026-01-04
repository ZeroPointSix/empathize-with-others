package com.empathy.ai.domain.model

import java.util.UUID

/**
 * AI军师会话
 *
 * 每个联系人可以有多个会话，每个会话包含多条对话记录。
 * 会话用于组织和管理用户与AI军师的对话历史。
 *
 * @property id 会话ID（UUID）
 * @property contactId 联系人ID
 * @property title 会话标题
 * @property createdAt 创建时间戳
 * @property updatedAt 最后更新时间戳
 * @property messageCount 消息数量
 * @property isActive 是否为活跃会话
 */
data class AiAdvisorSession(
    val id: String,
    val contactId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val isActive: Boolean = true
) {
    companion object {
        /**
         * 创建新会话
         *
         * @param contactId 联系人ID
         * @param title 会话标题，默认为"新对话"
         * @return 新创建的会话实例
         */
        fun create(contactId: String, title: String = "新对话"): AiAdvisorSession {
            val now = System.currentTimeMillis()
            return AiAdvisorSession(
                id = UUID.randomUUID().toString(),
                contactId = contactId,
                title = title,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
