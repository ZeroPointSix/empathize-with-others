package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地总结处理器（降级方案）
 *
 * 当AI总结失败时使用本地统计方案：
 * - 统计对话次数作为关系分数增量
 * - 生成简单的总结记录
 * - 不生成新的Facts和Tags
 */
@Singleton
class LocalSummaryProcessor @Inject constructor(
    private val contactRepository: ContactRepository,
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "LocalSummaryProcessor"
    }

    /**
     * 执行本地降级总结
     *
     * @param profile 联系人画像
     * @param conversations 对话记录列表
     * @param date 总结日期
     * @return 操作结果
     */
    suspend fun process(
        profile: ContactProfile,
        conversations: List<ConversationLog>,
        date: String
    ): Result<Unit> {
        return try {
            // 简单统计对话次数作为关系分数增量
            val scoreChange = minOf(conversations.size, MemoryConstants.MAX_DAILY_SCORE_CHANGE)
            val newScore = (profile.relationshipScore + scoreChange).coerceIn(
                MemoryConstants.MIN_RELATIONSHIP_SCORE,
                MemoryConstants.MAX_RELATIONSHIP_SCORE
            )

            // 更新关系分数和最后互动日期
            contactRepository.updateContactData(
                contactId = profile.id,
                relationshipScore = newScore,
                lastInteractionDate = DateUtils.getCurrentDateString()
            )

            // 保存简单的总结记录
            val summary = DailySummary(
                id = 0,
                contactId = profile.id,
                summaryDate = date,
                content = "今日共${conversations.size}次互动（本地统计）",
                keyEvents = emptyList(),
                newFacts = emptyList(),
                updatedTags = emptyList(),
                relationshipScoreChange = scoreChange,
                relationshipTrend = if (scoreChange > 0) {
                    RelationshipTrend.IMPROVING
                } else {
                    RelationshipTrend.STABLE
                }
            )

            dailySummaryRepository.saveSummary(summary)

            // 标记对话为已总结
            val logIds = conversations.map { it.id }
            conversationRepository.markAsSummarized(logIds)

            logger.d(TAG, "本地降级总结完成: contactId=${profile.id}, scoreChange=$scoreChange")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "本地降级总结失败", e)
            Result.failure(e)
        }
    }
}
