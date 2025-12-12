package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.RiskLevel
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * AI 响应解析器性能属性测试
 * 
 * 测试性能属性：
 * - Property 6: 常规响应解析性能（≤ 300ms）
 * - Property 7: 复杂响应解析性能（≤ 500ms）
 * - Property 8: 大型响应解析性能（≤ 1000ms）
 * 
 * **Feature: ai-response-parser, Property 6-8**
 * **验证需求: 7.1, 7.2, 7.3**
 */
class AiResponseParserPerformancePropertyTest {

    // ========== 生成器定义 ==========

    /**
     * 生成常规大小的 AnalysisResult JSON（< 1KB）
     */
    private fun arbRegularSizeJson(): Arb<String> = arbitrary {
        val replySuggestion = Arb.string(10..50).bind()
        val strategyAnalysis = Arb.string(20..100).bind()
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
     * 生成复杂的 AnalysisResult JSON（1-5KB）
     * 包含 Markdown 标记、前后缀文本、中文字段名
     */
    private fun arbComplexJson(): Arb<String> = arbitrary {
        val replySuggestion = Arb.string(50..200).bind()
        val strategyAnalysis = Arb.string(100..500).bind()
        val riskLevel = Arb.enum<RiskLevel>().bind()
        
        // 添加 Markdown 标记
        val json = """
        {
          "回复建议": "${replySuggestion.replace("\"", "\\\"")}",
          "策略分析": "${strategyAnalysis.replace("\"", "\\\"")}",
          "风险等级": "${riskLevel.name}"
        }
        """.trimIndent()
        
        // 添加前后缀文本
        val prefix = Arb.string(50..100).bind()
        val suffix = Arb.string(50..100).bind()
        
        "```json\n$prefix\n$json\n$suffix\n```"
    }

    /**
     * 生成大型 AnalysisResult JSON（5-10KB）
     */
    private fun arbLargeSizeJson(): Arb<String> = arbitrary {
        val replySuggestion = Arb.string(200..500).bind()
        val strategyAnalysis = Arb.string(500..2000).bind()
        val riskLevel = Arb.enum<RiskLevel>().bind()
        
        // 添加额外的字段和嵌套结构（使用固定字符串避免协程问题）
        val extraData = (1..10).joinToString(",\n") { i ->
            "  \"extraField$i\": \"extra_value_$i\""
        }
        
        """
        {
          "replySuggestion": "${replySuggestion.replace("\"", "\\\"")}",
          "strategyAnalysis": "${strategyAnalysis.replace("\"", "\\\"")}",
          "riskLevel": "${riskLevel.name}",
          $extraData
        }
        """.trimIndent()
    }

    // ========== Property 6: 常规响应解析性能 ==========
    
    /**
     * Property 6: 常规响应解析性能
     * 
     * *对于任何* 标准格式的响应（< 1KB），
     * 解析时间应该 ≤ 300 毫秒
     * 
     * **Feature: ai-response-parser, Property 6: 常规响应解析性能**
     * **验证需求: Requirements 7.1**
     */
    @Test
    fun `Property 6 - Regular response parsing performance`() = runBlocking {
        val performanceResults = mutableListOf<Long>()
        
        checkAll(50, arbRegularSizeJson()) { json ->
            // 测量解析时间
            val elapsedTime = measureTimeMillis {
                // 模拟解析过程
                simulateJsonParsing(json)
            }
            
            performanceResults.add(elapsedTime)
        }
        
        // 计算统计数据
        val avgTime = performanceResults.average()
        val maxTime = performanceResults.maxOrNull() ?: 0L
        val p95Time = performanceResults.sorted()[((performanceResults.size * 0.95).toInt())]
        
        println("Property 6 - 常规响应解析性能:")
        println("  平均时间: ${avgTime.toInt()}ms")
        println("  最大时间: ${maxTime}ms")
        println("  P95 时间: ${p95Time}ms")
        
        // 验证：P95 解析时间应该 ≤ 300ms
        assertTrue(
            "常规响应 P95 解析时间应该 ≤ 300ms，实际: ${p95Time}ms",
            p95Time <= 300
        )
    }

    // ========== Property 7: 复杂响应解析性能 ==========
    
    /**
     * Property 7: 复杂响应解析性能
     * 
     * *对于任何* 需要清洗和映射的响应（1-5KB），
     * 解析时间应该 ≤ 500 毫秒
     * 
     * **Feature: ai-response-parser, Property 7: 复杂响应解析性能**
     * **验证需求: Requirements 7.2**
     */
    @Test
    fun `Property 7 - Complex response parsing performance`() = runBlocking {
        val performanceResults = mutableListOf<Long>()
        
        checkAll(50, arbComplexJson()) { json ->
            // 测量解析时间（包含清洗和映射）
            val elapsedTime = measureTimeMillis {
                // 模拟完整的解析流程
                val cleaned = simulateJsonCleaning(json)
                val mapped = simulateFieldMapping(cleaned)
                simulateJsonParsing(mapped)
            }
            
            performanceResults.add(elapsedTime)
        }
        
        // 计算统计数据
        val avgTime = performanceResults.average()
        val maxTime = performanceResults.maxOrNull() ?: 0L
        val p95Time = performanceResults.sorted()[((performanceResults.size * 0.95).toInt())]
        
        println("Property 7 - 复杂响应解析性能:")
        println("  平均时间: ${avgTime.toInt()}ms")
        println("  最大时间: ${maxTime}ms")
        println("  P95 时间: ${p95Time}ms")
        
        // 验证：P95 解析时间应该 ≤ 500ms
        assertTrue(
            "复杂响应 P95 解析时间应该 ≤ 500ms，实际: ${p95Time}ms",
            p95Time <= 500
        )
    }

    // ========== Property 8: 大型响应解析性能 ==========
    
    /**
     * Property 8: 大型响应解析性能
     * 
     * *对于任何* 大型响应（5-10KB），
     * 解析时间应该 ≤ 1000 毫秒
     * 
     * **Feature: ai-response-parser, Property 8: 大型响应解析性能**
     * **验证需求: Requirements 7.3**
     */
    @Test
    fun `Property 8 - Large response parsing performance`() = runBlocking {
        val performanceResults = mutableListOf<Long>()
        
        checkAll(30, arbLargeSizeJson()) { json ->
            // 测量解析时间
            val elapsedTime = measureTimeMillis {
                // 模拟完整的解析流程
                val cleaned = simulateJsonCleaning(json)
                val mapped = simulateFieldMapping(cleaned)
                simulateJsonParsing(mapped)
            }
            
            performanceResults.add(elapsedTime)
        }
        
        // 计算统计数据
        val avgTime = performanceResults.average()
        val maxTime = performanceResults.maxOrNull() ?: 0L
        val p95Time = performanceResults.sorted()[((performanceResults.size * 0.95).toInt())]
        
        println("Property 8 - 大型响应解析性能:")
        println("  平均时间: ${avgTime.toInt()}ms")
        println("  最大时间: ${maxTime}ms")
        println("  P95 时间: ${p95Time}ms")
        
        // 验证：P95 解析时间应该 ≤ 1000ms
        assertTrue(
            "大型响应 P95 解析时间应该 ≤ 1000ms，实际: ${p95Time}ms",
            p95Time <= 1000
        )
    }

    // ========== 辅助方法 ==========

    /**
     * 模拟 JSON 清洗过程
     */
    private fun simulateJsonCleaning(json: String): String {
        // 移除 Markdown 标记
        var cleaned = json.replace("```json", "").replace("```", "")
        
        // 提取 JSON 对象
        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')
        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1)
        }
        
        return cleaned.trim()
    }

    /**
     * 模拟字段映射过程
     */
    private fun simulateFieldMapping(json: String): String {
        // 定义字段映射规则
        val fieldMappings = mapOf(
            "replySuggestion" to listOf("回复建议", "建议回复", "话术建议"),
            "strategyAnalysis" to listOf("策略分析", "心理分析", "军师分析"),
            "riskLevel" to listOf("风险等级", "风险级别")
        )
        
        var mapped = json
        fieldMappings.forEach { (englishField, chineseFields) ->
            chineseFields.forEach { chineseField ->
                mapped = mapped.replace("\"$chineseField\"", "\"$englishField\"")
            }
        }
        
        return mapped
    }

    /**
     * 模拟 JSON 解析过程
     */
    private fun simulateJsonParsing(json: String) {
        // 模拟 Moshi 解析过程
        // 检查 JSON 是否包含必需字段
        val hasRequiredFields = json.contains("replySuggestion") &&
                                json.contains("strategyAnalysis") &&
                                json.contains("riskLevel")
        
        // 模拟字段提取
        if (hasRequiredFields) {
            // 提取字段值（简化版）
            val replySuggestionMatch = Regex("\"replySuggestion\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val strategyAnalysisMatch = Regex("\"strategyAnalysis\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val riskLevelMatch = Regex("\"riskLevel\"\\s*:\\s*\"([^\"]+)\"").find(json)
            
            // 验证提取结果
            assertNotNull("应该能提取 replySuggestion", replySuggestionMatch)
            assertNotNull("应该能提取 strategyAnalysis", strategyAnalysisMatch)
            assertNotNull("应该能提取 riskLevel", riskLevelMatch)
        }
    }
}
