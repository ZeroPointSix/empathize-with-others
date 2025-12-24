package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.SafetyCheckResult

/**
 * AI 服务仓库接口 - "大脑连接器"
 *
 * 服务对象: AnalyzeChatUseCase, CheckDraftUseCase, ImportMediaUseCase,
 *          PolishDraftUseCase, GenerateReplyUseCase, RefinementUseCase
 * 对接网络请求或耗时的本地 AI 运算
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
interface AiRepository {
    /**
     * 帮我分析 (重辅助)
     *
     * [功能 A] 对聊天上下文进行深度分析，给出策略建议
     *
     * @param provider AI服务商配置（包含baseUrl、apiKey、model等）
     * @param promptContext 构建好的 Prompt (已包含脱敏后的上下文 + 目标 + 画像)
     * @param systemInstruction 系统指令
     * @return 包含建议回复、心理分析、风险等级的结构体
     */
    suspend fun analyzeChat(
        provider: AiProvider,
        promptContext: String,
        systemInstruction: String
    ): Result<AnalysisResult>

    /**
     * 帮我检查 (轻辅助/防踩雷)
     *
     * [功能 B] 实时检查用户正在输入的草稿是否触发雷区
     *
     * @param provider AI服务商配置（包含baseUrl、apiKey、model等）
     * @param draft 用户正在输入的草稿
     * @param riskRules 相关的雷区标签列表(文本形式)
     * @param systemInstruction 可选的自定义系统指令，为null时使用默认指令
     * @return 详细的安全检查结果
     */
    suspend fun checkDraftSafety(
        provider: AiProvider,
        draft: String,
        riskRules: List<String>,
        systemInstruction: String? = null
    ): Result<SafetyCheckResult>

    /**
     * 文本信息萃取
     *
     * [功能 D] 从文本中提取关键信息，包括事实、雷区和策略
     *
     * @param provider AI服务商配置（包含baseUrl、apiKey、model等）
     * @param inputText 输入的文本内容
     * @return 提取的结构化信息
     */
    suspend fun extractTextInfo(
        provider: AiProvider,
        inputText: String
    ): Result<ExtractedData>

    /**
     * 多模态转文字 (STT / OCR)
     *
     * [功能 C] 将音频/视频文件转录为文字
     *
     * @param mediaFilePath 媒体文件路径
     * @return 转录后的纯文本
     */
    suspend fun transcribeMedia(mediaFilePath: String): Result<String>

    /**
     * 通用文本生成
     *
     * [功能 E] 通用的AI文本生成接口，用于每日总结等场景
     *
     * @param provider AI服务商配置
     * @param prompt 用户提示词
     * @param systemInstruction 系统指令
     * @return 生成的文本
     */
    suspend fun generateText(
        provider: AiProvider,
        prompt: String,
        systemInstruction: String
    ): Result<String>

    // ==================== 新增方法（TD-00009） ====================

    /**
     * 帮我润色
     *
     * [功能 F] 优化用户草稿，使表达更得体
     *
     * @param provider AI服务商配置
     * @param draft 用户草稿
     * @param systemInstruction 系统指令
     * @return 润色结果（包含优化后的文本和可选的风险提示）
     */
    suspend fun polishDraft(
        provider: AiProvider,
        draft: String,
        systemInstruction: String
    ): Result<PolishResult>

    /**
     * 帮我回复
     *
     * [功能 G] 根据对方消息生成合适的回复
     *
     * @param provider AI服务商配置
     * @param message 对方的消息
     * @param systemInstruction 系统指令
     * @return 回复结果（包含建议的回复和策略说明）
     */
    suspend fun generateReply(
        provider: AiProvider,
        message: String,
        systemInstruction: String
    ): Result<ReplyResult>

    /**
     * 微调分析结果
     *
     * [功能 H] 根据用户反馈重新生成分析结果
     *
     * @param provider AI服务商配置
     * @param refinementPrompt 微调提示词（包含原始输入、上次结果、用户意见）
     * @return 新的分析结果
     */
    suspend fun refineAnalysis(
        provider: AiProvider,
        refinementPrompt: String
    ): Result<AnalysisResult>

    /**
     * 微调润色结果
     *
     * [功能 I] 根据用户反馈重新生成润色结果
     *
     * @param provider AI服务商配置
     * @param refinementPrompt 微调提示词（包含原始输入、上次结果、用户意见）
     * @return 新的润色结果
     */
    suspend fun refinePolish(
        provider: AiProvider,
        refinementPrompt: String
    ): Result<PolishResult>

    /**
     * 微调回复结果
     *
     * [功能 J] 根据用户反馈重新生成回复结果
     *
     * @param provider AI服务商配置
     * @param refinementPrompt 微调提示词（包含原始输入、上次结果、用户意见）
     * @return 新的回复结果
     */
    suspend fun refineReply(
        provider: AiProvider,
        refinementPrompt: String
    ): Result<ReplyResult>
}
