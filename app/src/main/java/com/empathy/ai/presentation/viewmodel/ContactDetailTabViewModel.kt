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
import com.empathy.ai.domain.usecase.EditFactUseCase
import com.empathy.ai.domain.usecase.EditConversationUseCase
import com.empathy.ai.domain.usecase.EditSummaryUseCase
import com.empathy.ai.domain.usecase.EditContactInfoUseCase
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
 * - 处理编辑功能（TD-00012）
 */
@HiltViewModel
class ContactDetailTabViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
    private val getBrainTagsUseCase: GetBrainTagsUseCase,
    private val saveBrainTagUseCase: SaveBrainTagUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val deleteBrainTagUseCase: DeleteBrainTagUseCase,
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    // TD-00012: 编辑功能UseCase
    private val editFactUseCase: EditFactUseCase,
    private val editConversationUseCase: EditConversationUseCase,
    private val editSummaryUseCase: EditSummaryUseCase,
    private val editContactInfoUseCase: EditContactInfoUseCase
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
        // TD-00012: 编辑功能事件
        else if (event is ContactDetailUiEvent.StartEditFact) {
            startEditFact(event.fact)
        } else if (event is ContactDetailUiEvent.ConfirmEditFact) {
            confirmEditFact(event.factId, event.newKey, event.newValue)
        } else if (event is ContactDetailUiEvent.CancelEditFact) {
            cancelEditFact()
        } else if (event is ContactDetailUiEvent.DeleteFactById) {
            deleteFactById(event.factId)
        } else if (event is ContactDetailUiEvent.StartEditSummary) {
            startEditSummary(event.summaryId)
        } else if (event is ContactDetailUiEvent.ConfirmEditSummary) {
            confirmEditSummary(event.summaryId, event.newContent)
        } else if (event is ContactDetailUiEvent.CancelEditSummary) {
            cancelEditSummary()
        } else if (event is ContactDetailUiEvent.DeleteSummary) {
            deleteSummary(event.summaryId)
        } else if (event is ContactDetailUiEvent.StartEditContactInfo) {
            startEditContactInfo()
        } else if (event is ContactDetailUiEvent.ConfirmEditContactInfo) {
            confirmEditContactInfo(event.newName, event.newTargetGoal)
        } else if (event is ContactDetailUiEvent.CancelEditContactInfo) {
            cancelEditContactInfo()
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
     * 
     * 注意：每个 TimelineItem 必须有唯一的 ID，用于 LazyColumn 的 key
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
        // Fact 模型现在有唯一的 id 字段，直接使用即可
        facts.forEach { fact ->
            items.add(
                TimelineItem.UserFact(
                    id = "fact_${fact.id}",
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
     * 修复BUG-00026: 使用Fact的唯一id字段，避免key重复导致崩溃
     */
    private fun addFactToStream(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val contact = currentState.contact ?: return@launch
                
                // 创建新的Fact（自动生成唯一ID）
                val newFact = Fact(
                    key = key,
                    value = value,
                    timestamp = System.currentTimeMillis(),
                    source = FactSource.MANUAL
                )
                
                // 调试日志：记录新创建的Fact ID
                com.empathy.ai.domain.util.DebugLogger.d(
                    "ContactDetailTabVM",
                    "========== 添加事实调试 =========="
                )
                com.empathy.ai.domain.util.DebugLogger.d(
                    "ContactDetailTabVM",
                    "新创建的Fact: id=${newFact.id}, key=${newFact.key}"
                )
                
                // 更新联系人的facts列表
                val updatedFacts = contact.facts + newFact
                
                // 创建更新后的联系人对象
                val updatedContact = contact.copy(facts = updatedFacts)
                
                // 调试日志：保存前的facts列表
                com.empathy.ai.domain.util.DebugLogger.d(
                    "ContactDetailTabVM",
                    "保存前 updatedFacts 数量: ${updatedFacts.size}"
                )
                updatedFacts.forEach { f ->
                    com.empathy.ai.domain.util.DebugLogger.d(
                        "ContactDetailTabVM",
                        "  id=${f.id}, key=${f.key}"
                    )
                }
                
                // 持久化到数据库
                saveProfileUseCase(updatedContact).onSuccess {
                    // 调试日志：保存成功后验证
                    com.empathy.ai.domain.util.DebugLogger.d(
                        "ContactDetailTabVM",
                        "保存成功! 验证数据库中的数据..."
                    )
                    
                    // 创建新的时间线项目（用户添加的事实）
                    // 使用 Fact 的唯一 id 字段确保唯一性
                    val newTimelineItem = TimelineItem.UserFact(
                        id = "fact_${newFact.id}",
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
                    
                    // 调试日志：更新后的内存状态
                    com.empathy.ai.domain.util.DebugLogger.d(
                        "ContactDetailTabVM",
                        "内存状态已更新，新Fact id=${newFact.id}"
                    )
                }.onFailure { error ->
                    com.empathy.ai.domain.util.DebugLogger.e(
                        "ContactDetailTabVM",
                        "保存失败: ${error.message}"
                    )
                    _uiState.update {
                        it.copy(error = "保存失败: ${error.message}")
                    }
                }
                
            } catch (e: Exception) {
                com.empathy.ai.domain.util.DebugLogger.e(
                    "ContactDetailTabVM",
                    "添加事实异常",
                    e
                )
                _uiState.update {
                    it.copy(error = e.message ?: "添加事实失败")
                }
            }
        }
    }

    // ========== TD-00012: 编辑功能方法 ==========

    /**
     * 开始编辑事实
     */
    private fun startEditFact(fact: Fact) {
        _uiState.update {
            it.copy(
                showEditFactDialog = true,
                editingFact = fact
            )
        }
    }

    /**
     * 确认编辑事实
     */
    private fun confirmEditFact(factId: String, newKey: String, newValue: String) {
        if (newKey.isBlank() || newValue.isBlank()) {
            _uiState.update { it.copy(error = "内容不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val contact = currentState.contact ?: return@launch
                val factToEdit = currentState.facts.find { it.id == factId } ?: return@launch

                // 调用UseCase
                editFactUseCase(
                    contactId = contact.id,
                    factId = factId,
                    newKey = newKey,
                    newValue = newValue
                ).onSuccess { result ->
                    when (result) {
                        is com.empathy.ai.domain.model.EditResult.Success -> {
                            // 更新本地状态
                            val updatedFacts = currentState.facts.map { fact ->
                                if (fact.id == factId) {
                                    fact.copyWithEdit(newKey, newValue)
                                } else fact
                            }

                            // 更新时间线项目
                            val updatedTimelineItems = currentState.timelineItems.map { item ->
                                if (item is TimelineItem.UserFact && item.fact.id == factId) {
                                    item.copy(fact = item.fact.copyWithEdit(newKey, newValue))
                                } else item
                            }

                            _uiState.update {
                                it.copy(
                                    facts = updatedFacts,
                                    timelineItems = updatedTimelineItems,
                                    topTags = updatedFacts.sortedByDescending { f -> f.timestamp }.take(5),
                                    showEditFactDialog = false,
                                    editingFact = null,
                                    successMessage = "事实已更新"
                                )
                            }
                        }
                        is com.empathy.ai.domain.model.EditResult.ValidationError -> {
                            _uiState.update { it.copy(error = result.message) }
                        }
                        is com.empathy.ai.domain.model.EditResult.NotFound -> {
                            _uiState.update { it.copy(error = "未找到事实") }
                        }
                        is com.empathy.ai.domain.model.EditResult.NoChanges -> {
                            _uiState.update {
                                it.copy(
                                    showEditFactDialog = false,
                                    editingFact = null,
                                    successMessage = "内容未变化"
                                )
                            }
                        }
                        else -> {
                            _uiState.update { it.copy(error = "更新事实失败") }
                        }
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "更新事实失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "更新事实失败")
                }
            }
        }
    }

    /**
     * 取消编辑事实
     */
    private fun cancelEditFact() {
        _uiState.update {
            it.copy(
                showEditFactDialog = false,
                editingFact = null
            )
        }
    }

    /**
     * 删除事实
     * 
     * 通过更新联系人的facts列表来删除事实
     */
    private fun deleteFactById(factId: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val contact = currentState.contact ?: return@launch

                // 调试日志：删除前的状态
                com.empathy.ai.domain.util.DebugLogger.d(
                    "ContactDetailTabVM",
                    "========== 删除事实调试 =========="
                )
                com.empathy.ai.domain.util.DebugLogger.d(
                    "ContactDetailTabVM",
                    "要删除的factId: $factId"
                )
                com.empathy.ai.domain.util.DebugLogger.d(
                    "ContactDetailTabVM",
                    "内存中facts数量: ${currentState.facts.size}"
                )
                currentState.facts.forEach { f ->
                    com.empathy.ai.domain.util.DebugLogger.d(
                        "ContactDetailTabVM",
                        "  id=${f.id}, key=${f.key}, 匹配=${f.id == factId}"
                    )
                }

                // 从facts列表中移除
                val updatedFacts = currentState.facts.filter { it.id != factId }
                
                com.empathy.ai.domain.util.DebugLogger.d(
                    "ContactDetailTabVM",
                    "过滤后facts数量: ${updatedFacts.size}"
                )
                
                val updatedContact = contact.copy(facts = updatedFacts)

                // 保存到数据库
                saveProfileUseCase(updatedContact).onSuccess {
                    com.empathy.ai.domain.util.DebugLogger.d(
                        "ContactDetailTabVM",
                        "删除成功，已保存到数据库"
                    )
                    
                    // 更新时间线项目
                    val updatedTimelineItems = currentState.timelineItems.filter { item ->
                        !(item is TimelineItem.UserFact && item.fact.id == factId)
                    }

                    _uiState.update {
                        it.copy(
                            contact = updatedContact,
                            facts = updatedFacts,
                            timelineItems = updatedTimelineItems,
                            topTags = updatedFacts.sortedByDescending { f -> f.timestamp }.take(5),
                            latestFact = updatedFacts.maxByOrNull { f -> f.timestamp },
                            showEditFactDialog = false,
                            editingFact = null,
                            successMessage = "事实已删除"
                        )
                    }
                }.onFailure { error ->
                    com.empathy.ai.domain.util.DebugLogger.e(
                        "ContactDetailTabVM",
                        "删除失败: ${error.message}"
                    )
                    _uiState.update {
                        it.copy(error = error.message ?: "删除事实失败")
                    }
                }
            } catch (e: Exception) {
                com.empathy.ai.domain.util.DebugLogger.e(
                    "ContactDetailTabVM",
                    "删除事实异常",
                    e
                )
                _uiState.update {
                    it.copy(error = e.message ?: "删除事实失败")
                }
            }
        }
    }

    /**
     * 开始编辑总结
     */
    private fun startEditSummary(summaryId: Long) {
        val currentState = _uiState.value
        val summary = currentState.summaries.find { it.id == summaryId }

        _uiState.update {
            it.copy(
                showEditSummaryDialog = true,
                editingSummaryId = summaryId,
                editingSummaryContent = summary?.content ?: ""
            )
        }
    }

    /**
     * 确认编辑总结
     */
    private fun confirmEditSummary(summaryId: Long, newContent: String) {
        if (newContent.isBlank()) {
            _uiState.update { it.copy(error = "内容不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val currentState = _uiState.value

                editSummaryUseCase(
                    summaryId = summaryId,
                    newContent = newContent
                ).onSuccess { result ->
                    when (result) {
                        is com.empathy.ai.domain.model.EditResult.Success -> {
                            // 更新本地状态
                            val updatedSummaries = currentState.summaries.map { summary ->
                                if (summary.id == summaryId) {
                                    summary.copyWithEdit(newContent)
                                } else summary
                            }

                            // 更新时间线项目
                            val updatedTimelineItems = currentState.timelineItems.map { item ->
                                if (item is TimelineItem.AiSummary && item.summary.id == summaryId) {
                                    item.copy(summary = item.summary.copyWithEdit(newContent))
                                } else item
                            }

                            _uiState.update {
                                it.copy(
                                    summaries = updatedSummaries,
                                    timelineItems = updatedTimelineItems,
                                    showEditSummaryDialog = false,
                                    editingSummaryId = null,
                                    editingSummaryContent = "",
                                    successMessage = "总结已更新"
                                )
                            }
                        }
                        is com.empathy.ai.domain.model.EditResult.ValidationError -> {
                            _uiState.update { it.copy(error = result.message) }
                        }
                        is com.empathy.ai.domain.model.EditResult.NotFound -> {
                            _uiState.update { it.copy(error = "未找到总结") }
                        }
                        is com.empathy.ai.domain.model.EditResult.NoChanges -> {
                            _uiState.update {
                                it.copy(
                                    showEditSummaryDialog = false,
                                    editingSummaryId = null,
                                    editingSummaryContent = "",
                                    successMessage = "内容未变化"
                                )
                            }
                        }
                        else -> {
                            _uiState.update { it.copy(error = "更新总结失败") }
                        }
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "更新总结失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "更新总结失败")
                }
            }
        }
    }

    /**
     * 取消编辑总结
     */
    private fun cancelEditSummary() {
        _uiState.update {
            it.copy(
                showEditSummaryDialog = false,
                editingSummaryId = null,
                editingSummaryContent = ""
            )
        }
    }

    /**
     * 删除总结
     */
    private fun deleteSummary(summaryId: Long) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value

                dailySummaryRepository.deleteSummary(summaryId).onSuccess {
                    // 从本地状态移除
                    val updatedSummaries = currentState.summaries.filter { it.id != summaryId }
                    val updatedTimelineItems = currentState.timelineItems.filter { item ->
                        !(item is TimelineItem.AiSummary && item.summary.id == summaryId)
                    }

                    _uiState.update {
                        it.copy(
                            summaries = updatedSummaries,
                            timelineItems = updatedTimelineItems,
                            summaryCount = updatedSummaries.size,
                            showEditSummaryDialog = false,
                            editingSummaryId = null,
                            successMessage = "总结已删除"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "删除总结失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "删除总结失败")
                }
            }
        }
    }

    /**
     * 开始编辑联系人信息
     */
    private fun startEditContactInfo() {
        _uiState.update {
            it.copy(showEditContactInfoDialog = true)
        }
    }

    /**
     * 确认编辑联系人信息
     */
    private fun confirmEditContactInfo(newName: String, newTargetGoal: String) {
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "姓名不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val contact = currentState.contact ?: return@launch

                // 分别编辑姓名和目标
                var hasError = false
                var errorMessage = ""

                // 编辑姓名
                if (newName != contact.name) {
                    editContactInfoUseCase.editName(contact.id, newName).onSuccess { result ->
                        when (result) {
                            is com.empathy.ai.domain.model.EditResult.ValidationError -> {
                                hasError = true
                                errorMessage = result.message
                            }
                            is com.empathy.ai.domain.model.EditResult.NotFound -> {
                                hasError = true
                                errorMessage = "未找到联系人"
                            }
                            else -> { /* Success or NoChanges */ }
                        }
                    }.onFailure { error ->
                        hasError = true
                        errorMessage = error.message ?: "更新姓名失败"
                    }
                }

                if (hasError) {
                    _uiState.update { it.copy(error = errorMessage) }
                    return@launch
                }

                // 编辑目标
                if (newTargetGoal != contact.targetGoal) {
                    editContactInfoUseCase.editGoal(contact.id, newTargetGoal).onSuccess { result ->
                        when (result) {
                            is com.empathy.ai.domain.model.EditResult.ValidationError -> {
                                hasError = true
                                errorMessage = result.message
                            }
                            is com.empathy.ai.domain.model.EditResult.NotFound -> {
                                hasError = true
                                errorMessage = "未找到联系人"
                            }
                            else -> { /* Success or NoChanges */ }
                        }
                    }.onFailure { error ->
                        hasError = true
                        errorMessage = error.message ?: "更新目标失败"
                    }
                }

                if (hasError) {
                    _uiState.update { it.copy(error = errorMessage) }
                    return@launch
                }

                // 更新本地状态
                val updatedContact = contact.copy(
                    name = newName,
                    targetGoal = newTargetGoal
                )

                _uiState.update {
                    it.copy(
                        contact = updatedContact,
                        showEditContactInfoDialog = false,
                        successMessage = "联系人信息已更新"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "更新联系人信息失败")
                }
            }
        }
    }

    /**
     * 取消编辑联系人信息
     */
    private fun cancelEditContactInfo() {
        _uiState.update {
            it.copy(showEditContactInfoDialog = false)
        }
    }
}
