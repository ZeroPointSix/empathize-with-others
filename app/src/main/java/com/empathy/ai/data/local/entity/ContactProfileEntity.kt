package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 联系人画像实体 - 对应数据库 profiles 表
 *
 * 表结构规范:
 * - 表名: profiles (复数形式,snake_case)
 * - 主键: id (String类型,不自增)
 * - 列名: snake_case风格
 * - Kotlin属性: camelCase风格
 *
 * 特殊字段处理:
 * - facts字段在Domain层是Map<String, String>,在DB层存储为JSON字符串
 * - 使用Gson在TypeConverter中进行序列化/反序列化
 *
 * @property id 联系人唯一标识(UUID或外部加密ID)
 * @property name 显示名称(e.g., "王总", "李铁柱")
 * @property targetGoal 核心攻略目标(e.g., "拿下合同", "修复父子关系")
 * @property contextDepth 上下文读取深度(默认10)
 * @property factsJson 核心事实槽(JSON字符串格式,存储Map<String, String>)
 *
 * @see com.empathy.ai.domain.model.ContactProfile
 * @see com.empathy.ai.data.local.converter.RoomTypeConverters
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
    val factsJson: String = "{}"
)
