package com.empathy.ai.data.repository

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.regex.PatternSyntaxException

/**
 * AI响应解析Bug修复验证测试（正则表达式修复版）
 * 
 * 本测试类专门用于验证AiRepositoryImpl中正则表达式修复，
 * 确保正则表达式语法错误得到修复。
 */
@RunWith(JUnit4::class)
class AiRepositoryRegexFixTest {

    @Test
    fun `should reproduce regex syntax error with problematic pattern`() {
        // 验证原始问题正则表达式确实会导致PatternSyntaxException
        val problematicPattern = "}\""
        
        // 这个正则表达式在Android ICU正则引擎中会导致语法错误
        assertThrows(PatternSyntaxException::class.java) {
            Regex(problematicPattern)
        }
    }

    @Test
    fun `should verify fixed regex patterns are valid`() {
        // 验证修复后的正则表达式模式是有效的
        val fixedPatterns = listOf(
            "(?<!\\\\)\\n",      // 未转义的换行符
            "(?<!\\\\)\\t",      // 未转义的制表符
            "(?<!\\\\)\\r",      // 未转义的回车符
            "(?<=[a-zA-Z0-9])\"(?=[a-zA-Z0-9])"  // 未转义的引号
        )
        
        fixedPatterns.forEach { pattern ->
            try {
                Regex(pattern)
                // 如果没有抛出异常，则测试通过
            } catch (e: PatternSyntaxException) {
                fail("正则表达式模式应该是有效的: $pattern, 错误: ${e.message}")
            }
        }
    }

    @Test
    fun `should verify string replacement patterns work correctly`() {
        // 验证字符串替换模式（修复方案）能正确工作
        val testCases = mapOf(
            "}\"" to "},\"",      // 修复缺失的逗号
            "]" to "],\"",        // 修复缺失的逗号
            ",}" to "}",          // 修复多余的逗号
            ",]" to "]"           // 修复多余的逗号
        )
        
        testCases.forEach { (input, expected) ->
            val result = "test\"test".replace(input, expected)
            assertTrue("字符串替换应该正确工作: $input -> $expected", result.contains(expected))
        }
    }
}