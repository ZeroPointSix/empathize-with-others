package com.empathy.ai.presentation.ui.screen.tag

import com.empathy.ai.domain.model.TagType

/**
 * 标签管理界面的用户事件
 *
 * 设计原则：
 * 1. 使用 sealed interface 定义，编译时类型安全
 * 2. 每个事件都是独立的类型
 * 3. 有参数用 data class，无参数用 data object
 * 4. 事件命名使用动词开头，描述用户意图
 */
sealed interface BrainTagUiEvent {
    // === 数据加载事件 ===

    /**
     * 加载标签列表
     */
    data object LoadTags : BrainTagUiEvent

    /**
     * 刷新标签列表
     */
    data object RefreshTags : BrainTagUiEvent

    // === 搜索相关事件 ===

    /**
     * 更新搜索查询
     * @param query 搜索关键词
     */
    data class UpdateSearchQuery(val query: String) : BrainTagUiEvent

    /**
     * 清除搜索
     */
    data object ClearSearch : BrainTagUiEvent

    // === 标签操作事件 ===

    /**
     * 删除标签
     * @param tagId 标签ID
     */
    data class DeleteTag(val tagId: Long) : BrainTagUiEvent

    // === 添加标签对话框事件 ===

    /**
     * 显示添加标签对话框
     */
    data object ShowAddDialog : BrainTagUiEvent

    /**
     * 隐藏添加标签对话框
     */
    data object HideAddDialog : BrainTagUiEvent

    /**
     * 更新新标签内容
     * @param content 标签内容
     */
    data class UpdateNewTagContent(val content: String) : BrainTagUiEvent

    /**
     * 更新选中的标签类型
     * @param type 标签类型
     */
    data class UpdateSelectedTagType(val type: TagType) : BrainTagUiEvent

    /**
     * 确认添加标签
     */
    data object ConfirmAddTag : BrainTagUiEvent

    // === 通用事件 ===

    /**
     * 清除错误
     */
    data object ClearError : BrainTagUiEvent

    /**
     * 导航返回
     */
    data object NavigateBack : BrainTagUiEvent
}
