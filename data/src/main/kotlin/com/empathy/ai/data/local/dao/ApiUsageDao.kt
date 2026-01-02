package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.empathy.ai.data.local.entity.ApiUsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * API 用量记录 DAO
 *
 * 提供用量记录的 CRUD 操作和统计查询
 */
@Dao
interface ApiUsageDao {

    /**
     * 插入用量记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ApiUsageEntity)

    /**
     * 批量插入用量记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ApiUsageEntity>)

    /**
     * 获取指定时间范围内的记录
     */
    @Query("""
        SELECT * FROM api_usage_records 
        WHERE created_at >= :startTime AND created_at <= :endTime
        ORDER BY created_at DESC
    """)
    suspend fun getRecordsByTimeRange(startTime: Long, endTime: Long): List<ApiUsageEntity>

    /**
     * 获取指定时间范围内的记录（Flow）
     */
    @Query("""
        SELECT * FROM api_usage_records 
        WHERE created_at >= :startTime AND created_at <= :endTime
        ORDER BY created_at DESC
    """)
    fun observeRecordsByTimeRange(startTime: Long, endTime: Long): Flow<List<ApiUsageEntity>>

    /**
     * 获取最近的记录
     */
    @Query("""
        SELECT * FROM api_usage_records 
        ORDER BY created_at DESC 
        LIMIT :limit
    """)
    suspend fun getRecentRecords(limit: Int): List<ApiUsageEntity>

    /**
     * 获取总记录数
     */
    @Query("SELECT COUNT(*) FROM api_usage_records")
    suspend fun getTotalCount(): Int

    /**
     * 获取指定时间范围内的统计数据
     */
    @Query("""
        SELECT 
            COUNT(*) as totalRequests,
            SUM(CASE WHEN is_success = 1 THEN 1 ELSE 0 END) as successRequests,
            SUM(CASE WHEN is_success = 0 THEN 1 ELSE 0 END) as failedRequests,
            COALESCE(SUM(total_tokens), 0) as totalTokens,
            COALESCE(SUM(prompt_tokens), 0) as totalPromptTokens,
            COALESCE(SUM(completion_tokens), 0) as totalCompletionTokens,
            COALESCE(AVG(request_time_ms), 0) as averageRequestTimeMs
        FROM api_usage_records
        WHERE created_at >= :startTime AND created_at <= :endTime
    """)
    suspend fun getUsageStats(startTime: Long, endTime: Long): UsageStatsRaw

    /**
     * 按服务商统计
     */
    @Query("""
        SELECT 
            provider_id as providerId,
            provider_name as providerName,
            COUNT(*) as requestCount,
            SUM(CASE WHEN is_success = 1 THEN 1 ELSE 0 END) as successCount,
            COALESCE(SUM(total_tokens), 0) as totalTokens,
            COALESCE(AVG(request_time_ms), 0) as averageRequestTimeMs
        FROM api_usage_records
        WHERE created_at >= :startTime AND created_at <= :endTime
        GROUP BY provider_id, provider_name
        ORDER BY requestCount DESC
    """)
    suspend fun getProviderStats(startTime: Long, endTime: Long): List<ProviderUsageStatsRaw>

    /**
     * 按模型统计
     */
    @Query("""
        SELECT 
            model_id as modelId,
            model_name as modelName,
            provider_id as providerId,
            provider_name as providerName,
            COUNT(*) as requestCount,
            SUM(CASE WHEN is_success = 1 THEN 1 ELSE 0 END) as successCount,
            COALESCE(SUM(total_tokens), 0) as totalTokens,
            COALESCE(AVG(request_time_ms), 0) as averageRequestTimeMs
        FROM api_usage_records
        WHERE created_at >= :startTime AND created_at <= :endTime
        GROUP BY model_id, model_name, provider_id, provider_name
        ORDER BY requestCount DESC
    """)
    suspend fun getModelStats(startTime: Long, endTime: Long): List<ModelUsageStatsRaw>

    /**
     * 删除指定时间之前的记录
     */
    @Query("DELETE FROM api_usage_records WHERE created_at < :beforeTime")
    suspend fun deleteRecordsBefore(beforeTime: Long): Int

    /**
     * 删除所有记录
     */
    @Query("DELETE FROM api_usage_records")
    suspend fun deleteAll()

    /**
     * 获取所有记录（用于导出）
     */
    @Query("SELECT * FROM api_usage_records ORDER BY created_at DESC")
    suspend fun getAllRecords(): List<ApiUsageEntity>
}

/**
 * 统计数据原始结果
 */
data class UsageStatsRaw(
    val totalRequests: Int,
    val successRequests: Int,
    val failedRequests: Int,
    val totalTokens: Long,
    val totalPromptTokens: Long,
    val totalCompletionTokens: Long,
    val averageRequestTimeMs: Long
)

/**
 * 按服务商统计原始结果
 */
data class ProviderUsageStatsRaw(
    val providerId: String,
    val providerName: String,
    val requestCount: Int,
    val successCount: Int,
    val totalTokens: Long,
    val averageRequestTimeMs: Long
)

/**
 * 按模型统计原始结果
 */
data class ModelUsageStatsRaw(
    val modelId: String,
    val modelName: String,
    val providerId: String,
    val providerName: String,
    val requestCount: Int,
    val successCount: Int,
    val totalTokens: Long,
    val averageRequestTimeMs: Long
)
