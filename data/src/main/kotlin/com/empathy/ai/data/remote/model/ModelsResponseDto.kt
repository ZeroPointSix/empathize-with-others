package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 模型列表响应 DTO
 *
 * 用于解析 OpenAI 兼容的 /models 端点响应
 *
 * 【SR-00001】模型列表自动获取与调试日志优化
 * - 用户切换服务商后自动获取可用模型列表
 * - 用于 AI 配置界面的模型选择下拉框
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
 * 【模型筛选】并非所有模型都适合当前场景
 * - 过滤条件：id 包含 "gpt"、"deepseek" 等
 * - 排除条件：davinci、babbage 等旧模型
 *
 * @property id 模型 ID（如 "gpt-4"、"deepseek-chat"）
 *             【格式】通常为 "厂商-模型名-版本" 格式
 * @property objectType 对象类型（通常为 "model"）
 * @property created 创建时间戳（Unix 时间戳）
 *                  【用途】排序：最新模型排在前面
 * @property ownedBy 所有者（如 "openai"、"deepseek"）
 *                   【用途】按厂商分组模型
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
