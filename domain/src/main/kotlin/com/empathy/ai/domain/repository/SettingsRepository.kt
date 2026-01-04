package com.empathy.ai.domain.repository

/**
 * 全局配置仓储接口
 *
 * 业务背景:
 * - 存储应用全局配置，包括API密钥、服务商选择、行为设置等
 * - 敏感配置（API Key）使用EncryptedSharedPreferences加密存储
 * - 区分敏感配置和非敏感配置，采用不同的存储策略
 *
 * 设计决策:
 * - 敏感数据加密：API Key使用硬件级加密
 * - 轻量级存储：非敏感配置使用SharedPreferences
 * - 统一返回Result：支持错误处理和链式调用
 *
 * 服务对象: 网络模块(NetworkModule)、设置页面
 */
interface SettingsRepository {

    /**
     * 获取API Key
     *
     * 存储: EncryptedSharedPreferences（加密存储）
     *
     * @return 包含API Key或null的Result
     */
    suspend fun getApiKey(): Result<String?>

    /**
     * 保存API Key
     *
     * 业务规则:
     * - 保存前验证Key格式
     * - 使用EncryptedSharedPreferences加密存储
     *
     * @param key API Key
     * @return 操作结果
     */
    suspend fun saveApiKey(key: String): Result<Unit>

    /**
     * 获取当前选择的AI服务商
     *
     * @return 包含服务商名称的Result（OpenAI/DeepSeek/Claude等）
     */
    suspend fun getAiProvider(): Result<String>

    /**
     * 保存AI服务商选择
     *
     * @param provider 服务商名称
     * @return 操作结果
     */
    suspend fun saveAiProvider(provider: String): Result<Unit>

    /**
     * 获取BaseUrl
     *
     * 用途: Retrofit动态注入API地址
     *
     * @return 包含BaseUrl的Result
     */
    suspend fun getBaseUrl(): Result<String>

    /**
     * 获取特定服务商需要的Headers
     *
     * 示例: {"Authorization": "Bearer...", "HTTP-Referer": "..."}
     *
     * @return 包含Headers Map的Result
     */
    suspend fun getProviderHeaders(): Result<Map<String, String>>

    /**
     * 获取数据掩码启用状态
     *
     * @return 包含启用状态的Result，默认为true
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
     * @return 包含启用状态的Result，默认为true
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
     * 用途: AI分析时回顾历史对话的条数
     *
     * @return 0/5/10，默认5
     */
    suspend fun getHistoryConversationCount(): Result<Int>

    /**
     * 设置历史对话条数
     *
     * 业务规则: 必须是0/5/10之一
     *
     * @param count 条数
     * @return 操作结果
     */
    suspend fun setHistoryConversationCount(count: Int): Result<Unit>

    /**
     * 获取最后总结日期
     *
     * 用途: 判断是否需要执行每日自动总结
     *
     * @return 最后总结日期字符串（格式：yyyy-MM-dd），从未总结过返回null
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
