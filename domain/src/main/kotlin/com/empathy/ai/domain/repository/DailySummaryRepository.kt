package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.SummaryType

/**
 * 每日总结仓储接口
 *
 * 业务背景 (PRD-00003):
 * - 每日总结是三层记忆架构的中层（中期记忆）
 * - AI生成的每日互动摘要，提炼关键事件和发现
 * - 用于更新长期记忆(Facts、标签、关系分数)
 *
 * 设计决策:
 * - 日期唯一性：每个联系人每天最多一条总结
 * - 类型区分：AI自动生成(AUTO) vs 用户手动生成(MANUAL)
 * - 范围查询：v9新增支持日期范围查询，便于回顾分析
 *
 * v9新增方法:
 * - getSummariesInRange: 获取指定日期范围内的总结
 * - getSummarizedDatesInRange: 获取已有总结的日期列表
 * - deleteSummariesInRange: 删除指定范围内的总结
 * - getManualSummaries: 获取手动生成的总结
 * - countMissingDatesInRange: 统计缺失总结的日期数量
 */
interface DailySummaryRepository {

    /**
     * 保存每日总结
     *
     * 业务规则:
     * - 如果该日期已存在总结，则覆盖
     * - 总结包含：summary、keyEvents、newFacts、updatedTags、relationshipScoreChange
     * - 自动记录创建时间
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
     * @param date 日期字符串（格式：yyyy-MM-dd）
     * @return 总结对象，不存在则返回null
     */
    suspend fun getSummaryByDate(contactId: String, date: String): Result<DailySummary?>

    /**
     * 检查指定日期是否已有总结
     *
     * 用途: 避免重复生成每日总结
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
     * 业务规则:
     * - 总结永久保留（数据量小，无需清理）
     * - 此方法为未来扩展预留
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

    // ==================== v9 新增方法 ====================

    /**
     * 获取指定日期范围内的总结
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 范围内的总结列表，按日期升序排列
     */
    suspend fun getSummariesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<List<DailySummary>>

    /**
     * 获取指定日期范围内已有总结的日期列表
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 已有总结的日期列表
     */
    suspend fun getSummarizedDatesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<List<String>>

    /**
     * 删除指定日期范围内的总结
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 删除的记录数
     */
    suspend fun deleteSummariesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<Int>

    /**
     * 获取手动生成的总结
     *
     * @param contactId 联系人ID
     * @return 手动生成的总结列表，按生成时间降序排列
     */
    suspend fun getManualSummaries(contactId: String): Result<List<DailySummary>>

    /**
     * 统计指定范围内缺失总结的日期数量
     *
     * @param contactId 联系人ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 缺失总结的日期数量
     */
    suspend fun countMissingDatesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<Int>

    /**
     * 获取指定类型的总结
     *
     * @param contactId 联系人ID
     * @param summaryType 总结类型
     * @return 指定类型的总结列表
     */
    suspend fun getSummariesByType(
        contactId: String,
        summaryType: SummaryType
    ): Result<List<DailySummary>>

    // ============================================================================
    // 编辑追踪扩展方法（v10）
    // ============================================================================

    /**
     * 根据ID获取总结
     *
     * @param summaryId 总结ID
     * @return 总结对象，不存在则返回null
     */
    suspend fun getById(summaryId: Long): DailySummary?

    /**
     * 更新总结内容（编辑追踪）
     *
     * 业务规则:
     * - 仅首次编辑时保存原始内容
     * - 记录修改时间，用于编辑历史追踪
     *
     * @param summaryId 总结ID
     * @param newContent 新的总结内容
     * @param modifiedTime 修改时间
     * @param originalContent 原始内容（仅首次编辑时保存）
     * @return 受影响的行数
     */
    suspend fun updateContent(
        summaryId: Long,
        newContent: String,
        modifiedTime: Long,
        originalContent: String
    ): Int

    /**
     * 删除指定ID的总结
     *
     * @param summaryId 总结ID
     * @return 操作结果
     */
    suspend fun deleteSummary(summaryId: Long): Result<Unit>
}
