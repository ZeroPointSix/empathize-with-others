package com.empathy.ai.domain.usecase

import android.util.Log
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.service.PrivacyEngine
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.PromptBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 核心业务流一: 主动分析聊天内容
 *
 * 触发场景: 用户点击悬浮窗的 [💡 帮我分析] 按钮
 *
 * 功能: 对聊天上下文进行深度分析，给出策略建议
 *
 * 记忆系统集成:
 * - 自动保存用户输入到对话记录
 * - 自动保存AI回复到对话记录
 * - 更新联系人最后互动日期
 *
 * 提示词系统集成:
 * - 使用PromptBuilder构建系统指令
 * - 支持用户自定义提示词和联系人专属提示词
 */
class AnalyzeChatUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val brainTagRepository: BrainTagRepository,
    private val privacyRepository: PrivacyRepository,
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository,
    private val aiProviderRepository: com.empathy.ai.domain.repository.AiProviderRepository,
    private val conversationRepository: ConversationRepository,
    private val promptBuilder: PromptBuilder
) {
    companion object {
        private const val TAG = "AnalyzeChatUseCase"
    }
    /**
     * 执行聊天分析
     *
     * @param contactId 当前正在和谁聊天
     * @param rawScreenContext 从屏幕抓取到的原始文本列表
     * @return 分析结果
     */
    suspend operator fun invoke(
        contactId: String,
        rawScreenContext: List<String>
    ): Result<AnalysisResult> {
        // 用于记录对话的ID，即使AI分析失败也要保存用户输入
        var conversationLogId: Long? = null

        return try {
            // 1. 前置检查: 确保已配置默认 AI 服务商
            val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
            if (defaultProvider == null) {
                return Result.failure(IllegalStateException("未配置默认 AI 服务商，请先在设置中配置"))
            }
            if (defaultProvider.apiKey.isBlank()) {
                return Result.failure(IllegalStateException("默认服务商的 API Key 为空，请检查配置"))
            }

            // 2. 并行加载数据
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return Result.failure(IllegalStateException("未找到联系人画像: $contactId"))

            val brainTags = brainTagRepository.getTagsForContact(contactId).first()
            val privacyMapping = privacyRepository.getPrivacyMapping().getOrElse { emptyMap() }

            // 3. 数据清洗: 去重与排序
            val cleanedContext = rawScreenContext
                .distinct() // 去重
                .takeLast(profile.contextDepth) // 保留最近 N 条

            // 4. 安全脱敏
            // 读取数据掩码设置
            val dataMaskingEnabled = settingsRepository.getDataMaskingEnabled()
                .getOrDefault(true)
            
            val maskedContext = if (dataMaskingEnabled) {
                // 启用数据掩码，进行脱敏处理
                PrivacyEngine.maskBatch(cleanedContext, privacyMapping)
            } else {
                // 未启用数据掩码，直接使用原始数据
                cleanedContext
            }

            // 5. 【记忆系统】保存用户输入到对话记录
            val userInputText = cleanedContext.joinToString("\n")
            conversationLogId = saveUserInput(contactId, userInputText)

            // 6. Prompt 组装（使用PromptBuilder）
            val redTags = brainTags.filter { it.type == TagType.RISK_RED }
            val greenTags = brainTags.filter { it.type == TagType.STRATEGY_GREEN }
            
            // 构建上下文数据
            val contextData = buildContextData(
                targetGoal = profile.targetGoal,
                facts = profile.facts,
                redTags = redTags,
                greenTags = greenTags,
                conversationHistory = maskedContext
            )
            
            // 使用PromptBuilder构建系统指令
            val promptContext = PromptContext.fromContact(profile)
            val systemInstructionTemplate = promptBuilder.buildSystemInstruction(
                scene = PromptScene.ANALYZE,
                contactId = contactId,
                context = promptContext
            )
            
            // 注入上下文数据
            val systemInstruction = promptBuilder.injectContextData(
                instruction = systemInstructionTemplate,
                contextData = contextData
            )

            // 7. AI 推理（传递provider配置）
            val analysisResult = aiRepository.analyzeChat(
                provider = defaultProvider,
                promptContext = contextData,
                systemInstruction = systemInstruction
            ).getOrThrow()

            // 8. 【记忆系统】保存AI回复到对话记录
            conversationLogId?.let { logId ->
                saveAiResponse(logId, analysisResult)
            }

            // 9. 【记忆系统】更新最后互动日期
            updateLastInteractionDate(contactId)

            Result.success(analysisResult)
        } catch (e: Exception) {
            // 即使AI分析失败，用户输入已经保存（如果conversationLogId不为null）
            Log.e(TAG, "分析失败，但用户输入已保存: logId=$conversationLogId", e)
            Result.failure(e)
        }
    }

    /**
     * 保存用户输入到对话记录
     *
     * @param contactId 联系人ID
     * @param userInput 用户输入文本
     * @return 对话记录ID，保存失败返回null
     */
    private suspend fun saveUserInput(contactId: String, userInput: String): Long? {
        return try {
            conversationRepository.saveUserInput(contactId, userInput).getOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "保存用户输入失败", e)
            null
        }
    }

    /**
     * 保存AI回复到对话记录
     *
     * @param logId 对话记录ID
     * @param analysisResult AI分析结果
     */
    private suspend fun saveAiResponse(logId: Long, analysisResult: AnalysisResult) {
        try {
            val aiResponseText = buildAiResponseText(analysisResult)
            conversationRepository.updateAiResponse(logId, aiResponseText)
        } catch (e: Exception) {
            Log.e(TAG, "保存AI回复失败", e)
            // 不抛出异常，保存失败不影响主流程
        }
    }

    /**
     * 构建AI回复文本
     */
    private fun buildAiResponseText(result: AnalysisResult): String {
        return buildString {
            appendLine("【分析结果】")
            appendLine("风险等级: ${result.riskLevel}")
            appendLine()
            appendLine("【军师分析】")
            appendLine(result.strategyAnalysis)
            appendLine()
            appendLine("【话术建议】")
            appendLine(result.replySuggestion)
        }
    }

    /**
     * 更新联系人最后互动日期
     *
     * @param contactId 联系人ID
     */
    private suspend fun updateLastInteractionDate(contactId: String) {
        try {
            val today = DateUtils.getCurrentDateString()
            contactRepository.updateLastInteractionDate(contactId, today)
        } catch (e: Exception) {
            Log.e(TAG, "更新最后互动日期失败", e)
            // 不抛出异常，更新失败不影响主流程
        }
    }

    /**
     * 构建上下文数据
     *
     * 将联系人信息、标签和聊天记录组装为上下文数据字符串
     */
    private fun buildContextData(
        targetGoal: String,
        facts: List<Fact>,
        redTags: List<BrainTag>,
        greenTags: List<BrainTag>,
        conversationHistory: List<String>
    ): String {
        return buildString {
            appendLine("【攻略目标】")
            appendLine(targetGoal)
            appendLine()

            if (facts.isNotEmpty()) {
                appendLine("【已知信息】")
                facts.forEach { fact ->
                    appendLine("- ${fact.key}: ${fact.value}")
                }
                appendLine()
            }

            if (redTags.isNotEmpty()) {
                appendLine("【雷区警告】")
                redTags.forEach { tag ->
                    appendLine("- ${tag.content}")
                }
                appendLine()
            }

            if (greenTags.isNotEmpty()) {
                appendLine("【策略建议】")
                greenTags.forEach { tag ->
                    appendLine("- ${tag.content}")
                }
                appendLine()
            }

            appendLine("【聊天记录】")
            conversationHistory.forEach { message ->
                appendLine(message)
            }
        }
    }
}
