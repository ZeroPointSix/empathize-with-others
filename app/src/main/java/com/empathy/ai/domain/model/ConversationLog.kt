package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.DateUtils

/**
 * 对话记录领域模型
 *
 * 表示一次用户与AI的对话
 *
 * @property id 对话记录ID
 * @property contactId 联系人ID
 * @property userInput 用户输入的聊天记录
 * @property aiResponse AI的分析回复（可能为空）
 * @property timestamp 记录时间（毫秒）
 * @property isSummarized 是否已被总结处理
 */
data class ConversationLog(
    val id: Long = 0,
    val contactId: String,
    val userInput: String,
    val aiResponse: String?,
    val timestamp: Long,
    val isSummarized: Boolean = false
) {
    init {
        require(contactId.isNotBlank()) { "contactId不能为空" }
        require(userInput.isNotBlank()) { "userInput不能为空" }
        require(timestamp > 0) { "timestamp必须大于0" }
    }

    /**
     * 获取日期字符串（yyyy-MM-dd）
     */
    fun getDateString(): String = DateUtils.formatDate(timestamp)

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
