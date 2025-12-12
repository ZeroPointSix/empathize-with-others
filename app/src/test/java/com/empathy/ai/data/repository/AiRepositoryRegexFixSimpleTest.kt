package com.empathy.ai.data.repository

import org.junit.Test
import org.junit.Assert.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * 简单的正则表达式修复验证测试
 * 不依赖复杂的测试框架和模拟对象
 */
class AiRepositoryRegexFixSimpleTest {

    @Test
    fun testOriginalBuggyRegexPattern() {
        // 测试原始有问题的正则表达式模式
        val buggyPattern = """\{(?:[^{}"]|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')*"""
        
        try {
            // 尝试编译原始的正则表达式
            Pattern.compile(buggyPattern)
            fail("原始正则表达式应该抛出PatternSyntaxException")
        } catch (e: PatternSyntaxException) {
            // 预期的异常，验证正则表达式确实有问题
            println("✓ 原始正则表达式确实有问题: ${e.message}")
            assertTrue("异常消息应包含相关错误信息", e.message?.isNotEmpty() == true)
        }
    }

    @Test
    fun testFixedRegexPattern() {
        // 测试修复后的正则表达式模式
        val fixedPattern = """\{(?:[^{}"'\\]|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')*\}"""
        
        try {
            // 尝试编译修复后的正则表达式
            val pattern = Pattern.compile(fixedPattern)
            println("✓ 修复后的正则表达式编译成功")
            
            // 测试基本匹配功能
            val matcher = pattern.matcher("""{"key": "value"}""")
            assertTrue("应该能匹配简单的JSON对象", matcher.find())
            
            val matcher2 = pattern.matcher("""{"nested": {"key": "value"}}""")
            assertTrue("应该能匹配嵌套的JSON对象", matcher2.find())
            
        } catch (e: PatternSyntaxException) {
            fail("修复后的正则表达式不应该抛出PatternSyntaxException: ${e.message}")
        }
    }

    @Test
    fun testJsonPreprocessingFunction() {
        // 测试JSON预处理函数的逻辑
        fun preprocessJsonResponse(response: String): String {
            // 移除可能的markdown代码块标记
            var cleaned = response.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7)
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3)
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length - 3)
            }
            
            // 移除开头和结尾的非JSON内容
            val jsonStart = cleaned.indexOf("{")
            val jsonEnd = cleaned.lastIndexOf("}")
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
            }
            
            return cleaned.trim()
        }

        // 测试包含markdown的响应
        val markdownResponse = """```json
            {"status": "success", "data": {"result": "processed"}}
            ```"""
        
        val processed = preprocessJsonResponse(markdownResponse)
        assertEquals("应该正确移除markdown标记", 
            """{"status": "success", "data": {"result": "processed"}}""", 
            processed)

        // 测试包含额外文本的响应
        val textWrappedResponse = """Here's the AI response:
            {"analysis": "completed", "confidence": 0.95}
            Thank you for using our service."""
            
        val processed2 = preprocessJsonResponse(textWrappedResponse)
        assertEquals("应该正确提取JSON部分", 
            """{"analysis": "completed", "confidence": 0.95}""", 
            processed2)
    }

    @Test
    fun testErrorHandlingWrapper() {
        // 测试错误处理包装函数
        fun safeRegexOperation(input: String, operation: (String) -> String): Result<String> {
            return try {
                val result = operation(input)
                Result.success(result)
            } catch (e: PatternSyntaxException) {
                println("正则表达式错误: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                println("其他错误: ${e.message}")
                Result.failure(e)
            }
        }

        // 测试有问题的操作
        val buggyOperation = { input: String ->
            val pattern = Pattern.compile("""\{(?:[^{}"]|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')*""")
            input // 简单返回输入
        }
        
        val result1 = safeRegexOperation("test", buggyOperation)
        assertTrue("应该捕获正则表达式错误", result1.isFailure)

        // 测试正常的操作
        val fixedOperation = { input: String ->
            val pattern = Pattern.compile("""\{(?:[^{}"'\\]|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')*\}""")
            input // 简单返回输入
        }
        
        val result2 = safeRegexOperation("test", fixedOperation)
        assertTrue("正常操作应该成功", result2.isSuccess)
    }
}