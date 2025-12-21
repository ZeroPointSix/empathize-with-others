package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ExportFormat
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileDimension
import com.empathy.ai.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * 导出用户画像用例
 *
 * 支持将用户画像导出为JSON或纯文本格式。
 */
class ExportUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * 导出用户画像
     *
     * @param format 导出格式
     * @return 导出结果，成功返回格式化的字符串，失败返回异常
     */
    suspend operator fun invoke(format: ExportFormat): Result<String> {
        // 1. 获取用户画像
        val profileResult = userProfileRepository.getUserProfile()
        if (profileResult.isFailure) {
            return Result.failure(
                profileResult.exceptionOrNull() ?: Exception("获取用户画像失败")
            )
        }
        val profile = profileResult.getOrThrow()
        
        // 2. 根据格式导出
        return when (format) {
            ExportFormat.JSON -> exportAsJson()
            ExportFormat.PLAIN_TEXT -> exportAsPlainText(profile)
        }
    }
    
    private suspend fun exportAsJson(): Result<String> {
        return userProfileRepository.exportUserProfile()
    }
    
    private fun exportAsPlainText(profile: UserProfile): Result<String> {
        return try {
            val text = buildString {
                appendLine("═══════════════════════════════════════")
                appendLine("           个人画像导出")
                appendLine("═══════════════════════════════════════")
                appendLine()
                
                // 基础维度
                appendLine("【基础信息】")
                appendLine()
                
                appendDimensionSection(
                    UserProfileDimension.PERSONALITY_TRAITS.displayName,
                    profile.personalityTraits
                )
                appendDimensionSection(
                    UserProfileDimension.VALUES.displayName,
                    profile.values
                )
                appendDimensionSection(
                    UserProfileDimension.INTERESTS.displayName,
                    profile.interests
                )
                appendDimensionSection(
                    UserProfileDimension.COMMUNICATION_STYLE.displayName,
                    profile.communicationStyle
                )
                appendDimensionSection(
                    UserProfileDimension.SOCIAL_PREFERENCES.displayName,
                    profile.socialPreferences
                )
                
                // 自定义维度
                if (profile.customDimensions.isNotEmpty()) {
                    appendLine("【自定义维度】")
                    appendLine()
                    profile.customDimensions.forEach { (name, tags) ->
                        appendDimensionSection(name, tags)
                    }
                }
                
                // 统计信息
                appendLine("───────────────────────────────────────")
                appendLine("画像完整度: ${profile.getCompleteness()}%")
                appendLine("标签总数: ${profile.getTotalTagCount()}")
                appendLine("创建时间: ${formatTimestamp(profile.createdAt)}")
                appendLine("更新时间: ${formatTimestamp(profile.updatedAt)}")
                appendLine("═══════════════════════════════════════")
            }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(Exception("导出纯文本失败: ${e.message}", e))
        }
    }
    
    private fun StringBuilder.appendDimensionSection(name: String, tags: List<String>) {
        append("• $name: ")
        if (tags.isEmpty()) {
            appendLine("（未填写）")
        } else {
            appendLine(tags.joinToString("、"))
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
