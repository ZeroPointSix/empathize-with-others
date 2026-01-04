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
