package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus

/**
 * AI军师对话记录数据库实体
 *
 * 存储用户与AI军师的对话内容，支持外键约束和级联删除。
 *
 * 业务背景 (PRD-00026):
 *   - 对话记录是最细粒度的数据，关联会话和联系人
 *   - 支持多种消息类型（USER/ASSISTANT/SYSTEM）
 *   - 发送状态用于跟踪流式响应的进度
 *
 * 设计决策 (TDD-00026):
 *   - 使用外键关联会话和联系人，支持级联删除
 *   - 创建复合索引（contact_id, session_id, timestamp）优化查询
 *   - send_status字段支持PENDING/Streaming/STOPPED/SUCCESS/FAILED状态
 *
 * 字段说明:
 *   - id: 对话记录唯一标识
 *   - contact_id: 关联的联系人ID
 *   - session_id: 关联的会话ID
 *   - message_type: 消息类型（USER/ASSISTANT/SYSTEM）
 *   - content: 消息内容
 *   - send_status: 发送状态（流式响应时使用）
 *
 * @see AiAdvisorSessionEntity 会话实体
 * @see FD-00026 AI军师对话功能设计
 */
@Entity(
    tableName = "ai_advisor_conversations",
    foreignKeys = [
        ForeignKey(
            entity = ContactProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AiAdvisorSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contact_id"]),
        Index(value = ["session_id"]),
        Index(value = ["timestamp"])
    ]
)
data class AiAdvisorConversationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "contact_id")
    val contactId: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "message_type")
    val messageType: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "send_status", defaultValue = "SUCCESS")
    val sendStatus: String = "SUCCESS"
) {
    /**
     * 转换为领域模型
     */
    fun toDomain(): AiAdvisorConversation = AiAdvisorConversation(
        id = id,
        contactId = contactId,
        sessionId = sessionId,
        messageType = MessageType.valueOf(messageType),
        content = content,
        timestamp = timestamp,
        createdAt = createdAt,
        sendStatus = SendStatus.valueOf(sendStatus)
    )

    companion object {
        /**
         * 从领域模型创建
         */
        fun fromDomain(conversation: AiAdvisorConversation): AiAdvisorConversationEntity =
            AiAdvisorConversationEntity(
                id = conversation.id,
                contactId = conversation.contactId,
                sessionId = conversation.sessionId,
                messageType = conversation.messageType.name,
                content = conversation.content,
                timestamp = conversation.timestamp,
                createdAt = conversation.createdAt,
                sendStatus = conversation.sendStatus.name
            )
    }
}
