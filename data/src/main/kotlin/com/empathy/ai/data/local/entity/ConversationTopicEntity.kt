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
 * 业务背景 (PRD-00016):
 *   - 每个联系人可以有多个对话主题（如"项目协作"、"日常闲聊"）
 *   - 活跃主题用于AI分析的上下文输入
 *   - 主题历史记录便于用户回顾和切换
 *
 * 设计决策 (TDD-00016):
 *   - 使用外键关联contact_profiles表，联系人删除时级联删除主题
 *   - 复合索引(contact_id, is_active)优化活跃主题查询
 *   - 单独索引contact_id优化按联系人的主题列表查询
 *   - UUID作为主键，支持跨设备同步（如果未来需要）
 *
 * 状态管理:
 *   - 每个联系人的活跃主题同一时间只有一个
 *   - 新主题创建时自动设为活跃，旧主题设为非活跃
 *   - 便于快速定位当前使用的上下文主题
 *
 * 字段说明:
 *   - id: 主题唯一标识（UUID）
 *   - contact_id: 外键，关联联系人
 *   - content: 主题内容（如"项目进度讨论"）
 *   - is_active: 是否为活跃主题
 *   - created_at/updated_at: 时间戳用于排序
 *
 * @see ContactProfileEntity 联系人实体（外键关联）
 * @see FD-00016 对话主题功能设计
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
