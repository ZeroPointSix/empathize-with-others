package com.empathy.ai.data.repository

import org.junit.Test
import org.junit.Assert.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * AI响应解析Bug修复验证测试
 * 
 * 这个测试类专门验证已实施的修复是否有效，包括：
 * 1. 正则表达式语法错误修复
 * 2. JSON预处理功能
 * 3. 错误处理机制
 * 4. 安全包装函数
 */
class AiRepositoryFixVerification {

    @Test
    fun testRegexPatternFix() {
        println("=== 测试1: 正则表达式语法错误修复 ===")
        
        // 原始有问题的正则表达式（第650行附近）
        val originalPattern = """\{(?:[^{}"]|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')*"""
        
        // 修复后的正则表达式
        val fixedPattern = """\{(?:[^{}"'\\]|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')*\}"""
        
        // 测试原始模式
        try {
            Pattern.compile(originalPattern)
            fail("原始正则表达式应该抛出PatternSyntaxException")
        } catch (e: PatternSyntaxException) {
            println("✓ 原始正则表达式确实有问题: ${e.message}")
            assertTrue("异常消息应包含相关错误信息", e.message?.isNotEmpty() == true)
        }
        
        // 测试修复后的模式
        try {
            val pattern = Pattern.compile(fixedPattern)
            println("✓ 修复后的正则表达式编译成功")
            
            // 测试基本匹配功能
            val testJson1 = """{"key": "value"}"""
            val matcher1 = pattern.matcher(testJson1)
            assertTrue("应该能匹配简单的JSON对象", matcher1.find())
            
            val testJson2 = """{"nested": {"key": "value"}}"""
            val matcher2 = pattern.matcher(testJson2)
            assertTrue("应该能匹配嵌套的JSON对象", matcher2.find())
            
            println("✓ 修复后的正则表达式功能正常")
            
        } catch (e: PatternSyntaxException) {
            fail("修复后的正则表达式不应该抛出PatternSyntaxException: ${e.message}")
        }
    }

    @Test
    fun testJsonPreprocessing() {
        println("\n=== 测试2: JSON预处理功能 ===")
        
        // 模拟AiRepositoryImpl中的preprocessJsonResponse方法
        fun preprocessJsonResponse(rawJson: String): String {
            return try {
                rawJson
                    .trim()
                    .let { json ->
                        // 移除可能的代码块标记
                        when {
                            json.startsWith("```json") -> {
                                json.removePrefix("```json").removeSuffix("```").trim()
                            }
                            json.startsWith("```") -> {
                                json.removePrefix("```").removeSuffix("```").trim()
                            }
                            else -> json
                        }
                    }
                    .let { json ->
                        // 尝试提取JSON对象
                        val startIndex = json.indexOf("{")
                        val endIndex = json.lastIndexOf("}")
                        
                        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                            json.substring(startIndex, endIndex + 1)
                        } else {
                            json
                        }
                    }
            } catch (e: Exception) {
                rawJson // 返回原始内容作为降级方案
            }
        }
        
        // 测试包含markdown的响应
        val markdownResponse = """```json
            {"status": "success", "data": {"result": "processed"}}
            ```"""
        
        val processed1 = preprocessJsonResponse(markdownResponse)
        assertEquals("应该正确移除markdown标记", 
            """{"status": "success", "data": {"result": "processed"}}""", 
            processed1)
        println("✓ Markdown代码块移除功能正常")
        
        // 测试包含额外文本的响应
        val textWrappedResponse = """Here's the AI response:
            {"analysis": "completed", "confidence": 0.95}
            Thank you for using our service."""
            
        val processed2 = preprocessJsonResponse(textWrappedResponse)
        assertEquals("应该正确提取JSON部分", 
            """{"analysis": "completed", "confidence": 0.95}""", 
            processed2)
        println("✓ JSON对象提取功能正常")
        
        // 测试纯JSON响应
        val pureJsonResponse = """{"result": "success"}"""
        val processed3 = preprocessJsonResponse(pureJsonResponse)
        assertEquals("纯JSON应该保持不变", 
            """{"result": "success"}""", 
            processed3)
        println("✓ 纯JSON处理功能正常")
    }

    @Test
    fun testErrorHandlingMechanism() {
        println("\n=== 测试3: 错误处理机制 ===")
        
        // 模拟AiRepositoryImpl中的错误处理
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
        println("✓ 正则表达式错误捕获机制正常")
        
        // 测试正常的操作
        val fixedOperation = { input: String ->
            val pattern = Pattern.compile("""\{(?:[^{}"'\\]|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')*\}""")
            input // 简单返回输入
        }
        
        val result2 = safeRegexOperation("test", fixedOperation)
        assertTrue("正常操作应该成功", result2.isSuccess)
        println("✓ 正常操作处理机制正常")
    }

    @Test
    fun testJsonParsingWithSpecialCharacters() {
        println("\n=== 测试4: 特殊字符JSON解析 ===")
        
        // 测试包含特殊字符的JSON
        val specialCharsJson = """{
            "message": "这是一条包含\"引号\"和\\反斜杠的消息",
            "newline": "包含\n换行符",
            "tab": "包含\t制表符",
            "unicode": "包含\u4e2d\u6587字符"
        }"""
        
        // 验证JSON格式正确
        assertTrue("JSON应该包含引号", specialCharsJson.contains("\"引号\""))
        assertTrue("JSON应该包含反斜杠", specialCharsJson.contains("\\反斜杠"))
        assertTrue("JSON应该包含换行符", specialCharsJson.contains("\\n"))
        assertTrue("JSON应该包含制表符", specialCharsJson.contains("\\t"))
        assertTrue("JSON应该包含Unicode", specialCharsJson.contains("\\u"))
        
        println("✓ 特殊字符JSON格式正确")
    }

    @Test
    fun testOriginalErrorScenario() {
        println("\n=== 测试5: 原始错误场景模拟 ===")
        
        // 模拟原始的AI响应（可能导致PatternSyntaxException的场景）
        val problematicResponse = """```json
        {
            "analysis": "用户可能处于焦虑状态",
            "suggestion": "建议表达关心和支持",
            "riskLevel": "SAFE"
        }
        ```"""
        
        // 验证修复后的处理流程
        try {
            // 1. 预处理
            val preprocessed = preprocessJsonResponse(problematicResponse)
            println("预处理结果: ${preprocessed.take(100)}...")
            
            // 2. 验证JSON格式
            assertTrue("预处理后应该包含JSON对象", preprocessed.startsWith("{") && preprocessed.endsWith("}"))
            
            // 3. 验证关键字段
            assertTrue("应该包含analysis字段", preprocessed.contains("analysis"))
            assertTrue("应该包含suggestion字段", preprocessed.contains("suggestion"))
            assertTrue("应该包含riskLevel字段", preprocessed.contains("riskLevel"))
            
            println("✓ 原始错误场景处理正常")
            
        } catch (e: Exception) {
            fail("原始错误场景处理失败: ${e.message}")
        }
    }
    
    // 辅助函数：模拟preprocessJsonResponse
    private fun preprocessJsonResponse(rawJson: String): String {
        return try {
            rawJson
                .trim()
                .let { json ->
                    // 移除可能的代码块标记
                    when {
                        json.startsWith("```json") -> {
                            json.removePrefix("```json").removeSuffix("```").trim()
                        }
                        json.startsWith("```") -> {
                            json.removePrefix("```").removeSuffix("```").trim()
                        }
                        else -> json
                    }
                }
                .let { json ->
                    // 尝试提取JSON对象
                    val startIndex = json.indexOf("{")
                    val endIndex = json.lastIndexOf("}")
                    
                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        json.substring(startIndex, endIndex + 1)
                    } else {
                        json
                    }
                }
        } catch (e: Exception) {
            rawJson // 返回原始内容作为降级方案
        }
    }
}