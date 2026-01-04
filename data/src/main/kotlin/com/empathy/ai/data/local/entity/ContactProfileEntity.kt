package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 联系人画像实体 - 对应数据库 profiles 表（扩展版）
 *
 * 【表结构规范】
 * - 表名：profiles（复数形式，snake_case）
 * - 主键：id（String类型，不自增，支持外部加密ID）
 * - 列名：snake_case风格
 * - Kotlin属性：camelCase风格
 *
 * 【JSON序列化策略】
 * SQLite只支持基本类型，List<Fact>需要序列化为JSON字符串存储
 * 使用Moshi在TypeConverter中进行序列化/反序列化
 *
 * 【v10编辑追踪的设计意图】
 * 记录用户对姓名/目标的修改历史：
 * - is_name_user_modified：姓名是否被用户修改过
 * - original_name：原始姓名（用于撤销回溯）
 * - name_last_modified_time：修改时间（用于时间线展示）
 *
 * @property factsJson 核心事实槽（JSON字符串，存储List<Fact>）
 * @see com.empathy.ai.domain.model.ContactProfile
 * @see com.empathy.ai.data.local.converter.FactListConverter
 */
@Entity(tableName = "profiles")
data class ContactProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "target_goal")
    val targetGoal: String,

    @ColumnInfo(name = "context_depth")
    val contextDepth: Int = 10,

    @ColumnInfo(name = "facts_json")
    val factsJson: String = "[]",

    @ColumnInfo(name = "relationship_score")
    val relationshipScore: Int = 50,

    @ColumnInfo(name = "last_interaction_date")
    val lastInteractionDate: String? = null,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,

    @ColumnInfo(name = "custom_prompt")
    val customPrompt: String? = null,

    // ==================== v10 编辑追踪字段 ====================

    /** 姓名是否被用户修改过 */
    @ColumnInfo(name = "is_name_user_modified", defaultValue = "0")
    val isNameUserModified: Boolean = false,

    /** 目标是否被用户修改过 */
    @ColumnInfo(name = "is_goal_user_modified", defaultValue = "0")
    val isGoalUserModified: Boolean = false,

    /** 姓名最后修改时间 */
    @ColumnInfo(name = "name_last_modified_time", defaultValue = "0")
    val nameLastModifiedTime: Long = 0L,

    /** 目标最后修改时间 */
    @ColumnInfo(name = "goal_last_modified_time", defaultValue = "0")
    val goalLastModifiedTime: Long = 0L,

    /** 原始姓名（修改前） */
    @ColumnInfo(name = "original_name", defaultValue = "NULL")
    val originalName: String? = null,

    /** 原始目标（修改前） */
    @ColumnInfo(name = "original_goal", defaultValue = "NULL")
    val originalGoal: String? = null
)
