package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.FactCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fact搜索过滤器
 *
 * 根据搜索关键词过滤分类和标签
 * 纯Kotlin实现，无Android依赖
 */
@Singleton
class FactSearchFilter @Inject constructor() {

    /**
     * 根据搜索关键词过滤分类列表
     *
     * 过滤规则：
     * 1. 空查询返回原列表
     * 2. 分类名称匹配返回完整分类
     * 3. 标签值匹配只返回匹配的标签
     * 4. 无匹配返回空列表
     * 5. 搜索不区分大小写
     *
     * @param categories 分类列表
     * @param query 搜索关键词
     * @return 过滤后的分类列表
     */
    fun filter(categories: List<FactCategory>, query: String): List<FactCategory> {
        // 空查询返回原列表
        if (query.isBlank()) {
            return categories
        }

        val trimmedQuery = query.trim()
        val result = mutableListOf<FactCategory>()

        for (category in categories) {
            // 分类名称匹配返回完整分类
            if (matches(category.key, trimmedQuery)) {
                result.add(category)
                continue
            }

            // 标签值匹配只返回匹配的标签
            val matchingFacts = category.facts.filter { fact ->
                matches(fact.value, trimmedQuery)
            }

            if (matchingFacts.isNotEmpty()) {
                result.add(
                    category.copy(
                        facts = matchingFacts,
                        // 搜索结果默认展开
                        isExpanded = true
                    )
                )
            }
        }

        return result
    }

    /**
     * 检查文本是否匹配搜索关键词
     *
     * 不区分大小写的部分匹配
     *
     * @param text 要检查的文本
     * @param query 搜索关键词
     * @return 是否匹配
     */
    fun matches(text: String, query: String): Boolean {
        return text.contains(query, ignoreCase = true)
    }

    /**
     * 高亮匹配的文本
     *
     * 返回匹配位置的起始和结束索引列表
     *
     * @param text 原始文本
     * @param query 搜索关键词
     * @return 匹配位置列表，每个元素为 Pair(startIndex, endIndex)
     */
    fun findMatchRanges(text: String, query: String): List<Pair<Int, Int>> {
        if (query.isBlank()) {
            return emptyList()
        }

        val ranges = mutableListOf<Pair<Int, Int>>()
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var startIndex = 0

        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) break
            ranges.add(Pair(index, index + query.length))
            startIndex = index + 1
        }

        return ranges
    }
}
