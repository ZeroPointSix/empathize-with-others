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
 * @property stream 是否使用流式响应。MVP阶段设为false,简化处理逻辑
 * @property responseFormat 响应格式配置。设置为{"type":"json_object"}强制返回JSON格式
 * @property tools Function Calling 工具定义列表
 * @property toolChoice 工具选择策略
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
    val responseFormat: ResponseFormat? = null,
    
    @Json(name = "tools")
    val tools: List<ToolDefinition>? = null,
    
    @Json(name = "tool_choice")
    val toolChoice: ToolChoice? = null
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

// ==================== Function Calling 相关模型 ====================

/**
 * 工具定义
 *
 * @property type 工具类型，目前只支持 "function"
 * @property function 函数定义
 */
@JsonClass(generateAdapter = true)
data class ToolDefinition(
    @Json(name = "type")
    val type: String = "function",
    
    @Json(name = "function")
    val function: FunctionDefinition
)

/**
 * 函数定义
 *
 * @property name 函数名称
 * @property description 函数描述
 * @property parameters 参数 JSON Schema
 */
@JsonClass(generateAdapter = true)
data class FunctionDefinition(
    @Json(name = "name")
    val name: String,
    
    @Json(name = "description")
    val description: String,
    
    @Json(name = "parameters")
    val parameters: FunctionParameters
)

/**
 * 函数参数定义（JSON Schema 格式）
 *
 * @property type 类型，固定为 "object"
 * @property properties 属性定义
 * @property required 必需字段列表
 */
@JsonClass(generateAdapter = true)
data class FunctionParameters(
    @Json(name = "type")
    val type: String = "object",
    
    @Json(name = "properties")
    val properties: Map<String, PropertyDefinition>,
    
    @Json(name = "required")
    val required: List<String>
)

/**
 * 属性定义
 *
 * @property type 属性类型 (string, number, boolean, array, object)
 * @property description 属性描述
 * @property enum 枚举值列表（可选）
 */
@JsonClass(generateAdapter = true)
data class PropertyDefinition(
    @Json(name = "type")
    val type: String,
    
    @Json(name = "description")
    val description: String,
    
    @Json(name = "enum")
    val enum: List<String>? = null
)

/**
 * 工具选择策略
 *
 * @property type 选择类型: "auto", "none", "function"
 * @property function 指定函数（当 type 为 "function" 时）
 */
@JsonClass(generateAdapter = true)
data class ToolChoice(
    @Json(name = "type")
    val type: String,
    
    @Json(name = "function")
    val function: ToolChoiceFunction? = null
)

/**
 * 工具选择中的函数指定
 *
 * @property name 函数名称
 */
@JsonClass(generateAdapter = true)
data class ToolChoiceFunction(
    @Json(name = "name")
    val name: String
)
