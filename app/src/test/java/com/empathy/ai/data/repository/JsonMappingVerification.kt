package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AnalysisResult
import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.Assert.*

/**
 * 简单的JSON字段映射验证测试
 * 不依赖复杂的测试框架，直接验证修复效果
 */
class JsonMappingVerification {

    private val moshi = Moshi.Builder().build()

    /**
     * 模拟AiRepositoryImpl中的字段映射逻辑
     */
    private fun mapChineseFieldNames(json: String): String {
        try {
            // 定义中英文字段名映射关系
            val fieldMappings = mapOf(
                "对方当前情绪和潜在意图" to "strategyAnalysis",
                "对方当前的情绪和潜在意图" to "strategyAnalysis",
                "情绪和潜在意图" to "strategyAnalysis",
                "关键洞察" to "strategyAnalysis",
                "心理分析" to "strategyAnalysis",
                "策略分析" to "strategyAnalysis",
                "策略建议" to "strategyAnalysis",
                "军师分析" to "strategyAnalysis",
                
                "可能存在的风险点" to "riskLevel",
                "风险等级" to "riskLevel",
                "风险级别" to "riskLevel",
                "风险" to "riskLevel",
                
                "具体的回复建议" to "replySuggestion",
                "回复建议" to "replySuggestion",
                "建议回复" to "replySuggestion",
                "回复内容" to "replySuggestion",
                "建议的回复内容" to "replySuggestion",
                "话术建议" to "replySuggestion"
            )
            
            var result = json
            var mappingCount = 0
            
            // 应用字段名映射
            fieldMappings.forEach { (chineseName, englishName) ->
                if (result.contains("\"$chineseName\"")) {
                    result = result.replace("\"$chineseName\"", "\"$englishName\"")
                    mappingCount++
                    println("映射字段名: $chineseName -> $englishName")
                }
            }
            
            // 特殊处理：如果检测到中文字段但没有找到对应映射，尝试智能映射
            if (result.contains(Regex("\"[\u4e00-\u9fff]+\""))) {
                println("检测到未映射的中文字段名，尝试智能映射")
                result = intelligentFieldMapping(result)
            }
            
            println("字段名映射完成，共映射 $mappingCount 个字段")
            return result
            
        } catch (e: Exception) {
            println("字段名映射失败: ${e.message}")
            return json // 返回原始字符串作为降级方案
        }
    }
    
    /**
     * 智能字段名映射
     */
    private fun intelligentFieldMapping(json: String): String {
        try {
            var result = json
            
            // 检测包含"建议"、"回复"等关键词的字段，映射为replySuggestion
            result = result.replace(Regex("\"([^\"]*建议[^\"]*)\"(?=\\s*:)")) { matchResult ->
                val fieldName = matchResult.groupValues[1]
                println("智能映射: $fieldName -> replySuggestion")
                "\"replySuggestion\""
            }
            
            // 检测包含"风险"、"危险"等关键词的字段，映射为riskLevel
            result = result.replace(Regex("\"([^\"]*风险[^\"]*)\"(?=\\s*:)")) { matchResult ->
                val fieldName = matchResult.groupValues[1]
                println("智能映射: $fieldName -> riskLevel")
                "\"riskLevel\""
            }
            
            // 检测包含"分析"、"心理"、"策略"等关键词的字段，映射为strategyAnalysis
            result = result.replace(Regex("\"([^\"]*分析[^\"]*)\"(?=\\s*:)")) { matchResult ->
                val fieldName = matchResult.groupValues[1]
                println("智能映射: $fieldName -> strategyAnalysis")
                "\"strategyAnalysis\""
            }
            
            return result
            
        } catch (e: Exception) {
            println("智能字段名映射失败: ${e.message}")
            return json
        }
    }

    @Test
    fun testChineseFieldNameMapping() {
        println("=== 测试中文字段名映射功能 ===")
        
        // 原始错误JSON（包含中文字段名）
        val originalJson = """{
  "对方当前的情绪和潜在意图": "由于聊天记录内容不明确，无法准确分析对方情绪状态。建议保持中立态度，避免过早下结论。",
  "可能存在的风险点": "高风险：聊天记录内容不清晰，可能导致误解。建议谨慎沟通，避免敏感话题。",
  "具体的回复建议": "由于信息不足且风险高，建议保持中立态度，表达理解和关心，避免过早下结论。"
}"""
        
        println("原始JSON:")
        println(originalJson)
        
        // 应用字段名映射
        val mappedJson = mapChineseFieldNames(originalJson)
        
        println("\n映射后JSON:")
        println(mappedJson)
        
        // 验证映射结果
        assertTrue("应包含strategyAnalysis字段", mappedJson.contains("\"strategyAnalysis\""))
        assertTrue("应包含riskLevel字段", mappedJson.contains("\"riskLevel\""))
        assertTrue("应包含replySuggestion字段", mappedJson.contains("\"replySuggestion\""))
        
        assertFalse("不应包含原始中文字段名", mappedJson.contains("对方当前的情绪和潜在意图"))
        assertFalse("不应包含原始中文字段名", mappedJson.contains("可能存在的风险点"))
        assertFalse("不应包含原始中文字段名", mappedJson.contains("具体的回复建议"))
        
        println("\n✅ 中文字段名映射测试通过")
    }

    @Test
    fun testJsonParsingAfterMapping() {
        println("\n=== 测试映射后的JSON解析 ===")
        
        // 原始错误JSON
        val originalJson = """{
  "对方当前的情绪和潜在意图": "由于聊天记录内容不明确，无法准确分析对方情绪状态。",
  "可能存在的风险点": "高风险：聊天记录内容不清晰，可能导致误解。",
  "具体的回复建议": "由于信息不足且风险高，建议保持中立态度。"
}"""
        
        // 应用字段名映射
        val mappedJson = mapChineseFieldNames(originalJson)
        
        println("映射后JSON:")
        println(mappedJson)
        
        try {
            // 尝试解析映射后的JSON
            val adapter = moshi.adapter(AnalysisResult::class.java)
            val result = adapter.fromJson(mappedJson)
            
            assertNotNull("解析结果不应为null", result)
            assertEquals("strategyAnalysis应正确解析", "由于聊天记录内容不明确，无法准确分析对方情绪状态。", result?.strategyAnalysis)
            assertEquals("riskLevel应正确解析", "高风险：聊天记录内容不清晰，可能导致误解。", result?.riskLevel)
            assertEquals("replySuggestion应正确解析", "由于信息不足且风险高，建议保持中立态度。", result?.replySuggestion)
            
            println("\n✅ 映射后JSON解析测试通过")
            println("解析结果:")
            println("- strategyAnalysis: ${result?.strategyAnalysis}")
            println("- riskLevel: ${result?.riskLevel}")
            println("- replySuggestion: ${result?.replySuggestion}")
            
        } catch (e: Exception) {
            fail("映射后JSON解析失败: ${e.message}")
        }
    }

    @Test
    fun testMixedFieldNames() {
        println("\n=== 测试混合字段名处理 ===")
        
        // 混合中英文字段名的JSON
        val mixedJson = """{
  "strategyAnalysis": "英文策略分析内容",
  "风险等级": "中文风险等级内容",
  "replySuggestion": "英文回复建议内容",
  "心理分析": "中文心理分析内容"
}"""
        
        println("混合字段名JSON:")
        println(mixedJson)
        
        // 应用字段名映射
        val mappedJson = mapChineseFieldNames(mixedJson)
        
        println("\n映射后JSON:")
        println(mappedJson)
        
        // 验证映射结果
        assertTrue("应保留原始英文字段名", mappedJson.contains("\"strategyAnalysis\""))
        assertTrue("应保留原始英文字段名", mappedJson.contains("\"replySuggestion\""))
        assertTrue("应映射中文字段名", mappedJson.contains("\"riskLevel\""))
        assertTrue("应映射中文字段名", mappedJson.contains("\"strategyAnalysis\""))
        
        assertFalse("不应包含原始中文字段名", mappedJson.contains("风险等级"))
        assertFalse("不应包含原始中文字段名", mappedJson.contains("心理分析"))
        
        println("\n✅ 混合字段名处理测试通过")
    }

    @Test
    fun testIntelligentMapping() {
        println("\n=== 测试智能字段名映射 ===")
        
        // 包含未预见中文字段名的JSON
        val unknownJson = """{
  "情感状态分析": "情感状态分析内容",
  "潜在风险评估": "潜在风险评估内容",
  "沟通话术建议": "沟通话术建议内容"
}"""
        
        println("未知中文字段名JSON:")
        println(unknownJson)
        
        // 应用字段名映射
        val mappedJson = mapChineseFieldNames(unknownJson)
        
        println("\n智能映射后JSON:")
        println(mappedJson)
        
        // 验证智能映射结果
        assertTrue("应智能映射分析字段", mappedJson.contains("\"strategyAnalysis\""))
        assertTrue("应智能映射风险字段", mappedJson.contains("\"riskLevel\""))
        assertTrue("应智能映射建议字段", mappedJson.contains("\"replySuggestion\""))
        
        println("\n✅ 智能字段名映射测试通过")
    }
}