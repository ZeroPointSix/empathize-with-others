package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.DetailTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 联系人详情标签页ViewModel
 *
 * 专门用于新的四标签页UI（概览、事实流、标签画像、资料库）
 *
 * 职责：
 * - 管理四个标签页的数据状态
 * - 处理标签页切换
 * - 构建时间线数据
 * - 处理标签确认/驳回
 */
@HiltViewModel
class ContactDetailTabViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
    private val getBrainTagsUseCase: GetBrainTagsUseCase,
    private val saveBrainTagUseCase: SaveBrainTagUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val deleteBrainTagUseCase: DeleteBrainTagUseCase,
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    /**
     * 统一事件处理入口
     *
     * 注意：此ViewModel只处理Tab页面相关的事件
     * 其他事件由ContactDetailViewModel处理
     */
    fun onEvent(event: ContactDetailUiEvent) {
        // 使用if-else链避免when表达式的类型推断问题
        if (event is ContactDetailUiEvent.SwitchTab) {
            switchTab(event.tab)
        } else if (event is ContactDetailUiEvent.SwitchViewMode) {
            switchViewMode(event.mode)
        } else if (event is ContactDetailUiEvent.ToggleFilter) {
            toggleFilter(event.filter)
        } else if (event is ContactDetailUiEvent.ConfirmTag) {
            confirmTag(event.factId)
        } else if (event is ContactDetailUiEvent.RejectTag) {
            rejectTag(event.factId)
        } else if (event is ContactDetailUiEvent.RefreshData) {
            refreshData()
        } else if (event is ContactDetailUiEvent.ClearError) {
            clearError()
        } else if (event is ContactDetailUiEvent.ClearSuccessMessage) {
            clearSuccessMessage()
        }
        // 对话记录管理事件
        else if (event is ContactDetailUiEvent.EditConversation) {
            editConversation(event.logId, event.newContent)
        } else if (event is ContactDetailUiEvent.DeleteConversation) {
            deleteConversation(event.logId)
        } else if (event is ContactDetailUiEvent.ShowEditConversationDialog) {
            showEditConversationDialog()
        } else if (event is ContactDetailUiEvent.HideEditConversationDialog) {
            hideEditConversationDialog()
        } else if (event is ContactDetailUiEvent.SelectConversation) {
            selectConversation(event.logId)
        }
        // 事实流添加事实事件
        else if (event is ContactDetailUiEvent.ShowAddFactToStreamDialog) {
            showAddFactToStreamDialog()
        } else if (event is ContactDetailUiEvent.HideAddFactToStreamDialog) {
            hideAddFactToStreamDialog()
        } else if (event is ContactDetailUiEvent.AddFactToStream) {
            addFactToStream(event.key, event.value)
        }
        // 其他事件不在此ViewModel处理
    }

    /**
     * 加载联系人详情数据
     */
    fun loadContactDetail(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 并行加载数据
                val contactResult = getContactUseCase(contactId)
                
                contactResult.onSuccess { contact ->
                    // 加载每日总结
                    val summariesResult = dailySummaryRepository.getSummariesByContact(contactId)
                    val summaries = summariesResult.getOrDefault(emptyList())
                    
                    // 加载对话记录
                    val conversationsResult = conversationRepository.getConversationsByContact(contactId)
                    val conversations = conversationsResult.getOrDefault(emptyList())
                    
                    // 构建时间线（使用对话记录、总结数据和用户事实）
                    val timelineItems = buildTimelineItems(
                        conversations, 
                        summaries, 
                        contact?.facts ?: emptyList()
                    )
                    
                    // 计算相识天数（使用当前时间作为默认值）
                    val daysSinceFirstMet = 0 // TODO: 需要添加createdAt字段到ContactProfile
                    
                    // 获取最近的标签（按时间戳排序）
                    val topTags = contact?.facts
                        ?.sortedByDescending { it.timestamp }
                        ?.take(5)
                        ?: emptyList()
                    
                    // 获取最新事实
                    val latestFact = contact?.facts?.maxByOrNull { it.timestamp }
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contact = contact,
                            summaries = summaries,
                            facts = contact?.facts ?: emptyList(),
                            timelineItems = timelineItems,
                            topTags = topTags,
                            latestFact = latestFact,
                            daysSinceFirstMet = daysSinceFirstMet,
                            conversationCount = conversations.size,
                            summaryCount = summaries.size
                        )
                    }
                    
                    // 加载标签
                    loadBrainTags(contactId)
                    
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载联系人失败"
                        )
                    }
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
     * 加载标签数据
     */
    private fun loadBrainTags(contactId: String) {
        viewModelScope.launch {
            try {
                getBrainTagsUseCase(contactId).collect { tags ->
                    val confirmedCount = tags.count { it.isConfirmed }
                    val guessedCount = tags.count { !it.isConfirmed }
                    
                    _uiState.update {
                        it.copy(
                            confirmedTagsCount = confirmedCount,
                            guessedTagsCount = guessedCount
                        )
                    }
                }
            } catch (e: Exception) {
                // 标签加载失败不影响主流程
            }
        }
    }

    /**
     * 构建时间线项目
     *
     * 包含对话记录、AI总结和用户添加的事实
     */
    private fun buildTimelineItems(
        conversations: List<ConversationLog>,
        summaries: List<DailySummary>,
        facts: List<Fact> = emptyList()
    ): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()
        
        // 添加对话记录
        conversations.forEach { log ->
            items.add(
                TimelineItem.Conversation(
                    id = "conv_${log.id}",
                    timestamp = log.timestamp,
                    emotionType = detectEmotion(log.userInput),
                    log = log
                )
            )
        }
        
        // 添加AI总结
        summaries.forEach { summary ->
            // 将日期字符串转换为时间戳
            val timestamp = try {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .parse(summary.summaryDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            items.add(
                TimelineItem.AiSummary(
                    id = "summary_${summary.id}",
                    timestamp = timestamp,
                    emotionType = EmotionType.NEUTRAL,
                    summary = summary
                )
            )
        }
        
        // 添加用户手动添加的事实
        facts.forEach { fact ->
            items.add(
                TimelineItem.UserFact(
                    id = "fact_${fact.timestamp}_${fact.key.hashCode()}",
                    timestamp = fact.timestamp,
                    emotionType = EmotionType.NEUTRAL,
                    fact = fact
                )
            )
        }
        
        // 按时间倒序排列
        return items.sortedByDescending { it.timestamp }
    }

    /**
     * 检测文本情绪
     */
    private fun detectEmotion(text: String): EmotionType {
        return EmotionType.fromText(text)
    }

    /**
     * 计算相识天数
     */
    private fun calculateDaysSinceFirstMet(createdAt: Long): Int {
        val now = System.currentTimeMillis()
        val diff = now - createdAt
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    /**
     * 切换标签页
     */
    private fun switchTab(tab: DetailTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    /**
     * 切换视图模式
     */
    private fun switchViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    /**
     * 切换筛选条件
     */
    private fun toggleFilter(filter: FilterType) {
        _uiState.update { currentState ->
            val newFilters = if (filter in currentState.selectedFilters) {
                currentState.selectedFilters - filter
            } else {
                currentState.selectedFilters + filter
            }
            currentState.copy(selectedFilters = newFilters)
        }
    }

    /**
     * 确认AI推测标签
     */
    private fun confirmTag(factId: Long) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val factToConfirm = currentState.facts.find { it.timestamp == factId }
                
                if (factToConfirm != null) {
                    val confirmedFact = factToConfirm.copy(
                        source = FactSource.MANUAL
                    )
                    
                    val newFacts = currentState.facts.map {
                        if (it.timestamp == factId) confirmedFact else it
                    }
                    
                    _uiState.update {
                        it.copy(
                            facts = newFacts,
                            successMessage = "标签已确认"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "确认标签失败")
                }
            }
        }
    }

    /**
     * 驳回AI推测标签
     */
    private fun rejectTag(factId: Long) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val newFacts = currentState.facts.filter { it.timestamp != factId }
                
                _uiState.update {
                    it.copy(
                        facts = newFacts,
                        successMessage = "标签已驳回"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "驳回标签失败")
                }
            }
        }
    }

    /**
     * 刷新数据
     */
    private fun refreshData() {
        val contactId = _uiState.value.contact?.id ?: return
        _uiState.update { it.copy(isRefreshing = true) }
        loadContactDetail(contactId)
        _uiState.update { it.copy(isRefreshing = false) }
    }

    /**
     * 清除错误信息
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 清除成功提示
     */
    private fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    // ========== 对话记录管理方法 ==========

    /**
     * 编辑对话记录
     */
    private fun editConversation(logId: Long, newContent: String) {
        if (newContent.isBlank()) return
        
        viewModelScope.launch {
            try {
                conversationRepository.updateUserInput(logId, newContent).onSuccess {
                    _uiState.update {
                        it.copy(
                            showEditConversationDialog = false,
                            selectedConversationId = null,
                            editingConversationContent = "",
                            successMessage = "对话已更新"
                        )
                    }
                    // 刷新数据
                    _uiState.value.contact?.id?.let { contactId ->
                        loadContactDetail(contactId)
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "更新对话失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "更新对话失败")
                }
            }
        }
    }

    /**
     * 删除对话记录
     */
    private fun deleteConversation(logId: Long) {
        viewModelScope.launch {
            try {
                conversationRepository.deleteConversation(logId).onSuccess {
                    _uiState.update {
                        it.copy(
                            showEditConversationDialog = false,
                            selectedConversationId = null,
                            successMessage = "对话已删除"
                        )
                    }
                    // 刷新数据
                    _uiState.value.contact?.id?.let { contactId ->
                        loadContactDetail(contactId)
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "删除对话失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "删除对话失败")
                }
            }
        }
    }

    /**
     * 显示编辑对话对话框
     */
    private fun showEditConversationDialog() {
        _uiState.update { it.copy(showEditConversationDialog = true) }
    }

    /**
     * 隐藏编辑对话对话框
     */
    private fun hideEditConversationDialog() {
        _uiState.update {
            it.copy(
                showEditConversationDialog = false,
                selectedConversationId = null,
                editingConversationContent = ""
            )
        }
    }

    /**
     * 选中对话记录
     */
    private fun selectConversation(logId: Long) {
        val currentState = _uiState.value
        val conversation = currentState.timelineItems
            .filterIsInstance<TimelineItem.Conversation>()
            .find { it.log.id == logId }
        
        _uiState.update {
            it.copy(
                selectedConversationId = logId,
                editingConversationContent = conversation?.log?.userInput ?: "",
                showEditConversationDialog = true
            )
        }
    }

    // ========== 事实流添加事实方法 ==========

    /**
     * 显示添加事实对话框
     */
    private fun showAddFactToStreamDialog() {
        _uiState.update { it.copy(showAddFactToStreamDialog = true) }
    }

    /**
     * 隐藏添加事实对话框
     */
    private fun hideAddFactToStreamDialog() {
        _uiState.update { it.copy(showAddFactToStreamDialog = false) }
    }

    /**
     * 添加事实到事实流
     * 
     * 修复BUG-00003: 添加持久化逻辑，确保事实保存到数据库
     * 修复BUG-00006: 使用增量更新，避免重新加载导致AI对话丢失
     */
    private fun addFactToStream(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val contact = currentState.contact ?: return@launch
                
                // 创建新的Fact
                val newFact = Fact(
                    key = key,
                    value = value,
                    timestamp = System.currentTimeMillis(),
                    source = FactSource.MANUAL
                )
                
                // 更新联系人的facts列表
                val updatedFacts = contact.facts + newFact
                
                // 创建更新后的联系人对象
                val updatedContact = contact.copy(facts = updatedFacts)
                
                // 持久化到数据库
                saveProfileUseCase(updatedContact).onSuccess {
                    // 创建新的时间线项目（用户添加的事实）
                    val newTimelineItem = TimelineItem.UserFact(
                        id = "fact_${newFact.timestamp}_${newFact.key.hashCode()}",
                        timestamp = newFact.timestamp,
                        emotionType = EmotionType.NEUTRAL,
                        fact = newFact
                    )
                    
                    // 增量更新状态，保留现有的 timelineItems（关键修复！）
                    _uiState.update {
                        it.copy(
                            contact = updatedContact,
                            facts = updatedFacts,
                            topTags = updatedFacts.sortedByDescending { f -> f.timestamp }.take(5),
                            latestFact = newFact,
                            timelineItems = (it.timelineItems + newTimelineItem)
                                .sortedByDescending { item -> item.timestamp },
                            showAddFactToStreamDialog = false,
                            successMessage = "事实已添加"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = "保存失败: ${error.message}")
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "添加事实失败")
                }
            }
        }
    }
}
