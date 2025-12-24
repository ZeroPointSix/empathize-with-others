package com.empathy.ai.domain.repository

/**
 * 全局配置仓库接口
 *
 * 服务对象: 网络模块 (NetworkModule)、设置页面
 * 使用 EncryptedSharedPreferences 存储敏感配置
 */
interface SettingsRepository {
    /**
     * 获取 API Key
     *
     * @return 包含 API Key 或 null 的 Result (加密存储)
     */
    suspend fun getApiKey(): Result<String?>

    /**
     * 保存 API Key
     *
     * @param key API Key
     * @return 操作结果
     */
    suspend fun saveApiKey(key: String): Result<Unit>

    /**
     * 获取当前选择的 AI 服务商
     *
     * @return 包含服务商名称的 Result (OpenAI / DeepSeek / Claude)
     */
    suspend fun getAiProvider(): Result<String>

    /**
     * 保存 AI 服务商选择
     *
     * @param provider 服务商名称
     * @return 操作结果
     */
    suspend fun saveAiProvider(provider: String): Result<Unit>

    /**
     * 获取 BaseUrl
     *
     * 用于 Retrofit 动态注入
     *
     * @return 包含 BaseUrl 的 Result
     */
    suspend fun getBaseUrl(): Result<String>

    /**
     * 获取特定服务商需要的 Headers
     *
     * 例如: {"Authorization": "Bearer...", "HTTP-Referer": "..."}
     *
     * @return 包含 Headers Map 的 Result
     */
    suspend fun getProviderHeaders(): Result<Map<String, String>>
    
    /**
     * 获取数据掩码启用状态
     *
     * @return 包含启用状态的 Result，默认为 true
     */
    suspend fun getDataMaskingEnabled(): Result<Boolean>
    
    /**
     * 设置数据掩码启用状态
     *
     * @param enabled 是否启用
     * @return 操作结果
     */
    suspend fun setDataMaskingEnabled(enabled: Boolean): Result<Unit>
    
    /**
     * 获取本地优先模式启用状态
     *
     * @return 包含启用状态的 Result，默认为 true
     */
    suspend fun getLocalFirstModeEnabled(): Result<Boolean>
    
    /**
     * 设置本地优先模式启用状态
     *
     * @param enabled 是否启用
     * @return 操作结果
     */
    suspend fun setLocalFirstModeEnabled(enabled: Boolean): Result<Unit>

    /**
     * 获取历史对话条数配置
     *
     * 用于AI分析时回顾历史对话的条数
     *
     * @return 0/5/10，默认5
     */
    suspend fun getHistoryConversationCount(): Result<Int>

    /**
     * 设置历史对话条数
     *
     * @param count 条数，必须是 0/5/10 之一
     * @return 操作结果
     */
    suspend fun setHistoryConversationCount(count: Int): Result<Unit>

    /**
     * 获取最后总结日期
     *
     * 用于判断是否需要执行每日自动总结
     *
     * @return 最后总结日期字符串（格式：yyyy-MM-dd），如果从未总结过返回null
     */
    suspend fun getLastSummaryDate(): Result<String?>

    /**
     * 设置最后总结日期
     *
     * @param date 日期字符串（格式：yyyy-MM-dd）
     * @return 操作结果
     */
    suspend fun setLastSummaryDate(date: String): Result<Unit>
}
