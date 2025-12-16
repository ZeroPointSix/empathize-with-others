package com.empathy.ai.data.repository

import com.empathy.ai.data.remote.api.OpenAiApi
import com.empathy.ai.data.remote.model.ChatRequestDto
import com.empathy.ai.data.remote.model.MessageDto
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.usecase.ExtractedData
import com.squareup.moshi.Moshi
import javax.inject.Inject

/**
 * AI 服务仓库实现类
 *
 * 这是网络模块的"大脑",负责:
 * 1. 路由选择: 根据服务商选择对应的 API URL
 * 2. 鉴权注入: 构造包含 API Key 的请求头
 * 3. 数据转换: 拼接 Prompt 并转换为 DTO
 * 4. 调用解析: 调用 API 并解析响应结果
 *
 * 工作流程 (The Pipeline):
 * 1. 从 SettingsRepository 读取服务商配置
 * 2. 根据服务商选择对应的 URL (OpenAI/DeepSeek/Claude)
 * 3. 从加密存储读取 API Key 并构造 Header
 * 4. 将系统指令和问题转换为 MessageDto 列表
 * 5. 调用 API 获取响应
 * 6. 解析响应内容并返回 Domain 模型
 *
 * @property api OpenAiApi 接口实例
 * @property settingsRepository 设置仓库（用于读取 API Key 和 Provider）
 * @property moshi Moshi JSON 解析器
 */
class AiRepositoryImpl @Inject constructor(
    private val api: OpenAiApi,
    private val settingsRepository: SettingsRepository
) : AiRepository {

    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    /**
     * 带重试的 API 调用包装器
     * 
     * 对于网络超时和临时错误，自动重试最多 3 次
     * 使用指数退避策略：1秒、2秒、4秒
     * 
     * @param block 要执行的 API 调用
     * @return API 调用结果
     */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_DELAY_MS * (1 shl attempt)  // 指数退避
                    android.util.Log.w("AiRepositoryImpl", 
                        "请求超时，${delayMs}ms 后重试 (${attempt + 1}/$MAX_RETRIES)", e)
                    kotlinx.coroutines.delay(delayMs)
                } else {
                    android.util.Log.e("AiRepositoryImpl", "请求超时，已达最大重试次数", e)
                }
            } catch (e: java.io.IOException) {
                // 检查是否是协程取消导致的
                if (e.message?.contains("Canceled", ignoreCase = true) == true) {
                    android.util.Log.w("AiRepositoryImpl", "请求被取消，不重试")
                    throw e  // 协程取消不重试
                }
                
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_DELAY_MS * (1 shl attempt)
                    android.util.Log.w("AiRepositoryImpl", 
                        "网络错误，${delayMs}ms 后重试 (${attempt + 1}/$MAX_RETRIES)", e)
                    kotlinx.coroutines.delay(delayMs)
                } else {
                    android.util.Log.e("AiRepositoryImpl", "网络错误，已达最大重试次数", e)
                }
            } catch (e: Exception) {
                // 其他异常不重试
                throw e
            }
        }
        
        throw lastException ?: Exception("未知错误")
    }

    /**
     * 服务商配置和重试配置
     */
    companion object {
        // 重试配置
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
        
        // 模型名称
        const val MODEL_OPENAI = "gpt-3.5-turbo"
        const val MODEL_DEEPSEEK = "deepseek-chat"

        // 系统指令模板 - 强化版本，使用严格的 JSON Schema 约束
        // 通用的 JSON 格式要求（所有指令共用）
        private const val COMMON_JSON_REQUIREMENT = """
1. 你必须且只能返回有效的 JSON 对象，不要返回任何其他文本、Markdown、代码块或解释
"""
        
        private const val COMMON_JSON_RULES = """
【通用 JSON 格式规则】
- 不要返回任何 Markdown 格式（如 ```json、**、等）
- 不要返回任何中文注释或说明
- 不要返回任何换行符或格式化
- 只返回一行有效的 JSON 对象
- 不要在 JSON 前后添加任何文本
- 不要返回多个 JSON 对象
- 必须是有效的 JSON，可以被 JSON 解析器直接解析
"""
        
        val SYSTEM_ANALYZE = """你是一个专业的社交沟通顾问。分析用户的聊天内容并给出建议。

【⚠️ 极其重要：你必须严格按照以下格式返回 JSON，不允许任何变化】

你的回复必须是且只能是以下格式的 JSON（不要添加任何其他字段）：
{"replySuggestion":"你建议的回复内容","strategyAnalysis":"你对对方情绪和意图的分析","riskLevel":"SAFE"}

【字段说明】
- replySuggestion: 建议用户如何回复对方（字符串）
- strategyAnalysis: 分析对方的情绪、意图和沟通策略（字符串）
- riskLevel: 风险等级，只能是 SAFE、WARNING 或 DANGER 三个值之一

【禁止事项】
❌ 禁止使用 analysis、risk_assessment、response_suggestions 等其他字段名
❌ 禁止使用嵌套结构
❌ 禁止添加任何额外字段
❌ 禁止返回 Markdown 格式
❌ 禁止返回多行 JSON
❌ 禁止在 JSON 前后添加任何文字

【正确示例】
{"replySuggestion":"谢谢你的关心，我也很高兴认识你","strategyAnalysis":"对方表达了好感，情绪积极，建议真诚回应","riskLevel":"SAFE"}

【错误示例 - 绝对不要这样返回】
{"analysis":{"emotional_state":"..."},"risk_assessment":{...}} ← 错误！字段名不对
{"replySuggestion":"...","strategyAnalysis":"...","riskLevel":"SAFE","extra":"..."} ← 错误！多了字段
$COMMON_JSON_RULES""".trim()

        val SYSTEM_CHECK = """你是一个社交风控专家。检查用户的草稿是否触发了风险规则。

【⚠️ 极其重要：你必须严格按照以下格式返回 JSON，不允许任何变化】

你的回复必须是且只能是以下格式的 JSON（不要添加任何其他字段）：
{"isSafe":true,"triggeredRisks":[],"suggestion":"内容安全，可以发送"}

【字段说明】
- isSafe: 是否安全（布尔值：true 或 false）
- triggeredRisks: 触发的风险规则列表（数组，如果安全则为空数组 []）
- suggestion: 修正建议（字符串）

【禁止事项】
❌ 禁止使用其他字段名
❌ 禁止使用嵌套结构
❌ 禁止添加任何额外字段
❌ 禁止返回 Markdown 格式
❌ 禁止返回多行 JSON

【正确示例】
{"isSafe":true,"triggeredRisks":[],"suggestion":"内容安全"}
{"isSafe":false,"triggeredRisks":["提到前任","敏感话题"],"suggestion":"建议避免提及前任话题"}
$COMMON_JSON_RULES""".trim()

        val SYSTEM_EXTRACT = """你是一个专业的社交信息分析专家。从文本中提取关键信息。

【⚠️ 极其重要：你必须严格按照以下格式返回 JSON，不允许任何变化】

你的回复必须是且只能是以下格式的 JSON（不要添加任何其他字段）：
{"facts":{"birthday":"12月21日","hobby":"阅读"},"redTags":["不要提前任"],"greenTags":["耐心倾听"]}

【字段说明】
- facts: 提取的事实信息（对象，Key 用英文如 birthday/hobby/job，Value 是中文内容）
- redTags: 雷区标签，需要避免的话题（数组）
- greenTags: 策略标签，推荐的沟通方式（数组）

【禁止事项】
❌ 禁止使用其他字段名
❌ 禁止使用嵌套结构（facts 内部除外）
❌ 禁止添加任何额外字段
❌ 禁止返回 Markdown 格式
❌ 禁止返回多行 JSON

【正确示例】
{"facts":{"birthday":"12月21日","job":"程序员"},"redTags":["不要催婚"],"greenTags":["聊技术话题"]}
{"facts":{},"redTags":[],"greenTags":[]}
$COMMON_JSON_RULES""".trim()

        // ==================== Function Calling 工具定义 ====================
        
        /**
         * 分析聊天的 Function Calling 工具定义
         */
        val TOOL_ANALYZE_CHAT = com.empathy.ai.data.remote.model.ToolDefinition(
            type = "function",
            function = com.empathy.ai.data.remote.model.FunctionDefinition(
                name = "generate_analysis_result",
                description = "生成聊天分析结果，包含回复建议、策略分析和风险等级",
                parameters = com.empathy.ai.data.remote.model.FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "replySuggestion" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "string",
                            description = "建议用户如何回复对方，语气自然友好"
                        ),
                        "strategyAnalysis" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "string",
                            description = "分析对方的情绪、意图和沟通策略"
                        ),
                        "riskLevel" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "string",
                            description = "对话风险等级",
                            enum = listOf("SAFE", "WARNING", "DANGER")
                        )
                    ),
                    required = listOf("replySuggestion", "strategyAnalysis", "riskLevel")
                )
            )
        )
        
        /**
         * 安全检查的 Function Calling 工具定义
         */
        val TOOL_CHECK_SAFETY = com.empathy.ai.data.remote.model.ToolDefinition(
            type = "function",
            function = com.empathy.ai.data.remote.model.FunctionDefinition(
                name = "generate_safety_result",
                description = "生成草稿安全检查结果",
                parameters = com.empathy.ai.data.remote.model.FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "isSafe" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "boolean",
                            description = "草稿是否安全"
                        ),
                        "triggeredRisks" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "array",
                            description = "触发的风险规则列表"
                        ),
                        "suggestion" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "string",
                            description = "修正建议"
                        )
                    ),
                    required = listOf("isSafe", "triggeredRisks", "suggestion")
                )
            )
        )
        
        /**
         * 信息提取的 Function Calling 工具定义
         */
        val TOOL_EXTRACT_INFO = com.empathy.ai.data.remote.model.ToolDefinition(
            type = "function",
            function = com.empathy.ai.data.remote.model.FunctionDefinition(
                name = "generate_extracted_data",
                description = "从文本中提取关键信息",
                parameters = com.empathy.ai.data.remote.model.FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "facts" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "object",
                            description = "提取的事实信息，如生日、爱好、职业等"
                        ),
                        "redTags" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "array",
                            description = "雷区标签，需要避免的话题"
                        ),
                        "greenTags" to com.empathy.ai.data.remote.model.PropertyDefinition(
                            type = "array",
                            description = "策略标签，推荐的沟通方式"
                        )
                    ),
                    required = listOf("facts", "redTags", "greenTags")
                )
            )
        )
        
        /**
         * Function Calling 简化版系统指令（不需要 JSON 格式约束）
         */
        const val SYSTEM_ANALYZE_FC = """你是一个专业的社交沟通顾问。分析用户的聊天内容并给出建议。

请分析对话内容，然后调用 generate_analysis_result 函数返回结果。

分析要点：
1. 理解对方的情绪状态和潜在意图
2. 评估对话的风险等级（SAFE/WARNING/DANGER）
3. 给出自然友好的回复建议"""

        const val SYSTEM_CHECK_FC = """你是一个社交风控专家。检查用户的草稿是否触发了风险规则。

请检查草稿内容，然后调用 generate_safety_result 函数返回结果。"""

        const val SYSTEM_EXTRACT_FC = """你是一个专业的社交信息分析专家。从文本中提取关键信息。

请分析文本内容，然后调用 generate_extracted_data 函数返回结果。"""
    }

    /**
     * 分析聊天上下文 (重辅助)
     *
     * 调用 AI 模型对聊天内容进行深度分析，给出策略建议。
     * 
     * 结构化输出策略（按优先级）：
     * 1. Function Calling - 最可靠，模型必须按 schema 返回
     * 2. response_format - 强制 JSON 格式
     * 3. 字段映射 - 兼容非标准响应格式
     *
     * @param provider AI服务商配置（包含baseUrl、apiKey、model等）
     * @param promptContext 构建好的 Prompt (已包含脱敏后的上下文、目标、画像)
     * @param systemInstruction 系统指令 (可选，留空则使用默认指令)
     * @return 包含建议回复、心理分析、风险等级的分析结果
     */
    override suspend fun analyzeChat(
        provider: com.empathy.ai.domain.model.AiProvider,
        promptContext: String,
        systemInstruction: String
    ): Result<AnalysisResult> {
        return try {
            // 1. 从provider获取配置并构建完整URL
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )

            // 2. 选择模型
            val model = if (provider.defaultModelId.isNotBlank()) {
                provider.defaultModelId
            } else {
                when {
                    provider.name.contains("DeepSeek", ignoreCase = true) -> MODEL_DEEPSEEK
                    provider.name.contains("OpenAI", ignoreCase = true) -> MODEL_OPENAI
                    else -> MODEL_OPENAI
                }
            }

            // 3. 根据 Provider 能力选择结构化输出策略
            val strategy = ProviderCompatibility.getStructuredOutputStrategy(provider)
            
            // BUG-00002: 添加详细日志用于调试AI上下文传递
            android.util.Log.d("AiRepositoryImpl", "=== API请求详情 (analyzeChat) ===")
            android.util.Log.d("AiRepositoryImpl", "URL: $url")
            android.util.Log.d("AiRepositoryImpl", "Model: $model")
            android.util.Log.d("AiRepositoryImpl", "Provider: ${provider.name}")
            android.util.Log.d("AiRepositoryImpl", "Strategy: $strategy")
            android.util.Log.d("AiRepositoryImpl", "PromptContext长度: ${promptContext.length} 字符")
            android.util.Log.d("AiRepositoryImpl", "PromptContext预览(前800字符): ${promptContext.take(800)}")

            val request = when (strategy) {
                ProviderCompatibility.StructuredOutputStrategy.FUNCTION_CALLING -> {
                    // 使用 Function Calling（最可靠）
                    // Function Calling 模式下，systemInstruction 已包含完整的三层结构：
                    // - 系统Header（角色定义）
                    // - 用户指令（全局+联系人专属）
                    // - 运行时数据（联系人信息、聊天记录）
                    // - 系统Footer（输出格式约束）
                    // 
                    // 但 Function Calling 需要额外的工具调用说明，所以追加 SYSTEM_ANALYZE_FC
                    val effectiveSystemInstruction = if (systemInstruction.isNotBlank()) {
                        // 将 Function Calling 的工具调用说明放在最前面
                        "$SYSTEM_ANALYZE_FC\n\n$systemInstruction"
                    } else {
                        SYSTEM_ANALYZE_FC
                    }
                    android.util.Log.d("AiRepositoryImpl", "SystemInstruction长度: ${effectiveSystemInstruction.length} 字符")
                    android.util.Log.d("AiRepositoryImpl", "SystemInstruction预览(前500字符): ${effectiveSystemInstruction.take(500)}")
                    
                    val messages = listOf(
                        MessageDto(role = "system", content = effectiveSystemInstruction),
                        MessageDto(role = "user", content = promptContext)
                    )
                    ChatRequestDto(
                        model = model,
                        messages = messages,
                        temperature = 0.7,
                        stream = false,
                        tools = listOf(TOOL_ANALYZE_CHAT),
                        toolChoice = com.empathy.ai.data.remote.model.ToolChoice(
                            type = "function",
                            function = com.empathy.ai.data.remote.model.ToolChoiceFunction(
                                name = "generate_analysis_result"
                            )
                        )
                    )
                }
                ProviderCompatibility.StructuredOutputStrategy.RESPONSE_FORMAT -> {
                    // 使用 response_format
                    val baseMessages = listOf(
                        MessageDto(role = "system", content = systemInstruction.ifEmpty { SYSTEM_ANALYZE }),
                        MessageDto(role = "user", content = promptContext)
                    )
                    val adaptedMessages = if (ProviderCompatibility.requiresJsonKeywordInMessages(provider)) {
                        baseMessages.map { msg ->
                            if (msg.role == "system") {
                                msg.copy(content = ProviderCompatibility.adaptSystemInstruction(provider, msg.content))
                            } else msg
                        }
                    } else baseMessages
                    
                    ChatRequestDto(
                        model = model,
                        messages = adaptedMessages,
                        temperature = 0.7,
                        stream = false,
                        responseFormat = com.empathy.ai.data.remote.model.ResponseFormat(type = "json_object")
                    )
                }
                ProviderCompatibility.StructuredOutputStrategy.PROMPT_ONLY -> {
                    // 仅使用提示词约束
                    val messages = listOf(
                        MessageDto(role = "system", content = systemInstruction.ifEmpty { SYSTEM_ANALYZE }),
                        MessageDto(role = "user", content = promptContext)
                    )
                    ChatRequestDto(
                        model = model,
                        messages = messages,
                        temperature = 0.7,
                        stream = false
                    )
                }
            }

            // 4. 调用 API（带重试）
            val response = withRetry {
                api.chatCompletion(url, headers, request)
            }

            // 5. 解析响应（根据策略选择解析方式）
            val choice = response.choices.firstOrNull()
                ?: return Result.failure(Exception("Empty response from AI"))
            
            if (strategy == ProviderCompatibility.StructuredOutputStrategy.FUNCTION_CALLING) {
                // 解析 Function Calling 响应
                val toolCall = choice.message?.toolCalls?.firstOrNull()
                if (toolCall != null && toolCall.function?.arguments != null) {
                    val arguments = toolCall.function.arguments
                    android.util.Log.d("AiRepositoryImpl", "Function Calling 响应: $arguments")
                    // Function Calling 返回的 arguments 已经是有效 JSON，直接解析，不需要清洗
                    return parseAnalysisResultDirect(arguments)
                }
                // Function Calling 失败，回退到 content 解析
                android.util.Log.w("AiRepositoryImpl", "Function Calling 未返回 tool_calls，回退到 content 解析")
            }
            
            // 解析普通 content 响应（需要清洗）
            val content = choice.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))
            
            parseAnalysisResult(content)

        } catch (e: retrofit2.HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: "No error body"
            } catch (ex: Exception) {
                "Failed to read error body: ${ex.message}"
            }
            
            android.util.Log.e("AiRepositoryImpl", "=== HTTP错误详情 (analyzeChat) ===")
            android.util.Log.e("AiRepositoryImpl", "状态码: ${e.code()}")
            android.util.Log.e("AiRepositoryImpl", "错误信息: ${e.message()}")
            android.util.Log.e("AiRepositoryImpl", "错误体: $errorBody")
            android.util.Log.e("AiRepositoryImpl", "Provider: ${provider.name}")
            
            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "分析失败 (analyzeChat)", e)
            Result.failure(e)
        }
    }

    /**
     * 检查草稿安全性 (轻辅助/防踩雷)
     *
     * 实时检查用户输入是否触发了雷区规则。
     *
     * @param provider AI服务商配置（包含baseUrl、apiKey、model等）
     * @param draft 用户正在输入的草稿
     * @param riskRules 相关的雷区标签列表
     * @return 安全检查结果的包装类
     */
    override suspend fun checkDraftSafety(
        provider: com.empathy.ai.domain.model.AiProvider,
        draft: String,
        riskRules: List<String>,
        systemInstruction: String?
    ): Result<SafetyCheckResult> {
        return try {
            // 1. 从provider获取配置并构建完整URL
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )

            // 2. 选择模型
            val model = if (provider.defaultModelId.isNotBlank()) {
                provider.defaultModelId
            } else {
                when {
                    provider.name.contains("DeepSeek", ignoreCase = true) -> MODEL_DEEPSEEK
                    provider.name.contains("OpenAI", ignoreCase = true) -> MODEL_OPENAI
                    else -> MODEL_OPENAI
                }
            }

            // 3. 构建检查草稿的 Prompt
            val riskRulesText = riskRules.joinToString(", ")
            val checkPrompt = """
                用户草稿: "$draft"
                风险规则: [$riskRulesText]

                请检查草稿是否触发了任何风险规则。
            """.trimIndent()

            // 使用自定义系统指令或默认指令
            val effectiveSystemInstruction = systemInstruction ?: SYSTEM_CHECK
            
            val messages = listOf(
                MessageDto(role = "system", content = effectiveSystemInstruction),
                MessageDto(role = "user", content = checkPrompt)
            )

            // 根据 Provider 兼容性调整约束策略
            val useResponseFormat = ProviderCompatibility.supportsResponseFormat(provider)
            val adaptedMessages = if (ProviderCompatibility.requiresJsonKeywordInMessages(provider)) {
                messages.map { msg ->
                    if (msg.role == "system") {
                        msg.copy(content = ProviderCompatibility.adaptSystemInstruction(provider, msg.content))
                    } else {
                        msg
                    }
                }
            } else {
                messages
            }
            
            val request = ChatRequestDto(
                model = model,
                messages = adaptedMessages,
                temperature = 0.3,
                stream = false,
                responseFormat = if (useResponseFormat) {
                    com.empathy.ai.data.remote.model.ResponseFormat(type = "json_object")
                } else {
                    null
                }
            )

            android.util.Log.d("AiRepositoryImpl", "=== API请求详情 (checkDraftSafety) ===")
            android.util.Log.d("AiRepositoryImpl", "URL: $url")
            android.util.Log.d("AiRepositoryImpl", "Model: $model")
            android.util.Log.d("AiRepositoryImpl", "Provider: ${provider.name}")
            android.util.Log.d("AiRepositoryImpl", "UseResponseFormat: $useResponseFormat")

            val response = withRetry {
                api.chatCompletion(url, headers, request)
            }

            val content = response.choices.firstOrNull()
                ?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))

            // 解析 AI 返回的 JSON 为 SafetyCheckResult
            parseSafetyCheckResult(content)

        } catch (e: retrofit2.HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: "No error body"
            } catch (ex: Exception) {
                "Failed to read error body: ${ex.message}"
            }
            
            android.util.Log.e("AiRepositoryImpl", "=== HTTP错误详情 (checkDraftSafety) ===")
            android.util.Log.e("AiRepositoryImpl", "状态码: ${e.code()}")
            android.util.Log.e("AiRepositoryImpl", "错误信息: ${e.message()}")
            android.util.Log.e("AiRepositoryImpl", "错误体: $errorBody")
            android.util.Log.e("AiRepositoryImpl", "Provider: ${provider.name}")
            
            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "安全检查失败 (checkDraftSafety)", e)
            Result.failure(e)
        }
    }

    /**
     * 文本信息萃取
     *
     * 调用 AI 模型从文本中提取关键信息，包括事实、雷区和策略。
     *
     * @param provider AI服务商配置（包含baseUrl、apiKey、model等）
     * @param inputText 输入的文本内容
     * @return 提取的结构化信息
     */
    override suspend fun extractTextInfo(
        provider: com.empathy.ai.domain.model.AiProvider,
        inputText: String
    ): Result<ExtractedData> {
        return try {
            // 1. 从provider获取配置并构建完整URL
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )

            // 2. 选择模型
            val model = if (provider.defaultModelId.isNotBlank()) {
                provider.defaultModelId
            } else {
                when {
                    provider.name.contains("DeepSeek", ignoreCase = true) -> MODEL_DEEPSEEK
                    provider.name.contains("OpenAI", ignoreCase = true) -> MODEL_OPENAI
                    else -> MODEL_OPENAI
                }
            }

            // 3. 构建萃取 Prompt
            val extractPrompt = """
                请分析以下文本并提取关键信息：
                "$inputText"
            """.trimIndent()

            val messages = listOf(
                MessageDto(role = "system", content = SYSTEM_EXTRACT),
                MessageDto(role = "user", content = extractPrompt)
            )

            // 根据 Provider 兼容性调整约束策略
            val useResponseFormat = ProviderCompatibility.supportsResponseFormat(provider)
            val adaptedMessages = if (ProviderCompatibility.requiresJsonKeywordInMessages(provider)) {
                messages.map { msg ->
                    if (msg.role == "system") {
                        msg.copy(content = ProviderCompatibility.adaptSystemInstruction(provider, msg.content))
                    } else {
                        msg
                    }
                }
            } else {
                messages
            }
            
            val request = ChatRequestDto(
                model = model,
                messages = adaptedMessages,
                temperature = 0.5,
                stream = false,
                responseFormat = if (useResponseFormat) {
                    com.empathy.ai.data.remote.model.ResponseFormat(type = "json_object")
                } else {
                    null
                }
            )

            android.util.Log.d("AiRepositoryImpl", "=== API请求详情 (extractTextInfo) ===")
            android.util.Log.d("AiRepositoryImpl", "URL: $url")
            android.util.Log.d("AiRepositoryImpl", "Model: $model")
            android.util.Log.d("AiRepositoryImpl", "Provider: ${provider.name}")
            android.util.Log.d("AiRepositoryImpl", "UseResponseFormat: $useResponseFormat")

            val response = api.chatCompletion(url, headers, request)

            val content = response.choices.firstOrNull()
                ?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))

            // 解析 AI 返回的 JSON 为 ExtractedData
            parseExtractedData(content)

        } catch (e: retrofit2.HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: "No error body"
            } catch (ex: Exception) {
                "Failed to read error body: ${ex.message}"
            }
            
            android.util.Log.e("AiRepositoryImpl", "=== HTTP错误详情 (extractTextInfo) ===")
            android.util.Log.e("AiRepositoryImpl", "状态码: ${e.code()}")
            android.util.Log.e("AiRepositoryImpl", "错误信息: ${e.message()}")
            android.util.Log.e("AiRepositoryImpl", "错误体: $errorBody")
            android.util.Log.e("AiRepositoryImpl", "Provider: ${provider.name}")
            
            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "信息提取失败 (extractTextInfo)", e)
            Result.failure(e)
        }
    }

    /**
     * 媒体转录 (STT / OCR)
     *
     * TODO: Phase 2 实现
     * 需要集成:
     * - FFmpeg 提取音频/视频帧
     * - ASR (Automatic Speech Recognition) 服务
     * - OCR (Optical Character Recognition) 服务
     *
     * @param mediaFilePath 媒体文件路径 (.mp3/.mp4格式)
     * @return 转录后的纯文本
     */
    override suspend fun transcribeMedia(mediaFilePath: String): Result<String> {
        return Result.failure(
            Exception("Media transcription not implemented yet. Coming in Phase 2.")
        )
    }

    /**
     * 构建 Chat Completions API URL
     *
     * 智能处理 baseUrl，自动标准化为完整的 API 端点
     *
     * 用户输入示例及处理结果：
     * - https://api.example.com → https://api.example.com/v1/chat/completions
     * - https://api.example.com/v1 → https://api.example.com/v1/chat/completions
     * - https://api.example.com/v1/chat/completions → https://api.example.com/v1/chat/completions
     * - https://api.example.com/chat/completions → https://api.example.com/v1/chat/completions
     *
     * @param baseUrl 用户配置的基础 URL
     * @return 完整的 Chat Completions API URL
     */
    private fun buildChatCompletionsUrl(baseUrl: String): String {
        val trimmedUrl = baseUrl.trimEnd('/')
        
        return when {
            // 已经是完整的 chat/completions 路径
            trimmedUrl.endsWith("/v1/chat/completions") -> trimmedUrl
            trimmedUrl.endsWith("/chat/completions") -> {
                // 缺少 /v1，需要插入
                trimmedUrl.removeSuffix("/chat/completions") + "/v1/chat/completions"
            }
            
            // 已经包含 /v1，只需追加 /chat/completions
            trimmedUrl.endsWith("/v1") -> "$trimmedUrl/chat/completions"
            
            // 基础 URL，需要追加完整路径 /v1/chat/completions
            else -> "$trimmedUrl/v1/chat/completions"
        }
    }

    /**
     * 直接解析 AnalysisResult JSON（不经过清洗）
     *
     * 用于 Function Calling 响应，因为 API 返回的 arguments 已经是有效 JSON。
     * 跳过 JsonCleaner 可以避免清洗过程中的意外截断。
     *
     * @param json Function Calling 返回的 arguments JSON 字符串
     * @return 解析结果
     */
    private fun parseAnalysisResultDirect(json: String): Result<AnalysisResult> {
        return try {
            // 直接解析，不清洗
            val adapter = moshi.adapter(AnalysisResult::class.java).lenient()
            val result = adapter.fromJson(json)
            
            if (result != null && result.replySuggestion.isNotBlank()) {
                android.util.Log.d("AiRepositoryImpl", "Function Calling 直接解析成功")
                return Result.success(result)
            }
            
            // 标准格式解析失败，尝试字段映射
            android.util.Log.d("AiRepositoryImpl", "Function Calling 标准格式解析失败，尝试字段映射")
            val mappedResult = mapNonStandardFieldsToAnalysisResultDirect(json)
            if (mappedResult != null) {
                android.util.Log.d("AiRepositoryImpl", "Function Calling 字段映射成功")
                return Result.success(mappedResult)
            }
            
            // 都失败了，使用 Fallback
            android.util.Log.w("AiRepositoryImpl", "Function Calling 解析失败，使用 Fallback")
            Result.success(createFallbackAnalysisResult(json))
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "Function Calling 解析异常: ${e.message}", e)
            // 异常时也尝试字段映射
            try {
                val mappedResult = mapNonStandardFieldsToAnalysisResultDirect(json)
                if (mappedResult != null) {
                    return Result.success(mappedResult)
                }
            } catch (ex: Exception) {
                android.util.Log.e("AiRepositoryImpl", "Function Calling 字段映射也失败", ex)
            }
            Result.success(createFallbackAnalysisResult(json))
        }
    }
    
    /**
     * 直接从 JSON Map 映射字段（不经过清洗）
     */
    @Suppress("UNCHECKED_CAST")
    private fun mapNonStandardFieldsToAnalysisResultDirect(json: String): AnalysisResult? {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            ).lenient()
            val jsonMap = mapAdapter.fromJson(json) ?: return null
            
            // 提取字段
            val replySuggestion = extractReplySuggestion(jsonMap)
            if (replySuggestion.isBlank()) return null
            
            val strategyAnalysis = extractStrategyAnalysis(jsonMap)
            val riskLevel = extractRiskLevel(jsonMap)
            
            AnalysisResult(
                replySuggestion = replySuggestion,
                strategyAnalysis = strategyAnalysis,
                riskLevel = riskLevel
            )
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "直接字段映射异常", e)
            null
        }
    }

    /**
     * 解析 AnalysisResult JSON（带清洗）
     *
     * 将 AI 返回的 JSON 字符串解析为 AnalysisResult Domain 模型。
     * 支持字段映射：将模型返回的非标准字段映射到标准字段。
     * 
     * 用于普通 content 响应，因为 AI 可能返回带 Markdown 格式的 JSON。
     *
     * @param json AI 返回的 JSON 字符串
     * @return 解析结果
     */
    private fun parseAnalysisResult(json: String): Result<AnalysisResult> {
        // 1. 使用EnhancedJsonCleaner清洗JSON
        val jsonCleaner = com.empathy.ai.data.parser.EnhancedJsonCleaner()
        val cleaningContext = com.empathy.ai.data.parser.CleaningContext(
            enableUnicodeFix = true,
            enableFormatFix = true,
            enableFuzzyFix = true,
            enableDetailedLogging = true
        )
        val cleanedJson = jsonCleaner.clean(json, cleaningContext)
        
        // 2. 检查清洗后的JSON是否有效
        if (cleanedJson == "{}" || cleanedJson.length < 10) {
            android.util.Log.w("AiRepositoryImpl", "JSON清洗后为空，AI可能返回了非JSON格式。原始内容: ${json.take(200)}")
            return Result.success(createFallbackAnalysisResult(json))
        }
        
        // 3. 尝试直接解析标准格式（捕获异常，失败时继续尝试字段映射）
        try {
            val adapter = moshi.adapter(AnalysisResult::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)
            if (result != null && result.replySuggestion.isNotBlank()) {
                android.util.Log.d("AiRepositoryImpl", "标准格式解析成功")
                return Result.success(result)
            }
        } catch (e: Exception) {
            android.util.Log.d("AiRepositoryImpl", "标准格式解析失败: ${e.message}，尝试字段映射")
        }
        
        // 4. 标准格式解析失败，尝试字段映射
        android.util.Log.d("AiRepositoryImpl", "尝试字段映射...")
        val mappedResult = mapNonStandardFieldsToAnalysisResult(cleanedJson)
        if (mappedResult != null) {
            android.util.Log.d("AiRepositoryImpl", "字段映射成功: replySuggestion=${mappedResult.replySuggestion.take(50)}...")
            return Result.success(mappedResult)
        }
        
        // 5. 字段映射也失败，使用Fallback
        android.util.Log.w("AiRepositoryImpl", "字段映射失败，使用Fallback")
        return Result.success(createFallbackAnalysisResult(json))
    }
    
    /**
     * 将非标准字段映射到 AnalysisResult
     *
     * 某些模型（如 Qwen3-Coder）会忽略提示词约束，返回自己的字段结构。
     * 这个方法尝试从这些非标准结构中提取有用信息。
     *
     * 支持的非标准字段映射：
     * - response_suggestions.recommended_response → replySuggestion
     * - response_suggestions.immediate_response → replySuggestion
     * - analysis.emotional_state + analysis.underlying_intention → strategyAnalysis
     * - analysis.risk_assessment / risk_assessment → riskLevel
     */
    @Suppress("UNCHECKED_CAST")
    private fun mapNonStandardFieldsToAnalysisResult(json: String): AnalysisResult? {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            ).lenient()
            val jsonMap = mapAdapter.fromJson(json) ?: return null
            
            // 提取回复建议
            val replySuggestion = extractReplySuggestion(jsonMap)
            if (replySuggestion.isBlank()) {
                android.util.Log.w("AiRepositoryImpl", "无法从非标准格式中提取回复建议")
                return null
            }
            
            // 提取策略分析
            val strategyAnalysis = extractStrategyAnalysis(jsonMap)
            
            // 提取风险等级
            val riskLevel = extractRiskLevel(jsonMap)
            
            AnalysisResult(
                replySuggestion = replySuggestion,
                strategyAnalysis = strategyAnalysis,
                riskLevel = riskLevel
            )
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "字段映射异常", e)
            null
        }
    }
    
    /**
     * 从非标准 JSON 中提取回复建议
     * 
     * 支持的字段名（按优先级）：
     * - replySuggestion（标准）
     * - suggested_response（Qwen3-Coder 常用）
     * - response_suggestions.recommended_response
     * - recommendation, reply, response 等
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractReplySuggestion(jsonMap: Map<String, Any>): String {
        // 尝试标准字段
        (jsonMap["replySuggestion"] as? String)?.let { return it }
        
        // 尝试 suggested_response（Qwen3-Coder 常用字段）
        (jsonMap["suggested_response"] as? String)?.let { return it }
        
        // 尝试 response_suggestions.recommended_response
        (jsonMap["response_suggestions"] as? Map<String, Any>)?.let { suggestions ->
            (suggestions["recommended_response"] as? String)?.let { return it }
            (suggestions["immediate_response"] as? String)?.let { return it }
            // 尝试 alternative_responses 数组的第一个
            (suggestions["alternative_responses"] as? List<String>)?.firstOrNull()?.let { return it }
        }
        
        // 尝试 suggestions.recommended_response
        (jsonMap["suggestions"] as? Map<String, Any>)?.let { suggestions ->
            (suggestions["recommended_response"] as? String)?.let { return it }
        }
        
        // 尝试其他常见字段名
        (jsonMap["recommendation"] as? String)?.let { return it }
        (jsonMap["reply"] as? String)?.let { return it }
        (jsonMap["response"] as? String)?.let { return it }
        (jsonMap["suggestion"] as? String)?.let { return it }
        
        return ""
    }
    
    /**
     * 从非标准 JSON 中提取策略分析
     * 
     * 支持的字段结构：
     * - strategyAnalysis（标准）
     * - analysis.emotional_state + analysis.underlying_intentions（Qwen3-Coder）
     * - warnings + strategies 数组（Qwen3-Coder）
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractStrategyAnalysis(jsonMap: Map<String, Any>): String {
        // 尝试标准字段
        (jsonMap["strategyAnalysis"] as? String)?.let { return it }
        
        val parts = mutableListOf<String>()
        
        // 尝试 analysis 对象（Qwen3-Coder 常用结构）
        (jsonMap["analysis"] as? Map<String, Any>)?.let { analysis ->
            (analysis["emotional_state"] as? String)?.let { parts.add("情绪状态: $it") }
            (analysis["underlying_intentions"] as? String)?.let { parts.add("潜在意图: $it") }
            (analysis["underlying_intention"] as? String)?.let { parts.add("潜在意图: $it") }
            (analysis["relationship_stage"] as? String)?.let { parts.add("关系阶段: $it") }
            (analysis["risk_assessment"] as? String)?.let { parts.add("风险评估: $it") }
        }
        
        // 尝试 warnings 数组（Qwen3-Coder 常用）
        (jsonMap["warnings"] as? List<String>)?.let { warnings ->
            if (warnings.isNotEmpty()) {
                parts.add("注意事项: ${warnings.joinToString("、")}")
            }
        }
        
        // 尝试 strategies 数组（Qwen3-Coder 常用）
        (jsonMap["strategies"] as? List<String>)?.let { strategies ->
            if (strategies.isNotEmpty()) {
                parts.add("建议策略: ${strategies.joinToString("、")}")
            }
        }
        
        if (parts.isNotEmpty()) {
            return parts.joinToString("; ")
        }
        
        // 尝试 strategic_recommendations
        (jsonMap["strategic_recommendations"] as? Map<String, Any>)?.let { recommendations ->
            (recommendations["approach_strategy"] as? String)?.let { return it }
        }
        
        // 尝试其他常见字段
        (jsonMap["strategy"] as? String)?.let { return it }
        (jsonMap["analysis_result"] as? String)?.let { return it }
        
        return "AI 分析完成"
    }
    
    /**
     * 从非标准 JSON 中提取风险等级
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractRiskLevel(jsonMap: Map<String, Any>): RiskLevel {
        // 尝试标准字段
        (jsonMap["riskLevel"] as? String)?.let { level ->
            return parseRiskLevel(level)
        }
        
        // 尝试 risk_assessment
        (jsonMap["risk_assessment"] as? Map<String, Any>)?.let { assessment ->
            (assessment["level"] as? String)?.let { return parseRiskLevel(it) }
            (assessment["risk_level"] as? String)?.let { return parseRiskLevel(it) }
        }
        
        // 尝试 analysis.risk_assessment
        (jsonMap["analysis"] as? Map<String, Any>)?.let { analysis ->
            (analysis["risk_assessment"] as? String)?.let { return parseRiskLevel(it) }
        }
        
        // 尝试其他常见字段
        (jsonMap["risk"] as? String)?.let { return parseRiskLevel(it) }
        (jsonMap["risk_level"] as? String)?.let { return parseRiskLevel(it) }
        
        // 默认返回 SAFE
        return RiskLevel.SAFE
    }
    
    /**
     * 解析风险等级字符串
     */
    private fun parseRiskLevel(level: String): RiskLevel {
        val normalizedLevel = level.uppercase().trim()
        return when {
            normalizedLevel.contains("DANGER") || normalizedLevel.contains("HIGH") -> RiskLevel.DANGER
            normalizedLevel.contains("WARNING") || normalizedLevel.contains("MEDIUM") || normalizedLevel.contains("CAUTION") -> RiskLevel.WARNING
            normalizedLevel.contains("SAFE") || normalizedLevel.contains("LOW") -> RiskLevel.SAFE
            else -> RiskLevel.SAFE
        }
    }
    
    /**
     * 创建Fallback的AnalysisResult
     *
     * 当AI返回非JSON格式时，将原始文本转换为可用的结果
     */
    private fun createFallbackAnalysisResult(originalText: String): AnalysisResult {
        // 提取有用的文本内容
        val cleanText = originalText
            .replace(Regex("#+\\s*"), "") // 移除Markdown标题
            .replace(Regex("\\*\\*"), "") // 移除加粗标记
            .replace(Regex("- "), "") // 移除列表标记
            .trim()
        
        return AnalysisResult(
            replySuggestion = extractReplyFromText(cleanText),
            strategyAnalysis = extractAnalysisFromText(cleanText),
            riskLevel = RiskLevel.WARNING // 默认为WARNING，因为无法准确判断
        )
    }
    
    /**
     * 从文本中提取回复建议
     */
    private fun extractReplyFromText(text: String): String {
        // 尝试提取"推荐回复"或"回复建议"部分
        val replyPatterns = listOf(
            Regex("推荐回复[：:】]?[\\s\\n]*[\"\"]([^\"\"]*)[\"\"]"),
            Regex("回复建议[：:】]?[\\s\\n]*[\"\"]([^\"\"]*)[\"\"]"),
            Regex("建议回复[：:】]?[\\s\\n]*[\"\"]([^\"\"]*)[\"\"]")
        )
        
        for (pattern in replyPatterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        
        // 如果没有找到，返回默认建议
        return "AI返回格式异常，建议：保持真诚和友善的态度进行回复"
    }
    
    /**
     * 从文本中提取策略分析
     */
    private fun extractAnalysisFromText(text: String): String {
        // 提取前200个字符作为分析摘要
        val summary = text.take(200)
        return "AI分析摘要：$summary${if (text.length > 200) "..." else ""}"
    }

    /**
     * 解析 SafetyCheckResult JSON
     *
     * 将 AI 返回的 JSON 字符串解析为 SafetyCheckResult Domain 模型。
     *
     * @param json AI 返回的 JSON 字符串
     * @return 解析结果
     */
    private fun parseSafetyCheckResult(json: String): Result<SafetyCheckResult> {
        return try {
            // 1. 使用EnhancedJsonCleaner清洗JSON
            val jsonCleaner = com.empathy.ai.data.parser.EnhancedJsonCleaner()
            val cleaningContext = com.empathy.ai.data.parser.CleaningContext(
                enableUnicodeFix = true,
                enableFormatFix = true,
                enableFuzzyFix = true,
                enableDetailedLogging = true
            )
            val cleanedJson = jsonCleaner.clean(json, cleaningContext)
            
            // 2. 解析清洗后的JSON
            val adapter = moshi.adapter(SafetyCheckResult::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)

            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to parse AI response as SafetyCheckResult. Cleaned JSON: $cleanedJson"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "Failed to parse SafetyCheckResult. Original: $json", e)
            Result.failure(e)
        }
    }

    /**
     * 解析 ExtractedData JSON
     *
     * 将 AI 返回的 JSON 字符串解析为 ExtractedData Domain 模型。
     *
     * @param json AI 返回的 JSON 字符串
     * @return 解析结果
     */
    private fun parseExtractedData(json: String): Result<ExtractedData> {
        return try {
            // 1. 使用EnhancedJsonCleaner清洗JSON
            val jsonCleaner = com.empathy.ai.data.parser.EnhancedJsonCleaner()
            val cleaningContext = com.empathy.ai.data.parser.CleaningContext(
                enableUnicodeFix = true,
                enableFormatFix = true,
                enableFuzzyFix = true,
                enableDetailedLogging = true
            )
            val cleanedJson = jsonCleaner.clean(json, cleaningContext)
            
            // 2. 解析清洗后的JSON
            val adapter = moshi.adapter(ExtractedData::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)

            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to parse AI response as ExtractedData. Cleaned JSON: $cleanedJson"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "Failed to parse ExtractedData. Original: $json", e)
            Result.failure(e)
        }
    }

    /**
     * 通用文本生成
     *
     * 调用 AI 模型生成文本，用于每日总结等场景。
     *
     * @param provider AI服务商配置
     * @param prompt 用户提示词
     * @param systemInstruction 系统指令
     * @return 生成的文本
     */
    override suspend fun generateText(
        provider: com.empathy.ai.domain.model.AiProvider,
        prompt: String,
        systemInstruction: String
    ): Result<String> {
        return try {
            // 1. 从provider获取配置并构建完整URL
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )

            // 2. 选择模型
            val model = if (provider.defaultModelId.isNotBlank()) {
                provider.defaultModelId
            } else {
                when {
                    provider.name.contains("DeepSeek", ignoreCase = true) -> MODEL_DEEPSEEK
                    provider.name.contains("OpenAI", ignoreCase = true) -> MODEL_OPENAI
                    else -> MODEL_OPENAI
                }
            }

            // 3. 构建消息
            val messages = listOf(
                MessageDto(role = "system", content = systemInstruction),
                MessageDto(role = "user", content = prompt)
            )

            val request = ChatRequestDto(
                model = model,
                messages = messages,
                temperature = 0.7,
                stream = false
            )

            android.util.Log.d("AiRepositoryImpl", "=== API请求详情 (generateText) ===")
            android.util.Log.d("AiRepositoryImpl", "URL: $url")
            android.util.Log.d("AiRepositoryImpl", "Model: $model")
            android.util.Log.d("AiRepositoryImpl", "Provider: ${provider.name}")

            // 4. 调用 API（带重试）
            val response = withRetry {
                api.chatCompletion(url, headers, request)
            }

            // 5. 提取响应内容
            val content = response.choices.firstOrNull()
                ?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))

            Result.success(content)

        } catch (e: retrofit2.HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: "No error body"
            } catch (ex: Exception) {
                "Failed to read error body: ${ex.message}"
            }

            android.util.Log.e("AiRepositoryImpl", "=== HTTP错误详情 (generateText) ===")
            android.util.Log.e("AiRepositoryImpl", "状态码: ${e.code()}")
            android.util.Log.e("AiRepositoryImpl", "错误信息: ${e.message()}")
            android.util.Log.e("AiRepositoryImpl", "错误体: $errorBody")
            android.util.Log.e("AiRepositoryImpl", "Provider: ${provider.name}")

            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "文本生成失败 (generateText)", e)
            Result.failure(e)
        }
    }
}
