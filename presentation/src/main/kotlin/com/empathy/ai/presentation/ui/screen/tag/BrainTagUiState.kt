package com.empathy.ai.presentation.ui.screen.tag

import com.empathy.ai.domain.model.BrainTag

/**
 * 标签管理界面的UI状态
 *
 * ## 业务职责
 * 封装标签管理页面的所有UI状态，采用不可变数据模式确保状态可预测。
 *
 * ## 状态分组
 * ```
 * BrainTagUiState
 * ├── 通用状态      ← isLoading, error
 * ├── 数据状态      ← tags, filteredTags
 * ├── 搜索状态      ← searchQuery, isSearching
 * ├── 添加对话框    ← showAddDialog, newTagContent, selectedTagType
 * └── 导航状态      ← shouldNavigateBack
 * ```
 *
 * ## 设计原则
 * 1. **不可变性**: 所有字段使用 val，状态变更通过 copy() 实现
 * 2. **空安全**: 所有字段都有默认值，避免空指针异常
 * 3. **派生状态**: displayTags 根据搜索状态动态计算
 *
 * @property isLoading 是否正在加载（显示全屏加载指示器）
 * @property error 错误信息（非空时显示错误对话框）
 * @property tags 原始标签列表
 * @property filteredTags 过滤后的标签列表
 * @property searchQuery 搜索关键词
 * @property isSearching 是否处于搜索模式
 * @property showAddDialog 是否显示添加标签对话框
 * @property newTagContent 新标签内容（对话框输入）
 * @property selectedTagType 选中的标签类型（STRATEGY_GREEN/RISK_RED）
 * @property shouldNavigateBack 是否应返回
 */
data class BrainTagUiState(
    // 通用状态
    val isLoading: Boolean = false,
    val error: String? = null,

    // 数据状态
    val tags: List<BrainTag> = emptyList(),
    val filteredTags: List<BrainTag> = emptyList(),

    // 搜索状态
    val searchQuery: String = "",
    val isSearching: Boolean = false,

    // 添加对话框状态
    val showAddDialog: Boolean = false,
    val newTagContent: String = "",
    val selectedTagType: String = "STRATEGY_GREEN",

    // 导航状态
    val shouldNavigateBack: Boolean = false
) {
    /** 是否有标签数据 */
    val hasTags: Boolean get() = tags.isNotEmpty()

    /** 显示的标签列表（根据搜索状态动态返回） */
    val displayTags: List<BrainTag>
        get() = if (searchQuery.isNotBlank()) filteredTags else tags

    /** 是否为空状态（无数据且非加载中） */
    val isEmptyState: Boolean get() = displayTags.isEmpty() && !isLoading

    /** 是否可以添加标签（内容非空） */
    val canAddTag: Boolean get() = newTagContent.isNotBlank()
}
