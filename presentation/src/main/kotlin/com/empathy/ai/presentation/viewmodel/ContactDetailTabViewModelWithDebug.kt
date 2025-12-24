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
import com.empathy.ai.domain.usecase.GroupFactsByCategoryUseCase
import com.empathy.ai.domain.usecase.BatchDeleteFactsUseCase
import com.empathy.ai.domain.usecase.BatchMoveFactsUseCase
import com.empathy.ai.domain.util.FactSearchFilter
import com.empathy.ai.domain.model.EditModeState
import com.empathy.ai.domain.model.PersonaSearchState
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.DetailTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 联系人详情标签页ViewModel - 调试版本
 *
 * 专门用于新的四标签页UI（概览、事实流、标签画像、资料库）
 *
 * 添加了批量删除操作的调试日志
 */
@HiltViewModel
class ContactDetailTabViewModelWithDebug @Inject constructor(
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
    private val editContactInfoUseCase: EditContactInfoUseCase,
    // TD-00014: 标签画像V2 UseCase
    private val groupFactsByCategoryUseCase: GroupFactsByCategoryUseCase,
    private val batchDeleteFactsUseCase: BatchDeleteFactsUseCase,
    private val batchMoveFactsUseCase: BatchMoveFactsUseCase,
    private val factSearchFilter: FactSearchFilter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()
    
    /** 搜索防抖Job */
    private var searchJob: Job? = null
    
    /** 搜索防抖延迟（毫秒） */
    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    /**
     * 统一事件处理入口
     */
    fun onEvent(event: ContactDetailUiEvent) {
        // 使用if-else链避免when表达式的类型推断问题
        if (event is ContactDetailUiEvent.ConfirmBatchDelete) {
            confirmBatchDeleteWithDebug()
        }
        // 其他事件处理...
    }

    /**
     * 加载联系人详情数据
     */
    fun loadContactDetail(contactId: String) {
        BatchDeleteDebugLogger.logLoadContactDetail(contactId)
        
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
                    
                    // 构建时间线
                    val timelineItems = buildTimelineItems(
                        conversations, 
                        summaries, 
                        contact?.facts ?: emptyList()
                    )
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contact = contact,
                            summaries = summaries,
                            facts = contact?.facts ?: emptyList(),
                            timelineItems = timelineItems,
                            conversationCount = conversations.size,
                            summaryCount = summaries.size
                        )
                    }
                    
                    // TD-00014: 自动启用PersonaTabV2并刷新分类数据
                    val facts = contact?.facts ?: emptyList()
                    val categories = refreshCategories(facts)
                    val availableCategories = categories.map { it.key }
                    
                    _uiState.update {
                        it.copy(
                            usePersonaTabV2 = true,
                            factCategories = categories,
                            availableCategories = availableCategories
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
     * 确认批量删除 - 带调试日志
     */
    private fun confirmBatchDeleteWithDebug() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val contact = currentState.contact ?: return@launch
                val factIds = currentState.editModeState.selectedFactIds.toList()
                
                if (factIds.isEmpty()) return@launch
                
                // 记录批量删除操作开始
                BatchDeleteDebugLogger.logBatchDeleteStart(contact.id, factIds)
                
                batchDeleteFactsUseCase(contact.id, factIds).onSuccess { deletedCount ->
                    // 记录批量删除成功
                    BatchDeleteDebugLogger.logBatchDeleteSuccess(deletedCount)
                    
                    // 刷新数据
                    loadContactDetail(contact.id)
                    
                    _uiState.update {
                        it.copy(
                            editModeState = EditModeState(),
                            successMessage = "已删除 $deletedCount 个标签"
                        )
                    }
                }.onFailure { error ->
                    // 记录批量删除失败
                    BatchDeleteDebugLogger.logBatchDeleteFailure(error.message ?: "删除失败")
                    
                    _uiState.update {
                        it.copy(
                            editModeState = it.editModeState.hideDeleteConfirmDialog(),
                            error = error.message ?: "删除失败"
                        )
                    }
                }
            } catch (e: Exception) {
                BatchDeleteDebugLogger.logBatchDeleteFailure(e.message ?: "删除失败")
                
                _uiState.update {
                    it.copy(
                        editModeState = it.editModeState.hideDeleteConfirmDialog(),
                        error = e.message ?: "删除失败"
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
     * 刷新分类列表
     */
    private fun refreshCategories(facts: List<Fact>): List<com.empathy.ai.domain.model.FactCategory> {
        val isDarkMode = false
        return groupFactsByCategoryUseCase(facts, isDarkMode)
    }
}