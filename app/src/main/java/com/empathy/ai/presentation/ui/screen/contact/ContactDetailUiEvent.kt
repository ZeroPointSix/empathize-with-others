package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.domain.model.BrainTag

/**
 * 联系人详情界面的用户事件
 *
 * 设计原则：
 * 1. 使用 sealed interface 定义，编译时类型安全
 * 2. 每个事件都是独立的类型
 * 3. 有参数用 data class，无参数用 data object
 * 4. 事件命名使用动词开头，描述用户意图
 */
sealed interface ContactDetailUiEvent {
    // === 数据加载事件 ===

    /**
     * 加载联系人详情
     * @param contactId 联系人ID，空字符串表示新建
     */
    data class LoadContact(val contactId: String) : ContactDetailUiEvent

    /**
     * 重新加载联系人详情
     */
    data object ReloadContact : ContactDetailUiEvent

    // === 编辑相关事件 ===

    /**
     * 开始编辑
     */
    data object StartEdit : ContactDetailUiEvent

    /**
     * 取消编辑
     */
    data object CancelEdit : ContactDetailUiEvent

    /**
     * 保存联系人
     */
    data object SaveContact : ContactDetailUiEvent

    /**
     * 删除联系人
     */
    data object DeleteContact : ContactDetailUiEvent

    // === 表单字段更新事件 ===

    /**
     * 更新名称
     * @param name 新名称
     */
    data class UpdateName(val name: String) : ContactDetailUiEvent

    /**
     * 更新目标
     * @param targetGoal 新目标
     */
    data class UpdateTargetGoal(val targetGoal: String) : ContactDetailUiEvent

    /**
     * 更新上下文深度
     * @param contextDepth 新深度
     */
    data class UpdateContextDepth(val contextDepth: Int) : ContactDetailUiEvent

    // === 事实管理事件 ===

    /**
     * 添加事实
     * @param key 事实键
     * @param value 事实值
     */
    data class AddFact(val key: String, val value: String) : ContactDetailUiEvent

    /**
     * 更新事实
     * @param key 事实键
     * @param value 新值
     */
    data class UpdateFact(val key: String, val value: String) : ContactDetailUiEvent

    /**
     * 删除事实
     * @param key 事实键
     */
    data class DeleteFact(val key: String) : ContactDetailUiEvent

    /**
     * 更新新事实键
     * @param key 事实键
     */
    data class UpdateNewFactKey(val key: String) : ContactDetailUiEvent

    /**
     * 更新新事实值
     * @param value 事实值
     */
    data class UpdateNewFactValue(val value: String) : ContactDetailUiEvent

    // === 标签管理事件 ===

    /**
     * 加载标签
     * @param contactId 联系人ID
     */
    data class LoadBrainTags(val contactId: String) : ContactDetailUiEvent

    /**
     * 添加标签
     * @param tag 标签内容
     * @param type 标签类型（RED/GREEN）
     */
    data class AddBrainTag(val tag: String, val type: String) : ContactDetailUiEvent

    /**
     * 删除标签
     * @param tagId 标签ID
     */
    data class DeleteBrainTag(val tagId: Long) : ContactDetailUiEvent

    /**
     * 更新标签搜索查询
     * @param query 搜索关键词
     */
    data class UpdateTagSearchQuery(val query: String) : ContactDetailUiEvent

    /**
     * 切换标签类型选择
     * @param tagType 标签类型
     */
    data class ToggleTagTypeSelection(val tagType: String) : ContactDetailUiEvent

    /**
     * 开始搜索标签
     */
    data object StartTagSearch : ContactDetailUiEvent

    /**
     * 清除标签搜索
     */
    data object ClearTagSearch : ContactDetailUiEvent

    // === 对话框事件 ===

    /**
     * 显示删除确认对话框
     */
    data object ShowDeleteConfirmDialog : ContactDetailUiEvent

    /**
     * 隐藏删除确认对话框
     */
    data object HideDeleteConfirmDialog : ContactDetailUiEvent

    /**
     * 显示未保存更改对话框
     */
    data object ShowUnsavedChangesDialog : ContactDetailUiEvent

    /**
     * 隐藏未保存更改对话框
     */
    data object HideUnsavedChangesDialog : ContactDetailUiEvent

    /**
     * 显示添加事实对话框
     */
    data object ShowAddFactDialog : ContactDetailUiEvent

    /**
     * 隐藏添加事实对话框
     */
    data object HideAddFactDialog : ContactDetailUiEvent

    /**
     * 显示添加标签对话框
     */
    data object ShowAddTagDialog : ContactDetailUiEvent

    /**
     * 隐藏添加标签对话框
     */
    data object HideAddTagDialog : ContactDetailUiEvent

    // === 字段验证事件 ===

    /**
     * 验证名称
     */
    data object ValidateName : ContactDetailUiEvent

    /**
     * 验证目标
     */
    data object ValidateTargetGoal : ContactDetailUiEvent

    /**
     * 验证上下文深度
     */
    data object ValidateContextDepth : ContactDetailUiEvent

    /**
     * 验证事实键
     */
    data object ValidateFactKey : ContactDetailUiEvent

    /**
     * 验证事实值
     */
    data object ValidateFactValue : ContactDetailUiEvent

    // === 通用事件 ===

    /**
     * 清除错误
     */
    data object ClearError : ContactDetailUiEvent

    /**
     * 重置表单
     */
    data object ResetForm : ContactDetailUiEvent

    /**
     * 导航到聊天
     * @param contactId 联系人ID
     */
    data class NavigateToChat(val contactId: String) : ContactDetailUiEvent

    /**
     * 导航返回
     */
    data object NavigateBack : ContactDetailUiEvent

    /**
     * 确认导航返回（忽略未保存的更改）
     */
    data object ConfirmNavigateBack : ContactDetailUiEvent
}