package com.empathy.ai.data.repository

import com.empathy.ai.data.local.ApiKeyStorage
import com.empathy.ai.data.local.ProxyPreferences
import com.empathy.ai.data.local.dao.AiProviderDao
import com.empathy.ai.data.local.entity.AiProviderEntity
import com.empathy.ai.data.remote.model.ModelsResponseDto
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConnectionTestResult
import com.empathy.ai.domain.model.ProxyConfig
import com.empathy.ai.domain.model.ProxyType
import com.empathy.ai.domain.repository.AiProviderRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * AI 服务商仓库实现类
 *
 * 这是连接 Domain 层（纯 Kotlin）和 Data 层（Android/SQL）的桥梁。
 */
class AiProviderRepositoryImpl @Inject constructor(
    private val dao: AiProviderDao,
    private val apiKeyStorage: ApiKeyStorage,
    private val proxyPreferences: ProxyPreferences,
    private val moshi: Moshi
) : AiProviderRepository {

    private val listType = Types.newParameterizedType(List::class.java, AiModel::class.java)
    private val modelsAdapter = moshi.adapter<List<AiModel>>(listType)

    override fun getAllProviders(): Flow<List<AiProvider>> {
        return dao.getAllProviders()
            .map { entities -> entities.map { entityToDomain(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getProvider(id: String): Result<AiProvider?> {
        return try {
            val entity = dao.getProviderById(id)
            Result.success(entity?.let { entityToDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDefaultProvider(): Result<AiProvider?> {
        return try {
            val entity = dao.getDefaultProvider().first()
            Result.success(entity?.let { entityToDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveProvider(provider: AiProvider): Result<Unit> {
        return try {
            if (provider.isDefault) {
                dao.clearAllDefaultFlags()
            }
            val apiKeyRef = apiKeyStorage.generateKey(provider.id)
            apiKeyStorage.save(apiKeyRef, provider.apiKey)
            val entity = domainToEntity(provider, apiKeyRef)
            dao.insertOrUpdate(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProvider(id: String): Result<Unit> {
        return try {
            dao.deleteById(id)
            val apiKeyRef = apiKeyStorage.generateKey(id)
            apiKeyStorage.delete(apiKeyRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setDefaultProvider(id: String): Result<Unit> {
        return try {
            dao.clearAllDefaultFlags()
            dao.setDefaultById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildModelsUrl(baseUrl: String): String {
        val trimmedUrl = baseUrl.trimEnd('/')
        return when {
            trimmedUrl.endsWith("/v1/models") -> trimmedUrl
            trimmedUrl.endsWith("/models") -> trimmedUrl
            trimmedUrl.endsWith("/v1") -> "$trimmedUrl/models"
            trimmedUrl.endsWith("/v1/chat/completions") ->
                trimmedUrl.removeSuffix("/chat/completions") + "/models"
            trimmedUrl.endsWith("/chat/completions") ->
                trimmedUrl.removeSuffix("/chat/completions") + "/v1/models"
            else -> "$trimmedUrl/v1/models"
        }
    }

    override suspend fun testConnection(provider: AiProvider): Result<ConnectionTestResult> {
        if (!provider.isValid()) {
            return Result.success(
                ConnectionTestResult.failure(
                    ConnectionTestResult.ErrorType.UNKNOWN,
                    "服务商配置无效"
                )
            )
        }

        val startTime = System.currentTimeMillis()
        val testUrl = buildModelsUrl(provider.baseUrl)

        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(testUrl)
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val latencyMs = System.currentTimeMillis() - startTime

                val result = when (response.code) {
                    200 -> {
                        response.close()
                        ConnectionTestResult.success(latencyMs)
                    }
                    401 -> {
                        response.close()
                        ConnectionTestResult.failure(
                            ConnectionTestResult.ErrorType.INVALID_API_KEY,
                            "API Key 无效"
                        )
                    }
                    403 -> {
                        response.close()
                        ConnectionTestResult.failure(
                            ConnectionTestResult.ErrorType.INVALID_API_KEY,
                            "API Key 权限不足"
                        )
                    }
                    404 -> {
                        response.close()
                        ConnectionTestResult.failure(
                            ConnectionTestResult.ErrorType.ENDPOINT_UNREACHABLE,
                            "API 端点不存在"
                        )
                    }
                    429 -> {
                        response.close()
                        ConnectionTestResult.failure(
                            ConnectionTestResult.ErrorType.QUOTA_EXCEEDED,
                            "API 配额已用尽"
                        )
                    }
                    else -> {
                        val errorBody = response.body?.string() ?: "未知错误"
                        response.close()
                        ConnectionTestResult.failure(
                            ConnectionTestResult.ErrorType.UNKNOWN,
                            "HTTP ${response.code}: $errorBody"
                        )
                    }
                }
                Result.success(result)
            } catch (e: SocketTimeoutException) {
                Result.success(
                    ConnectionTestResult.failure(
                        ConnectionTestResult.ErrorType.TIMEOUT,
                        "请求超时"
                    )
                )
            } catch (e: UnknownHostException) {
                Result.success(
                    ConnectionTestResult.failure(
                        ConnectionTestResult.ErrorType.ENDPOINT_UNREACHABLE,
                        "无法解析主机名"
                    )
                )
            } catch (e: ConnectException) {
                Result.success(
                    ConnectionTestResult.failure(
                        ConnectionTestResult.ErrorType.NETWORK_ERROR,
                        "网络连接失败"
                    )
                )
            } catch (e: Exception) {
                Result.success(
                    ConnectionTestResult.failure(
                        ConnectionTestResult.ErrorType.UNKNOWN,
                        e.message ?: "未知错误"
                    )
                )
            }
        }
    }

    override suspend fun fetchAvailableModels(provider: AiProvider): Result<List<AiModel>> {
        if (provider.baseUrl.isBlank() || provider.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("服务商配置不完整：需要 baseUrl 和 apiKey"))
        }

        val modelsUrl = buildModelsUrl(provider.baseUrl)

        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(modelsUrl)
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                when (response.code) {
                    200 -> {
                        val responseBody = response.body?.string()
                        response.close()

                        if (responseBody.isNullOrBlank()) {
                            return@withContext Result.failure(Exception("服务商返回空响应"))
                        }

                        val modelsResponse = parseModelsResponse(responseBody)
                            ?: return@withContext Result.failure(Exception("无法解析模型列表响应"))

                        val chatModels = modelsResponse.data
                            .filter { isChatModel(it.id) }
                            .map { dto ->
                                AiModel(
                                    id = dto.id,
                                    displayName = generateDisplayName(dto.id)
                                )
                            }
                            .sortedBy { it.id }

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
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("请求超时，请检查网络连接"))
            } catch (e: UnknownHostException) {
                Result.failure(Exception("无法解析主机名，请检查 API 端点"))
            } catch (e: ConnectException) {
                Result.failure(Exception("网络连接失败"))
            } catch (e: Exception) {
                Result.failure(Exception("获取模型列表失败：${e.message}"))
            }
        }
    }

    private fun parseModelsResponse(json: String): ModelsResponseDto? {
        return try {
            val adapter = moshi.adapter(ModelsResponseDto::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun isChatModel(modelId: String): Boolean {
        val lowerCaseId = modelId.lowercase()
        val excludePatterns = listOf(
            "embedding", "whisper", "dall-e", "tts", "text-embedding",
            "ada", "babbage", "curie", "davinci", "moderation"
        )
        if (excludePatterns.any { lowerCaseId.contains(it) }) {
            return false
        }
        val includePatterns = listOf(
            "gpt", "chat", "turbo", "claude", "deepseek",
            "qwen", "llama", "mistral", "gemini", "palm"
        )
        return includePatterns.any { lowerCaseId.contains(it) }
    }

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

    private fun entityToDomain(entity: AiProviderEntity): AiProvider {
        val models = try {
            modelsAdapter.fromJson(entity.modelsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
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
            temperature = entity.temperature,
            maxTokens = entity.maxTokens,
            createdAt = entity.createdAt
        )
    }

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
            temperature = provider.temperature,
            maxTokens = provider.maxTokens,
            createdAt = provider.createdAt
        )
    }

    // ==================== TD-00025: 代理配置相关方法 ====================

    override suspend fun getProxyConfig(): ProxyConfig {
        return proxyPreferences.getProxyConfig()
    }

    override suspend fun saveProxyConfig(config: ProxyConfig) {
        proxyPreferences.saveProxyConfig(config)
    }

    override suspend fun testProxyConnection(config: ProxyConfig): Result<Long> {
        if (!config.isValid()) {
            return Result.failure(IllegalArgumentException("代理配置无效"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                val proxyType = when (config.type) {
                    ProxyType.HTTP, ProxyType.HTTPS -> Proxy.Type.HTTP
                    ProxyType.SOCKS4, ProxyType.SOCKS5 -> Proxy.Type.SOCKS
                }
                
                val proxy = Proxy(proxyType, InetSocketAddress(config.host, config.port))
                
                val clientBuilder = OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                
                // 添加代理认证
                if (config.requiresAuth()) {
                    val proxyAuthenticator = Authenticator { _, response ->
                        val credential = Credentials.basic(config.username, config.password)
                        response.request.newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build()
                    }
                    clientBuilder.proxyAuthenticator(proxyAuthenticator)
                }
                
                val client = clientBuilder.build()
                
                // 测试连接到一个可靠的端点
                val request = Request.Builder()
                    .url("https://www.google.com/generate_204")
                    .head()
                    .build()
                
                val response = client.newCall(request).execute()
                val latencyMs = System.currentTimeMillis() - startTime
                response.close()
                
                if (response.isSuccessful || response.code == 204) {
                    Result.success(latencyMs)
                } else {
                    Result.failure(Exception("代理连接失败: HTTP ${response.code}"))
                }
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("代理连接超时"))
            } catch (e: UnknownHostException) {
                Result.failure(Exception("无法解析代理服务器地址"))
            } catch (e: ConnectException) {
                Result.failure(Exception("无法连接到代理服务器"))
            } catch (e: Exception) {
                Result.failure(Exception("代理测试失败: ${e.message}"))
            }
        }
    }
}
