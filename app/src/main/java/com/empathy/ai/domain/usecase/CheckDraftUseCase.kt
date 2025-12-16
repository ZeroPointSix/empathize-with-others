package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.PrivacyEngine
import com.empathy.ai.domain.util.PromptBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 核心业务流二: 主动风控检查
 *
 * 触发场景: 用户打完字心里没底，点击悬浮窗的 [🛡️ 帮我检查] 按钮
 *
 * 功能: 检查用户正在输入的草稿是否触发雷区
 *
 * 提示词系统集成:
 * - 使用PromptBuilder构建CHECK场景的系统指令
 * - 支持用户自定义提示词
 */
class CheckDraftUseCase @Inject constructor(
    private val brainTagRepository: BrainTagRepository,
    private val privacyRepository: PrivacyRepository,
    private val aiRepository: AiRepository,
    private val settingsRepository: com.empathy.ai.domain.repository.SettingsRepository,
    private val aiProviderRepository: com.empathy.ai.domain.repository.AiProviderRepository,
    private val contactRepository: ContactRepository,
    private val promptBuilder: PromptBuilder
) {
    /**
     * 执行草稿安全检查
     *
     * @param contactId 当前对象
     * @param draftSnapshot 当前输入框的文本内容快照
     * @param enableDeepCheck 是否启用云端语义检查 (可选)
     * @return 安全检查结果
     */
    suspend operator fun invoke(
        contactId: String,
        draftSnapshot: String,
        enableDeepCheck: Boolean = false
    ): Result<SafetyCheckResult> {
        return try {
            // 读取本地优先模式设置
            val localFirstEnabled = settingsRepository.getLocalFirstModeEnabled()
                .getOrDefault(true)
            
            // 1. 极速加载: 仅读取该联系人的雷区标签
            val redTags = brainTagRepository.getTagsForContact(contactId)
                .first()
                .filter { it.type == TagType.RISK_RED }

            if (redTags.isEmpty()) {
                // 没有雷区规则，直接返回安全
                return Result.success(SafetyCheckResult(isSafe = true))
            }

            // 2. Layer 1: 本地匹配 (关键词检测)
            // 如果启用了本地优先模式，优先使用本地规则
            if (localFirstEnabled) {
                val triggeredTags = mutableListOf<String>()

                redTags.forEach { tag ->
                    if (draftSnapshot.contains(tag.content, ignoreCase = true)) {
                        triggeredTags.add(tag.content)
                    }
                }

                // 如果本地匹配命中，立即返回危险
                if (triggeredTags.isNotEmpty()) {
                    return Result.success(
                        SafetyCheckResult(
                            isSafe = false,
                            triggeredRisks = triggeredTags,
                            suggestion = "检测到敏感内容: ${triggeredTags.joinToString(", ")}"
                        )
                    )
                }
                
                // 本地检查通过，如果未启用深度检查，直接返回安全
                if (!enableDeepCheck) {
                    return Result.success(SafetyCheckResult(isSafe = true))
                }
            }

            // 3. Layer 2: 云端语义检查
            // 当本地优先模式关闭，或本地检查通过且启用深度检查时执行
            if (!localFirstEnabled || enableDeepCheck) {
                // 获取默认服务商
                val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
                if (defaultProvider == null) {
                    return Result.failure(IllegalStateException("未配置默认 AI 服务商，请先在设置中配置"))
                }
                if (defaultProvider.apiKey.isBlank()) {
                    return Result.failure(IllegalStateException("默认服务商的 API Key 为空，请检查配置"))
                }
                
                // 先脱敏
                val privacyMapping = privacyRepository.getPrivacyMapping().getOrElse { emptyMap() }
                val maskedDraft = PrivacyEngine.mask(draftSnapshot, privacyMapping)

                // 构建提示词上下文
                val profile = contactRepository.getProfile(contactId).getOrNull()
                val promptContext = if (profile != null) {
                    PromptContext.fromContact(profile)
                } else {
                    PromptContext(riskTags = redTags.map { it.content })
                }
                
                // 使用PromptBuilder构建系统指令
                val systemInstruction = promptBuilder.buildSimpleInstruction(
                    scene = PromptScene.CHECK,
                    contactId = contactId,
                    context = promptContext
                )

                // 调用 AI 进行语义风险检查（传递provider配置和自定义系统指令）
                val riskRules = redTags.map { it.content }
                val deepCheckResult = aiRepository.checkDraftSafety(
                    provider = defaultProvider,
                    draft = maskedDraft,
                    riskRules = riskRules,
                    systemInstruction = systemInstruction
                ).getOrThrow()

                return Result.success(deepCheckResult)
            }

            // 本地检查通过，且未启用深度检查
            Result.success(SafetyCheckResult(isSafe = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
