package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * API 用量记录实体 - 对应数据库 api_usage_records 表
 *
 * 表结构规范:
 * - 表名: api_usage_records (复数形式, snake_case)
 * - 主键: id (String 类型, UUID)
 * - 列名: snake_case 风格
 *
 * 索引优化:
 * - provider_id: 按服务商查询统计
 * - model_id: 按模型查询统计
 * - created_at: 按时间范围查询
 * - is_success: 按成功/失败筛选
 *
 * @property id 记录唯一标识 (UUID)
 * @property providerId 服务商 ID
 * @property providerName 服务商名称（冗余存储）
 * @property modelId 模型 ID
 * @property modelName 模型名称（冗余存储）
 * @property promptTokens 输入 Token 数
 * @property completionTokens 输出 Token 数
 * @property totalTokens 总 Token 数
 * @property requestTimeMs 请求耗时（毫秒）
 * @property isSuccess 是否成功
 * @property errorMessage 错误信息
 * @property createdAt 创建时间戳
 */
@Entity(
    tableName = "api_usage_records",
    indices = [
        Index(value = ["provider_id"]),
        Index(value = ["model_id"]),
        Index(value = ["created_at"]),
        Index(value = ["is_success"])
    ]
)
data class ApiUsageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    @ColumnInfo(name = "provider_name")
    val providerName: String,

    @ColumnInfo(name = "model_id")
    val modelId: String,

    @ColumnInfo(name = "model_name")
    val modelName: String,

    @ColumnInfo(name = "prompt_tokens")
    val promptTokens: Int = 0,

    @ColumnInfo(name = "completion_tokens")
    val completionTokens: Int = 0,

    @ColumnInfo(name = "total_tokens")
    val totalTokens: Int = 0,

    @ColumnInfo(name = "request_time_ms")
    val requestTimeMs: Long = 0,

    @ColumnInfo(name = "is_success")
    val isSuccess: Boolean = true,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
