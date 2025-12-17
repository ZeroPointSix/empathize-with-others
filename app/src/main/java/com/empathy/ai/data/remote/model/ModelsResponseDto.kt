package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 模型列表响应 DTO
 *
 * 用于解析 OpenAI 兼容的 /models 端点响应
 *
 * 响应示例：
 * ```json
 * {
 *   "object": "list",
 *   "data": [
 *     {
 *       "id": "gpt-4",
 *       "object": "model",
 *       "created": 1687882411,
 *       "owned_by": "openai"
 *     }
 *   ]
 * }
 * ```
 *
 * @see SR-00001 模型列表自动获取与调试日志优化
 */
@JsonClass(generateAdapter = true)
data class ModelsResponseDto(
    @Json(name = "data")
    val data: List<ModelDto>,

    @Json(name = "object")
    val objectType: String? = null
)

/**
 * 单个模型 DTO
 *
 * @property id 模型 ID（如 "gpt-4"、"deepseek-chat"）
 * @property objectType 对象类型（通常为 "model"）
 * @property created 创建时间戳（Unix 时间戳）
 * @property ownedBy 所有者（如 "openai"、"deepseek"）
 */
@JsonClass(generateAdapter = true)
data class ModelDto(
    @Json(name = "id")
    val id: String,

    @Json(name = "object")
    val objectType: String? = null,

    @Json(name = "created")
    val created: Long? = null,

    @Json(name = "owned_by")
    val ownedBy: String? = null
)
