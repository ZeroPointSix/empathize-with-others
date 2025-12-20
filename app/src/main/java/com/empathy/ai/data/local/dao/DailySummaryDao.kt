package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.empathy.ai.data.local.entity.DailySummaryEntity

/**
 * 每日总结DAO
 *
 * 提供每日总结的数据库访问方法
 *
 * v9新增方法：
 * - getSummariesInRange: 获取指定日期范围内的总结
 * - getSummarizedDatesInRange: 获取已有总结的日期列表
 * - deleteSummariesInRange: 删除指定范围内的总结
 * - getManualSummaries: 获取手动生成的总结
 * - countMissingDatesInRange: 统计缺失总结的日期数量
 */
@Dao
interface DailySummaryDao {

    /**
     * 插入每日总结（如果已存在则替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailySummaryEntity): Long

    /**
     * 获取指定联系人的所有总结
     */
    @Query("SELECT * FROM daily_summaries WHERE contact_id = :contactId ORDER BY summary_date DESC")
    suspend fun getSummariesByContact(contactId: String): List<DailySummaryEntity>

    /**
     * 获取指定日期的总结
     */
    @Query("SELECT * FROM daily_summaries WHERE contact_id = :contactId AND summary_date = :date")
    suspend fun getSummaryByDate(contactId: String, date: String): DailySummaryEntity?

    /**
     * 检查指定日期是否已有总结
     */
    @Query("SELECT EXISTS(SELECT 1 FROM daily_summaries WHERE contact_id = :contactId AND summary_date = :date)")
    suspend fun hasSummaryForDate(contactId: String, date: String): Boolean

    /**
     * 删除指定联系人的所有总结
     */
    @Query("DELETE FROM daily_summaries WHERE contact_id = :contactId")
    suspend fun deleteByContactId(contactId: String): Int

    /**
     * 清理过期的总结
     */
    @Query("DELETE FROM daily_summaries WHERE created_at < :beforeTimestamp")
    suspend fun cleanupOldSummaries(beforeTimestamp: Long): Int

    /**
     * 获取指定联系人最近N条总结
     */
    @Query("SELECT * FROM daily_summaries WHERE contact_id = :contactId ORDER BY summary_date DESC LIMIT :limit")
    suspend fun getRecentSummaries(contactId: String, limit: Int): List<DailySummaryEntity>

    // ==================== v9 新增方法 ====================

    /**
     * 获取指定日期范围内的总结
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 范围内的总结列表，按日期升序排列
     */
    @Query("""
        SELECT * FROM daily_summaries 
        WHERE contact_id = :contactId 
        AND summary_date >= :startDate 
        AND summary_date <= :endDate 
        ORDER BY summary_date ASC
    """)
    suspend fun getSummariesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): List<DailySummaryEntity>

    /**
     * 获取指定日期范围内已有总结的日期列表
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 已有总结的日期列表
     */
    @Query("""
        SELECT summary_date FROM daily_summaries 
        WHERE contact_id = :contactId 
        AND summary_date >= :startDate 
        AND summary_date <= :endDate
    """)
    suspend fun getSummarizedDatesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): List<String>

    /**
     * 删除指定日期范围内的总结
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 删除的记录数
     */
    @Query("""
        DELETE FROM daily_summaries 
        WHERE contact_id = :contactId 
        AND summary_date >= :startDate 
        AND summary_date <= :endDate
    """)
    suspend fun deleteSummariesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Int

    /**
     * 获取手动生成的总结
     *
     * @param contactId 联系人ID
     * @return 手动生成的总结列表，按生成时间降序排列
     */
    @Query("""
        SELECT * FROM daily_summaries 
        WHERE contact_id = :contactId 
        AND generation_source = 'MANUAL' 
        ORDER BY generated_at DESC
    """)
    suspend fun getManualSummaries(contactId: String): List<DailySummaryEntity>

    /**
     * 统计指定范围内缺失总结的日期数量
     *
     * 注意：此方法使用递归CTE生成日期序列，SQLite 3.8.3+支持
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 缺失总结的日期数量
     */
    @Query("""
        WITH RECURSIVE dates(date) AS (
            SELECT :startDate
            UNION ALL
            SELECT date(date, '+1 day') FROM dates WHERE date < :endDate
        )
        SELECT COUNT(*) FROM dates
        WHERE date NOT IN (
            SELECT summary_date FROM daily_summaries 
            WHERE contact_id = :contactId
        )
    """)
    suspend fun countMissingDatesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Int

    /**
     * 获取指定类型的总结
     *
     * @param contactId 联系人ID
     * @param summaryType 总结类型（DAILY/CUSTOM_RANGE）
     * @return 指定类型的总结列表
     */
    @Query("""
        SELECT * FROM daily_summaries 
        WHERE contact_id = :contactId 
        AND summary_type = :summaryType 
        ORDER BY summary_date DESC
    """)
    suspend fun getSummariesByType(
        contactId: String,
        summaryType: String
    ): List<DailySummaryEntity>
}
