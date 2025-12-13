package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI 聊天请求 DTO
 *
 * 符合 OpenAI Chat Completion API 标准的请求结构。
 * 支持多服务商(OpenAI、DeepSeek等)的兼容接口。
 *
 * @property model 模型名称 (例如: "gpt-3.5-turbo", "deepseek-chat", "gpt-4")
 * @property messages 消息列表,包含系统指令、用户问题和历史上下文
 * @property temperature 温度参数 (0.0-2.0),控制回复的随机性。越高越随机,越低越确定。
 *                  建议: 创意任务 0.8, 确定性任务 0.2, 通用场景 0.7
 * @property stream 是否使用流式响应。MVP阶段设为false,简化处理逻辑
 * @property responseFormat 响应格式配置。设置为{"type":"json_object"}强制返回JSON格式
 *
 * 示例:
 * ```json
 * {
 *   "model": "gpt-3.5-turbo",
 *   "messages": [
 *     {"role": "system", "content": "你是一个助手"},
 *     {"role": "user", "content": "你好"}
 *   ],
 *   "temperature": 0.7,
 *   "stream": false,
 *   "response_format": {"type": "json_object"}
 * }
 * ```
 */
@JsonClass(generateAdapter = true)
data class ChatRequestDto(
    @Json(name = "model")
    val model: String,

    @Json(name = "messages")
    val messages: List<MessageDto>,

    @Json(name = "temperature")
    val temperature: Double = 0.7,

    @Json(name = "stream")
    val stream: Boolean = false,
    
    @Json(name = "response_format")
    val responseFormat: ResponseFormat? = null
)

/**
 * 响应格式配置
 *
 * @property type 格式类型。"json_object"表示强制返回JSON格式
 */
@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "type")
    val type: String = "json_object"
)
