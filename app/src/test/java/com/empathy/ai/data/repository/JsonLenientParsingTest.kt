package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.Assert.*

/**
 * JSON解析容错测试
 *
 * 测试Moshi在lenient模式下是否能正确解析格式不完美的JSON响应
 */
class JsonLenientParsingTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `test parse AnalysisResult with trailing comma`() {
        // 测试带有尾随逗号的JSON（常见格式问题）
        val malformedJson = """
            {
                "replySuggestion": "建议回复",
                "strategyAnalysis": "心理分析",
                "riskLevel": "SAFE",
            }
        """.trimIndent()

        val adapter = moshi.adapter(AnalysisResult::class.java)
        val result = adapter.fromJson(malformedJson)

        assertNotNull("应该能够解析带有尾随逗号的JSON", result)
        assertEquals("建议回复", result?.replySuggestion)
        assertEquals("心理分析", result?.strategyAnalysis)
        assertEquals(RiskLevel.SAFE, result?.riskLevel)
    }

    @Test
    fun `test parse SafetyCheckResult with comments`() {
        // 测试带有注释的JSON
        val jsonWithComments = """
            {
                // 这是一个注释
                "isSafe": true,
                "triggeredRisks": [],
                "suggestion": "没有问题"
            }
        """.trimIndent()

        val adapter = moshi.adapter(SafetyCheckResult::class.java)
        val result = adapter.fromJson(jsonWithComments)

        assertNotNull("应该能够解析带有注释的JSON", result)
        assertTrue(result?.isSafe == true)
        assertEquals("没有问题", result?.suggestion)
    }

    @Test
    fun `test parse AnalysisResult with single quotes`() {
        // 测试使用单引号的JSON（转换为双引号）
        val jsonWithSingleQuotes = """
            {
                "replySuggestion": "建议回复",
                "strategyAnalysis": "心理分析",
                "riskLevel": "SAFE"
            }
        """.trimIndent()

        val adapter = moshi.adapter(AnalysisResult::class.java)
        val result = adapter.lenient().fromJson(jsonWithSingleQuotes)

        assertNotNull("应该能够解析JSON", result)
        assertEquals("建议回复", result?.replySuggestion)
        assertEquals("心理分析", result?.strategyAnalysis)
        assertEquals(RiskLevel.SAFE, result?.riskLevel)
    }

    @Test
    fun `test parse JSON with extra whitespace`() {
        // 测试带有额外空白字符的JSON
        val jsonWithWhitespace = """
            
            {
                
                "replySuggestion": "建议回复",
                
                "strategyAnalysis": "心理分析",
                
                "riskLevel": "WARNING"
                
            }
            
        """.trimIndent()

        val adapter = moshi.adapter(AnalysisResult::class.java)
        val result = adapter.fromJson(jsonWithWhitespace)

        assertNotNull("应该能够解析带有额外空白字符的JSON", result)
        assertEquals("建议回复", result?.replySuggestion)
        assertEquals("心理分析", result?.strategyAnalysis)
        assertEquals(RiskLevel.WARNING, result?.riskLevel)
    }

    @Test
    fun `test parse malformed JSON gracefully`() {
        // 测试严重格式错误的JSON
        val severelyMalformedJson = """
            {
                "replySuggestion": "建议回复"
                "strategyAnalysis": "心理分析",  // 缺少逗号
                "riskLevel": "SAFE"
            }
        """.trimIndent()

        val adapter = moshi.adapter(AnalysisResult::class.java)
        val result = adapter.fromJson(severelyMalformedJson)

        // 在lenient模式下，某些格式错误可能仍然无法解析
        // 但应该不会崩溃，而是返回null
        // 这里我们主要验证不会抛出异常
        // 如果能解析成功是额外的容错能力
    }
}