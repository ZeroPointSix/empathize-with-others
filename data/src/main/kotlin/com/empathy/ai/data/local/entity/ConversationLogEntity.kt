package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 对话记录数据库实体
 *
 * 存储用户与AI的对话记录
 *
 * v10新增字段（编辑追踪）:
 * - is_user_modified: 是否被用户修改过
 * - last_modified_time: 最后修改时间
 * - original_user_input: 原始用户输入
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
