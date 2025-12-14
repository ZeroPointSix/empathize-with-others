package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.DailySummary

/**
 * 每日总结仓库接口
 *
 * 定义每日总结的数据访问接口
 */
interface DailySummaryRepository {

    /**
     * 保存每日总结
     *
     * @param summary 每日总结对象
     * @return 保存的总结ID
     */
    suspend fun saveSummary(summary: DailySummary): Result<Long>

    /**
     * 获取指定联系人的所有总结
     *
     * @param contactId 联系人ID
     * @return 总结列表，按日期倒序
     */
    suspend fun getSummariesByContact(contactId: String): Result<List<DailySummary>>

    /**
     * 获取指定日期的总结
     *
     * @param contactId 联系人ID
     * @param date 日期字符串
     * @return 总结对象，不存在则返回null
     */
    suspend fun getSummaryByDate(contactId: String, date: String): Result<DailySummary?>

    /**
     * 检查指定日期是否已有总结
     *
     * @param contactId 联系人ID
     * @param date 日期字符串
     * @return 是否存在
     */
    suspend fun hasSummaryForDate(contactId: String, date: String): Result<Boolean>

    /**
     * 删除指定联系人的所有总结
     *
     * @param contactId 联系人ID
     * @return 删除的记录数
     */
    suspend fun deleteByContactId(contactId: String): Result<Int>

    /**
     * 清理过期的总结
     *
     * @param beforeTimestamp 此时间之前的总结将被删除
     * @return 删除的记录数
     */
    suspend fun cleanupOldSummaries(beforeTimestamp: Long): Result<Int>

    /**
     * 获取指定联系人最近N天的总结
     *
     * @param contactId 联系人ID
     * @param days 天数
     * @return 总结列表，按日期倒序
     */
    suspend fun getRecentSummaries(contactId: String, days: Int): List<DailySummary>
}
