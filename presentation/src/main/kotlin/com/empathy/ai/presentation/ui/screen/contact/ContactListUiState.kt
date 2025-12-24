package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.domain.model.ContactProfile

/**
 * 联系人列表界面的UI状态
 *
 * 设计原则：
 * 1. 所有字段都有默认值，避免空指针
 * 2. 使用 data class，自动获得 copy() 方法
 * 3. 状态是不可变的 (val)，通过 copy() 更新
 * 4. 包含所有UI信息：数据 + 加载状态 + 错误信息 + 交互状态
 */
data class ContactListUiState(
    // 通用状态字段
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // 联系人数据
    val contacts: List<ContactProfile> = emptyList(),
    val filteredContacts: List<ContactProfile> = emptyList(),

    // 搜索状态
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<ContactProfile> = emptyList(),

    // 选中状态
    val selectedContactIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,

    // UI交互状态
    val showAddContactDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val contactToDelete: ContactProfile? = null,

    // 分页状态（如果需要）
    val hasMore: Boolean = false,
    val currentPage: Int = 0,
    val pageSize: Int = 20,

    // 导航状态
    val shouldNavigateToChat: String = "",
    val shouldNavigateToEdit: String = "",
    val shouldNavigateToSettings: Boolean = false,
    val shouldNavigateBack: Boolean = false
) {
    // 计算属性：是否有联系人
    val hasContacts: Boolean
        get() = contacts.isNotEmpty()

    // 计算属性：是否显示搜索结果
    val isShowingSearchResults: Boolean
        get() = searchQuery.isNotBlank() && isSearching

    // 计算属性：显示的联系人列表
    val displayContacts: List<ContactProfile>
        get() = if (isShowingSearchResults) searchResults else filteredContacts

    // 计算属性：是否为空状态
    val isEmptyState: Boolean
        get() = displayContacts.isEmpty() && !isLoading

    // 计算属性：是否可以删除
    val canDeleteSelected: Boolean
        get() = selectedContactIds.isNotEmpty()

    // 计算属性：选中的联系人数量
    val selectedCount: Int
        get() = selectedContactIds.size
}