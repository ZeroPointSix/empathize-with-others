package com.empathy.ai.presentation.ui.screen.prompt

import com.empathy.ai.R
import com.empathy.ai.domain.model.PromptScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptEditorUiState单元测试
 */
class PromptEditorUiStateTest {

    // ========== charCount测试 ==========

    @Test
    fun `charCount returns 0 for empty prompt`() {
        val state = PromptEditorUiState(currentPrompt = "")
        assertEquals(0, state.charCount)
    }

    @Test
    fun `charCount returns correct length for non-empty prompt`() {
        val state = PromptEditorUiState(currentPrompt = "Hello World")
        assertEquals(11, state.charCount)
    }

    @Test
    fun `charCount handles unicode characters correctly`() {
        val state = PromptEditorUiState(currentPrompt = "你好世界")
        assertEquals(4, state.charCount)
    }

    // ========== isOverLimit测试 ==========

    @Test
    fun `isOverLimit returns false when under limit`() {
        val state = PromptEditorUiState(currentPrompt = "a".repeat(1000))
        assertFalse(state.isOverLimit)
    }

    @Test
    fun `isOverLimit returns true when over limit`() {
        val state = PromptEditorUiState(currentPrompt = "a".repeat(1001))
        assertTrue(state.isOverLimit)
    }

    @Test
    fun `isOverLimit returns false at exactly limit`() {
        val state = PromptEditorUiState(currentPrompt = "a".repeat(1000))
        assertFalse(state.isOverLimit)
    }

    // ========== isNearLimit测试 ==========

    @Test
    fun `isNearLimit returns false when under warning threshold`() {
        val state = PromptEditorUiState(currentPrompt = "a".repeat(800))
        assertFalse(state.isNearLimit)
    }

    @Test
    fun `isNearLimit returns true when over warning threshold`() {
        val state = PromptEditorUiState(currentPrompt = "a".repeat(801))
        assertTrue(state.isNearLimit)
    }

    @Test
    fun `isNearLimit returns false at exactly warning threshold`() {
        val state = PromptEditorUiState(currentPrompt = "a".repeat(800))
        assertFalse(state.isNearLimit)
    }

    // ========== hasUnsavedChanges测试 ==========

    @Test
    fun `hasUnsavedChanges returns false when prompts are equal`() {
        val state = PromptEditorUiState(
            originalPrompt = "test",
            currentPrompt = "test"
        )
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges returns true when prompts differ`() {
        val state = PromptEditorUiState(
            originalPrompt = "original",
            currentPrompt = "modified"
        )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges returns true when original is empty and current is not`() {
        val state = PromptEditorUiState(
            originalPrompt = "",
            currentPrompt = "new content"
        )
        assertTrue(state.hasUnsavedChanges)
    }

    // ========== canSave测试 ==========

    @Test
    fun `canSave returns true when all conditions met`() {
        val state = PromptEditorUiState(
            currentPrompt = "valid prompt",
            isLoading = false,
            isSaving = false
        )
        assertTrue(state.canSave)
    }

    @Test
    fun `canSave returns false when over limit`() {
        val state = PromptEditorUiState(
            currentPrompt = "a".repeat(1001),
            isLoading = false,
            isSaving = false
        )
        assertFalse(state.canSave)
    }

    @Test
    fun `canSave returns false when loading`() {
        val state = PromptEditorUiState(
            currentPrompt = "valid",
            isLoading = true,
            isSaving = false
        )
        assertFalse(state.canSave)
    }

    @Test
    fun `canSave returns false when saving`() {
        val state = PromptEditorUiState(
            currentPrompt = "valid",
            isLoading = false,
            isSaving = true
        )
        assertFalse(state.canSave)
    }

    // ========== getTitleResId测试 ==========

    @Test
    fun `getTitleResId returns global title for GlobalScene mode`() {
        val state = PromptEditorUiState(
            editMode = PromptEditMode.GlobalScene(PromptScene.ANALYZE)
        )
        assertEquals(R.string.prompt_editor_title_global, state.getTitleResId())
    }

    @Test
    fun `getTitleResId returns contact title for ContactCustom mode`() {
        val state = PromptEditorUiState(
            editMode = PromptEditMode.ContactCustom("1", "小明")
        )
        assertEquals(R.string.prompt_editor_title_contact, state.getTitleResId())
    }

    // ========== getContactName测试 ==========

    @Test
    fun `getContactName returns null for GlobalScene mode`() {
        val state = PromptEditorUiState(
            editMode = PromptEditMode.GlobalScene(PromptScene.ANALYZE)
        )
        assertNull(state.getContactName())
    }

    @Test
    fun `getContactName returns contact name for ContactCustom mode`() {
        val state = PromptEditorUiState(
            editMode = PromptEditMode.ContactCustom("1", "小明")
        )
        assertEquals("小明", state.getContactName())
    }

    // ========== 常量测试 ==========

    @Test
    fun `MAX_PROMPT_LENGTH is 1000`() {
        assertEquals(1000, PromptEditorUiState.MAX_PROMPT_LENGTH)
    }

    @Test
    fun `WARN_PROMPT_LENGTH is 800`() {
        assertEquals(800, PromptEditorUiState.WARN_PROMPT_LENGTH)
    }
}
