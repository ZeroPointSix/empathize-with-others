package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.PrivacyEngine
import javax.inject.Inject

/**
 * 数据提取结果 (待确认)
 *
 * AI 从文本中提取的信息，需要用户确认后才会入库
 */
data class ExtractedData(
    val facts: Map<String, String> = emptyMap(),
    val redTags: List<String> = emptyList(),
    val greenTags: List<String> = emptyList()
)

// MVP Mock实现，Phase2替换为真实AI调用
/**
 * 核心业务流三: 文本喂养
 *
 * 触发场景: 用户在 App 内点击 [+] 导入数据 并粘贴了一段文字
 *
 * 功能: 从用户提供的文本中提取事实、雷区和策略
 *
 * 注意: 本 UseCase 不直接保存数据，而是返回提取结果供 UI 确认
 */
class FeedTextUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val privacyRepository: PrivacyRepository
) {
    /**
     * 从文本中提取信息
     *
     * @param contactId 目标联系人 ID
     * @param rawText 用户粘贴的文本
     * @return 提取的数据 (待用户确认)
     */
    suspend operator fun invoke(
        contactId: String,
        rawText: String
    ): Result<ExtractedData> {
        return try {
            // 1. 前置脱敏
            val privacyMapping = privacyRepository.getPrivacyMapping().getOrElse { emptyMap() }
            val maskedText = PrivacyEngine.mask(rawText, privacyMapping)

            // 2. AI 萃取
            val extractResult = aiRepository.extractTextInfo(maskedText)

            if (extractResult.isFailure) {
                // AI 调用失败，返回基本的 Mock 数据
                val fallbackData = ExtractedData(
                    facts = mapOf(
                        "爱好" to "阅读",
                        "职业" to "工程师",
                        "生日" to "12.21"
                    ),
                    redTags = listOf(
                        "不要提前任",
                        "不要催促付款"
                    ),
                    greenTags = listOf(
                        "多夸衣品好",
                        "耐心倾听"
                    )
                )
                return Result.success(fallbackData)
            }

            // 3. 返回 AI 提取的结果 (供 UI 确认)
            Result.success(extractResult.getOrThrow())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
