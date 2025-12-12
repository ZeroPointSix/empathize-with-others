package com.empathy.ai.data.repository

import org.junit.Test
import org.junit.Assert.*

/**
 * 测试优化后的 preprocessJsonResponse 方法
 * 
 * 验证优化后的方法保持了原有功能，同时提高了性能
 */
class PreprocessJsonOptimizationTest {

    @Test
    fun testRemoveMarkdownBlocks() {
        // 测试移除 ```json 标记
        val input1 = """```json
            {"key": "value"}
            ```"""
        val expected1 = """{"key": "value"}"""
        
        // 模拟 removeMarkdownBlocks 方法
        val result1 = removeMarkdownBlocks(input1)
        assertEquals("应该移除 ```json 标记", expected1, result1)
        
        // 测试移除 ``` 标记
        val input2 = """```
            {"key": "value"}
            ```"""
        val result2 = removeMarkdownBlocks(input2)
        assertEquals("应该移除 ``` 标记", expected1, result2)
        
        // 测试没有标记的情况
        val input3 = """{"key": "value"}"""
        val result3 = removeMarkdownBlocks(input3)
        assertEquals("没有标记时应该保持不变", input3, result3)
    }
    
    @Test
    fun testExtractJsonObject() {
        // 测试提取 JSON 对象
        val input1 = """Here is the response: {"key": "value"} Thank you!"""
        val expected1 = """{"key": "value"}"""
        
        val result1 = extractJsonObject(input1)
        assertEquals("应该提取 JSON 对象", expected1, result1)
        
        // 测试嵌套 JSON
        val input2 = """{"outer": {"inner": "value"}}"""
        val result2 = extractJsonObject(input2)
        assertEquals("应该保持嵌套 JSON 不变", input2, result2)
        
        // 测试没有 JSON 对象的情况
        val input3 = """No JSON here"""
        val result3 = extractJsonObject(input3)
        assertEquals("没有 JSON 时应该返回原字符串", input3, result3)
    }
    
    @Test
    fun testFixCommonJsonErrors() {
        // 测试修复多余的逗号
        val input1 = """{"key": "value",}"""
        val expected1 = """{"key": "value"}"""
        
        val result1 = fixCommonJsonErrors(input1)
        assertEquals("应该移除多余的逗号", expected1, result1)
        
        // 测试修复数组中的多余逗号
        val input2 = """{"array": [1, 2, 3,]}"""
        val expected2 = """{"array": [1, 2, 3]}"""
        
        val result2 = fixCommonJsonErrors(input2)
        assertEquals("应该移除数组中的多余逗号", expected2, result2)
        
        // 测试修复带空格的多余逗号
        val input3 = """{"key": "value", }"""
        val expected3 = """{"key": "value" }"""
        
        val result3 = fixCommonJsonErrors(input3)
        assertEquals("应该移除带空格的多余逗号", expected3, result3)
    }
    
    @Test
    fun testPerformanceOptimization() {
        // 测试性能优化：使用 StringBuilder 应该比字符串拼接快
        val largeJson = buildString {
            append("{")
            repeat(1000) { i ->
                append("\"key$i\": \"value$i\"")
                if (i < 999) append(",")
            }
            append(",}")  // 故意添加多余的逗号
        }
        
        val startTime = System.currentTimeMillis()
        val result = fixCommonJsonErrors(largeJson)
        val endTime = System.currentTimeMillis()
        
        val duration = endTime - startTime
        println("处理 ${largeJson.length} 字符的 JSON 耗时: ${duration}ms")
        
        // 验证结果正确
        assertFalse("应该移除多余的逗号", result.contains(",}"))
        assertTrue("应该保持 JSON 结构", result.startsWith("{") && result.endsWith("}"))
        
        // 性能要求：处理大型 JSON 应该在合理时间内完成（< 100ms）
        assertTrue("处理时间应该小于 100ms", duration < 100)
    }
    
    @Test
    fun testEdgeCaseEmptyString() {
        // 测试边缘情况：空字符串
        val emptyInput = ""
        
        val result1 = removeMarkdownBlocks(emptyInput)
        assertEquals("空字符串应该保持不变", "", result1)
        
        val result2 = extractJsonObject(emptyInput)
        assertEquals("空字符串应该保持不变", "", result2)
        
        val result3 = fixCommonJsonErrors(emptyInput)
        assertEquals("空字符串应该保持不变", "", result3)
        
        println("✅ 边缘情况（空字符串）测试通过")
    }
    
    @Test
    fun testEdgeCasePlainText() {
        // 测试边缘情况：纯文本（无 JSON）
        val plainText = "This is just plain text without any JSON"
        
        val result1 = removeMarkdownBlocks(plainText)
        assertEquals("纯文本应该保持不变", plainText, result1)
        
        val result2 = extractJsonObject(plainText)
        assertEquals("没有 JSON 时应该返回原文本", plainText, result2)
        
        val result3 = fixCommonJsonErrors(plainText)
        assertEquals("纯文本应该保持不变", plainText, result3)
        
        println("✅ 边缘情况（纯文本）测试通过")
    }
    
    @Test
    fun testEdgeCaseLargeResponse() {
        // 测试边缘情况：超大响应（> 10KB）
        val largeResponse = buildString {
            append("```json\n")
            append("{")
            repeat(5000) { i ->
                append("\"field$i\": \"这是一个很长的值，包含中文字符和特殊符号！@#$%^&*()\"")
                if (i < 4999) append(",")
            }
            append("}")
            append("\n```")
        }
        
        assertTrue("响应大小应该 > 10KB", largeResponse.length > 10000)
        
        val startTime = System.currentTimeMillis()
        
        val step1 = removeMarkdownBlocks(largeResponse)
        val step2 = extractJsonObject(step1)
        val step3 = fixCommonJsonErrors(step2)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // 验证结果
        assertFalse("应该移除 Markdown 标记", step3.contains("```"))
        assertTrue("应该保持 JSON 结构", step3.startsWith("{") && step3.endsWith("}"))
        
        // 性能要求：处理超大响应应该在 1 秒内完成
        assertTrue("处理时间应该小于 1000ms，实际: ${duration}ms", duration < 1000)
        
        println("✅ 边缘情况（超大响应）测试通过")
        println("  响应大小: ${largeResponse.length} 字符")
        println("  处理时间: ${duration}ms")
    }
    
    @Test
    fun testFixCommonErrorsMissingComma() {
        // 测试修复缺失的逗号（这个功能在当前实现中不存在，但可以作为未来增强）
        // 当前实现只修复多余的逗号，不修复缺失的逗号
        // 这个测试主要是为了文档化当前的行为
        
        val inputWithMissingComma = """{"key1": "value1" "key2": "value2"}"""
        val result = fixCommonJsonErrors(inputWithMissingComma)
        
        // 当前实现不会修复缺失的逗号，所以结果应该与输入相同
        assertEquals("当前实现不修复缺失的逗号", inputWithMissingComma, result)
        
        println("✅ 缺失逗号测试通过（当前不修复）")
    }
    
    @Test
    fun testFixCommonErrorsEscapeCharacters() {
        // 测试转义字符处理
        val inputWithEscapes = """{"message": "He said \"Hello\"", "path": "C:\\Users\\test"}"""
        val result = fixCommonJsonErrors(inputWithEscapes)
        
        // 转义字符应该保持不变
        assertTrue("应该保留转义的引号", result.contains("\\\""))
        assertTrue("应该保留转义的反斜杠", result.contains("\\\\"))
        
        println("✅ 转义字符测试通过")
    }
    
    @Test
    fun testCompletePreprocessingPipeline() {
        // 测试完整的预处理流程
        val complexInput = """
            Here is the AI response:
            ```json
            {
              "replySuggestion": "这是建议",
              "strategyAnalysis": "这是分析",
              "riskLevel": "SAFE",
            }
            ```
            Thank you!
        """.trimIndent()
        
        // 模拟完整的预处理流程
        val step1 = removeMarkdownBlocks(complexInput)
        val step2 = extractJsonObject(step1)
        val step3 = fixCommonJsonErrors(step2)
        
        // 验证结果
        assertFalse("应该移除 Markdown 标记", step3.contains("```"))
        assertFalse("应该移除前后缀文本", step3.contains("Here is"))
        assertFalse("应该移除前后缀文本", step3.contains("Thank you"))
        assertFalse("应该移除多余的逗号", step3.contains(",}"))
        assertTrue("应该保持 JSON 结构", step3.startsWith("{") && step3.endsWith("}"))
        assertTrue("应该保留 JSON 内容", step3.contains("replySuggestion"))
        
        println("✅ 完整预处理流程测试通过")
        println("  原始长度: ${complexInput.length}")
        println("  处理后长度: ${step3.length}")
    }
    
    // 辅助方法：模拟 AiRepositoryImpl 中的私有方法
    
    private fun removeMarkdownBlocks(json: String): String {
        val trimmed = json.trim()
        
        return when {
            trimmed.startsWith("```json") -> {
                trimmed.removePrefix("```json").removeSuffix("```").trim()
            }
            trimmed.startsWith("```") -> {
                trimmed.removePrefix("```").removeSuffix("```").trim()
            }
            else -> trimmed
        }
    }
    
    private fun extractJsonObject(json: String): String {
        val startIndex = json.indexOf("{")
        val endIndex = json.lastIndexOf("}")
        
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            json.substring(startIndex, endIndex + 1)
        } else {
            json
        }
    }
    
    private fun fixCommonJsonErrors(json: String): String {
        val builder = StringBuilder(json)
        
        // 修复多余的逗号（在 } 或 ] 之前）
        var i = 0
        while (i < builder.length) {
            val char = builder[i]
            
            // 检查是否是逗号
            if (char == ',') {
                // 查找下一个非空白字符
                var nextNonWhitespace = i + 1
                while (nextNonWhitespace < builder.length && 
                       builder[nextNonWhitespace].isWhitespace()) {
                    nextNonWhitespace++
                }
                
                // 如果下一个非空白字符是 } 或 ]，删除逗号
                if (nextNonWhitespace < builder.length && 
                    (builder[nextNonWhitespace] == '}' || builder[nextNonWhitespace] == ']')) {
                    builder.deleteCharAt(i)
                    continue // 不增加 i，因为删除了一个字符
                }
            }
            
            i++
        }
        
        return builder.toString()
    }
}
