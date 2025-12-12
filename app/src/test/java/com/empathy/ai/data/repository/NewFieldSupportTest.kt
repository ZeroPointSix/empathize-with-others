package com.empathy.ai.data.repository

import org.junit.Test
import org.junit.Assert.*

/**
 * 测试新增的字段支持
 * 
 * 验证 extractReplySuggestion 和 extractStrategyAnalysis 方法
 * 对 reply、response、answer 和 analysis 嵌套对象的支持
 */
class NewFieldSupportTest {

    /**
     * 测试 reply 字段支持
     */
    @Test
    fun testReplyFieldSupport() {
        val jsonMap = mapOf(
            "analysis" to mapOf(
                "emotion" to "紧张但真诚",
                "intention" to "希望快速推进关系",
                "risk" to listOf("表白过早可能导致对方压力大", "缺乏前期情感铺垫")
            ),
            "strategy" to "先建立轻松互动氛围",
            "reply" to "哇，突然收到这句话有点小惊喜呢～"
        )
        
        val result = extractReplySuggestionFromMap(jsonMap)
        assertEquals("哇，突然收到这句话有点小惊喜呢～", result)
        println("✅ reply 字段支持测试通过")
    }

    /**
     * 测试 response 字段支持
     */
    @Test
    fun testResponseFieldSupport() {
        val jsonMap = mapOf(
            "response" to "这是一个回复"
        )
        
        val result = extractReplySuggestionFromMap(jsonMap)
        assertEquals("这是一个回复", result)
        println("✅ response 字段支持测试通过")
    }

    /**
     * 测试 answer 字段支持
     */
    @Test
    fun testAnswerFieldSupport() {
        val jsonMap = mapOf(
            "answer" to "这是一个答案"
        )
        
        val result = extractReplySuggestionFromMap(jsonMap)
        assertEquals("这是一个答案", result)
        println("✅ answer 字段支持测试通过")
    }

    /**
     * 测试 analysis 嵌套对象支持（emotion、intention、risk）
     * 
     * 注意：当只有 analysis 嵌套对象时（没有 strategy 字段），
     * 应该从 analysis 中提取信息
     */
    @Test
    fun testAnalysisNestedObjectSupport() {
        // 只有 analysis 嵌套对象，没有 strategy 字段
        val jsonMap = mapOf(
            "analysis" to mapOf(
                "emotion" to "紧张但真诚，直接表达好感",
                "intention" to "希望快速推进关系，确认对方心意",
                "risk" to listOf("表白过早可能导致对方压力大", "缺乏前期情感铺垫，容易被婉拒")
            ),
            "reply" to "哇，突然收到这句话有点小惊喜呢～"
        )
        
        val result = extractStrategyAnalysisFromMap(jsonMap)
        
        // 验证包含情绪信息
        assertTrue("应该包含情绪信息", result.contains("情绪") || result.contains("紧张但真诚"))
        // 验证包含意图信息
        assertTrue("应该包含意图信息", result.contains("意图") || result.contains("希望快速推进关系"))
        // 验证包含风险信息
        assertTrue("应该包含风险信息", result.contains("风险") || result.contains("表白过早"))
        
        println("✅ analysis 嵌套对象支持测试通过")
        println("提取的策略分析:\n$result")
    }

    /**
     * 测试 strategy 字段优先级
     */
    @Test
    fun testStrategyFieldPriority() {
        val jsonMap = mapOf(
            "strategy" to "先建立轻松互动氛围",
            "analysis" to mapOf(
                "emotion" to "紧张"
            )
        )
        
        val result = extractStrategyAnalysisFromMap(jsonMap)
        assertEquals("先建立轻松互动氛围", result)
        println("✅ strategy 字段优先级测试通过")
    }

    // ========== 辅助方法 ==========
    // 模拟 AiRepositoryImpl 中的私有方法

    @Suppress("UNCHECKED_CAST")
    private fun extractReplySuggestionFromMap(jsonMap: Map<String, Any>): String {
        // 1. 标准字段名
        (jsonMap["replySuggestion"] as? String)?.let { 
            if (it.isNotBlank()) return it 
        }
        
        // 2. reply 字段
        (jsonMap["reply"] as? String)?.let { 
            if (it.isNotBlank()) return it 
        }
        
        // 3. response 字段
        (jsonMap["response"] as? String)?.let { 
            if (it.isNotBlank()) return it 
        }
        
        // 4. answer 字段
        (jsonMap["answer"] as? String)?.let { 
            if (it.isNotBlank()) return it 
        }
        
        // 5. recommended_response 字段
        (jsonMap["recommended_response"] as? String)?.let { 
            if (it.isNotBlank()) return it 
        }
        
        // 6. 中文字段名
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
            
            // 2. strategy 字段
            val strategy = jsonMap["strategy"] as? String
            if (!strategy.isNullOrBlank()) {
                append(strategy)
                return@buildString
            }
            
            // 3. 中文字段名
            val chineseFieldNames = listOf(
                "策略分析",
                "心理分析",
                "军师分析"
            )
            for (fieldName in chineseFieldNames) {
                (jsonMap[fieldName] as? String)?.let { 
                    if (it.isNotBlank()) {
                        append(it)
                        return@buildString
                    }
                }
            }
            
            // 4. analysis 嵌套对象
            val analysisObj = jsonMap["analysis"] as? Map<String, Any>
            if (analysisObj != null) {
                val emotionalState = analysisObj["emotional_state"] as? String
                    ?: analysisObj["emotion"] as? String
                    ?: analysisObj["情绪"] as? String
                val potentialIntent = analysisObj["potential_intent"] as? String
                    ?: analysisObj["intention"] as? String
                    ?: analysisObj["意图"] as? String
                
                val riskArray = analysisObj["risk"] as? List<*>
                    ?: analysisObj["risks"] as? List<*>
                    ?: analysisObj["风险"] as? List<*>
                
                var hasAnalysisContent = false
                
                if (!emotionalState.isNullOrBlank() || !potentialIntent.isNullOrBlank() || !riskArray.isNullOrEmpty()) {
                    append("【对方状态】\n")
                    hasAnalysisContent = true
                    
                    if (!emotionalState.isNullOrBlank()) {
                        append("情绪: $emotionalState\n")
                    }
                    if (!potentialIntent.isNullOrBlank()) {
                        append("意图: $potentialIntent\n")
                    }
                    
                    if (!riskArray.isNullOrEmpty()) {
                        append("\n【风险提示】\n")
                        riskArray.forEachIndexed { index, risk ->
                            (risk as? String)?.let { 
                                if (it.isNotBlank()) {
                                    append("${index + 1}. $it\n")
                                }
                            }
                        }
                    }
                    
                    append("\n")
                }
                
                if (hasAnalysisContent) {
                    return@buildString
                }
            }
            
            if (isEmpty()) {
                append("默认分析")
            }
        }
    }
}
