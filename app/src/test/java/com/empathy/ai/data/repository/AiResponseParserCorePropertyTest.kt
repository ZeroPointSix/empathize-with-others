package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * AI 响应解析器核心属性测试
 * 
 * 测试核心属性：
 * - Property 1: Markdown 清洗一致性
 * - Property 2: JSON 提取正确性
 * - Property 3: 字段映射完整性
 * - Property 4: AnalysisResult 解析成功率
 * - Property 5: 降级策略有效性
 * 
 * **Feature: ai-response-parser, Property 1-5**
 * **验证需求: 1.1, 1.2, 2.1, 3.1, 3.2, 6.1, 6.2**
 */
class AiResponseParserCorePropertyTest {

    // ========== 生成器定义 ==========

    /**
     * 生成有效的 AnalysisResult JSON 对象
     */
    private fun arbAnalysisResultJson(): Arb<String> = arbitrary {
        val replySuggestion = Arb.string(10..100).bind()
        val strategyAnalysis = Arb.string(20..200).bind()
        val riskLevel = Arb.enum<RiskLevel>().bind()
        
        """
        {
          "replySuggestion": "$replySuggestion",
          "strategyAnalysis": "$strategyAnalysis",
          "riskLevel": "${riskLevel.name}"
        }
        """.trimIndent()
    }

    /**
     * 生成带 Markdown 代码块标记的 JSON
     */
    private fun arbMarkdownWrappedJson(): Arb<String> = arbitrary {
        val json = arbAnalysisResultJson().bind()
        val markdownType = Arb.choice(
            Arb.constant("```json\n$json\n```"),
            Arb.constant("```\n$json\n```"),
            Arb.constant("```json\n$json```"),
            Arb.constant("```\n$json```")
        ).bind()
        markdownType
    }

    /**
     * 生成带前后缀文本的 JSON
     */
    private fun arbJsonWithPrefixSuffix(): Arb<String> = arbitrary {
        val json = arbAnalysisResultJson().bind()
        val prefix = Arb.string(0..50).bind()
        val suffix = Arb.string(0..50).bind()
        "$prefix$json$suffix"
    }

    /**
     * 生成中文字段名的 JSON
     */
    private fun arbChineseFieldJson(): Arb<String> = arbitrary {
        val replySuggestion = Arb.string(10..100).bind()
        val strategyAnalysis = Arb.string(20..200).bind()
        val riskLevel = Arb.enum<RiskLevel>().bind()
        
        val fieldMapping = Arb.choice(
            // 使用配置文件中定义的中文字段名
            Arb.constant("""
            {
              "回复建议": "$replySuggestion",
              "策略分析": "$strategyAnalysis",
              "风险等级": "${riskLevel.name}"
            }
            """.trimIndent()),
            Arb.constant("""
            {
              "建议回复": "$replySuggestion",
              "心理分析": "$strategyAnalysis",
              "风险级别": "${riskLevel.name}"
            }
            """.trimIndent())
        ).bind()
        
        fieldMapping
    }

    /**
     * 生成部分字段缺失的 JSON
     */
    private fun arbPartialJson(): Arb<String> = arbitrary {
        val replySuggestion = Arb.string(10..100).bind()
        val includeStrategy = Arb.boolean().bind()
        val includeRisk = Arb.boolean().bind()
        
        buildString {
            append("{\n")
            append("  \"replySuggestion\": \"$replySuggestion\"")
            if (includeStrategy) {
                append(",\n  \"strategyAnalysis\": \"${Arb.string(20..200).bind()}\"")
            }
            if (includeRisk) {
                append(",\n  \"riskLevel\": \"${Arb.enum<RiskLevel>().bind().name}\"")
            }
            append("\n}")
        }
    }

    // ========== Property 1: Markdown 清洗一致性 ==========
    
    /**
     * Property 1: Markdown 清洗一致性
     * 
     * *对于任何* 包含 Markdown 代码块标记的 JSON 响应，
     * 清洗后应该移除所有代码块标记并保留 JSON 内容
     * 
     * **Feature: ai-response-parser, Property 1: Markdown 清洗一致性**
     * **验证需求: Requirements 1.1**
     */
    @Test
    fun `Property 1 - Markdown cleaning consistency`() = runBlocking {
        checkAll(100, arbMarkdownWrappedJson()) { wrappedJson ->
            // 创建测试用的 AiRepositoryImpl 实例
            val repository = createTestRepository()
            
            // 调用 preprocessJsonResponse 方法（通过反射访问私有方法）
            val cleanedJson = callPreprocessJsonResponse(repository, wrappedJson)
            
            // 验证：清洗后的 JSON 不应包含 Markdown 标记
            assertFalse(
                "清洗后的 JSON 不应包含 ```json 标记",
                cleanedJson.contains("```json")
            )
            assertFalse(
                "清洗后的 JSON 不应包含 ``` 标记",
                cleanedJson.contains("```")
            )
            
            // 验证：清洗后的 JSON 应该是有效的 JSON 对象
            assertTrue(
                "清洗后的 JSON 应该以 { 开头",
                cleanedJson.trim().startsWith("{")
            )
            assertTrue(
                "清洗后的 JSON 应该以 } 结尾",
                cleanedJson.trim().endsWith("}")
            )
        }
    }

    // ========== Property 2: JSON 提取正确性 ==========
    
    /**
     * Property 2: JSON 提取正确性
     * 
     * *对于任何* 包含前后缀文本的响应，
     * 应该能够正确提取 JSON 对象部分
     * 
     * **Feature: ai-response-parser, Property 2: JSON 提取正确性**
     * **验证需求: Requirements 1.2**
     */
    @Test
    fun `Property 2 - JSON extraction correctness`() = runBlocking {
        checkAll(100, arbJsonWithPrefixSuffix()) { jsonWithExtra ->
            val repository = createTestRepository()
            
            // 调用 preprocessJsonResponse 方法
            val extractedJson = callPreprocessJsonResponse(repository, jsonWithExtra)
            
            // 验证：提取后的 JSON 应该是有效的 JSON 对象
            assertTrue(
                "提取后的 JSON 应该以 { 开头",
                extractedJson.trim().startsWith("{")
            )
            assertTrue(
                "提取后的 JSON 应该以 } 结尾",
                extractedJson.trim().endsWith("}")
            )
            
            // 验证：提取后的 JSON 应该包含必需的字段
            assertTrue(
                "提取后的 JSON 应该包含 replySuggestion 字段",
                extractedJson.contains("replySuggestion") || 
                extractedJson.contains("回复建议") ||
                extractedJson.contains("建议回复")
            )
        }
    }

    // ========== Property 3: 字段映射完整性 ==========
    
    /**
     * Property 3: 字段映射完整性
     * 
     * *对于任何* 包含配置文件中定义的中文字段名的 JSON，
     * 映射后所有中文字段名都应该被转换为对应的英文字段名
     * 
     * **Feature: ai-response-parser, Property 3: 字段映射完整性**
     * **验证需求: Requirements 2.1**
     */
    @Test
    fun `Property 3 - Field mapping completeness`() = runBlocking {
        checkAll(100, arbChineseFieldJson()) { chineseJson ->
            val repository = createTestRepository()
            
            // 调用 preprocessJsonResponse 方法（包含字段映射）
            val mappedJson = callPreprocessJsonResponse(repository, chineseJson)
            
            // 验证：映射后的 JSON 应该包含英文字段名
            assertTrue(
                "映射后的 JSON 应该包含 replySuggestion 字段",
                mappedJson.contains("\"replySuggestion\"")
            )
            assertTrue(
                "映射后的 JSON 应该包含 strategyAnalysis 字段",
                mappedJson.contains("\"strategyAnalysis\"")
            )
            assertTrue(
                "映射后的 JSON 应该包含 riskLevel 字段",
                mappedJson.contains("\"riskLevel\"")
            )
            
            // 验证：映射后的 JSON 不应该包含中文字段名
            assertFalse(
                "映射后的 JSON 不应该包含中文字段名",
                mappedJson.contains("\"回复建议\"") ||
                mappedJson.contains("\"建议回复\"") ||
                mappedJson.contains("\"策略分析\"") ||
                mappedJson.contains("\"心理分析\"") ||
                mappedJson.contains("\"风险等级\"") ||
                mappedJson.contains("\"风险级别\"")
            )
        }
    }

    // ========== Property 4: AnalysisResult 解析成功率 ==========
    
    /**
     * Property 4: AnalysisResult 解析成功率
     * 
     * *对于任何* 符合标准格式或包含已知中文字段的 AnalysisResult JSON，
     * 解析成功率应该 ≥ 95%
     * 
     * **Feature: ai-response-parser, Property 4: AnalysisResult 解析成功率**
     * **验证需求: Requirements 3.1, 3.2**
     */
    @Test
    fun `Property 4 - AnalysisResult parsing success rate`() = runBlocking {
        var successCount = 0
        var totalCount = 0
        
        // 测试标准格式的 JSON
        checkAll(50, arbAnalysisResultJson()) { json ->
            totalCount++
            val repository = createTestRepository()
            
            // 调用 parseAnalysisResult 方法
            val result = callParseAnalysisResult(repository, json)
            
            if (result.isSuccess) {
                successCount++
                val analysisResult = result.getOrNull()
                assertNotNull("解析结果不应为 null", analysisResult)
                assertNotNull("replySuggestion 不应为 null", analysisResult?.replySuggestion)
                assertNotNull("strategyAnalysis 不应为 null", analysisResult?.strategyAnalysis)
                assertNotNull("riskLevel 不应为 null", analysisResult?.riskLevel)
            }
        }
        
        // 测试中文字段名的 JSON
        checkAll(50, arbChineseFieldJson()) { json ->
            totalCount++
            val repository = createTestRepository()
            
            val result = callParseAnalysisResult(repository, json)
            
            if (result.isSuccess) {
                successCount++
            }
        }
        
        // 计算成功率
        val successRate = successCount.toDouble() / totalCount.toDouble()
        
        println("AnalysisResult 解析成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
        // 验证：成功率应该 ≥ 95%
        assertTrue(
            "AnalysisResult 解析成功率应该 ≥ 95%，实际: ${successRate * 100}%",
            successRate >= 0.95
        )
    }

    // ========== Property 5: 降级策略有效性 ==========
    
    /**
     * Property 5: 降级策略有效性
     * 
     * *对于任何* 无法解析的响应，
     * 系统应该返回包含默认值的 Domain Model，而不是抛出异常
     * 
     * **Feature: ai-response-parser, Property 5: 降级策略有效性**
     * **验证需求: Requirements 6.1, 6.2**
     */
    @Test
    fun `Property 5 - Fallback strategy effectiveness`() = runBlocking {
        // 生成各种无效的 JSON
        val invalidJsons = listOf(
            "",                           // 空字符串
            "null",                       // null
            "{}",                         // 空对象
            "{invalid json}",             // 无效 JSON
            "纯文本内容",                  // 纯文本
            "{ \"unknown\": \"field\" }"  // 未知字段
        )
        
        invalidJsons.forEach { invalidJson ->
            val repository = createTestRepository()
            
            // 调用 parseAnalysisResult 方法
            val result = callParseAnalysisResult(repository, invalidJson)
            
            // 验证：应该返回成功结果（使用默认值）
            assertTrue(
                "对于无效 JSON '$invalidJson'，应该返回成功结果（使用默认值）",
                result.isSuccess
            )
            
            val analysisResult = result.getOrNull()
            assertNotNull("解析结果不应为 null", analysisResult)
            
            // 验证：返回的应该是默认值
            assertNotNull("replySuggestion 不应为 null", analysisResult?.replySuggestion)
            assertNotNull("strategyAnalysis 不应为 null", analysisResult?.strategyAnalysis)
            assertNotNull("riskLevel 不应为 null", analysisResult?.riskLevel)
            
            // 验证：默认值应该包含提示信息
            assertTrue(
                "默认的 replySuggestion 应该包含提示信息",
                analysisResult?.replySuggestion?.contains("AI") == true ||
                analysisResult?.replySuggestion?.contains("暂时") == true
            )
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试用的 AiRepositoryImpl 实例
     */
    private fun createTestRepository(): AiRepositoryImpl {
        // 创建 mock 的依赖
        val mockApi = createMockOpenAiApi()
        val mockProviderRepository = createMockProviderRepository()
        
        return AiRepositoryImpl(mockApi, mockProviderRepository)
    }

    /**
     * 创建 mock 的 OpenAiApi
     */
    private fun createMockOpenAiApi(): com.empathy.ai.data.remote.api.OpenAiApi {
        // 使用 MockK 或手动创建 mock
        return object : com.empathy.ai.data.remote.api.OpenAiApi {
            override suspend fun chatCompletion(
                url: String,
                headers: Map<String, String>,
                request: com.empathy.ai.data.remote.model.ChatRequestDto
            ): com.empathy.ai.data.remote.model.ChatResponseDto {
                throw NotImplementedError("Mock API - not used in property tests")
            }
        }
    }

    /**
     * 创建 mock 的 AiProviderRepository
     */
    private fun createMockProviderRepository(): com.empathy.ai.domain.repository.AiProviderRepository {
        return object : com.empathy.ai.domain.repository.AiProviderRepository {
            override fun getAllProviders(): kotlinx.coroutines.flow.Flow<List<com.empathy.ai.domain.model.AiProvider>> {
                throw NotImplementedError("Mock repository - not used in property tests")
            }

            override suspend fun getProvider(id: String): Result<com.empathy.ai.domain.model.AiProvider?> {
                throw NotImplementedError("Mock repository - not used in property tests")
            }

            override suspend fun getDefaultProvider(): Result<com.empathy.ai.domain.model.AiProvider?> {
                throw NotImplementedError("Mock repository - not used in property tests")
            }

            override suspend fun saveProvider(provider: com.empathy.ai.domain.model.AiProvider): Result<Unit> {
                throw NotImplementedError("Mock repository - not used in property tests")
            }

            override suspend fun deleteProvider(id: String): Result<Unit> {
                throw NotImplementedError("Mock repository - not used in property tests")
            }

            override suspend fun setDefaultProvider(id: String): Result<Unit> {
                throw NotImplementedError("Mock repository - not used in property tests")
            }

            override suspend fun testConnection(provider: com.empathy.ai.domain.model.AiProvider): Result<com.empathy.ai.domain.model.ConnectionTestResult> {
                throw NotImplementedError("Mock repository - not used in property tests")
            }
        }
    }

    /**
     * 通过反射调用 preprocessJsonResponse 私有方法
     */
    private fun callPreprocessJsonResponse(repository: AiRepositoryImpl, json: String): String {
        val method = AiRepositoryImpl::class.java.getDeclaredMethod(
            "preprocessJsonResponse",
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(repository, json) as String
    }

    /**
     * 通过反射调用 parseAnalysisResult 私有方法
     */
    private fun callParseAnalysisResult(repository: AiRepositoryImpl, json: String): Result<AnalysisResult> {
        val method = AiRepositoryImpl::class.java.getDeclaredMethod(
            "parseAnalysisResult",
            String::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(repository, json) as Result<AnalysisResult>
    }
}
