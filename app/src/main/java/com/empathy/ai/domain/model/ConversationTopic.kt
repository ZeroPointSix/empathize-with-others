package com.empathy.ai.domain.model

import java.util.UUID

/**
 * 对话主题领域模型
 *
 * 表示用户为特定联系人设置的对话主题，用于增强AI理解对话背景。
 * 主题作为系统提示词的一部分传递给AI，帮助AI更好地理解对话背景、
 * 保持话题连贯性，并提供更精准的回应。
 *
 * @property id 主题唯一标识
 * @property contactId 关联的联系人ID
 * @property content 主题内容（最多500字符）
 * @property createdAt 创建时间戳
 * @property updatedAt 最后更新时间戳
 * @property isActive 是否为当前活跃主题
 */
data class ConversationTopic(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    companion object {
        /** 主题内容最大长度 */
        const val MAX_CONTENT_LENGTH = 500

        /** 主题预览最大长度 */
        const val PREVIEW_LENGTH = 50
    }

    /**
     * 获取主题内容预览（前50字符）
     *
     * @return 截断后的预览文本，超过50字符时添加省略号
     */
    fun getPreview(): String {
        return if (content.length > PREVIEW_LENGTH) {
            content.take(PREVIEW_LENGTH) + "..."
        } else {
            content
        }
    }

    /**
     * 验证主题内容是否有效
     *
     * @return true 如果内容非空且不超过最大长度
     */
    fun isValid(): Boolean {
        return content.isNotBlank() && content.length <= MAX_CONTENT_LENGTH
    }
}
