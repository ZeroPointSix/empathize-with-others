package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.AiStreamChunk
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.model.KnowledgeQueryResponse
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.SafetyCheckResult
import kotlinx.coroutines.flow.Flow

/**
 * AI服务仓储接口 - "大脑连接器"
 *
 * 业务背景 (PRD-00009):
 * - 封装所有AI能力调用，作为应用与LLM服务之间的桥梁
 * - 支持多种AI功能：分析、检查、提取、转录、润色、回复
 * - 采用OpenAI兼容API标准，支持多服务商切换
 *
 * 设计决策 (TDD-00009):
 * - 统一的Provider参数：每个方法接收AiProvider，支持运行时切换服务商
 * - 系统指令分离：systemInstruction可自定义，null时使用默认指令
 * - 返回Result类型：统一成功/失败处理，支持链式调用和错误传播
 *
 * 服务对象:
 * - AnalyzeChatUseCase (帮我分析)
 * - CheckDraftUseCase (帮我检查)
 * - PolishDraftUseCase (帮我润色)
 * - GenerateReplyUseCase (帮我回复)
 * - RefinementUseCase (微调优化)
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
interface AiRepository {
    /**
     * 帮我分析 - 重辅助 (功能A)
     *
     * 业务规则 (PRD-00009):
     * - 对聊天上下文进行深度分析，给出策略建议
     * - 返回包含风险等级、分析结论、建议回复的完整结构
     * - 是最核心的AI功能，输出质量直接影响用户体验
     *
     * 设计权衡:
     * - promptContext由上层UseCase构建，包含脱敏后的完整上下文
     * - systemInstruction可自定义，null时使用默认的系统提示词模板
     *
     * @param provider AI服务商配置（包含baseUrl、apiKey、model等）
     * @param promptContext 构建好的Prompt (已包含脱敏后的上下文 + 目标 + 画像)
     * @param systemInstruction 系统指令
     * @return 包含建议回复、心理分析、风险等级的结构体
     */
    suspend fun analyzeChat(
        provider: AiProvider,
        promptContext: String,
        systemInstruction: String
    ): Result<AnalysisResult>

    /**
     * 帮我检查 - 轻辅助 (功能B)
     *
     * 业务规则 (PRD-00009):
     * - 实时检查用户正在输入的草稿是否触发雷区
     * - 快速响应，优先使用较低的temperature以保证稳定性
     * - 返回详细的触发风险列表和修改建议
     *
     * 设计权衡:
     * - riskRules作为纯文本传入，与AI分析时的标签列表一致
     * - 支持自定义systemInstruction覆盖默认检查规则
     *
     * @param provider AI服务商配置
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
     * 文本信息萃取 - 喂养功能 (功能D)
     *
     * 业务规则 (PRD-00009):
     * - 从文本中提取关键信息，包括事实、雷区和策略
     * - 用于"喂养"功能，让AI从对话中学习联系人特点
     * - 提取的Fact可直接存入联系人画像
     *
     * 设计权衡:
     * - 输入仅为原始文本，输出为结构化的ExtractedData
     * - 不依赖联系人画像context，专注于当前文本的语义理解
     *
     * @param provider AI服务商配置
     * @param inputText 输入的文本内容
     * @return 提取的结构化信息
     */
    suspend fun extractTextInfo(
        provider: AiProvider,
        inputText: String
    ): Result<ExtractedData>

    /**
     * 多模态转文字 - 导入功能 (功能C)
     *
     * 业务规则 (PRD-00009):
     * - 将音频/视频文件转录为文字
     * - 支持导入语音消息、视频等多媒体内容进行分析
     * - 调用外部STT/OCR服务完成转录
     *
     * 设计权衡:
     * - 独立的媒体处理，不依赖AI Provider配置
     * - 返回纯文本供后续AI分析使用
     *
     * @param mediaFilePath 媒体文件路径
     * @return 转录后的纯文本
     */
    suspend fun transcribeMedia(mediaFilePath: String): Result<String>

    /**
     * 通用文本生成 (功能E)
     *
     * 业务规则:
     * - 通用的AI文本生成接口，用于每日总结等场景
     * - 不返回结构化结果，直接返回原始文本
     * - 由调用方负责解析和转换结果
     *
     * 用途:
     * - 每日总结生成
     * - 自定义提示词场景
     * - 任何需要纯文本输出的场景
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

    // ==================== TD-00009 新增方法 ====================

    /**
     * 帮我润色 (功能F)
     *
     * 业务规则:
     * - 优化用户草稿，使表达更得体
     * - 在保持原意的同时提升表达质量
     * - 可选择性地返回潜在风险提示
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
     * 帮我回复 (功能G)
     *
     * 业务规则:
     * - 根据对方消息生成合适的回复建议
     * - 考虑当前关系分数和聊天上下文
     * - 返回多个候选回复供用户选择
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

    // ==================== 微调方法 ====================

    /**
     * 微调分析结果 (功能H)
     *
     * 业务规则:
     * - 根据用户反馈重新生成分析结果
     * - 将原始输入、上次结果、用户意见组合成微调提示词
     * - 保持上下文一致性的同时优化输出
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
     * 微调润色结果 (功能I)
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
     * 微调回复结果 (功能J)
     *
     * @param provider AI服务商配置
     * @param refinementPrompt 微调提示词（包含原始输入、上次结果、用户意见）
     * @return 新的回复结果
     */
    suspend fun refineReply(
        provider: AiProvider,
        refinementPrompt: String
    ): Result<ReplyResult>

    // ==================== FD-00028: 流式响应接口 ====================

    /**
     * 流式文本生成 (功能K)
     *
     * 业务规则 (FD-00028):
     * - 使用Server-Sent Events (SSE) 实现实时文本流
     * - 支持DeepSeek R1等模型的思考过程展示
     * - 返回Flow<AiStreamChunk>，支持多种事件类型
     *
     * 设计决策:
     * - 使用Flow而非suspend函数，支持流式数据发射
     * - AiStreamChunk密封类定义统一的事件协议
     * - 调用方负责处理各种Chunk类型并更新UI
     *
     * 用途:
     * - AI军师流式对话
     * - 实时打字机效果
     * - 思考过程可视化
     *
     * @param provider AI服务商配置
     * @param prompt 用户提示词
     * @param systemInstruction 系统指令
     * @return 流式响应Flow
     *
     * @see AiStreamChunk 流式数据块类型定义
     * @see FD-00028 AI军师流式对话升级功能设计
     */
    fun generateTextStream(
        provider: AiProvider,
        prompt: String,
        systemInstruction: String
    ): Flow<AiStreamChunk>

    // ==================== PRD-00031: 知识查询接口 ====================

    /**
     * 知识查询 (功能L)
     *
     * 业务规则 (PRD-00031):
     * - 悬浮窗新增第4个Tab"快速问答"
     * - 支持联网优先、AI本地知识兜底的知识获取策略
     * - 返回Markdown格式的知识解释和相关推荐
     *
     * 设计决策:
     * - 统一的查询接口，内部根据模型能力选择联网或本地策略
     * - 返回KnowledgeQueryResponse，包含内容、来源和推荐
     * - 超时时间：联网3秒，本地2秒
     *
     * @param provider AI服务商配置
     * @param content 查询内容（已清理和验证）
     * @param systemInstruction 系统指令
     * @return 知识查询响应
     *
     * @see KnowledgeQueryResponse 知识查询响应模型
     * @see PRD-00031 悬浮窗快速知识回答功能需求
     */
    suspend fun queryKnowledge(
        provider: AiProvider,
        content: String,
        systemInstruction: String
    ): Result<KnowledgeQueryResponse>
}
