package com.empathy.ai.presentation.ui.screen.tag

import com.empathy.ai.domain.model.BrainTag

/**
 * 标签管理界面的UI状态
 *
 * 设计原则：
 * 1. 所有字段都有默认值，避免空指针
 * 2. 使用 data class，自动获得 copy() 方法
 * 3. 状态是不可变的 (val)，通过 copy() 更新
 * 4. 包含所有UI信息：数据 + 加载状态 + 错误信息
 */
data class BrainTagUiState(
    // 通用状态字段
    val isLoading: Boolean = false,
    val error: String? = null,

    // 标签数据
    val tags: List<BrainTag> = emptyList(),
    val filteredTags: List<BrainTag> = emptyList(),

    // 搜索状态
    val searchQuery: String = "",
    val isSearching: Boolean = false,

    // 添加标签对话框状态
    val showAddDialog: Boolean = false,
    val newTagContent: String = "",
    val selectedTagType: String = "STRATEGY_GREEN",

    // 导航状态
    val shouldNavigateBack: Boolean = false
) {
    // 计算属性：是否有标签
    val hasTags: Boolean
        get() = tags.isNotEmpty()

    // 计算属性：显示的标签列表
    val displayTags: List<BrainTag>
        get() = if (searchQuery.isNotBlank()) filteredTags else tags

    // 计算属性：是否为空状态
    val isEmptyState: Boolean
        get() = displayTags.isEmpty() && !isLoading

    // 计算属性：是否可以添加标签
    val canAddTag: Boolean
        get() = newTagContent.isNotBlank()
}
