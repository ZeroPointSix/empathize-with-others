package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日总结数据库实体
 *
 * 存储每日对话的AI总结
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
        Index(value = ["contact_id", "summary_date"], unique = true)
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
    val createdAt: Long
)
