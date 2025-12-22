package com.empathy.ai.domain.model

/**
 * 标签画像搜索状态
 *
 * 管理标签画像页面的搜索功能状态
 *
 * @property query 搜索关键词
 * @property isSearching 是否正在搜索（用于显示加载状态）
 */
data class PersonaSearchState(
    val query: String = "",
    val isSearching: Boolean = false
) {
    /**
     * 是否有搜索关键词
     */
    val hasQuery: Boolean
        get() = query.isNotBlank()

    /**
     * 更新搜索关键词
     *
     * @param newQuery 新的搜索关键词
     * @return 新的PersonaSearchState实例
     */
    fun updateQuery(newQuery: String): PersonaSearchState {
        return copy(query = newQuery, isSearching = newQuery.isNotBlank())
    }

    /**
     * 清除搜索
     *
     * @return 新的PersonaSearchState实例（重置为初始状态）
     */
    fun clear(): PersonaSearchState {
        return PersonaSearchState()
    }

    /**
     * 设置搜索完成状态
     *
     * @return 新的PersonaSearchState实例
     */
    fun searchCompleted(): PersonaSearchState {
        return copy(isSearching = false)
    }
}
