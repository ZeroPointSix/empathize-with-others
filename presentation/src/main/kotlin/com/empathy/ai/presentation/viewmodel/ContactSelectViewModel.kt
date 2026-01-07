package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import com.empathy.ai.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 联系人选择页面ViewModel
 *
 * ## 业务职责
 * 管理联系人选择页面的状态和业务逻辑：
 * - 加载所有联系人列表
 * - 支持联系人搜索（P2优先级）
 * - 选择联系人并保存到偏好设置
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 * - FD-00029: AI军师UI架构优化功能设计
 *
 * ## 数据流
 * ```
 * ContactSelectScreen
 *     ↓ (hiltViewModel注入)
 * ContactSelectViewModel
 *     ↓ (调用Repository)
 * ContactRepository.getAllProfiles()
 *     ↓
 * 返回 Flow<List<ContactProfile>>
 * ```
 *
 * @see com.empathy.ai.presentation.ui.screen.advisor.ContactSelectScreen
 */
@HiltViewModel
class ContactSelectViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val aiAdvisorPreferences: AiAdvisorPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactSelectUiState())
    val uiState: StateFlow<ContactSelectUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    /**
     * 加载联系人列表
     *
     * 业务规则 (PRD-00029/R-029-05):
     * - 显示所有联系人
     * - 按更新时间倒序排列
     */
    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            contactRepository.getAllProfiles()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载联系人失败"
                        )
                    }
                }
                .collect { contacts ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contacts = contacts,
                            filteredContacts = filterContacts(contacts, it.searchQuery),
                            isEmpty = contacts.isEmpty()
                        )
                    }
                }
        }
    }

    /**
     * 选择联系人
     *
     * 业务规则 (PRD-00029/US-002):
     * - 保存联系人ID到偏好设置
     * - 触发导航到对话界面
     *
     * @param contactId 选中的联系人ID
     */
    fun selectContact(contactId: String) {
        aiAdvisorPreferences.setLastContactId(contactId)
        _uiState.update { it.copy(selectedContactId = contactId) }
    }

    /**
     * 搜索联系人
     *
     * 业务规则 (PRD-00029/TD-029-02):
     * - P2优先级功能
     * - 按姓名模糊匹配
     *
     * @param query 搜索关键词
     */
    fun searchContacts(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredContacts = filterContacts(state.contacts, query)
            )
        }
    }

    /**
     * 过滤联系人列表
     */
    private fun filterContacts(contacts: List<ContactProfile>, query: String): List<ContactProfile> {
        if (query.isBlank()) return contacts
        return contacts.filter { contact ->
            contact.name.contains(query, ignoreCase = true)
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 重置选择状态
     * 导航完成后调用
     */
    fun resetSelection() {
        _uiState.update { it.copy(selectedContactId = null) }
    }
}

/**
 * 联系人选择UI状态
 *
 * @property isLoading 是否正在加载
 * @property contacts 完整联系人列表
 * @property filteredContacts 过滤后的联系人列表（搜索结果）
 * @property isEmpty 是否为空（无联系人）
 * @property searchQuery 搜索关键词
 * @property selectedContactId 选中的联系人ID，非空时触发导航
 * @property error 错误信息
 */
data class ContactSelectUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactProfile> = emptyList(),
    val filteredContacts: List<ContactProfile> = emptyList(),
    val isEmpty: Boolean = false,
    val searchQuery: String = "",
    val selectedContactId: String? = null,
    val error: String? = null
)
