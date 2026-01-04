package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.DateUtils

/**
 * 对话记录领域模型（扩展版）
 *
 * 表示一次用户与AI的对话。
 * 这是AI军师分析功能的核心数据源，提供完整的对话历史。
 *
 * 业务背景 (PRD-00008/PRD-00026):
 * - conversation_logs表是AI军师的主要数据来源（PRD-00026/3.2.1）
 * - 按时间正序排列，构建完整对话流
 * - 识别并标记对话中的发送者身份
 * - 从对话中提取关键关系事件和情感变化
 *
 * 设计决策 (TDD-00008):
 * - 使用Long自增ID，保证插入顺序
 * - aiResponse可为null，支持实时分析场景
 * - 支持编辑追踪，保留原始内容用于审计
 * - isSummarized标记避免重复总结处理
 *
 * 任务追踪: FD-00008/对话历史存储
 *
 * @property id 对话记录ID
 * @property contactId 联系人ID，关联ContactProfile
 * @property userInput 用户输入的聊天记录
 * @property aiResponse AI的分析回复（可能为空）
 * @property timestamp 记录时间（毫秒），用于时间线排序
 * @property isSummarized 是否已被总结处理
 * @property isUserModified 是否被用户修改过
 * @property lastModifiedTime 最后修改时间
 * @property originalUserInput 原始用户输入（修改前）
 */
data class ConversationLog(
    val id: Long = 0,
    val contactId: String,
    val userInput: String,
    val aiResponse: String?,
    val timestamp: Long,
    val isSummarized: Boolean = false,
    // ==================== 编辑追踪字段 ====================
    val isUserModified: Boolean = false,
    val lastModifiedTime: Long = timestamp,
    val originalUserInput: String? = null
) {
    init {
        require(contactId.isNotBlank()) { "contactId不能为空" }
        require(userInput.isNotBlank()) { "userInput不能为空" }
        require(timestamp > 0) { "timestamp必须大于0" }
    }

    /**
     * 创建编辑后的副本
     *
     * @param newUserInput 新的用户输入内容
     * @return 编辑后的ConversationLog副本
     */
    fun copyWithEdit(newUserInput: String): ConversationLog {
        return copy(
            userInput = newUserInput,
            isUserModified = true,
            lastModifiedTime = System.currentTimeMillis(),
            // 仅首次编辑时保存原始值
            originalUserInput = if (originalUserInput == null) userInput else originalUserInput
        )
    }

    /**
     * 判断内容是否有变化
     *
     * @param newUserInput 新的用户输入
     * @return 是否有变化
     */
    fun hasChanges(newUserInput: String): Boolean {
        return userInput != newUserInput
    }

    /**
     * 获取日期字符串（yyyy-MM-dd）
     */
    fun getDateString(): String = DateUtils.formatDate(timestamp)

    /**
     * 格式化最后修改时间
     */
    fun formatLastModifiedTime(): String = DateUtils.formatRelativeTime(lastModifiedTime)

    /**
     * 判断是否完整（有AI回复）
     */
    fun isComplete(): Boolean = aiResponse != null

    /**
     * 获取对话总长度
     */
    fun getTotalLength(): Int {
        return userInput.length + (aiResponse?.length ?: 0)
    }
}
