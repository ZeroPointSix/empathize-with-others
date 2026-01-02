package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ApiUsageRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 清理 API 用量记录用例
 *
 * 清理过期的用量记录，默认保留90天数据
 *
 * TD-00025: AI配置功能完善 - 用量统计功能
 */
class CleanupApiUsageUseCase @Inject constructor(
    private val apiUsageRepository: ApiUsageRepository
) {
    /**
     * 清理指定天数之前的记录
     *
     * @param daysToKeep 保留天数，默认90天
     * @return 删除的记录数
     */
    suspend operator fun invoke(daysToKeep: Int = DEFAULT_DAYS_TO_KEEP): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())
            val deletedCount = apiUsageRepository.clearRecordsBefore(cutoffTime)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清理指定时间之前的记录
     *
     * @param beforeTime 时间戳
     * @return 删除的记录数
     */
    suspend fun clearBefore(beforeTime: Long): Result<Int> {
        return try {
            val deletedCount = apiUsageRepository.clearRecordsBefore(beforeTime)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清除所有记录
     *
     * @return 操作结果
     */
    suspend fun clearAll(): Result<Unit> {
        return try {
            apiUsageRepository.clearAllRecords()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取当前记录总数
     *
     * @return 记录总数
     */
    suspend fun getRecordCount(): Result<Int> {
        return try {
            val count = apiUsageRepository.getTotalRecordCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /** 默认保留天数 */
        const val DEFAULT_DAYS_TO_KEEP = 90

        /** 最大记录数 */
        const val MAX_RECORDS = 10000
    }
}
