package com.empathy.ai.data.local

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class FloatingWindowPreferencesScreenshotPermissionTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var floatingWindowPreferences: FloatingWindowPreferences

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit

        val moshi = mockk<Moshi>(relaxed = true)
        floatingWindowPreferences = FloatingWindowPreferences(context, moshi)
    }

    @Test
    fun `saveMediaProjectionPermission should cache in memory`() {
        val intent = Intent(Intent.ACTION_VIEW)

        floatingWindowPreferences.saveMediaProjectionPermission(Activity.RESULT_OK, intent)

        assertNotNull(floatingWindowPreferences.getMediaProjectionPermission())
    }

    @Test
    fun `saveMediaProjectionPermission persistence behavior should respect sdk level`() {
        val intent = Intent(Intent.ACTION_VIEW)

        floatingWindowPreferences.saveMediaProjectionPermission(Activity.RESULT_OK, intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            verify(exactly = 0) { editor.putString("media_projection_result_data", any()) }
            verify(exactly = 0) { editor.putInt("media_projection_result_code", any()) }
        } else {
            verify { editor.putString("media_projection_result_data", any()) }
            verify { editor.putInt("media_projection_result_code", any()) }
        }
    }
}
