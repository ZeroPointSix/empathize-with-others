package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ApiUsageStats
import com.empathy.ai.domain.model.UsageStatsPeriod
import com.empathy.ai.domain.repository.ApiUsageRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * 获取 API 用量统计用例
 *
 * 支持按时间段（今日/本周/本月/全部）获取统计数据
 *
 * TD-00025: AI配置功能完善 - 用量统计功能
 */
class GetApiUsageStatsUseCase @Inject constructor(
    private val apiUsageRepository: ApiUsageRepository
) {
    /**
     * 获取指定时间段的用量统计
     *
     * @param period 统计时间段
     * @return 用量统计结果
     */
    suspend operator fun invoke(period: UsageStatsPeriod): Result<ApiUsageStats> {
        return try {
            val stats = apiUsageRepository.getUsageStats(period)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取指定时间范围的用量统计
     *
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 用量统计结果
     */
    suspend fun getByTimeRange(startTime: Long, endTime: Long): Result<ApiUsageStats> {
        return try {
            val stats = apiUsageRepository.getUsageStats(startTime, endTime)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取今日统计
     */
    suspend fun getToday(): Result<ApiUsageStats> = invoke(UsageStatsPeriod.TODAY)

    /**
     * 获取本周统计
     */
    suspend fun getThisWeek(): Result<ApiUsageStats> = invoke(UsageStatsPeriod.THIS_WEEK)

    /**
     * 获取本月统计
     */
    suspend fun getThisMonth(): Result<ApiUsageStats> = invoke(UsageStatsPeriod.THIS_MONTH)

    /**
     * 获取全部统计
     */
    suspend fun getAll(): Result<ApiUsageStats> = invoke(UsageStatsPeriod.ALL)

    /**
     * 获取总用量统计
     */
    suspend fun getTotalUsage(): Result<ApiUsageStats> {
        return try {
            val stats = apiUsageRepository.getTotalUsage()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * 获取今日开始时间戳
         */
        fun getTodayStartTime(): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        /**
         * 获取本周开始时间戳
         */
        fun getWeekStartTime(): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        /**
         * 获取本月开始时间戳
         */
        fun getMonthStartTime(): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }
}
