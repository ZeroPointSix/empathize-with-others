package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.repository.DailySummaryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 总结冲突检测器
 *
 * 负责检测指定日期范围内是否已存在总结，
 * 以及查找缺失总结的日期
 */
@Singleton
class SummaryConflictChecker @Inject constructor(
    private val dailySummaryRepository: DailySummaryRepository
) {
    /**
     * 检测冲突
     *
     * 检查指定日期范围内是否已存在总结
     *
     * @param contactId 联系人ID
     * @param dateRange 日期范围
     * @return 冲突检测结果
     */
    suspend fun checkConflict(
        contactId: String,
        dateRange: DateRange
    ): Result<ConflictResult> {
        return try {
            val existingSummaries = dailySummaryRepository.getSummariesInRange(
                contactId,
                dateRange.startDate,
                dateRange.endDate
            ).getOrNull() ?: emptyList()

            if (existingSummaries.isEmpty()) {
                Result.success(ConflictResult.NoConflict)
            } else {
                val conflictDates = existingSummaries.map { it.summaryDate }
                Result.success(
                    ConflictResult.HasConflict(
                        existingSummaries = existingSummaries,
                        conflictDates = conflictDates
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检测缺失的总结日期
     *
     * 查找指定日期范围内没有总结的日期
     *
     * @param contactId 联系人ID
     * @param dateRange 日期范围
     * @return 缺失总结的日期列表
     */
    suspend fun findMissingDates(
        contactId: String,
        dateRange: DateRange
    ): Result<List<String>> {
        return try {
            val allDates = dateRange.getAllDates()
            val summarizedDates = dailySummaryRepository
                .getSummarizedDatesInRange(contactId, dateRange.startDate, dateRange.endDate)
                .getOrNull() ?: emptyList()

            val missingDates = allDates.filter { it !in summarizedDates }
            Result.success(missingDates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 统计缺失的总结数量
     *
     * @param contactId 联系人ID
     * @param dateRange 日期范围
     * @return 缺失总结的日期数量
     */
    suspend fun countMissingDates(
        contactId: String,
        dateRange: DateRange
    ): Result<Int> {
        return dailySummaryRepository.countMissingDatesInRange(
            contactId,
            dateRange.startDate,
            dateRange.endDate
        )
    }

    /**
     * 检查是否有任何冲突
     *
     * @param contactId 联系人ID
     * @param dateRange 日期范围
     * @return 是否存在冲突
     */
    suspend fun hasAnyConflict(
        contactId: String,
        dateRange: DateRange
    ): Boolean {
        return when (val result = checkConflict(contactId, dateRange).getOrNull()) {
            is ConflictResult.HasConflict -> true
            else -> false
        }
    }
}
