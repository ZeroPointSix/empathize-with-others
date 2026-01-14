package com.empathy.ai.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.empathy.ai.domain.service.FloatingWindowService

/**
 * MediaProjection 授权中转页。
 *
 * 背景 (PRD-00036/3.1, TDD-00036):
 * - MediaProjection 的授权弹窗必须由 Activity 发起；而截图入口位于悬浮窗 Service 内。
 * - 因此通过一个“透明中转 Activity”触发系统授权，再把结果回传给 `FloatingWindowService` 完成后续截图链路。
 */
class ScreenshotPermissionActivity : Activity() {
    private val projectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            val resultIntent = Intent(this, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_MEDIA_PROJECTION_RESULT
                putExtra(FloatingWindowService.EXTRA_RESULT_CODE, resultCode)
                putExtra(FloatingWindowService.EXTRA_RESULT_DATA, data)
            }
            startService(resultIntent)
            finish()
        }
    }

    companion object {
        private const val REQUEST_CODE = 2001
    }
}
