package com.empathy.ai.presentation.ui.screen.chat

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ChatMessage
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.SafetyCheckResult

/**
 * 聊天界面的UI状态
 *
 * 设计原则：
 * 1. 所有字段都有默认值，避免空指针
 * 2. 使用 data class，自动获得 copy() 方法
 * 3. 状态是不可变的 (val)，通过 copy() 更新
 * 4. 包含所有UI信息：数据 + 加载状态 + 错误信息
 */
data class ChatUiState(
    // 通用状态字段
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // 联系人信息
    val contactId: String = "",
    val contactProfile: ContactProfile? = null,

    // 聊天消息
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",

    // 分析相关状态
    val isAnalyzing: Boolean = false,
    val analysisResult: AnalysisResult? = null,
    val showAnalysisDialog: Boolean = false,

    // 安全检查状态
    val isCheckingSafety: Boolean = false,
    val safetyCheckResult: SafetyCheckResult? = null,
    val showSafetyWarning: Boolean = false,

    // UI交互状态
    val isScrollingToBottom: Boolean = false,
    val showScrollToBottomButton: Boolean = false,

    // 导航状态
    val shouldNavigateBack: Boolean = false
) {
    // 计算属性：是否有消息
    val hasMessages: Boolean
        get() = messages.isNotEmpty()

    // 计算属性：是否可以发送消息
    val canSendMessage: Boolean
        get() = inputText.isNotBlank() && !isLoading && !isAnalyzing

    // 计算属性：是否需要显示安全警告
    val shouldShowSafetyWarning: Boolean
        get() = showSafetyWarning && safetyCheckResult?.isSafe == false
}