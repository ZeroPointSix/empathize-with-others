package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.empathy.ai.domain.model.ConversationTopic

/**
 * 对话主题数据库实体
 *
 * 存储用户为联系人设置的对话主题，支持主题历史记录和活跃状态管理。
 *
 * 表结构设计：
 * - 使用外键关联contact_profiles表，联系人删除时级联删除主题
 * - 复合索引优化按联系人和活跃状态的查询
 * - 单独索引优化按联系人ID的查询
 */
@Entity(
    tableName = "conversation_topics",
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
        Index(value = ["contact_id", "is_active"])
    ]
)
data class ConversationTopicEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "contact_id")
    val contactId: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean
) {
    /**
     * 转换为领域模型
     */
    fun toDomain(): ConversationTopic = ConversationTopic(
        id = id,
        contactId = contactId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isActive = isActive
    )

    companion object {
        /**
         * 从领域模型创建实体
         */
        fun fromDomain(topic: ConversationTopic): ConversationTopicEntity =
            ConversationTopicEntity(
                id = topic.id,
                contactId = topic.contactId,
                content = topic.content,
                createdAt = topic.createdAt,
                updatedAt = topic.updatedAt,
                isActive = topic.isActive
            )
    }
}
