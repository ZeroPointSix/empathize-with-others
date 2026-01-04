package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI 聊天请求 DTO
 *
 * 符合 OpenAI Chat Completion API 标准的请求结构。
 * 支持多服务商(OpenAI、DeepSeek等)的兼容接口。
 *
 * 【设计权衡】为什么需要 DTO 层？
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ 领域模型 (Domain)              │ 传输模型 (DTO)                     │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ 业务语义丰富                   │ API协议规范化                      │
 * │ 与业务规则紧耦合               │ 与外部协议解耦                     │
 * │ 可自由扩展                    │ 需考虑兼容性                       │
 * └─────────────────────────────────────────────────────────────────────┘
 * 分离设计确保：领域模型变更不影响API序列化，API协议变更不影响业务逻辑
 *
 * @property model 模型名称 (例如: "gpt-3.5-turbo", "deepseek-chat", "gpt-4")
 *                 【选择策略】根据任务复杂度选择：
 *                 - gpt-3.5-turbo: 成本敏感、简单问答
 *                 - gpt-4/deepseek-chat: 复杂分析、长上下文
 * @property messages 消息列表,包含系统指令、用户问题和历史上下文
 *                    【优化】TDD-00026限制：历史最多20条，控制Token消耗
 * @property temperature 温度参数 (0.0-2.0),控制回复的随机性
 *                       0.0-0.3: 确定性回复，适合分析场景（AI军师推荐）
 *                       0.7: 平衡模式
 *                       1.0+: 创意性回复
 * @property maxTokens 最大生成Token数
 *                     【预算控制】防止AI回复过长消耗过多Token
 * @property stream 是否使用流式响应。MVP阶段设为false,简化处理逻辑
 *                  【后续优化】v1.1可启用stream实现打字机效果
 * @property responseFormat 响应格式配置。设置为{"type":"json_object"}强制返回JSON格式
 *                          【用途】确保AI回复可解析，避免Markdown格式干扰
 * @property tools Function Calling 工具定义列表（预留扩展）
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

    @Json(name = "max_tokens")
    val maxTokens: Int? = null,

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
 * 【业务规则】为什么强制 JSON 模式？
 * - 避免AI回复Markdown格式导致的解析失败
 * - 确保结构化数据提取的一致性
 * - 符合 AI军师 功能对分析结果格式化的需求
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
