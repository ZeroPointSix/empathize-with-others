package com.empathy.ai.domain.usecase

import android.util.Log
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ConversationContextConfig
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.MessageSender
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.TimestampedMessage
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.repository.TopicRepository
import com.empathy.ai.domain.service.PrivacyEngine
import com.empathy.ai.domain.util.ConversationContextBuilder
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.UserProfileContextBuilder
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
    private val promptBuilder: PromptBuilder,
    private val conversationContextBuilder: ConversationContextBuilder,
    private val userProfileContextBuilder: UserProfileContextBuilder,
    private val topicRepository: TopicRepository
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

            // 5. 【对话上下文连续性】先查询历史（必须在保存当前输入之前）
            // 【重要】顺序不能颠倒！否则会把当前输入也当作"历史"查出来
            val historyCount = settingsRepository.getHistoryConversationCount()
                .getOrDefault(ConversationContextConfig.DEFAULT_HISTORY_COUNT)
            
            Log.d(TAG, "历史配置: historyCount=$historyCount, contactId=$contactId")
            
            val historyContext = if (historyCount > 0) {
                buildHistoryContext(contactId, historyCount)
            } else {
                ""
            }
            
            Log.d(TAG, "历史上下文长度: ${historyContext.length}, 内容预览: ${historyContext.take(200)}")

            // 6. 【记忆系统】保存用户输入到对话记录（在查询历史之后）
            // 【PRD-00008】添加身份前缀，标识这是"对方说的"内容
            val userInputText = cleanedContext.joinToString("\n")
            val prefixedInput = IdentityPrefixHelper.addPrefix(
                content = userInputText,
                actionType = ActionType.ANALYZE
            )
            conversationLogId = saveUserInput(contactId, prefixedInput)
            Log.d(TAG, "保存用户输入(带身份前缀): contactId=$contactId, logId=$conversationLogId")

            // 7. Prompt 组装（使用PromptBuilder三层分离架构）
            val redTags = brainTags.filter { it.type == TagType.RISK_RED }
            val greenTags = brainTags.filter { it.type == TagType.STRATEGY_GREEN }
            
            // 【PRD-00008】为发送给AI的聊天记录添加身份前缀
            val prefixedContext = maskedContext.map { message ->
                IdentityPrefixHelper.addPrefix(message, ActionType.ANALYZE)
            }
            
            // 【PRD-00013】获取用户画像上下文（智能筛选相关信息）
            val userProfileContext = try {
                val userInputForFilter = cleanedContext.joinToString("\n")
                userProfileContextBuilder.buildAnalysisContext(profile, userInputForFilter)
                    .getOrNull() ?: ""
            } catch (e: Exception) {
                Log.w(TAG, "获取用户画像上下文失败，降级为空上下文", e)
                ""  // 降级：用户画像获取失败不影响主流程
            }
            
            Log.d(TAG, "用户画像上下文长度: ${userProfileContext.length}")

            // 【TD-00016】获取当前对话主题
            val activeTopic = try {
                topicRepository.getActiveTopic(contactId)
            } catch (e: Exception) {
                Log.w(TAG, "获取对话主题失败，降级为无主题", e)
                null  // 降级：主题获取失败不影响主流程
            }
            
            Log.d(TAG, "当前对话主题: ${activeTopic?.content?.take(50) ?: "无"}")
            
            // 构建运行时数据（系统自动注入，用户不可见）
            val runtimeData = buildContextData(
                targetGoal = profile.targetGoal,
                facts = profile.facts,
                redTags = redTags,
                greenTags = greenTags,
                conversationHistory = prefixedContext,  // 使用带前缀的上下文
                historyContext = historyContext,
                userProfileContext = userProfileContext  // 【新增】用户画像上下文
            )
            
            // 使用PromptBuilder构建完整系统指令
            // 三层分离：系统约束 + 用户指令 + 运行时数据
            // 【TD-00016】使用buildWithTopic方法注入对话主题
            val promptContext = PromptContext.fromContact(profile)
            val systemInstruction = promptBuilder.buildWithTopic(
                scene = PromptScene.ANALYZE,
                contactId = contactId,
                context = promptContext,
                topic = activeTopic,
                runtimeData = runtimeData  // 运行时数据直接传入，不再使用占位符
            )

            // 8. AI 推理（传递provider配置）
            // 注意：promptContext传递运行时数据，systemInstruction传递完整指令
            val analysisResult = aiRepository.analyzeChat(
                provider = defaultProvider,
                promptContext = runtimeData,
                systemInstruction = systemInstruction
            ).getOrThrow()

            // 9. 【记忆系统】保存AI回复到对话记录
            conversationLogId?.let { logId ->
                saveAiResponse(logId, analysisResult)
            }

            // 10. 【记忆系统】更新最后互动日期
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
     * 【新增】构建历史上下文
     *
     * 【重要说明】当前版本仅回放用户侧历史
     * 原因：数据库 conversation_logs 表只存储了 user_input
     *
     * @param contactId 联系人ID
     * @param limit 历史条数
     * @return 带时间流逝标记的历史上下文字符串
     */
    private suspend fun buildHistoryContext(contactId: String, limit: Int): String {
        return try {
            val recentLogs = conversationRepository
                .getRecentConversations(contactId, limit)
                .getOrDefault(emptyList())

            if (recentLogs.isEmpty()) return ""

            // 将ConversationLog转换为TimestampedMessage
            // 【注意】当前版本 sender 固定为 USER
            val messages = recentLogs.mapNotNull { log ->
                try {
                    TimestampedMessage(
                        content = log.userInput,
                        timestamp = log.timestamp,
                        sender = MessageSender.ME
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "跳过无效的对话记录: ${log.id}", e)
                    null
                }
            }

            conversationContextBuilder.buildHistoryContext(messages)
        } catch (e: Exception) {
            Log.e(TAG, "构建历史上下文失败，降级为空历史", e)
            ""  // 降级：返回空历史，不影响主流程
        }
    }

    /**
     * 构建上下文数据
     *
     * 将联系人信息、标签、用户画像和聊天记录组装为上下文数据字符串
     *
     * @param targetGoal 攻略目标
     * @param facts 已知事实
     * @param redTags 雷区标签
     * @param greenTags 策略标签
     * @param conversationHistory 聊天记录
     * @param historyContext 历史对话上下文（带时间流逝标记）
     * @param userProfileContext 用户画像上下文（可选）
     */
    private fun buildContextData(
        targetGoal: String,
        facts: List<Fact>,
        redTags: List<BrainTag>,
        greenTags: List<BrainTag>,
        conversationHistory: List<String>,
        historyContext: String = "",
        userProfileContext: String = ""
    ): String {
        return buildString {
            // 【新增】用户画像区块（放在最前面，让AI先了解用户特点）
            if (userProfileContext.isNotBlank()) {
                appendLine(userProfileContext)
                appendLine()
            }

            // 【新增】历史对话区块（放在最前面，让AI先了解背景）
            if (historyContext.isNotBlank()) {
                appendLine(historyContext)
                appendLine()
            }

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
