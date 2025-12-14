package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.empathy.ai.data.local.entity.FailedSummaryTaskEntity

/**
 * 失败总结任务DAO
 *
 * 提供失败任务的数据库访问方法
 */
@Dao
interface FailedSummaryTaskDao {

    /**
     * 插入失败任务
     */
    @Insert
    suspend fun insert(task: FailedSummaryTaskEntity): Long

    /**
     * 更新失败任务
     */
    @Update
    suspend fun update(task: FailedSummaryTaskEntity)

    /**
     * 获取所有待重试的任务（重试次数<3）
     */
    @Query("SELECT * FROM failed_summary_tasks WHERE retry_count < 3 ORDER BY failed_at ASC")
    suspend fun getPendingTasks(): List<FailedSummaryTaskEntity>

    /**
     * 删除指定任务
     */
    @Query("DELETE FROM failed_summary_tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)

    /**
     * 删除指定联系人的所有失败任务
     */
    @Query("DELETE FROM failed_summary_tasks WHERE contact_id = :contactId")
    suspend fun deleteByContactId(contactId: String): Int

    /**
     * 清理过期的失败任务（7天前的）
     */
    @Query("DELETE FROM failed_summary_tasks WHERE failed_at < :beforeTimestamp")
    suspend fun cleanupOldTasks(beforeTimestamp: Long): Int

    /**
     * 清理已放弃的任务（重试次数>=3）
     */
    @Query("DELETE FROM failed_summary_tasks WHERE retry_count >= 3")
    suspend fun cleanupAbandonedTasks(): Int
}
