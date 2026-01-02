package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ApiUsageRecord
import com.empathy.ai.domain.model.ApiUsageStats
import com.empathy.ai.domain.model.UsageStatsPeriod
import kotlinx.coroutines.flow.Flow

/**
 * API 用量统计仓库接口
 *
 * 定义用量记录的存储和查询操作
 *
 * TD-00025: AI配置功能完善 - 用量统计功能
 */
interface ApiUsageRepository {

    /**
     * 记录 API 调用用量
     *
     * @param record 用量记录
     */
    suspend fun recordUsage(record: ApiUsageRecord)

    /**
     * 获取指定时间段的用量统计
     *
     * @param period 统计时间段
     * @return 用量统计结果
     */
    suspend fun getUsageStats(period: UsageStatsPeriod): ApiUsageStats

    /**
     * 获取指定时间范围的用量统计
     *
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 用量统计结果
     */
    suspend fun getUsageStats(startTime: Long, endTime: Long): ApiUsageStats

    /**
     * 获取最近的用量记录
     *
     * @param limit 记录数量限制
     * @return 用量记录列表
     */
    suspend fun getRecentRecords(limit: Int = 100): List<ApiUsageRecord>

    /**
     * 观察指定时间范围的用量记录
     *
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 用量记录 Flow
     */
    fun observeRecords(startTime: Long, endTime: Long): Flow<List<ApiUsageRecord>>

    /**
     * 清除指定时间之前的记录
     *
     * @param beforeTime 时间戳
     * @return 删除的记录数
     */
    suspend fun clearRecordsBefore(beforeTime: Long): Int

    /**
     * 清除所有记录
     */
    suspend fun clearAllRecords()

    /**
     * 获取总用量统计
     *
     * @return 全部时间的用量统计
     */
    suspend fun getTotalUsage(): ApiUsageStats

    /**
     * 导出用量数据
     *
     * @return JSON 格式的用量数据
     */
    suspend fun exportUsageData(): String

    /**
     * 获取记录总数
     *
     * @return 记录总数
     */
    suspend fun getTotalRecordCount(): Int
}
