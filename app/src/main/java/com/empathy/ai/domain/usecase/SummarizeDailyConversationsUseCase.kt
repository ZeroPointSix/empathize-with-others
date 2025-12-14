package com.empathy.ai.domain.usecase

import android.util.Log
import com.empathy.ai.data.local.MemoryPreferences
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.util.AiSummaryProcessor
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.FailedTaskRecovery
import com.empathy.ai.domain.util.LocalSummaryProcessor
import com.empathy.ai.domain.repository.DailySummaryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日自动总结用例
 *
 * 负责在新一天首次打开App时自动触发对话总结：
 * - 查询所有有未总结对话的联系人
 * - 调用AI生成每日总结
 * - 更新联系人的Facts、BrainTag和关系分数
 * - 处理失败任务的恢复
 *
 * 触发时机：新一天首次打开App时
 *
 * 重构说明：
 * - AI总结逻辑委托给AiSummaryProcessor
 * - 本地降级逻辑委托给LocalSummaryProcessor
 * - 失败任务恢复委托给FailedTaskRecovery
 */
@Singleton
class SummarizeDailyConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val contactRepository: ContactRepository,
    private val memoryPreferences: MemoryPreferences,
    private val aiSummaryProcessor: AiSummaryProcessor,
    private val localSummaryProcessor: LocalSummaryProcessor,
    private val failedTaskRecovery: FailedTaskRecovery
) {
    companion object {
        private const val TAG = "SummarizeDailyUseCase"
    }

    /**
     * 总结结果统计
     */
    data class SummaryResult(
        val totalContacts: Int,
        val successCount: Int,
        val failedCount: Int,
        val skippedCount: Int
    )

    /**
     * 执行每日总结
     *
     * @return 总结结果统计
     */
    suspend operator fun invoke(): Result<SummaryResult> {
        return try {
            val today = DateUtils.getCurrentDateString()
            val lastSummaryDate = memoryPreferences.getLastSummaryDate()

            // 检查是否需要执行总结（日期不同才执行）
            if (lastSummaryDate == today) {
                Log.d(TAG, "今日已执行过总结，跳过")
                return Result.success(SummaryResult(0, 0, 0, 0))
            }

            Log.d(TAG, "开始执行每日总结，上次总结日期: $lastSummaryDate")

            // 获取昨天的日期作为总结目标
            val yesterday = DateUtils.getYesterdayDateString()

            // 查询所有联系人
            val allContacts = contactRepository.getAllProfiles().first()
            if (allContacts.isEmpty()) {
                Log.d(TAG, "没有联系人，跳过总结")
                memoryPreferences.setLastSummaryDate(today)
                return Result.success(SummaryResult(0, 0, 0, 0))
            }

            var successCount = 0
            var failedCount = 0
            var skippedCount = 0

            // 遍历联系人，执行总结
            for (contact in allContacts) {
                val result = summarizeForContact(contact.id, yesterday)
                when {
                    result.isSuccess && result.getOrNull() == true -> successCount++
                    result.isSuccess && result.getOrNull() == false -> skippedCount++
                    result.isFailure -> failedCount++
                }
            }

            // 更新最后总结日期
            memoryPreferences.setLastSummaryDate(today)

            // 恢复失败任务
            val recoveryResult = failedTaskRecovery.recover { contactId, date ->
                summarizeForContact(contactId, date).getOrNull() == true
            }
            Log.d(TAG, "失败任务恢复: $recoveryResult")

            val summaryResult = SummaryResult(
                totalContacts = allContacts.size,
                successCount = successCount,
                failedCount = failedCount,
                skippedCount = skippedCount
            )

            Log.d(TAG, "每日总结完成: $summaryResult")
            Result.success(summaryResult)
        } catch (e: Exception) {
            Log.e(TAG, "每日总结执行失败", e)
            Result.failure(e)
        }
    }

    /**
     * 为单个联系人执行总结
     *
     * @param contactId 联系人ID
     * @param date 总结日期
     * @return true=成功总结, false=无需总结（没有对话）
     */
    private suspend fun summarizeForContact(
        contactId: String,
        date: String
    ): Result<Boolean> {
        return try {
            // 查询该联系人在指定日期的对话记录
            val conversations = conversationRepository
                .getLogsByContactAndDate(contactId, date)
                .getOrNull() ?: emptyList()

            if (conversations.isEmpty()) {
                Log.d(TAG, "联系人 $contactId 在 $date 没有对话记录，跳过")
                return Result.success(false)
            }

            // 检查是否已有总结
            val hasSummary = dailySummaryRepository
                .hasSummaryForDate(contactId, date)
                .getOrNull() ?: false

            if (hasSummary) {
                Log.d(TAG, "联系人 $contactId 在 $date 已有总结，跳过")
                return Result.success(false)
            }

            // 获取联系人画像
            val profile = contactRepository.getProfile(contactId).getOrNull()
            if (profile == null) {
                Log.w(TAG, "联系人 $contactId 不存在，跳过")
                return Result.success(false)
            }

            // 尝试AI总结
            val aiResult = aiSummaryProcessor.process(profile, conversations, date)

            if (aiResult.isSuccess) {
                // 标记对话为已总结
                markConversationsAsSummarized(conversations)
                Result.success(true)
            } else {
                // AI失败，使用降级方案
                Log.w(TAG, "AI总结失败，使用降级方案", aiResult.exceptionOrNull())
                localSummaryProcessor.process(profile, conversations, date)

                // 保存失败任务以便后续重试
                failedTaskRecovery.saveFailedTask(
                    contactId = contactId,
                    summaryDate = date,
                    failureReason = aiResult.exceptionOrNull()?.message ?: "Unknown error"
                )

                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "联系人 $contactId 总结失败", e)
            Result.failure(e)
        }
    }

    /**
     * 标记对话为已总结
     */
    private suspend fun markConversationsAsSummarized(conversations: List<ConversationLog>) {
        val logIds = conversations.map { it.id }
        conversationRepository.markAsSummarized(logIds)
    }
}
