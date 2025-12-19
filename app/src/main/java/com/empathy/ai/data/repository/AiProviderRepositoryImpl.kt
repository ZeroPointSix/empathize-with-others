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
     * 注意：整个网络操作都在 IO 线程执行，避免 NetworkOnMainThreadException
     * @see BUG-00016 获取模型列表主线程网络异常分析
     *
     * @param provider 要测试的服务商
     * @return 详细的测试结果
     */
    override suspend fun testConnection(provider: AiProvider): Result<com.empathy.ai.domain.model.ConnectionTestResult> {
        // 基本验证（可以在任何线程执行）
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

        // 构建测试 URL（纯计算，可以在任何线程执行）
        val testUrl = buildModelsUrl(provider.baseUrl)
        android.util.Log.d("AiProviderRepositoryImpl", "测试连接 URL: $testUrl")

        // 所有网络操作都在 IO 线程执行
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
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
                val response = client.newCall(request).execute()

                // 计算延迟
                val latencyMs = System.currentTimeMillis() - startTime

                // 解析响应（现在在 IO 线程，可以安全读取响应体）
                val result = when (response.code) {
                    200 -> {
                        response.close()
                        com.empathy.ai.domain.model.ConnectionTestResult.success(latencyMs)
                    }
                    401 -> {
                        response.close()
                        com.empathy.ai.domain.model.ConnectionTestResult.failure(
                            com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.INVALID_API_KEY,
                            "API Key 无效"
                        )
                    }
                    403 -> {
                        response.close()
                        com.empathy.ai.domain.model.ConnectionTestResult.failure(
                            com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.INVALID_API_KEY,
                            "API Key 权限不足"
                        )
                    }
                    404 -> {
                        response.close()
                        com.empathy.ai.domain.model.ConnectionTestResult.failure(
                            com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.ENDPOINT_UNREACHABLE,
                            "API 端点不存在"
                        )
                    }
                    429 -> {
                        response.close()
                        com.empathy.ai.domain.model.ConnectionTestResult.failure(
                            com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.QUOTA_EXCEEDED,
                            "API 配额已用尽"
                        )
                    }
                    else -> {
                        val errorBody = response.body?.string() ?: "未知错误"
                        response.close()
                        com.empathy.ai.domain.model.ConnectionTestResult.failure(
                            com.empathy.ai.domain.model.ConnectionTestResult.ErrorType.UNKNOWN,
                            "HTTP ${response.code}: $errorBody"
                        )
                    }
                }

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
    }

    /**
     * 获取服务商可用的模型列表
     *
     * 调用 OpenAI 兼容的 /models 端点获取可用模型
     * 自动过滤非聊天模型（如 embedding、whisper、dall-e 等）
     *
     * 注意：整个网络操作都在 IO 线程执行，避免 NetworkOnMainThreadException
     * @see BUG-00016 获取模型列表主线程网络异常分析
     *
     * @param provider 要查询的服务商（需要 baseUrl 和 apiKey）
     * @return Result 包含可用模型列表，失败时返回错误信息
     *
     * @see SR-00001 模型列表自动获取与调试日志优化
     */
    override suspend fun fetchAvailableModels(provider: AiProvider): Result<List<AiModel>> {
        // 基本验证（可以在任何线程执行）
        if (provider.baseUrl.isBlank() || provider.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("服务商配置不完整：需要 baseUrl 和 apiKey"))
        }

        // 构建 Models API URL（纯计算，可以在任何线程执行）
        val modelsUrl = buildModelsUrl(provider.baseUrl)
        android.util.Log.d("AiProviderRepositoryImpl", "获取模型列表 URL: $modelsUrl")

        // 所有网络操作都在 IO 线程执行
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 创建 OkHttp 客户端
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                // 构建请求
                val request = okhttp3.Request.Builder()
                    .url(modelsUrl)
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                // 执行请求
                val response = client.newCall(request).execute()

                // 处理响应（现在在 IO 线程，可以安全读取响应体）
                when (response.code) {
                    200 -> {
                        val responseBody = response.body?.string()
                        response.close()

                        if (responseBody.isNullOrBlank()) {
                            return@withContext Result.failure(Exception("服务商返回空响应"))
                        }

                        // 解析响应
                        val modelsResponse = parseModelsResponse(responseBody)
                        if (modelsResponse == null) {
                            return@withContext Result.failure(Exception("无法解析模型列表响应"))
                        }

                        // 过滤并转换模型
                        val chatModels = modelsResponse.data
                            .filter { isChatModel(it.id) }
                            .map { dto ->
                                AiModel(
                                    id = dto.id,
                                    displayName = generateDisplayName(dto.id)
                                )
                            }
                            .sortedBy { it.id }

                        android.util.Log.d("AiProviderRepositoryImpl", "获取到 ${chatModels.size} 个聊天模型")
                        Result.success(chatModels)
                    }
                    401 -> {
                        response.close()
                        Result.failure(Exception("API Key 无效（401）"))
                    }
                    403 -> {
                        response.close()
                        Result.failure(Exception("API Key 权限不足（403）"))
                    }
                    404 -> {
                        response.close()
                        Result.failure(Exception("该服务商不支持获取模型列表（404）"))
                    }
                    429 -> {
                        response.close()
                        Result.failure(Exception("API 配额已用尽（429）"))
                    }
                    else -> {
                        val errorBody = response.body?.string() ?: "未知错误"
                        response.close()
                        Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(Exception("请求超时，请检查网络连接"))
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("无法解析主机名，请检查 API 端点"))
            } catch (e: java.net.ConnectException) {
                Result.failure(Exception("网络连接失败"))
            } catch (e: Exception) {
                android.util.Log.e("AiProviderRepositoryImpl", "获取模型列表失败", e)
                Result.failure(Exception("获取模型列表失败：${e.message}"))
            }
        }
    }

    /**
     * 解析模型列表响应
     */
    private fun parseModelsResponse(json: String): com.empathy.ai.data.remote.model.ModelsResponseDto? {
        return try {
            val adapter = moshi.adapter(com.empathy.ai.data.remote.model.ModelsResponseDto::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            android.util.Log.e("AiProviderRepositoryImpl", "解析模型列表响应失败", e)
            null
        }
    }

    /**
     * 判断是否为聊天模型
     *
     * 过滤掉非聊天模型（embedding、whisper、dall-e、tts 等）
     */
    private fun isChatModel(modelId: String): Boolean {
        val lowerCaseId = modelId.lowercase()

        // 排除的模型类型
        val excludePatterns = listOf(
            "embedding",
            "whisper",
            "dall-e",
            "tts",
            "text-embedding",
            "ada",
            "babbage",
            "curie",
            "davinci",
            "moderation"
        )

        // 如果包含排除关键词，则不是聊天模型
        if (excludePatterns.any { lowerCaseId.contains(it) }) {
            return false
        }

        // 包含的模型类型（聊天模型关键词）
        val includePatterns = listOf(
            "gpt",
            "chat",
            "turbo",
            "claude",
            "deepseek",
            "qwen",
            "llama",
            "mistral",
            "gemini",
            "palm"
        )

        // 如果包含聊天模型关键词，则是聊天模型
        return includePatterns.any { lowerCaseId.contains(it) }
    }

    /**
     * 根据模型 ID 生成显示名称
     *
     * 将模型 ID 转换为更友好的显示名称
     * 例如：gpt-4-turbo-preview → GPT-4 Turbo Preview
     */
    private fun generateDisplayName(modelId: String): String {
        return modelId
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                when (word.lowercase()) {
                    "gpt" -> "GPT"
                    "turbo" -> "Turbo"
                    "preview" -> "Preview"
                    "mini" -> "Mini"
                    "deepseek" -> "DeepSeek"
                    "chat" -> "Chat"
                    "coder" -> "Coder"
                    else -> word.replaceFirstChar { it.uppercase() }
                }
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
            timeoutMs = entity.timeoutMs,
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
            timeoutMs = provider.timeoutMs,
            createdAt = provider.createdAt
        )
    }
}
