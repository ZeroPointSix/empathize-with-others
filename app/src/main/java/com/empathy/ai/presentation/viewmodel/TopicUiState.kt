package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ConversationTopic

/**
 * 对话主题UI状态
 *
 * 管理主题设置界面的所有状态数据
 */
data class TopicUiState(
    /** 当前活跃主题 */
    val currentTopic: ConversationTopic? = null,
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 是否显示设置对话框 */
    val showSettingDialog: Boolean = false,
    /** 输入框内容 */
    val inputContent: String = "",
    /** 错误信息 */
    val errorMessage: String? = null,
    /** 是否保存成功 */
    val saveSuccess: Boolean = false,
    /** 主题历史记录 */
    val topicHistory: List<ConversationTopic> = emptyList()
) {
    /** 是否有活跃主题 */
    val hasActiveTopic: Boolean get() = currentTopic != null

    /** 主题预览文本 */
    val topicPreview: String get() = currentTopic?.getPreview() ?: ""

    /** 输入字符数 */
    val inputCharCount: Int get() = inputContent.length

    /** 是否超出字符限制 */
    val isOverLimit: Boolean get() = inputCharCount > ConversationTopic.MAX_CONTENT_LENGTH

    /** 是否可以保存 */
    val canSave: Boolean get() = inputContent.isNotBlank() && !isOverLimit && !isLoading
}
