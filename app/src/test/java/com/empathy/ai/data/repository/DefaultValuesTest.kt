package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.RiskLevel
import org.junit.Test
import org.junit.Assert.*

/**
 * DefaultValues 单元测试
 *
 * 测试默认值定义是否符合预期
 */
class DefaultValuesTest {

    @Test
    fun `ANALYSIS_RESULT should have safe default values`() {
        // When
        val result = DefaultValues.ANALYSIS_RESULT

        // Then
        assertNotNull("replySuggestion 不应为 null", result.replySuggestion)
        assertNotNull("strategyAnalysis 不应为 null", result.strategyAnalysis)
        assertEquals("默认风险等级应为 SAFE", RiskLevel.SAFE, result.riskLevel)
        
        assertTrue(
            "replySuggestion 应该包含提示信息",
            result.replySuggestion.contains("AI") || result.replySuggestion.contains("重试")
        )
        assertTrue(
            "strategyAnalysis 应该包含说明信息",
            result.strategyAnalysis.contains("不可用") || result.strategyAnalysis.contains("建议")
        )
    }

    @Test
    fun `SAFETY_CHECK_RESULT should have safe default values`() {
        // When
        val result = DefaultValues.SAFETY_CHECK_RESULT

        // Then
        assertTrue("默认应该是安全的", result.isSafe)
        assertTrue("默认不应触发任何风险", result.triggeredRisks.isEmpty())
        assertNotNull("suggestion 不应为 null", result.suggestion)
        val suggestion = result.suggestion ?: ""
        assertTrue(
            "suggestion 应该包含警告信息",
            suggestion.contains("不可用") || suggestion.contains("谨慎")
        )
    }

    @Test
    fun `EXTRACTED_DATA should have empty default values`() {
        // When
        val result = DefaultValues.EXTRACTED_DATA

        // Then
        assertNotNull("facts 不应为 null", result.facts)
        assertNotNull("redTags 不应为 null", result.redTags)
        assertNotNull("greenTags 不应为 null", result.greenTags)
        
        assertTrue("默认 facts 应该为空", result.facts.isEmpty())
        assertTrue("默认 redTags 应该为空", result.redTags.isEmpty())
        assertTrue("默认 greenTags 应该为空", result.greenTags.isEmpty())
    }

    @Test
    fun `logDefaultValueUsage should not throw exception`() {
        // When & Then - 确保方法不会抛出异常
        DefaultValues.logDefaultValueUsage("AnalysisResult", "测试原因")
        DefaultValues.logDefaultValueUsage("SafetyCheckResult", "网络错误")
        DefaultValues.logDefaultValueUsage("ExtractedData", "解析失败")
    }

    @Test
    fun `ANALYSIS_RESULT should provide helpful error messages`() {
        // When
        val result = DefaultValues.ANALYSIS_RESULT

        // Then
        val replySuggestion = result.replySuggestion ?: ""
        val strategyAnalysis = result.strategyAnalysis ?: ""
        assertTrue(
            "应该提示用户重试",
            replySuggestion.contains("重试") || strategyAnalysis.contains("重试")
        )
        assertTrue(
            "应该提示用户切换模型",
            replySuggestion.contains("切换") || strategyAnalysis.contains("切换")
        )
    }

    @Test
    fun `SAFETY_CHECK_RESULT should warn users about unavailability`() {
        // When
        val result = DefaultValues.SAFETY_CHECK_RESULT

        // Then
        val suggestion = result.suggestion ?: ""
        assertTrue(
            "应该警告用户功能不可用",
            suggestion.contains("不可用") || suggestion.contains("暂时")
        )
        assertTrue(
            "应该建议用户谨慎操作",
            suggestion.contains("谨慎") || suggestion.contains("手动")
        )
    }
}
