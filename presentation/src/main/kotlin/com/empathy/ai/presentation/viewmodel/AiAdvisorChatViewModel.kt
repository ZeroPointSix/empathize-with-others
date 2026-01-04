package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.usecase.CreateAdvisorSessionUseCase
import com.empathy.ai.domain.usecase.DeleteAdvisorConversationUseCase
import com.empathy.ai.domain.usecase.GetAdvisorConversationsUseCase
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageUseCase
import com.empathy.ai.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI军师对话界面ViewModel
 *
 * 管理AI军师对话界面的状态，包括会话管理、消息发送、联系人切换等。
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
    private val deleteAdvisorConversationUseCase: DeleteAdvisorConversationUseCase
) : ViewModel() {

    private val contactId: String = savedStateHandle[NavRoutes.AI_ADVISOR_CHAT_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow(AiAdvisorChatUiState())
    val uiState: StateFlow<AiAdvisorChatUiState> = _uiState.asStateFlow()

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
     */
    fun sendMessage() {
        val currentState = _uiState.value
        val message = currentState.inputText.trim()
        if (message.isEmpty() || currentState.isSending) return

        val sessionId = currentState.currentSessionId ?: return

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
     * 重试发送失败的消息
     */
    fun retryMessage(conversation: AiAdvisorConversation) {
        if (conversation.sendStatus != SendStatus.FAILED) return

        viewModelScope.launch {
            // Delete failed message and resend
            deleteAdvisorConversationUseCase(conversation.id)
            _uiState.update { it.copy(inputText = conversation.content) }
            sendMessage()
        }
    }

    /**
     * 切换会话
     */
    fun switchSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId) }
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
 */
data class AiAdvisorChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
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
    val error: String? = null
)
