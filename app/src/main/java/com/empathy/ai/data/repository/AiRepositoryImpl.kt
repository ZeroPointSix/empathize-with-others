package com.empathy.ai.data.repository

import com.empathy.ai.data.remote.api.OpenAiApi
import com.empathy.ai.data.remote.model.ChatRequestDto
import com.empathy.ai.data.remote.model.MessageDto
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.SettingsRepository
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

    private val moshi = Moshi.Builder().build()

    /**
     * 服务商配置
     */
    companion object {
        // 模型名称
        const val MODEL_OPENAI = "gpt-3.5-turbo"
        const val MODEL_DEEPSEEK = "deepseek-chat"

        // 系统指令模板
        val SYSTEM_ANALYZE = """你是一个专业的社交沟通顾问。请分析对话内容，给出：
1. 对方的状态分析(情绪、潜在意图)
2. 关键洞察/陷阱
3. 建议行动策略

请严格用 JSON 格式回复：
{"replySuggestion": "建议的回复", "strategyAnalysis": "心理分析", "riskLevel": "SAFE|WARNING|DANGER"}""".trim()

        val SYSTEM_CHECK = """你是一个社交风控专家。请检查用户的草稿是否触发了风险规则。
请严格用 JSON 格式回复：
{"isSafe": true/false, "triggeredRisks": ["雷区1"], "suggestion": "修正建议"}""".trim()
    }

    /**
     * 分析聊天上下文 (重辅助)
     *
     * 调用 AI 模型对聊天内容进行深度分析，给出策略建议。
     *
     * @param promptContext 构建好的 Prompt (已包含脱敏后的上下文、目标、画像)
     * @param systemInstruction 系统指令 (可选，留空则使用默认指令)
     * @return 包含建议回复、心理分析、风险等级的分析结果
     */
    override suspend fun analyzeChat(
        promptContext: String,
        systemInstruction: String
    ): Result<AnalysisResult> {
        return try {
            // 1. 获取配置（URL 和 Headers）
            val urlResult = settingsRepository.getBaseUrl()
            val headersResult = settingsRepository.getProviderHeaders()

            if (urlResult.isFailure) {
                return Result.failure(urlResult.exceptionOrNull()!!)
            }
            if (headersResult.isFailure) {
                return Result.failure(headersResult.exceptionOrNull()!!)
            }

            val url = urlResult.getOrThrow()
            val headers = headersResult.getOrThrow()

            // 2. 获取服务商（用于选择模型）
            val providerResult = settingsRepository.getAiProvider()
            val provider = providerResult.getOrDefault("OpenAI")

            val model = when (provider) {
                "OpenAI" -> MODEL_OPENAI
                "DeepSeek" -> MODEL_DEEPSEEK
                else -> MODEL_OPENAI
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

            // 4. 构建请求
            val request = ChatRequestDto(
                model = model,
                messages = messages,
                temperature = 0.7,
                stream = false
            )

            // 5. 调用 API
            val response = api.chatCompletion(url, headers, request)

            // 6. 解析响应
            val content = response.choices.firstOrNull()
                ?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))

            // 7. 解析 AI 返回的 JSON 为 AnalysisResult
            parseAnalysisResult(content)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查草稿安全性 (轻辅助/防踩雷)
     *
     * 实时检查用户输入是否触发了雷区规则。
     *
     * @param draft 用户正在输入的草稿
     * @param riskRules 相关的雷区标签列表
     * @return 安全检查结果的包装类
     */
    override suspend fun checkDraftSafety(
        draft: String,
        riskRules: List<String>
    ): Result<SafetyCheckResult> {
        return try {
            // 1. 获取配置
            val urlResult = settingsRepository.getBaseUrl()
            val headersResult = settingsRepository.getProviderHeaders()

            if (urlResult.isFailure) {
                return Result.failure(urlResult.exceptionOrNull()!!)
            }
            if (headersResult.isFailure) {
                return Result.failure(headersResult.exceptionOrNull()!!)
            }

            val url = urlResult.getOrThrow()
            val headers = headersResult.getOrThrow()

            // 2. 获取模型
            val providerResult = settingsRepository.getAiProvider()
            val provider = providerResult.getOrDefault("OpenAI")

            val model = when (provider) {
                "OpenAI" -> MODEL_OPENAI
                "DeepSeek" -> MODEL_DEEPSEEK
                else -> MODEL_OPENAI
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
                stream = false
            )

            val response = api.chatCompletion(url, headers, request)

            val content = response.choices.firstOrNull()
                ?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))

            // 解析 AI 返回的 JSON 为 SafetyCheckResult
            parseSafetyCheckResult(content)

        } catch (e: Exception) {
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
     * 解析 AnalysisResult JSON
     *
     * 将 AI 返回的 JSON 字符串解析为 AnalysisResult Domain 模型。
     *
     * @param json AI 返回的 JSON 字符串
     * @return 解析结果
     */
    private fun parseAnalysisResult(json: String): Result<AnalysisResult> {
        return try {
            val adapter = moshi.adapter(AnalysisResult::class.java)
            val result = adapter.fromJson(json)

            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to parse AI response as AnalysisResult"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            val adapter = moshi.adapter(SafetyCheckResult::class.java)
            val result = adapter.fromJson(json)

            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to parse AI response as SafetyCheckResult"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
