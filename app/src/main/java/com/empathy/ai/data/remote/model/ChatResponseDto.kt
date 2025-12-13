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
 * @property finishReason 完成原因 (例如: "stop" 表示自然结束, "tool_calls" 表示调用工具)
 */
@JsonClass(generateAdapter = true)
data class ChoiceDto(
    @Json(name = "message")
    val message: ResponseMessageDto?,

    @Json(name = "index")
    val index: Int?,

    @Json(name = "finish_reason")
    val finishReason: String?
)

/**
 * 响应消息 DTO（扩展版，支持 tool_calls）
 *
 * @property role 角色 (assistant)
 * @property content 文本内容（普通响应时有值）
 * @property toolCalls 工具调用列表（Function Calling 响应时有值）
 */
@JsonClass(generateAdapter = true)
data class ResponseMessageDto(
    @Json(name = "role")
    val role: String?,

    @Json(name = "content")
    val content: String?,
    
    @Json(name = "tool_calls")
    val toolCalls: List<ToolCallDto>? = null
)

/**
 * 工具调用 DTO
 *
 * @property id 调用 ID
 * @property type 类型，固定为 "function"
 * @property function 函数调用详情
 */
@JsonClass(generateAdapter = true)
data class ToolCallDto(
    @Json(name = "id")
    val id: String?,
    
    @Json(name = "type")
    val type: String?,
    
    @Json(name = "function")
    val function: FunctionCallDto?
)

/**
 * 函数调用详情 DTO
 *
 * @property name 函数名称
 * @property arguments 函数参数（JSON 字符串）
 */
@JsonClass(generateAdapter = true)
data class FunctionCallDto(
    @Json(name = "name")
    val name: String?,
    
    @Json(name = "arguments")
    val arguments: String?
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
