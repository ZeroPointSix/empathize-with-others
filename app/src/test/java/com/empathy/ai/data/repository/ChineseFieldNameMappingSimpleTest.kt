package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.reflect.Method

/**
 * 简化的中文字段名映射测试
 * 
 * 专门测试JSON字段名映射功能，避免复杂的依赖问题
 */
@RunWith(JUnit4::class)
class ChineseFieldNameMappingSimpleTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `should map Chinese field names to English in raw JSON`() {
        // 测试用例：AI返回中文字段名的JSON
        val chineseJson = """
        {
            "对方当前的情绪和潜在意图": "由于聊天记录内容不明确（'大撒大撒'可能为无意义或模糊信息），无法准确判断情绪和意图。可能对方在测试、随意聊天或表达困惑，但缺乏足够上下文。",
            "可能存在的风险点": "高风险：聊天记录内容不清晰，可能涉及敏感话题或误解。雷区警告和策略建议均为'懂得都懂'，暗示存在未明确说明的敏感区域，需极度谨慎以避免触碰。",
            "具体的回复建议": "由于信息不足且风险高，建议保持中立、安全的回复，避免深入讨论。例如：'哈哈，这个话题有点抽象，我们聊点别的吧？' 或 '我不太明白，能具体说说吗？' 以试探对方意图，同时不触碰潜在雷区。"
        }
        """.trimIndent()

        // 通过反射访问私有方法进行测试
        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, chineseJson) as String
        
        // 验证字段名已被映射
        assertTrue("应该包含英文字段名 replySuggestion", processedJson.contains("\"replySuggestion\""))
        assertTrue("应该包含英文字段名 strategyAnalysis", processedJson.contains("\"strategyAnalysis\""))
        assertTrue("应该包含英文字段名 riskLevel", processedJson.contains("\"riskLevel\""))
        
        // 验证不再包含中文字段名
        assertFalse("不应该包含中文字段名 '具体的回复建议'", processedJson.contains("\"具体的回复建议\""))
        assertFalse("不应该包含中文字段名 '对方当前的情绪和潜在意图'", processedJson.contains("\"对方当前的情绪和潜在意图\""))
        assertFalse("不应该包含中文字段名 '可能存在的风险点'", processedJson.contains("\"可能存在的风险点\""))
        
        // 尝试解析为AnalysisResult对象
        val adapter = moshi.adapter(AnalysisResult::class.java)
        val result = adapter.lenient().fromJson(processedJson)
        
        assertNotNull("应该能够成功解析AnalysisResult", result)
        assertNotNull("回复建议不应该为null", result?.replySuggestion)
        assertNotNull("策略分析不应该为null", result?.strategyAnalysis)
        assertNotNull("风险等级不应该为null", result?.riskLevel)
        
        // 验证内容正确性
        assertTrue("回复建议应该包含'哈哈，这个话题有点抽象'", 
            result?.replySuggestion?.contains("哈哈，这个话题有点抽象") == true)
        assertTrue("策略分析应该包含'大撒大撒'", 
            result?.strategyAnalysis?.contains("大撒大撒") == true)
    }

    @Test
    fun `should handle mixed Chinese and English field names`() {
        // 测试混合中英文字段名的情况
        val mixedJson = """
        {
            "replySuggestion": "建议的回复内容",
            "对方当前的情绪和潜在意图": "心理分析内容",
            "riskLevel": "SAFE"
        }
        """.trimIndent()

        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, mixedJson) as String
        
        // 验证英文字段名保持不变
        assertTrue("应该保持英文字段名 replySuggestion", processedJson.contains("\"replySuggestion\""))
        assertTrue("应该保持英文字段名 riskLevel", processedJson.contains("\"riskLevel\""))
        
        // 验证中文字段名被映射
        assertTrue("应该映射中文字段名为 strategyAnalysis", processedJson.contains("\"strategyAnalysis\""))
        assertFalse("不应该包含中文字段名", processedJson.contains("\"对方当前的情绪和潜在意图\""))
    }

    @Test
    fun `should handle various Chinese field name variations`() {
        // 测试各种中文字段名变体
        val variationsJson = """
        {
            "回复建议": "建议回复内容",
            "心理分析": "分析内容",
            "风险等级": "WARNING"
        }
        """.trimIndent()

        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, variationsJson) as String
        
        // 验证各种变体都被正确映射
        assertTrue("应该映射'回复建议'为 replySuggestion", processedJson.contains("\"replySuggestion\""))
        assertTrue("应该映射'心理分析'为 strategyAnalysis", processedJson.contains("\"strategyAnalysis\""))
        assertTrue("应该映射'风险等级'为 riskLevel", processedJson.contains("\"riskLevel\""))
    }

    @Test
    fun `should handle original error scenario`() {
        // 模拟原始错误场景：包含中文字段名的JSON响应
        val originalErrorJson = """
        {
            "对方当前的情绪和潜在意图": "由于聊天记录内容不明确，无法准确判断情绪和意图。",
            "可能存在的风险点": "高风险：聊天记录内容不清晰，可能涉及敏感话题。",
            "具体的回复建议": "由于信息不足且风险高，建议保持中立、安全的回复。"
        }
        """.trimIndent()

        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, originalErrorJson) as String
        
        // 验证中文字段名被正确映射
        assertTrue("应该映射'对方当前的情绪和潜在意图'为 strategyAnalysis", 
            processedJson.contains("\"strategyAnalysis\""))
        assertTrue("应该映射'可能存在的风险点'为 riskLevel", 
            processedJson.contains("\"riskLevel\""))
        assertTrue("应该映射'具体的回复建议'为 replySuggestion", 
            processedJson.contains("\"replySuggestion\""))
        
        // 验证不再包含原始中文字段名
        assertFalse("不应该包含原始中文字段名'对方当前的情绪和潜在意图'", 
            processedJson.contains("\"对方当前的情绪和潜在意图\""))
        assertFalse("不应该包含原始中文字段名'可能存在的风险点'", 
            processedJson.contains("\"可能存在的风险点\""))
        assertFalse("不应该包含原始中文字段名'具体的回复建议'", 
            processedJson.contains("\"具体的回复建议\""))
        
        // 验证能够成功解析为AnalysisResult
        val adapter = moshi.adapter(AnalysisResult::class.java)
        val result = adapter.lenient().fromJson(processedJson)
        
        assertNotNull("应该能够成功解析AnalysisResult，不再抛出JsonDataException", result)
        assertNotNull("回复建议应该不为null", result?.replySuggestion)
        assertNotNull("策略分析应该不为null", result?.strategyAnalysis)
        assertNotNull("风险等级应该不为null", result?.riskLevel)
    }

    @Test
    fun `should gracefully handle mapping failures`() {
        // 测试映射失败时的降级处理
        val problematicJson = """
        {
            "未知字段名": "未知内容",
            "replySuggestion": "正常内容"
        }
        """.trimIndent()

        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, problematicJson) as String
        
        // 验证已知字段被正确处理，未知字段保持不变
        assertTrue("应该保持英文字段名 replySuggestion", processedJson.contains("\"replySuggestion\""))
        assertTrue("应该保持未知字段名", processedJson.contains("\"未知字段名\""))
        
        // 至少不应该崩溃
        assertNotNull("处理结果不应该为null", processedJson)
    }

    @Test
    fun `should handle nested structure mapping`() {
        // 测试嵌套结构中的字段映射
        val nestedJson = """
        {
            "回复建议": "这是建议",
            "详细信息": {
                "对方当前的情绪和潜在意图": "情绪分析",
                "风险等级": "SAFE"
            }
        }
        """.trimIndent()

        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, nestedJson) as String
        
        // 验证顶层字段被映射
        assertTrue("应该映射顶层的'回复建议'", processedJson.contains("\"replySuggestion\""))
        
        // 验证嵌套字段被映射
        assertTrue("应该映射嵌套的'对方当前的情绪和潜在意图'", processedJson.contains("\"strategyAnalysis\""))
        assertTrue("应该映射嵌套的'风险等级'", processedJson.contains("\"riskLevel\""))
        
        // 验证不再包含中文字段名
        assertFalse("不应该包含中文字段名'回复建议'", processedJson.contains("\"回复建议\""))
        assertFalse("不应该包含中文字段名'对方当前的情绪和潜在意图'", 
            processedJson.contains("\"对方当前的情绪和潜在意图\""))
        assertFalse("不应该包含中文字段名'风险等级'", processedJson.contains("\"风险等级\""))
        
        println("✅ 嵌套结构映射测试通过")
    }

    @Test
    fun `should handle array with Chinese field names`() {
        // 测试数组中的字段映射
        val arrayJson = """
        {
            "回复建议": "这是建议",
            "风险列表": [
                {"风险等级": "WARNING", "描述": "风险1"},
                {"风险等级": "SAFE", "描述": "风险2"}
            ]
        }
        """.trimIndent()

        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, arrayJson) as String
        
        // 验证顶层字段被映射
        assertTrue("应该映射'回复建议'", processedJson.contains("\"replySuggestion\""))
        
        // 验证数组中的字段被映射
        assertTrue("应该映射数组中的'风险等级'", processedJson.contains("\"riskLevel\""))
        
        // 验证不再包含中文字段名
        assertFalse("不应该包含中文字段名'回复建议'", processedJson.contains("\"回复建议\""))
        assertFalse("不应该包含中文字段名'风险等级'", processedJson.contains("\"风险等级\""))
        
        println("✅ 数组字段映射测试通过")
    }

    @Test
    fun `should handle all configured Chinese field names`() {
        // 测试配置文件中的所有中文字段名
        val allFieldsJson = """
        {
            "回复建议": "建议1",
            "建议回复": "建议2",
            "话术建议": "建议3",
            "策略分析": "分析1",
            "心理分析": "分析2",
            "军师分析": "分析3",
            "风险等级": "SAFE",
            "风险级别": "WARNING",
            "是否安全": true,
            "安全": false,
            "触发的风险": ["风险1"],
            "风险列表": ["风险2"],
            "建议": "建议内容",
            "修改建议": "修改内容",
            "事实": {"key": "value"},
            "事实信息": {"key2": "value2"},
            "红色标签": ["标签1"],
            "雷区": ["标签2"],
            "绿色标签": ["标签3"],
            "策略": ["标签4"]
        }
        """.trimIndent()

        val preprocessMethod = getPreprocessJsonResponseMethod()
        val processedJson = preprocessMethod.invoke(null, allFieldsJson) as String
        
        // 验证所有中文字段名都被映射为对应的英文字段名
        assertTrue("应该包含 replySuggestion", processedJson.contains("\"replySuggestion\""))
        assertTrue("应该包含 strategyAnalysis", processedJson.contains("\"strategyAnalysis\""))
        assertTrue("应该包含 riskLevel", processedJson.contains("\"riskLevel\""))
        assertTrue("应该包含 isSafe", processedJson.contains("\"isSafe\""))
        assertTrue("应该包含 triggeredRisks", processedJson.contains("\"triggeredRisks\""))
        assertTrue("应该包含 suggestion", processedJson.contains("\"suggestion\""))
        assertTrue("应该包含 facts", processedJson.contains("\"facts\""))
        assertTrue("应该包含 redTags", processedJson.contains("\"redTags\""))
        assertTrue("应该包含 greenTags", processedJson.contains("\"greenTags\""))
        
        // 验证不再包含任何中文字段名
        assertFalse("不应该包含'回复建议'", processedJson.contains("\"回复建议\""))
        assertFalse("不应该包含'策略分析'", processedJson.contains("\"策略分析\""))
        assertFalse("不应该包含'风险等级'", processedJson.contains("\"风险等级\""))
        assertFalse("不应该包含'是否安全'", processedJson.contains("\"是否安全\""))
        assertFalse("不应该包含'触发的风险'", processedJson.contains("\"触发的风险\""))
        assertFalse("不应该包含'红色标签'", processedJson.contains("\"红色标签\""))
        assertFalse("不应该包含'绿色标签'", processedJson.contains("\"绿色标签\""))
        
        println("✅ 所有配置字段映射测试通过")
    }

    /**
     * 通过反射获取AiRepositoryImpl的preprocessJsonResponse方法
     */
    private fun getPreprocessJsonResponseMethod(): Method {
        val clazz = Class.forName("com.empathy.ai.data.repository.AiRepositoryImpl")
        val method = clazz.getDeclaredMethod("preprocessJsonResponse", String::class.java)
        method.isAccessible = true
        return method
    }
}