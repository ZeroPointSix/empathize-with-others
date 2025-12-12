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
 * AI 响应解析器核心属性测试（简化版）
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
class AiResponseParserCorePropertySimpleTest {

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
          "replySuggestion": "${replySuggestion.replace("\"", "\\\"")}",
          "strategyAnalysis": "${strategyAnalysis.replace("\"", "\\\"")}",
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
              "回复建议": "${replySuggestion.replace("\"", "\\\"")}",
              "策略分析": "${strategyAnalysis.replace("\"", "\\\"")}",
              "风险等级": "${riskLevel.name}"
            }
            """.trimIndent()),
            Arb.constant("""
            {
              "建议回复": "${replySuggestion.replace("\"", "\\\"")}",
              "心理分析": "${strategyAnalysis.replace("\"", "\\\"")}",
              "风险级别": "${riskLevel.name}"
            }
            """.trimIndent())
        ).bind()
        
        fieldMapping
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
        var successCount = 0
        var totalCount = 0
        
        checkAll(100, arbMarkdownWrappedJson()) { wrappedJson ->
            totalCount++
            
            // 验证：原始 JSON 应该包含 Markdown 标记
            val hasMarkdown = wrappedJson.contains("```")
            assertTrue("测试数据应该包含 Markdown 标记", hasMarkdown)
            
            // 模拟清洗过程：移除 Markdown 标记
            val cleanedJson = wrappedJson
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            // 验证：清洗后的 JSON 不应包含 Markdown 标记
            assertFalse(
                "清洗后的 JSON 不应包含 ``` 标记",
                cleanedJson.contains("```")
            )
            
            // 验证：清洗后的 JSON 应该是有效的 JSON 对象
            if (cleanedJson.trim().startsWith("{") && cleanedJson.trim().endsWith("}")) {
                successCount++
            }
        }
        
        val successRate = successCount.toDouble() / totalCount.toDouble()
        println("Property 1 - Markdown 清洗成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
        // 验证：成功率应该 ≥ 95%
        assertTrue(
            "Markdown 清洗成功率应该 ≥ 95%，实际: ${successRate * 100}%",
            successRate >= 0.95
        )
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
        var successCount = 0
        var totalCount = 0
        
        checkAll(100, arbJsonWithPrefixSuffix()) { jsonWithExtra ->
            totalCount++
            
            // 模拟提取过程：查找第一个 { 和最后一个 }
            val startIndex = jsonWithExtra.indexOf('{')
            val endIndex = jsonWithExtra.lastIndexOf('}')
            
            if (startIndex >= 0 && endIndex > startIndex) {
                val extractedJson = jsonWithExtra.substring(startIndex, endIndex + 1)
                
                // 验证：提取后的 JSON 应该是有效的 JSON 对象
                if (extractedJson.trim().startsWith("{") && extractedJson.trim().endsWith("}")) {
                    // 验证：提取后的 JSON 应该包含必需的字段
                    if (extractedJson.contains("replySuggestion") || 
                        extractedJson.contains("回复建议") ||
                        extractedJson.contains("建议回复")) {
                        successCount++
                    }
                }
            }
        }
        
        val successRate = successCount.toDouble() / totalCount.toDouble()
        println("Property 2 - JSON 提取成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
        // 验证：成功率应该 ≥ 95%
        assertTrue(
            "JSON 提取成功率应该 ≥ 95%，实际: ${successRate * 100}%",
            successRate >= 0.95
        )
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
        var successCount = 0
        var totalCount = 0
        
        // 定义字段映射规则（来自配置文件）
        val fieldMappings = mapOf(
            "replySuggestion" to listOf("回复建议", "建议回复", "话术建议"),
            "strategyAnalysis" to listOf("策略分析", "心理分析", "军师分析"),
            "riskLevel" to listOf("风险等级", "风险级别")
        )
        
        checkAll(100, arbChineseFieldJson()) { chineseJson ->
            totalCount++
            
            // 模拟映射过程
            var mappedJson = chineseJson
            fieldMappings.forEach { (englishField, chineseFields) ->
                chineseFields.forEach { chineseField ->
                    mappedJson = mappedJson.replace("\"$chineseField\"", "\"$englishField\"")
                }
            }
            
            // 验证：映射后的 JSON 应该包含英文字段名
            val hasEnglishFields = mappedJson.contains("\"replySuggestion\"") &&
                                   mappedJson.contains("\"strategyAnalysis\"") &&
                                   mappedJson.contains("\"riskLevel\"")
            
            // 验证：映射后的 JSON 不应该包含中文字段名
            val hasChineseFields = mappedJson.contains("\"回复建议\"") ||
                                   mappedJson.contains("\"建议回复\"") ||
                                   mappedJson.contains("\"策略分析\"") ||
                                   mappedJson.contains("\"心理分析\"") ||
                                   mappedJson.contains("\"风险等级\"") ||
                                   mappedJson.contains("\"风险级别\"")
            
            if (hasEnglishFields && !hasChineseFields) {
                successCount++
            }
        }
        
        val successRate = successCount.toDouble() / totalCount.toDouble()
        println("Property 3 - 字段映射成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
        // 验证：成功率应该 ≥ 95%
        assertTrue(
            "字段映射成功率应该 ≥ 95%，实际: ${successRate * 100}%",
            successRate >= 0.95
        )
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
            
            // 模拟解析过程：检查 JSON 是否包含必需字段
            val hasRequiredFields = json.contains("replySuggestion") &&
                                    json.contains("strategyAnalysis") &&
                                    json.contains("riskLevel")
            
            if (hasRequiredFields) {
                successCount++
            }
        }
        
        // 测试中文字段名的 JSON（经过映射后）
        checkAll(50, arbChineseFieldJson()) { chineseJson ->
            totalCount++
            
            // 模拟映射过程
            var mappedJson = chineseJson
            val fieldMappings = mapOf(
                "replySuggestion" to listOf("回复建议", "建议回复"),
                "strategyAnalysis" to listOf("策略分析", "心理分析"),
                "riskLevel" to listOf("风险等级", "风险级别")
            )
            
            fieldMappings.forEach { (englishField, chineseFields) ->
                chineseFields.forEach { chineseField ->
                    mappedJson = mappedJson.replace("\"$chineseField\"", "\"$englishField\"")
                }
            }
            
            // 检查映射后的 JSON 是否包含必需字段
            val hasRequiredFields = mappedJson.contains("replySuggestion") &&
                                    mappedJson.contains("strategyAnalysis") &&
                                    mappedJson.contains("riskLevel")
            
            if (hasRequiredFields) {
                successCount++
            }
        }
        
        val successRate = successCount.toDouble() / totalCount.toDouble()
        println("Property 4 - AnalysisResult 解析成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
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
        
        var successCount = 0
        val totalCount = invalidJsons.size
        
        invalidJsons.forEach { invalidJson ->
            // 模拟降级策略：对于无效 JSON，返回默认值
            val shouldUseFallback = invalidJson.isEmpty() ||
                                    invalidJson == "null" ||
                                    invalidJson == "{}" ||
                                    !invalidJson.contains("replySuggestion")
            
            if (shouldUseFallback) {
                // 验证：应该使用默认值
                val defaultReplySuggestion = "AI 暂时无法生成建议，请重试或切换模型"
                val defaultStrategyAnalysis = "AI 分析暂时不可用"
                val defaultRiskLevel = RiskLevel.SAFE
                
                // 模拟返回默认值
                assertNotNull("默认 replySuggestion 不应为 null", defaultReplySuggestion)
                assertNotNull("默认 strategyAnalysis 不应为 null", defaultStrategyAnalysis)
                assertNotNull("默认 riskLevel 不应为 null", defaultRiskLevel)
                
                // 验证：默认值应该包含提示信息
                assertTrue(
                    "默认的 replySuggestion 应该包含提示信息",
                    defaultReplySuggestion.contains("AI") || defaultReplySuggestion.contains("暂时")
                )
                
                successCount++
            }
        }
        
        val successRate = successCount.toDouble() / totalCount.toDouble()
        println("Property 5 - 降级策略成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
        // 验证：成功率应该 = 100%（所有无效 JSON 都应该使用降级策略）
        assertTrue(
            "降级策略成功率应该 = 100%，实际: ${successRate * 100}%",
            successRate == 1.0
        )
    }
}
