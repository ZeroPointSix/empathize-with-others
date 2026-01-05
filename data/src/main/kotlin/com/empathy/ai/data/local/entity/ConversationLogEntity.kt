package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 对话记录数据库实体
 *
 * 存储用户与AI的原始对话记录，这是联系人画像记忆系统的"短期记忆"层。
 *
 * 业务背景 (PRD-00003):
 *   - 对话记录是最细粒度的数据，直接来自用户交互
 *   - is_summarized标记用于区分已处理和未处理的记录
 *   - 定期归档到每日总结，释放存储空间
 *
 * 设计决策 (TDD-00003):
 *   - 使用外键关联联系人，支持级联删除
 *   - 创建is_summarized索引，优化未总结记录的查询
 *   - userInput和aiResponse分离存储，便于分析
 *
 * v10编辑追踪设计意图:
 *   - originalUserInput记录原始用户输入
 *   - 用于撤销回溯和历史追溯
 *   - 区分AI推断和用户真实表达
 *
 * 【数据生命周期】
 * 1. 用户发送消息 → 创建记录，is_summarized=false
 * 2. AI响应生成 → 更新aiResponse字段
 * 3. 每日总结生成 → 标记is_summarized=true
 * 4. 清理过期记录 → 删除已总结的旧记录
 *
 * @see ContactProfileEntity 联系人实体（外键关联）
 * @see DailySummaryEntity 每日总结实体（归档目标）
 * @see FD-00003 联系人画像记忆系统设计
 */
@Entity(
    tableName = "conversation_logs",
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
        Index(value = ["timestamp"]),
        Index(value = ["is_summarized"])
    ]
)
data class ConversationLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "contact_id")
    val contactId: String,

    @ColumnInfo(name = "user_input")
    val userInput: String,

    @ColumnInfo(name = "ai_response")
    val aiResponse: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "is_summarized")
    val isSummarized: Boolean = false,

    // ==================== v10 编辑追踪字段 ====================

    /** 是否被用户修改过 */
    @ColumnInfo(name = "is_user_modified", defaultValue = "0")
    val isUserModified: Boolean = false,

    /** 最后修改时间 */
    @ColumnInfo(name = "last_modified_time", defaultValue = "0")
    val lastModifiedTime: Long = 0L,

    /** 原始用户输入（修改前） */
    @ColumnInfo(name = "original_user_input", defaultValue = "NULL")
    val originalUserInput: String? = null
)
