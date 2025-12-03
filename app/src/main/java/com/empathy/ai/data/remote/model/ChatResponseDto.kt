package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI 聊天响应 DTO
 *
 * 符合 OpenAI Chat Completion API 标准的响应结构。
 *
 * @property id 响应唯一标识
 * @property choices 回复选项列表 (通常我们只取第 0 个)
 * @property usage Token 使用情况统计 (可选)
 *
 * 示例:
 * ```json
 * {
 *   "id": "chatcmpl-123",
 *   "choices": [
 *     {
 *       "message": {
 *         "role": "assistant",
 *         "content": "你好!有什么可以帮助你的吗?"
 *       }
 *     }
 *   ],
 *   "usage": {
 *     "prompt_tokens": 10,
 *     "completion_tokens": 20,
 *     "total_tokens": 30
 *   }
 * }
 * ```
 */
@JsonClass(generateAdapter = true)
data class ChatResponseDto(
    @Json(name = "id")
    val id: String?,

    @Json(name = "choices")
    val choices: List<ChoiceDto>,

    @Json(name = "usage")
    val usage: UsageDto? = null
)

/**
 * 回复选项 DTO
 *
 * @property message 回复消息
 * @property index 选项索引 (通常为 0)
 * @property finishReason 完成原因 (例如: "stop" 表示自然结束)
 */
@JsonClass(generateAdapter = true)
data class ChoiceDto(
    @Json(name = "message")
    val message: MessageDto?,

    @Json(name = "index")
    val index: Int?,

    @Json(name = "finish_reason")
    val finishReason: String?
)

/**
 * Token 使用情况 DTO
 *
 * @property promptTokens 输入文本消耗的 Token 数量
 * @property completionTokens 回复文本消耗的 Token 数量
 * @property totalTokens 总共消耗的 Token 数量
 */
@JsonClass(generateAdapter = true)
data class UsageDto(
    @Json(name = "prompt_tokens")
    val promptTokens: Int?,

    @Json(name = "completion_tokens")
    val completionTokens: Int?,

    @Json(name = "total_tokens")
    val totalTokens: Int?
)
