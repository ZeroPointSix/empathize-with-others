package com.empathy.ai.data.parser

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.model.RiskLevel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * AI响应解析器组件单元测试
 * 
 * 测试各个组件的独立功能：
 * - JsonCleaner 组件测试
 * - FieldMapper 组件测试
 * - FallbackHandler 组件测试
 * - AiResponseParser 组件测试
 * - ParsingContext 上下文测试
 * 
 * **Feature: ai-response-parser, Component Testing**
 * **验证需求: 组件功能验证**
 */
class AiResponseParserComponentTest {

    // ========== 测试依赖 ==========

    private lateinit var jsonCleaner: JsonCleaner
    private lateinit var fieldMapper: FieldMapper
    private lateinit var fallbackHandler: FallbackHandler
    private lateinit var parser: AiResponseParser

    @Before
    fun setup() {
        jsonCleaner = EnhancedJsonCleaner()
        fieldMapper = SmartFieldMapper()
        fallbackHandler = MultiLevelFallbackHandler()
        parser = StrategyBasedAiResponseParser(
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler
        )
    }

    // ========== JsonCleaner 组件测试 ==========

    /**
     * 测试 JsonCleaner 的 Markdown 清洗功能
     */
    @Test
    fun `Test JsonCleaner markdown cleaning`() {
        val testCases = mapOf(
            "```json\n{\"test\": \"value\"}\n```" to "{\"test\": \"value\"}",
            "```\n{\"test\": \"value\"}\n```" to "{\"test\": \"value\"}",
            "```json\n{\"test\": \"value\"}```" to "{\"test\": \"value\"}",
            "```\n{\"test\": \"value\"}```" to "{\"test\": \"value\"}"
        )
        
        testCases.forEach { (input, expected) ->
            val result = jsonCleaner.clean(input)
            assertEquals(
                "JsonCleaner 应该正确清洗 Markdown: $input",
                expected,
                result
            )
        }
    }

    /**
     * 测试 JsonCleaner 的 JSON 提取功能
     */
    @Test
    fun `Test JsonCleaner JSON extraction`() {
        val testCases = mapOf(
            "prefix {\"test\": \"value\"} suffix" to "{\"test\": \"value\"}",
            "文本内容{\"test\": \"value\"}更多文本" to "{\"test\": \"value\"}",
            "{\"test\": \"value\"}" to "{\"test\": \"value\"}"
        )
        
        testCases.forEach { (input, expected) ->
            val result = jsonCleaner.clean(input)
            assertEquals(
                "JsonCleaner 应该正确提取 JSON: $input",
                expected,
                result
            )
        }
    }

    /**
     * 测试 JsonCleaner 的 Unicode 修复功能
     */
    @Test
    fun `Test JsonCleaner Unicode fixing`() {
        val unicodeJson = "{\"message\": \"\\u4f60\\u597d\"}" // 你好
        val expected = "{\"message\": \"你好\"}"
        
        val context = CleaningContext(fixUnicode = true)
        val result = jsonCleaner.clean(unicodeJson, context)
        
        assertEquals(
            "JsonCleaner 应该修复 Unicode 编码",
            expected,
            result
        )
    }

    /**
     * 测试 JsonCleaner 的有效性验证功能
     */
    @Test
    fun `Test JsonCleaner validation`() {
        val validCases = listOf(
            "{\"test\": \"value\"}",
            "```json\n{\"test\": \"value\"}\n```",
            "prefix {\"test\": \"value\"} suffix"
        )
        
        val invalidCases = listOf(
            "",
            "null",
            "纯文本",
            "{invalid json}"
        )
        
        validCases.forEach { validCase ->
            assertTrue(
                "JsonCleaner 应该识别有效 JSON: $validCase",
                jsonCleaner.isValid(validCase)
            )
        }
        
        invalidCases.forEach { invalidCase ->
            assertFalse(
                "JsonCleaner 应该识别无效 JSON: $invalidCase",
                jsonCleaner.isValid(invalidCase)
            )
        }
    }

    // ========== FieldMapper 组件测试 ==========

    /**
     * 测试 FieldMapper 的基本映射功能
     */
    @Test
    fun `Test FieldMapper basic mapping`() {
        val chineseJson = """
        {
          "回复建议": "测试建议",
          "策略分析": "测试分析",
          "风险等级": "LOW"
        }
        """.trimIndent()
        
        val result = fieldMapper.mapFields(chineseJson)
        
        assertTrue(
            "FieldMapper 应该映射中文字段名为英文字段名",
            result.contains("\"replySuggestion\"") &&
            result.contains("\"strategyAnalysis\"") &&
            result.contains("\"riskLevel\"")
        )
        
        assertFalse(
            "FieldMapper 不应该保留中文字段名",
            result.contains("\"回复建议\"") ||
            result.contains("\"策略分析\"") ||
            result.contains("\"风险等级\"")
        )
    }

    /**
     * 测试 FieldMapper 的动态学习功能
     */
    @Test
    fun `Test FieldMapper dynamic learning`() {
        // 添加自定义映射
        fieldMapper.addMapping("customField", listOf("自定义字段", "特殊字段"))
        
        val testJson = """
        {
          "自定义字段": "测试值",
          "其他字段": "其他值"
        }
        """.trimIndent()
        
        val result = fieldMapper.mapFields(testJson)
        
        assertTrue(
            "FieldMapper 应该使用自定义映射",
            result.contains("\"customField\"")
        )
        
        assertFalse(
            "FieldMapper 不应该保留中文字段名",
            result.contains("\"自定义字段\"")
        )
    }

    /**
     * 测试 FieldMapper 的模糊匹配功能
     */
    @Test
    fun `Test FieldMapper fuzzy matching`() {
        val context = MappingContext(enableFuzzyMatching = true, fuzzyThreshold = 0.8)
        
        val testJson = """
        {
          "回复建议": "测试建议",
          "策略分析": "测试分析",
          "风险等级": "LOW"
        }
        """.trimIndent()
        
        val result = fieldMapper.mapFields(testJson, context)
        
        assertTrue(
            "FieldMapper 应该使用模糊匹配",
            result.contains("\"replySuggestion\"") &&
            result.contains("\"strategyAnalysis\"") &&
            result.contains("\"riskLevel\"")
        )
    }

    /**
     * 测试 FieldMapper 的映射管理功能
     */
    @Test
    fun `Test FieldMapper mapping management`() {
        // 添加映射
        fieldMapper.addMapping("testField", listOf("测试字段"))
        
        // 获取所有映射
        val allMappings = fieldMapper.getAllMappings()
        assertTrue(
            "FieldMapper 应该包含添加的映射",
            allMappings.containsKey("testField")
        )
        assertTrue(
            "FieldMapper 应该包含正确的映射值",
            allMappings["testField"]?.contains("测试字段") == true
        )
        
        // 清除映射
        fieldMapper.clearMappings()
        val clearedMappings = fieldMapper.getAllMappings()
        assertFalse(
            "FieldMapper 清除后应该不包含任何映射",
            clearedMappings.containsKey("testField")
        )
    }

    // ========== FallbackHandler 组件测试 ==========

    /**
     * 测试 FallbackHandler 的解析失败处理
     */
    @Test
    fun `Test FallbackHandler parsing failure handling`() {
        val exception = RuntimeException("Test parsing error")
        val context = FallbackContext(
            originalJson = "",
            operationType = "test",
            error = exception
        )
        
        val result = fallbackHandler.handleParsingFailure<AnalysisResult>(
            exception,
            AnalysisResult::class.java,
            context
        )
        
        assertTrue(
            "FallbackHandler 应该返回成功结果",
            result is FallbackResult.Success
        )
        
        val analysisResult = (result as FallbackResult.Success).data
        assertNotNull(
            "FallbackHandler 应该返回非 null 的 AnalysisResult",
            analysisResult
        )
        assertNotNull(
            "FallbackHandler 应该提供默认的 replySuggestion",
            analysisResult.replySuggestion
        )
    }

    /**
     * 测试 FallbackHandler 的部分结果处理
     */
    @Test
    fun `Test FallbackHandler partial result handling`() {
        val partialData = mapOf("replySuggestion" to "部分建议")
        val context = FallbackContext(
            originalJson = "",
            operationType = "test"
        )
        
        val result = fallbackHandler.handlePartialResult<AnalysisResult>(
            partialData,
            AnalysisResult::class.java,
            context
        )
        
        assertTrue(
            "FallbackHandler 应该返回成功结果",
            result is FallbackResult.Success
        )
        
        val analysisResult = (result as FallbackResult.Success).data
        assertEquals(
            "FallbackHandler 应该保留部分数据",
            "部分建议",
            analysisResult.replySuggestion
        )
        assertNotNull(
            "FallbackHandler 应该为缺失字段提供默认值",
            analysisResult.strategyAnalysis
        )
    }

    /**
     * 测试 FallbackHandler 的默认值生成
     */
    @Test
    fun `Test FallbackHandler default value generation`() {
        val context = FallbackContext(
            originalJson = "",
            operationType = "test"
        )
        
        val analysisResult = fallbackHandler.generateDefaultValue<AnalysisResult>(
            AnalysisResult::class.java,
            context
        )
        
        assertNotNull(
            "FallbackHandler 应该生成默认的 AnalysisResult",
            analysisResult
        )
        assertNotNull(
            "默认的 replySuggestion 不应为 null",
            analysisResult.replySuggestion
        )
        assertNotNull(
            "默认的 strategyAnalysis 不应为 null",
            analysisResult.strategyAnalysis
        )
        assertNotNull(
            "默认的 riskLevel 不应为 null",
            analysisResult.riskLevel
        )
    }

    // ========== AiResponseParser 组件测试 ==========

    /**
     * 测试 AiResponseParser 的标准解析策略
     */
    @Test
    fun `Test AiResponseParser standard strategy`() = runBlocking {
        val validJson = """
        {
          "replySuggestion": "测试建议",
          "strategyAnalysis": "测试分析",
          "riskLevel": "LOW"
        }
        """.trimIndent()
        
        val context = ParsingContext(strategy = ParsingStrategy.STANDARD)
        val result = parser.parseAnalysisResult(validJson, context)
        
        assertTrue(
            "标准策略应该成功解析有效 JSON",
            result.isSuccess
        )
        
        val analysisResult = result.getOrNull()
        assertEquals(
            "应该正确解析 replySuggestion",
            "测试建议",
            analysisResult?.replySuggestion
        )
        assertEquals(
            "应该正确解析 strategyAnalysis",
            "测试分析",
            analysisResult?.strategyAnalysis
        )
        assertEquals(
            "应该正确解析 riskLevel",
            RiskLevel.LOW,
            analysisResult?.riskLevel
        )
    }

    /**
     * 测试 AiResponseParser 的容错解析策略
     */
    @Test
    fun `Test AiResponseParser fallback strategy`() = runBlocking {
        val invalidJson = "invalid json"
        
        val context = ParsingContext(strategy = ParsingStrategy.FALLBACK)
        val result = parser.parseAnalysisResult(invalidJson, context)
        
        assertTrue(
            "容错策略应该处理无效 JSON",
            result.isSuccess
        )
        
        val analysisResult = result.getOrNull()
        assertNotNull(
            "容错策略应该返回默认值",
            analysisResult
        )
    }

    /**
     * 测试 AiResponseParser 的智能解析策略
     */
    @Test
    fun `Test AiResponseParser intelligent strategy`() = runBlocking {
        val chineseJson = """
        {
          "回复建议": "测试建议",
          "策略分析": "测试分析",
          "风险等级": "LOW"
        }
        """.trimIndent()
        
        val context = ParsingContext(strategy = ParsingStrategy.INTELLIGENT)
        val result = parser.parseAnalysisResult(chineseJson, context)
        
        assertTrue(
            "智能策略应该处理中文字段名",
            result.isSuccess
        )
        
        val analysisResult = result.getOrNull()
        assertEquals(
            "智能策略应该正确映射中文字段名",
            "测试建议",
            analysisResult?.replySuggestion
        )
    }

    /**
     * 测试 AiResponseParser 的上下文传递
     */
    @Test
    fun `Test AiResponseParser context passing`() = runBlocking {
        val testJson = """
        {
          "replySuggestion": "测试建议",
          "strategyAnalysis": "测试分析",
          "riskLevel": "LOW"
        }
        """.trimIndent()
        
        val context = ParsingContext(
            operationId = "test-operation",
            modelName = "test-model",
            strategy = ParsingStrategy.INTELLIGENT
        )
        
        val result = parser.parseAnalysisResult(testJson, context)
        
        assertTrue(
            "应该成功解析",
            result.isSuccess
        )
        
        // 验证上下文信息被正确传递
        // 注意：这里需要根据实际实现来验证上下文信息的使用
    }

    // ========== ParsingContext 上下文测试 ==========

    /**
     * 测试 ParsingContext 的创建和使用
     */
    @Test
    fun `Test ParsingContext creation and usage`() {
        val context = ParsingContext(
            operationId = "test-op",
            modelName = "test-model",
            strategy = ParsingStrategy.INTELLIGENT,
            enableLogging = true
        )
        
        assertEquals(
            "应该正确设置 operationId",
            "test-op",
            context.operationId
        )
        assertEquals(
            "应该正确设置 modelName",
            "test-model",
            context.modelName
        )
        assertEquals(
            "应该正确设置 strategy",
            ParsingStrategy.INTELLIGENT,
            context.strategy
        )
        assertTrue(
            "应该正确设置 enableLogging",
            context.enableLogging
        )
    }

    /**
     * 测试 CleaningContext 的创建和使用
     */
    @Test
    fun `Test CleaningContext creation and usage`() {
        val context = CleaningContext(
            enableDetailedLogging = true,
            fixUnicode = true,
            extractJsonOnly = false
        )
        
        assertTrue(
            "应该正确设置 enableDetailedLogging",
            context.enableDetailedLogging
        )
        assertTrue(
            "应该正确设置 fixUnicode",
            context.fixUnicode
        )
        assertFalse(
            "应该正确设置 extractJsonOnly",
            context.extractJsonOnly
        )
    }

    /**
     * 测试 MappingContext 的创建和使用
     */
    @Test
    fun `Test MappingContext creation and usage`() {
        val context = MappingContext(
            enableFuzzyMatching = true,
            fuzzyThreshold = 0.8,
            enableDynamicLearning = true
        )
        
        assertTrue(
            "应该正确设置 enableFuzzyMatching",
            context.enableFuzzyMatching
        )
        assertEquals(
            "应该正确设置 fuzzyThreshold",
            0.8,
            context.fuzzyThreshold,
            0.01
        )
        assertTrue(
            "应该正确设置 enableDynamicLearning",
            context.enableDynamicLearning
        )
    }

    /**
     * 测试 FallbackContext 的创建和使用
     */
    @Test
    fun `Test FallbackContext creation and usage`() {
        val exception = RuntimeException("Test error")
        val context = FallbackContext(
            originalJson = "{\"test\": \"value\"}",
            operationType = "test-operation",
            error = exception,
            enableIntelligentInference = true
        )
        
        assertEquals(
            "应该正确设置 originalJson",
            "{\"test\": \"value\"}",
            context.originalJson
        )
        assertEquals(
            "应该正确设置 operationType",
            "test-operation",
            context.operationType
        )
        assertEquals(
            "应该正确设置 error",
            exception,
            context.error
        )
        assertTrue(
            "应该正确设置 enableIntelligentInference",
            context.enableIntelligentInference
        )
    }
}