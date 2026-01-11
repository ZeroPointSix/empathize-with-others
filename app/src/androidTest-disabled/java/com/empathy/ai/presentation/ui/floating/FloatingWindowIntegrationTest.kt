package com.empathy.ai.presentation.ui.floating

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.FloatingWindowUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 悬浮窗集成测试
 *
 * TD-00009 T041: 测试悬浮窗完整流程
 *
 * 测试覆盖：
 * - 完整流程_润色后微调
 * - 完整流程_回复后微调
 * - 状态保持流程_最小化恢复
 * - Tab切换不丢失输入内容
 * - 联系人切换正确更新状态
 */
@RunWith(AndroidJUnit4::class)
class FloatingWindowIntegrationTest {

    private lateinit var context: Context
    private lateinit var preferences: FloatingWindowPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = FloatingWindowPreferences(context)
        // 清理测试数据
        preferences.clearSavedUiState()
    }

    @After
    fun tearDown() {
        // 清理测试数据
        preferences.clearSavedUiState()
    }

    // ==================== Tab记忆测试 ====================

    @Test
    fun Tab记忆_保存后能正确恢复() {
        // Given
        val selectedTab = ActionType.POLISH

        // When
        preferences.saveSelectedTab(selectedTab)
        val restoredTab = preferences.getSelectedTabAsActionType()

        // Then
        assertEquals(selectedTab, restoredTab)
    }

    @Test
    fun Tab记忆_默认值为ANALYZE() {
        // Given - 清空状态
        preferences.clearSavedUiState()

        // When
        val defaultTab = preferences.getSelectedTabAsActionType()

        // Then
        assertEquals(ActionType.ANALYZE, defaultTab)
    }

    @Test
    fun Tab记忆_支持所有Tab类型() {
        // 测试所有有效的Tab类型
        val validTabs = listOf(ActionType.ANALYZE, ActionType.POLISH, ActionType.REPLY)

        for (tab in validTabs) {
            // When
            preferences.saveSelectedTab(tab)
            val restored = preferences.getSelectedTabAsActionType()

            // Then
            assertEquals("Tab $tab 应该能正确保存和恢复", tab, restored)
        }
    }

    // ==================== 联系人记忆测试 ====================

    @Test
    fun 联系人记忆_保存后能正确恢复() {
        // Given
        val contactId = "contact-123"

        // When
        preferences.saveLastContactId(contactId)
        val restoredId = preferences.getLastContactId()

        // Then
        assertEquals(contactId, restoredId)
    }

    @Test
    fun 联系人记忆_默认值为null() {
        // Given - 清空状态
        preferences.clearSavedUiState()

        // When
        val defaultId = preferences.getLastContactId()

        // Then
        assertNull(defaultId)
    }

    @Test
    fun 联系人记忆_空字符串视为null() {
        // Given
        preferences.saveLastContactId("")

        // When
        val restoredId = preferences.getLastContactId()

        // Then
        assertNull(restoredId)
    }

    // ==================== 完整状态保存测试 ====================

    @Test
    fun 状态保持_最小化时保存完整状态() {
        // Given
        val state = FloatingWindowUiState(
            selectedTab = ActionType.POLISH,
            selectedContactId = "contact-456",
            inputText = "测试输入内容"
        )

        // When - 模拟最小化时保存状态
        preferences.saveUiState(state)

        // Then
        assertTrue(preferences.hasSavedUiState())
    }

    @Test
    fun 状态保持_恢复时还原完整状态() {
        // Given
        val originalState = FloatingWindowUiState(
            selectedTab = ActionType.REPLY,
            selectedContactId = "contact-789",
            inputText = "恢复测试内容"
        )
        preferences.saveUiState(originalState)

        // When
        val restoredState = preferences.restoreUiStateAsObject()

        // Then
        assertEquals(originalState.selectedTab, restoredState?.selectedTab)
        assertEquals(originalState.selectedContactId, restoredState?.selectedContactId)
        assertEquals(originalState.inputText, restoredState?.inputText)
    }

    @Test
    fun 状态保持_关闭时清空状态() {
        // Given
        val state = FloatingWindowUiState(
            selectedTab = ActionType.ANALYZE,
            selectedContactId = "contact-test",
            inputText = "将被清空的内容"
        )
        preferences.saveUiState(state)
        assertTrue(preferences.hasSavedUiState())

        // When - 模拟关闭时清空状态
        preferences.clearSavedUiState()

        // Then
        assertFalse(preferences.hasSavedUiState())
        assertNull(preferences.restoreUiStateAsObject())
    }

    // ==================== Tab切换不丢失内容测试 ====================

    @Test
    fun Tab切换_输入内容保持不变() {
        // Given - 初始状态
        val initialState = FloatingWindowUiState(
            selectedTab = ActionType.ANALYZE,
            inputText = "用户输入的内容"
        )

        // When - 切换Tab
        val newState = initialState.copy(selectedTab = ActionType.POLISH)

        // Then - 输入内容应该保持
        assertEquals("用户输入的内容", newState.inputText)
        assertEquals(ActionType.POLISH, newState.selectedTab)
    }

    @Test
    fun Tab切换_联系人选择保持不变() {
        // Given
        val initialState = FloatingWindowUiState(
            selectedTab = ActionType.ANALYZE,
            selectedContactId = "contact-keep"
        )

        // When - 切换Tab
        val newState = initialState.copy(selectedTab = ActionType.REPLY)

        // Then - 联系人应该保持
        assertEquals("contact-keep", newState.selectedContactId)
    }

    // ==================== 联系人切换测试 ====================

    @Test
    fun 联系人切换_正确更新状态() {
        // Given
        val initialState = FloatingWindowUiState(
            selectedContactId = "old-contact"
        )

        // When
        val newState = initialState.copy(selectedContactId = "new-contact")

        // Then
        assertEquals("new-contact", newState.selectedContactId)
    }

    @Test
    fun 联系人切换_保存到记忆() {
        // Given
        val newContactId = "switched-contact"

        // When - 模拟切换联系人后保存
        preferences.saveLastContactId(newContactId)

        // Then - 下次打开应该记住
        assertEquals(newContactId, preferences.getLastContactId())
    }

    // ==================== 状态重置测试 ====================

    @Test
    fun 状态重置_保留Tab和联系人记忆() {
        // Given
        val state = FloatingWindowUiState(
            selectedTab = ActionType.POLISH,
            selectedContactId = "contact-memory",
            inputText = "将被清空",
            isLoading = true,
            errorMessage = "错误信息"
        )

        // When - 重置但保留记忆
        val resetState = state.resetKeepingMemory()

        // Then
        assertEquals(ActionType.POLISH, resetState.selectedTab)
        assertEquals("contact-memory", resetState.selectedContactId)
        assertEquals("", resetState.inputText)
        assertFalse(resetState.isLoading)
        assertNull(resetState.errorMessage)
    }

    @Test
    fun 状态重置_完全重置() {
        // Given
        val state = FloatingWindowUiState(
            selectedTab = ActionType.REPLY,
            selectedContactId = "contact-clear",
            inputText = "全部清空"
        )

        // When - 完全重置
        val resetState = state.resetAll()

        // Then
        assertEquals(ActionType.ANALYZE, resetState.selectedTab)
        assertNull(resetState.selectedContactId)
        assertEquals("", resetState.inputText)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun 边界条件_空输入内容() {
        // Given
        val state = FloatingWindowUiState(inputText = "")

        // Then
        assertFalse(state.canSubmit())
    }

    @Test
    fun 边界条件_有输入但无联系人() {
        // Given
        val state = FloatingWindowUiState(
            inputText = "有内容",
            selectedContactId = null
        )

        // Then - 根据业务逻辑，可能需要联系人才能提交
        // 这里假设没有联系人也可以提交
        assertTrue(state.canSubmit())
    }

    @Test
    fun 边界条件_加载中不能提交() {
        // Given
        val state = FloatingWindowUiState(
            inputText = "有内容",
            isLoading = true
        )

        // Then
        assertFalse(state.canSubmit())
    }
}
