package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI 聊天响应 DTO
 *
 * 符合 OpenAI Chat Completion API 标准的响应结构。
 *
 * 【设计决策】可选字段使用 Nullable
 * - API响应可能因服务商不同而缺少某些字段
 * - 使用 Nullable 确保解析稳定性，不因字段缺失而崩溃
 *
 * @property id 响应唯一标识
 *            【用途】用于请求追踪、错误排查、限流控制
 *            【格式】通常为 "chatcmpl-xxxxxx" 前缀
 * @property choices 回复选项列表 (通常我们只取第 0 个)
 *                   【多选场景】n>1 时会有多个选项，用于批量生成
 * @property usage Token 使用情况统计 (可选)
 *                 【成本监控】用于统计API调用费用
 *                 【性能分析】输入/输出Token比例反映任务复杂度
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
 *                   【提取策略】取 choices[0].message.content
 *                   【边界】choices 可能为空数组，需做空判断
 * @property index 选项索引 (通常为 0)
 *                【用途】多选项时标识当前选项
 * @property finishReason 完成原因 (例如: "stop" 表示自然结束, "tool_calls" 表示调用工具)
 *                        【枚举值】
 *                        - stop: 正常结束
 *                        - length: 达到max_tokens限制
 *                        - tool_calls: 需要调用工具
 *                        - content_filter: 内容过滤
 *                        - function_call: 函数调用响应（兼容旧版）
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
