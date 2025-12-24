package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.DateUtils

/**
 * 对话记录领域模型（扩展版）
 *
 * 表示一次用户与AI的对话
 * 支持编辑追踪功能
 *
 * @property id 对话记录ID
 * @property contactId 联系人ID
 * @property userInput 用户输入的聊天记录
 * @property aiResponse AI的分析回复（可能为空）
 * @property timestamp 记录时间（毫秒）
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
