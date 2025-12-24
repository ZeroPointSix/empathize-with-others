package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 服务商实体 - 对应数据库 ai_providers 表
 *
 * 表结构规范:
 * - 表名: ai_providers (复数形式, snake_case)
 * - 主键: id (String 类型, UUID)
 * - 列名: snake_case 风格
 * - Kotlin 属性: camelCase 风格
 *
 * 特殊字段处理:
 * - models 字段在 Domain 层是 List<AiModel>, 在 DB 层存储为 JSON 字符串
 * - apiKeyRef 存储的是 EncryptedSharedPreferences 的 key, 不是实际的 API Key
 * - 使用 Moshi 在 Repository 中进行序列化/反序列化
 *
 * 索引优化:
 * - is_default 字段添加索引, 优化查询默认服务商的性能
 *
 * @property id 服务商唯一标识 (UUID)
 * @property name 显示名称 (例如: "OpenAI", "DeepSeek")
 * @property baseUrl API 端点 (例如: "https://api.openai.com/v1")
 * @property apiKeyRef EncryptedSharedPreferences 的 key (用于查找加密存储的 API Key)
 * @property modelsJson 可用模型列表 (JSON 字符串格式, 存储 List<AiModel>)
 * @property defaultModelId 默认模型 ID
 * @property isDefault 是否为默认服务商
 * @property timeoutMs 请求超时时间（毫秒）
 * @property createdAt 创建时间戳
 *
 * @see com.empathy.ai.domain.model.AiProvider
 * @see com.empathy.ai.data.local.ApiKeyStorage
 */
@Entity(
    tableName = "ai_providers",
    indices = [Index(value = ["is_default"])]
)
data class AiProviderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "base_url")
    val baseUrl: String,

    @ColumnInfo(name = "api_key_ref")
    val apiKeyRef: String,

    @ColumnInfo(name = "models_json")
    val modelsJson: String,

    @ColumnInfo(name = "default_model_id")
    val defaultModelId: String,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "timeout_ms")
    val timeoutMs: Long = 30000L,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
