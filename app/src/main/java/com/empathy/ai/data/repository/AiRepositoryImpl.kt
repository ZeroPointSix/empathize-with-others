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
        val SYSTEM_ANALYZE = """你是一个专业的社交沟通顾问。

【强制 JSON 格式要求】
你的回复必须是有效的 JSON 格式，且必须符合以下 JSON Schema：
{
  "type": "object",
  "properties": {
    "replySuggestion": {"type": "string"},
    "strategyAnalysis": {"type": "string"},
    "riskLevel": {"type": "string", "enum": ["SAFE", "WARNING", "DANGER"]}
  },
  "required": ["replySuggestion", "strategyAnalysis", "riskLevel"],
  "additionalProperties": false
}

【强制要求】
1. 你必须且只能返回有效的 JSON 对象，不要返回任何其他文本、Markdown、代码块或解释
2. 返回的 JSON 必须包含以下三个英文 Key，且只能是这三个 Key：
   - replySuggestion (字符串类型，必须)
   - strategyAnalysis (字符串类型，必须)
   - riskLevel (字符串类型，必须，只能是 SAFE、WARNING 或 DANGER)
3. 不允许添加任何额外的 Key 或嵌套结构
4. 所有 Value 必须是字符串类型，不允许 null 或其他类型
5. 如果无法分析，也必须返回上述格式，使用默认值

【JSON 格式示例】
{"replySuggestion":"建议的回复内容","strategyAnalysis":"对方情绪和意图的分析","riskLevel":"SAFE"}

【严格规则】
- riskLevel 的值必须是以下之一（区分大小写）：SAFE、WARNING、DANGER
- 不要返回任何 Markdown 格式（如 ```json、**、等）
- 不要返回任何中文注释或说明
- 不要返回任何换行符或格式化
- 只返回一行有效的 JSON 对象
- 不要在 JSON 前后添加任何文本
- 不要返回多个 JSON 对象
- 必须是有效的 JSON，可以被 JSON 解析器直接解析""".trim()

        val SYSTEM_CHECK = """你是一个社交风控专家。

【强制 JSON 格式要求】
你的回复必须是有效的 JSON 格式，且必须符合以下 JSON Schema：
{
  "type": "object",
  "properties": {
    "isSafe": {"type": "boolean"},
    "triggeredRisks": {"type": "array", "items": {"type": "string"}},
    "suggestion": {"type": "string"}
  },
  "required": ["isSafe", "triggeredRisks", "suggestion"],
  "additionalProperties": false
}

【强制要求】
1. 你必须且只能返回有效的 JSON 对象，不要返回任何其他文本、Markdown、代码块或解释
2. 返回的 JSON 必须包含以下三个英文 Key，且只能是这三个 Key：
   - isSafe (布尔类型，必须，true 或 false)
   - triggeredRisks (数组类型，必须，包含触发的风险规则名称)
   - suggestion (字符串类型，必须，修正建议)
3. 不允许添加任何额外的 Key 或嵌套结构
4. 如果没有触发风险，triggeredRisks 必须是空数组 []
5. 如果无法检查，也必须返回上述格式，使用默认值

【JSON 格式示例】
{"isSafe":true,"triggeredRisks":[],"suggestion":"内容安全"}

【严格规则】
- isSafe 必须是布尔值：true 或 false（不是字符串）
- triggeredRisks 必须是数组，即使为空也要写 []
- 不要返回任何 Markdown 格式（如 ```json、**、等）
- 不要返回任何中文注释或说明
- 不要返回任何换行符或格式化
- 只返回一行有效的 JSON 对象
- 不要在 JSON 前后添加任何文本
- 不要返回多个 JSON 对象
- 必须是有效的 JSON，可以被 JSON 解析器直接解析""".trim()

        val SYSTEM_EXTRACT = """你是一个专业的社交信息分析专家。

【强制 JSON 格式要求】
你的回复必须是有效的 JSON 格式，且必须符合以下 JSON Schema：
{
  "type": "object",
  "properties": {
    "facts": {"type": "object", "additionalProperties": {"type": "string"}},
    "redTags": {"type": "array", "items": {"type": "string"}},
    "greenTags": {"type": "array", "items": {"type": "string"}}
  },
  "required": ["facts", "redTags", "greenTags"],
  "additionalProperties": false
}

【强制要求】
1. 你必须且只能返回有效的 JSON 对象，不要返回任何其他文本、Markdown、代码块或解释
2. 返回的 JSON 必须包含以下三个英文 Key，且只能是这三个 Key：
   - facts (对象类型，必须，包含提取的事实信息)
   - redTags (数组类型，必须，包含雷区标签)
   - greenTags (数组类型，必须，包含策略标签)
3. 不允许添加任何额外的 Key 或嵌套结构
4. facts 对象中的 Key 必须是英文，Value 必须是字符串
5. redTags 和 greenTags 必须是字符串数组，如果没有则为空数组 []
6. 如果无法提取，也必须返回上述格式，使用默认值

【JSON 格式示例】
{"facts":{"birthday":"12.21","hobby":"reading"},"redTags":["不要提前任"],"greenTags":["耐心倾听"]}

【严格规则】
- facts 中的所有 Key 必须是英文（如 birthday、hobby、job 等）
- redTags 和 greenTags 中的值可以是中文
- 不要返回任何 Markdown 格式（如 ```json、**、等）
- 不要返回任何中文注释或说明
- 不要返回任何换行符或格式化
- 只返回一行有效的 JSON 对象
- 不要在 JSON 前后添加任何文本
- 不要返回多个 JSON 对象
- 必须是有效的 JSON，可以被 JSON 解析器直接解析""".trim()
    }

    /**
     * 分析聊天上下文 (重辅助)
     *
     * 调用 AI 模型对聊天内容进行深度分析，给出策略建议。
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

            // 2. 选择模型（优先使用provider的defaultModelId，否则根据名称推断）
            val model = if (provider.defaultModelId.isNotBlank()) {
                provider.defaultModelId
            } else {
                when {
                    provider.name.contains("DeepSeek", ignoreCase = true) -> MODEL_DEEPSEEK
                    provider.name.contains("OpenAI", ignoreCase = true) -> MODEL_OPENAI
                    else -> MODEL_OPENAI
                }
            }

            // 3. 构建消息列表
            val messages = listOf(
                MessageDto(
                    role = "system",
                    content = systemInstruction.ifEmpty { SYSTEM_ANALYZE }
                ),
                MessageDto(
                    role = "user",
                    content = promptContext
                )
            )

            // 4. 构建请求（强制JSON格式）
            // 使用 response_format 字段强制 AI 返回 JSON 格式
            // 这是比提示词更强的约束，确保模型必须返回有效的 JSON
            val request = ChatRequestDto(
                model = model,
                messages = messages,
                temperature = 0.7,
                stream = false,
                responseFormat = com.empathy.ai.data.remote.model.ResponseFormat(type = "json_object")
            )

            // 5. 调用 API（带重试）
            android.util.Log.d("AiRepositoryImpl", "=== API请求详情 ===")
            android.util.Log.d("AiRepositoryImpl", "URL: $url")
            android.util.Log.d("AiRepositoryImpl", "Model: $model")
            android.util.Log.d("AiRepositoryImpl", "Provider: ${provider.name}")
            android.util.Log.d("AiRepositoryImpl", "ResponseFormat: ${request.responseFormat}")
            
            val response = withRetry {
                api.chatCompletion(url, headers, request)
            }

            // 6. 解析响应
            val content = response.choices.firstOrNull()
                ?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))

            // 7. 解析 AI 返回的 JSON 为 AnalysisResult
            parseAnalysisResult(content)

        } catch (e: retrofit2.HttpException) {
            // HTTP错误，尝试读取错误详情
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
        riskRules: List<String>
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

            val messages = listOf(
                MessageDto(role = "system", content = SYSTEM_CHECK),
                MessageDto(role = "user", content = checkPrompt)
            )

            val request = ChatRequestDto(
                model = model,
                messages = messages,
                temperature = 0.3,
                stream = false,
                responseFormat = com.empathy.ai.data.remote.model.ResponseFormat(type = "json_object")
            )

            android.util.Log.d("AiRepositoryImpl", "=== API请求详情 (checkDraftSafety) ===")
            android.util.Log.d("AiRepositoryImpl", "URL: $url")
            android.util.Log.d("AiRepositoryImpl", "Model: $model")
            android.util.Log.d("AiRepositoryImpl", "Provider: ${provider.name}")

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

            val request = ChatRequestDto(
                model = model,
                messages = messages,
                temperature = 0.5,
                stream = false,
                responseFormat = com.empathy.ai.data.remote.model.ResponseFormat(type = "json_object")
            )

            android.util.Log.d("AiRepositoryImpl", "=== API请求详情 (extractTextInfo) ===")
            android.util.Log.d("AiRepositoryImpl", "URL: $url")
            android.util.Log.d("AiRepositoryImpl", "Model: $model")
            android.util.Log.d("AiRepositoryImpl", "Provider: ${provider.name}")

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
     * 解析 AnalysisResult JSON
     *
     * 将 AI 返回的 JSON 字符串解析为 AnalysisResult Domain 模型。
     *
     * @param json AI 返回的 JSON 字符串
     * @return 解析结果
     */
    private fun parseAnalysisResult(json: String): Result<AnalysisResult> {
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
            
            // 2. 检查清洗后的JSON是否有效
            if (cleanedJson == "{}" || cleanedJson.length < 10) {
                android.util.Log.w("AiRepositoryImpl", "JSON清洗后为空，AI可能返回了非JSON格式。原始内容: ${json.take(200)}")
                // 返回Fallback结果
                return Result.success(createFallbackAnalysisResult(json))
            }
            
            // 3. 解析清洗后的JSON
            val adapter = moshi.adapter(AnalysisResult::class.java).lenient()
            val result = adapter.fromJson(cleanedJson)

            if (result != null) {
                Result.success(result)
            } else {
                android.util.Log.w("AiRepositoryImpl", "JSON解析返回null，使用Fallback。Cleaned JSON: $cleanedJson")
                Result.success(createFallbackAnalysisResult(json))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiRepositoryImpl", "Failed to parse AnalysisResult. Original: ${json.take(200)}", e)
            // 返回Fallback结果而不是失败
            Result.success(createFallbackAnalysisResult(json))
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
}
