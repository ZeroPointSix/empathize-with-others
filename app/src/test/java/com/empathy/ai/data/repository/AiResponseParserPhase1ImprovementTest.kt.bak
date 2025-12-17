package com.empathy.ai.data.repository

import com.empathy.ai.data.remote.api.OpenAiApi
import com.empathy.ai.data.remote.model.ChatRequestDto
import com.empathy.ai.data.remote.model.MessageDto
import com.empathy.ai.data.remote.model.ResponseFormatDto
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.usecase.ExtractedData
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AI响应解析器阶段1改进测试
 * 
 * 测试所有新增的改进功能，包括：
 * 1. 增强的字段映射机制
 * 2. 模糊匹配逻辑
 * 3. 改进的JSON预处理
 * 4. 多层次的字段提取策略
 * 5. 智能降级策略
 * 6. 错误分类和监控
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AiResponseParserPhase1ImprovementTest {

    private lateinit var aiRepository: AiRepositoryImpl
    private val mockApi: OpenAiApi = mockk()
    private val mockProviderRepository: AiProviderRepository = mockk()
    private val moshi = Moshi.Builder().build()

    @Before
    fun setup() {
        // 创建AiRepositoryImpl实例用于测试私有方法
        aiRepository = AiRepositoryImpl(mockApi, mockProviderRepository)
        
        // 模拟默认提供商
        val mockProvider = mockk<com.empathy.ai.domain.model.AiProvider>()
        every { mockProvider.name } returns "TestProvider"
        every { mockProvider.baseUrl } returns "https://api.example.com"
        every { mockProvider.apiKey } returns "test-api-key"
        every { mockProvider.defaultModelId } returns "gpt-3.5-turbo"
        
        coEvery { mockProviderRepository.getDefaultProvider() } returns Result.success(mockProvider)
    }

    @Test
    fun `测试字段映射增强 - 标准字段`() = runTest {
        // 测试标准字段名映射
        val json = """{"replySuggestion": "测试回复", "strategyAnalysis": "测试分析", "riskLevel": "SAFE"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        assertEquals("回复建议应该匹配", "测试回复", analysisResult?.replySuggestion)
        assertEquals("策略分析应该匹配", "测试分析", analysisResult?.strategyAnalysis)
        assertEquals("风险等级应该匹配", RiskLevel.SAFE, analysisResult?.riskLevel)
    }

    @Test
    fun `测试字段映射增强 - 中文字段`() = runTest {
        // 测试中文字段名映射
        val json = """{"回复建议": "测试回复", "策略分析": "测试分析", "风险等级": "安全"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        assertEquals("回复建议应该匹配", "测试回复", analysisResult?.replySuggestion)
        assertEquals("策略分析应该匹配", "测试分析", analysisResult?.strategyAnalysis)
        assertEquals("风险等级应该匹配", RiskLevel.SAFE, analysisResult?.riskLevel)
    }

    @Test
    fun `测试字段映射增强 - 变体字段`() = runTest {
        // 测试变体字段名映射
        val json = """{"reply": "测试回复", "analysis": "测试分析", "level": "low"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        assertEquals("回复建议应该匹配", "测试回复", analysisResult?.replySuggestion)
        assertEquals("策略分析应该匹配", "测试分析", analysisResult?.strategyAnalysis)
        assertEquals("风险等级应该匹配", RiskLevel.SAFE, analysisResult?.riskLevel)
    }

    @Test
    fun `测试JSON预处理增强 - 移除Markdown代码块`() = runTest {
        // 测试Markdown代码块移除
        val jsonWithMarkdown = """```json
{"replySuggestion": "测试回复", "strategyAnalysis": "测试分析", "riskLevel": "SAFE"}
```"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, jsonWithMarkdown) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        assertEquals("回复建议应该匹配", "测试回复", analysisResult?.replySuggestion)
    }

    @Test
    fun `测试JSON预处理增强 - 处理Unicode编码`() = runTest {
        // 测试Unicode编码处理
        val jsonWithUnicode = """{"replySuggestion": "测试\\u0020回复", "strategyAnalysis": "测试分析", "riskLevel": "SAFE"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, jsonWithUnicode) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        assertEquals("回复建议应该包含空格", "测试 回复", analysisResult?.replySuggestion)
    }

    @Test
    fun `测试JSON预处理增强 - 修复格式错误`() = runTest {
        // 测试格式错误修复
        val jsonWithErrors = """{"replySuggestion": "测试回复", "strategyAnalysis": "测试分析", "riskLevel": "SAFE",}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, jsonWithErrors) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        assertEquals("回复建议应该匹配", "测试回复", analysisResult?.replySuggestion)
        assertEquals("策略分析应该匹配", "测试分析", analysisResult?.strategyAnalysis)
        assertEquals("风险等级应该匹配", RiskLevel.SAFE, analysisResult?.riskLevel)
    }

    @Test
    fun `测试SafetyCheckResult解析增强`() = runTest {
        // 测试SafetyCheckResult解析增强
        val json = """{"isSafe": false, "triggeredRisks": ["风险1", "风险2"], "suggestion": "建议修改"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseSafetyCheckResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<SafetyCheckResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val safetyResult = result.getOrNull()
        assertNotNull("结果不应为null", safetyResult)
        assertEquals("安全状态应该匹配", false, safetyResult?.isSafe)
        assertEquals("风险列表应该匹配", listOf("风险1", "风险2"), safetyResult?.triggeredRisks)
        assertEquals("建议应该匹配", "建议修改", safetyResult?.suggestion)
    }

    @Test
    fun `测试SafetyCheckResult解析增强 - 中文字段`() = runTest {
        // 测试SafetyCheckResult中文字段映射
        val json = """{"是否安全": false, "风险列表": ["风险1", "风险2"], "修改建议": "建议修改"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseSafetyCheckResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<SafetyCheckResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val safetyResult = result.getOrNull()
        assertNotNull("结果不应为null", safetyResult)
        assertEquals("安全状态应该匹配", false, safetyResult?.isSafe)
        assertEquals("风险列表应该匹配", listOf("风险1", "风险2"), safetyResult?.triggeredRisks)
        assertEquals("建议应该匹配", "建议修改", safetyResult?.suggestion)
    }

    @Test
    fun `测试ExtractedData解析增强`() = runTest {
        // 测试ExtractedData解析增强
        val json = """{"facts": {"姓名": "张三", "年龄": "25"}, "redTags": ["不要谈论政治"], "greenTags": ["多分享生活趣事"]}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseExtractedData", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<ExtractedData>
        
        assertTrue("解析应该成功", result.isSuccess)
        val extractedData = result.getOrNull()
        assertNotNull("结果不应为null", extractedData)
        assertEquals("事实信息应该匹配", mapOf("姓名" to "张三", "年龄" to "25"), extractedData?.facts)
        assertEquals("红色标签应该匹配", listOf("不要谈论政治"), extractedData?.redTags)
        assertEquals("绿色标签应该匹配", listOf("多分享生活趣事"), extractedData?.greenTags)
    }

    @Test
    fun `测试ExtractedData解析增强 - 中文字段`() = runTest {
        // 测试ExtractedData中文字段映射
        val json = """{"事实信息": {"姓名": "张三", "年龄": "25"}, "雷区": ["不要谈论政治"], "策略": ["多分享生活趣事"]}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseExtractedData", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<ExtractedData>
        
        assertTrue("解析应该成功", result.isSuccess)
        val extractedData = result.getOrNull()
        assertNotNull("结果不应为null", extractedData)
        assertEquals("事实信息应该匹配", mapOf("姓名" to "张三", "年龄" to "25"), extractedData?.facts)
        assertEquals("红色标签应该匹配", listOf("不要谈论政治"), extractedData?.redTags)
        assertEquals("绿色标签应该匹配", listOf("多分享生活趣事"), extractedData?.greenTags)
    }

    @Test
    fun `测试多层次字段提取策略`() = runTest {
        // 测试多层次字段提取策略
        val json = """{"nested": {"reply": "嵌套回复", "analysis": "嵌套分析", "level": "high"}}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        assertEquals("应该从嵌套结构提取回复", "嵌套回复", analysisResult?.replySuggestion)
        assertEquals("应该从嵌套结构提取分析", "嵌套分析", analysisResult?.strategyAnalysis)
        assertEquals("应该从嵌套结构提取风险等级", RiskLevel.DANGER, analysisResult?.riskLevel)
    }

    @Test
    fun `测试智能推断逻辑`() = runTest {
        // 测试智能推断逻辑
        val json = """{"content": "用户提到了危险和高风险的内容，需要谨慎处理"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        // 智能推断应该能够从内容中推断出高风险
        assertEquals("应该智能推断出高风险", RiskLevel.DANGER, analysisResult?.riskLevel)
    }

    @Test
    fun `测试智能降级策略`() = runTest {
        // 测试智能降级策略
        val invalidJson = """{"invalid": "json", "structure": "here"}"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, invalidJson) as Result<AnalysisResult>
        
        assertTrue("解析应该成功（使用降级策略）", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        // 即使JSON无效，也应该有默认值
        assertNotNull("应该有默认回复建议", analysisResult?.replySuggestion)
        assertNotNull("应该有默认策略分析", analysisResult?.strategyAnalysis)
        assertNotNull("应该有默认风险等级", analysisResult?.riskLevel)
    }

    @Test
    fun `测试错误分类和监控`() = runTest {
        // 测试错误分类和监控
        val malformedJson = """{"replySuggestion": "测试", "strategyAnalysis": "测试", "riskLevel": """ // 故意的JSON语法错误
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, malformedJson) as Result<AnalysisResult>
        
        // 即使JSON格式错误，也应该通过降级策略成功
        assertTrue("解析应该成功（使用降级策略）", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
    }

    @Test
    fun `测试模糊字段匹配`() = runTest {
        // 测试模糊字段匹配
        val json = """{"replySugestion": "测试回复", "strateyAnalysis": "测试分析", "riskLevl": "SAFE"}""" // 故意的拼写错误
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功（使用模糊匹配）", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        // 模糊匹配应该能够识别拼写错误的字段名
        assertNotNull("应该有回复建议", analysisResult?.replySuggestion)
        assertNotNull("应该有策略分析", analysisResult?.strategyAnalysis)
        assertNotNull("应该有风险等级", analysisResult?.riskLevel)
    }

    @Test
    fun `测试数组格式字段提取`() = runTest {
        // 测试数组格式字段提取
        val json = """{
            "suggestions": [
                {"text": "建议1", "priority": "high"},
                {"text": "建议2", "priority": "low"}
            ],
            "analysis_points": ["分析点1", "分析点2"],
            "risk_levels": ["safe", "warning"]
        }"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        // 应该优先选择高优先级的建议
        assertEquals("应该选择高优先级建议", "建议1", analysisResult?.replySuggestion)
        // 应该从数组中提取分析内容
        assertTrue("应该有策略分析", analysisResult?.strategyAnalysis?.isNotEmpty() == true)
        // 应该有风险等级
        assertNotNull("应该有风险等级", analysisResult?.riskLevel)
    }

    @Test
    fun `测试嵌套结构字段提取`() = runTest {
        // 测试嵌套结构字段提取
        val json = """{
            "response": {
                "text": "嵌套回复",
                "analysis": "嵌套分析"
            },
            "safety": {
                "level": "warning"
            }
        }"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseAnalysisResult", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<AnalysisResult>
        
        assertTrue("解析应该成功", result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull("结果不应为null", analysisResult)
        // 应该从嵌套结构中提取字段
        assertEquals("应该从嵌套结构提取回复", "嵌套回复", analysisResult?.replySuggestion)
        assertEquals("应该从嵌套结构提取分析", "嵌套分析", analysisResult?.strategyAnalysis)
        assertEquals("应该从嵌套结构提取风险等级", RiskLevel.WARNING, analysisResult?.riskLevel)
    }

    @Test
    fun `测试布尔类型转换`() = runTest {
        // 测试布尔类型转换
        val json1 = """{"isSafe": true}"""
        val json2 = """{"isSafe": "true"}"""
        val json3 = """{"isSafe": 1}"""
        val json4 = """{"是否安全": "是"}"""
        
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseSafetyCheckResult", String::class.java)
        method.isAccessible = true
        
        // 测试各种布尔表示形式
        val result1 = method.invoke(aiRepository, json1) as Result<SafetyCheckResult>
        assertTrue("布尔值true应该解析成功", result1.isSuccess)
        assertEquals("布尔值true应该映射为true", true, result1.getOrNull()?.isSafe)
        
        val result2 = method.invoke(aiRepository, json2) as Result<SafetyCheckResult>
        assertTrue("字符串'true'应该解析成功", result2.isSuccess)
        assertEquals("字符串'true'应该映射为true", true, result2.getOrNull()?.isSafe)
        
        val result3 = method.invoke(aiRepository, json3) as Result<SafetyCheckResult>
        assertTrue("数字1应该解析成功", result3.isSuccess)
        assertEquals("数字1应该映射为true", true, result3.getOrNull()?.isSafe)
        
        val result4 = method.invoke(aiRepository, json4) as Result<SafetyCheckResult>
        assertTrue("中文'是'应该解析成功", result4.isSuccess)
        assertEquals("中文'是'应该映射为true", true, result4.getOrNull()?.isSafe)
    }

    @Test
    fun `测试事实信息扁平化`() = runTest {
        // 测试事实信息扁平化
        val json = """{
            "facts": {
                "basic": {
                    "name": "张三",
                    "age": 25
                },
                "preferences": {
                    "hobbies": ["阅读", "旅行"],
                    "food": "中餐"
                }
            }
        }"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseExtractedData", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<ExtractedData>
        
        assertTrue("解析应该成功", result.isSuccess)
        val extractedData = result.getOrNull()
        assertNotNull("结果不应为null", extractedData)
        // 嵌套对象应该被扁平化
        assertEquals("应该有扁平化的事实信息", 4, extractedData?.facts?.size)
        assertTrue("应该包含姓名", extractedData?.facts?.containsKey("basic") == true)
        assertTrue("应该包含偏好", extractedData?.facts?.containsKey("preferences") == true)
    }

    @Test
    fun `测试标签去重`() = runTest {
        // 测试标签去重
        val json = """{
            "redTags": ["不要谈论政治", "不要谈论政治", "避免敏感话题", "不要谈论政治"],
            "greenTags": ["多分享生活趣事", "多分享生活趣事", "保持耐心", "多分享生活趣事"]
        }"""
        
        // 使用反射调用私有方法
        val method = AiRepositoryImpl::class.java.getDeclaredMethod("parseExtractedData", String::class.java)
        method.isAccessible = true
        val result = method.invoke(aiRepository, json) as Result<ExtractedData>
        
        assertTrue("解析应该成功", result.isSuccess)
        val extractedData = result.getOrNull()
        assertNotNull("结果不应为null", extractedData)
        // 标签应该被去重
        assertEquals("红色标签应该去重", 2, extractedData?.redTags?.size)
        assertEquals("绿色标签应该去重", 2, extractedData?.greenTags?.size)
        assertTrue("应该包含'不要谈论政治'", extractedData?.redTags?.contains("不要谈论政治") == true)
        assertTrue("应该包含'避免敏感话题'", extractedData?.redTags?.contains("避免敏感话题") == true)
        assertTrue("应该包含'多分享生活趣事'", extractedData?.greenTags?.contains("多分享生活趣事") == true)
        assertTrue("应该包含'保持耐心'", extractedData?.greenTags?.contains("保持耐心") == true)
    }
}