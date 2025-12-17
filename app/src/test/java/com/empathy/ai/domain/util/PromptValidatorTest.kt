package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.PromptValidationResult
import com.empathy.ai.testutil.PromptTestDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PromptValidator单元测试
 *
 * 测试覆盖：
 * - 空提示词验证
 * - 长度限制验证
 * - 无效变量验证
 * - 警告阈值验证
 * - 批量验证
 */
class PromptValidatorTest {

    private lateinit var validator: PromptValidator
    private lateinit var variableResolver: PromptVariableResolver

    @Before
    fun setup() {
        variableResolver = PromptVariableResolver()
        validator = PromptValidator(variableResolver)
    }

    // ========== 空提示词测试 ==========

    @Test
    fun `validate should return Success for empty prompt when allowEmpty is true`() {
        // Given
        val prompt = PromptTestDataFactory.createEmptyPrompt()

        // When - 默认allowEmpty=true
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then - 空值合法，表示使用系统默认
        assertTrue(result is PromptValidationResult.Success)
    }

    @Test
    fun `validate should return Error for empty prompt when allowEmpty is false`() {
        // Given
        val prompt = PromptTestDataFactory.createEmptyPrompt()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE, allowEmpty = false)

        // Then
        assertTrue(result is PromptValidationResult.Error)
        assertEquals(
            PromptValidationResult.ErrorType.EMPTY_PROMPT,
            (result as PromptValidationResult.Error).errorType
        )
    }

    @Test
    fun `validate should return Success for blank prompt when allowEmpty is true`() {
        // Given
        val prompt = PromptTestDataFactory.createBlankPrompt()

        // When - 默认allowEmpty=true
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then - 空值合法，表示使用系统默认
        assertTrue(result is PromptValidationResult.Success)
    }

    @Test
    fun `validate should return Error for blank prompt when allowEmpty is false`() {
        // Given
        val prompt = PromptTestDataFactory.createBlankPrompt()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE, allowEmpty = false)

        // Then
        assertTrue(result is PromptValidationResult.Error)
        assertEquals(
            PromptValidationResult.ErrorType.EMPTY_PROMPT,
            (result as PromptValidationResult.Error).errorType
        )
    }

    // ========== 长度限制测试 ==========

    @Test
    fun `validate should return Error for prompt exceeding max length`() {
        // Given
        val prompt = PromptTestDataFactory.createOverLengthPrompt()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Error)
        assertEquals(
            PromptValidationResult.ErrorType.EXCEEDS_LENGTH_LIMIT,
            (result as PromptValidationResult.Error).errorType
        )
        assertTrue(result.message.contains("1000"))
    }

    @Test
    fun `validate should return Success for prompt at max length`() {
        // Given
        val prompt = PromptTestDataFactory.createMaxLengthPrompt()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        // 1000字符刚好在限制内，但超过警告阈值800
        assertTrue(result is PromptValidationResult.Warning)
    }

    @Test
    fun `validate should return Warning for prompt near length limit`() {
        // Given
        val prompt = PromptTestDataFactory.createJustOverWarningPrompt()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Warning)
        assertEquals(
            PromptValidationResult.WarningType.NEAR_LENGTH_LIMIT,
            (result as PromptValidationResult.Warning).warningType
        )
    }

    @Test
    fun `validate should return Success for prompt under warning threshold`() {
        // Given
        val prompt = "A".repeat(800) // 刚好在警告阈值

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Success)
    }

    // ========== 无效变量测试 ==========

    @Test
    fun `validate should return Error for invalid variables`() {
        // Given
        val prompt = PromptTestDataFactory.createTemplateWithOnlyInvalidVariables()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Error)
        assertEquals(
            PromptValidationResult.ErrorType.INVALID_VARIABLES,
            (result as PromptValidationResult.Error).errorType
        )
        assertTrue(result.message.contains("unknown_var"))
    }

    @Test
    fun `validate should return Success for valid variables`() {
        // Given
        val prompt = "分析与{{contact_name}}的对话"

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Success)
    }

    @Test
    fun `validate should accept all available variables for scene`() {
        // Given
        val prompt = PromptTestDataFactory.createTemplateWithAllVariables()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Success)
    }

    // ========== 正常提示词测试 ==========

    @Test
    fun `validate should return Success for valid prompt`() {
        // Given
        val prompt = PromptTestDataFactory.createSafePrompt()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Success)
    }

    @Test
    fun `validate should return Success for prompt with valid variables`() {
        // Given
        val prompt = PromptTestDataFactory.createSafePromptWithVariables()

        // When
        val result = validator.validate(prompt, PromptScene.ANALYZE)

        // Then
        assertTrue(result is PromptValidationResult.Success)
    }

    // ========== 批量验证测试 ==========

    @Test
    fun `validateAll should validate all scenes with allowEmpty true by default`() {
        // Given
        val prompts = mapOf(
            PromptScene.ANALYZE to "分析提示词",
            PromptScene.CHECK to "",  // 空提示词，默认允许
            PromptScene.EXTRACT to "提取提示词",
            PromptScene.SUMMARY to PromptTestDataFactory.createOverLengthPrompt()
        )

        // When - 默认allowEmpty=true
        val results = validator.validateAll(prompts)

        // Then
        assertEquals(4, results.size)
        assertTrue(results[PromptScene.ANALYZE] is PromptValidationResult.Success)
        assertTrue(results[PromptScene.CHECK] is PromptValidationResult.Success) // 空值现在合法
        assertTrue(results[PromptScene.EXTRACT] is PromptValidationResult.Success)
        assertTrue(results[PromptScene.SUMMARY] is PromptValidationResult.Error)
    }

    @Test
    fun `validateAll should reject empty prompts when allowEmpty is false`() {
        // Given
        val prompts = mapOf(
            PromptScene.ANALYZE to "分析提示词",
            PromptScene.CHECK to "",  // 空提示词
            PromptScene.EXTRACT to "提取提示词",
            PromptScene.SUMMARY to PromptTestDataFactory.createOverLengthPrompt()
        )

        // When
        val results = validator.validateAll(prompts, allowEmpty = false)

        // Then
        assertEquals(4, results.size)
        assertTrue(results[PromptScene.ANALYZE] is PromptValidationResult.Success)
        assertTrue(results[PromptScene.CHECK] is PromptValidationResult.Error) // 空值不允许
        assertTrue(results[PromptScene.EXTRACT] is PromptValidationResult.Success)
        assertTrue(results[PromptScene.SUMMARY] is PromptValidationResult.Error)
    }

    // ========== 常量测试 ==========

    @Test
    fun `MAX_PROMPT_LENGTH should be 1000`() {
        assertEquals(1000, PromptValidator.MAX_PROMPT_LENGTH)
    }

    @Test
    fun `WARNING_LENGTH_THRESHOLD should be 800`() {
        assertEquals(800, PromptValidator.WARNING_LENGTH_THRESHOLD)
    }
}
