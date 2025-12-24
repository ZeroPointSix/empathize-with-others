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
    val hasQuery: Boolean get() = query.isNotBlank()

    fun updateQuery(newQuery: String): PersonaSearchState =
        copy(query = newQuery, isSearching = newQuery.isNotBlank())

    fun clear(): PersonaSearchState = PersonaSearchState()

    fun searchCompleted(): PersonaSearchState = copy(isSearching = false)
}
