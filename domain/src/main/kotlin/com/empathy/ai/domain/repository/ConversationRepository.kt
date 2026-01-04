package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ConversationLog
import kotlinx.coroutines.flow.Flow

/**
 * 对话记录仓储接口
 *
 * 业务背景 (PRD-00003):
 * - 对话记录是三层记忆架构的底层（短期记忆）
 * - 记录每次用户发起的AI分析请求和AI回复
 * - 用于每日总结的原材料，也是AI分析时回顾历史的依据
 *
 * 设计决策:
 * - 用户输入先保存：用户输入立即持久化，不等AI响应
 * - 异步保存AI回复：AI响应后异步更新aiResponse字段
 * - 智能筛选：getRecentConversations限制返回数量，避免token超限
 *
 * 生命周期管理:
 * - 未总结对话：最多保留7天，超期自动标记放弃
 * - 已总结对话：保留30天，之后自动清理
 */
interface ConversationRepository {

    /**
     * 保存用户输入
     *
     * 业务规则:
     * - 用户输入立即持久化，不等AI响应
     * - 即使AI分析失败，用户输入也会被保存
     * - 默认使用当前时间戳，可自定义
     *
     * @param contactId 联系人ID
     * @param userInput 用户输入的聊天记录
     * @param timestamp 时间戳（毫秒）
     * @return 保存的对话记录ID
     */
    suspend fun saveUserInput(
        contactId: String,
        userInput: String,
        timestamp: Long = System.currentTimeMillis()
    ): Result<Long>

    /**
     * 更新AI回复
     *
     * 业务规则:
     * - AI响应后异步更新aiResponse字段
     * - 更新后标记isSummarized = false，等待每日总结
     *
     * @param logId 对话记录ID
     * @param aiResponse AI的分析回复
     * @return 操作结果
     */
    suspend fun updateAiResponse(logId: Long, aiResponse: String): Result<Unit>

    /**
     * 获取未总结的对话记录
     *
     * 业务规则:
     * - 用于每日总结流程
     * - 只获取指定时间戳之后的记录
     * - 按contact_id分组，供SummarizeDailyConversationsUseCase处理
     *
     * @param sinceTimestamp 起始时间戳（只获取此时间之后的记录）
     * @return 未总结的对话记录列表
     */
    suspend fun getUnsummarizedLogs(sinceTimestamp: Long): Result<List<ConversationLog>>

    /**
     * 获取指定联系人在指定日期的对话记录
     *
     * @param contactId 联系人ID
     * @param date 日期字符串，格式"yyyy-MM-dd"
     * @return 对话记录列表
     */
    suspend fun getLogsByContactAndDate(contactId: String, date: String): Result<List<ConversationLog>>

    /**
     * 标记对话为已总结
     *
     * @param logIds 对话记录ID列表
     * @return 操作结果
     */
    suspend fun markAsSummarized(logIds: List<Long>): Result<Unit>

    /**
     * 删除指定联系人的所有对话记录
     *
     * @param contactId 联系人ID
     * @return 删除的记录数
     */
    suspend fun deleteByContactId(contactId: String): Result<Int>

    /**
     * 清理过期的已总结对话
     *
     * 业务规则:
     * - 已总结对话保留30天
     * - 每周自动执行一次清理
     * - 避免数据库无限增长
     *
     * @param beforeTimestamp 此时间之前的已总结对话将被删除
     * @return 删除的记录数
     */
    suspend fun cleanupOldSummarizedLogs(beforeTimestamp: Long): Result<Int>

    /**
     * 获取指定联系人的所有对话记录
     *
     * @param contactId 联系人ID
     * @return 对话记录列表
     */
    suspend fun getConversationsByContact(contactId: String): Result<List<ConversationLog>>

    /**
     * 获取指定联系人的对话记录流
     *
     * @param contactId 联系人ID
     * @return 对话记录流，按时间戳倒序排列
     */
    fun getConversationsByContactFlow(contactId: String): Flow<List<ConversationLog>>

    /**
     * 更新对话记录的用户输入
     *
     * @param logId 对话记录ID
     * @param userInput 新的用户输入内容
     * @return 操作结果
     */
    suspend fun updateUserInput(logId: Long, userInput: String): Result<Unit>

    /**
     * 删除单条对话记录
     *
     * @param logId 对话记录ID
     * @return 操作结果
     */
    suspend fun deleteConversation(logId: Long): Result<Unit>

    /**
     * 获取指定联系人的最近N条对话记录
     *
     * 用途: 构建对话上下文连续性，支持AI分析时回顾历史对话
     *
     * 业务规则:
     * - 返回按时间正序排列的对话记录（最早的在前）
     * - 限制最大条数，避免上下文过长导致token超限
     * - 用于构建AI分析的对话历史上下文
     *
     * @param contactId 联系人ID
     * @param limit 最大条数
     * @return 按时间正序排列的对话记录（最早的在前）
     */
    suspend fun getRecentConversations(
        contactId: String,
        limit: Int
    ): Result<List<ConversationLog>>

    // ============================================================================
    // 编辑追踪扩展方法（v10）
    // ============================================================================

    /**
     * 根据ID获取对话记录
     *
     * @param logId 对话记录ID
     * @return 对话记录，不存在则返回null
     */
    suspend fun getById(logId: Long): ConversationLog?

    /**
     * 更新对话内容（编辑追踪）
     *
     * 业务规则:
     * - 仅首次编辑时保存原始输入
     * - 记录修改时间，用于编辑历史追踪
     *
     * @param logId 对话记录ID
     * @param newUserInput 新的用户输入内容
     * @param modifiedTime 修改时间
     * @param originalInput 原始用户输入（仅首次编辑时保存）
     * @return 受影响的行数
     */
    suspend fun updateUserInputWithTracking(
        logId: Long,
        newUserInput: String,
        modifiedTime: Long,
        originalInput: String
    ): Int
}
