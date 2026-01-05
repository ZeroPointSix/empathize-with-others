package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 策略标签实体 - 对应数据库 brain_tags 表
 *
 * 存储用户的"大脑标签"，即联系人的特征标签，用于AI分析的上下文输入。
 *
 * 业务背景 (PRD-00003):
 *   - 标签分为两种类型：雷区标签（RISK_RED）和策略标签（STRATEGY_GREEN）
 *   - 雷区标签：提醒用户避免的行为（如"不吃香菜"）
 *   - 策略标签：建议采用的策略（如"多倾听"）
 *   - 标签来源：用户手动添加 或 AI分析推断（AI_INFERRED）
 *
 * 设计决策 (TDD-00003):
 *   - 使用自增Long主键，便于快速插入
 *   - contact_id必须加索引，因为几乎所有查询都是按联系人筛选
 *   - type字段在Domain层是枚举，在DB层存储为字符串（灵活兼容）
 *   - is_confirmed字段用于AI推断标签的确认状态
 *
 * 特殊字段处理:
 *   - type字段使用TypeConverter进行枚举<->字符串转换
 *   - is_confirmed默认true，手动添加的标签无需确认
 *
 * 字段说明:
 *   - id: 数据库自增ID
 *   - contact_id: 外键，关联到哪个联系人
 *   - content: 标签内容（如"不喜欢吃香菜"）
 *   - type: 标签类型（RISK_RED/STRATEGY_GREEN）
 *   - source: 来源标记（MANUAL用户添加/AI_INFERRED AI推断）
 *   - is_confirmed: AI推断的标签是否已确认
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
