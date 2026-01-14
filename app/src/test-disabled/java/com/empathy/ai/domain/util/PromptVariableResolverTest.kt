package com.empathy.ai.domain.util

import com.empathy.ai.testutil.PromptTestDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PromptVariableResolver单元测试
 *
 * 测试覆盖：
 * - 变量替换功能
 * - 变量提取功能
 * - 无效变量检测
 * - 边界情况
 *
 * 注意：CR-00012优化后已移除缓存机制，相关测试保留以验证结果一致性
 */
class PromptVariableResolverTest {

    private lateinit var resolver: PromptVariableResolver

    @Before
    fun setup() {
        resolver = PromptVariableResolver()
    }

    // ========== resolve() 测试 ==========

    @Test
    fun `resolve should replace all valid variables`() {
        // Given
        val template = PromptTestDataFactory.createTemplateWithAllVariables()
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = resolver.resolve(template, context)

        // Then
        assertTrue(result.contains("测试联系人"))
        assertTrue(result.contains("暧昧期"))
        assertTrue(result.contains("不喜欢被催"))
        assertTrue(result.contains("喜欢被夸"))
        assertTrue(result.contains("5"))
        assertTrue(result.contains("2025-12-16"))
    }

    @Test
    fun `resolve should keep unknown variables unchanged`() {
        // Given
        val template = "Hello {{unknown_var}}, your name is {{contact_name}}"
        val context = PromptTestDataFactory.createPromptContext(contactName = "小明")

        // When
        val result = resolver.resolve(template, context)

        // Then
        assertEquals("Hello {{unknown_var}}, your name is 小明", result)
    }

    @Test
    fun `resolve should be case insensitive for variable names`() {
        // Given
        val template = "{{CONTACT_NAME}} and {{Contact_Name}} and {{contact_name}}"
        val context = PromptTestDataFactory.createPromptContext(contactName = "测试")

        // When
        val result = resolver.resolve(template, context)

        // Then
        assertEquals("测试 and 测试 and 测试", result)
    }

    @Test
    fun `resolve should handle empty context`() {
        // Given
        val template = "联系人: {{contact_name}}, 关系: {{relationship_status}}"
        val context = PromptTestDataFactory.createEmptyPromptContext()

        // When
        val result = resolver.resolve(template, context)

        // Then
        // 空上下文时，变量值为null，保持原始占位符
        assertEquals("联系人: {{contact_name}}, 关系: {{relationship_status}}", result)
    }

    @Test
    fun `resolve should handle template without variables`() {
        // Given
        val template = "这是一个没有变量的模板"
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = resolver.resolve(template, context)

        // Then
        assertEquals(template, result)
    }

    @Test
    fun `resolve should handle empty template`() {
        // Given
        val template = ""
        val context = PromptTestDataFactory.createPromptContext()

        // When
        val result = resolver.resolve(template, context)

        // Then
        assertEquals("", result)
    }

    @Test
    fun `resolve should handle list variables`() {
        // Given
        val template = "雷区: {{risk_tags}}, 策略: {{strategy_tags}}"
        val context = PromptTestDataFactory.createPromptContext(
            riskTags = listOf("雷区1", "雷区2"),
            strategyTags = listOf("策略1", "策略2")
        )

        // When
        val result = resolver.resolve(template, context)

        // Then
        assertTrue(result.contains("雷区1"))
        assertTrue(result.contains("雷区2"))
        assertTrue(result.contains("策略1"))
        assertTrue(result.contains("策略2"))
    }

    // ========== extractVariables() 测试 ==========

    @Test
    fun `extractVariables should return all variable names`() {
        // Given
        val template = PromptTestDataFactory.createTemplateWithAllVariables()

        // When
        val variables = resolver.extractVariables(template)

        // Then
        assertEquals(6, variables.size)
        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("risk_tags"))
        assertTrue(variables.contains("strategy_tags"))
        assertTrue(variables.contains("facts_count"))
        assertTrue(variables.contains("today_date"))
    }

    @Test
    fun `extractVariables should return distinct variables`() {
        // Given
        val template = "{{name}} and {{name}} and {{NAME}}"

        // When
        val variables = resolver.extractVariables(template)

        // Then
        assertEquals(1, variables.size)
        assertEquals("name", variables[0])
    }

    @Test
    fun `extractVariables should return empty list for template without variables`() {
        // Given
        val template = "没有变量的模板"

        // When
        val variables = resolver.extractVariables(template)

        // Then
        assertTrue(variables.isEmpty())
    }

    @Test
    fun `extractVariables should return consistent results on multiple calls`() {
        // Given
        val template = "{{var1}} {{var2}}"

        // When
        val first = resolver.extractVariables(template)
        val second = resolver.extractVariables(template)

        // Then - 多次调用结果应该一致
        assertEquals(first, second)
    }

    // ========== findInvalidVariables() 测试 ==========

    @Test
    fun `findInvalidVariables should return invalid variables`() {
        // Given
        val template = PromptTestDataFactory.createTemplateWithInvalidVariables()
        val allowedVariables = listOf("contact_name", "relationship_status")

        // When
        val invalid = resolver.findInvalidVariables(template, allowedVariables)

        // Then
        assertEquals(1, invalid.size)
        assertEquals("invalid_var", invalid[0])
    }

    @Test
    fun `findInvalidVariables should return empty list when all valid`() {
        // Given
        val template = "{{contact_name}} {{relationship_status}}"
        val allowedVariables = listOf("contact_name", "relationship_status")

        // When
        val invalid = resolver.findInvalidVariables(template, allowedVariables)

        // Then
        assertTrue(invalid.isEmpty())
    }

    @Test
    fun `findInvalidVariables should be case insensitive`() {
        // Given
        val template = "{{CONTACT_NAME}}"
        val allowedVariables = listOf("contact_name")

        // When
        val invalid = resolver.findInvalidVariables(template, allowedVariables)

        // Then
        assertTrue(invalid.isEmpty())
    }

    // ========== clearCache() 测试 ==========

    @Test
    fun `clearCache should not throw exception`() {
        // Given
        val template = "{{var1}}"
        resolver.extractVariables(template)

        // When - clearCache保留为空实现，确保不抛异常
        resolver.clearCache()

        // Then - 调用后功能正常
        val result = resolver.extractVariables(template)
        assertEquals(1, result.size)
    }
}
