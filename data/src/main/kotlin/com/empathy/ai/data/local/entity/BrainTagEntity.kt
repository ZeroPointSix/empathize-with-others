package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 策略标签实体 - 对应数据库 brain_tags 表
 *
 * 表结构规范:
 * - 表名: brain_tags (复数形式,snake_case)
 * - 主键: id (Long类型,自增autoGenerate = true)
 * - 外键索引: contact_id (必须加索引,因为查询几乎都是WHERE contact_id = ?)
 * - 列名: snake_case风格
 * - Kotlin属性: camelCase风格
 *
 * 特殊字段处理:
 * - type字段在Domain层是TagType枚举,在DB层存储为String(使用enum.name)
 * - 使用TypeConverter进行枚举和字符串的转换
 *
 * @property id 数据库自增ID
 * @property contactId 外键:关联到哪个联系人(必须加索引)
 * @property content 标签内容(e.g., "不喜欢吃香菜")
 * @property type 类型:雷区(RISK_RED)或策略(STRATEGY_GREEN)
 * @property source 来源标记:MANUAL(用户手动添加)或AI_INFERRED(AI分析得出)
 *
 * @see com.empathy.ai.domain.model.BrainTag
 * @see com.empathy.ai.domain.model.TagType
 * @see com.empathy.ai.data.local.converter.RoomTypeConverters
 */
@Entity(
    tableName = "brain_tags",
    indices = [
        Index(value = ["contact_id"])
    ]
)
data class BrainTagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "contact_id")
    val contactId: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "tag_type")
    val type: String,

    @ColumnInfo(name = "source")
    val source: String = "MANUAL",

    @ColumnInfo(name = "is_confirmed", defaultValue = "1")
    val isConfirmed: Boolean = true
)
