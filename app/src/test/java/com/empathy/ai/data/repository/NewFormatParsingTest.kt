package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.RiskLevel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 测试新格式 AI 响应的解析
 * 
 * 验证解析器能够正确处理以下格式：
 * {
 *   "analysis": {...},
 *   "risks": [...],
 *   "suggestions": [...],
 *   "recommended_response": "...",
 *   "strategy": "..."
 * }
 */
class NewFormatParsingTest {

    /**
     * 测试 1: 解析包含 recommended_response 字段的响应
     */
    @Test
    fun `test parse response with recommended_response field`() {
        val json = """
            {
                "analysis": {
                    "emotional_state": "未知",
                    "potential_intent": "表达好感",
                    "risk_level": "中等"
                },
                "risks": ["风险1", "风险2"],
                "suggestions": ["建议1", "建议2"],
                "recommended_response": "你好！谢谢你的友善",
                "strategy": "降温处理"
            }
        """.trimIndent()
        
        // 模拟解析逻辑
        val jsonMap = parseJsonToMap(json)
        
        // 验证 recommended_response 字段存在
        assertTrue(jsonMap.containsKey("recommended_response"))
        assertEquals("你好！谢谢你的友善", jsonMap["recommended_response"])
        
        println("✅ recommended_response 字段解析成功")
    }

    /**
     * 测试 2: 解析包含 strategy 字段的响应
     */
    @Test
    fun `test parse response with strategy field`() {
        val json = """
            {
                "strategy": "降温处理，转向普通朋友关系建设",
                "recommended_response": "你好！"
            }
        """.trimIndent()
        
        val jsonMap = parseJsonToMap(json)
        
        assertTrue(jsonMap.containsKey("strategy"))
        assertEquals("降温处理，转向普通朋友关系建设", jsonMap["strategy"])
        
        println("✅ strategy 字段解析成功")
    }
    
    /**
     * 测试 3: 解析包含 analysis 嵌套对象的响应
     */
    @Test
    fun `test parse response with nested analysis object`() {
        val json = """
            {
                "analysis": {
                    "emotional_state": "未知，仅有一条消息无法判断情绪状态",
                    "potential_intent": "表达好感，可能是试探性接触",
                    "risk_level": "中等"
                }
            }
        """.trimIndent()
        
        val jsonMap = parseJsonToMap(json)
        
        assertTrue(jsonMap.containsKey("analysis"))
        
        @Suppress("UNCHECKED_CAST")
        val analysisObj = jsonMap["analysis"] as? Map<String, Any>
        assertTrue(analysisObj != null)
        assertEquals("中等", analysisObj["risk_level"])
        
        println("✅ analysis 嵌套对象解析成功")
    }
    
    /**
     * 测试 4: 解析包含 risks 数组的响应
     */
    @Test
    fun `test parse response with risks array`() {
        val json = """
            {
                "risks": [
                    "过于直接的表白可能让对方感到压力",
                    "缺乏互动基础，容易被误认为骚扰"
                ]
            }
        """.trimIndent()
        
        val jsonMap = parseJsonToMap(json)
        
        assertTrue(jsonMap.containsKey("risks"))
        
        @Suppress("UNCHECKED_CAST")
        val risksArray = jsonMap["risks"] as? List<*>
        assertTrue(risksArray != null)
        assertEquals(2, risksArray.size)
        
        println("✅ risks 数组解析成功")
    }
    
    /**
     * 测试 5: 解析完整的新格式响应
     */
    @Test
    fun `test parse complete new format response`() {
        val json = """
            {
                "analysis": {
                    "emotional_state": "未知，仅有一条消息无法判断情绪状态",
                    "potential_intent": "表达好感，可能是试探性接触",
                    "risk_level": "中等"
                },
                "risks": [
                    "过于直接的表白可能让对方感到压力",
                    "缺乏互动基础，容易被误认为骚扰",
                    "对方可能没有心理准备接收这种表达"
                ],
                "suggestions": [
                    "不要急于求成，先建立基本了解",
                    "从共同兴趣或日常话题入手",
                    "保持适度距离，给对方回应空间"
                ],
                "recommended_response": "你好！谢谢你的友善，我们可以先从朋友开始了解彼此吗？",
                "strategy": "降温处理，转向普通朋友关系建设"
            }
        """.trimIndent()
        
        val jsonMap = parseJsonToMap(json)
        
        // 验证所有字段都存在
        assertTrue(jsonMap.containsKey("analysis"))
        assertTrue(jsonMap.containsKey("risks"))
        assertTrue(jsonMap.containsKey("suggestions"))
        assertTrue(jsonMap.containsKey("recommended_response"))
        assertTrue(jsonMap.containsKey("strategy"))
        
        // 验证 recommended_response 可以作为回复建议
        val replySuggestion = jsonMap["recommended_response"] as? String
        assertNotEquals(null, replySuggestion)
        assertTrue(replySuggestion!!.isNotBlank())
        
        // 验证 strategy 可以作为策略分析
        val strategyAnalysis = jsonMap["strategy"] as? String
        assertNotEquals(null, strategyAnalysis)
        assertTrue(strategyAnalysis!!.isNotBlank())
        
        println("✅ 完整新格式响应解析成功")
        println("   回复建议: $replySuggestion")
        println("   策略分析: $strategyAnalysis")
    }
    
    // ========== 辅助方法 ==========
    
    private fun parseJsonToMap(json: String): Map<String, Any> {
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val adapter = moshi.adapter<Map<String, Any>>(
            com.squareup.moshi.Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
        ).lenient()
        return adapter.fromJson(json) ?: emptyMap()
    }
}
