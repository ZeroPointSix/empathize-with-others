package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.CategoryColor
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactCategory
import com.empathy.ai.domain.util.CategoryColorAssigner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 按分类分组Fact用例
 *
 * 将Fact列表按key字段分组，并为每个分类分配颜色
 */
@Singleton
class GroupFactsByCategoryUseCase @Inject constructor(
    private val colorAssigner: CategoryColorAssigner
) {
    /**
     * 将Fact列表按分类分组
     *
     * @param facts Fact列表
     * @param isDarkMode 是否为深色模式
     * @return 分组后的FactCategory列表
     */
    operator fun invoke(facts: List<Fact>, isDarkMode: Boolean): List<FactCategory> {
        if (facts.isEmpty()) {
            return emptyList()
        }

        // 按key分组
        val groupedFacts = facts.groupBy { it.key }

        // 转换为FactCategory列表
        return groupedFacts.map { (key, factsInCategory) ->
            FactCategory(
                key = key,
                // 分类内标签按时间倒序排列
                facts = factsInCategory.sortedByDescending { it.timestamp },
                color = colorAssigner.assignColor(key, isDarkMode),
                isExpanded = true
            )
        }
            // 分类按名称排序
            .sortedBy { it.key }
    }

    /**
     * 保持现有展开状态的分组
     *
     * @param facts Fact列表
     * @param isDarkMode 是否为深色模式
     * @param existingCategories 现有的分类列表（用于保持展开状态）
     * @return 分组后的FactCategory列表
     */
    fun invokeWithState(
        facts: List<Fact>,
        isDarkMode: Boolean,
        existingCategories: List<FactCategory>
    ): List<FactCategory> {
        val newCategories = invoke(facts, isDarkMode)
        
        // 创建现有分类的展开状态映射
        val expandedStateMap = existingCategories.associate { it.key to it.isExpanded }
        
        // 应用现有的展开状态
        return newCategories.map { category ->
            val existingExpanded = expandedStateMap[category.key]
            if (existingExpanded != null) {
                category.copy(isExpanded = existingExpanded)
            } else {
                category
            }
        }
    }
}
