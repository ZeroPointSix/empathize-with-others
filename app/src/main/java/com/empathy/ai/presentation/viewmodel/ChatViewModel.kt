package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ChatMessage
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageSender
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.domain.usecase.CheckDraftUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.presentation.ui.screen.chat.ChatUiEvent
import com.empathy.ai.presentation.ui.screen.chat.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 聊天界面的ViewModel
 *
 * 职责：
 * 1. 管理聊天界面的 UI 状态
 * 2. 处理用户交互事件
 * 3. 调用 UseCase 执行业务逻辑
 * 4. 异常处理和状态更新
 *
 * 注意：由于当前项目没有专门的消息Repository，这里模拟消息管理
 * 在实际项目中，应该有专门的GetMessagesUseCase和SaveMessageUseCase
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase,
    private val checkDraftUseCase: CheckDraftUseCase,
    private val getContactUseCase: GetContactUseCase
) : ViewModel() {

    // 私有可变状态（只能内部修改）
    private val _uiState = MutableStateFlow(ChatUiState())

    // 公开不可变状态（外部只读）
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // 模拟的消息存储（实际项目中应该在数据库中）
    private val mockMessages = mutableMapOf<String, MutableList<ChatMessage>>()

    /**
     * 统一的事件处理入口
     *
     * 设计意图：
     * 1. 单一入口，便于追踪和调试
     * 2. when 表达式确保处理所有事件类型
     */
    fun onEvent(event: ChatUiEvent) {
        when (event) {
            // === 消息相关事件 ===
            is ChatUiEvent.SendMessage -> sendMessage(event.content)
            is ChatUiEvent.UpdateInputText -> updateInputText(event.text)
            is ChatUiEvent.LoadChat -> loadChat(event.contactId)
            is ChatUiEvent.RefreshChat -> refreshChat()
            is ChatUiEvent.DeleteMessage -> deleteMessage(event.messageId)

            // === 分析相关事件 ===
            is ChatUiEvent.AnalyzeChat -> analyzeChat()
            is ChatUiEvent.ShowAnalysisDialog -> showAnalysisDialog()
            is ChatUiEvent.DismissAnalysisDialog -> dismissAnalysisDialog()
            is ChatUiEvent.ApplySuggestion -> applySuggestion(event.suggestion)

            // === 安全检查相关事件 ===
            is ChatUiEvent.CheckDraftSafety -> checkDraftSafety(event.text)
            is ChatUiEvent.ShowSafetyWarning -> showSafetyWarning()
            is ChatUiEvent.DismissSafetyWarning -> dismissSafetyWarning()

            // === UI交互事件 ===
            is ChatUiEvent.ScrollToBottom -> scrollToBottom()
            is ChatUiEvent.MarkScrolling -> markScrolling()
            is ChatUiEvent.MarkScrollingFinished -> markScrollingFinished()

            // === 通用事件 ===
            is ChatUiEvent.ClearError -> clearError()
            is ChatUiEvent.NavigateBack -> navigateBack()
        }
    }

    /**
     * 私有方法：发送消息
     *
     * 状态更新模式：使用 update {} 函数式更新
     */
    private fun sendMessage(content: String) {
        viewModelScope.launch {
            // 输入验证
            if (content.isBlank()) {
                _uiState.update { it.copy(error = "消息不能为空") }
                return@launch
            }

            val currentState = _uiState.value
            val newMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = content.trim(),
                sender = MessageSender.ME,
                timestamp = System.currentTimeMillis()
            )

            // 模拟保存消息到本地存储
            val messages = mockMessages.getOrPut(currentState.contactId) { mutableListOf() }
            messages.add(newMessage)

            // 更新UI状态
            _uiState.update { currentState ->
                currentState.copy(
                    messages = messages.toList(), // 创建不可变副本
                    inputText = "", // 清空输入框
                    error = null
                )
            }

            // 模拟对方回复（延迟1秒）
            kotlinx.coroutines.delay(1000)
            simulateOtherReply(content)
        }
    }

    /**
     * 模拟对方回复（仅用于演示）
     */
    private fun simulateOtherReply(userMessage: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val replyMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = generateMockReply(userMessage),
                sender = MessageSender.THEM,
                timestamp = System.currentTimeMillis()
            )

            val messages = mockMessages.getOrPut(currentState.contactId) { mutableListOf() }
            messages.add(replyMessage)

            _uiState.update { it.copy(messages = messages.toList()) }
        }
    }

    /**
     * 生成模拟回复（仅用于演示）
     */
    private fun generateMockReply(userMessage: String): String {
        return when {
            userMessage.contains("你好") -> "你好！很高兴见到你！"
            userMessage.contains("在吗") -> "在的，有什么事吗？"
            userMessage.contains("谢谢") -> "不客气！"
            userMessage.contains("再见") -> "再见，保持联系！"
            else -> "收到你的消息了，让我想想..."
        }
    }

    /**
     * 更新输入框内容
     */
    private fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }

        // 自动检查草稿安全
        if (text.isNotBlank()) {
            onEvent(ChatUiEvent.CheckDraftSafety(text))
        }
    }

    /**
     * 加载聊天记录
     */
    private fun loadChat(contactId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    contactId = contactId,
                    error = null
                )
            }

            try {
                // 加载联系人信息
                val profile = getContactUseCase(contactId).getOrNull()

                // 加载聊天记录（这里使用模拟数据）
                val messages = mockMessages.getOrPut(contactId) {
                    // 如果没有记录，生成一些模拟数据
                    mutableListOf(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            content = "你好，很高兴认识你！",
                            sender = MessageSender.THEM,
                            timestamp = System.currentTimeMillis() - 3600000
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contactProfile = profile,
                        messages = messages.toList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    /**
     * 刷新聊天记录
     */
    private fun refreshChat() {
        val currentState = _uiState.value
        if (currentState.contactId.isNotEmpty()) {
            loadChat(currentState.contactId)
        }
    }

    /**
     * 删除消息
     */
    private fun deleteMessage(messageId: String) {
        val currentState = _uiState.value
        val messages = mockMessages[currentState.contactId]
        messages?.removeIf { it.id == messageId }

        _uiState.update {
            it.copy(
                messages = messages?.toList() ?: emptyList()
            )
        }
    }

    /**
     * 分析聊天
     *
     * 异步操作模式：
     * 1. 开始前设置 isAnalyzing = true
     * 2. 调用UseCase
     * 3. 成功/失败分别处理
     * 4. 最后设置 isAnalyzing = false
     */
    private fun analyzeChat() {
        viewModelScope.launch {
            // 1. 设置加载状态
            _uiState.update { it.copy(isAnalyzing = true, error = null) }

            try {
                // 2. 调用UseCase
                val currentState = _uiState.value
                val contactId = currentState.contactId

                if (contactId.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            error = "请先选择联系人"
                        )
                    }
                    return@launch
                }

                // 提取消息内容用于分析
                val messageContents = currentState.messages
                    .takeLast(currentState.contactProfile?.contextDepth ?: 10)
                    .map { it.content }

                if (messageContents.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            error = "没有可分析的消息"
                        )
                    }
                    return@launch
                }

                val result = analyzeChatUseCase(
                    contactId = contactId,
                    rawScreenContext = messageContents
                )

                // 3. 处理结果
                result.onSuccess { analysisResult ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            analysisResult = analysisResult,
                            showAnalysisDialog = true
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            error = error.message ?: "分析失败，请重试"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        error = e.message ?: "分析失败"
                    )
                }
            }
        }
    }

    /**
     * 检查草稿安全
     */
    private fun checkDraftSafety(text: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCheckingSafety = true) }

                val currentState = _uiState.value
                val result = checkDraftUseCase(
                    contactId = currentState.contactId,
                    draftSnapshot = text
                )

                result.onSuccess { safetyCheckResult ->
                    _uiState.update {
                        it.copy(
                            isCheckingSafety = false,
                            safetyCheckResult = safetyCheckResult,
                            showSafetyWarning = !safetyCheckResult.isSafe
                        )
                    }
                }.onFailure { error ->
                    // 检查失败时不显示警告，但记录错误
                    _uiState.update {
                        it.copy(
                            isCheckingSafety = false,
                            safetyCheckResult = SafetyCheckResult(isSafe = true),
                            showSafetyWarning = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingSafety = false,
                        safetyCheckResult = SafetyCheckResult(isSafe = true),
                        showSafetyWarning = false
                    )
                }
            }
        }
    }

    /**
     * 应用话术建议
     */
    private fun applySuggestion(suggestion: String) {
        _uiState.update { it.copy(inputText = suggestion, showAnalysisDialog = false) }
    }

    // === UI状态管理方法 ===

    private fun showAnalysisDialog() {
        _uiState.update { it.copy(showAnalysisDialog = true) }
    }

    private fun dismissAnalysisDialog() {
        _uiState.update { it.copy(showAnalysisDialog = false) }
    }

    private fun showSafetyWarning() {
        _uiState.update { it.copy(showSafetyWarning = true) }
    }

    private fun dismissSafetyWarning() {
        _uiState.update { it.copy(showSafetyWarning = false) }
    }

    private fun scrollToBottom() {
        _uiState.update { it.copy(isScrollingToBottom = true) }
    }

    private fun markScrolling() {
        _uiState.update { it.copy(showScrollToBottomButton = false) }
    }

    private fun markScrollingFinished() {
        _uiState.update { it.copy(isScrollingToBottom = false) }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun navigateBack() {
        // 这里可以添加导航逻辑，或者通过状态通知UI层
        _uiState.update { it.copy(shouldNavigateBack = true) }
    }

    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        mockMessages.clear()
    }
}