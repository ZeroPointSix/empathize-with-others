package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ConversationTopic

/**
 * 对话主题UI事件
 *
 * 定义用户在主题设置界面可以触发的所有操作
 */
sealed class TopicUiEvent {
    /** 显示设置对话框 */
    data object ShowSettingDialog : TopicUiEvent()

    /** 隐藏设置对话框 */
    data object HideSettingDialog : TopicUiEvent()

    /** 更新输入内容 */
    data class UpdateInput(val content: String) : TopicUiEvent()

    /** 保存主题 */
    data object SaveTopic : TopicUiEvent()

    /** 清除主题 */
    data object ClearTopic : TopicUiEvent()

    /** 从历史记录选择主题 */
    data class SelectFromHistory(val topic: ConversationTopic) : TopicUiEvent()

    /** 清除错误信息 */
    data object ClearError : TopicUiEvent()

    /** 清除保存成功状态 */
    data object ClearSaveSuccess : TopicUiEvent()
}
