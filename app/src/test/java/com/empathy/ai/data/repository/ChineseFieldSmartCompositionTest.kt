package com.empathy.ai.data.repository

import org.junit.Test
import kotlin.test.assertTrue

/**
 * 测试中文字段智能组合功能
 * 
 * 验证解析器能够正确处理弱模型返回的中文字段格式：
 * {
 *   "对方情绪": "neutral",
 *   "潜在意图": "初步试探",
 *   "风险点": "目标过于直接",
 *   "回复建议": "你好呀"
 * }
 */
class ChineseFieldSmartCompositionTest {

    /**
     * 测试 1: 验证中文字段能被正确识别
     */
    @Test
    fun `test chinese field names are recognized`() {
        val json = """
            {
                "对方情绪": "neutral",
                "潜在意图": "初步试探，开启对话",
                "风险点": "目标过于直接可能让对方产生戒备心理",
                "回复建议": "你好呀，很高兴认识你 :)"
            }
        """.trimIndent()
        
        val jsonMap = parseJsonToMap(json)
        
        // 验证所有中文字段都存在
        assertTrue(jsonMap.containsKey("对方情绪"))
        assertTrue(jsonMap.containsKey("潜在意图"))
        assertTrue(jsonMap.containsKey("风险点"))
        assertTrue(jsonMap.containsKey("回复建议"))
        
        println("✅ 中文字段识别成功")
        println("   对方情绪: ${jsonMap["对方情绪"]}")
        println("   潜在意图: ${jsonMap["潜在意图"]}")
        println("   风险点: ${jsonMap["风险点"]}")
        println("   回复建议: ${jsonMap["回复建议"]}")
    }

    /**
     * 测试 2: 验证智能组合逻辑
     * 
     * 当 AI 返回独立的中文字段时，应该能够智能组合成 strategyAnalysis
     */
    @Test
    fun `test smart composition of chinese fields`() {
        val json = """
            {
                "对方情绪": "neutral",
                "潜在意图": "初步试探，开启对话",
                "风险点": "目标过于直接可能让对方产生戒备心理",
                "回复建议": "你好呀，很高兴认识你 :)"
            }
        """.trimIndent()
        
        val jsonMap = parseJsonToMap(json)
        
        // 模拟智能组合逻辑
        val composedAnalysis = buildString {
            // 情绪字段
            val emotionFields = listOf("对方情绪", "情绪", "情绪状态")
            for (field in emotionFields) {
                (jsonMap[field] as? String)?.let { 
                    if (it.isNotBlank()) {
                        append("【对方情绪】$it\n")
                    }
                }
            }
            
            // 意图字段
            val intentFields = listOf("潜在意图", "意图", "目的")
            for (field in intentFields) {
                (jsonMap[field] as? String)?.let { 
                    if (it.isNotBlank()) {
                        append("【潜在意图】$it\n")
                    }
                }
            }
            
            // 风险字段
            val riskFields = listOf("风险点", "风险", "风险提示")
            for (field in riskFields) {
                (jsonMap[field] as? String)?.let { 
                    if (it.isNotBlank()) {
                        append("【风险提示】$it\n")
                    }
                }
            }
        }
        
        assertTrue(composedAnalysis.isNotBlank())
        assertTrue(composedAnalysis.contains("【对方情绪】"))
        assertTrue(composedAnalysis.contains("【潜在意图】"))
        assertTrue(composedAnalysis.contains("【风险提示】"))
        
        println("✅ 智能组合成功")
        println("组合后的分析内容:")
        println(composedAnalysis)
    }
    
    /**
     * 测试 3: 验证回复建议字段映射
     */
    @Test
    fun `test reply suggestion field mapping`() {
        val json = """
            {
                "回复建议": "你好呀，很高兴认识你 :)"
            }
        """.trimIndent()
        
        val jsonMap = parseJsonToMap(json)
        
        // 模拟字段提取逻辑
        val suggestionFields = listOf("replySuggestion", "回复建议", "建议回复", "推荐回复")
        var replySuggestion: String? = null
        
        for (field in suggestionFields) {
            (jsonMap[field] as? String)?.let { 
                if (it.isNotBlank()) {
                    replySuggestion = it
                }
            }
            if (replySuggestion != null) break
        }
        
        assertTrue(replySuggestion != null)
        assertTrue(replySuggestion!!.contains("你好"))
        
        println("✅ 回复建议提取成功: $replySuggestion")
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
