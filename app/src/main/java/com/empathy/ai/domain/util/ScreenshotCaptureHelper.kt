package com.empathy.ai.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.empathy.ai.domain.model.ScreenshotAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 区域截图捕获与本地临时落盘。
 *
 * 业务背景 (PRD-00036/2.1):
 * - 用户希望把第三方 App 里的图片/视频画面“截一块”快速塞进悬浮窗输入框，作为对话上下文。
 *
 * 设计决策 (TDD-00036):
 * - MediaProjection 只能拿到“整屏图像”，因此先整屏抓取，再按 `region` 裁剪。
 * - 附件只存放在 `cacheDir/screenshots`，用于“发送前临时附件”；由上层在发送后清理 (PRD-00036/3.6)。
 *
 * 性能与带宽 (TDD-00036/1.4):
 * - 限制最大边长为 `MAX_SIDE_PX`，并将 JPEG 体积控制在 `MAX_FILE_BYTES` 以内，避免插入多张截图时爆内存/超请求体。
 */
class ScreenshotCaptureHelper(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    suspend fun captureRegion(
        mediaProjection: MediaProjection,
        region: Rect
    ): ScreenshotAttachment? = withContext(Dispatchers.IO) {
        val metrics = getScreenMetrics()
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        if (screenWidth <= 0 || screenHeight <= 0) return@withContext null

        val imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )
        var virtualDisplay: VirtualDisplay? = null
        try {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "screenshot",
                screenWidth,
                screenHeight,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                null
            )

            val bitmap = acquireLatestBitmap(imageReader, screenWidth, screenHeight) ?: return@withContext null
            val safeRect = clampRect(region, screenWidth, screenHeight) ?: return@withContext null
            val cropped = Bitmap.createBitmap(
                bitmap,
                safeRect.left,
                safeRect.top,
                safeRect.width(),
                safeRect.height()
            )
            bitmap.recycle()

            val scaled = scaleIfNeeded(cropped)
            if (scaled !== cropped) {
                cropped.recycle()
            }

            return@withContext saveToCache(scaled)
        } catch (e: Exception) {
            null
        } finally {
            try {
                virtualDisplay?.release()
            } catch (_: Exception) {
            }
            try {
                imageReader.close()
            } catch (_: Exception) {
            }
        }
    }

    fun deleteAttachment(attachment: ScreenshotAttachment) {
        val file = File(attachment.localPath)
        if (file.exists()) {
            file.delete()
        }
    }

    private suspend fun acquireLatestBitmap(
        reader: ImageReader,
        width: Int,
        height: Int
    ): Bitmap? {
        // MediaProjection 输出到 ImageReader 的首帧可能为空；短暂轮询可提升稳定性（不同设备/模拟器上差异明显）。
        repeat(6) {
            delay(50)
            val image = reader.acquireLatestImage() ?: return@repeat
            val bitmap = imageToBitmap(image, width, height)
            image.close()
            if (bitmap != null) return bitmap
        }
        return null
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()
            cropped
        } catch (e: Exception) {
            null
        }
    }

    private fun clampRect(rect: Rect, maxWidth: Int, maxHeight: Int): Rect? {
        val left = rect.left.coerceIn(0, maxWidth - 1)
        val top = rect.top.coerceIn(0, maxHeight - 1)
        val right = rect.right.coerceIn(1, maxWidth)
        val bottom = rect.bottom.coerceIn(1, maxHeight)
        if (right <= left || bottom <= top) return null
        return Rect(left, top, right, bottom)
    }

    private fun scaleIfNeeded(bitmap: Bitmap): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= MAX_SIDE_PX) return bitmap
        val scale = MAX_SIDE_PX / maxSide.toFloat()
        val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun saveToCache(bitmap: Bitmap): ScreenshotAttachment? {
        val cacheDir = File(context.cacheDir, "screenshots")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = File(cacheDir, "shot_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        val outputStream = ByteArrayOutputStream()
        var quality = 85
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        if (outputStream.size() > MAX_FILE_BYTES) {
            outputStream.reset()
            quality = 70
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        file.writeBytes(outputStream.toByteArray())
        outputStream.close()
        val attachment = ScreenshotAttachment(
            id = UUID.randomUUID().toString(),
            localPath = file.absolutePath,
            width = bitmap.width,
            height = bitmap.height,
            sizeBytes = file.length(),
            createdAt = System.currentTimeMillis()
        )
        bitmap.recycle()
        return attachment
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = context.resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        return metrics
    }

    companion object {
        private const val MAX_SIDE_PX = 1280
        private const val MAX_FILE_BYTES = 1_000_000
    }
}
