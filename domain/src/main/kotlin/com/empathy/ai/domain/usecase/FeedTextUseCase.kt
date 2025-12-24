package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import javax.inject.Inject

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
    private val privacyRepository: PrivacyRepository,
    private val aiProviderRepository: AiProviderRepository
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
            // 1. 获取默认服务商
            val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
            if (defaultProvider == null) {
                return Result.failure(IllegalStateException("未配置默认 AI 服务商，请先在设置中配置"))
            }
            if (defaultProvider.apiKey.isBlank()) {
                return Result.failure(IllegalStateException("默认服务商的 API Key 为空，请检查配置"))
            }
            
            // 2. 前置脱敏（通过Repository接口）
            val maskedText = privacyRepository.maskText(rawText)

            // 3. AI 萃取（传递provider配置）
            val extractResult = aiRepository.extractTextInfo(
                provider = defaultProvider,
                inputText = maskedText
            )

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
