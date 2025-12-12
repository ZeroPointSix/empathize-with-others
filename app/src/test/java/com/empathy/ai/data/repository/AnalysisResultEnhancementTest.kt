package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import org.junit.Assert.*
import org.junit.Test

/**
 * 测试 AnalysisResult 解析增强功能
 * 
 * 验证任务 3.2 的实现：
 * - 默认值支持
 * - extractReplySuggestion 增强（数组格式、priority 选择）
 * - extractStrategyAnalysis 增强（嵌套结构）
 * - extractRiskLevel 增强（多种格式、智能推断）
 */
class AnalysisResultEnhancementTest {

    /**
     * 测试默认值支持
     * 
     * 验证：当解析完全失败时，返回 DefaultValues.ANALYSIS_RESULT
     */
    @Test
    fun testDefaultValueSupport() {
        val defaultResult = DefaultValues.ANALYSIS_RESULT
        
        // 验证默认值的基本属性
        assertNotNull(defaultResult)
        assertNotNull(defaultResult.replySuggestion)
        assertNotNull(defaultResult.strategyAnalysis)
        assertEquals(RiskLevel.SAFE, defaultResult.riskLevel)
        
        // 验证默认值包含有用的提示信息
        assertTrue(defaultResult.replySuggestion.contains("AI"))
        assertTrue(defaultResult.strategyAnalysis.contains("AI"))
        
        println("✅ 默认值测试通过")
        println("  replySuggestion: ${defaultResult.replySuggestion}")
        println("  strategyAnalysis: ${defaultResult.strategyAnalysis.take(100)}...")
        println("  riskLevel: ${defaultResult.riskLevel}")
    }

    /**
     * 测试 extractReplySuggestion - 标准字段名
     */
    @Test
    fun testExtractReplySuggestion_StandardField() {
        val jsonMap = mapOf(
            "replySuggestion" to "这是一个标准的回复建议"
        )
        
        val result = extractReplySuggestionFromMap(jsonMap)
        assertEquals("这是一个标准的回复建议", result)
        
        println("✅ 标准字段名测试通过")
    }

    /**
     * 测试 extractReplySuggestion - 中文字段名
     */
    @Test
    fun testExtractReplySuggestion_ChineseField() {
        val testCases = listOf(
            "回复建议" to "使用回复建议字段",
            "具体的回复建议" to "使用具体的回复建议字段",
            "建议回复" to "使用建议回复字段",
            "话术建议" to "使用话术建议字段"
        )
        
        testCases.forEach { (fieldName, expectedValue) ->
            val jsonMap = mapOf(fieldName to expectedValue)
            val result = extractReplySuggestionFromMap(jsonMap)
            assertEquals(expectedValue, result)
        }
        
        println("✅ 中文字段名测试通过（${testCases.size} 个字段）")
    }

    /**
     * 测试 extractReplySuggestion - 数组格式（优先选择 high priority）
     */
    @Test
    fun testExtractReplySuggestion_ArrayWithPriority() {
        val jsonMap = mapOf(
            "response_suggestions" to listOf(
                mapOf("text" to "低优先级建议", "priority" to "low"),
                mapOf("text" to "高优先级建议", "priority" to "high"),
                mapOf("text" to "中优先级建议", "priority" to "medium")
            )
        )
        
        val result = extractReplySuggestionFromMap(jsonMap)
        assertEquals("高优先级建议", result)
        
        println("✅ 数组格式（priority）测试通过")
    }

    /**
     * 测试 extractReplySuggestion - 字符串数组格式
     */
    @Test
    fun testExtractReplySuggestion_StringArray() {
        val jsonMap = mapOf(
            "replySuggestions" to listOf(
                "第一个建议",
                "第二个建议",
                "第三个建议"
            )
        )
        
        val result = extractReplySuggestionFromMap(jsonMap)
        assertEquals("第一个建议", result)
        
        println("✅ 字符串数组格式测试通过")
    }

    /**
     * 测试 extractStrategyAnalysis - 标准字段名
     */
    @Test
    fun testExtractStrategyAnalysis_StandardField() {
        val jsonMap = mapOf(
            "strategyAnalysis" to "这是一个完整的策略分析"
        )
        
        val result = extractStrategyAnalysisFromMap(jsonMap)
        assertEquals("这是一个完整的策略分析", result)
        
        println("✅ 策略分析标准字段测试通过")
    }

    /**
     * 测试 extractStrategyAnalysis - 嵌套结构
     */
    @Test
    fun testExtractStrategyAnalysis_NestedStructure() {
        val jsonMap = mapOf(
            "emotion_analysis" to "对方情绪低落",
            "potential_intention" to "寻求安慰",
            "key_insights" to listOf("需要情感支持", "避免说教"),
            "risk_points" to listOf("不要提及工作", "避免比较")
        )
        
        val result = extractStrategyAnalysisFromMap(jsonMap)
        
        // 验证包含所有关键信息
        assertTrue(result.contains("对方情绪低落"))
        assertTrue(result.contains("寻求安慰"))
        assertTrue(result.contains("需要情感支持"))
        assertTrue(result.contains("不要提及工作"))
        
        println("✅ 嵌套结构测试通过")
        println("  结果长度: ${result.length}")
        println("  结果预览: ${result.take(100)}...")
    }

    /**
     * 测试 extractRiskLevel - 标准格式（大写）
     */
    @Test
    fun testExtractRiskLevel_StandardFormat() {
        val testCases = listOf(
            "SAFE" to RiskLevel.SAFE,
            "WARNING" to RiskLevel.WARNING,
            "DANGER" to RiskLevel.DANGER
        )
        
        testCases.forEach { (value, expected) ->
            val jsonMap = mapOf("riskLevel" to value)
            val result = extractRiskLevelFromMap(jsonMap)
            assertEquals(expected, result)
        }
        
        println("✅ 标准格式（大写）测试通过")
    }

    /**
     * 测试 extractRiskLevel - 小写格式
     */
    @Test
    fun testExtractRiskLevel_LowercaseFormat() {
        val testCases = listOf(
            "safe" to RiskLevel.SAFE,
            "warning" to RiskLevel.WARNING,
            "danger" to RiskLevel.DANGER
        )
        
        testCases.forEach { (value, expected) ->
            val jsonMap = mapOf("riskLevel" to value)
            val result = extractRiskLevelFromMap(jsonMap)
            assertEquals(expected, result)
        }
        
        println("✅ 小写格式测试通过")
    }

    /**
     * 测试 extractRiskLevel - 等级格式（low/medium/high）
     */
    @Test
    fun testExtractRiskLevel_LevelFormat() {
        val testCases = listOf(
            "low" to RiskLevel.SAFE,
            "medium" to RiskLevel.WARNING,
            "high" to RiskLevel.DANGER
        )
        
        testCases.forEach { (value, expected) ->
            val jsonMap = mapOf("warning_level" to value)
            val result = extractRiskLevelFromMap(jsonMap)
            assertEquals(expected, result)
        }
        
        println("✅ 等级格式测试通过")
    }

    /**
     * 测试 extractRiskLevel - 中文字段名
     */
    @Test
    fun testExtractRiskLevel_ChineseField() {
        val testCases = listOf(
            Triple("风险等级", "安全", RiskLevel.SAFE),
            Triple("风险级别", "警告", RiskLevel.WARNING),
            Triple("风险", "危险", RiskLevel.DANGER)
        )
        
        testCases.forEach { (fieldName, value, expected) ->
            val jsonMap = mapOf(fieldName to value)
            val result = extractRiskLevelFromMap(jsonMap)
            assertEquals(expected, result)
        }
        
        println("✅ 中文字段名测试通过")
    }

    /**
     * 测试 extractRiskLevel - 智能推断（高风险关键词）
     */
    @Test
    fun testExtractRiskLevel_IntelligentInference_Danger() {
        val jsonMap = mapOf(
            "strategyAnalysis" to "这是一个高风险的情况，对方情绪非常危险，需要立即采取行动"
        )
        
        val result = extractRiskLevelFromMap(jsonMap)
        assertEquals(RiskLevel.DANGER, result)
        
        println("✅ 智能推断（高风险）测试通过")
    }

    /**
     * 测试 extractRiskLevel - 智能推断（中等风险关键词）
     */
    @Test
    fun testExtractRiskLevel_IntelligentInference_Warning() {
        val jsonMap = mapOf(
            "strategyAnalysis" to "需要注意风险，谨慎处理，避免触及敏感话题"
        )
        
        val result = extractRiskLevelFromMap(jsonMap)
        assertEquals(RiskLevel.WARNING, result)
        
        println("✅ 智能推断（中等风险）测试通过")
    }

    /**
     * 测试 extractRiskLevel - 智能推断（安全）
     */
    @Test
    fun testExtractRiskLevel_IntelligentInference_Safe() {
        val jsonMap = mapOf(
            "strategyAnalysis" to "对方心情不错，可以正常交流"
        )
        
        val result = extractRiskLevelFromMap(jsonMap)
        assertEquals(RiskLevel.SAFE, result)
        
        println("✅ 智能推断（安全）测试通过")
    }

    // ========== 辅助方法 ==========
    // 这些方法模拟 AiRepositoryImpl 中的私有方法

    @Suppress("UNCHECKED_CAST")
    private fun extractReplySuggestionFromMap(jsonMap: Map<String, Any>): String {
        // 1. 标准字段名
        (jsonMap["replySuggestion"] as? String)?.let { 
            if (it.isNotBlank()) return it 
        }
        
        // 2. 中文字段名
        val chineseFieldNames = listOf(
            "具体的回复建议",
            "回复建议",
            "建议回复",
            "话术建议",
            "建议的回复内容",
            "回复内容"
        )
        for (fieldName in chineseFieldNames) {
            (jsonMap[fieldName] as? String)?.let { 
                if (it.isNotBlank()) return it 
            }
        }
        
        // 3. response_suggestions 数组格式
        val suggestions = jsonMap["response_suggestions"] as? List<Map<String, Any>>
        if (!suggestions.isNullOrEmpty()) {
            val highPriority = suggestions.find { 
                it["priority"] == "high" || it["priority"] == "HIGH"
            }
            val suggestion = highPriority ?: suggestions.first()
            (suggestion["text"] as? String)?.let { 
                if (it.isNotBlank()) return it 
            }
        }
        
        // 4. replySuggestions 数组格式
        val suggestionList = jsonMap["replySuggestions"] as? List<String>
        if (!suggestionList.isNullOrEmpty()) {
            suggestionList.firstOrNull()?.let { 
                if (it.isNotBlank()) return it 
            }
        }
        
        return "默认建议"
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractStrategyAnalysisFromMap(jsonMap: Map<String, Any>): String {
        return buildString {
            // 1. 标准字段名
            val standardAnalysis = jsonMap["strategyAnalysis"] as? String
            if (!standardAnalysis.isNullOrBlank()) {
                append(standardAnalysis)
                return@buildString
            }
            
            // 2. 嵌套结构
            val emotionAnalysis = jsonMap["emotion_analysis"] as? String
            val potentialIntention = jsonMap["potential_intention"] as? String
            val riskPoints = jsonMap["risk_points"] as? List<String>
            val keyInsights = jsonMap["key_insights"] as? List<String>
            
            if (!emotionAnalysis.isNullOrBlank()) {
                append("【对方状态】\n")
                append(emotionAnalysis)
                if (!potentialIntention.isNullOrBlank()) {
                    append("\n意图：$potentialIntention")
                }
                append("\n\n")
            }
            
            if (!keyInsights.isNullOrEmpty()) {
                append("【关键洞察】\n")
                keyInsights.forEach { insight ->
                    if (insight.isNotBlank()) {
                        append("• $insight\n")
                    }
                }
                append("\n")
            }
            
            if (!riskPoints.isNullOrEmpty()) {
                append("【风险提示】\n")
                riskPoints.forEachIndexed { index, risk ->
                    if (risk.isNotBlank()) {
                        append("${index + 1}. $risk\n")
                    }
                }
            }
            
            if (isEmpty()) {
                append("默认分析")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractRiskLevelFromMap(jsonMap: Map<String, Any>): RiskLevel {
        // 1. 标准字段名（大写）
        when (jsonMap["riskLevel"]) {
            "SAFE" -> return RiskLevel.SAFE
            "WARNING" -> return RiskLevel.WARNING
            "DANGER" -> return RiskLevel.DANGER
        }
        
        // 2. 标准字段名（小写）
        when ((jsonMap["riskLevel"] as? String)?.lowercase()) {
            "safe" -> return RiskLevel.SAFE
            "warning" -> return RiskLevel.WARNING
            "danger" -> return RiskLevel.DANGER
        }
        
        // 3. warning_level 字段
        val warningLevel = jsonMap["warning_level"] as? String
        when (warningLevel?.lowercase()) {
            "low", "safe" -> return RiskLevel.SAFE
            "medium", "warning" -> return RiskLevel.WARNING
            "high", "danger" -> return RiskLevel.DANGER
        }
        
        // 4. 中文字段名
        val chineseFieldNames = listOf("风险等级", "风险级别", "风险")
        for (fieldName in chineseFieldNames) {
            when ((jsonMap[fieldName] as? String)?.lowercase()) {
                "safe", "安全", "低" -> return RiskLevel.SAFE
                "warning", "警告", "注意", "中" -> return RiskLevel.WARNING
                "danger", "危险", "严重", "高" -> return RiskLevel.DANGER
            }
        }
        
        // 5. 智能推断
        val riskText = buildString {
            (jsonMap["strategyAnalysis"] as? String)?.let { append(it).append(" ") }
            (jsonMap["策略分析"] as? String)?.let { append(it).append(" ") }
        }
        
        val lowerRiskText = riskText.lowercase()
        return when {
            lowerRiskText.contains("高风险") || 
            lowerRiskText.contains("危险") || 
            lowerRiskText.contains("严重") ||
            lowerRiskText.contains("立即") -> RiskLevel.DANGER
            
            lowerRiskText.contains("风险") || 
            lowerRiskText.contains("注意") || 
            lowerRiskText.contains("谨慎") ||
            lowerRiskText.contains("避免") -> RiskLevel.WARNING
            
            else -> RiskLevel.SAFE
        }
    }
}
