package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.ViewMode

/**
 * 联系人详情UI事件
 *
 * 密封类，表示所有可能的用户交互事件
 */
sealed class ContactDetailUiEvent {
    
    // ========== 数据加载事件 ==========
    data class LoadContact(val contactId: String) : ContactDetailUiEvent()
    data object ReloadContact : ContactDetailUiEvent()
    data class LoadRelationshipData(val contactId: String) : ContactDetailUiEvent()
    
    // ========== 编辑相关事件 ==========
    data object StartEdit : ContactDetailUiEvent()
    data object CancelEdit : ContactDetailUiEvent()
    data object SaveContact : ContactDetailUiEvent()
    data object DeleteContact : ContactDetailUiEvent()
    
    // ========== 表单字段更新事件 ==========
    data class UpdateName(val name: String) : ContactDetailUiEvent()
    data class UpdateTargetGoal(val targetGoal: String) : ContactDetailUiEvent()
    data class UpdateContextDepth(val contextDepth: Int) : ContactDetailUiEvent()
    
    // ========== 事实管理事件 ==========
    data class AddFact(val key: String, val value: String) : ContactDetailUiEvent()
    data class UpdateFact(val key: String, val value: String) : ContactDetailUiEvent()
    data class DeleteFact(val key: String) : ContactDetailUiEvent()
    data class DeleteFactItem(val fact: Fact) : ContactDetailUiEvent()
    data class UpdateNewFactKey(val key: String) : ContactDetailUiEvent()
    data class UpdateNewFactValue(val value: String) : ContactDetailUiEvent()
    
    // ========== 标签管理事件 ==========
    data class LoadBrainTags(val contactId: String) : ContactDetailUiEvent()
    data class AddBrainTag(val tag: String, val type: TagType) : ContactDetailUiEvent()
    data class DeleteBrainTag(val tagId: Long) : ContactDetailUiEvent()
    data class UpdateTagSearchQuery(val query: String) : ContactDetailUiEvent()
    data class ToggleTagTypeSelection(val tagType: TagType) : ContactDetailUiEvent()
    data object StartTagSearch : ContactDetailUiEvent()
    data object ClearTagSearch : ContactDetailUiEvent()
    
    // ========== Tab页面事件 ==========
    data class SwitchTab(val tab: DetailTab) : ContactDetailUiEvent()
    data class SwitchViewMode(val mode: ViewMode) : ContactDetailUiEvent()
    data class ToggleFilter(val filter: FilterType) : ContactDetailUiEvent()
    data class ConfirmTag(val factId: Long) : ContactDetailUiEvent()
    data class RejectTag(val factId: Long) : ContactDetailUiEvent()
    
    // ========== 对话框事件 ==========
    data object ShowDeleteConfirmDialog : ContactDetailUiEvent()
    data object HideDeleteConfirmDialog : ContactDetailUiEvent()
    data object ShowUnsavedChangesDialog : ContactDetailUiEvent()
    data object HideUnsavedChangesDialog : ContactDetailUiEvent()
    data object ShowAddFactDialog : ContactDetailUiEvent()
    data object HideAddFactDialog : ContactDetailUiEvent()
    data object ShowAddTagDialog : ContactDetailUiEvent()
    data object HideAddTagDialog : ContactDetailUiEvent()
    
    // ========== 字段验证事件 ==========
    data object ValidateName : ContactDetailUiEvent()
    data object ValidateTargetGoal : ContactDetailUiEvent()
    data object ValidateContextDepth : ContactDetailUiEvent()
    data object ValidateFactKey : ContactDetailUiEvent()
    data object ValidateFactValue : ContactDetailUiEvent()
    
    // ========== 通用事件 ==========
    data object ClearError : ContactDetailUiEvent()
    data object ClearSuccessMessage : ContactDetailUiEvent()
    data object ResetForm : ContactDetailUiEvent()
    data class NavigateToChat(val contactId: String) : ContactDetailUiEvent()
    data object NavigateBack : ContactDetailUiEvent()
    data object ConfirmNavigateBack : ContactDetailUiEvent()
    data object RefreshData : ContactDetailUiEvent()
}
