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

    companion object {
        private const val TAG = "AiAdvisorChatViewModel"
    }

    private val contactId: String = savedStateHandle[NavRoutes.AI_ADVISOR_CHAT_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow(AiAdvisorChatUiState())
    val uiState: StateFlow<AiAdvisorChatUiState> = _uiState.asStateFlow()

    /** 流式响应Job，用于取消操作 */
    private var streamingJob: Job? = null

    /** conversations Flow收集Job，用于避免多个收集器冲突 - BUG-046修复 */
    private var conversationsJob: Job? = null

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
     * 
     * BUG-046-P0-V3-003修复：保持当前会话ID，避免自动跳转到第一个会话
     */
    private suspend fun loadSessions(contactId: String) {
        getAdvisorSessionsUseCase(contactId).onSuccess { sessions ->
            if (sessions.isEmpty()) {
                // Create default session
                createNewSession(contactId)
            } else {
                // BUG-046修复：保持当前会话，如果存在的话
                val currentSessionId = _uiState.value.currentSessionId
                val activeSession = if (currentSessionId != null) {
                    sessions.find { it.id == currentSessionId } ?: sessions.first()
                } else {
                    sessions.first()
                }
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
     * 
     * BUG-046修复：使用单一Flow收集器，避免多个收集器冲突
     * 在加载新会话的对话前，先取消之前的收集器
     */
    private fun loadConversations(sessionId: String) {
        // BUG-046修复：取消之前的收集器
        conversationsJob?.cancel()
        
        conversationsJob = viewModelScope.launch {
            getAdvisorConversationsUseCase(sessionId).collect { conversations ->
                // BUG-046修复：验证sessionId仍然匹配，避免跨会话数据污染
                if (_uiState.value.currentSessionId == sessionId) {
                    _uiState.update { it.copy(conversations = conversations) }
                }
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
     * BUG-048修复：记录用户输入到lastUserInput，用于重新生成时避免消息角色混淆
     * BUG-00060：会话自动命名功能，第一条消息时自动更新会话标题
     */
    private fun sendMessageStreaming(message: String, sessionId: String) {
        // BUG-044-P0-003: 先取消旧的流式请求
        streamingJob?.cancel()
        streamingJob = null

        // BUG-00060: 检查是否需要自动命名（第一条消息）
        val currentSession = _uiState.value.sessions.find { it.id == sessionId }
        if (currentSession?.messageCount == 0) {
            val newTitle = generateSessionTitle(message)
            viewModelScope.launch {
                aiAdvisorRepository.updateSessionTitle(sessionId, newTitle)
                // 更新本地会话列表中的标题
                _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions.map { session ->
                            if (session.id == sessionId) {
                                session.copy(title = newTitle)
                            } else {
                                session
                            }
                        }
                    )
                }
            }
        }

        // BUG-048修复：记录用户输入，用于重新生成时使用
        _uiState.update {
            it.copy(
                isStreaming = true,
                isSending = false,
                inputText = "",
                error = null,
                streamingContent = "",
                thinkingContent = "",
                thinkingElapsedMs = 0,
                currentStreamingMessageId = null,
                lastUserInput = message // BUG-048: 记录用户输入
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
                            // BUG-046-P0-V3-001修复：完成后保持内容，等待conversations列表更新
                            // 记录完成时的会话ID，用于延迟清空时验证
                            val completedSessionId = _uiState.value.currentSessionId
                            val completedMessageId = _uiState.value.currentStreamingMessageId
                            
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    // 保持streamingContent，让UI继续显示直到conversations更新
                                    streamingContent = state.fullText,
                                    thinkingContent = "",
                                    thinkingElapsedMs = 0,
                                    // 保持currentStreamingMessageId，让UI知道显示哪条消息
                                    lastTokenUsage = state.usage
                                )
                            }
                            
                            // BUG-047优化：增加延迟时间，确保DB更新和Flow通知完成
                            // 同时验证会话ID未变化，避免跨会话状态污染
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1200)
                                val currentState = _uiState.value
                                
                                // 验证消息是否已在conversations列表中且有内容
                                val messageInList = currentState.conversations.any { 
                                    it.id == completedMessageId && it.content.isNotEmpty() 
                                }
                                
                                if (currentState.currentSessionId == completedSessionId &&
                                    currentState.currentStreamingMessageId == completedMessageId) {
                                    if (messageInList) {
                                        // 消息已正确显示在列表中，可以安全清空streamingContent
                                        _uiState.update {
                                            it.copy(
                                                streamingContent = "",
                                                currentStreamingMessageId = null
                                            )
                                        }
                                    }
                                    // 如果消息未在列表中，保持streamingContent作为备份显示
                                }
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
     * BUG-046-P1-V3-002修复：增强错误处理，确保消息正确删除或更新
     * BUG-048修复：使用updateAiMessageContentAndStatus确保只更新AI类型的消息
     * BUG-048-V4修复：增强状态管理，确保停止后消息不消失，添加重试机制
     */
    fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null

        val messageId = _uiState.value.currentStreamingMessageId
        val currentContent = _uiState.value.streamingContent

        // BUG-048-V4修复：如果没有messageId，说明流式还没开始，直接清空状态返回
        if (messageId == null) {
            _uiState.update {
                it.copy(
                    isStreaming = false,
                    isRegenerating = false,
                    streamingContent = "",
                    currentStreamingMessageId = null,
                    thinkingContent = "",
                    thinkingElapsedMs = 0
                )
            }
            return
        }

        // BUG-047-P0-V5-002修复：先标记为非流式状态，但保持内容显示
        // BUG-048-V4修复：保持streamingContent和currentStreamingMessageId，直到数据库更新成功
        _uiState.update {
            it.copy(
                isStreaming = false,
                isRegenerating = false,
                // 关键：保持 streamingContent 和 currentStreamingMessageId，让UI继续显示
                thinkingContent = "",
                thinkingElapsedMs = 0
            )
        }

        // BUG-048-V4修复：异步更新数据库，使用重试机制确保消息不消失
        viewModelScope.launch {
            try {
                // 即使没有内容也保留消息，显示"[用户已停止生成]"
                val finalContent = if (currentContent.isNotEmpty()) {
                    currentContent + "\n\n[用户已停止生成]"
                } else {
                    "[用户已停止生成]"
                }
                
                // BUG-048修复：使用带类型验证的方法，确保只更新AI消息
                val result = aiAdvisorRepository.updateAiMessageContentAndStatus(
                    messageId,
                    finalContent,
                    SendStatus.CANCELLED
                )
                
                if (result.isSuccess && result.getOrNull() == true) {
                    // BUG-048-V4修复：使用重试机制等待Flow更新，最多等待2秒
                    var retryCount = 0
                    val maxRetries = 4
                    val retryDelay = 500L
                    
                    while (retryCount < maxRetries) {
                        kotlinx.coroutines.delay(retryDelay)
                        
                        // 验证消息已在列表中且有内容
                        val messageInList = _uiState.value.conversations.any { 
                            it.id == messageId && it.content.isNotEmpty() 
                        }
                        
                        if (messageInList) {
                            // 消息已正确显示在列表中，清空流式状态
                            _uiState.update {
                                it.copy(
                                    streamingContent = "",
                                    currentStreamingMessageId = null
                                )
                            }
                            return@launch
                        }
                        
                        retryCount++
                    }
                    
                    // 超时后仍然清空流式状态，但消息应该已经在数据库中了
                    _uiState.update {
                        it.copy(
                            streamingContent = "",
                            currentStreamingMessageId = null
                        )
                    }
                } else if (result.isSuccess && result.getOrNull() == false) {
                    // BUG-048: 消息不是AI类型，可能是时序问题导致的ID错误
                    // 保持streamingContent显示，让用户看到内容，但显示错误提示
                    _uiState.update {
                        it.copy(
                            error = "停止生成失败：消息类型不匹配，请重试"
                        )
                    }
                } else {
                    // BUG-048-V4修复：更新失败时保持内容显示，不清空状态
                    _uiState.update {
                        it.copy(
                            error = "保存消息失败，请重试"
                        )
                    }
                }
            } catch (e: Exception) {
                // BUG-048-V4修复：异常时也保持内容显示，让用户可以看到已生成的内容
                _uiState.update {
                    it.copy(
                        error = "操作失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 重新生成最后一条AI回复
     *
     * BUG-046-P0-V3-002修复：等待删除完成后再重新生成，避免出现两个AI对话框
     * BUG-045-P0-NEW-002修复：不创建新的用户消息，直接重新生成AI回复
     * BUG-048修复：优先使用lastUserInput，避免终止后重新生成时消息角色混淆
     * BUG-048-V3修复：使用时间戳排序确保找到正确的用户消息，添加时间戳约束
     * BUG-048-V4修复：添加relatedUserMessageId三重保障机制，确保应用重启后仍能正确获取用户输入
     * 删除最后一条AI消息，使用相同的用户问题重新生成。
     */
    fun regenerateLastMessage() {
        val conversations = _uiState.value.conversations
        val sessionId = _uiState.value.currentSessionId ?: return
        
        // BUG-048-V3修复：使用时间戳找到最后一条AI消息（包括CANCELLED状态）
        val lastAiMessage = conversations
            .filter { it.messageType == MessageType.AI }
            .maxByOrNull { it.timestamp }
        
        if (lastAiMessage == null) {
            _uiState.update { it.copy(error = "未找到AI消息，无法重新生成") }
            return
        }
        
        // BUG-048-V4修复：三重保障获取用户输入
        val (userInputToUse, relatedUserMessageId) = getUserInputForRegenerate(lastAiMessage, conversations)
        
        // 验证用户输入不为空
        if (userInputToUse.isEmpty()) {
            _uiState.update { it.copy(error = "未找到有效的用户消息，无法重新生成") }
            return
        }

        viewModelScope.launch {
            // BUG-046修复：设置重新生成状态
            _uiState.update { it.copy(isRegenerating = true) }
            
            // 删除最后一条AI消息
            val deleteResult = deleteAdvisorConversationUseCase(lastAiMessage.id)
            
            if (deleteResult.isSuccess) {
                // BUG-048-V3修复：增加延迟时间，确保Flow更新完成
                kotlinx.coroutines.delay(200)
                
                // BUG-048-V4修复：使用验证过的用户输入和关联ID进行重新生成
                regenerateStreaming(userInputToUse, sessionId, relatedUserMessageId)
            } else {
                _uiState.update { 
                    it.copy(
                        isRegenerating = false,
                        error = "删除消息失败，请重试"
                    ) 
                }
            }
        }
    }

    /**
     * BUG-048-V4 + BUG-00059修复: 获取重新生成时的用户输入
     *
     * BUG-00059修复：增强验证逻辑，确保不会错误地使用AI生成的内容
     * 
     * 优先级：
     * 1. 通过relatedUserMessageId查找关联的用户消息（最可靠，持久化）
     * 2. 内存中的lastUserInput（需要验证不是AI内容）
     * 3. 时间戳回退查找（兼容旧数据，需要验证消息类型）
     *
     * @param lastAiMessage 最后一条AI消息
     * @param conversations 当前对话列表
     * @return Pair<用户输入内容, 关联的用户消息ID>
     */
    private fun getUserInputForRegenerate(
        lastAiMessage: AiAdvisorConversation,
        conversations: List<AiAdvisorConversation>
    ): Pair<String, String?> {
        // BUG-00059修复：优先级1 - 通过relatedUserMessageId查找（最可靠）
        val relatedUserMessageId = lastAiMessage.relatedUserMessageId
        if (!relatedUserMessageId.isNullOrEmpty()) {
            val relatedMessage = conversations.find { it.id == relatedUserMessageId }
            // 验证关联的消息确实是USER类型
            if (relatedMessage != null && 
                relatedMessage.messageType == MessageType.USER &&
                relatedMessage.content.isNotEmpty() &&
                !isLikelyAiContent(relatedMessage.content)) {
                return Pair(relatedMessage.content, relatedUserMessageId)
            }
        }
        
        // 优先级2：使用内存中的lastUserInput（需要验证）
        val fromMemory = _uiState.value.lastUserInput
        if (fromMemory.isNotEmpty() && !isLikelyAiContent(fromMemory)) {
            // 尝试找到对应的用户消息ID
            val relatedId = conversations
                .filter { 
                    it.messageType == MessageType.USER && 
                    it.sendStatus == SendStatus.SUCCESS &&
                    it.timestamp < lastAiMessage.timestamp
                }
                .maxByOrNull { it.timestamp }
                ?.id
            return Pair(fromMemory, relatedId)
        }
        
        // 优先级3：时间戳回退查找（兼容旧数据）
        val fallbackMessage = conversations
            .filter { 
                it.messageType == MessageType.USER && 
                it.sendStatus == SendStatus.SUCCESS &&
                it.timestamp < lastAiMessage.timestamp &&
                !isLikelyAiContent(it.content)  // BUG-00059: 验证不是AI内容
            }
            .maxByOrNull { it.timestamp }
        
        return Pair(fallbackMessage?.content ?: "", fallbackMessage?.id)
    }

    /**
     * BUG-00059: 检测内容是否可能是AI生成的
     * 
     * 通过以下特征判断：
     * 1. 包含停止生成标记
     * 2. 内容过长（用户输入通常较短）
     * 3. 包含AI特有的格式标记
     */
    private fun isLikelyAiContent(content: String): Boolean {
        // 检查停止生成标记
        if (content.contains("[用户已停止生成]")) return true
        
        // 检查AI特有的格式标记
        val aiMarkers = listOf(
            "核心策略",
            "## ",  // Markdown标题
            "**",   // Markdown加粗
            "1. ",  // 有序列表
            "- ",   // 无序列表
            "ENFJ", "INFP", "INTJ", "ENTP"  // MBTI类型（AI分析常用）
        )
        val markerCount = aiMarkers.count { content.contains(it) }
        if (markerCount >= 2) return true
        
        // 检查内容长度（用户输入通常不超过200字符）
        if (content.length > 300) return true
        
        return false
    }

    /**
     * 重新生成AI回复（不创建新的用户消息）
     *
     * BUG-045-P0-NEW-002修复：专门用于重新生成场景，跳过用户消息保存
     * BUG-048-V4修复：传递relatedUserMessageId，确保新AI消息关联正确的用户消息
     *
     * @param userMessage 用户消息内容
     * @param sessionId 会话ID
     * @param relatedUserMessageId 关联的用户消息ID（用于新AI消息的关联）
     */
    private fun regenerateStreaming(
        userMessage: String, 
        sessionId: String,
        relatedUserMessageId: String? = null
    ) {
        streamingJob?.cancel()
        streamingJob = null

        _uiState.update {
            it.copy(
                isStreaming = true,
                isSending = false,
                error = null,
                streamingContent = "",
                thinkingContent = "",
                thinkingElapsedMs = 0,
                currentStreamingMessageId = null
            )
        }

        streamingJob = viewModelScope.launch {
            // BUG-048-V4: 使用skipUserMessage参数跳过用户消息保存，并传递relatedUserMessageId
            sendAdvisorMessageStreamingUseCase(
                contactId = contactId, 
                sessionId = sessionId, 
                userMessage = userMessage, 
                skipUserMessage = true,
                relatedUserMessageId = relatedUserMessageId
            )
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
                            // BUG-047修复：与sendMessageStreaming保持一致的完成处理逻辑
                            val completedSessionId = _uiState.value.currentSessionId
                            val completedMessageId = _uiState.value.currentStreamingMessageId
                            
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    isRegenerating = false,
                                    streamingContent = state.fullText,
                                    thinkingContent = "",
                                    thinkingElapsedMs = 0,
                                    lastTokenUsage = state.usage
                                )
                            }
                            
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1200)
                                val currentState = _uiState.value
                                
                                // 验证消息是否已在conversations列表中且有内容
                                val messageInList = currentState.conversations.any { 
                                    it.id == completedMessageId && it.content.isNotEmpty() 
                                }
                                
                                if (currentState.currentSessionId == completedSessionId &&
                                    currentState.currentStreamingMessageId == completedMessageId) {
                                    if (messageInList) {
                                        _uiState.update {
                                            it.copy(
                                                streamingContent = "",
                                                currentStreamingMessageId = null
                                            )
                                        }
                                    }
                                }
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
     * BUG-046修复：取消conversations收集器，避免跨会话数据污染
     */
    fun switchSession(sessionId: String) {
        // BUG-044-P1-005: 先停止当前流式响应
        stopGeneration()
        
        // BUG-046修复：取消conversations收集器
        conversationsJob?.cancel()
        conversationsJob = null

        _uiState.update {
            it.copy(
                currentSessionId = sessionId,
                conversations = emptyList(), // 清空当前对话，等待新会话加载
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
     * BUG-00058: 从导航参数触发创建新会话
     * BUG-00060: 空会话复用功能
     * 
     * 当用户从会话历史页面点击"新建会话"时调用此方法。
     * 与createNewSession的区别是：此方法会先检查是否存在空会话可复用。
     * 
     * 业务规则：
     * - 如果存在空会话（messageCount=0），复用该会话
     * - 如果不存在空会话，创建新会话
     */
    fun createNewSessionFromNavigation() {
        viewModelScope.launch {
            // 先清空当前对话状态
            _uiState.update { currentState ->
                currentState.copy(
                    conversations = emptyList(),
                    currentSessionId = null,
                    streamingContent = "",
                    thinkingContent = "",
                    currentStreamingMessageId = null,
                    isLoading = true
                )
            }
            
            // BUG-00060: 先检查是否存在空会话可复用
            aiAdvisorRepository.getLatestEmptySession(contactId).onSuccess { emptySession ->
                if (emptySession != null) {
                    // 复用空会话
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentSessionId = emptySession.id,
                            isLoading = false
                        )
                    }
                    // 重新加载会话列表以确保UI同步
                    loadSessions(contactId)
                } else {
                    // 创建新会话
                    createAdvisorSessionUseCase(contactId).onSuccess { session ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                sessions = listOf(session) + currentState.sessions,
                                currentSessionId = session.id,
                                isLoading = false
                            )
                        }
                        // 加载新会话的对话（应该是空的）
                        loadConversations(session.id)
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "创建会话失败"
                            )
                        }
                    }
                }
            }.onFailure { error ->
                // 查询失败时降级为创建新会话
                createAdvisorSessionUseCase(contactId).onSuccess { session ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            sessions = listOf(session) + currentState.sessions,
                            currentSessionId = session.id,
                            isLoading = false
                        )
                    }
                    loadConversations(session.id)
                }.onFailure { createError ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = createError.message ?: "创建会话失败"
                        )
                    }
                }
            }
        }
    }

    /**
     * BUG-00061: 根据sessionId加载指定会话
     * 
     * 当用户从会话历史页面点击某个会话时调用此方法。
     * 
     * @param sessionId 要加载的会话ID
     */
    fun loadSessionById(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            aiAdvisorRepository.getSessionById(sessionId).onSuccess { session ->
                if (session != null) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentSessionId = session.id,
                            isLoading = false
                        )
                    }
                    // 加载该会话的对话内容
                    loadConversations(session.id)
                    // 同时加载会话列表以保持UI同步
                    loadSessions(contactId)
                } else {
                    // 会话不存在，显示错误
                    _uiState.update { 
                        it.copy(
                            error = "会话不存在或已被删除",
                            isLoading = false
                        ) 
                    }
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        error = error.message ?: "加载会话失败",
                        isLoading = false
                    ) 
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

    /**
     * BUG-00060: 生成会话标题
     *
     * 根据用户消息生成会话标题，用于会话自动命名功能。
     *
     * 规则：
     * - 取用户消息的前20个字符
     * - 去除换行符，替换为空格
     * - 超过20字符时添加"..."后缀
     *
     * @param userMessage 用户消息内容
     * @return 生成的会话标题
     */
    private fun generateSessionTitle(userMessage: String): String {
        val maxLength = 20
        val cleaned = userMessage.trim().replace("\n", " ").replace("\r", "")
        return if (cleaned.length > maxLength) {
            cleaned.take(maxLength) + "..."
        } else {
            cleaned.ifEmpty { "新对话" }
        }
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
 * @property isRegenerating 是否正在重新生成（BUG-046新增）
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
    val isRegenerating: Boolean = false, // BUG-046新增：是否正在重新生成
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
    val lastTokenUsage: TokenUsage? = null,
    // BUG-048新增：记录最后一次用户输入，用于重新生成时避免消息角色混淆
    val lastUserInput: String = ""
)
