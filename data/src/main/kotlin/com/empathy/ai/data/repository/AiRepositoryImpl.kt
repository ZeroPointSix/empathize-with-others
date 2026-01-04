/**
 * Package com.empathy.ai.data.repository 实现了AI服务访问层
 *
 * 业务背景 (PRD-00025):
 *   - 支持多AI服务商（OpenAI、DeepSeek、魔搭、Claude等）
 *   - 统一的接口抽象，屏蔽服务商差异
 *   - API用量统计与监控（TD-00025新增）
 *
 * 设计决策 (TDD-00025):
 *   - 使用 ProviderCompatibility 封装服务商差异判断
 *   - 多策略结构化输出：Function Calling > Response Format > Prompt Only
 *   - 指数退避重试机制：平衡用户体验与系统稳定性
 *   - Token估算算法：基于字符集分组的经验公式
 *
 * 任务追踪:
 *   - TD-00025 - AI配置功能完善
 */
package com.empathy.ai.data.repository

import android.util.Log
import com.empathy.ai.data.parser.CleaningContext
import com.empathy.ai.data.parser.EnhancedJsonCleaner
import com.empathy.ai.data.remote.api.OpenAiApi
import com.empathy.ai.data.remote.model.ChatRequestDto
import com.empathy.ai.data.remote.model.FunctionDefinition
import com.empathy.ai.data.remote.model.FunctionParameters
import com.empathy.ai.data.remote.model.MessageDto
import com.empathy.ai.data.remote.model.PropertyDefinition
import com.empathy.ai.data.remote.model.ResponseFormat
import com.empathy.ai.data.remote.model.ToolChoice
import com.empathy.ai.data.remote.model.ToolChoiceFunction
import com.empathy.ai.data.remote.model.ToolDefinition
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ApiUsageRecord
import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.ApiUsageRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.util.SystemPrompts
import com.empathy.ai.data.util.AiResponseCleaner
import com.empathy.ai.data.util.DebugLogger
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import javax.inject.Inject

/**
 * AI 服务仓库实现类
 *
 * 核心职责:
 *   - AI API 请求封装与响应解析
 *   - 多服务商兼容适配
 *   - API 用量统计与错误处理
 *
 * 设计权衡 (TDD-00025):
 *   - 选择 Optional 依赖 apiUsageRepository：兼容旧版本不崩溃
 *   - 降级策略：加密存储失败时使用明文存储（日志警告）
 *
 * 任务追踪:
 *   - TD-00025 - AI配置功能完善（用量统计）
 */
class AiRepositoryImpl @Inject constructor(
    private val api: OpenAiApi,
    private val settingsRepository: SettingsRepository,
    private val apiUsageRepository: ApiUsageRepository? = null  // TD-00025: 可选依赖，支持用量记录
) : AiRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
        const val MODEL_OPENAI = "gpt-3.5-turbo"
        const val MODEL_DEEPSEEK = "deepseek-chat"

        private const val COMMON_JSON_RULES = """
【通用 JSON 格式规则】
- 不要返回任何 Markdown 格式
- 只返回一行有效的 JSON 对象
"""

        val SYSTEM_ANALYZE = """你是一个专业的社交沟通顾问。分析用户的聊天内容并给出建议。

你的回复必须是且只能是以下格式的 JSON：
{"replySuggestion":"建议的回复内容","strategyAnalysis":"策略分析","riskLevel":"SAFE"}

【字段说明】
- replySuggestion: 建议用户如何回复对方
- strategyAnalysis: 分析对方的情绪、意图和沟通策略
- riskLevel: 风险等级，只能是 SAFE、WARNING 或 DANGER
$COMMON_JSON_RULES""".trim()

        val SYSTEM_CHECK = """你是一个社交风控专家。检查用户的草稿是否触发了风险规则。

你的回复必须是且只能是以下格式的 JSON：
{"isSafe":true,"triggeredRisks":[],"suggestion":"内容安全"}
$COMMON_JSON_RULES""".trim()

        val SYSTEM_EXTRACT = """你是一个专业的社交信息分析专家。从文本中提取关键信息。

你的回复必须是且只能是以下格式的 JSON：
{"facts":{"birthday":"12月21日"},"redTags":["不要提前任"],"greenTags":["耐心倾听"]}
$COMMON_JSON_RULES""".trim()

        val TOOL_ANALYZE_CHAT = ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = "generate_analysis_result",
                description = "生成聊天分析结果",
                parameters = FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "replySuggestion" to PropertyDefinition(type = "string", description = "建议回复"),
                        "strategyAnalysis" to PropertyDefinition(type = "string", description = "策略分析"),
                        "riskLevel" to PropertyDefinition(type = "string", description = "风险等级", enum = listOf("SAFE", "WARNING", "DANGER"))
                    ),
                    required = listOf("replySuggestion", "strategyAnalysis", "riskLevel")
                )
            )
        )

        val TOOL_CHECK_SAFETY = ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = "generate_safety_result",
                description = "生成安全检查结果",
                parameters = FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "isSafe" to PropertyDefinition(type = "boolean", description = "是否安全"),
                        "triggeredRisks" to PropertyDefinition(type = "array", description = "触发的风险"),
                        "suggestion" to PropertyDefinition(type = "string", description = "建议")
                    ),
                    required = listOf("isSafe", "triggeredRisks", "suggestion")
                )
            )
        )

        val TOOL_EXTRACT_INFO = ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = "generate_extracted_data",
                description = "提取关键信息",
                parameters = FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "facts" to PropertyDefinition(type = "object", description = "事实信息"),
                        "redTags" to PropertyDefinition(type = "array", description = "雷区标签"),
                        "greenTags" to PropertyDefinition(type = "array", description = "策略标签")
                    ),
                    required = listOf("facts", "redTags", "greenTags")
                )
            )
        )

        const val SYSTEM_ANALYZE_FC = """你是一个专业的社交沟通顾问。分析用户的聊天内容并给出建议。
请分析对话内容，然后调用 generate_analysis_result 函数返回结果。"""

        const val SYSTEM_CHECK_FC = """你是一个社交风控专家。检查用户的草稿是否触发了风险规则。
请检查草稿内容，然后调用 generate_safety_result 函数返回结果。"""

        const val SYSTEM_EXTRACT_FC = """你是一个专业的社交信息分析专家。从文本中提取关键信息。
请分析文本内容，然后调用 generate_extracted_data 函数返回结果。"""
    }

    // ==================== TD-00025: 用量记录辅助方法 ====================

    /**
     * 记录API用量
     *
     * @param providerId 服务商ID
     * @param providerName 服务商名称
     * @param modelId 模型ID
     * @param modelName 模型名称
     * @param promptTokens 输入Token数（估算）
     * @param completionTokens 输出Token数（估算）
     * @param requestTimeMs 请求耗时（毫秒）
     * @param isSuccess 是否成功
     * @param errorMessage 错误消息（失败时）
     */
    private suspend fun recordUsage(
        providerId: String,
        providerName: String,
        modelId: String,
        modelName: String = modelId,
        promptTokens: Int,
        completionTokens: Int,
        requestTimeMs: Long,
        isSuccess: Boolean,
        errorMessage: String? = null
    ) {
        try {
            apiUsageRepository?.recordUsage(
                ApiUsageRecord(
                    id = UUID.randomUUID().toString(),
                    providerId = providerId,
                    providerName = providerName,
                    modelId = modelId,
                    modelName = modelName,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = promptTokens + completionTokens,
                    requestTimeMs = requestTimeMs,
                    isSuccess = isSuccess,
                    errorMessage = errorMessage,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.w("AiRepositoryImpl", "记录用量失败: ${e.message}")
        }
    }

    /**
     * estimateTokens 估算Token数量
     *
     * [Token Estimation] 简化的Token估算算法
     * 权衡：准确估算需要加载 tokenizer 模型，开销过大
     * 采用经验公式：
     *   - 中文字符：约 1.5 字符/token（汉字信息密度高）
     *   - 英文字符：约 4 字符/token
     *   - 至少返回 1，确保统计正确性
     *
     * 设计权衡 (TDD-00025):
     *   - 选择字符集分组估算：比固定比率更准确，比加载模型更轻量
     *   - coerceAtLeast(1)：避免除零导致的异常
     */
    private fun estimateTokens(text: String): Int {
        val chineseCount = text.count { it.code in 0x4E00..0x9FFF }
        val otherCount = text.length - chineseCount
        return (chineseCount / 1.5 + otherCount / 4.0).toInt().coerceAtLeast(1)
    }

    /**
     * withRetry 实现指数退避重试策略
     *
     * [Retry Strategy] 指数退避重试机制
     * 场景区分：
     *   - SocketTimeoutException：可重试（服务端繁忙）
     *   - IOException（含Canceled）：不可重试（用户主动取消）
     *   - 其他Exception：立即失败（业务逻辑错误）
     *
     * 设计权衡 (TDD-00025):
     *   - 选择指数退避而非固定间隔：减少服务器压力
     *   - 最大重试3次：平衡用户体验与系统稳定性
     *   - 延迟公式：INITIAL_DELAY_MS * 2^attempt
     */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_DELAY_MS * (1 shl attempt)
                    Log.w("AiRepositoryImpl", "请求超时，${delayMs}ms 后重试 (${attempt + 1}/$MAX_RETRIES)")
                    delay(delayMs)
                }
            } catch (e: IOException) {
                if (e.message?.contains("Canceled", ignoreCase = true) == true) {
                    throw e
                }
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_DELAY_MS * (1 shl attempt)
                    Log.w("AiRepositoryImpl", "网络错误，${delayMs}ms 后重试 (${attempt + 1}/$MAX_RETRIES)")
                    delay(delayMs)
                }
            } catch (e: Exception) {
                throw e
            }
        }
        throw lastException ?: Exception("未知错误")
    }


    override suspend fun analyzeChat(
        provider: AiProvider,
        promptContext: String,
        systemInstruction: String
    ): Result<AnalysisResult> {
        val startTime = System.currentTimeMillis()
        val model = selectModel(provider)
        // TD-00025: 使用服务商配置的 temperature 和 maxTokens
        val effectiveTemperature = provider.temperature.toDouble()
        val effectiveMaxTokens = if (provider.maxTokens > 0) provider.maxTokens else null
        
        return try {
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )
            val strategy = ProviderCompatibility.getStructuredOutputStrategy(provider)

            DebugLogger.logApiRequest(
                tag = "AiRepositoryImpl",
                method = "analyzeChat",
                url = url,
                model = model,
                providerName = provider.name,
                promptContext = promptContext,
                additionalInfo = mapOf("Strategy" to strategy),
                temperature = provider.temperature,
                maxTokens = effectiveMaxTokens
            )

            val request = when (strategy) {
                ProviderCompatibility.StructuredOutputStrategy.FUNCTION_CALLING -> {
                    val effectiveSystemInstruction = if (systemInstruction.isNotBlank()) {
                        "$SYSTEM_ANALYZE_FC\n\n$systemInstruction"
                    } else SYSTEM_ANALYZE_FC
                    ChatRequestDto(
                        model = model,
                        messages = listOf(
                            MessageDto(role = "system", content = effectiveSystemInstruction),
                            MessageDto(role = "user", content = promptContext)
                        ),
                        temperature = effectiveTemperature,
                        maxTokens = effectiveMaxTokens,
                        stream = false,
                        tools = listOf(TOOL_ANALYZE_CHAT),
                        toolChoice = ToolChoice(type = "function", function = ToolChoiceFunction(name = "generate_analysis_result"))
                    )
                }
                ProviderCompatibility.StructuredOutputStrategy.RESPONSE_FORMAT -> {
                    val effectiveSystemInstruction = systemInstruction.ifEmpty { SYSTEM_ANALYZE }
                    val messages = listOf(
                        MessageDto(role = "system", content = effectiveSystemInstruction),
                        MessageDto(role = "user", content = promptContext)
                    )
                    val adaptedMessages = if (ProviderCompatibility.requiresJsonKeywordInMessages(provider)) {
                        messages.map { msg ->
                            if (msg.role == "system") msg.copy(content = ProviderCompatibility.adaptSystemInstruction(provider, msg.content))
                            else msg
                        }
                    } else messages
                    ChatRequestDto(
                        model = model,
                        messages = adaptedMessages,
                        temperature = effectiveTemperature,
                        maxTokens = effectiveMaxTokens,
                        stream = false,
                        responseFormat = ResponseFormat(type = "json_object")
                    )
                }
                ProviderCompatibility.StructuredOutputStrategy.PROMPT_ONLY -> {
                    val effectiveSystemInstruction = systemInstruction.ifEmpty { SYSTEM_ANALYZE }
                    ChatRequestDto(
                        model = model,
                        messages = listOf(
                            MessageDto(role = "system", content = effectiveSystemInstruction),
                            MessageDto(role = "user", content = promptContext)
                        ),
                        temperature = effectiveTemperature,
                        maxTokens = effectiveMaxTokens,
                        stream = false
                    )
                }
            }

            val response = withRetry { api.chatCompletion(url, headers, request) }
            val choice = response.choices.firstOrNull()
                ?: return Result.failure(Exception("Empty response from AI"))

            val content = if (strategy == ProviderCompatibility.StructuredOutputStrategy.FUNCTION_CALLING) {
                val toolCall = choice.message?.toolCalls?.firstOrNull()
                if (toolCall != null && toolCall.function?.arguments != null) {
                    toolCall.function.arguments
                } else {
                    choice.message?.content ?: ""
                }
            } else {
                choice.message?.content ?: ""
            }

            // TD-00025: 记录成功的用量
            val requestTimeMs = System.currentTimeMillis() - startTime
            recordUsage(
                providerId = provider.id,
                providerName = provider.name,
                modelId = model,
                promptTokens = estimateTokens(systemInstruction + promptContext),
                completionTokens = estimateTokens(content),
                requestTimeMs = requestTimeMs,
                isSuccess = true
            )

            if (strategy == ProviderCompatibility.StructuredOutputStrategy.FUNCTION_CALLING) {
                val toolCall = choice.message?.toolCalls?.firstOrNull()
                if (toolCall != null && toolCall.function?.arguments != null) {
                    return parseAnalysisResultDirect(toolCall.function.arguments)
                }
            }

            if (content.isBlank()) {
                return Result.failure(Exception("Empty response from AI"))
            }
            parseAnalysisResult(content)

        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() ?: "No error body" } catch (ex: Exception) { "Failed to read error body" }
            Log.e("AiRepositoryImpl", "HTTP错误 (analyzeChat): ${e.code()} - $errorBody")
            // TD-00025: 记录失败的用量
            val requestTimeMs = System.currentTimeMillis() - startTime
            recordUsage(
                providerId = provider.id,
                providerName = provider.name,
                modelId = model,
                promptTokens = estimateTokens(systemInstruction + promptContext),
                completionTokens = 0,
                requestTimeMs = requestTimeMs,
                isSuccess = false,
                errorMessage = "HTTP ${e.code()}"
            )
            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "分析失败 (analyzeChat)", e)
            // TD-00025: 记录失败的用量
            val requestTimeMs = System.currentTimeMillis() - startTime
            recordUsage(
                providerId = provider.id,
                providerName = provider.name,
                modelId = model,
                promptTokens = estimateTokens(systemInstruction + promptContext),
                completionTokens = 0,
                requestTimeMs = requestTimeMs,
                isSuccess = false,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }


    override suspend fun checkDraftSafety(
        provider: AiProvider,
        draft: String,
        riskRules: List<String>,
        systemInstruction: String?
    ): Result<SafetyCheckResult> {
        return try {
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )
            val model = selectModel(provider)
            val riskRulesText = riskRules.joinToString(", ")
            val checkPrompt = """用户草稿: "$draft"
风险规则: [$riskRulesText]
请检查草稿是否触发了任何风险规则。"""

            val effectiveSystemInstruction = systemInstruction ?: SYSTEM_CHECK
            val messages = listOf(
                MessageDto(role = "system", content = effectiveSystemInstruction),
                MessageDto(role = "user", content = checkPrompt)
            )

            val useResponseFormat = ProviderCompatibility.supportsResponseFormat(provider)
            val adaptedMessages = if (ProviderCompatibility.requiresJsonKeywordInMessages(provider)) {
                messages.map { msg ->
                    if (msg.role == "system") msg.copy(content = ProviderCompatibility.adaptSystemInstruction(provider, msg.content))
                    else msg
                }
            } else messages

            val request = ChatRequestDto(
                model = model,
                messages = adaptedMessages,
                temperature = 0.3,
                stream = false,
                responseFormat = if (useResponseFormat) ResponseFormat(type = "json_object") else null
            )

            val response = withRetry { api.chatCompletion(url, headers, request) }
            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))
            parseSafetyCheckResult(content)

        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() ?: "No error body" } catch (ex: Exception) { "Failed to read error body" }
            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "安全检查失败", e)
            Result.failure(e)
        }
    }

    override suspend fun extractTextInfo(provider: AiProvider, inputText: String): Result<ExtractedData> {
        return try {
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )
            val model = selectModel(provider)
            val extractPrompt = """请分析以下文本并提取关键信息：
"$inputText""""

            val messages = listOf(
                MessageDto(role = "system", content = SYSTEM_EXTRACT),
                MessageDto(role = "user", content = extractPrompt)
            )

            val useResponseFormat = ProviderCompatibility.supportsResponseFormat(provider)
            val adaptedMessages = if (ProviderCompatibility.requiresJsonKeywordInMessages(provider)) {
                messages.map { msg ->
                    if (msg.role == "system") msg.copy(content = ProviderCompatibility.adaptSystemInstruction(provider, msg.content))
                    else msg
                }
            } else messages

            val request = ChatRequestDto(
                model = model,
                messages = adaptedMessages,
                temperature = 0.5,
                stream = false,
                responseFormat = if (useResponseFormat) ResponseFormat(type = "json_object") else null
            )

            val response = api.chatCompletion(url, headers, request)
            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))
            parseExtractedData(content)

        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() ?: "No error body" } catch (ex: Exception) { "Failed to read error body" }
            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "信息提取失败", e)
            Result.failure(e)
        }
    }

    override suspend fun transcribeMedia(mediaFilePath: String): Result<String> {
        return Result.failure(Exception("Media transcription not implemented yet."))
    }


    override suspend fun generateText(
        provider: AiProvider,
        prompt: String,
        systemInstruction: String
    ): Result<String> {
        return try {
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )
            val model = selectModel(provider)
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

            val response = withRetry { api.chatCompletion(url, headers, request) }
            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))
            Result.success(content)

        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() ?: "No error body" } catch (ex: Exception) { "Failed to read error body" }
            Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "文本生成失败", e)
            Result.failure(e)
        }
    }

    override suspend fun polishDraft(
        provider: AiProvider,
        draft: String,
        systemInstruction: String
    ): Result<PolishResult> {
        val startTime = System.currentTimeMillis()
        val model = selectModel(provider)
        
        return try {
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )
            val messages = listOf(
                MessageDto(role = "system", content = systemInstruction),
                MessageDto(role = "user", content = draft)
            )

            val useResponseFormat = ProviderCompatibility.supportsResponseFormat(provider)
            // TD-00025: 使用服务商配置的 temperature 和 maxTokens
            val effectiveTemperature = provider.temperature.toDouble()
            val effectiveMaxTokens = if (provider.maxTokens > 0) provider.maxTokens else null
            
            val request = ChatRequestDto(
                model = model,
                messages = messages,
                temperature = effectiveTemperature,
                maxTokens = effectiveMaxTokens,
                stream = false,
                responseFormat = if (useResponseFormat) ResponseFormat(type = "json_object") else null
            )

            DebugLogger.logApiRequest(
                tag = "AiRepositoryImpl",
                method = "polishDraft",
                url = url,
                model = model,
                providerName = provider.name,
                promptContext = draft,
                systemInstruction = systemInstruction,
                additionalInfo = mapOf("UseResponseFormat" to useResponseFormat),
                temperature = provider.temperature,
                maxTokens = effectiveMaxTokens
            )

            val response = withRetry { api.chatCompletion(url, headers, request) }
            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))
            
            // TD-00025: 记录成功的用量
            val requestTimeMs = System.currentTimeMillis() - startTime
            recordUsage(
                providerId = provider.id,
                providerName = provider.name,
                modelId = model,
                promptTokens = estimateTokens(systemInstruction + draft),
                completionTokens = estimateTokens(content),
                requestTimeMs = requestTimeMs,
                isSuccess = true
            )
            
            parsePolishResult(content)

        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "润色失败", e)
            // TD-00025: 记录失败的用量
            val requestTimeMs = System.currentTimeMillis() - startTime
            recordUsage(
                providerId = provider.id,
                providerName = provider.name,
                modelId = model,
                promptTokens = estimateTokens(systemInstruction + draft),
                completionTokens = 0,
                requestTimeMs = requestTimeMs,
                isSuccess = false,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }

    override suspend fun generateReply(
        provider: AiProvider,
        message: String,
        systemInstruction: String
    ): Result<ReplyResult> {
        val startTime = System.currentTimeMillis()
        val model = selectModel(provider)
        
        return try {
            val url = buildChatCompletionsUrl(provider.baseUrl)
            val headers = mapOf(
                "Authorization" to "Bearer ${provider.apiKey}",
                "Content-Type" to "application/json"
            )
            val messages = listOf(
                MessageDto(role = "system", content = systemInstruction),
                MessageDto(role = "user", content = message)
            )

            val useResponseFormat = ProviderCompatibility.supportsResponseFormat(provider)
            // TD-00025: 使用服务商配置的 temperature 和 maxTokens
            val effectiveTemperature = provider.temperature.toDouble()
            val effectiveMaxTokens = if (provider.maxTokens > 0) provider.maxTokens else null
            
            val request = ChatRequestDto(
                model = model,
                messages = messages,
                temperature = effectiveTemperature,
                maxTokens = effectiveMaxTokens,
                stream = false,
                responseFormat = if (useResponseFormat) ResponseFormat(type = "json_object") else null
            )

            DebugLogger.logApiRequest(
                tag = "AiRepositoryImpl",
                method = "generateReply",
                url = url,
                model = model,
                providerName = provider.name,
                promptContext = message,
                systemInstruction = systemInstruction,
                additionalInfo = mapOf("UseResponseFormat" to useResponseFormat),
                temperature = provider.temperature,
                maxTokens = effectiveMaxTokens
            )

            val response = withRetry { api.chatCompletion(url, headers, request) }
            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))
            
            // TD-00025: 记录成功的用量
            val requestTimeMs = System.currentTimeMillis() - startTime
            recordUsage(
                providerId = provider.id,
                providerName = provider.name,
                modelId = model,
                promptTokens = estimateTokens(systemInstruction + message),
                completionTokens = estimateTokens(content),
                requestTimeMs = requestTimeMs,
                isSuccess = true
            )
            
            parseReplyResult(content)

        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "生成回复失败", e)
            // TD-00025: 记录失败的用量
            val requestTimeMs = System.currentTimeMillis() - startTime
            recordUsage(
                providerId = provider.id,
                providerName = provider.name,
                modelId = model,
                promptTokens = estimateTokens(systemInstruction + message),
                completionTokens = 0,
                requestTimeMs = requestTimeMs,
                isSuccess = false,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }


    override suspend fun refineAnalysis(provider: AiProvider, refinementPrompt: String): Result<AnalysisResult> {
        val systemInstruction = SystemPrompts.getHeader(PromptScene.ANALYZE) + "\n\n" + SystemPrompts.getFooter(PromptScene.ANALYZE)
        return analyzeChat(provider, refinementPrompt, systemInstruction)
    }

    override suspend fun refinePolish(provider: AiProvider, refinementPrompt: String): Result<PolishResult> {
        val systemInstruction = SystemPrompts.getHeader(PromptScene.POLISH) + "\n\n" + SystemPrompts.getFooter(PromptScene.POLISH)
        return polishDraft(provider, refinementPrompt, systemInstruction)
    }

    override suspend fun refineReply(provider: AiProvider, refinementPrompt: String): Result<ReplyResult> {
        val systemInstruction = SystemPrompts.getHeader(PromptScene.REPLY) + "\n\n" + SystemPrompts.getFooter(PromptScene.REPLY)
        return generateReply(provider, refinementPrompt, systemInstruction)
    }

    // ==================== 辅助方法 ====================

    /**
     * selectModel 根据服务商配置选择最优模型
     *
     * 业务规则 (PRD-00025):
     *   - 优先使用用户配置的 defaultModelId
     *   - 未配置时根据服务商名称推断默认模型
     *   - DeepSeek 专用 deepseek-chat，OpenAI 专用 gpt-3.5-turbo
     *
     * 设计权衡 (TDD-00025):
     *   - 选择名称推断而非硬编码，因为新兴服务商不断出现
     *   - 保留 gpt-3.5-turbo 作为兜底，兼容性最佳
     */
    private fun selectModel(provider: AiProvider): String {
        return if (provider.defaultModelId.isNotBlank()) {
            provider.defaultModelId
        } else {
            when {
                provider.name.contains("DeepSeek", ignoreCase = true) -> MODEL_DEEPSEEK
                provider.name.contains("OpenAI", ignoreCase = true) -> MODEL_OPENAI
                else -> MODEL_OPENAI
            }
        }
    }

    /**
     * buildChatCompletionsUrl 构建标准化的 API URL
     *
     * [URL Normalization] 兼容多种 API URL 格式
     * 支持的格式：
     *   - https://api.example.com/v1/chat/completions（标准）
     *   - https://api.example.com/chat/completions（无版本号）
     *   - https://api.example.com/v1（仅版本号）
     *   - https://api.example.com（基础URL）
     *
     * 设计权衡：
     *   - 自动补全版本号路径，提升用户体验
     *   - trimEnd('/') 避免拼接时的双斜杠问题
     */
    private fun buildChatCompletionsUrl(baseUrl: String): String {
        val trimmedUrl = baseUrl.trimEnd('/')
        return when {
            trimmedUrl.endsWith("/v1/chat/completions") -> trimmedUrl
            trimmedUrl.endsWith("/chat/completions") -> trimmedUrl.removeSuffix("/chat/completions") + "/v1/chat/completions"
            trimmedUrl.endsWith("/v1") -> "$trimmedUrl/chat/completions"
            else -> "$trimmedUrl/v1/chat/completions"
        }
    }

    private fun parseAnalysisResultDirect(json: String): Result<AnalysisResult> {
        return try {
            val adapter = moshi.adapter(AnalysisResult::class.java).lenient()
            val result = adapter.fromJson(json)
            if (result != null && result.replySuggestion.isNotBlank()) {
                val cleanedResult = cleanAnalysisResultSuggestion(result)
                return Result.success(cleanedResult)
            }
            val mappedResult = mapNonStandardFieldsToAnalysisResultDirect(json)
            if (mappedResult != null) {
                val cleanedResult = cleanAnalysisResultSuggestion(mappedResult)
                return Result.success(cleanedResult)
            }
            Result.success(createFallbackAnalysisResult(json))
        } catch (e: Exception) {
            try {
                val mappedResult = mapNonStandardFieldsToAnalysisResultDirect(json)
                if (mappedResult != null) {
                    val cleanedResult = cleanAnalysisResultSuggestion(mappedResult)
                    return Result.success(cleanedResult)
                }
            } catch (ex: Exception) { }
            Result.success(createFallbackAnalysisResult(json))
        }
    }

    private fun cleanAnalysisResultSuggestion(result: AnalysisResult): AnalysisResult {
        val cleanedSuggestion = AiResponseCleaner.smartClean(result.replySuggestion)
        return result.copy(replySuggestion = cleanedSuggestion)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapNonStandardFieldsToAnalysisResultDirect(json: String): AnalysisResult? {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            ).lenient()
            val jsonMap = mapAdapter.fromJson(json) ?: return null
            val replySuggestion = extractReplySuggestion(jsonMap)
            if (replySuggestion.isBlank()) return null
            val strategyAnalysis = extractStrategyAnalysis(jsonMap)
            val riskLevel = extractRiskLevel(jsonMap)
            AnalysisResult(replySuggestion = replySuggestion, strategyAnalysis = strategyAnalysis, riskLevel = riskLevel)
        } catch (e: Exception) { null }
    }


    private fun parseAnalysisResult(json: String): Result<AnalysisResult> {
        val jsonCleaner = EnhancedJsonCleaner()
        val cleaningContext = CleaningContext(
            enableUnicodeFix = true,
            enableFormatFix = true,
            enableFuzzyFix = true,
            enableDetailedLogging = true
        )
        val cleanedJson = jsonCleaner.clean(json, cleaningContext)

        if (cleanedJson == "{}" || cleanedJson.length < 10) {
            return Result.success(createFallbackAnalysisResult(json))
        }

        try {
            val adapter = moshi.adapter(AnalysisResult::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)
            if (result != null && result.replySuggestion.isNotBlank()) {
                val cleanedResult = cleanAnalysisResultSuggestion(result)
                return Result.success(cleanedResult)
            }
        } catch (e: Exception) {
            Log.d("AiRepositoryImpl", "标准格式解析失败: ${e.message}")
        }

        val mappedResult = mapNonStandardFieldsToAnalysisResult(cleanedJson)
        if (mappedResult != null) {
            val cleanedResult = cleanAnalysisResultSuggestion(mappedResult)
            return Result.success(cleanedResult)
        }

        return Result.success(createFallbackAnalysisResult(json))
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapNonStandardFieldsToAnalysisResult(json: String): AnalysisResult? {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            ).lenient()
            val jsonMap = mapAdapter.fromJson(json) ?: return null
            val replySuggestion = extractReplySuggestion(jsonMap)
            if (replySuggestion.isBlank()) return null
            val strategyAnalysis = extractStrategyAnalysis(jsonMap)
            val riskLevel = extractRiskLevel(jsonMap)
            AnalysisResult(replySuggestion = replySuggestion, strategyAnalysis = strategyAnalysis, riskLevel = riskLevel)
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractReplySuggestion(jsonMap: Map<String, Any>): String {
        (jsonMap["replySuggestion"] as? String)?.let { return it }
        (jsonMap["suggested_response"] as? String)?.let { return it }
        (jsonMap["response_suggestions"] as? Map<String, Any>)?.let { suggestions ->
            (suggestions["recommended_response"] as? String)?.let { return it }
            (suggestions["immediate_response"] as? String)?.let { return it }
            (suggestions["alternative_responses"] as? List<String>)?.firstOrNull()?.let { return it }
        }
        (jsonMap["suggestions"] as? Map<String, Any>)?.let { suggestions ->
            (suggestions["recommended_response"] as? String)?.let { return it }
        }
        (jsonMap["recommendation"] as? String)?.let { return it }
        (jsonMap["reply"] as? String)?.let { return it }
        (jsonMap["response"] as? String)?.let { return it }
        (jsonMap["suggestion"] as? String)?.let { return it }
        return ""
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractStrategyAnalysis(jsonMap: Map<String, Any>): String {
        (jsonMap["strategyAnalysis"] as? String)?.let { return it }
        val parts = mutableListOf<String>()
        (jsonMap["analysis"] as? Map<String, Any>)?.let { analysis ->
            (analysis["emotional_state"] as? String)?.let { parts.add("情绪状态: $it") }
            (analysis["underlying_intentions"] as? String)?.let { parts.add("潜在意图: $it") }
            (analysis["underlying_intention"] as? String)?.let { parts.add("潜在意图: $it") }
            (analysis["relationship_stage"] as? String)?.let { parts.add("关系阶段: $it") }
            (analysis["risk_assessment"] as? String)?.let { parts.add("风险评估: $it") }
        }
        (jsonMap["warnings"] as? List<String>)?.let { warnings ->
            if (warnings.isNotEmpty()) parts.add("注意事项: ${warnings.joinToString("、")}")
        }
        (jsonMap["strategies"] as? List<String>)?.let { strategies ->
            if (strategies.isNotEmpty()) parts.add("建议策略: ${strategies.joinToString("、")}")
        }
        if (parts.isNotEmpty()) return parts.joinToString("; ")
        (jsonMap["strategic_recommendations"] as? Map<String, Any>)?.let { recommendations ->
            (recommendations["approach_strategy"] as? String)?.let { return it }
        }
        (jsonMap["strategy"] as? String)?.let { return it }
        (jsonMap["analysis_result"] as? String)?.let { return it }
        return "AI 分析完成"
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractRiskLevel(jsonMap: Map<String, Any>): RiskLevel {
        (jsonMap["riskLevel"] as? String)?.let { return parseRiskLevel(it) }
        (jsonMap["risk_assessment"] as? Map<String, Any>)?.let { assessment ->
            (assessment["level"] as? String)?.let { return parseRiskLevel(it) }
            (assessment["risk_level"] as? String)?.let { return parseRiskLevel(it) }
        }
        (jsonMap["analysis"] as? Map<String, Any>)?.let { analysis ->
            (analysis["risk_assessment"] as? String)?.let { return parseRiskLevel(it) }
        }
        (jsonMap["risk"] as? String)?.let { return parseRiskLevel(it) }
        (jsonMap["risk_level"] as? String)?.let { return parseRiskLevel(it) }
        return RiskLevel.SAFE
    }

    private fun parseRiskLevel(level: String): RiskLevel {
        val normalizedLevel = level.uppercase().trim()
        return when {
            normalizedLevel.contains("DANGER") || normalizedLevel.contains("HIGH") -> RiskLevel.DANGER
            normalizedLevel.contains("WARNING") || normalizedLevel.contains("MEDIUM") || normalizedLevel.contains("CAUTION") -> RiskLevel.WARNING
            normalizedLevel.contains("SAFE") || normalizedLevel.contains("LOW") -> RiskLevel.SAFE
            else -> RiskLevel.SAFE
        }
    }

    private fun createFallbackAnalysisResult(originalText: String): AnalysisResult {
        val cleanText = originalText
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\*\\*"), "")
            .replace(Regex("- "), "")
            .trim()
        return AnalysisResult(
            replySuggestion = extractReplyFromText(cleanText),
            strategyAnalysis = extractAnalysisFromText(cleanText),
            riskLevel = RiskLevel.WARNING
        )
    }

    private fun extractReplyFromText(text: String): String {
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
        return "AI返回格式异常，建议：保持真诚和友善的态度进行回复"
    }

    private fun extractAnalysisFromText(text: String): String {
        val summary = text.take(200)
        return "AI分析摘要：$summary${if (text.length > 200) "..." else ""}"
    }

    // ==================== 解析方法 ====================

    /**
     * 解析 SafetyCheckResult JSON
     */
    private fun parseSafetyCheckResult(json: String): Result<SafetyCheckResult> {
        return try {
            val jsonCleaner = EnhancedJsonCleaner()
            val cleaningContext = CleaningContext(
                enableUnicodeFix = true,
                enableFormatFix = true,
                enableFuzzyFix = true,
                enableDetailedLogging = true
            )
            val cleanedJson = jsonCleaner.clean(json, cleaningContext)

            val adapter = moshi.adapter(SafetyCheckResult::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)

            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to parse AI response as SafetyCheckResult. Cleaned JSON: $cleanedJson"))
            }
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "Failed to parse SafetyCheckResult. Original: $json", e)
            Result.failure(e)
        }
    }

    /**
     * 解析 ExtractedData JSON
     */
    private fun parseExtractedData(json: String): Result<ExtractedData> {
        return try {
            val jsonCleaner = EnhancedJsonCleaner()
            val cleaningContext = CleaningContext(
                enableUnicodeFix = true,
                enableFormatFix = true,
                enableFuzzyFix = true,
                enableDetailedLogging = true
            )
            val cleanedJson = jsonCleaner.clean(json, cleaningContext)

            val adapter = moshi.adapter(ExtractedData::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)

            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to parse AI response as ExtractedData. Cleaned JSON: $cleanedJson"))
            }
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "Failed to parse ExtractedData. Original: $json", e)
            Result.failure(e)
        }
    }

    /**
     * 解析润色结果
     */
    private fun parsePolishResult(json: String): Result<PolishResult> {
        return try {
            val jsonCleaner = EnhancedJsonCleaner()
            val cleaningContext = CleaningContext(
                enableUnicodeFix = true,
                enableFormatFix = true,
                enableFuzzyFix = true,
                enableDetailedLogging = true
            )
            val cleanedJson = jsonCleaner.clean(json, cleaningContext)

            val adapter = moshi.adapter(PolishResult::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)

            if (result != null && result.polishedText.isNotBlank()) {
                Result.success(result)
            } else {
                // Fallback: 将原始内容作为润色结果
                Result.success(
                    PolishResult(
                        polishedText = extractTextFromResponse(json),
                        hasRisk = false,
                        riskWarning = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "解析润色结果失败", e)
            // Fallback
            Result.success(
                PolishResult(
                    polishedText = extractTextFromResponse(json),
                    hasRisk = false,
                    riskWarning = null
                )
            )
        }
    }

    /**
     * 解析回复结果
     */
    private fun parseReplyResult(json: String): Result<ReplyResult> {
        return try {
            val jsonCleaner = EnhancedJsonCleaner()
            val cleaningContext = CleaningContext(
                enableUnicodeFix = true,
                enableFormatFix = true,
                enableFuzzyFix = true,
                enableDetailedLogging = true
            )
            val cleanedJson = jsonCleaner.clean(json, cleaningContext)

            val adapter = moshi.adapter(ReplyResult::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)

            if (result != null && result.suggestedReply.isNotBlank()) {
                Result.success(result)
            } else {
                // Fallback: 将原始内容作为回复建议
                Result.success(
                    ReplyResult(
                        suggestedReply = extractTextFromResponse(json),
                        strategyNote = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AiRepositoryImpl", "解析回复结果失败", e)
            // Fallback
            Result.success(
                ReplyResult(
                    suggestedReply = extractTextFromResponse(json),
                    strategyNote = null
                )
            )
        }
    }

    /**
     * 从响应中提取文本（用于Fallback）
     */
    private fun extractTextFromResponse(response: String): String {
        return response
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\*\\*"), "")
            .trim()
            .take(500)
    }
}
