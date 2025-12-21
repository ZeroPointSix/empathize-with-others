package com.empathy.ai.presentation.ui.screen.userprofile

import com.empathy.ai.domain.model.UserProfile

/**
 * 用户画像界面UI状态
 *
 * 包含界面渲染所需的所有状态数据。
 */
data class UserProfileUiState(
    /** 当前用户画像数据 */
    val profile: UserProfile = UserProfile(),
    
    /** 选中的标签页索引（0=基础信息，1=自定义维度） */
    val selectedTabIndex: Int = 0,
    
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    
    /** 错误消息 */
    val error: String? = null,
    
    /** 是否显示添加标签对话框 */
    val showAddTagDialog: Boolean = false,
    
    /** 是否显示编辑标签对话框 */
    val showEditTagDialog: Boolean = false,
    
    /** 是否显示添加维度对话框 */
    val showAddDimensionDialog: Boolean = false,
    
    /** 是否显示删除确认对话框 */
    val showDeleteConfirmDialog: Boolean = false,
    
    /** 是否显示导出对话框 */
    val showExportDialog: Boolean = false,
    
    /** 是否显示重置确认对话框 */
    val showResetConfirmDialog: Boolean = false,
    
    /** 当前编辑的维度键名 */
    val currentEditDimension: String? = null,
    
    /** 当前编辑的标签 */
    val currentEditTag: String? = null,
    
    /** 待删除的维度名称（用于删除确认） */
    val pendingDeleteDimension: String? = null,
    
    /** 待删除的标签（用于删除确认） */
    val pendingDeleteTag: String? = null,
    
    /** 操作成功提示消息 */
    val successMessage: String? = null
) {
    /**
     * 画像完整度百分比
     */
    val completeness: Int
        get() = profile.getCompleteness()
    
    /**
     * 标签总数
     */
    val totalTagCount: Int
        get() = profile.getTotalTagCount()
    
    /**
     * 画像是否为空
     */
    val isEmpty: Boolean
        get() = profile.isEmpty()
    
    /**
     * 是否可以添加更多自定义维度
     */
    val canAddCustomDimension: Boolean
        get() = profile.canAddCustomDimension()
    
    /**
     * 自定义维度数量
     */
    val customDimensionCount: Int
        get() = profile.getCustomDimensionCount()
}
