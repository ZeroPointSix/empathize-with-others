package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日总结数据库实体
 *
 * 存储每日对话的AI总结，支持单日总结和自定义范围总结
 *
 * v9新增字段：
 * - start_date: 范围总结开始日期
 * - end_date: 范围总结结束日期
 * - summary_type: 总结类型（DAILY/CUSTOM_RANGE）
 * - generation_source: 生成来源（AUTO/MANUAL）
 * - conversation_count: 分析的对话数量
 * - generated_at: 生成时间戳
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

    /** 生成来源：AUTO（自动）或 MANUAL（手动） */
    @ColumnInfo(name = "generation_source", defaultValue = "'AUTO'")
    val generationSource: String = "AUTO",

    /** 分析的对话数量 */
    @ColumnInfo(name = "conversation_count", defaultValue = "0")
    val conversationCount: Int = 0,

    /** 生成时间戳 */
    @ColumnInfo(name = "generated_at", defaultValue = "0")
    val generatedAt: Long = 0
)
