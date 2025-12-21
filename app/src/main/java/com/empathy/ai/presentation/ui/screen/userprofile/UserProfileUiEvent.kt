package com.empathy.ai.presentation.ui.screen.userprofile

import com.empathy.ai.domain.model.ExportFormat

/**
 * 用户画像界面UI事件
 *
 * 定义所有用户交互事件。
 */
sealed class UserProfileUiEvent {
    
    // ==================== 标签操作事件 ====================
    
    /**
     * 显示添加标签对话框
     *
     * @property dimensionKey 目标维度键名
     */
    data class ShowAddTagDialog(val dimensionKey: String) : UserProfileUiEvent()
    
    /**
     * 添加标签
     *
     * @property dimensionKey 目标维度键名
     * @property tag 标签内容
     */
    data class AddTag(val dimensionKey: String, val tag: String) : UserProfileUiEvent()
    
    /**
     * 显示编辑标签对话框
     *
     * @property dimensionKey 维度键名
     * @property tag 当前标签内容
     */
    data class ShowEditTagDialog(val dimensionKey: String, val tag: String) : UserProfileUiEvent()
    
    /**
     * 编辑标签
     *
     * @property dimensionKey 维度键名
     * @property oldTag 原标签内容
     * @property newTag 新标签内容
     */
    data class EditTag(
        val dimensionKey: String,
        val oldTag: String,
        val newTag: String
    ) : UserProfileUiEvent()
    
    /**
     * 显示删除标签确认对话框
     *
     * @property dimensionKey 维度键名
     * @property tag 要删除的标签
     */
    data class ShowDeleteTagConfirm(val dimensionKey: String, val tag: String) : UserProfileUiEvent()
    
    /**
     * 确认删除标签
     */
    data object ConfirmDeleteTag : UserProfileUiEvent()
    
    /**
     * 隐藏标签相关对话框
     */
    data object HideTagDialog : UserProfileUiEvent()
    
    // ==================== 维度操作事件 ====================
    
    /**
     * 显示添加维度对话框
     */
    data object ShowAddDimensionDialog : UserProfileUiEvent()
    
    /**
     * 添加自定义维度
     *
     * @property dimensionName 维度名称
     */
    data class AddDimension(val dimensionName: String) : UserProfileUiEvent()
    
    /**
     * 显示删除维度确认对话框
     *
     * @property dimensionName 要删除的维度名称
     */
    data class ShowDeleteDimensionConfirm(val dimensionName: String) : UserProfileUiEvent()
    
    /**
     * 确认删除维度
     */
    data object ConfirmDeleteDimension : UserProfileUiEvent()
    
    /**
     * 隐藏维度相关对话框
     */
    data object HideDimensionDialog : UserProfileUiEvent()
    
    // ==================== 导出事件 ====================
    
    /**
     * 显示导出对话框
     */
    data object ShowExportDialog : UserProfileUiEvent()
    
    /**
     * 导出用户画像
     *
     * @property format 导出格式
     */
    data class ExportProfile(val format: ExportFormat) : UserProfileUiEvent()
    
    /**
     * 隐藏导出对话框
     */
    data object HideExportDialog : UserProfileUiEvent()
    
    // ==================== 其他事件 ====================
    
    /**
     * 切换标签页
     *
     * @property tabIndex 目标标签页索引
     */
    data class SwitchTab(val tabIndex: Int) : UserProfileUiEvent()
    
    /**
     * 显示重置确认对话框
     */
    data object ShowResetConfirm : UserProfileUiEvent()
    
    /**
     * 确认重置画像
     */
    data object ConfirmResetProfile : UserProfileUiEvent()
    
    /**
     * 隐藏重置确认对话框
     */
    data object HideResetConfirm : UserProfileUiEvent()
    
    /**
     * 清除错误消息
     */
    data object ClearError : UserProfileUiEvent()
    
    /**
     * 清除成功消息
     */
    data object ClearSuccessMessage : UserProfileUiEvent()
    
    /**
     * 刷新画像数据
     */
    data object RefreshProfile : UserProfileUiEvent()
}
