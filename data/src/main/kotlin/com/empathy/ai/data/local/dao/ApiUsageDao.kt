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
 * 提供用量记录的 CRUD 操作和统计查询。
 *
 * 业务背景 (PRD-00025):
 *   - 追踪各服务商的API调用消耗，便于成本控制
 *   - 记录Token使用量，用于优化提示词长度
 *   - 统计请求成功率，识别不稳定的服务商
 *
 * 设计决策:
 *   - 使用聚合查询减少数据处理量
 *   - 支持Flow响应式查询，UI实时更新统计
 *   - 定期自动清理旧记录，防止数据库无限增长
 *
 * 统计指标:
 *   - totalRequests: 总请求数
 *   - successRequests/failedRequests: 成功/失败数
 *   - totalTokens: 消耗的总Token数
 *   - averageRequestTimeMs: 平均响应时间
 *
 * @see ApiUsageEntity 用量记录实体
 * @see FD-00025 AI配置功能完善
 */
@Dao
interface ApiUsageDao {

    /**
     * 插入用量记录
     *
     * 【Upsert策略】使用REPLACE策略：
     * - ID存在时覆盖更新
     * - ID不存在时插入新记录
     * - 防止重复记录
     *
     * @param record 用量记录实体
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
     *
     * 【时间范围查询】用于图表展示：
     * - startTime: 开始时间戳
     * - endTime: 结束时间戳
     * - 按创建时间倒序排列
     *
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 符合条件的记录列表
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
     *
     * 【自动清理触发】用于判断是否需要清理：
     * - 如果记录数超过上限（10000条）
     * - 自动触发旧记录清理
     * - 释放存储空间
     *
     * @return 总记录数
     */
    @Query("SELECT COUNT(*) FROM api_usage_records")
    suspend fun getTotalCount(): Int

    /**
     * 获取指定时间范围内的统计数据
     *
     * 【聚合查询】使用SQL聚合函数：
     *   COUNT(*): 统计总请求数
     *   SUM(CASE WHEN ...): 分别统计成功和失败
     *   COALESCE(SUM(...), 0): 处理NULL值
     *   AVG(): 计算平均响应时间
     *
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 聚合统计数据
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
     *
     * 【自动清理】释放存储空间：
     * - 保留最近90天的记录
     * - 避免数据库无限增长
     *
     * @param beforeTime 时间阈值
     * @return 删除的记录数
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
 *
 * 用于接收SQL聚合查询的原始结果，
 * 不包含业务逻辑，仅作为数据载体。
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
 *
 * 用于接收按服务商分组的聚合查询结果，
 * 展示各服务商的用量分布。
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
 *
 * 用于接收按模型分组的聚合查询结果，
 * 展示各模型的使用情况。
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
