package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption

/**
 * 联系人列表界面的UI状态
 *
 * ## 业务职责
 * 封装联系人列表页面的所有UI状态，采用不可变数据模式确保状态可预测。
 *
 * ## 状态分组
 * ```
 * ContactListUiState
 * ├── 通用状态        ← isLoading, isRefreshing, error
 * ├── 数据状态        ← contacts, filteredContacts
 * ├── 搜索状态        ← searchQuery, isSearching, searchResults
 * ├── 选择状态        ← selectedContactIds, isSelectionMode
 * ├── 交互状态        ← showAddContactDialog, showDeleteConfirmDialog...
 * ├── 分页状态        ← hasMore, currentPage, pageSize
 * ├── 排序状态        ← sortOption
 * └── 导航状态        ← shouldNavigateToChat, shouldNavigateToEdit...
 * ```
 *
 * ## 设计原则
 * 1. **不可变性**: 所有字段使用 val，状态变更通过 copy() 实现
 * 2. **空安全**: 所有字段都有默认值，避免空指针异常
 * 3. **数据类优势**: 使用 data class 自动获得 equals/hashCode/copy/toString
 * 4. **派生状态**: 计算属性减少冗余状态，保持状态单一来源
 *
 * ## 关键派生状态
 * - `displayContacts`: 根据搜索状态返回原始或过滤后的列表
 * - `isEmptyState`: 用于显示空状态UI（区分"加载中"和"真的没有数据"）
 * - `isSelectionMode`: 控制多选UI的显示
 *
 * @property isLoading 是否正在加载（显示骨架屏）
 * @property isRefreshing 是否正在刷新（下拉刷新状态）
 * @property error 错误信息（非空时显示错误卡片）
 * @property contacts 原始联系人列表
 * @property filteredContacts 过滤后的联系人列表
 * @property searchQuery 搜索关键词
 * @property isSearching 是否正在搜索（激活搜索模式）
 * @property searchResults 搜索结果列表
 * @property searchHistory 最近搜索历史
 * @property recentContacts 最近访问联系人
 * @property selectedContactIds 选中的联系人ID集合（多选用）
 * @property isSelectionMode 是否处于多选模式
 * @property showAddContactDialog 是否显示添加联系人对话框
 * @property showDeleteConfirmDialog 是否显示删除确认对话框
 * @property contactToDelete 待删除的联系人（对话框用）
 * @property hasMore 是否有更多数据（分页用）
 * @property currentPage 当前页码
 * @property pageSize 每页大小
 * @property shouldNavigateToChat 待导航的聊天联系人ID
 * @property shouldNavigateToEdit 待编辑的联系人ID
 * @property shouldNavigateToSettings 是否应导航到设置页
 * @property shouldNavigateBack 是否应返回
 * @property sortOption 当前排序选项
 */
data class ContactListUiState(
    // 通用状态
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // 数据状态
    val contacts: List<ContactProfile> = emptyList(),
    val filteredContacts: List<ContactProfile> = emptyList(),

    // 搜索状态
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<ContactProfile> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val recentContacts: List<ContactProfile> = emptyList(),

    // 选择状态
    val selectedContactIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,

    // 交互状态
    val showAddContactDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val contactToDelete: ContactProfile? = null,

    // 分页状态
    val hasMore: Boolean = false,
    val currentPage: Int = 0,
    val pageSize: Int = 20,

    // 导航状态
    val shouldNavigateToChat: String = "",
    val shouldNavigateToEdit: String = "",
    val shouldNavigateToSettings: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val sortOption: ContactSortOption = ContactSortOption.NAME,
    // 首次加载完成标记，避免初始错误闪现
    val hasLoadedContacts: Boolean = false
) {
    /** 是否有联系人数据 */
    val hasContacts: Boolean get() = contacts.isNotEmpty()

    /** 是否显示搜索结果 */
    val isShowingSearchResults: Boolean get() = searchQuery.isNotBlank() && isSearching

    /** 显示的联系人列表（根据搜索状态动态返回） */
    val displayContacts: List<ContactProfile>
        get() = if (isShowingSearchResults) searchResults else filteredContacts

    /** 是否为空状态（无数据且非加载中） */
    val isEmptyState: Boolean get() = displayContacts.isEmpty() && !isLoading

    /** 是否可以删除选中的联系人 */
    val canDeleteSelected: Boolean get() = selectedContactIds.isNotEmpty()

    /** 选中的联系人数量 */
    val selectedCount: Int get() = selectedContactIds.size

    /** 是否有最近访问联系人 */
    val hasRecentContacts: Boolean get() = recentContacts.isNotEmpty()
}
