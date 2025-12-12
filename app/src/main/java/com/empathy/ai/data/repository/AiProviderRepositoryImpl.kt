package com.empathy.ai.data.repository

import com.empathy.ai.data.local.ApiKeyStorage
import com.empathy.ai.data.local.dao.AiProviderDao
import com.empathy.ai.data.local.entity.AiProviderEntity
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.repository.AiProviderRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * AI 服务商仓库实现类
 *
 * 这是连接 Domain 层（纯 Kotlin）和 Data 层（Android/SQL）的桥梁。
 *
 * 工作流程:
 * 1. saveProvider: Domain 对象 → 拆包 → 转换 → 封装成 Entity → DAO 写入 + API Key 加密存储
 * 2. getAllProviders: DAO 查询 (Flow) → 数据清洗 → 还原 → 返回 Domain 对象 Flow
 * 3. deleteProvider: DAO 删除 + API Key 清理
 *
 * 映射规范（写在文件底部）:
 * - entityToDomain(): Entity → Domain Model（需要从 ApiKeyStorage 读取 API Key）
 * - domainToEntity(): Domain Model → Entity（需要将 API Key 保存到 ApiKeyStorage）
 *
 * @property dao AI 服务商 DAO
 * @property apiKeyStorage API Key 加密存储
 * @property moshi JSON 序列化工具
 */
class AiProviderRepositoryImpl @Inject constructor(
    private val dao: AiProviderDao,
    private val apiKeyStorage: ApiKeyStorage,
    private val moshi: Moshi
) : AiProviderRepository {

    private val listType = Types.newParameterizedType(List::class.java, AiModel::class.java)
    private val modelsAdapter = moshi.adapter<List<AiModel>>(listType)

    /**
     * 获取所有服务商
     *
     * 1. 对接管道: 调用 dao.getAllProviders() 拿到 Flow<List<Entity>>
     * 2. 数据清洗: 使用 .map 操作符转换数据流
     * 3. 还原: 遍历 List, 把每个 Entity 转换成 Domain Model（包括读取 API Key）
     * 4. 性能优化: 使用 distinctUntilChanged 避免重复发射相同数据
     * 5. 交付: 最终吐出 Flow<List<AiProvider>> 给 UseCase
     *
     * @return 服务商列表的 Flow
     */
    override fun getAllProviders(): Flow<List<AiProvider>> {
        return dao.getAllProviders()
            .map { entities ->
                entities.map { entityToDomain(it) }
            }
            .distinctUntilChanged()
    }

    /**
     * 根据 ID 获取单个服务商
     *
     * @param id 服务商 ID
     * @return 包含服务商或 null 的 Result
     */
    override suspend fun getProvider(id: String): Result<AiProvider?> {
        return try {
            val entity = dao.getProviderById(id)
            Result.success(entity?.let { entityToDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取默认服务商
     *
     * @return 包含默认服务商或 null 的 Result
     */
    override suspend fun getDefaultProvider(): Result<AiProvider?> {
        return try {
            val entity = dao.getDefaultProvider().first()
            Result.success(entity?.let { entityToDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存服务商
     *
     * 工作流程:
     * 1. 将 Domain Model 转换为 Entity
     * 2. 将 API Key 加密存储到 EncryptedSharedPreferences
     * 3. 将 Entity 保存到数据库
     * 4. 如果是默认服务商, 先清除其他服务商的默认标记
     *
     * @param provider 要保存的服务商
     * @return 操作结果
     */
    override suspend fun saveProvider(provider: AiProvider): Result<Unit> {
        return try {
            // 如果是默认服务商, 先清除其他服务商的默认标记
            if (provider.isDefault) {
                dao.clearAllDefaultFlags()
            }

            // 保存 API Key 到加密存储
            val apiKeyRef = apiKeyStorage.generateKey(provider.id)
            apiKeyStorage.save(apiKeyRef, provider.apiKey)

            // 保存 Entity 到数据库
            val entity = domainToEntity(provider, apiKeyRef)
            dao.insertOrUpdate(entity)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除服务商
     *
     * 工作流程:
     * 1. 从数据库删除 Entity
     * 2. 从 EncryptedSharedPreferences 删除 API Key
     *
     * @param id 服务商 ID
     * @return 操作结果
     */
    override suspend fun deleteProvider(id: String): Result<Unit> {
        return try {
            // 删除数据库记录
            dao.deleteById(id)

            // 删除加密存储的 API Key
            val apiKeyRef = apiKeyStorage.generateKey(id)
            apiKeyStorage.delete(apiKeyRef)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 设置默认服务商
     *
     * 工作流程:
     * 1. 清除所有服务商的默认标记
     * 2. 设置指定服务商为默认
     *
     * @param id 服务商 ID
     * @return 操作结果
     */
    override suspend fun setDefaultProvider(id: String): Result<Unit> {
        return try {
            dao.clearAllDefaultFlags()
            dao.setDefaultById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 构建 Models API URL（用于测试连接）
     *
     * 智能处理 baseUrl，自动标准化为完整的 API 端点
     * 与 AiRepositoryImpl.buildChatCompletionsUrl() 使用相同的标准化逻辑
     *
     * 用户输入示例及处理结果：
     * - https://api.example.com → https://api.example.com/v1/models
     * - https://api.example.com/v1 → https://api.example.com/v1/models
     * - https://api.example.com/v1/chat/completions → https://api.example.com/v1/models
     * - https://api.example.com/chat/completions → https://api.example.com/v1/models
     *
     * @param baseUrl 用户配置的基础 URL
     * @return 完整的 Models API URL
     */
    private fun buildModelsUrl(baseUrl: String): String {
        val trimmedUrl = baseUrl.trimEnd('/')
        
        return when {
            // 已经是完整的 models 路径
            trimmedUrl.endsWith("/v1/models") -> trimmedUrl
            trimmedUrl.endsWith("/models") -> trimmedUrl
            
            // 已经包含 /v1，只需追加 /models
            trimmedUrl.endsWith("/v1") -> "$trimmedUrl/models"
            
            // 移除可能的 chat/completions 后缀，然后追加 /v1/models
            trimmedUrl.endsWith("/v1/chat/completions") -> 
                trimmedUrl.removeSuffix("/chat/completions") + "/models"
            trimmedUrl.endsWith("/chat/completions") -> 
                trimmedUrl.removeSuffix("/chat/completions") + "/v1/models"
            
            // 基础 URL，需要追加完整路径 /v1/models
            else -> "$trimmedUrl/v1/models"
        }
    }

    /**
     * 测试连接
     *
     * 发送真实的 API 请求验证配置是否正确
     * 使用 OpenAI 兼容的 /v1/models 端点进行测试
     * 
     * URL 构建逻辑与 AiRepositoryImpl 保持一致，确保测试结果准确反映实际请求
     *
     * @param provider 要测试的服务商
     * @return 详细的测试结果
     */
    override suspend fun testConnection(provider: AiProvider): Result<com.empathy.ai.domain.model.ConnectionTestResult> {
        return try {
            // 基本验证
            if (!provider.isValid()) {
                return Result.success(
                    com.empathy.ai.domain.model.ConnectionTestResult.failure(
                        com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.UNKNOWN,
                        "服务商配置无效"
                    )
                )
            }
            
            // 记录开始时间
            val startTime = System.currentTimeMillis()
            
            // 构建测试 URL（使用统一的 URL 构建逻辑）
            val testUrl = buildModelsUrl(provider.baseUrl)
            android.util.Log.d("AiProviderRepositoryImpl", "测试连接 URL: $testUrl")
            
            // 创建 OkHttp 客户端
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            // 构建请求
            val request = okhttp3.Request.Builder()
                .url(testUrl)
                .addHeader("Authorization", "Bearer ${provider.apiKey}")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()
            
            // 执行请求
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            
            // 计算延迟
            val latencyMs = System.currentTimeMillis() - startTime
            
            // 解析响应
            val result = when (response.code) {
                200 -> {
                    com.empathy.ai.domain.model.ConnectionTestResult.success(latencyMs)
                }
                401 -> {
                    com.empathy.ai.domain.model.ConnectionTestResult.failure(
                        com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.INVALID_API_KEY,
                        "API Key 无效"
                    )
                }
                403 -> {
                    com.empathy.ai.domain.model.ConnectionTestResult.failure(
                        com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.INVALID_API_KEY,
                        "API Key 权限不足"
                    )
                }
                404 -> {
                    com.empathy.ai.domain.model.ConnectionTestResult.failure(
                        com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.ENDPOINT_UNREACHABLE,
                        "API 端点不存在"
                    )
                }
                429 -> {
                    com.empathy.ai.domain.model.ConnectionTestResult.failure(
                        com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.QUOTA_EXCEEDED,
                        "API 配额已用尽"
                    )
                }
                else -> {
                    val errorBody = response.body?.string() ?: "未知错误"
                    com.empathy.ai.domain.model.ConnectionTestResult.failure(
                        com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.UNKNOWN,
                        "HTTP ${response.code}: $errorBody"
                    )
                }
            }
            
            response.close()
            Result.success(result)
            
        } catch (e: java.net.SocketTimeoutException) {
            Result.success(
                com.empathy.ai.domain.model.ConnectionTestResult.failure(
                    com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.TIMEOUT,
                    "请求超时"
                )
            )
        } catch (e: java.net.UnknownHostException) {
            Result.success(
                com.empathy.ai.domain.model.ConnectionTestResult.failure(
                    com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.ENDPOINT_UNREACHABLE,
                    "无法解析主机名"
                )
            )
        } catch (e: java.net.ConnectException) {
            Result.success(
                com.empathy.ai.domain.model.ConnectionTestResult.failure(
                    com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.NETWORK_ERROR,
                    "网络连接失败"
                )
            )
        } catch (e: Exception) {
            Result.success(
                com.empathy.ai.domain.model.ConnectionTestResult.failure(
                    com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.UNKNOWN,
                    e.message ?: "未知错误"
                )
            )
        }
    }

    // ============================================================================
    // 私有映射函数
    // ============================================================================

    /**
     * Entity → Domain Model 转换
     *
     * 把 AiProviderEntity 转换为 Domain 层的 AiProvider。
     * 核心工作:
     * 1. 把 modelsJson (JSON 字符串) 还原成 List<AiModel>
     * 2. 从 ApiKeyStorage 读取加密存储的 API Key
     *
     * @param entity AiProviderEntity 对象
     * @return Domain 层的 AiProvider 对象
     */
    private fun entityToDomain(entity: AiProviderEntity): AiProvider {
        // 解析模型列表
        val models = try {
            modelsAdapter.fromJson(entity.modelsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList<AiModel>()
        }

        // 读取 API Key
        val apiKey = apiKeyStorage.get(entity.apiKeyRef) ?: ""

        return AiProvider(
            id = entity.id,
            name = entity.name,
            baseUrl = entity.baseUrl,
            apiKey = apiKey,
            models = models,
            defaultModelId = entity.defaultModelId,
            isDefault = entity.isDefault,
            createdAt = entity.createdAt
        )
    }

    /**
     * Domain Model → Entity 转换
     *
     * 把 Domain 层的 AiProvider 转换为 AiProviderEntity。
     * 核心工作:
     * 1. 把 models List 转换为 JSON 字符串 (使用 Moshi)
     * 2. 使用提供的 apiKeyRef (API Key 已在外部保存到 ApiKeyStorage)
     *
     * @param provider AiProvider 对象
     * @param apiKeyRef API Key 的存储引用
     * @return Data 层的 AiProviderEntity 对象
     */
    private fun domainToEntity(provider: AiProvider, apiKeyRef: String): AiProviderEntity {
        val modelsJson = modelsAdapter.toJson(provider.models)

        return AiProviderEntity(
            id = provider.id,
            name = provider.name,
            baseUrl = provider.baseUrl,
            apiKeyRef = apiKeyRef,
            modelsJson = modelsJson,
            defaultModelId = provider.defaultModelId,
            isDefault = provider.isDefault,
            createdAt = provider.createdAt
        )
    }
}
