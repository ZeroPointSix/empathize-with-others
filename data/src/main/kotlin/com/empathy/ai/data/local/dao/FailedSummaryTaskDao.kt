package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.empathy.ai.data.local.entity.FailedSummaryTaskEntity

/**
 * 失败总结任务DAO
 *
 * 提供失败任务的数据库访问方法，实现容错重试机制。
 *
 * 业务背景 (PRD-00003):
 *   - AI总结可能因网络问题、服务商错误等失败
 *   - 失败任务需要记录并支持后续重试
 *   - 避免因临时错误导致数据丢失
 *
 * 设计决策:
 *   - 使用自增主键，便于任务队列管理
 *   - 创建contact_id和failed_at索引，优化重试查询
 *   - retry_count记录重试次数，避免无限重试
 *
 * 【重试策略】
 * - 最多重试3次（retry_count < 3）
 * - 重试间隔采用指数退避策略
 * - 7天后自动清理已失败的任务
 * - 超过最大重试次数后标记为最终失败
 *
 * @see FailedSummaryTaskEntity 失败任务实体
 * @see FD-00003 联系人画像记忆系统设计
 */
@Dao
interface FailedSummaryTaskDao {

    /**
     * 插入失败任务
     *
     * 【记录失败】总结任务失败时调用：
     * - 记录失败原因，便于问题诊断
     * - 设置初始重试次数为0
     * - 设置失败时间戳
     *
     * @param task 失败任务实体
     * @return 插入的任务ID
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
     *
     * 【重试队列】系统启动时检查待重试任务：
     * - 筛选 retry_count < 3 的任务
     * - 按失败时间正序排列（先失败的先重试）
     * - 重试成功后删除记录
     *
     * @return 待重试任务列表
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
     *
     * 【自动清理】释放存储空间：
     * - 保留7天的失败记录用于问题追溯
     * - 超过7天自动清理
     *
     * @param beforeTimestamp 时间阈值
     * @return 删除的任务数
     */
    @Query("DELETE FROM failed_summary_tasks WHERE failed_at < :beforeTimestamp")
    suspend fun cleanupOldTasks(beforeTimestamp: Long): Int

    /**
     * 清理已放弃的任务（重试次数>=3）
     *
     * 【最终放弃】超过最大重试次数后清理：
     * - 重试3次仍失败，标记为已放弃
     * - 这些任务需要人工干预处理
     * - 定期清理释放空间
     *
     * @return 删除的任务数
     */
    @Query("DELETE FROM failed_summary_tasks WHERE retry_count >= 3")
    suspend fun cleanupAbandonedTasks(): Int
}
