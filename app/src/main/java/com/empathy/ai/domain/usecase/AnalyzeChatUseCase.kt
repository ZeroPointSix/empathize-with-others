package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.service.PrivacyEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 核心业务流一: 主动分析聊天内容
 *
 * 触发场景: 用户点击悬浮窗的 [💡 帮我分析] 按钮
 *
 * 功能: 对聊天上下文进行深度分析，给出策略建议
 */
class AnalyzeChatUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val brainTagRepository: BrainTagRepository,
    private val privacyRepository: PrivacyRepository,
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository
) {
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
        return try {
            // 1. 前置检查: 确保已配置 API Key
            val apiKey = settingsRepository.getApiKey().getOrNull()
            if (apiKey.isNullOrBlank()) {
                return Result.failure(IllegalStateException("未配置 API Key，请先在设置中配置"))
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
            val maskedContext = PrivacyEngine.maskBatch(cleanedContext, privacyMapping)

            // 5. Prompt 组装
            val prompt = buildPrompt(
                targetGoal = profile.targetGoal,
                facts = profile.facts,
                redTags = brainTags.filter { it.type == TagType.RISK_RED },
                greenTags = brainTags.filter { it.type == TagType.STRATEGY_GREEN },
                conversationHistory = maskedContext
            )

            val systemInstruction = buildSystemInstruction()

            // 6. AI 推理
            val analysisResult = aiRepository.analyzeChat(prompt, systemInstruction).getOrThrow()

            Result.success(analysisResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 构建 Prompt
     */
    private fun buildPrompt(
        targetGoal: String,
        facts: Map<String, String>,
        redTags: List<com.empathy.ai.domain.model.BrainTag>,
        greenTags: List<com.empathy.ai.domain.model.BrainTag>,
        conversationHistory: List<String>
    ): String {
        return buildString {
            appendLine("【攻略目标】")
            appendLine(targetGoal)
            appendLine()

            if (facts.isNotEmpty()) {
                appendLine("【已知信息】")
                facts.forEach { (key, value) ->
                    appendLine("- $key: $value")
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

    /**
     * 构建系统指令
     */
    private fun buildSystemInstruction(): String {
        return """
            你是一个专业的社交沟通顾问。

            请基于提供的信息，分析当前聊天情况，并给出:
            1. 对方当前的情绪和潜在意图
            2. 可能存在的风险点
            3. 具体的回复建议（可直接发送的文本）

            注意事项:
            - 严格遵守雷区警告，不要触碰敏感话题
            - 优先使用策略建议中的方法
            - 回复要真诚、自然，不要太过刻意
            - 如果发现高风险情况，请明确标注
        """.trimIndent()
    }
}
