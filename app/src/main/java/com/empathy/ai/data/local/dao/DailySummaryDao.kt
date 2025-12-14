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
}
