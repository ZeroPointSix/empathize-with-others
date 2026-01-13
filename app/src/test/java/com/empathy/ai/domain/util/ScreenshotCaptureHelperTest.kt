package com.empathy.ai.domain.util

import android.content.Context
import android.view.WindowManager
import com.empathy.ai.domain.model.ScreenshotAttachment
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ScreenshotCaptureHelperTest {

    private lateinit var context: Context
    private lateinit var windowManager: WindowManager
    private lateinit var helper: ScreenshotCaptureHelper

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        windowManager = mockk(relaxed = true)
        every { context.getSystemService(Context.WINDOW_SERVICE) } returns windowManager
        helper = ScreenshotCaptureHelper(context)
    }

    @Test
    fun `deleteAttachment should remove existing file`() {
        val file = File.createTempFile("shot_", ".jpg")
        file.writeBytes(ByteArray(10))
        val attachment = ScreenshotAttachment(
            id = "id",
            localPath = file.absolutePath,
            width = 10,
            height = 10,
            sizeBytes = file.length(),
            createdAt = System.currentTimeMillis()
        )

        assertTrue(file.exists())
        helper.deleteAttachment(attachment)
        assertFalse(file.exists())
    }

    @Test
    fun `deleteAttachment should ignore missing file`() {
        val file = File.createTempFile("shot_", ".jpg")
        val path = file.absolutePath
        file.delete()
        val attachment = ScreenshotAttachment(
            id = "id",
            localPath = path,
            width = 10,
            height = 10,
            sizeBytes = 0,
            createdAt = 0
        )

        helper.deleteAttachment(attachment)
        assertFalse(File(path).exists())
    }
}
