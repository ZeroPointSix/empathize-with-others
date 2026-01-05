package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日总结数据库实体
 *
 * 存储每日对话的AI总结，支持单日总结和自定义范围总结。
 * 这是联系人画像记忆系统的"中期记忆"层。
 *
 * 业务背景 (PRD-00003):
 *   - 每日总结是对话记录的聚合，用于长期记忆分析
 *   - keyEvents记录标志性事件，用于AI分析的上下文输入
 *   - 关系分数变化追踪联系人关系的演变趋势
 *
 * 设计决策 (TDD-00003):
 *   - 使用外键关联联系人，支持级联删除
 *   - 创建复合唯一索引(contact_id, summary_date)防止重复
 *   - summary_type和generation_source用于分类和筛选
 *
 * v9新增字段说明:
 *   - start_date/end_date: 支持范围总结（如跨周、跨月）
 *   - summary_type: DAILY（单日）或 CUSTOM_RANGE（范围）
 *   - generation_source: AUTO（AI自动生成）或 MANUAL（用户手动）
 *   - conversation_count: 参与总结的对话数量，用于质量评估
 *
 * v10编辑追踪字段说明:
 *   - is_user_modified: 标记用户是否手动编辑过
 *   - last_modified_time: 最后修改时间，用于历史追溯
 *   - original_content: 保留原始内容，支持撤销回溯
 *
 * @see ContactProfileEntity 联系人实体（外键关联）
 * @see FD-00003 联系人画像记忆系统设计
 */
@Entity(
    tableName = "daily_summaries",
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
        Index(value = ["summary_date"]),
        Index(value = ["contact_id", "summary_date"], unique = true),
        Index(value = ["summary_type"]),
        Index(value = ["generation_source"])
    ]
)
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "contact_id")
    val contactId: String,

    @ColumnInfo(name = "summary_date")
    val summaryDate: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "key_events_json")
    val keyEventsJson: String,

    @ColumnInfo(name = "relationship_score")
    val relationshipScore: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    // ==================== v9 新增字段 ====================

    /** 范围总结开始日期（仅CUSTOM_RANGE类型使用） */
    @ColumnInfo(name = "start_date", defaultValue = "NULL")
    val startDate: String? = null,

    /** 范围总结结束日期（仅CUSTOM_RANGE类型使用） */
    @ColumnInfo(name = "end_date", defaultValue = "NULL")
    val endDate: String? = null,

    /** 总结类型：DAILY（单日）或 CUSTOM_RANGE（范围） */
    @ColumnInfo(name = "summary_type", defaultValue = "'DAILY'")
    val summaryType: String = "DAILY",

    /** 生成来源：AUTO（AI自动生成）或 MANUAL（用户手动） */
    @ColumnInfo(name = "generation_source", defaultValue = "'AUTO'")
    val generationSource: String = "AUTO",

    /** 分析的对话数量（用于总结质量评估） */
    @ColumnInfo(name = "conversation_count", defaultValue = "0")
    val conversationCount: Int = 0,

    /** AI总结生成时间戳 */
    @ColumnInfo(name = "generated_at", defaultValue = "0")
    val generatedAt: Long = 0,

    // ==================== v10 编辑追踪字段 ====================

    /** 是否被用户修改过 */
    @ColumnInfo(name = "is_user_modified", defaultValue = "0")
    val isUserModified: Boolean = false,

    /** 最后修改时间 */
    @ColumnInfo(name = "last_modified_time", defaultValue = "0")
    val lastModifiedTime: Long = 0L,

    /** 原始内容（修改前） */
    @ColumnInfo(name = "original_content", defaultValue = "NULL")
    val originalContent: String? = null
)
