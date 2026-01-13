package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FloatingWindowUiState 单元测试
 *
 * 测试悬浮窗UI状态模型的各种方法
 */
class FloatingWindowUiStateTest {

    @Test
    fun `default state should have correct initial values`() {
        // When
        val state = FloatingWindowUiState()

        // Then
        assertEquals(ActionType.ANALYZE, state.selectedTab)
        assertNull(state.selectedContactId)
        assertEquals("", state.inputText)
        assertNull(state.lastResult)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `hasResult should return false when lastResult is null`() {
        // Given
        val state = FloatingWindowUiState()

        // Then
        assertFalse(state.hasResult())
    }

    @Test
    fun `hasResult should return true when lastResult is not null`() {
        // Given
        val analysisResult = AnalysisResult(
            replySuggestion = "建议的回复",
            strategyAnalysis = "测试摘要",
            riskLevel = RiskLevel.SAFE
        )
        val state = FloatingWindowUiState(
            lastResult = AiResult.Analysis(analysisResult)
        )

        // Then
        assertTrue(state.hasResult())
    }

    @Test
    fun `canSubmit should return false when isLoading is true`() {
        // Given
        val state = FloatingWindowUiState(
            isLoading = true,
            inputText = "测试内容",
            selectedContactId = "contact_1"
        )

        // Then
        assertFalse(state.canSubmit())
    }

    @Test
    fun `canSubmit should return false when inputText is blank`() {
        // Given
        val state = FloatingWindowUiState(
            isLoading = false,
            inputText = "   ",
            selectedContactId = "contact_1"
        )

        // Then
        assertFalse(state.canSubmit())
    }

    @Test
    fun `canSubmit should return false when selectedContactId is null`() {
        // Given
        val state = FloatingWindowUiState(
            isLoading = false,
            inputText = "测试内容",
            selectedContactId = null
        )

        // Then
        assertFalse(state.canSubmit())
    }

    @Test
    fun `canSubmit should return true when all conditions are met`() {
        // Given
        val state = FloatingWindowUiState(
            isLoading = false,
            inputText = "测试内容",
            selectedContactId = "contact_1"
        )

        // Then
        assertTrue(state.canSubmit())
    }

    @Test
    fun `clearResult should clear lastResult and errorMessage`() {
        // Given
        val analysisResult = AnalysisResult(
            replySuggestion = "建议的回复",
            strategyAnalysis = "测试摘要",
            riskLevel = RiskLevel.SAFE
        )
        val state = FloatingWindowUiState(
            lastResult = AiResult.Analysis(analysisResult),
            errorMessage = "错误信息"
        )

        // When
        val clearedState = state.clearResult()

        // Then
        assertNull(clearedState.lastResult)
        assertNull(clearedState.errorMessage)
    }

    @Test
    fun `clearResult should preserve other fields`() {
        // Given
        val state = FloatingWindowUiState(
            selectedTab = ActionType.POLISH,
            selectedContactId = "contact_1",
            inputText = "测试内容",
            isLoading = false
        )

        // When
        val clearedState = state.clearResult()

        // Then
        assertEquals(ActionType.POLISH, clearedState.selectedTab)
        assertEquals("contact_1", clearedState.selectedContactId)
        assertEquals("测试内容", clearedState.inputText)
        assertFalse(clearedState.isLoading)
    }

    @Test
    fun `hasError should return false when errorMessage is null`() {
        // Given
        val state = FloatingWindowUiState(errorMessage = null)

        // Then
        assertFalse(state.hasError())
    }

    @Test
    fun `hasError should return false when errorMessage is blank`() {
        // Given
        val state = FloatingWindowUiState(errorMessage = "   ")

        // Then
        assertFalse(state.hasError())
    }

    @Test
    fun `hasError should return true when errorMessage is not blank`() {
        // Given
        val state = FloatingWindowUiState(errorMessage = "错误信息")

        // Then
        assertTrue(state.hasError())
    }

    @Test
    fun `resetKeepingMemory should clear input and result but keep tab and contact`() {
        // Given
        val analysisResult = AnalysisResult(
            replySuggestion = "建议的回复",
            strategyAnalysis = "测试摘要",
            riskLevel = RiskLevel.SAFE
        )
        val state = FloatingWindowUiState(
            selectedTab = ActionType.REPLY,
            selectedContactId = "contact_1",
            inputText = "测试内容",
            lastResult = AiResult.Analysis(analysisResult),
            isLoading = true,
            errorMessage = "错误"
        )

        // When
        val resetState = state.resetKeepingMemory()

        // Then
        assertEquals(ActionType.REPLY, resetState.selectedTab)
        assertEquals("contact_1", resetState.selectedContactId)
        assertEquals("", resetState.inputText)
        assertNull(resetState.lastResult)
        assertFalse(resetState.isLoading)
        assertNull(resetState.errorMessage)
    }

    @Test
    fun `resetAll should return default state`() {
        // Given
        val state = FloatingWindowUiState(
            selectedTab = ActionType.REPLY,
            selectedContactId = "contact_1",
            inputText = "测试内容",
            isLoading = true,
            errorMessage = "错误"
        )

        // When
        val resetState = state.resetAll()

        // Then
        assertEquals(ActionType.ANALYZE, resetState.selectedTab)
        assertNull(resetState.selectedContactId)
        assertEquals("", resetState.inputText)
        assertNull(resetState.lastResult)
        assertFalse(resetState.isLoading)
        assertNull(resetState.errorMessage)
    }

    // ==================== restoreFrom 测试 ====================

    @Test
    fun `restoreFrom should restore state from valid tab name`() {
        // Given
        val state = FloatingWindowUiState()

        // When
        val restoredState = state.restoreFrom("POLISH", "contact_1", "测试内容")

        // Then
        assertEquals(ActionType.POLISH, restoredState.selectedTab)
        assertEquals("contact_1", restoredState.selectedContactId)
        assertEquals("测试内容", restoredState.inputText)
    }

    @Test
    fun `restoreFrom should use ANALYZE for invalid tab name`() {
        // Given
        val state = FloatingWindowUiState()

        // When
        val restoredState = state.restoreFrom("INVALID_TAB", "contact_1", "测试内容")

        // Then
        assertEquals(ActionType.ANALYZE, restoredState.selectedTab)
        assertEquals("contact_1", restoredState.selectedContactId)
        assertEquals("测试内容", restoredState.inputText)
    }

    @Test
    fun `restoreFrom should handle null contact id`() {
        // Given
        val state = FloatingWindowUiState()

        // When
        val restoredState = state.restoreFrom("REPLY", null, "内容")

        // Then
        assertEquals(ActionType.REPLY, restoredState.selectedTab)
        assertNull(restoredState.selectedContactId)
        assertEquals("内容", restoredState.inputText)
    }

    // ==================== fromPersisted 测试 ====================

    @Test
    fun `fromPersisted should create state from valid data`() {
        // When
        val state = FloatingWindowUiState.fromPersisted("POLISH", "contact_123", "持久化内容")

        // Then
        assertEquals(ActionType.POLISH, state.selectedTab)
        assertEquals("contact_123", state.selectedContactId)
        assertEquals("持久化内容", state.inputText)
        assertFalse(state.isLoading)
        assertNull(state.lastResult)
        assertNull(state.errorMessage)
    }

    @Test
    fun `fromPersisted should use ANALYZE for invalid tab name`() {
        // When
        val state = FloatingWindowUiState.fromPersisted("UNKNOWN", "contact_1", "内容")

        // Then
        assertEquals(ActionType.ANALYZE, state.selectedTab)
    }

    @Test
    fun `fromPersisted should handle empty input text`() {
        // When
        val state = FloatingWindowUiState.fromPersisted("ANALYZE", "contact_1", "")

        // Then
        assertEquals("", state.inputText)
    }

    // ==================== 常量测试 ====================

    @Test
    fun `DEFAULT_TAB_NAME should be ANALYZE`() {
        assertEquals("ANALYZE", FloatingWindowUiState.DEFAULT_TAB_NAME)
    }

    @Test
    fun `DEFAULT_INPUT_TEXT should be empty string`() {
        assertEquals("", FloatingWindowUiState.DEFAULT_INPUT_TEXT)
    }

    @Test
    fun `MAX_INPUT_LENGTH should be 5000`() {
        assertEquals(5000, FloatingWindowUiState.MAX_INPUT_LENGTH)
    }
}