package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.empathy.ai.domain.model.AiAdvisorSession

/**
 * AI军师会话数据库实体
 *
 * 存储AI军师会话信息，支持外键约束和级联删除。
 */
@Entity(
    tableName = "ai_advisor_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ContactProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contact_id"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"])
    ]
)
data class AiAdvisorSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "contact_id")
    val contactId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "message_count", defaultValue = "0")
    val messageCount: Int = 0,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true
) {
    /**
     * 转换为领域模型
     */
    fun toDomain(): AiAdvisorSession = AiAdvisorSession(
        id = id,
        contactId = contactId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        messageCount = messageCount,
        isActive = isActive
    )

    companion object {
        /**
         * 从领域模型创建
         */
        fun fromDomain(session: AiAdvisorSession): AiAdvisorSessionEntity =
            AiAdvisorSessionEntity(
                id = session.id,
                contactId = session.contactId,
                title = session.title,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt,
                messageCount = session.messageCount,
                isActive = session.isActive
            )
    }
}
