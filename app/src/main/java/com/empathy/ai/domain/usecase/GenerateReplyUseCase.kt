package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.SessionContextService
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.domain.util.PromptBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 生成回复用例
 *
 * 根据对方的话生成合适的回复建议。
 *
 * 流程：
 * 1. 前置检查（AI服务商配置）
 * 2. 加载联系人数据和标签
 * 3. 数据脱敏
 * 4. 添加身份前缀
 * 5. 获取历史对话上下文（BUG-00015修复）
 * 6. 保存对话记录
 * 7. 构建提示词
 * 8. 调用AI
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 * @see BUG-00015 三种模式上下文不共通问题分析
 */
class GenerateReplyUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val brainTagRepository: BrainTagRepository,
    private val privacyRepository: PrivacyRepository,
    private val aiRepository: AiRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val promptBuilder: PromptBuilder,
    private val sessionContextService: SessionContextService
) {
    /**
     * 生成回复建议
     *
     * @param contactId 联系人ID
     * @param theirMessage 对方说的话
     * @return 回复结果
     */
    suspend operator fun invoke(
        contactId: String,
        theirMessage: String
    ): Result<ReplyResult> {
        return try {
            // 1. 前置检查
            val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
                ?: return Result.failure(FloatingWindowError.NoProviderConfigured)

            // 2. 加载联系人数据
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return Result.failure(FloatingWindowError.ContactNotFound(contactId))

            val brainTags = brainTagRepository.getTagsForContact(contactId).first()
            val redTags = brainTags.filter { it.type == TagType.RISK_RED }
            val greenTags = brainTags.filter { it.type == TagType.STRATEGY_GREEN }

            // 3. 数据脱敏（通过Repository接口，符合Clean Architecture）
            val maskedMessage = privacyRepository.maskText(theirMessage)

            // 4. 添加身份前缀
            val prefixedMessage = IdentityPrefixHelper.addPrefix(
                content = maskedMessage,
                actionType = ActionType.REPLY
            )

            // 5. 【BUG-00015修复】获取历史对话上下文
            val historyContext = sessionContextService.getHistoryContext(contactId)

            // 6. 构建提示词
            // 【BUG-00023修复】移除自动保存逻辑，改为用户点击复制按钮时保存
            val promptContext = PromptContext.fromContact(profile)
            val runtimeData = buildRuntimeData(prefixedMessage, redTags, greenTags, profile, historyContext)
            val systemInstruction = promptBuilder.buildSystemInstruction(
                scene = PromptScene.REPLY,
                contactId = contactId,
                context = promptContext,
                runtimeData = runtimeData
            )

            // 7. 调用AI
            aiRepository.generateReply(
                provider = defaultProvider,
                message = prefixedMessage,
                systemInstruction = systemInstruction
            )
        } catch (e: Exception) {
            android.util.Log.e("GenerateReplyUseCase", "生成回复失败", e)
            Result.failure(FloatingWindowError.fromThrowable(e))
        }
    }

    /**
     * 构建运行时数据
     *
     * @param message 对方消息（已添加身份前缀）
     * @param redTags 雷区标签列表
     * @param greenTags 策略标签列表
     * @param profile 联系人画像
     * @param historyContext 历史对话上下文（BUG-00015修复）
     */
    private fun buildRuntimeData(
        message: String,
        redTags: List<BrainTag>,
        greenTags: List<BrainTag>,
        profile: ContactProfile,
        historyContext: String
    ): String {
        return buildString {
            // 【BUG-00015修复】历史对话放在最前面，让AI先了解背景
            if (historyContext.isNotBlank()) {
                appendLine(historyContext)
                appendLine()
            }
            appendLine("【攻略目标】")
            appendLine(profile.targetGoal ?: "维护良好关系")
            appendLine()
            appendLine("【对方消息】")
            appendLine(message)
            if (redTags.isNotEmpty()) {
                appendLine()
                appendLine("【雷区警告】")
                redTags.forEach { appendLine("- ${it.content}") }
            }
            if (greenTags.isNotEmpty()) {
                appendLine()
                appendLine("【策略建议】")
                greenTags.forEach { appendLine("- ${it.content}") }
            }
        }
    }
}
