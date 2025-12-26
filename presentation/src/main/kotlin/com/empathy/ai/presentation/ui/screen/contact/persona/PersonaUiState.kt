package com.empathy.ai.presentation.ui.screen.contact.persona

import com.empathy.ai.presentation.theme.TagCategory
import com.empathy.ai.presentation.ui.component.persona.InferredTag

/**
 * 画像标签数据类
 */
data class PersonaTag(
    val id: String,
    val name: String,
    val category: String,
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 画像库页面UI状态
 * 
 * @param searchQuery 搜索关键词
 * @param expandedCategories 展开的分类集合（字符串形式）
 * @param categoryTags 各分类的标签映射
 * @param inferredTags AI推测的标签列表
 * @param isLoading 是否加载中
 * @param error 错误信息
 * 
 * @see TDD-00020 8.3 PersonaTab状态管理
 */
data class PersonaUiState(
    val searchQuery: String = "",
    val expandedCategories: Set<String> = emptySet(),
    val categoryTags: Map<String, List<PersonaTag>> = emptyMap(),
    val inferredTags: List<InferredTag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 是否有搜索关键词
     */
    val hasSearchQuery: Boolean
        get() = searchQuery.isNotBlank()
    
    /**
     * 是否有AI推测标签
     */
    val hasInferredTags: Boolean
        get() = inferredTags.isNotEmpty()
    
    /**
     * 是否有错误
     */
    val hasError: Boolean
        get() = error != null
    
    /**
     * 获取过滤后的分类标签映射
     */
    val filteredCategoryTags: Map<String, List<PersonaTag>>
        get() {
            if (searchQuery.isBlank()) return categoryTags
            
            return categoryTags.mapValues { (_, tags) ->
                tags.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }
    
    /**
     * 检查分类是否展开
     */
    fun isCategoryExpanded(category: String): Boolean {
        return category in expandedCategories
    }
    
    /**
     * 切换分类展开状态
     */
    fun toggleCategory(category: String): PersonaUiState {
        val newExpanded = if (category in expandedCategories) {
            expandedCategories - category
        } else {
            expandedCategories + category
        }
        return copy(expandedCategories = newExpanded)
    }
    
    /**
     * 获取指定分类的标签列表
     */
    fun getTagsForCategory(category: String): List<PersonaTag> {
        return categoryTags[category] ?: emptyList()
    }
    
    /**
     * 获取指定分类的标签数量
     */
    fun getTagCountForCategory(category: String): Int {
        return categoryTags[category]?.size ?: 0
    }
    
    /**
     * 移除AI推测标签
     */
    fun removeInferredTag(tagId: String): PersonaUiState {
        return copy(inferredTags = inferredTags.filter { it.id != tagId })
    }
    
    /**
     * 获取过滤后的AI推测标签
     */
    fun getFilteredInferredTags(): List<InferredTag> {
        return if (searchQuery.isBlank()) {
            inferredTags
        } else {
            inferredTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
}
