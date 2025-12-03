package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 消息单元 DTO
 *
 * 符合 OpenAI Chat Completion API 标准的消息结构。
 *
 * @property role 角色: "system" | "user" | "assistant"
 * @property content 消息内容
 *
 * 示例:
 * ```json
 * {
 *   "role": "user",
 *   "content": "你好"
 * }
 * ```
 */
@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "role")
    val role: String,

    @Json(name = "content")
    val content: String
)
