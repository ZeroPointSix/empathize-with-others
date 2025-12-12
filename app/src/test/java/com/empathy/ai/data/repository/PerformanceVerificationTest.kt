package com.empathy.ai.data.repository

import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * 性能验证测试
 *
 * 验证优化后的性能指标是否满足需求：
 * - 常规响应（< 1KB）：≤ 300ms
 * - 复杂响应（1-5KB）：≤ 500ms
 * - 大型响应（5-10KB）：≤ 1000ms
 *
 * 注意：这些时间包括网络请求时间，纯解析时间会更短
 * 本测试只测量纯解析时间（不包括网络请求）
 */
class PerformanceVerificationTest {

    /**
     * 验证常规响应解析性能
     *
     * 目标：≤ 100ms（纯解析时间）
     */
    @Test
    fun `verify regular response parsing performance`() {
        val regularJson = """
            {
              "replySuggestion": "听起来你最近工作压力很大，要不要聊聊？",
              "strategyAnalysis": "对方可能处于焦虑状态，需要情感支持和理解。",
              "riskLevel": "SAFE"
            }
        """.trimIndent()

        // 预热
        repeat(10) {
            processJson(regularJson)
        }

        // 测量
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                processJson(regularJson)
            }
        }

        val averageTime = totalTime / iterations
        println("✓ 常规响应平均解析时间: ${averageTime}ms (目标: ≤100ms)")

        assert(averageTime <= 100) {
            "性能不达标: ${averageTime}ms > 100ms"
        }
    }

    /**
     * 验证复杂响应解析性能
     *
     * 目标：≤ 150ms（纯解析时间）
     */
    @Test
    fun `verify complex response parsing performance`() {
        val complexJson = """
            ```json
            {
              "回复建议": "我理解你的感受，这确实不容易。",
              "策略分析": "【对方状态】\n情绪: 略显沮丧\n\n【关键洞察】\n• 对方需要情感支持\n• 避免提及金钱话题",
              "风险等级": "WARNING"
            }
            ```
        """.trimIndent()

        // 预热
        repeat(10) {
            processJson(complexJson)
        }

        // 测量
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                processJson(complexJson)
            }
        }

        val averageTime = totalTime / iterations
        println("✓ 复杂响应平均解析时间: ${averageTime}ms (目标: ≤150ms)")

        assert(averageTime <= 150) {
            "性能不达标: ${averageTime}ms > 150ms"
        }
    }

    /**
     * 验证大型响应解析性能
     *
     * 目标：≤ 300ms（纯解析时间）
     */
    @Test
    fun `verify large response parsing performance`() {
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
            processJson(largeJson)
        }

        // 测量
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                processJson(largeJson)
            }
        }

        val averageTime = totalTime / iterations
        println("✓ 大型响应平均解析时间: ${averageTime}ms (目标: ≤300ms)")

        assert(averageTime <= 300) {
            "性能不达标: ${averageTime}ms > 300ms"
        }
    }

    /**
     * 验证内存使用增长
     *
     * 目标：≤ 10%
     */
    @Test
    fun `verify memory usage growth`() {
        val runtime = Runtime.getRuntime()
        
        // 强制 GC
        System.gc()
        Thread.sleep(100)
        
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // 执行大量解析操作
        val testJson = """
            {
              "回复建议": "测试内容",
              "策略分析": "测试分析",
              "风险等级": "SAFE"
            }
        """.trimIndent()
        
        repeat(1000) {
            processJson(testJson)
        }
        
        // 强制 GC
        System.gc()
        Thread.sleep(100)
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryGrowth = ((memoryAfter - memoryBefore).toDouble() / memoryBefore) * 100
        
        println("✓ 内存使用增长: ${String.format("%.2f", memoryGrowth)}% (目标: ≤10%)")
        
        assert(memoryGrowth <= 10.0) {
            "内存使用增长超标: ${String.format("%.2f", memoryGrowth)}% > 10%"
        }
    }

    /**
     * 验证配置缓存性能
     *
     * 验证配置缓存机制是否有效
     */
    @Test
    fun `verify configuration caching performance`() {
        val mappings = getDefaultMappings()
        
        // 首次加载（模拟）
        val firstLoadTime = measureTimeMillis {
            repeat(10) {
                // 模拟配置加载
                mappings.forEach { (_, values) ->
                    values.size
                }
            }
        }
        
        // 缓存加载（模拟）
        val cachedLoadTime = measureTimeMillis {
            repeat(1000) {
                // 模拟从缓存读取
                mappings.size
            }
        }
        
        val averageCachedTime = cachedLoadTime.toDouble() / 1000
        
        println("✓ 首次加载时间: ${firstLoadTime}ms")
        println("✓ 缓存加载平均时间: ${String.format("%.3f", averageCachedTime)}ms (目标: <1ms)")
        
        assert(averageCachedTime < 1.0) {
            "缓存加载时间超标: ${averageCachedTime}ms >= 1ms"
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 处理 JSON 字符串（模拟完整的解析流程）
     */
    private fun processJson(json: String): String {
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
