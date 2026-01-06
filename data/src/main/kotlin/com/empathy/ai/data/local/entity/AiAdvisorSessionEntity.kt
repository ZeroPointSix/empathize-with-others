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
 *
 * 业务背景 (PRD-00026):
 *   - 会话是对话记录的逻辑分组，每个联系人可有多个会话
 *   - is_active标识当前活跃会话，用于快速定位
 *   - message_count用于会话列表展示排序
 *
 * 设计决策 (TDD-00026):
 *   - 外键关联联系人，删除联系人时自动删除会话
 *   - 创建updated_at索引，支持按更新时间排序
 *   - is_active默认true，新会话创建时设为true，旧会话设为false
 *
 * 字段说明:
 *   - id: 会话唯一标识（UUID）
 *   - contact_id: 关联的联系人ID
 *   - title: 会话标题（首次对话内容截取或用户自定义）
 *   - message_count: 消息总数，用于会话排序
 *   - is_active: 是否为当前活跃会话
 *   - created_at/updated_at: 时间戳用于排序和同步
 *
 * @see AiAdvisorConversationEntity 对话实体
 * @see FD-00026 AI军师对话功能设计
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
     *
     * 领域模型是内存中的数据模型，不包含Room的ColumnInfo注解。
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
         * 从领域模型创建Entity
         *
         * 负责领域模型到数据库实体的转换，
         * 将枚举类型转换为字符串，将布尔值映射到数据库字段。
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
