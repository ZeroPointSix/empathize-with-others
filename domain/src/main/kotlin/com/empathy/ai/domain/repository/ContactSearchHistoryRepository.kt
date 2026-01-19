package com.empathy.ai.domain.repository

/**
 * 联系人搜索历史仓库
 *
 * 用于持久化联系人搜索关键词并提供最近搜索列表。
 */
interface ContactSearchHistoryRepository {

    /**
     * 读取搜索历史列表（按时间倒序）
     */
    suspend fun getHistory(): Result<List<String>>

    /**
     * 保存搜索关键词并返回更新后的历史列表
     */
    suspend fun saveQuery(query: String): Result<List<String>>

    /**
     * 清空搜索历史
     */
    suspend fun clearHistory(): Result<Unit>
}
