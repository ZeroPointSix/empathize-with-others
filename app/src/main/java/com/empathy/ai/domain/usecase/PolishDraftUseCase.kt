package com.empathy.ai.domain.usecase

import android.util.Log
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.SessionContextService
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.UserProfileContextBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 润色草稿用例
 *
 * 优化用户草稿，使表达更得体，同时检测潜在风险。
 *
 * 流程：
 * 1. 前置检查（AI服务商配置）
 * 2. 加载联系人数据和雷区标签
 * 3. 数据脱敏
 * 4. 添加身份前缀
 * 5. 获取历史对话上下文（BUG-00015修复）
 * 6. 获取用户画像上下文（TD-00013修复）
 * 7. 构建提示词
 * 8. 调用AI
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 * @see BUG-00015 三种模式上下文不共通问题分析
 * @see TD-00013 自己画像界面任务清单 - 润色模式用户画像集成修复
 */
class PolishDraftUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val brainTagRepository: BrainTagRepository,
    private val privacyRepository: PrivacyRepository,
    private val aiRepository: AiRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val promptBuilder: PromptBuilder,
    private val sessionContextService: SessionContextService,
    private val userProfileContextBuilder: UserProfileContextBuilder
) {
    companion object {
        private const val TAG = "PolishDraftUseCase"
    }
    /**
     * 执行草稿润色
     *
     * @param contactId 联系人ID
     * @param draftText 用户草稿
     * @return 润色结果
     */
    suspend operator fun invoke(
        contactId: String,
        draftText: String
    ): Result<PolishResult> {
        return try {
            // 1. 前置检查
            val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
                ?: return Result.failure(FloatingWindowError.NoProviderConfigured)

            // 2. 加载联系人数据
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return Result.failure(FloatingWindowError.ContactNotFound(contactId))

            val brainTags = brainTagRepository.getTagsForContact(contactId).first()
            val redTags = brainTags.filter { it.type == TagType.RISK_RED }

            // 3. 数据脱敏（通过Repository接口，符合Clean Architecture）
            val maskedDraft = privacyRepository.maskText(draftText)

            // 4. 添加身份前缀
            val prefixedDraft = IdentityPrefixHelper.addPrefix(
                content = maskedDraft,
                actionType = ActionType.POLISH
            )

            // 5. 【BUG-00015修复】获取历史对话上下文
            val historyContext = sessionContextService.getHistoryContext(contactId)

            // 6. 【TD-00013修复】获取用户画像上下文
            val userProfileContext = try {
                userProfileContextBuilder.buildAnalysisContext(profile, draftText)
                    .getOrNull() ?: ""
            } catch (e: Exception) {
                Log.w(TAG, "获取用户画像上下文失败，降级为空上下文", e)
                ""  // 降级：用户画像获取失败不影响主流程
            }
            
            Log.d(TAG, "用户画像上下文长度: ${userProfileContext.length}")

            // 7. 构建提示词
            // 【BUG-00023修复】移除自动保存逻辑，改为用户点击复制按钮时保存
            val promptContext = PromptContext.fromContact(profile)
            val runtimeData = buildRuntimeData(prefixedDraft, redTags, historyContext, userProfileContext)
            val systemInstruction = promptBuilder.buildSystemInstruction(
                scene = PromptScene.POLISH,
                contactId = contactId,
                context = promptContext,
                runtimeData = runtimeData
            )

            // 8. 调用AI
            aiRepository.polishDraft(
                provider = defaultProvider,
                draft = prefixedDraft,
                systemInstruction = systemInstruction
            )
        } catch (e: Exception) {
            android.util.Log.e("PolishDraftUseCase", "润色失败", e)
            Result.failure(FloatingWindowError.fromThrowable(e))
        }
    }

    /**
     * 构建运行时数据
     *
     * @param draft 用户草稿（已添加身份前缀）
     * @param redTags 雷区标签列表
     * @param historyContext 历史对话上下文（BUG-00015修复）
     * @param userProfileContext 用户画像上下文（TD-00013修复）
     */
    private fun buildRuntimeData(
        draft: String,
        redTags: List<BrainTag>,
        historyContext: String,
        userProfileContext: String
    ): String {
        return buildString {
            // 【TD-00013修复】用户画像放在最前面，让AI先了解用户特点
            if (userProfileContext.isNotBlank()) {
                appendLine(userProfileContext)
                appendLine()
            }
            // 【BUG-00015修复】历史对话放在用户画像之后
            if (historyContext.isNotBlank()) {
                appendLine(historyContext)
                appendLine()
            }
            appendLine("【用户草稿】")
            appendLine(draft)
            if (redTags.isNotEmpty()) {
                appendLine()
                appendLine("【雷区警告】")
                redTags.forEach { appendLine("- ${it.content}") }
            }
        }
    }

}
