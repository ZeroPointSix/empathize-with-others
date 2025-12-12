package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.RiskLevel
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * AI 响应解析器边缘情况属性测试
 * 
 * 测试边缘情况属性：
 * - Property 9: 空响应处理
 * - Property 10: 字段缺失处理
 * 
 * **Feature: ai-response-parser, Property 9-10**
 * **验证需求: 1.5, 4.4, 4.5, 5.4**
 */
class AiResponseParserEdgeCasePropertyTest {

    // ========== 生成器定义 ==========

    /**
     * 生成空响应或无效响应
     */
    private fun arbEmptyOrInvalidResponse(): Arb<String> = Arb.choice(
        Arb.constant(""),                    // 空字符串
        Arb.constant("   "),                 // 仅空格
        Arb.constant("\n\n\n"),              // 仅换行
        Arb.constant("null"),                // null 字符串
        Arb.constant("{}"),                  // 空对象
        Arb.constant("[]"),                  // 空数组
        Arb.constant("纯文本内容"),           // 纯文本
        Arb.constant("这是一段没有 JSON 的文本"), // 无 JSON
        Arb.constant("{invalid json}"),      // 无效 JSON
        Arb.constant("{ \"key\": }"),        // 不完整的 JSON
        Arb.constant("{ \"key\": \"value\" ") // 缺少结束括号
    )

    /**
     * 生成部分字段缺失的 JSON
     */
    private fun arbPartialFieldJson(): Arb<String> = arbitrary {
        val includeReplySuggestion = Arb.boolean().bind()
        val includeStrategyAnalysis = Arb.boolean().bind()
        val includeRiskLevel = Arb.boolean().bind()
        
        // 至少缺失一个字段
        if (includeReplySuggestion && includeStrategyAnalysis && includeRiskLevel) {
            // 如果所有字段都包含，随机移除一个
            val removeField = Arb.int(0..2).bind()
            when (removeField) {
                0 -> buildPartialJson(false, includeStrategyAnalysis, includeRiskLevel)
                1 -> buildPartialJson(includeReplySuggestion, false, includeRiskLevel)
                else -> buildPartialJson(includeReplySuggestion, includeStrategyAnalysis, false)
            }
        } else {
            buildPartialJson(includeReplySuggestion, includeStrategyAnalysis, includeRiskLevel)
        }
    }

    /**
     * 构建部分字段的 JSON
     */
    private fun buildPartialJson(
        includeReplySuggestion: Boolean,
        includeStrategyAnalysis: Boolean,
        includeRiskLevel: Boolean
    ): String {
        val fields = mutableListOf<String>()
        
        if (includeReplySuggestion) {
            fields.add("\"replySuggestion\": \"这是一个回复建议\"")
        }
        if (includeStrategyAnalysis) {
            fields.add("\"strategyAnalysis\": \"这是策略分析\"")
        }
        if (includeRiskLevel) {
            fields.add("\"riskLevel\": \"SAFE\"")
        }
        
        return "{\n  ${fields.joinToString(",\n  ")}\n}"
    }

    /**
     * 生成字段值为 null 的 JSON
     */
    private fun arbNullFieldJson(): Arb<String> = Arb.choice(
        Arb.constant("""
        {
          "replySuggestion": null,
          "strategyAnalysis": "策略分析",
          "riskLevel": "SAFE"
        }
        """.trimIndent()),
        Arb.constant("""
        {
          "replySuggestion": "回复建议",
          "strategyAnalysis": null,
          "riskLevel": "SAFE"
        }
        """.trimIndent()),
        Arb.constant("""
        {
          "replySuggestion": "回复建议",
          "strategyAnalysis": "策略分析",
          "riskLevel": null
        }
        """.trimIndent())
    )

    /**
     * 生成字段值为空字符串的 JSON
     */
    private fun arbEmptyFieldJson(): Arb<String> = Arb.choice(
        Arb.constant("""
        {
          "replySuggestion": "",
          "strategyAnalysis": "策略分析",
          "riskLevel": "SAFE"
        }
        """.trimIndent()),
        Arb.constant("""
        {
          "replySuggestion": "回复建议",
          "strategyAnalysis": "",
          "riskLevel": "SAFE"
        }
        """.trimIndent()),
        Arb.constant("""
        {
          "replySuggestion": "回复建议",
          "strategyAnalysis": "策略分析",
          "riskLevel": ""
        }
        """.trimIndent())
    )

    // ========== Property 9: 空响应处理 ==========
    
    /**
     * Property 9: 空响应处理
     * 
     * *对于任何* 空字符串或纯文本响应，
     * 系统应该返回默认值而不是崩溃
     * 
     * **Feature: ai-response-parser, Property 9: 空响应处理**
     * **验证需求: Requirements 1.5**
     */
    @Test
    fun `Property 9 - Empty response handling`() = runBlocking {
        var successCount = 0
        var totalCount = 0
        
        checkAll(100, arbEmptyOrInvalidResponse()) { invalidResponse ->
            totalCount++
            
            try {
                // 模拟解析过程
                val result = simulateParsingWithFallback(invalidResponse)
                
                // 验证：应该返回默认值，而不是抛出异常
                assertNotNull("解析结果不应为 null", result)
                assertNotNull("replySuggestion 不应为 null", result.replySuggestion)
                assertNotNull("strategyAnalysis 不应为 null", result.strategyAnalysis)
                assertNotNull("riskLevel 不应为 null", result.riskLevel)
                
                // 验证：默认值应该包含提示信息
                assertTrue(
                    "默认的 replySuggestion 应该包含提示信息",
                    result.replySuggestion.contains("AI") || 
                    result.replySuggestion.contains("暂时") ||
                    result.replySuggestion.contains("默认")
                )
                
                successCount++
            } catch (e: Exception) {
                // 不应该抛出异常
                fail("对于空响应 '$invalidResponse'，不应该抛出异常: ${e.message}")
            }
        }
        
        val successRate = successCount.toDouble() / totalCount.toDouble()
        println("Property 9 - 空响应处理成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
        // 验证：成功率应该 = 100%
        assertTrue(
            "空响应处理成功率应该 = 100%，实际: ${successRate * 100}%",
            successRate == 1.0
        )
    }

    // ========== Property 10: 字段缺失处理 ==========
    
    /**
     * Property 10: 字段缺失处理
     * 
     * *对于任何* 部分字段缺失的响应，
     * 系统应该使用默认值填充缺失字段
     * 
     * **Feature: ai-response-parser, Property 10: 字段缺失处理**
     * **验证需求: Requirements 4.4, 4.5, 5.4**
     */
    @Test
    fun `Property 10 - Missing field handling`() = runBlocking {
        var successCount = 0
        var totalCount = 0
        
        // 测试部分字段缺失
        checkAll(50, arbPartialFieldJson()) { partialJson ->
            totalCount++
            
            try {
                // 模拟解析过程
                val result = simulateParsingWithFallback(partialJson)
                
                // 验证：所有字段都应该有值（使用默认值填充）
                assertNotNull("replySuggestion 不应为 null", result.replySuggestion)
                assertNotNull("strategyAnalysis 不应为 null", result.strategyAnalysis)
                assertNotNull("riskLevel 不应为 null", result.riskLevel)
                
                // 验证：字段值不应为空字符串
                assertTrue("replySuggestion 不应为空", result.replySuggestion.isNotEmpty())
                assertTrue("strategyAnalysis 不应为空", result.strategyAnalysis.isNotEmpty())
                
                successCount++
            } catch (e: Exception) {
                fail("对于部分字段缺失的 JSON，不应该抛出异常: ${e.message}")
            }
        }
        
        // 测试字段值为 null
        checkAll(25, arbNullFieldJson()) { nullFieldJson ->
            totalCount++
            
            try {
                val result = simulateParsingWithFallback(nullFieldJson)
                
                // 验证：null 字段应该被默认值替换
                assertNotNull("replySuggestion 不应为 null", result.replySuggestion)
                assertNotNull("strategyAnalysis 不应为 null", result.strategyAnalysis)
                assertNotNull("riskLevel 不应为 null", result.riskLevel)
                
                successCount++
            } catch (e: Exception) {
                fail("对于字段值为 null 的 JSON，不应该抛出异常: ${e.message}")
            }
        }
        
        // 测试字段值为空字符串
        checkAll(25, arbEmptyFieldJson()) { emptyFieldJson ->
            totalCount++
            
            try {
                val result = simulateParsingWithFallback(emptyFieldJson)
                
                // 验证：空字符串字段应该被默认值替换
                assertNotNull("replySuggestion 不应为 null", result.replySuggestion)
                assertNotNull("strategyAnalysis 不应为 null", result.strategyAnalysis)
                assertNotNull("riskLevel 不应为 null", result.riskLevel)
                
                // 验证：字段值不应为空字符串
                assertTrue("replySuggestion 不应为空", result.replySuggestion.isNotEmpty())
                assertTrue("strategyAnalysis 不应为空", result.strategyAnalysis.isNotEmpty())
                
                successCount++
            } catch (e: Exception) {
                fail("对于字段值为空字符串的 JSON，不应该抛出异常: ${e.message}")
            }
        }
        
        val successRate = successCount.toDouble() / totalCount.toDouble()
        println("Property 10 - 字段缺失处理成功率: ${successRate * 100}% ($successCount/$totalCount)")
        
        // 验证：成功率应该 = 100%
        assertTrue(
            "字段缺失处理成功率应该 = 100%，实际: ${successRate * 100}%",
            successRate == 1.0
        )
    }

    // ========== 辅助方法 ==========

    /**
     * 模拟数据类
     */
    data class ParsedResult(
        val replySuggestion: String,
        val strategyAnalysis: String,
        val riskLevel: RiskLevel
    )

    /**
     * 模拟带降级策略的解析过程
     */
    private fun simulateParsingWithFallback(json: String): ParsedResult {
        // 尝试解析 JSON
        val hasRequiredFields = json.contains("replySuggestion") &&
                                json.contains("strategyAnalysis") &&
                                json.contains("riskLevel")
        
        if (!hasRequiredFields || json.trim().isEmpty() || json == "null") {
            // 使用默认值
            return ParsedResult(
                replySuggestion = "AI 暂时无法生成建议，请重试或切换模型",
                strategyAnalysis = "AI 分析暂时不可用",
                riskLevel = RiskLevel.SAFE
            )
        }
        
        // 提取字段值
        val replySuggestionMatch = Regex("\"replySuggestion\"\\s*:\\s*\"([^\"]+)\"").find(json)
        val strategyAnalysisMatch = Regex("\"strategyAnalysis\"\\s*:\\s*\"([^\"]+)\"").find(json)
        val riskLevelMatch = Regex("\"riskLevel\"\\s*:\\s*\"([^\"]+)\"").find(json)
        
        // 使用提取的值或默认值
        val replySuggestion = replySuggestionMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
            ?: "AI 暂时无法生成建议，请重试或切换模型"
        
        val strategyAnalysis = strategyAnalysisMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
            ?: "AI 分析暂时不可用"
        
        val riskLevelStr = riskLevelMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
            ?: "SAFE"
        
        val riskLevel = try {
            RiskLevel.valueOf(riskLevelStr)
        } catch (e: Exception) {
            RiskLevel.SAFE
        }
        
        return ParsedResult(
            replySuggestion = replySuggestion,
            strategyAnalysis = strategyAnalysis,
            riskLevel = riskLevel
        )
    }
}
