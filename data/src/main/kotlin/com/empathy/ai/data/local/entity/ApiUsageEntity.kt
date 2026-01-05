package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * API 用量记录实体 - 对应数据库 api_usage_records 表
 *
 * 存储API调用用量统计数据，用于成本控制和用量分析。
 *
 * 业务背景 (PRD-00025):
 *   - 追踪各服务商的API调用消耗，便于成本控制
 *   - 记录Token使用量，用于优化提示词长度
 *   - 统计请求成功率，识别不稳定的服务商
 *
 * 设计决策 (TDD-00025):
 *   - 使用UUID作为主键，支持分布式场景
 *   - provider_name和model_name冗余存储，便于独立查询
 *   - 创建复合索引优化常用统计查询场景
 *
 * 索引优化:
 *   - provider_id: 按服务商查询统计（如"DeepSeek本月花费"）
 *   - model_id: 按模型查询统计（如"gpt-4的使用量"）
 *   - created_at: 按时间范围查询（如"最近7天"）
 *   - is_success: 按成功/失败筛选（如"失败率分析"）
 *
 * 字段说明:
 *   - id: 记录唯一标识（UUID）
 *   - provider_id/name: 服务商ID和名称
 *   - model_id/name: 模型ID和名称
 *   - prompt_tokens/completion_tokens: 输入/输出Token数
 *   - total_tokens: 总Token数（冗余存储，便于查询）
 *   - request_time_ms: 请求耗时（性能监控）
 *   - is_success: 是否成功（成功率统计）
 *   - error_message: 失败时的错误信息
 *   - created_at: 创建时间戳
 *
 * @see FD-00025 AI配置功能完善
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
