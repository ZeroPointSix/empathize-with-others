package com.empathy.ai.presentation.ui.screen.chat

/**
 * 聊天界面的用户事件
 *
 * 设计原则：
 * 1. 使用 sealed interface 定义，编译时类型安全
 * 2. 每个事件都是独立的类型
 * 3. 有参数用 data class，无参数用 data object
 * 4. 事件命名使用动词开头，描述用户意图
 */
sealed interface ChatUiEvent {
    // === 消息相关事件 ===

    /**
     * 发送消息
     * @param content 消息内容
     */
    data class SendMessage(val content: String) : ChatUiEvent

    /**
     * 更新输入框内容
     * @param text 新的输入内容
     */
    data class UpdateInputText(val text: String) : ChatUiEvent

    /**
     * 加载聊天记录
     * @param contactId 联系人ID
     */
    data class LoadChat(val contactId: String) : ChatUiEvent

    /**
     * 刷新聊天记录
     */
    data object RefreshChat : ChatUiEvent

    /**
     * 删除消息
     * @param messageId 消息ID
     */
    data class DeleteMessage(val messageId: String) : ChatUiEvent

    // === 分析相关事件 ===

    /**
     * 分析聊天
     */
    data object AnalyzeChat : ChatUiEvent

    /**
     * 显示分析结果对话框
     */
    data object ShowAnalysisDialog : ChatUiEvent

    /**
     * 隐藏分析结果对话框
     */
    data object DismissAnalysisDialog : ChatUiEvent

    /**
     * 应用话术建议
     * @param suggestion 建议内容
     */
    data class ApplySuggestion(val suggestion: String) : ChatUiEvent

    // === 安全检查相关事件 ===

    /**
     * 检查草稿安全
     * @param text 要检查的文本
     */
    data class CheckDraftSafety(val text: String) : ChatUiEvent

    /**
     * 显示安全警告
     */
    data object ShowSafetyWarning : ChatUiEvent

    /**
     * 隐藏安全警告
     */
    data object DismissSafetyWarning : ChatUiEvent

    // === UI交互事件 ===

    /**
     * 滚动到底部
     */
    data object ScrollToBottom : ChatUiEvent

    /**
     * 标记正在滚动
     */
    data object MarkScrolling : ChatUiEvent

    /**
     * 标记滚动结束
     */
    data object MarkScrollingFinished : ChatUiEvent

    // === 通用事件 ===

    /**
     * 清除错误
     */
    data object ClearError : ChatUiEvent

    /**
     * 导航返回
     */
    data object NavigateBack : ChatUiEvent
}