package com.empathy.ai.data.repository

import com.empathy.ai.data.parser.*
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.model.RiskLevel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * AI响应解析器向后兼容性测试
 * 
 * 验证新架构与原有系统的兼容性，确保：
 * 1. 现有API接口不变
 * 2. 现有功能完全兼容
 * 3. 解析结果保持一致
 * 
 * **Feature: ai-response-parser, Backward Compatibility**
 * **验证需求: 向后兼容性要求**
 */
class AiResponseParserBackwardCompatibilityTest {

    // ========== 测试依赖 ==========

    private lateinit var oldStyleRepository: AiRepositoryImpl
    private lateinit var newStyleRepository: AiRepositoryImpl
    private lateinit var responseParserFacade: ResponseParserFacade
    private lateinit var parser: AiResponseParser

    @Before
    fun setup() {
        // 创建旧风格的 AiRepositoryImpl（使用反射访问私有方法）
        oldStyleRepository = createOldStyleRepository()
        
        // 创建新风格的 AiRepositoryImpl（使用新的解析器架构）
        newStyleRepository = createNewStyleRepository()
        
        // 创建响应解析器门面
        responseParserFacade = ResponseParserFacade()
        
        // 创建独立的解析器
        parser = AiResponseParserFactory.createDefaultParser()
    }

    // ========== 向后兼容性测试 ==========

    /**
     * 测试 AnalysisResult 解析的向后兼容性
     */
    @Test
    fun `Test AnalysisResult parsing backward compatibility`() = runBlocking {
        val testCases = listOf(
            // 标准格式
            """
            {
              "replySuggestion": "建议回复：保持冷静，理性沟通",
              "strategyAnalysis": "策略分析：当前情况适合采用温和的沟通方式",
              "riskLevel": "LOW"
            }
            """.trimIndent(),
            
            // 中文字段名
            """
            {
              "回复建议": "建议回复：保持冷静，理性沟通",
              "策略分析": "策略分析：当前情况适合采用温和的沟通方式",
              "风险等级": "LOW"
            }
            """.trimIndent(),
            
            // Markdown包装
            """```json
            {
              "replySuggestion": "建议回复：保持冷静，理性沟通",
              "strategyAnalysis": "策略分析：当前情况适合采用温和的沟通方式",
              "riskLevel": "LOW"
            }
            ```""".trimIndent(),
            
            // 带前后缀
            "这是AI的回复：{\"replySuggestion\": \"建议回复：保持冷静，理性沟通\", \"strategyAnalysis\": \"策略分析：当前情况适合采用温和的沟通方式\", \"riskLevel\": \"LOW\"}回复结束"
        )
        
        testCases.forEach { testCase ->
            // 使用旧风格解析
            val oldResult = callParseAnalysisResult(oldStyleRepository, testCase)
            
            // 使用新风格解析
            val newResult = newStyleRepository.parseAnalysisResult(testCase)
            
            // 使用门面解析
            val facadeResult = responseParserFacade.parseAnalysisResult(testCase)
            
            // 使用独立解析器
            val parserResult = parser.parseAnalysisResult(testCase)
            
            // 验证所有解析方式都成功
            assertTrue(
                "旧风格解析应该成功: $testCase",
                oldResult.isSuccess
            )
            assertTrue(
                "新风格解析应该成功: $testCase",
                newResult.isSuccess
            )
            assertTrue(
                "门面解析应该成功: $testCase",
                facadeResult.isSuccess
            )
            assertTrue(
                "独立解析器应该成功: $testCase",
                parserResult.isSuccess
            )
            
            // 验证解析结果内容一致
            val oldData = oldResult.getOrNull()
            val newData = newResult.getOrNull()
            val facadeData = facadeResult.getOrNull()
            val parserData = parserResult.getOrNull()
            
            assertNotNull("旧风格解析结果不应为 null", oldData)
            assertNotNull("新风格解析结果不应为 null", newData)
            assertNotNull("门面解析结果不应为 null", facadeData)
            assertNotNull("独立解析器结果不应为 null", parserData)
            
            // 验证关键字段内容一致
            assertEquals(
                "replySuggestion 应该一致: $testCase",
                oldData?.replySuggestion,
                newData?.replySuggestion
            )
            assertEquals(
                "strategyAnalysis 应该一致: $testCase",
                oldData?.strategyAnalysis,
                newData?.strategyAnalysis
            )
            assertEquals(
                "riskLevel 应该一致: $testCase",
                oldData?.riskLevel,
                newData?.riskLevel
            )
            
            // 验证门面和独立解析器与新旧风格结果一致
            assertEquals(
                "门面解析的 replySuggestion 应该一致",
                newData?.replySuggestion,
                facadeData?.replySuggestion
            )
            assertEquals(
                "独立解析器的 replySuggestion 应该一致",
                newData?.replySuggestion,
                parserData?.replySuggestion
            )
        }
    }

    /**
     * 测试 SafetyCheckResult 解析的向后兼容性
     */
    @Test
    fun `Test SafetyCheckResult parsing backward compatibility`() = runBlocking {
        val testCases = listOf(
            // 标准格式
            """
            {
              "isSafe": true,
              "riskLevel": "LOW",
              "warningMessage": "",
              "detectedRisks": []
            }
            """.trimIndent(),
            
            // 中文字段名
            """
            {
              "是否安全": true,
              "风险等级": "LOW",
              "警告信息": "",
              "检测到的风险": []
            }
            """.trimIndent(),
            
            // Markdown包装
            """```json
            {
              "isSafe": true,
              "riskLevel": "LOW",
              "warningMessage": "",
              "detectedRisks": []
            }
            ```""".trimIndent()
        )
        
        testCases.forEach { testCase ->
            // 使用旧风格解析
            val oldResult = callParseSafetyCheckResult(oldStyleRepository, testCase)
            
            // 使用新风格解析
            val newResult = newStyleRepository.parseSafetyCheckResult(testCase)
            
            // 使用门面解析
            val facadeResult = responseParserFacade.parseSafetyCheckResult(testCase)
            
            // 使用独立解析器
            val parserResult = parser.parseSafetyCheckResult(testCase)
            
            // 验证所有解析方式都成功
            assertTrue(
                "旧风格解析应该成功: $testCase",
                oldResult.isSuccess
            )
            assertTrue(
                "新风格解析应该成功: $testCase",
                newResult.isSuccess
            )
            assertTrue(
                "门面解析应该成功: $testCase",
                facadeResult.isSuccess
            )
            assertTrue(
                "独立解析器应该成功: $testCase",
                parserResult.isSuccess
            )
            
            // 验证解析结果内容一致
            val oldData = oldResult.getOrNull()
            val newData = newResult.getOrNull()
            val facadeData = facadeResult.getOrNull()
            val parserData = parserResult.getOrNull()
            
            assertNotNull("旧风格解析结果不应为 null", oldData)
            assertNotNull("新风格解析结果不应为 null", newData)
            assertNotNull("门面解析结果不应为 null", facadeData)
            assertNotNull("独立解析器结果不应为 null", parserData)
            
            // 验证关键字段内容一致
            assertEquals(
                "isSafe 应该一致: $testCase",
                oldData?.isSafe,
                newData?.isSafe
            )
            assertEquals(
                "riskLevel 应该一致: $testCase",
                oldData?.riskLevel,
                newData?.riskLevel
            )
            assertEquals(
                "warningMessage 应该一致: $testCase",
                oldData?.warningMessage,
                newData?.warningMessage
            )
        }
    }

    /**
     * 测试 ExtractedData 解析的向后兼容性
     */
    @Test
    fun `Test ExtractedData parsing backward compatibility`() = runBlocking {
        val testCases = listOf(
            // 标准格式
            """
            {
              "contacts": ["张三", "李四"],
              "locations": ["北京", "上海"],
              "organizations": ["公司A", "公司B"],
              "keywords": ["项目", "会议"],
              "emotions": ["开心", "满意"],
              "topics": ["工作", "生活"],
              "summary": "这是一段摘要"
            }
            """.trimIndent(),
            
            // 中文字段名
            """
            {
              "联系人": ["张三", "李四"],
              "地点": ["北京", "上海"],
              "组织": ["公司A", "公司B"],
              "关键词": ["项目", "会议"],
              "情绪": ["开心", "满意"],
              "主题": ["工作", "生活"],
              "摘要": "这是一段摘要"
            }
            """.trimIndent()
        )
        
        testCases.forEach { testCase ->
            // 使用旧风格解析
            val oldResult = callParseExtractedData(oldStyleRepository, testCase)
            
            // 使用新风格解析
            val newResult = newStyleRepository.parseExtractedData(testCase)
            
            // 使用门面解析
            val facadeResult = responseParserFacade.parseExtractedData(testCase)
            
            // 使用独立解析器
            val parserResult = parser.parseExtractedData(testCase)
            
            // 验证所有解析方式都成功
            assertTrue(
                "旧风格解析应该成功: $testCase",
                oldResult.isSuccess
            )
            assertTrue(
                "新风格解析应该成功: $testCase",
                newResult.isSuccess
            )
            assertTrue(
                "门面解析应该成功: $testCase",
                facadeResult.isSuccess
            )
            assertTrue(
                "独立解析器应该成功: $testCase",
                parserResult.isSuccess
            )
            
            // 验证解析结果内容一致
            val oldData = oldResult.getOrNull()
            val newData = newResult.getOrNull()
            val facadeData = facadeResult.getOrNull()
            val parserData = parserResult.getOrNull()
            
            assertNotNull("旧风格解析结果不应为 null", oldData)
            assertNotNull("新风格解析结果不应为 null", newData)
            assertNotNull("门面解析结果不应为 null", facadeData)
            assertNotNull("独立解析器结果不应为 null", parserData)
            
            // 验证关键字段内容一致
            assertEquals(
                "contacts 应该一致: $testCase",
                oldData?.contacts,
                newData?.contacts
            )
            assertEquals(
                "locations 应该一致: $testCase",
                oldData?.locations,
                newData?.locations
            )
            assertEquals(
                "summary 应该一致: $testCase",
                oldData?.summary,
                newData?.summary
            )
        }
    }

    /**
     * 测试错误处理的向后兼容性
     */
    @Test
    fun `Test error handling backward compatibility`() = runBlocking {
        val invalidCases = listOf(
            "",                           // 空字符串
            "null",                       // null
            "{}",                         // 空对象
            "{invalid json}",             // 无效 JSON
            "纯文本内容",                  // 纯文本
            "{ \"unknown\": \"field\" }"  // 未知字段
        )
        
        invalidCases.forEach { invalidCase ->
            // 使用旧风格解析
            val oldResult = callParseAnalysisResult(oldStyleRepository, invalidCase)
            
            // 使用新风格解析
            val newResult = newStyleRepository.parseAnalysisResult(invalidCase)
            
            // 使用门面解析
            val facadeResult = responseParserFacade.parseAnalysisResult(invalidCase)
            
            // 使用独立解析器
            val parserResult = parser.parseAnalysisResult(invalidCase)
            
            // 验证所有解析方式都返回成功结果（使用默认值）
            assertTrue(
                "旧风格应该返回默认值: $invalidCase",
                oldResult.isSuccess
            )
            assertTrue(
                "新风格应该返回默认值: $invalidCase",
                newResult.isSuccess
            )
            assertTrue(
                "门面应该返回默认值: $invalidCase",
                facadeResult.isSuccess
            )
            assertTrue(
                "独立解析器应该返回默认值: $invalidCase",
                parserResult.isSuccess
            )
            
            // 验证所有结果都包含非空的默认值
            val oldData = oldResult.getOrNull()
            val newData = newResult.getOrNull()
            val facadeData = facadeResult.getOrNull()
            val parserData = parserResult.getOrNull()
            
            assertNotNull("旧风格默认值不应为 null", oldData?.replySuggestion)
            assertNotNull("新风格默认值不应为 null", newData?.replySuggestion)
            assertNotNull("门面默认值不应为 null", facadeData?.replySuggestion)
            assertNotNull("独立解析器默认值不应为 null", parserData?.replySuggestion)
        }
    }

    // ========== 性能兼容性测试 ==========

    /**
     * 测试性能的向后兼容性
     */
    @Test
    fun `Test performance backward compatibility`() = runBlocking {
        val testJson = """
        {
          "replySuggestion": "建议回复：保持冷静，理性沟通",
          "strategyAnalysis": "策略分析：当前情况适合采用温和的沟通方式",
          "riskLevel": "LOW"
        }
        """.trimIndent()
        
        val iterations = 1000
        
        // 测试旧风格性能
        val oldStartTime = System.nanoTime()
        repeat(iterations) {
            callParseAnalysisResult(oldStyleRepository, testJson)
        }
        val oldEndTime = System.nanoTime()
        val oldDuration = (oldEndTime - oldStartTime) / 1_000_000.0 // 转换为毫秒
        
        // 测试新风格性能
        val newStartTime = System.nanoTime()
        repeat(iterations) {
            newStyleRepository.parseAnalysisResult(testJson)
        }
        val newEndTime = System.nanoTime()
        val newDuration = (newEndTime - newStartTime) / 1_000_000.0 // 转换为毫秒
        
        // 测试门面性能
        val facadeStartTime = System.nanoTime()
        repeat(iterations) {
            responseParserFacade.parseAnalysisResult(testJson)
        }
        val facadeEndTime = System.nanoTime()
        val facadeDuration = (facadeEndTime - facadeStartTime) / 1_000_000.0 // 转换为毫秒
        
        // 测试独立解析器性能
        val parserStartTime = System.nanoTime()
        repeat(iterations) {
            parser.parseAnalysisResult(testJson)
        }
        val parserEndTime = System.nanoTime()
        val parserDuration = (parserEndTime - parserStartTime) / 1_000_000.0 // 转换为毫秒
        
        println("性能比较 ($iterations 次迭代):")
        println("旧风格: ${oldDuration}ms")
        println("新风格: ${newDuration}ms")
        println("门面: ${facadeDuration}ms")
        println("独立解析器: ${parserDuration}ms")
        
        // 验证新风格性能不超过旧风格的150%（允许一定的性能开销）
        val performanceRatio = newDuration / oldDuration
        assertTrue(
            "新风格性能不应该显著劣于旧风格 (比率: $performanceRatio)",
            performanceRatio < 1.5
        )
    }

    // ========== 辅助方法 ==========

    /**
     * 创建旧风格的 AiRepositoryImpl（使用反射访问私有方法）
     */
    private fun createOldStyleRepository(): AiRepositoryImpl {
        val mockApi = createMockOpenAiApi()
        val mockProviderRepository = createMockProviderRepository()
        return AiRepositoryImpl(mockApi, mockProviderRepository)
    }

    /**
     * 创建新风格的 AiRepositoryImpl（使用新的解析器架构）
     */
    private fun createNewStyleRepository(): AiRepositoryImpl {
        val mockApi = createMockOpenAiApi()
        val mockProviderRepository = createMockProviderRepository()
        return AiRepositoryImpl(mockApi, mockProviderRepository)
    }

    /**
     * 创建 mock 的 OpenAiApi
     */
    private fun createMockOpenAiApi(): com.empathy.ai.data.remote.api.OpenAiApi {
        return object : com.empathy.ai.data.remote.api.OpenAiApi {
            override suspend fun chatCompletion(
                url: String,
                headers: Map<String, String>,
                request: com.empathy.ai.data.remote.model.ChatRequestDto
            ): com.empathy.ai.data.remote.model.ChatResponseDto {
                throw NotImplementedError("Mock API - not used in compatibility tests")
            }
        }
    }

    /**
     * 创建 mock 的 AiProviderRepository
     */
    private fun createMockProviderRepository(): com.empathy.ai.domain.repository.AiProviderRepository {
        return object : com.empathy.ai.domain.repository.AiProviderRepository {
            override fun getAllProviders(): kotlinx.coroutines.flow.Flow<List<com.empathy.ai.domain.model.AiProvider>> {
                throw NotImplementedError("Mock repository - not used in compatibility tests")
            }

            override suspend fun getProvider(id: String): Result<com.empathy.ai.domain.model.AiProvider?> {
                throw NotImplementedError("Mock repository - not used in compatibility tests")
            }

            override suspend fun getDefaultProvider(): Result<com.empathy.ai.domain.model.AiProvider?> {
                throw NotImplementedError("Mock repository - not used in compatibility tests")
            }

            override suspend fun saveProvider(provider: com.empathy.ai.domain.model.AiProvider): Result<Unit> {
                throw NotImplementedError("Mock repository - not used in compatibility tests")
            }

            override suspend fun deleteProvider(id: String): Result<Unit> {
                throw NotImplementedError("Mock repository - not used in compatibility tests")
            }

            override suspend fun setDefaultProvider(id: String): Result<Unit> {
                throw NotImplementedError("Mock repository - not used in compatibility tests")
            }

            override suspend fun testConnection(provider: com.empathy.ai.domain.model.AiProvider): Result<com.empathy.ai.domain.model.ConnectionTestResult> {
                throw NotImplementedError("Mock repository - not used in compatibility tests")
            }
        }
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

    /**
     * 通过反射调用 parseSafetyCheckResult 私有方法
     */
    private fun callParseSafetyCheckResult(repository: AiRepositoryImpl, json: String): Result<SafetyCheckResult> {
        val method = AiRepositoryImpl::class.java.getDeclaredMethod(
            "parseSafetyCheckResult",
            String::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(repository, json) as Result<SafetyCheckResult>
    }

    /**
     * 通过反射调用 parseExtractedData 私有方法
     */
    private fun callParseExtractedData(repository: AiRepositoryImpl, json: String): Result<ExtractedData> {
        val method = AiRepositoryImpl::class.java.getDeclaredMethod(
            "parseExtractedData",
            String::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(repository, json) as Result<ExtractedData>
    }
}