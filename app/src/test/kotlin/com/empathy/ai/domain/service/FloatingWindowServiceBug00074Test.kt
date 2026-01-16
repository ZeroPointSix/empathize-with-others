package com.empathy.ai.domain.service

import com.empathy.ai.data.local.FloatingWindowPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingWindowServiceBug00074Test {

    @Test
    fun `regenerate_displayModeBubble_shouldExpand`() {
        val shouldExpand = FloatingWindowService.shouldExpandForRegenerate(
            FloatingWindowPreferences.DISPLAY_MODE_BUBBLE,
            true
        )

        assertTrue(shouldExpand)
    }

    @Test
    fun `regenerate_displayModeDialog_viewNotVisible_shouldExpand`() {
        val shouldExpand = FloatingWindowService.shouldExpandForRegenerate(
            FloatingWindowPreferences.DISPLAY_MODE_DIALOG,
            false
        )

        assertTrue(shouldExpand)
    }

    @Test
    fun `regenerate_displayModeDialog_viewVisible_shouldNotExpand`() {
        val shouldExpand = FloatingWindowService.shouldExpandForRegenerate(
            FloatingWindowPreferences.DISPLAY_MODE_DIALOG,
            true
        )

        assertFalse(shouldExpand)
    }

    @Test
    fun `regenerate_displayModeDialog_viewUnknown_shouldExpand`() {
        val shouldExpand = FloatingWindowService.shouldExpandForRegenerate(
            FloatingWindowPreferences.DISPLAY_MODE_DIALOG,
            null
        )

        assertTrue(shouldExpand)
    }
}
