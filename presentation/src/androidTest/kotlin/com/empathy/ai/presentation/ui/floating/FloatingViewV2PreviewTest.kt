package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.empathy.ai.domain.model.ScreenshotAttachment
import com.empathy.ai.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class FloatingViewV2PreviewTest {

    @Test
    fun screenshotThumbnail_clickInvokesPreviewCallback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val baseContext = instrumentation.targetContext
        val context = ContextThemeWrapper(
            baseContext,
            com.google.android.material.R.style.Theme_Material3_Light_NoActionBar
        )
        val windowManager = baseContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val imageFile = createTempImage(context)
        val attachment = createAttachment(imageFile)
        var capturedPath: String? = null
        var imageView: ImageView? = null

        instrumentation.runOnMainSync {
            val floatingView = FloatingViewV2(context, windowManager)
            floatingView.setOnScreenshotPreviewListener { capturedPath = it }
            floatingView.setScreenshotAttachments(listOf(attachment))
            val container = getAttachmentContainer(floatingView)
            imageView = findFirstImageView(container)
            imageView?.performClick()
        }

        assertNotNull("Thumbnail ImageView should exist", imageView)
        assertEquals(imageFile.absolutePath, capturedPath)
        imageFile.delete()
    }

    @Test
    fun screenshotDelete_clickDoesNotInvokePreviewCallback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val baseContext = instrumentation.targetContext
        val context = ContextThemeWrapper(
            baseContext,
            com.google.android.material.R.style.Theme_Material3_Light_NoActionBar
        )
        val windowManager = baseContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val imageFile = createTempImage(context)
        val attachment = createAttachment(imageFile)
        var capturedPath: String? = null
        var deleteView: View? = null

        instrumentation.runOnMainSync {
            val floatingView = FloatingViewV2(context, windowManager)
            floatingView.setOnScreenshotPreviewListener { capturedPath = it }
            floatingView.setScreenshotAttachments(listOf(attachment))
            val container = getAttachmentContainer(floatingView)
            val description = context.getString(R.string.cd_screenshot_delete)
            deleteView = findFirstViewWithContentDescription(container, description)
            deleteView?.performClick()
        }

        assertNotNull("Delete view should exist", deleteView)
        assertNull(capturedPath)
        imageFile.delete()
    }

    private fun createTempImage(context: Context): File {
        val file = File(context.cacheDir, "preview_test_${System.currentTimeMillis()}.png")
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file
    }

    private fun createAttachment(file: File): ScreenshotAttachment {
        return ScreenshotAttachment(
            id = "test-${System.currentTimeMillis()}",
            localPath = file.absolutePath,
            width = 10,
            height = 10,
            sizeBytes = file.length(),
            createdAt = System.currentTimeMillis()
        )
    }

    private fun getAttachmentContainer(view: FloatingViewV2): LinearLayout {
        val field = FloatingViewV2::class.java.getDeclaredField("attachmentContainer")
        field.isAccessible = true
        return field.get(view) as LinearLayout
    }

    private fun findFirstImageView(root: View): ImageView? {
        if (root is ImageView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val found = findFirstImageView(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findFirstViewWithContentDescription(root: View, description: CharSequence): View? {
        if (description == root.contentDescription) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val found = findFirstViewWithContentDescription(child, description)
                if (found != null) return found
            }
        }
        return null
    }
}
