package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.repository.TopicRepository
import com.empathy.ai.domain.usecase.ClearTopicUseCase
import com.empathy.ai.domain.usecase.GetTopicUseCase
import com.empathy.ai.domain.usecase.SetTopicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主题管理 ViewModel
 *
 * ## 业务职责
 * 管理讨论主题的完整生命周期：
 * - 主题列表加载和搜索
 * - 主题创建和编辑
 * - 主题历史记录管理
 * - 主题关联设置（提醒、优先级）
 * - 多联系人主题隔离
 *
 * ## 核心数据流
 * ```
 * LoadTopics → Select/Create → Edit → Save → Notify
 * ```
 *
 * ## 关键业务概念
 * - **ConversationTopic**: 讨论话题（如"生日礼物"、"约会安排"）
 * - **TopicHistory**: 主题历史记录（快速复用历史主题）
 * - **TopicSetting**: 主题相关设置（提醒时间、重要程度）
 *
 * ## 设计决策
 * - **按时间分组**: 历史主题按日期分组展示
 * - **快速复用**: 支持从历史记录快速创建新主题
 * - **状态追踪**: 标记已完成/未完成/进行中
 * - **动态联系人**: 支持通过loadTopic()动态切换观察的联系人
 *
 * ## 主题状态
 * - PENDING: 待讨论
 * - IN_PROGRESS: 讨论中
 * - COMPLETED: 已完成
 * - ARCHIVED: 已归档
 *
 * ## 数据流说明
 * - init中根据contactId初始化观察和历史加载
 * - loadTopic()支持外部触发重新加载指定联系人的主题
 * - observeTopicForContact()使用Flow持续监听主题变化
 *
 * @see com.empathy.ai.presentation.ui.screen.TopicScreen
 */
@HiltViewModel
class TopicViewModel @Inject constructor(
    private val setTopicUseCase: SetTopicUseCase,
    private val getTopicUseCase: GetTopicUseCase,
    private val clearTopicUseCase: ClearTopicUseCase,
    private val topicRepository: TopicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contactId: String = savedStateHandle.get<String>("contactId") ?: ""
    
    // 当前操作的联系人ID（可能通过loadTopic动态设置）
    private var currentContactId: String = contactId

    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()

    init {
        if (contactId.isNotBlank()) {
            observeTopic()
            loadTopicHistory()
        }
    }

    /**
     * 使用指定的联系人ID初始化
     */
    fun initWithContactId(newContactId: String) {
        if (newContactId.isNotBlank() && newContactId != contactId) {
            viewModelScope.launch {
                observeTopicForContact(newContactId)
                loadTopicHistoryForContact(newContactId)
            }
        }
    }

    /**
     * 加载指定联系人的主题数据
     * 
     * 用于从外部触发主题数据加载
     */
    fun loadTopic(targetContactId: String) {
        if (targetContactId.isBlank()) return
        currentContactId = targetContactId
        viewModelScope.launch {
            observeTopicForContact(targetContactId)
            loadTopicHistoryForContact(targetContactId)
        }
    }

    /**
     * 观察当前主题变化
     */
    private fun observeTopic() {
        observeTopicForContact(contactId)
    }

    private fun observeTopicForContact(targetContactId: String) {
        viewModelScope.launch {
            getTopicUseCase.observe(targetContactId)
                .distinctUntilChanged()
                .collect { topic ->
                    _uiState.update { it.copy(currentTopic = topic) }
                }
        }
    }

    /**
     * 加载主题历史
     */
    private fun loadTopicHistory() {
        loadTopicHistoryForContact(contactId)
    }

    private fun loadTopicHistoryForContact(targetContactId: String) {
        viewModelScope.launch {
            val history = topicRepository.getTopicHistory(targetContactId, 10)
            _uiState.update { it.copy(topicHistory = history) }
        }
    }

    /**
     * 处理UI事件
     */
    fun onEvent(event: TopicUiEvent) {
        when (event) {
            is TopicUiEvent.ShowSettingDialog -> showSettingDialog()
            is TopicUiEvent.HideSettingDialog -> hideSettingDialog()
            is TopicUiEvent.UpdateInput -> updateInput(event.content)
            is TopicUiEvent.SaveTopic -> saveTopic()
            is TopicUiEvent.ClearTopic -> clearTopic()
            is TopicUiEvent.SelectFromHistory -> selectFromHistory(event.topic)
            is TopicUiEvent.ClearError -> clearError()
            is TopicUiEvent.ClearSaveSuccess -> clearSaveSuccess()
        }
    }

    private fun showSettingDialog() {
        val currentContent = _uiState.value.currentTopic?.content ?: ""
        _uiState.update {
            it.copy(
                showSettingDialog = true,
                inputContent = currentContent
            )
        }
    }

    private fun hideSettingDialog() {
        _uiState.update {
            it.copy(
                showSettingDialog = false,
                inputContent = "",
                errorMessage = null
            )
        }
    }

    private fun updateInput(content: String) {
        _uiState.update { it.copy(inputContent = content) }
    }

    private fun saveTopic() {
        val content = _uiState.value.inputContent.trim()
        if (content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "主题内容不能为空") }
            return
        }
        
        if (currentContactId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "联系人ID无效") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            setTopicUseCase(currentContactId, content)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showSettingDialog = false,
                            inputContent = "",
                            saveSuccess = true
                        )
                    }
                    loadTopicHistoryForContact(currentContactId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "保存失败"
                        )
                    }
                }
        }
    }

    private fun clearTopic() {
        if (currentContactId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "联系人ID无效") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            clearTopicUseCase(currentContactId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showSettingDialog = false,
                            inputContent = ""
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "清除失败"
                        )
                    }
                }
        }
    }

    private fun selectFromHistory(topic: ConversationTopic) {
        _uiState.update { it.copy(inputContent = topic.content) }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
