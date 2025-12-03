package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.PrivacyEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 核心业务流二: 主动风控检查
 *
 * 触发场景: 用户打完字心里没底，点击悬浮窗的 [🛡️ 帮我检查] 按钮
 *
 * 功能: 检查用户正在输入的草稿是否触发雷区
 */
class CheckDraftUseCase @Inject constructor(
    private val brainTagRepository: BrainTagRepository,
    private val privacyRepository: PrivacyRepository,
    private val aiRepository: AiRepository
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
            // 1. 极速加载: 仅读取该联系人的雷区标签
            val redTags = brainTagRepository.getTagsForContact(contactId)
                .first()
                .filter { it.type == TagType.RISK_RED }

            if (redTags.isEmpty()) {
                // 没有雷区规则，直接返回安全
                return Result.success(SafetyCheckResult(isSafe = true))
            }

            // 2. Layer 1: 本地匹配 (关键词检测)
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

            // 3. Layer 2: 云端语义检查 (可选)
            if (enableDeepCheck) {
                // 先脱敏
                val privacyMapping = privacyRepository.getPrivacyMapping().getOrElse { emptyMap() }
                val maskedDraft = PrivacyEngine.mask(draftSnapshot, privacyMapping)

                // 调用 AI 进行语义风险检查
                val riskRules = redTags.map { it.content }
                val deepCheckResult = aiRepository.checkDraftSafety(maskedDraft, riskRules).getOrThrow()

                return Result.success(deepCheckResult)
            }

            // 本地检查通过，且未启用深度检查
            Result.success(SafetyCheckResult(isSafe = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
