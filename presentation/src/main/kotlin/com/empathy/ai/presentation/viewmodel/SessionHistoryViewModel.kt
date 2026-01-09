package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 会话历史页面ViewModel
 *
 * ## 业务职责
 * 管理会话历史页面的状态和业务逻辑：
 * - 加载当前联系人的所有历史会话
 * - 支持删除会话
 * - 支持重命名会话（BUG-00060新增）
 * - 支持置顶/取消置顶会话（BUG-00060新增）
 * - 显示联系人名称
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 * - FD-00029: AI军师UI架构优化功能设计
 * - BUG-00060: 会话管理增强需求
 *
 * ## 数据流
 * ```
 * SessionHistoryScreen
 *     ↓ (hiltViewModel注入)
 * SessionHistoryViewModel
 *     ↓ (调用Repository)
 * AiAdvisorRepository.getSessions(contactId)
 *     ↓
 * 返回 List<AiAdvisorSession>
 * ```
 *
 * @see com.empathy.ai.presentation.ui.screen.advisor.SessionHistoryScreen
 */
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val contactRepository: ContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 从路由参数获取联系人ID */
    private val contactId: String = savedStateHandle[NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow(SessionHistoryUiState())
    val uiState: StateFlow<SessionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadContactInfo()
        loadSessions()
    }

    /**
     * 加载联系人信息
     *
     * 用于显示"与 {联系人名} 的对话"分组标题
     */
    private fun loadContactInfo() {
        viewModelScope.launch {
            contactRepository.getProfile(contactId)
                .onSuccess { contact ->
                    _uiState.update { it.copy(contactName = contact?.name ?: "未知联系人") }
                }
                .onFailure {
                    _uiState.update { it.copy(contactName = "未知联系人") }
                }
        }
    }

    /**
     * 加载会话列表
     *
     * 业务规则 (PRD-00029/R-029-10):
     * - 显示当前联系人的所有历史会话
     * - 按置顶状态和更新时间排序（BUG-00060）
     */
    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            aiAdvisorRepository.getSessions(contactId)
                .onSuccess { sessions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessions = sessions,
                            isEmpty = sessions.isEmpty()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                }
        }
    }

    /**
     * 删除会话
     *
     * 业务规则 (PRD-00029/R-029-15):
     * - 支持左滑删除会话（P1优先级）
     * - 删除后自动刷新列表
     *
     * @param sessionId 要删除的会话ID
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            aiAdvisorRepository.deleteSession(sessionId)
                .onSuccess {
                    loadSessions() // 重新加载列表
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "删除失败") }
                }
        }
    }

    /**
     * BUG-00060: 重命名会话
     *
     * 业务规则:
     * - 支持手动修改会话名称
     * - 重命名后自动刷新列表
     *
     * @param sessionId 会话ID
     * @param newTitle 新标题
     */
    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) {
            _uiState.update { it.copy(error = "会话名称不能为空") }
            return
        }
        
        viewModelScope.launch {
            aiAdvisorRepository.updateSessionTitle(sessionId, newTitle.trim())
                .onSuccess {
                    loadSessions() // 重新加载列表
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "重命名失败") }
                }
        }
    }

    /**
     * BUG-00060: 切换会话置顶状态
     *
     * 业务规则:
     * - 置顶会话在列表中优先显示
     * - 多个置顶会话按更新时间排序
     * - 切换后自动刷新列表
     *
     * @param sessionId 会话ID
     * @param isPinned 是否置顶
     */
    fun togglePinSession(sessionId: String, isPinned: Boolean) {
        viewModelScope.launch {
            aiAdvisorRepository.updateSessionPinned(sessionId, isPinned)
                .onSuccess {
                    loadSessions() // 重新加载列表
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "操作失败") }
                }
        }
    }

    /**
     * 显示重命名对话框
     *
     * @param session 要重命名的会话
     */
    fun showRenameDialog(session: AiAdvisorSession) {
        _uiState.update { 
            it.copy(
                showRenameDialog = true,
                sessionToRename = session
            ) 
        }
    }

    /**
     * 隐藏重命名对话框
     */
    fun hideRenameDialog() {
        _uiState.update { 
            it.copy(
                showRenameDialog = false,
                sessionToRename = null
            ) 
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 获取当前联系人ID
     */
    fun getContactId(): String = contactId
}

/**
 * 会话历史UI状态
 *
 * @property isLoading 是否正在加载
 * @property contactName 联系人名称，用于显示分组标题
 * @property sessions 会话列表
 * @property isEmpty 是否为空（无历史会话）
 * @property error 错误信息
 * @property showRenameDialog 是否显示重命名对话框（BUG-00060新增）
 * @property sessionToRename 待重命名的会话（BUG-00060新增）
 */
data class SessionHistoryUiState(
    val isLoading: Boolean = false,
    val contactName: String = "",
    val sessions: List<AiAdvisorSession> = emptyList(),
    val isEmpty: Boolean = false,
    val error: String? = null,
    val showRenameDialog: Boolean = false,
    val sessionToRename: AiAdvisorSession? = null
)
