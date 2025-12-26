package com.empathy.ai.presentation.ui.screen.prompt

import com.empathy.ai.domain.model.PromptScene

/**
 * 提示词编辑器UI事件密封类
 *
 * 定义用户在编辑界面可能触发的所有事件
 */
sealed class PromptEditorUiEvent {
    /**
     * 更新提示词内容
     *
     * @property text 新的提示词文本
     */
    data class UpdatePrompt(val text: String) : PromptEditorUiEvent()

    /**
     * 切换场景
     *
     * @property scene 目标场景
     */
    data class SwitchScene(val scene: PromptScene) : PromptEditorUiEvent()

    /**
     * 保存提示词
     */
    data object SavePrompt : PromptEditorUiEvent()

    /**
     * 取消编辑
     */
    data object CancelEdit : PromptEditorUiEvent()

    /**
     * 确认放弃修改
     */
    data object ConfirmDiscard : PromptEditorUiEvent()

    /**
     * 关闭放弃修改对话框
     */
    data object DismissDiscardDialog : PromptEditorUiEvent()

    /**
     * 清除错误信息
     */
    data object ClearError : PromptEditorUiEvent()

    /**
     * 恢复默认提示词
     */
    data object ResetToDefault : PromptEditorUiEvent()
}
