package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConnectionTestResult
import com.empathy.ai.domain.model.ProxyConfig
import kotlinx.coroutines.flow.Flow

/**
 * AI服务商仓储接口
 *
 * 业务背景:
 * - 支持多服务商配置，用户可自由切换不同的AI服务提供商
 * - 采用OpenAI兼容API标准，支持主流LLM服务
 *
 * 设计决策:
 * - 默认服务商机制：用户可设置首选服务商，分析时优先使用
 * - 动态模型获取：调用服务商API获取可用模型列表
 * - 代理配置支持：TD-00025新增，支持HTTP/SOCKS5代理
 *
 * 任务追踪:
 * - TD-00025: AI配置功能完善 - 服务商与代理管理
 */
interface AiProviderRepository {
    /**
     * 获取所有服务商
     *
     * @return Flow 发射服务商列表
     */
    fun getAllProviders(): Flow<List<AiProvider>>
    
    /**
     * 根据 ID 获取服务商
     *
     * @param id 服务商 ID
     * @return Result 包含服务商或 null（如果不存在）
     */
    suspend fun getProvider(id: String): Result<AiProvider?>
    
    /**
     * 获取默认服务商
     *
     * @return Result 包含默认服务商或 null（如果未设置）
     */
    suspend fun getDefaultProvider(): Result<AiProvider?>
    
    /**
     * 保存服务商
     *
     * 业务规则:
     * - 如果ID已存在则更新，否则插入新记录
     * - 保存时验证API Key格式和BaseUrl可达性
     *
     * @param provider 要保存的服务商
     * @return Result 表示操作成功或失败
     */
    suspend fun saveProvider(provider: AiProvider): Result<Unit>
    
    /**
     * 删除服务商
     *
     * @param id 要删除的服务商 ID
     * @return Result 表示操作成功或失败
     */
    suspend fun deleteProvider(id: String): Result<Unit>
    
    /**
     * 设置默认服务商
     *
     * 将指定服务商设为默认，并取消其他服务商的默认状态
     *
     * @param id 要设为默认的服务商 ID
     * @return Result 表示操作成功或失败
     */
    suspend fun setDefaultProvider(id: String): Result<Unit>
    
    /**
     * 测试服务商连接
     *
     * 设计权衡: 发送真实的API请求验证配置，而非仅验证URL格式
     * 确保用户配置的服务商真正可用，避免AI调用时才发现问题
     *
     * 业务规则: 返回详细的测试结果，包括延迟、状态码、可用模型数
     *
     * @param provider 要测试的服务商
     * @return Result 包含详细的测试结果
     */
    suspend fun testConnection(provider: AiProvider): Result<ConnectionTestResult>

    /**
     * 获取服务商可用的模型列表
     *
     * 设计权衡: 动态获取模型列表而非硬编码
     * 不同服务商、不同时期的模型可能变化，动态获取保证准确性
     *
     * 业务规则: 调用OpenAI兼容的 /models 端点获取可用模型
     * 失败时返回空列表而非抛出异常，保证降级可用
     *
     * @param provider 要查询的服务商（需要 baseUrl 和 apiKey）
     * @return Result 包含可用模型列表，失败时返回空列表
     *
     * @see SR-00001 模型列表自动获取与调试日志优化
     */
    suspend fun fetchAvailableModels(provider: AiProvider): Result<List<AiModel>>

    // ==================== TD-00025: 代理配置相关方法 ====================

    /**
     * 获取代理配置
     *
     * @return 当前代理配置，无配置时返回默认ProxyConfig
     */
    suspend fun getProxyConfig(): ProxyConfig

    /**
     * 保存代理配置
     *
     * 业务规则:
     * - 支持HTTP和SOCKS5两种代理类型
     * - 密码加密存储在EncryptedSharedPreferences
     *
     * @param config 代理配置
     */
    suspend fun saveProxyConfig(config: ProxyConfig)

    /**
     * 测试代理连接
     *
     * 设计权衡: 测试代理连通性而非直接测试AI API
     * 通过访问通用URL验证代理是否正常工作
     *
     * @param config 要测试的代理配置
     * @return Result 包含延迟时间（毫秒）或错误信息
     */
    suspend fun testProxyConnection(config: ProxyConfig): Result<Long>
}
