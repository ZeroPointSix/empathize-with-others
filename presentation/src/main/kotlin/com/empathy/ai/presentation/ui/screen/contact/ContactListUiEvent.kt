package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption

/**
 * 联系人列表界面的用户事件
 *
 * 设计原则：
 * 1. 使用 sealed interface 定义，编译时类型安全
 * 2. 每个事件都是独立的类型
 * 3. 有参数用 data class，无参数用 data object
 * 4. 事件命名使用动词开头，描述用户意图
 */
sealed interface ContactListUiEvent {
    // === 数据加载事件 ===

    /**
     * 加载联系人列表
     */
    data object LoadContacts : ContactListUiEvent

    /**
     * 刷新联系人列表
     */
    data object RefreshContacts : ContactListUiEvent

    /**
     * 加载更多联系人
     */
    data object LoadMoreContacts : ContactListUiEvent

    // === 搜索相关事件 ===

    /**
     * 更新搜索查询
     * @param query 搜索关键词
     */
    data class UpdateSearchQuery(val query: String) : ContactListUiEvent

    /**
     * 开始搜索
     */
    data object StartSearch : ContactListUiEvent

    /**
     * 清除搜索
     */
    data object ClearSearch : ContactListUiEvent

    /**
     * 取消搜索
     */
    data object CancelSearch : ContactListUiEvent

    /**
     * 保存当前搜索词到历史
     */
    data object SaveSearchHistory : ContactListUiEvent

    /**
     * 清空搜索历史
     */
    data object ClearSearchHistory : ContactListUiEvent

    // === 最近访问相关事件 ===

    /**
     * 刷新最近访问联系人
     */
    data object RefreshRecentContacts : ContactListUiEvent

    /**
     * 清空最近访问联系人
     */
    data object ClearRecentContacts : ContactListUiEvent

    // === 选择相关事件 ===

    /**
     * 选择联系人
     * @param contactId 联系人ID
     */
    data class SelectContact(val contactId: String) : ContactListUiEvent

    /**
     * 取消选择联系人
     * @param contactId 联系人ID
     */
    data class DeselectContact(val contactId: String) : ContactListUiEvent

    /**
     * 切换选择状态
     * @param contactId 联系人ID
     */
    data class ToggleContactSelection(val contactId: String) : ContactListUiEvent

    /**
     * 全选
     */
    data object SelectAll : ContactListUiEvent

    /**
     * 清除所有选择
     */
    data object ClearSelection : ContactListUiEvent

    /**
     * 开始选择模式
     */
    data object StartSelectionMode : ContactListUiEvent

    /**
     * 结束选择模式
     */
    data object EndSelectionMode : ContactListUiEvent

    // === 联系人操作事件 ===

    /**
     * 点击联系人进入聊天
     * @param contactId 联系人ID
     */
    data class OpenChat(val contactId: String) : ContactListUiEvent

    /**
     * 编辑联系人
     * @param contactId 联系人ID
     */
    data class EditContact(val contactId: String) : ContactListUiEvent

    /**
     * 删除联系人
     * @param contact 联系人信息
     */
    data class DeleteContact(val contact: ContactProfile) : ContactListUiEvent

    /**
     * 删除选中的联系人
     */
    data object DeleteSelectedContacts : ContactListUiEvent

    /**
     * 显示添加联系人对话框
     */
    data object ShowAddContactDialog : ContactListUiEvent

    /**
     * 隐藏添加联系人对话框
     */
    data object HideAddContactDialog : ContactListUiEvent

    /**
     * 显示删除确认对话框
     * @param contact 要删除的联系人
     */
    data class ShowDeleteConfirmDialog(val contact: ContactProfile) : ContactListUiEvent

    /**
     * 隐藏删除确认对话框
     */
    data object HideDeleteConfirmDialog : ContactListUiEvent

    // === 排序相关事件 ===

    /**
     * 更新排序选项
     * @param option 排序选项
     */
    data class UpdateSortOption(val option: ContactSortOption) : ContactListUiEvent

    // === 通用事件 ===

    /**
     * 清除错误
     */
    data object ClearError : ContactListUiEvent

    /**
     * 导航到设置
     */
    data object NavigateToSettings : ContactListUiEvent

    /**
     * 导航返回
     */
    data object NavigateBack : ContactListUiEvent
}
