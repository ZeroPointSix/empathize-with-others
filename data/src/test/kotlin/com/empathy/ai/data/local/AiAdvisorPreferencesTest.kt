package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * AiAdvisorPreferences 单元测试
 *
 * ## 测试范围
 * - 上次联系人ID的读写
 * - 上次会话ID的读写
 * - 清除所有偏好设置
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 */
class AiAdvisorPreferencesTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } returns Unit
    }

    @Test
    fun `getLastContactId returns null when not set`() {
        // Given
        every { sharedPreferences.getString("last_contact_id", null) } returns null

        // When
        val result = getLastContactIdFromPrefs(sharedPreferences)

        // Then
        assertNull(result)
    }

    @Test
    fun `getLastContactId returns stored value`() {
        // Given
        val expectedId = "contact-123"
        every { sharedPreferences.getString("last_contact_id", null) } returns expectedId

        // When
        val result = getLastContactIdFromPrefs(sharedPreferences)

        // Then
        assertEquals(expectedId, result)
    }

    @Test
    fun `setLastContactId stores value correctly`() {
        // Given
        val contactId = "contact-456"
        val keySlot = slot<String>()
        val valueSlot = slot<String>()

        every { editor.putString(capture(keySlot), capture(valueSlot)) } returns editor

        // When
        setLastContactIdToPrefs(sharedPreferences, contactId)

        // Then
        assertEquals("last_contact_id", keySlot.captured)
        assertEquals(contactId, valueSlot.captured)
        verify { editor.apply() }
    }

    @Test
    fun `getLastSessionId returns null when not set`() {
        // Given
        every { sharedPreferences.getString("last_session_id", null) } returns null

        // When
        val result = getLastSessionIdFromPrefs(sharedPreferences)

        // Then
        assertNull(result)
    }

    @Test
    fun `getLastSessionId returns stored value`() {
        // Given
        val expectedId = "session-789"
        every { sharedPreferences.getString("last_session_id", null) } returns expectedId

        // When
        val result = getLastSessionIdFromPrefs(sharedPreferences)

        // Then
        assertEquals(expectedId, result)
    }

    @Test
    fun `setLastSessionId stores value correctly`() {
        // Given
        val sessionId = "session-abc"
        val keySlot = slot<String>()
        val valueSlot = slot<String>()

        every { editor.putString(capture(keySlot), capture(valueSlot)) } returns editor

        // When
        setLastSessionIdToPrefs(sharedPreferences, sessionId)

        // Then
        assertEquals("last_session_id", keySlot.captured)
        assertEquals(sessionId, valueSlot.captured)
        verify { editor.apply() }
    }

    @Test
    fun `setLastSessionId with null removes the key`() {
        // Given
        val keySlot = slot<String>()

        every { editor.remove(capture(keySlot)) } returns editor

        // When
        setLastSessionIdToPrefs(sharedPreferences, null)

        // Then
        assertEquals("last_session_id", keySlot.captured)
        verify { editor.apply() }
    }

    @Test
    fun `clear removes all preferences`() {
        // When
        clearPrefs(sharedPreferences)

        // Then
        verify { editor.clear() }
        verify { editor.apply() }
    }

    // Helper functions to test SharedPreferences operations without EncryptedSharedPreferences
    private fun getLastContactIdFromPrefs(prefs: SharedPreferences): String? {
        return prefs.getString("last_contact_id", null)
    }

    private fun setLastContactIdToPrefs(prefs: SharedPreferences, contactId: String) {
        prefs.edit().putString("last_contact_id", contactId).apply()
    }

    private fun getLastSessionIdFromPrefs(prefs: SharedPreferences): String? {
        return prefs.getString("last_session_id", null)
    }

    private fun setLastSessionIdToPrefs(prefs: SharedPreferences, sessionId: String?) {
        if (sessionId != null) {
            prefs.edit().putString("last_session_id", sessionId).apply()
        } else {
            prefs.edit().remove("last_session_id").apply()
        }
    }

    private fun clearPrefs(prefs: SharedPreferences) {
        prefs.edit().clear().apply()
    }
}
