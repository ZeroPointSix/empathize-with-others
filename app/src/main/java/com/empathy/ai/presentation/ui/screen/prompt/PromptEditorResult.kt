package com.empathy.ai.presentation.ui.screen.prompt

/**
 * 提示词编辑结果密封类
 *
 * 用于通知调用方编辑操作的结果
 */
sealed class PromptEditorResult {
    /**
     * 保存成功
     */
    data object Saved : PromptEditorResult()

    /**
     * 取消编辑（包括确认放弃修改）
     */
    data object Cancelled : PromptEditorResult()
}
