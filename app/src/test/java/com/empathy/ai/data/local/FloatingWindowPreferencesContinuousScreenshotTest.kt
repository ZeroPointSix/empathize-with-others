package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FloatingWindowPreferencesContinuousScreenshotTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var preferences: FloatingWindowPreferences

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        val moshi = Moshi.Builder().build()
        preferences = FloatingWindowPreferences(context, moshi)
    }

    @Test
    fun `saveContinuousScreenshotEnabled should persist value`() {
        preferences.saveContinuousScreenshotEnabled(true)

        verify { editor.putBoolean("continuous_screenshot_enabled", true) }
        verify { editor.apply() }
    }

    @Test
    fun `isContinuousScreenshotEnabled should return saved value`() {
        every { sharedPreferences.getBoolean("continuous_screenshot_enabled", false) } returns true

        val result = preferences.isContinuousScreenshotEnabled()

        assertTrue(result)
    }

    @Test
    fun `isContinuousScreenshotEnabled should default to false`() {
        every { sharedPreferences.getBoolean("continuous_screenshot_enabled", false) } returns false

        val result = preferences.isContinuousScreenshotEnabled()

        assertFalse(result)
    }
}
