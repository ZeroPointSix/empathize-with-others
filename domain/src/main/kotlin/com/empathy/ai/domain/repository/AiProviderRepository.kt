package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConnectionTestResult
import kotlinx.coroutines.flow.Flow

/**
 * AI 服务商仓库接口
 *
 * 定义 AI 服务商配置的数据访问操作
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
     * 如果 ID 已存在则更新，否则插入新记录
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
     * 发送真实的 API 请求验证配置是否正确
     *
     * @param provider 要测试的服务商
     * @return Result 包含详细的测试结果
     */
    suspend fun testConnection(provider: AiProvider): Result<ConnectionTestResult>

    /**
     * 获取服务商可用的模型列表
     *
     * 调用 OpenAI 兼容的 /models 端点获取可用模型
     *
     * @param provider 要查询的服务商（需要 baseUrl 和 apiKey）
     * @return Result 包含可用模型列表，失败时返回错误信息
     *
     * @see SR-00001 模型列表自动获取与调试日志优化
     */
    suspend fun fetchAvailableModels(provider: AiProvider): Result<List<AiModel>>
}
