package com.empathy.ai.data.repository

import org.junit.Test
import org.junit.Assert.*

/**
 * FieldMappingConfig 单元测试
 *
 * 测试字段映射配置的默认值功能
 */
class FieldMappingConfigTest {

    @Test
    fun `getDefaultMappings should return all required field mappings`() {
        // Given
        val expectedFields = setOf(
            // AnalysisResult
            "replySuggestion",
            "strategyAnalysis",
            "riskLevel",
            // SafetyCheckResult
            "isSafe",
            "triggeredRisks",
            "suggestion",
            // ExtractedData
            "facts",
            "redTags",
            "greenTags"
        )

        // When
        val mappings = FieldMappingConfig.getDefaultMappings()

        // Then
        assertEquals("应该包含所有必需的字段映射", expectedFields, mappings.keys)
    }

    @Test
    fun `getDefaultMappings should return non-empty Chinese field names for each mapping`() {
        // When
        val mappings = FieldMappingConfig.getDefaultMappings()

        // Then
        mappings.forEach { (englishField, chineseFields) ->
            assertTrue(
                "字段 $englishField 应该至少有一个中文映射",
                chineseFields.isNotEmpty()
            )
        }
    }

    @Test
    fun `replySuggestion mapping should contain expected Chinese field names`() {
        // When
        val mappings = FieldMappingConfig.getDefaultMappings()
        val replySuggestionMappings = mappings["replySuggestion"]

        // Then
        assertNotNull("replySuggestion 映射不应为 null", replySuggestionMappings)
        assertTrue(
            "应该包含 '回复建议'",
            replySuggestionMappings!!.contains("回复建议")
        )
        assertTrue(
            "应该包含 '建议回复'",
            replySuggestionMappings.contains("建议回复")
        )
    }

    @Test
    fun `strategyAnalysis mapping should contain expected Chinese field names`() {
        // When
        val mappings = FieldMappingConfig.getDefaultMappings()
        val strategyAnalysisMappings = mappings["strategyAnalysis"]

        // Then
        assertNotNull("strategyAnalysis 映射不应为 null", strategyAnalysisMappings)
        assertTrue(
            "应该包含 '策略分析'",
            strategyAnalysisMappings!!.contains("策略分析")
        )
        assertTrue(
            "应该包含 '心理分析'",
            strategyAnalysisMappings.contains("心理分析")
        )
    }

    @Test
    fun `riskLevel mapping should contain expected Chinese field names`() {
        // When
        val mappings = FieldMappingConfig.getDefaultMappings()
        val riskLevelMappings = mappings["riskLevel"]

        // Then
        assertNotNull("riskLevel 映射不应为 null", riskLevelMappings)
        assertTrue(
            "应该包含 '风险等级'",
            riskLevelMappings!!.contains("风险等级")
        )
        assertTrue(
            "应该包含 '风险级别'",
            riskLevelMappings.contains("风险级别")
        )
    }

    @Test
    fun `clearCache should reset cached mappings`() {
        // Given
        FieldMappingConfig.getDefaultMappings() // 触发缓存

        // When
        FieldMappingConfig.clearCache()

        // Then
        // 无法直接验证缓存是否清除，但至少确保方法不会抛出异常
        val mappings = FieldMappingConfig.getDefaultMappings()
        assertNotNull("清除缓存后应该仍能获取默认配置", mappings)
    }
}
