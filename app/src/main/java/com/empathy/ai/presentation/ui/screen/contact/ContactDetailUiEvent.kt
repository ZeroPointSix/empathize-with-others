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

    // ========== 对话记录管理事件 ==========
    data class EditConversation(val logId: Long, val newContent: String) : ContactDetailUiEvent()
    data class DeleteConversation(val logId: Long) : ContactDetailUiEvent()
    data object ShowEditConversationDialog : ContactDetailUiEvent()
    data object HideEditConversationDialog : ContactDetailUiEvent()
    data class SelectConversation(val logId: Long) : ContactDetailUiEvent()

    // ========== 事实流添加事实事件 ==========
    data object ShowAddFactToStreamDialog : ContactDetailUiEvent()
    data object HideAddFactToStreamDialog : ContactDetailUiEvent()
    data class AddFactToStream(val key: String, val value: String) : ContactDetailUiEvent()

    // ========== 对话框事件 ==========
    data object ShowDeleteConfirmDialog : ContactDetailUiEvent()
    data object HideDeleteConfirmDialog : ContactDetailUiEvent()
    data object ShowUnsavedChangesDialog : ContactDetailUiEvent()
    data object HideUnsavedChangesDialog : ContactDetailUiEvent()
    data object ShowAddFactDialog : ContactDetailUiEvent()
    data object HideAddFactDialog : ContactDetailUiEvent()
    data object ShowAddTagDialog : ContactDetailUiEvent()
    data object HideAddTagDialog : ContactDetailUiEvent()
    data class UpdateNewTagContent(val content: String) : ContactDetailUiEvent()
    data class UpdateNewTagType(val type: TagType) : ContactDetailUiEvent()
    data object ConfirmAddTag : ContactDetailUiEvent()

    // ========== 编辑功能事件（TD-00012） ==========
    // 事实编辑
    data class StartEditFact(val fact: Fact) : ContactDetailUiEvent()
    data class ConfirmEditFact(val factId: String, val newKey: String, val newValue: String) : ContactDetailUiEvent()
    data object CancelEditFact : ContactDetailUiEvent()
    data class DeleteFactById(val factId: String) : ContactDetailUiEvent()
    
    // 总结编辑
    data class StartEditSummary(val summaryId: Long) : ContactDetailUiEvent()
    data class ConfirmEditSummary(val summaryId: Long, val newContent: String) : ContactDetailUiEvent()
    data object CancelEditSummary : ContactDetailUiEvent()
    data class DeleteSummary(val summaryId: Long) : ContactDetailUiEvent()
    
    // 联系人信息编辑
    data object StartEditContactInfo : ContactDetailUiEvent()
    data class ConfirmEditContactInfo(val newName: String, val newTargetGoal: String) : ContactDetailUiEvent()
    data object CancelEditContactInfo : ContactDetailUiEvent()
    
    // ========== 标签画像V2事件（TD-00014） ==========
    // 搜索相关
    data class UpdatePersonaSearch(val query: String) : ContactDetailUiEvent()
    data object ClearPersonaSearch : ContactDetailUiEvent()
    
    // 分类展开/折叠
    data class ToggleCategoryExpand(val categoryKey: String) : ContactDetailUiEvent()
    
    // 编辑模式
    data class EnterEditMode(val initialFactId: String? = null) : ContactDetailUiEvent()
    data object ExitEditMode : ContactDetailUiEvent()
    
    // 标签选择
    data class ToggleFactSelection(val factId: String) : ContactDetailUiEvent()
    data class SelectAllInCategory(val categoryKey: String) : ContactDetailUiEvent()
    data object DeselectAllFacts : ContactDetailUiEvent()
    data object SelectAllFacts : ContactDetailUiEvent()
    
    // 批量操作对话框
    data object ShowBatchDeleteConfirm : ContactDetailUiEvent()
    data object HideBatchDeleteConfirm : ContactDetailUiEvent()
    data object ConfirmBatchDelete : ContactDetailUiEvent()
    data object ShowBatchMoveDialog : ContactDetailUiEvent()
    data object HideBatchMoveDialog : ContactDetailUiEvent()
    data class ConfirmBatchMove(val targetCategory: String) : ContactDetailUiEvent()
    
    // Feature Flag
    data class SetUsePersonaTabV2(val enabled: Boolean) : ContactDetailUiEvent()
    
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
