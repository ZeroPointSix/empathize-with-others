package com.empathy.ai.data.repository

import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * AI 响应解析性能基准测试
 *
 * 测试目标：
 * - 常规响应（< 1KB）：≤ 300ms
 * - 复杂响应（1-5KB）：≤ 500ms
 * - 大型响应（5-10KB）：≤ 1000ms
 *
 * 注意：这些时间包括网络请求时间，纯解析时间会更短
 * 本测试只测量纯解析时间（不包括网络请求）
 */
class AiResponseParserPerformanceBenchmarkTest {

    /**
     * 测试常规响应解析性能（< 1KB）
     *
     * 目标：≤ 100ms（纯解析时间）
     */
    @Test
    fun `test regular response parsing performance`() {
        // 准备测试数据：标准格式的 AnalysisResult JSON（约 500 字节）
        val regularJson = """
            {
              "replySuggestion": "听起来你最近工作压力很大，要不要聊聊？",
              "strategyAnalysis": "对方可能处于焦虑状态，需要情感支持和理解。建议表达关心，避免说教。",
              "riskLevel": "SAFE"
            }
        """.trimIndent()

        // 预热（避免 JIT 编译影响）
        repeat(10) {
            preprocessAndParse(regularJson)
        }

        // 测量解析时间（多次测量取平均值）
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                preprocessAndParse(regularJson)
            }
        }

        val averageTime = totalTime / iterations
        println("常规响应平均解析时间: ${averageTime}ms")
        println("总时间: ${totalTime}ms, 迭代次数: $iterations")

        // 验证性能指标
        assert(averageTime <= 100) {
            "常规响应解析时间超标: ${averageTime}ms > 100ms"
        }
    }

    /**
     * 测试复杂响应解析性能（1-5KB）
     *
     * 目标：≤ 150ms（纯解析时间）
     */
    @Test
    fun `test complex response parsing performance`() {
        // 准备测试数据：包含 Markdown 标记和中文字段名的复杂 JSON（约 2KB）
        val complexJson = """
            ```json
            {
              "回复建议": "我理解你的感受，这确实不容易。我们可以一起想想办法。",
              "策略分析": "【对方状态】\n情绪: 略显沮丧\n原因: 工作压力大，项目进度落后\n\n【关键洞察】\n• 对方需要情感支持\n• 避免提及金钱话题（雷区）\n• 可以分享类似经历建立共鸣\n\n【策略建议】\n1. 表达理解和同情\n2. 提供实际帮助或建议\n3. 避免说教或批评\n4. 适时转移话题到轻松内容",
              "风险等级": "WARNING"
            }
            ```
        """.trimIndent()

        // 预热
        repeat(10) {
            preprocessAndParse(complexJson)
        }

        // 测量解析时间
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                preprocessAndParse(complexJson)
            }
        }

        val averageTime = totalTime / iterations
        println("复杂响应平均解析时间: ${averageTime}ms")
        println("总时间: ${totalTime}ms, 迭代次数: $iterations")

        // 验证性能指标
        assert(averageTime <= 150) {
            "复杂响应解析时间超标: ${averageTime}ms > 150ms"
        }
    }

    /**
     * 测试大型响应解析性能（5-10KB）
     *
     * 目标：≤ 300ms（纯解析时间）
     */
    @Test
    fun `test large response parsing performance`() {
        // 准备测试数据：大型 JSON（约 8KB）
        val largeJson = buildString {
            append("```json\n")
            append("{\n")
            append("  \"回复建议\": \"")
            append("这是一个很长的回复建议。".repeat(50))
            append("\",\n")
            append("  \"策略分析\": \"")
            append("这是一个很长的策略分析。".repeat(100))
            append("\",\n")
            append("  \"风险等级\": \"SAFE\"\n")
            append("}\n")
            append("```")
        }

        println("大型响应大小: ${largeJson.length} 字节")

        // 预热
        repeat(10) {
            preprocessAndParse(largeJson)
        }

        // 测量解析时间
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                preprocessAndParse(largeJson)
            }
        }

        val averageTime = totalTime / iterations
        println("大型响应平均解析时间: ${averageTime}ms")
        println("总时间: ${totalTime}ms, 迭代次数: $iterations")

        // 验证性能指标
        assert(averageTime <= 300) {
            "大型响应解析时间超标: ${averageTime}ms > 300ms"
        }
    }

    /**
     * 测试字段映射性能
     *
     * 测量字段映射操作的性能开销
     */
    @Test
    fun `test field mapping performance`() {
        val jsonWithChineseFields = """
            {
              "回复建议": "测试内容",
              "策略分析": "测试分析",
              "风险等级": "SAFE",
              "是否安全": true,
              "触发的风险": ["风险1", "风险2"],
              "建议": "测试建议",
              "事实": {"姓名": "张三", "年龄": "30"},
              "红色标签": ["标签1", "标签2"],
              "绿色标签": ["标签3", "标签4"]
            }
        """.trimIndent()

        // 预热
        repeat(10) {
            mapChineseFieldNames(jsonWithChineseFields)
        }

        // 测量映射时间
        val iterations = 1000
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                mapChineseFieldNames(jsonWithChineseFields)
            }
        }

        val averageTime = totalTime.toDouble() / iterations
        println("字段映射平均时间: ${String.format("%.3f", averageTime)}ms")
        println("总时间: ${totalTime}ms, 迭代次数: $iterations")

        // 验证性能指标（字段映射应该很快，< 10ms）
        assert(averageTime <= 10.0) {
            "字段映射时间超标: ${averageTime}ms > 10ms"
        }
    }

    /**
     * 测试 JSON 清洗性能
     *
     * 测量 JSON 清洗操作的性能开销
     */
    @Test
    fun `test JSON cleaning performance`() {
        val dirtyJson = """
            ```json
            这是一些前缀文本
            {
              "field1": "value1",
              "field2": "value2",
              "field3": "value3",
            }
            这是一些后缀文本
            ```
        """.trimIndent()

        // 预热
        repeat(10) {
            preprocessJsonResponse(dirtyJson)
        }

        // 测量清洗时间
        val iterations = 1000
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                preprocessJsonResponse(dirtyJson)
            }
        }

        val averageTime = totalTime.toDouble() / iterations
        println("JSON清洗平均时间: ${String.format("%.3f", averageTime)}ms")
        println("总时间: ${totalTime}ms, 迭代次数: $iterations")

        // 验证性能指标（JSON 清洗应该很快，< 5ms）
        assert(averageTime <= 5.0) {
            "JSON清洗时间超标: ${averageTime}ms > 5ms"
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 模拟完整的预处理和解析流程
     */
    private fun preprocessAndParse(json: String): String {
        // Step 1: 移除 Markdown 标记
        val step1 = removeMarkdownBlocks(json)
        
        // Step 2: 提取 JSON 对象
        val step2 = extractJsonObject(step1)
        
        // Step 3: 字段名映射
        val step3 = mapChineseFieldNames(step2)
        
        // Step 4: 修复常见格式错误
        val step4 = fixCommonJsonErrors(step3)
        
        return step4
    }

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
        var i = 0
        while (i < builder.length) {
            val char = builder[i]
            if (char == ',') {
                var nextNonWhitespace = i + 1
                while (nextNonWhitespace < builder.length && 
                       builder[nextNonWhitespace].isWhitespace()) {
                    nextNonWhitespace++
                }
                if (nextNonWhitespace < builder.length && 
                    (builder[nextNonWhitespace] == '}' || builder[nextNonWhitespace] == ']')) {
                    builder.deleteCharAt(i)
                    continue
                }
            }
            i++
        }
        return builder.toString()
    }

    private fun mapChineseFieldNames(json: String): String {
        // 使用默认映射配置
        val mappings = getDefaultMappings()
        var result = json
        
        mappings.forEach { (englishName, chineseNames) ->
            chineseNames.forEach { chineseName ->
                val pattern = "\"$chineseName\""
                if (result.contains(pattern)) {
                    result = result.replace(pattern, "\"$englishName\"")
                }
            }
        }
        
        return result
    }

    private fun preprocessJsonResponse(json: String): String {
        val step1 = removeMarkdownBlocks(json)
        val step2 = extractJsonObject(step1)
        val step3 = fixCommonJsonErrors(step2)
        return step3
    }

    private fun getDefaultMappings(): Map<String, List<String>> {
        return mapOf(
            "replySuggestion" to listOf("回复建议", "建议回复", "话术建议"),
            "strategyAnalysis" to listOf("策略分析", "心理分析", "军师分析"),
            "riskLevel" to listOf("风险等级", "风险级别"),
            "isSafe" to listOf("是否安全", "安全"),
            "triggeredRisks" to listOf("触发的风险", "风险列表"),
            "suggestion" to listOf("建议", "修改建议"),
            "facts" to listOf("事实", "事实信息"),
            "redTags" to listOf("红色标签", "雷区"),
            "greenTags" to listOf("绿色标签", "策略")
        )
    }
}
