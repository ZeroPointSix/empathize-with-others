package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.ApiUsageDao
import com.empathy.ai.data.local.entity.ApiUsageEntity
import com.empathy.ai.domain.model.ApiUsageRecord
import com.empathy.ai.domain.model.ApiUsageStats
import com.empathy.ai.domain.model.ModelUsageStats
import com.empathy.ai.domain.model.ProviderUsageStats
import com.empathy.ai.domain.model.UsageStatsPeriod
import com.empathy.ai.domain.repository.ApiUsageRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApiUsageRepositoryImpl 实现了API用量统计的数据访问层
 *
 * 【架构位置】Clean Architecture Data层 (TD-00025新增)
 * 【业务背景】(PRD-00025)AI配置功能完善 - 用量统计
 *   - 记录每次API调用的Token消耗和耗时
 *   - 按时间段统计用量，支持日/周/月视图
 *   - 自动清理旧记录，防止数据库无限增长
 *
 * 【设计决策】(TDD-00025)
 *   - MAX_RECORDS = 10000：最大保留1万条记录
 *   - CLEANUP_DAYS = 90：自动清理90天前的旧记录
 *   - 自动清理触发：记录数超过上限时触发
 *
 * 【关键逻辑】
 *   - recordUsage：记录API用量，自动触发清理检查
 *   - getUsageStats：按服务商/模型聚合统计
 *   - getTimeRange：根据UsageStatsPeriod计算时间范围
 *
 * 【任务追踪】
 *   - FD-00025/Task-002: API用量记录功能
 *   - FD-00025/Task-003: 用量统计和图表展示
 */
@Singleton
class ApiUsageRepositoryImpl @Inject constructor(
    private val apiUsageDao: ApiUsageDao,
    private val moshi: Moshi
) : ApiUsageRepository {

    companion object {
        /** 最大记录数，超过时自动清理旧记录 */
        private const val MAX_RECORDS = 10000
        /** 自动清理保留天数 */
        private const val CLEANUP_DAYS = 90
    }

    override suspend fun recordUsage(record: ApiUsageRecord) {
        val entity = record.toEntity()
        apiUsageDao.insert(entity)
        
        // 检查是否需要自动清理
        val totalCount = apiUsageDao.getTotalCount()
        if (totalCount > MAX_RECORDS) {
            val cleanupTime = getCleanupTime()
            apiUsageDao.deleteRecordsBefore(cleanupTime)
        }
    }

    override suspend fun getUsageStats(period: UsageStatsPeriod): ApiUsageStats {
        val (startTime, endTime) = getTimeRange(period)
        return getUsageStats(startTime, endTime)
    }

    override suspend fun getUsageStats(startTime: Long, endTime: Long): ApiUsageStats {
        val statsRaw = apiUsageDao.getUsageStats(startTime, endTime)
        val providerStatsRaw = apiUsageDao.getProviderStats(startTime, endTime)
        val modelStatsRaw = apiUsageDao.getModelStats(startTime, endTime)

        return ApiUsageStats(
            totalRequests = statsRaw.totalRequests,
            successRequests = statsRaw.successRequests,
            failedRequests = statsRaw.failedRequests,
            totalTokens = statsRaw.totalTokens,
            totalPromptTokens = statsRaw.totalPromptTokens,
            totalCompletionTokens = statsRaw.totalCompletionTokens,
            averageRequestTimeMs = statsRaw.averageRequestTimeMs,
            providerStats = providerStatsRaw.map { it.toDomain() },
            modelStats = modelStatsRaw.map { it.toDomain() },
            startTime = startTime,
            endTime = endTime
        )
    }

    override suspend fun getRecentRecords(limit: Int): List<ApiUsageRecord> {
        return apiUsageDao.getRecentRecords(limit).map { it.toDomain() }
    }

    override fun observeRecords(startTime: Long, endTime: Long): Flow<List<ApiUsageRecord>> {
        return apiUsageDao.observeRecordsByTimeRange(startTime, endTime)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun clearRecordsBefore(beforeTime: Long): Int {
        return apiUsageDao.deleteRecordsBefore(beforeTime)
    }

    override suspend fun clearAllRecords() {
        apiUsageDao.deleteAll()
    }

    override suspend fun getTotalUsage(): ApiUsageStats {
        return getUsageStats(UsageStatsPeriod.ALL)
    }

    override suspend fun exportUsageData(): String {
        val records = apiUsageDao.getAllRecords().map { it.toDomain() }
        val type = Types.newParameterizedType(List::class.java, ApiUsageRecord::class.java)
        val adapter = moshi.adapter<List<ApiUsageRecord>>(type)
        return adapter.toJson(records)
    }

    override suspend fun getTotalRecordCount(): Int {
        return apiUsageDao.getTotalCount()
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取时间范围
     */
    private fun getTimeRange(period: UsageStatsPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = System.currentTimeMillis()

        val startTime = when (period) {
            UsageStatsPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            UsageStatsPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            UsageStatsPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            UsageStatsPeriod.ALL -> 0L
        }

        return startTime to endTime
    }

    /**
     * 获取清理时间点（90天前）
     */
    private fun getCleanupTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -CLEANUP_DAYS)
        return calendar.timeInMillis
    }

    // ==================== 转换方法 ====================

    private fun ApiUsageRecord.toEntity(): ApiUsageEntity {
        return ApiUsageEntity(
            id = id,
            providerId = providerId,
            providerName = providerName,
            modelId = modelId,
            modelName = modelName,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            requestTimeMs = requestTimeMs,
            isSuccess = isSuccess,
            errorMessage = errorMessage,
            createdAt = createdAt
        )
    }

    private fun ApiUsageEntity.toDomain(): ApiUsageRecord {
        return ApiUsageRecord(
            id = id,
            providerId = providerId,
            providerName = providerName,
            modelId = modelId,
            modelName = modelName,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            requestTimeMs = requestTimeMs,
            isSuccess = isSuccess,
            errorMessage = errorMessage,
            createdAt = createdAt
        )
    }

    private fun com.empathy.ai.data.local.dao.ProviderUsageStatsRaw.toDomain(): ProviderUsageStats {
        return ProviderUsageStats(
            providerId = providerId,
            providerName = providerName,
            requestCount = requestCount,
            successCount = successCount,
            totalTokens = totalTokens,
            averageRequestTimeMs = averageRequestTimeMs
        )
    }

    private fun com.empathy.ai.data.local.dao.ModelUsageStatsRaw.toDomain(): ModelUsageStats {
        return ModelUsageStats(
            modelId = modelId,
            modelName = modelName,
            providerId = providerId,
            providerName = providerName,
            requestCount = requestCount,
            successCount = successCount,
            totalTokens = totalTokens,
            averageRequestTimeMs = averageRequestTimeMs
        )
    }
}
