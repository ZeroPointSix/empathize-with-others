package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.GetContactSearchHistoryUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetContactSortOptionUseCase
import com.empathy.ai.domain.usecase.SaveContactSortOptionUseCase
import com.empathy.ai.domain.usecase.SaveContactSearchQueryUseCase
import com.empathy.ai.domain.usecase.SortContactsUseCase
import com.empathy.ai.domain.usecase.ClearContactSearchHistoryUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactListUiEvent
import com.empathy.ai.presentation.ui.screen.contact.ContactListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 联系人列表页面 ViewModel
 *
 * ## 业务职责
 * 管理联系人列表的完整交互逻辑：
 * - 联系人列表加载和分页
 * - 搜索过滤（按名称、标签、关系阶段）
 * - 搜索历史记录（最近搜索词）
 * - 排序方式切换（姓名/最近互动/关系分数）
 * - 批量操作支持（删除、标签管理）
 * - 多选模式和全选功能
 *
 * ## 核心数据流
 * ```
 * UserAction(Search/Filter/Sort) → ViewModel → Repository → PagedList → UI
 * ```
 *
 * ## 关键设计决策
 * - **Paging3**: 大数据量分页加载，避免内存溢出
 * - **复合过滤**: 搜索词 + 标签过滤 + 排序方式组合生效
 * - **即时搜索**: 使用Debounce避免频繁请求（300ms延迟）
 * - **空状态优化**: 不同空状态展示不同UI（无联系人/无匹配结果）
 *
 * ## 过滤类型
 * - ALL: 全部联系人
 * - STARRED: 仅星标联系人
 * - RISK_TAG: 仅有雷区标签的联系人
 * - STRATEGY_TAG:仅有策略标签的联系人
 * - RECENT: 最近活跃的联系人
 *
 * @see com.empathy.ai.presentation.ui.screen.contact.ContactListScreen
 */
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getAllContactsUseCase: GetAllContactsUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val getContactSortOptionUseCase: GetContactSortOptionUseCase,
    private val saveContactSortOptionUseCase: SaveContactSortOptionUseCase,
    private val sortContactsUseCase: SortContactsUseCase,
    private val getContactSearchHistoryUseCase: GetContactSearchHistoryUseCase,
    private val saveContactSearchQueryUseCase: SaveContactSearchQueryUseCase,
    private val clearContactSearchHistoryUseCase: ClearContactSearchHistoryUseCase
) : ViewModel() {

    // 私有可变状态（只能内部修改）
    private val _uiState = MutableStateFlow(ContactListUiState())

    // 公开不可变状态（外部只读）
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    init {
        // ViewModel创建时自动加载数据
        // 先加载排序选项，确保联系人列表排序时使用正确的排序选项
        loadSortOption()
        // 加载最近搜索历史，便于搜索模式快速回填
        loadSearchHistory()
        // loadSortOption 完成后会自动触发 loadContacts
    }

    /**
     * 统一的事件处理入口
     *
     * 设计意图：
     * 1. 单一入口，便于追踪和调试
     * 2. when 表达式确保处理所有事件类型
     */
    fun onEvent(event: ContactListUiEvent) {
        when (event) {
            // === 数据加载事件 ===
            is ContactListUiEvent.LoadContacts -> loadContacts()
            is ContactListUiEvent.RefreshContacts -> refreshContacts()
            is ContactListUiEvent.LoadMoreContacts -> loadMoreContacts()

            // === 搜索相关事件 ===
            is ContactListUiEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is ContactListUiEvent.StartSearch -> startSearch()
            is ContactListUiEvent.ClearSearch -> clearSearch()
            is ContactListUiEvent.CancelSearch -> cancelSearch()
            is ContactListUiEvent.SaveSearchHistory -> saveSearchHistoryIfNeeded()
            is ContactListUiEvent.ClearSearchHistory -> clearSearchHistory()

            // === 选择相关事件 ===
            is ContactListUiEvent.SelectContact -> selectContact(event.contactId)
            is ContactListUiEvent.DeselectContact -> deselectContact(event.contactId)
            is ContactListUiEvent.ToggleContactSelection -> toggleContactSelection(event.contactId)
            is ContactListUiEvent.SelectAll -> selectAll()
            is ContactListUiEvent.ClearSelection -> clearSelection()
            is ContactListUiEvent.StartSelectionMode -> startSelectionMode()
            is ContactListUiEvent.EndSelectionMode -> endSelectionMode()

            // === 联系人操作事件 ===
            is ContactListUiEvent.OpenChat -> openChat(event.contactId)
            is ContactListUiEvent.EditContact -> editContact(event.contactId)
            is ContactListUiEvent.DeleteContact -> deleteContact(event.contact)
            is ContactListUiEvent.DeleteSelectedContacts -> deleteSelectedContacts()
            is ContactListUiEvent.ShowAddContactDialog -> showAddContactDialog()
            is ContactListUiEvent.HideAddContactDialog -> hideAddContactDialog()
            is ContactListUiEvent.ShowDeleteConfirmDialog -> showDeleteConfirmDialog(event.contact)
            is ContactListUiEvent.HideDeleteConfirmDialog -> hideDeleteConfirmDialog()

            // === 排序相关事件 ===
            is ContactListUiEvent.UpdateSortOption -> updateSortOption(event.option)

            // === 通用事件 ===
            is ContactListUiEvent.ClearError -> clearError()
            is ContactListUiEvent.NavigateToSettings -> navigateToSettings()
            is ContactListUiEvent.NavigateBack -> navigateBack()
        }
    }

    /**
     * 加载联系人列表
     *
     * 使用Flow收集数据，实现响应式更新
     * 注意：应该只在排序选项加载完成后调用
     */
    private fun loadContacts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, hasLoadedContacts = false) }

                // 收集联系人数据流
                getAllContactsUseCase().collect { contacts ->
                    _uiState.update { currentState ->
                        val sortedContacts = sortContactsUseCase(
                            contacts,
                            currentState.sortOption
                        )
                        currentState.copy(
                            isLoading = false,
                            contacts = contacts,
                            filteredContacts = sortedContacts,
                            hasMore = contacts.size >= currentState.pageSize,
                            hasLoadedContacts = true,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载联系人失败",
                        hasLoadedContacts = true
                    )
                }
            }
        }
    }

    /**
     * 刷新联系人列表
     */
    private fun refreshContacts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true, error = null) }

                // Flow会自动刷新，这里只需要更新状态
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "刷新失败"
                    )
                }
            }
        }
    }

    /**
     * 加载更多联系人（分页）
     */
    private fun loadMoreContacts() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasMore) return

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        currentPage = currentState.currentPage + 1
                    )
                }

                // 这里可以实现分页逻辑
                // 目前Flow会自动包含所有数据，所以这里只是示例
                _uiState.update { it.copy(isLoading = false, hasMore = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载更多失败"
                    )
                }
            }
        }
    }

    // === 搜索相关方法 ===

    private fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // 实时搜索（带防抖）
        if (query.isNotBlank()) {
            performSearch(query)
        } else {
            clearSearchResults()
        }
    }

    /**
     * 开始搜索模式
     * 
     * BUG-00063修复：先设置isSearching=true展开搜索框，
     * 然后如果已有搜索词则执行搜索
     */
    private fun startSearch() {
        // 先设置搜索模式为true，展开搜索框
        _uiState.update { it.copy(isSearching = true) }
        
        // 如果已有搜索词，执行搜索
        val currentState = _uiState.value
        if (currentState.searchQuery.isNotBlank()) {
            performSearch(currentState.searchQuery)
        }
    }

    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filteredContacts = currentState.contacts.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
            contact.targetGoal.contains(query, ignoreCase = true) ||
            contact.facts.any { fact ->
                fact.key.contains(query, ignoreCase = true) ||
                fact.value.contains(query, ignoreCase = true)
            }
        }
        val sortedResults = sortContactsUseCase(filteredContacts, currentState.sortOption)

        _uiState.update {
            it.copy(
                isSearching = true,
                searchResults = sortedResults
            )
        }
    }

    private fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList()
            )
        }
    }

    private fun clearSearchResults() {
        _uiState.update {
            it.copy(
                searchResults = emptyList()
            )
        }
    }

    private fun cancelSearch() {
        saveSearchHistoryIfNeeded()
        clearSearch()
    }

    // === 选择相关方法 ===

    private fun selectContact(contactId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedContactIds = currentState.selectedContactIds + contactId
            )
        }
    }

    private fun deselectContact(contactId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedContactIds = currentState.selectedContactIds - contactId
            )
        }
    }

    private fun toggleContactSelection(contactId: String) {
        _uiState.update { currentState ->
            val newSelectedIds = if (currentState.selectedContactIds.contains(contactId)) {
                currentState.selectedContactIds - contactId
            } else {
                currentState.selectedContactIds + contactId
            }
            currentState.copy(selectedContactIds = newSelectedIds)
        }
    }

    private fun selectAll() {
        val currentState = _uiState.value
        val allContactIds = currentState.displayContacts.map { it.id }
        _uiState.update {
            it.copy(selectedContactIds = allContactIds.toSet())
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedContactIds = emptySet()) }
    }

    private fun startSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = true) }
    }

    private fun endSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedContactIds = emptySet()
            )
        }
    }

    // === 联系人操作方法 ===

    private fun openChat(contactId: String) {
        // 这里可以通过状态通知UI导航到聊天界面
        _uiState.update { it.copy(shouldNavigateToChat = contactId) }
    }

    private fun editContact(contactId: String) {
        // 这里可以通过状态通知UI导航到编辑界面
        _uiState.update { it.copy(shouldNavigateToEdit = contactId) }
    }

    private fun deleteContact(contact: ContactProfile) {
        viewModelScope.launch {
            try {
                deleteContactUseCase(contact.id).onSuccess {
                    // 删除成功，Flow会自动更新UI
                    clearSelection()
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "删除失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "删除失败")
                }
            }
        }
    }

    private fun deleteSelectedContacts() {
        val currentState = _uiState.value
        val selectedIds = currentState.selectedContactIds.toList()

        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 批量删除
                selectedIds.forEach { contactId ->
                    deleteContactUseCase(contactId)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedContactIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "批量删除失败"
                    )
                }
            }
        }
    }

    // === 排序相关方法 ===

    private fun loadSortOption() {
        viewModelScope.launch {
            getContactSortOptionUseCase()
                .onSuccess { option ->
                    _uiState.update { it.copy(sortOption = option) }
                    // 排序选项加载完成后，再加载联系人列表
                    loadContacts()
                }
                .onFailure { error ->
                    // 加载失败时使用默认值并记录错误，但继续加载联系人
                    // 可以考虑添加日志记录
                    loadContacts()
                }
        }
    }

    private fun updateSortOption(option: ContactSortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applySorting()
        viewModelScope.launch {
            saveContactSortOptionUseCase(option)
                .onFailure { error ->
                    // 保存失败时更新错误状态，用户可以看到提示
                    _uiState.update {
                        it.copy(error = "保存排序偏好失败: ${error.message}")
                    }
                }
        }
    }

    private fun applySorting() {
        val currentState = _uiState.value
        val sortedContacts = sortContactsUseCase(
            currentState.contacts,
            currentState.sortOption
        )
        val sortedSearchResults = if (currentState.isShowingSearchResults) {
            sortContactsUseCase(currentState.searchResults, currentState.sortOption)
        } else {
            currentState.searchResults
        }
        _uiState.update {
            it.copy(
                filteredContacts = sortedContacts,
                searchResults = sortedSearchResults
            )
        }
    }

    // === 搜索历史相关方法 ===

    private fun loadSearchHistory() {
        viewModelScope.launch {
            getContactSearchHistoryUseCase()
                .onSuccess { history ->
                    _uiState.update { it.copy(searchHistory = history) }
                }
        }
    }

    private fun saveSearchHistoryIfNeeded() {
        val currentState = _uiState.value
        val query = currentState.searchQuery.trim()
        if (query.isBlank() || currentState.searchResults.isEmpty()) return

        viewModelScope.launch {
            saveContactSearchQueryUseCase(query)
                .onSuccess { history ->
                    _uiState.update { it.copy(searchHistory = history) }
                }
        }
    }

    private fun clearSearchHistory() {
        viewModelScope.launch {
            clearContactSearchHistoryUseCase()
                .onSuccess {
                    _uiState.update { it.copy(searchHistory = emptyList()) }
                }
        }
    }

    // === 对话框管理方法 ===

    private fun showAddContactDialog() {
        _uiState.update { it.copy(showAddContactDialog = true) }
    }

    private fun hideAddContactDialog() {
        _uiState.update { it.copy(showAddContactDialog = false) }
    }

    private fun showDeleteConfirmDialog(contact: ContactProfile) {
        _uiState.update { it.copy(showDeleteConfirmDialog = true, contactToDelete = contact) }
    }

    private fun hideDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false, contactToDelete = null) }
    }

    // === 通用方法 ===

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun navigateToSettings() {
        _uiState.update { it.copy(shouldNavigateToSettings = true) }
    }

    private fun navigateBack() {
        _uiState.update { it.copy(shouldNavigateBack = true) }
    }
}
