package com.empathy.ai.domain.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.service.FloatingWindowService
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingWindowServiceScreenshotTest {

    @Test
    fun startService_withCanceledProjectionResult_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_MEDIA_PROJECTION_RESULT
            putExtra(FloatingWindowService.EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            putExtra(FloatingWindowService.EXTRA_RESULT_DATA, null as Intent?)
        }

        val component = context.startService(intent)

        assertNotNull(component)
        context.stopService(Intent(context, FloatingWindowService::class.java))
    }
}
