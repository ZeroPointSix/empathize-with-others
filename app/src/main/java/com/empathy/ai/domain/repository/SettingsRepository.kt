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
}
