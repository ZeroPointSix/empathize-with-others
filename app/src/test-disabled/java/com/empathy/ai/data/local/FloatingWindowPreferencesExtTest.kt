package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FloatingWindowPreferences 扩展功能单元测试
 *
 * 测试TD-00009阶段2新增的状态管理功能：
 * - Tab记忆保存/恢复
 * - 联系人ID保存/恢复
 * - 完整状态保存/恢复/清除
 * - hasSavedUiState()正确反映状态
 */
class FloatingWindowPreferencesExtTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var moshi: Moshi
    private lateinit var preferences: FloatingWindowPreferences

    // 存储模拟的SharedPreferences数据
    private val prefsData = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        moshi = Moshi.Builder().build()

        // 模拟SharedPreferences行为
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        // 模拟editor的链式调用
        every { editor.putString(any(), any()) } answers {
            prefsData[firstArg()] = secondArg()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            prefsData[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            prefsData.remove(firstArg())
            editor
        }
        every { editor.apply() } answers { }

        // 模拟读取操作
        every { sharedPreferences.getString(any(), any()) } answers {
            prefsData[firstArg()] as? String ?: secondArg()
        }
        every { sharedPreferences.getBoolean(any(), any()) } answers {
            prefsData[firstArg()] as? Boolean ?: secondArg()
        }

        preferences = FloatingWindowPreferences(context, moshi)
    }

    // ==================== Tab记忆测试 ====================

    @Test
    fun `saveSelectedTab should save tab name to preferences`() {
        // When
        preferences.saveSelectedTab("POLISH")

        // Then
        assertEquals("POLISH", prefsData["selected_tab"])
    }

    @Test
    fun `getSelectedTab should return saved tab name`() {
        // Given
        prefsData["selected_tab"] = "REPLY"

        // When
        val result = preferences.getSelectedTab()

        // Then
        assertEquals("REPLY", result)
    }

    @Test
    fun `getSelectedTab should return ANALYZE as default`() {
        // Given - no saved tab

        // When
        val result = preferences.getSelectedTab()

        // Then
        assertEquals("ANALYZE", result)
    }

    @Test
    fun `tab memory should persist across multiple saves`() {
        // Given
        preferences.saveSelectedTab("ANALYZE")
        preferences.saveSelectedTab("POLISH")
        preferences.saveSelectedTab("REPLY")

        // Then
        assertEquals("REPLY", prefsData["selected_tab"])
    }

    // ==================== 联系人ID记忆测试 ====================

    @Test
    fun `saveLastContactId should save contact id to preferences`() {
        // When
        preferences.saveLastContactId("contact_123")

        // Then
        assertEquals("contact_123", prefsData["last_contact_id"])
    }

    @Test
    fun `getLastContactId should return saved contact id`() {
        // Given
        prefsData["last_contact_id"] = "contact_456"

        // When
        val result = preferences.getLastContactId()

        // Then
        assertEquals("contact_456", result)
    }

    @Test
    fun `getLastContactId should return null when not saved`() {
        // Given - no saved contact id

        // When
        val result = preferences.getLastContactId()

        // Then
        assertNull(result)
    }

    @Test
    fun `contact id memory should persist across multiple saves`() {
        // Given
        preferences.saveLastContactId("contact_1")
        preferences.saveLastContactId("contact_2")
        preferences.saveLastContactId("contact_3")

        // Then
        assertEquals("contact_3", prefsData["last_contact_id"])
    }

    // ==================== 输入内容保存测试 ====================

    @Test
    fun `saveInputText should save input text to preferences`() {
        // When
        preferences.saveInputText("测试输入内容")

        // Then
        assertEquals("测试输入内容", prefsData["saved_input_text"])
    }

    @Test
    fun `getInputText should return saved input text`() {
        // Given
        prefsData["saved_input_text"] = "保存的内容"

        // When
        val result = preferences.getInputText()

        // Then
        assertEquals("保存的内容", result)
    }

    @Test
    fun `getInputText should return empty string when not saved`() {
        // Given - no saved input text

        // When
        val result = preferences.getInputText()

        // Then
        assertEquals("", result)
    }

    // ==================== 完整UI状态保存/恢复测试 ====================

    @Test
    fun `saveUiState should save all state fields`() {
        // When
        preferences.saveUiState("POLISH", "contact_123", "测试内容")

        // Then
        assertEquals("POLISH", prefsData["selected_tab"])
        assertEquals("contact_123", prefsData["last_contact_id"])
        assertEquals("测试内容", prefsData["saved_input_text"])
        assertEquals(true, prefsData["has_saved_state"])
    }

    @Test
    fun `saveUiState should handle null contact id`() {
        // When
        preferences.saveUiState("REPLY", null, "内容")

        // Then
        assertEquals("REPLY", prefsData["selected_tab"])
        assertFalse(prefsData.containsKey("last_contact_id"))
        assertEquals("内容", prefsData["saved_input_text"])
        assertEquals(true, prefsData["has_saved_state"])
    }

    @Test
    fun `restoreUiState should return all saved state fields`() {
        // Given
        prefsData["selected_tab"] = "REPLY"
        prefsData["last_contact_id"] = "contact_789"
        prefsData["saved_input_text"] = "恢复的内容"

        // When
        val (tab, contactId, inputText) = preferences.restoreUiState()

        // Then
        assertEquals("REPLY", tab)
        assertEquals("contact_789", contactId)
        assertEquals("恢复的内容", inputText)
    }

    @Test
    fun `restoreUiState should return defaults when nothing saved`() {
        // Given - no saved state

        // When
        val (tab, contactId, inputText) = preferences.restoreUiState()

        // Then
        assertEquals("ANALYZE", tab)
        assertNull(contactId)
        assertEquals("", inputText)
    }

    // ==================== 状态清除测试 ====================

    @Test
    fun `clearSavedUiState should remove input text and set flag to false`() {
        // Given
        prefsData["saved_input_text"] = "要清除的内容"
        prefsData["has_saved_state"] = true

        // When
        preferences.clearSavedUiState()

        // Then
        assertFalse(prefsData.containsKey("saved_input_text"))
        assertEquals(false, prefsData["has_saved_state"])
    }

    @Test
    fun `clearSavedUiState should preserve tab and contact id memory`() {
        // Given
        prefsData["selected_tab"] = "POLISH"
        prefsData["last_contact_id"] = "contact_123"
        prefsData["saved_input_text"] = "要清除的内容"
        prefsData["has_saved_state"] = true

        // When
        preferences.clearSavedUiState()

        // Then
        assertEquals("POLISH", prefsData["selected_tab"])
        assertEquals("contact_123", prefsData["last_contact_id"])
    }

    // ==================== hasSavedUiState测试 ====================

    @Test
    fun `hasSavedUiState should return true when state is saved`() {
        // Given
        prefsData["has_saved_state"] = true

        // When
        val result = preferences.hasSavedUiState()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasSavedUiState should return false when state is not saved`() {
        // Given
        prefsData["has_saved_state"] = false

        // When
        val result = preferences.hasSavedUiState()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasSavedUiState should return false by default`() {
        // Given - no saved state flag

        // When
        val result = preferences.hasSavedUiState()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasSavedUiState should reflect state after saveUiState`() {
        // Given
        preferences.saveUiState("ANALYZE", "contact_1", "内容")

        // Then
        assertEquals(true, prefsData["has_saved_state"])
    }

    @Test
    fun `hasSavedUiState should reflect state after clearSavedUiState`() {
        // Given
        prefsData["has_saved_state"] = true

        // When
        preferences.clearSavedUiState()

        // Then
        assertEquals(false, prefsData["has_saved_state"])
    }

    // ==================== 完整流程测试 ====================

    @Test
    fun `minimize and restore flow should preserve state`() {
        // Given - 用户在悬浮窗中操作
        preferences.saveSelectedTab("POLISH")
        preferences.saveLastContactId("contact_abc")

        // When - 最小化时保存完整状态
        preferences.saveUiState("POLISH", "contact_abc", "正在编辑的内容")

        // Then - 恢复时应该能获取完整状态
        val (tab, contactId, inputText) = preferences.restoreUiState()
        assertEquals("POLISH", tab)
        assertEquals("contact_abc", contactId)
        assertEquals("正在编辑的内容", inputText)
        assertEquals(true, prefsData["has_saved_state"])
    }

    @Test
    fun `close flow should clear input but keep memory`() {
        // Given - 有保存的状态
        preferences.saveUiState("REPLY", "contact_xyz", "要清除的内容")

        // When - 关闭悬浮窗
        preferences.clearSavedUiState()

        // Then - 输入内容被清除，但Tab和联系人记忆保留
        assertEquals("REPLY", prefsData["selected_tab"])
        assertEquals("contact_xyz", prefsData["last_contact_id"])
        assertFalse(prefsData.containsKey("saved_input_text"))
        assertEquals(false, prefsData["has_saved_state"])
    }

    @Test
    fun `reopen after close should use memory but not input`() {
        // Given - 关闭后重新打开
        prefsData["selected_tab"] = "POLISH"
        prefsData["last_contact_id"] = "contact_memory"
        prefsData["has_saved_state"] = false
        // 没有saved_input_text

        // When
        val (tab, contactId, inputText) = preferences.restoreUiState()

        // Then
        assertEquals("POLISH", tab)
        assertEquals("contact_memory", contactId)
        assertEquals("", inputText)
        assertFalse(preferences.hasSavedUiState())
    }
}
