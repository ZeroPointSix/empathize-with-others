package com.empathy.ai.integration

import com.empathy.ai.data.remote.api.OpenAiApi
import com.empathy.ai.data.remote.model.ChatRequestDto
import com.empathy.ai.data.remote.model.MessageDto
import com.empathy.ai.data.remote.model.ResponseFormatDto
import com.empathy.ai.data.repository.AiRepositoryImpl
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConnectionTestResult
import com.empathy.ai.domain.repository.AiProviderRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * 真实 AI 模型集成测试
 * 
 * 测试目标：
 * 1. 测试多个真实 AI 服务商的模型
 * 2. 验证解析成功率 ≥ 95%
 * 3. 测试不同模型的响应格式兼容性
 * 
 * 测试的服务商：
 * - x666.me (多模型代理)
 * - ModelScope (阿里云模型服务)
 */
class RealAiModelIntegrationTest {

    private lateinit var moshi: Moshi
    private lateinit var okHttpClient: OkHttpClient
    
    // 测试配置
    private val testProviders = listOf(
        TestProvider(
            name = "x666.me",
            baseUrl = "https://x666.me",
            apiKey = "sk-BaN9AcQXfzNmMy12i4nYpQonxm78loqFrQbt4LGBzlxMKpVD",
            models = listOf(
                "gpt-4o-mini",
                "gpt-4.1-mini",
                "gpt-4.1-nano",
                "gemini-2.5-flash"
            )
        ),
        TestProvider(
            name = "ModelScope",
            baseUrl = "https://api-inference.modelscope.cn",
            apiKey = "ms-fc7e77c9-c7cd-4b41-81e1-a567e01e49b5",
            models = listOf(
                "Qwen/Qwen3-235B-A22B-Thinking-2507",
                "Qwen/Qwen3-Coder-480B-A35B-Instruct",
                "Qwen/Qwen3-235B-A22B-Instruct-2507",
                "MiniMax/MiniMax-M1-80k",
                "deepseek-ai/DeepSeek-R1"
            )
        )
    )
    
    @Before
    fun setup() {
        // 配置 Moshi
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        // 配置 OkHttpClient（增加超时时间，添加日志）
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    /**
     * 测试 1: 列出预配置的模型
     * 
     * 显示将要测试的所有模型
     */
    @Test
    fun `test list preconfigured models`() {
        println("\n=== 测试 1: 预配置的模型列表 ===\n")
        
        testProviders.forEach { provider ->
            println("服务商: ${provider.name}")
            println("Base URL: ${provider.baseUrl}")
            println("模型数量: ${provider.models.size}")
            println("模型列表:")
            provider.models.forEach { model ->
                println("  - $model")
            }
            println()
        }
    }
    
    /**
     * 测试 2: 测试 AnalysisResult 解析
     * 
     * 验证：
     * - 不同模型返回的响应能够正确解析
     * - 解析成功率 ≥ 95%
     */
    @Test
    fun `test AnalysisResult parsing with real models`() = runBlocking {
        println("\n=== 测试 2: AnalysisResult 解析测试 ===\n")
        
        val testPrompt = """
            【对话上下文】
            用户: 今天钓鱼没收获，有点沮丧
            
            【联系人画像】
            姓名: 李四
            爱好: 钓鱼
            雷区: 不要提钱
            策略: 多夸他衣品好
            
            请分析这段对话并给出建议。
        """.trimIndent()
        
        var totalTests = 0
        var successfulTests = 0
        val failedModels = mutableListOf<String>()
        
        testProviders.forEach { provider ->
            println("测试服务商: ${provider.name}")
            println("=" .repeat(50))
            
            val api = createApiService(provider.baseUrl)
            val mockProviderRepo = MockProviderRepository(provider)
            val repository = AiRepositoryImpl(api, mockProviderRepo)
            
            provider.models.forEach { modelId ->
                totalTests++
                println("\n测试模型: $modelId")
                
                try {
                    // 更新 mock 仓库的模型 ID
                    mockProviderRepo.updateModelId(modelId)
                    
                    // 调用分析接口
                    val result = repository.analyzeChat(
                        promptContext = testPrompt,
                        systemInstruction = ""
                    )
                    
                    if (result.isSuccess) {
                        val analysisResult = result.getOrNull()!!
                        println("✅ 解析成功")
                        println("  回复建议: ${analysisResult.replySuggestion.take(50)}...")
                        println("  策略分析: ${analysisResult.strategyAnalysis.take(50)}...")
                        println("  风险等级: ${analysisResult.riskLevel}")
                        successfulTests++
                    } else {
                        println("❌ 解析失败: ${result.exceptionOrNull()?.message}")
                        failedModels.add("${provider.name}/$modelId")
                    }
                    
                } catch (e: Exception) {
                    println("❌ 测试异常: ${e.message}")
                    failedModels.add("${provider.name}/$modelId")
                }
                
                // 避免请求过快
                kotlinx.coroutines.delay(1000)
            }
            
            println()
        }
        
        // 计算成功率
        val successRate = (successfulTests.toDouble() / totalTests) * 100
        
        println("\n" + "=".repeat(50))
        println("测试总结")
        println("=".repeat(50))
        println("总测试数: $totalTests")
        println("成功数: $successfulTests")
        println("失败数: ${totalTests - successfulTests}")
        println("成功率: ${"%.2f".format(successRate)}%")
        
        if (failedModels.isNotEmpty()) {
            println("\n失败的模型:")
            failedModels.forEach { println("  - $it") }
        }
        
        println()
        
        // 验证成功率 ≥ 95%
        assertTrue(
            successRate >= 95.0,
            "解析成功率 (${"%.2f".format(successRate)}%) 低于要求的 95%"
        )
    }
    
    /**
     * 测试 3: 测试 SafetyCheckResult 解析
     */
    @Test
    fun `test SafetyCheckResult parsing with real models`() = runBlocking {
        println("\n=== 测试 3: SafetyCheckResult 解析测试 ===\n")
        
        val testDraft = "你最近赚了多少钱？"
        val riskRules = listOf("不要提钱", "避免谈论收入")
        
        var totalTests = 0
        var successfulTests = 0
        
        testProviders.forEach { provider ->
            println("测试服务商: ${provider.name}")
            
            val api = createApiService(provider.baseUrl)
            val mockProviderRepo = MockProviderRepository(provider)
            val repository = AiRepositoryImpl(api, mockProviderRepo)
            
            // 只测试每个服务商的第一个模型（节省时间）
            val modelId = provider.models.first()
            totalTests++
            
            println("测试模型: $modelId")
            
            try {
                mockProviderRepo.updateModelId(modelId)
                
                val result = repository.checkDraftSafety(
                    draft = testDraft,
                    riskRules = riskRules
                )
                
                if (result.isSuccess) {
                    val safetyResult = result.getOrNull()!!
                    println("✅ 解析成功")
                    println("  是否安全: ${safetyResult.isSafe}")
                    println("  触发的风险: ${safetyResult.triggeredRisks}")
                    println("  建议: ${safetyResult.suggestion}")
                    successfulTests++
                } else {
                    println("❌ 解析失败: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                println("❌ 测试异常: ${e.message}")
            }
            
            println()
            kotlinx.coroutines.delay(1000)
        }
        
        val successRate = (successfulTests.toDouble() / totalTests) * 100
        println("SafetyCheckResult 解析成功率: ${"%.2f".format(successRate)}%")
        
        assertTrue(
            successRate >= 95.0,
            "SafetyCheckResult 解析成功率低于 95%"
        )
    }
    
    // ========== 辅助方法 ==========
    
    private fun createApiService(baseUrl: String): OpenAiApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        
        return retrofit.create(OpenAiApi::class.java)
    }
    
    private fun normalizeBaseUrl(baseUrl: String): String {
        var url = baseUrl.trimEnd('/')
        
        val suffixesToRemove = listOf(
            "/v1/chat/completions",
            "/chat/completions",
            "/v1/models",
            "/models",
            "/v1"
        )
        
        for (suffix in suffixesToRemove) {
            if (url.endsWith(suffix)) {
                url = url.removeSuffix(suffix)
                break
            }
        }
        
        // 确保以 / 结尾（Retrofit 要求）
        return if (url.endsWith("/")) url else "$url/"
    }
    

    
    // ========== 数据类 ==========
    
    data class TestProvider(
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val models: List<String>
    )
    
    /**
     * Mock AiProviderRepository 用于测试
     */
    class MockProviderRepository(
        private val testProvider: TestProvider
    ) : AiProviderRepository {
        
        private var currentModelId = testProvider.models.first()
        
        fun updateModelId(modelId: String) {
            currentModelId = modelId
        }
        
        private fun createAiProvider(): AiProvider {
            return AiProvider(
                id = "test-provider-1",
                name = testProvider.name,
                baseUrl = testProvider.baseUrl,
                apiKey = testProvider.apiKey,
                models = testProvider.models.map { AiModel(id = it) },
                defaultModelId = currentModelId,
                isDefault = true
            )
        }
        
        override suspend fun getDefaultProvider(): Result<AiProvider?> {
            return Result.success(createAiProvider())
        }
        
        override fun getAllProviders(): Flow<List<AiProvider>> {
            return flowOf(listOf(createAiProvider()))
        }
        
        override suspend fun getProvider(id: String): Result<AiProvider?> {
            return Result.success(createAiProvider())
        }
        
        override suspend fun saveProvider(provider: AiProvider): Result<Unit> {
            return Result.success(Unit)
        }
        
        override suspend fun deleteProvider(id: String): Result<Unit> {
            return Result.success(Unit)
        }
        
        override suspend fun setDefaultProvider(id: String): Result<Unit> {
            return Result.success(Unit)
        }
        
        override suspend fun testConnection(provider: AiProvider): Result<ConnectionTestResult> {
            return Result.success(
                ConnectionTestResult.success(latencyMs = 100)
            )
        }
    }
}
