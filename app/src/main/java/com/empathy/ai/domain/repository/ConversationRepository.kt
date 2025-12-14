package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ConversationLog

/**
 * 对话记录仓库接口
 *
 * 定义对话记录的数据访问接口
 */
interface ConversationRepository {

    /**
     * 保存用户输入
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
     * @param logId 对话记录ID
     * @param aiResponse AI的分析回复
     * @return 操作结果
     */
    suspend fun updateAiResponse(logId: Long, aiResponse: String): Result<Unit>

    /**
     * 获取未总结的对话记录
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
     * @param beforeTimestamp 此时间之前的已总结对话将被删除
     * @return 删除的记录数
     */
    suspend fun cleanupOldSummarizedLogs(beforeTimestamp: Long): Result<Int>
}
