package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.model.StreamingState
import com.empathy.ai.domain.model.TokenUsage
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.usecase.CreateAdvisorSessionUseCase
import com.empathy.ai.domain.usecase.DeleteAdvisorConversationUseCase
import com.empathy.ai.domain.usecase.GetAdvisorConversationsUseCase
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageStreamingUseCase
import com.empathy.ai.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI军师对话界面 ViewModel
 *
 * ## 业务职责
 * 管理AI军师单次对话的完整生命周期：
 * - 消息历史管理（用户/AI/建议回复三种类型）
 * - 输入状态和发送流程控制
 * - 会话管理与切换
 * - 联系人选择和切换
 *
 * ## 关联文档
 * - PRD-00026: AI军师对话功能需求（多轮对话、上下文管理）
 * - TDD-00026: AI军师对话功能技术设计
 * - FD-00026: AI军师对话功能设计（消息流、Token计算）
 *
 * ## 核心数据流
 * ```
 * UserInput → Validate → AIRequest → Response → UIUpdate
 *              ↓                              ↓
 *         ShowError                     TokenCounting
 * ```
 *
 * ## 关键设计决策
 * 1. **消息类型区分**: 用户消息、AI回复、建议回复三种类型，UI展示差异化
 * 2. **会话隔离**: 每个联系人独立会话列表，支持会话切换
 * 3. **联系人切换**: 切换前确认，避免误操作丢失对话上下文
 * 4. **输入验证**: 防止空消息和超长消息，UI层面引导用户
 *
 * ## 重要约束
 * - 单条消息最大长度限制（防止LLM拒绝处理过长内容）
 * - 发送中状态禁止重复发送
 * - 失败消息支持重试
 *
 * @see com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorChatScreen
 */
@HiltViewModel
class AiAdvisorChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getContactUseCase: GetContactUseCase,
    private val getAllContactsUseCase: GetAllContactsUseCase,
    private val createAdvisorSessionUseCase: CreateAdvisorSessionUseCase,
    private val getAdvisorSessionsUseCase: GetAdvisorSessionsUseCase,
    private val getAdvisorConversationsUseCase: GetAdvisorConversationsUseCase,
    private val sendAdvisorMessageUseCase: SendAdvisorMessageUseCase,
    private val sendAdvisorMessageStreamingUseCase: SendAdvisorMessageStreamingUseCase,
    private val deleteAdvisorConversationUseCase: DeleteAdvisorConversationUseCase,
    private val aiAdvisorRepository: AiAdvisorRepository
) : ViewModel() {

    private val contactId: String = savedStateHandle[NavRoutes.AI_ADVISOR_CHAT_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow(AiAdvisorChatUiState())
    val uiState: StateFlow<AiAdvisorChatUiState> = _uiState.asStateFlow()

    /** 流式响应Job，用于取消操作 */
    private var streamingJob: Job? = null

    /** 是否启用流式模式（可配置） */
    private var useStreamingMode: Boolean = true

    init {
        if (contactId.isNotEmpty()) {
            loadContact(contactId)
        }
        loadAllContacts()
    }

    /**
     * 加载联系人信息
     */
    fun loadContact(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getContactUseCase(id).onSuccess { contact ->
                _uiState.update { it.copy(contactName = contact?.name ?: "未知联系人") }
                loadSessions(id)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "加载联系人失败")
                }
            }
        }
    }

    /**
     * 加载所有联系人（用于联系人选择器）
     */
    private fun loadAllContacts() {
        viewModelScope.launch {
            getAllContactsUseCase().collect { contacts ->
                _uiState.update { it.copy(allContacts = contacts) }
            }
        }
    }

    /**
     * 加载会话列表
     */
    private suspend fun loadSessions(contactId: String) {
        getAdvisorSessionsUseCase(contactId).onSuccess { sessions ->
            if (sessions.isEmpty()) {
                // Create default session
                createNewSession(contactId)
            } else {
                val activeSession = sessions.first()
                _uiState.update {
                    it.copy(
                        sessions = sessions,
                        currentSessionId = activeSession.id,
                        isLoading = false
                    )
                }
                loadConversations(activeSession.id)
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(isLoading = false, error = error.message ?: "加载会话失败")
            }
        }
    }

    /**
     * 加载对话记录
     */
    private fun loadConversations(sessionId: String) {
        viewModelScope.launch {
            getAdvisorConversationsUseCase(sessionId).collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
    }

    /**
     * 更新输入文本
     */
    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * 发送消息
     *
     * 根据配置选择流式或非流式模式发送消息。
     * 流式模式提供更好的用户体验，支持实时显示AI回复。
     */
    fun sendMessage() {
        val currentState = _uiState.value
        val message = currentState.inputText.trim()
        if (message.isEmpty() || currentState.isSending || currentState.isStreaming) return

        val sessionId = currentState.currentSessionId ?: return

        if (useStreamingMode) {
            sendMessageStreaming(message, sessionId)
        } else {
            sendMessageNonStreaming(message, sessionId)
        }
    }

    /**
     * 流式发送消息
     *
     * 使用SSE流式响应，实时显示AI回复内容。
     * 支持思考过程展示和停止生成功能。
     *
     * BUG-044-P0-003修复：先取消旧的流式请求，避免重复请求导致卡住
     */
    private fun sendMessageStreaming(message: String, sessionId: String) {
        // BUG-044-P0-003: 先取消旧的流式请求
        streamingJob?.cancel()
        streamingJob = null

        _uiState.update {
            it.copy(
                isStreaming = true,
                isSending = false,
                inputText = "",
                error = null,
                streamingContent = "",
                thinkingContent = "",
                thinkingElapsedMs = 0,
                currentStreamingMessageId = null
            )
        }

        streamingJob = viewModelScope.launch {
            sendAdvisorMessageStreamingUseCase(contactId, sessionId, message)
                .collect { state ->
                    when (state) {
                        is StreamingState.Started -> {
                            _uiState.update {
                                it.copy(currentStreamingMessageId = state.messageId)
                            }
                        }

                        is StreamingState.ThinkingUpdate -> {
                            _uiState.update {
                                it.copy(
                                    thinkingContent = state.content,
                                    thinkingElapsedMs = state.elapsedMs
                                )
                            }
                        }

                        is StreamingState.TextUpdate -> {
                            _uiState.update {
                                it.copy(streamingContent = state.content)
                            }
                        }

                        is StreamingState.Completed -> {
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    streamingContent = "",
                                    thinkingContent = "",
                                    thinkingElapsedMs = 0,
                                    currentStreamingMessageId = null,
                                    lastTokenUsage = state.usage
                                )
                            }
                        }

                        is StreamingState.Error -> {
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    streamingContent = "",
                                    thinkingContent = "",
                                    error = state.error.message ?: "流式响应失败"
                                )
                            }
                        }
                    }
                }
        }
    }

    /**
     * 非流式发送消息（降级模式）
     *
     * 当流式模式不可用时使用，一次性返回完整响应。
     */
    private fun sendMessageNonStreaming(message: String, sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "", error = null) }

            sendAdvisorMessageUseCase(contactId, sessionId, message).onSuccess {
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isSending = false, error = error.message ?: "发送失败")
                }
            }
        }
    }

    /**
     * 停止生成
     *
     * 取消当前流式响应，将消息状态更新为已取消。
     *
     * BUG-044-P1-002修复：保存当前内容，避免显示空的"..."消息
     */
    fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null

        val messageId = _uiState.value.currentStreamingMessageId
        val currentContent = _uiState.value.streamingContent

        _uiState.update {
            it.copy(
                isStreaming = false,
                streamingContent = "",
                thinkingContent = "",
                thinkingElapsedMs = 0,
                currentStreamingMessageId = null
            )
        }

        // BUG-044-P1-002: 根据内容决定处理方式
        messageId?.let { id ->
            viewModelScope.launch {
                if (currentContent.isNotEmpty()) {
                    // 有内容时，保存内容并标记为取消
                    aiAdvisorRepository.updateMessageContentAndStatus(
                        id,
                        currentContent + "\n\n[用户已停止生成]",
                        SendStatus.CANCELLED
                    )
                } else {
                    // 没有内容时，删除消息
                    aiAdvisorRepository.deleteMessage(id)
                }
            }
        }
    }

    /**
     * 重新生成最后一条AI回复
     *
     * 删除最后一条AI消息，使用相同的用户问题重新生成。
     */
    fun regenerateLastMessage() {
        val conversations = _uiState.value.conversations
        val lastAiMessage = conversations.lastOrNull { it.messageType == MessageType.AI }
        val lastUserMessage = conversations.lastOrNull { it.messageType == MessageType.USER }

        if (lastAiMessage != null && lastUserMessage != null) {
            viewModelScope.launch {
                // 删除最后一条AI消息
                deleteAdvisorConversationUseCase(lastAiMessage.id)
                // 重新发送用户消息
                _uiState.update { it.copy(inputText = lastUserMessage.content) }
                sendMessage()
            }
        }
    }

    /**
     * 切换流式模式
     *
     * @param enabled 是否启用流式模式
     */
    fun setStreamingMode(enabled: Boolean) {
        useStreamingMode = enabled
    }

    /**
     * 重试发送失败的消息
     *
     * BUG-044-P1-001修复：扩展条件，包括CANCELLED状态
     */
    fun retryMessage(conversation: AiAdvisorConversation) {
        // BUG-044-P1-001: 支持FAILED和CANCELLED状态的重试
        if (conversation.sendStatus != SendStatus.FAILED &&
            conversation.sendStatus != SendStatus.CANCELLED) return

        viewModelScope.launch {
            // Delete failed/cancelled message and resend
            deleteAdvisorConversationUseCase(conversation.id)
            _uiState.update { it.copy(inputText = conversation.content) }
            sendMessage()
        }
    }

    /**
     * 切换会话
     *
     * BUG-044-P1-005修复：切换前先停止当前流式响应
     */
    fun switchSession(sessionId: String) {
        // BUG-044-P1-005: 先停止当前流式响应
        stopGeneration()

        _uiState.update {
            it.copy(
                currentSessionId = sessionId,
                streamingContent = "",
                thinkingContent = "",
                thinkingElapsedMs = 0,
                error = null
            )
        }
        loadConversations(sessionId)
    }

    /**
     * 创建新会话
     */
    fun createNewSession(forContactId: String = contactId) {
        viewModelScope.launch {
            createAdvisorSessionUseCase(forContactId).onSuccess { session ->
                _uiState.update { currentState ->
                    currentState.copy(
                        sessions = listOf(session) + currentState.sessions,
                        currentSessionId = session.id,
                        isLoading = false
                    )
                }
                loadConversations(session.id)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "创建会话失败")
                }
            }
        }
    }

    /**
     * 显示联系人选择器
     */
    fun showContactSelector() {
        _uiState.update { it.copy(showContactSelector = true) }
    }

    /**
     * 隐藏联系人选择器
     */
    fun hideContactSelector() {
        _uiState.update { it.copy(showContactSelector = false) }
    }

    /**
     * 切换联系人（显示确认对话框）
     */
    fun switchContact(newContactId: String) {
        if (newContactId == contactId) {
            hideContactSelector()
            return
        }
        _uiState.update {
            it.copy(
                showContactSelector = false,
                showSwitchConfirmDialog = true,
                pendingContactId = newContactId
            )
        }
    }

    /**
     * 确认切换联系人
     */
    fun confirmSwitch() {
        val pendingId = _uiState.value.pendingContactId ?: return
        _uiState.update {
            it.copy(
                showSwitchConfirmDialog = false,
                pendingContactId = null,
                shouldNavigateToContact = pendingId
            )
        }
    }

    /**
     * 取消切换联系人
     */
    fun cancelSwitch() {
        _uiState.update {
            it.copy(
                showSwitchConfirmDialog = false,
                pendingContactId = null
            )
        }
    }

    /**
     * 删除消息
     */
    fun deleteMessage(conversationId: String) {
        viewModelScope.launch {
            deleteAdvisorConversationUseCase(conversationId).onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "删除失败")
                }
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 清除导航状态
     */
    fun clearNavigationState() {
        _uiState.update { it.copy(shouldNavigateToContact = null) }
    }
}

/**
 * AI军师对话界面UI状态
 *
 * 包含流式响应相关的状态字段，支持实时显示AI回复。
 *
 * @property isLoading 是否正在加载
 * @property isSending 是否正在发送（非流式模式）
 * @property isStreaming 是否正在流式接收
 * @property contactName 联系人名称
 * @property inputText 输入框文本
 * @property currentSessionId 当前会话ID
 * @property sessions 会话列表
 * @property conversations 对话记录列表
 * @property allContacts 所有联系人列表
 * @property showContactSelector 是否显示联系人选择器
 * @property showSwitchConfirmDialog 是否显示切换确认对话框
 * @property pendingContactId 待切换的联系人ID
 * @property shouldNavigateToContact 应导航到的联系人ID
 * @property error 错误信息
 * @property streamingContent 流式接收的文本内容
 * @property thinkingContent 流式接收的思考内容
 * @property thinkingElapsedMs 思考耗时（毫秒）
 * @property currentStreamingMessageId 当前流式消息ID
 * @property lastTokenUsage 最后一次Token使用统计
 */
data class AiAdvisorChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val contactName: String = "",
    val inputText: String = "",
    val currentSessionId: String? = null,
    val sessions: List<AiAdvisorSession> = emptyList(),
    val conversations: List<AiAdvisorConversation> = emptyList(),
    val allContacts: List<ContactProfile> = emptyList(),
    val showContactSelector: Boolean = false,
    val showSwitchConfirmDialog: Boolean = false,
    val pendingContactId: String? = null,
    val shouldNavigateToContact: String? = null,
    val error: String? = null,
    // 流式响应相关状态
    val streamingContent: String = "",
    val thinkingContent: String = "",
    val thinkingElapsedMs: Long = 0,
    val currentStreamingMessageId: String? = null,
    val lastTokenUsage: TokenUsage? = null
)
