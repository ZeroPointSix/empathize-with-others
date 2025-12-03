package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.SafetyCheckResult

/**
 * AI 服务仓库接口 - "大脑连接器"
 *
 * 服务对象: AnalyzeChatUseCase, CheckDraftUseCase, ImportMediaUseCase
 * 对接网络请求或耗时的本地 AI 运算
 */
interface AiRepository {
    /**
     * 帮我分析 (重辅助)
     *
     * [功能 A] 对聊天上下文进行深度分析，给出策略建议
     *
     * @param promptContext 构建好的 Prompt (已包含脱敏后的上下文 + 目标 + 画像)
     * @param systemInstruction 系统指令
     * @return 包含建议回复、心理分析、风险等级的结构体
     */
    suspend fun analyzeChat(
        promptContext: String,
        systemInstruction: String
    ): Result<AnalysisResult>

    /**
     * 帮我检查 (轻辅助/防踩雷)
     *
     * [功能 B] 实时检查用户正在输入的草稿是否触发雷区
     *
     * @param draft 用户正在输入的草稿
     * @param riskRules 相关的雷区标签列表(文本形式)
     * @return 详细的安全检查结果
     */
    suspend fun checkDraftSafety(
        draft: String,
        riskRules: List<String>
    ): Result<SafetyCheckResult>

    /**
     * 多模态转文字 (STT / OCR)
     *
     * [功能 C] 将音频/视频文件转录为文字
     *
     * @param mediaFilePath 媒体文件路径
     * @return 转录后的纯文本
     */
    suspend fun transcribeMedia(mediaFilePath: String): Result<String>
}
