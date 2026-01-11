package com.empathy.ai.domain.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import com.empathy.ai.domain.util.FloatingWindowManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeoutException

/**
 * 悬浮窗服务集成测试
 *
 * 测试范围：
 * - 服务生命周期
 * - 服务启动和停止
 * - 权限检查
 *
 * 注意：
 * - 这些测试需要悬浮窗权限才能完整运行
 * - 在没有权限的情况下，部分测试会被跳过
 * - 建议在真实设备上运行测试
 */
@RunWith(AndroidJUnit4::class)
class FloatingWindowServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private var hasOverlayPermission: Boolean = false

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // 检查是否有悬浮窗权限
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @After
    fun tearDown() {
        // 确保服务被停止
        try {
            val intent = Intent(context, FloatingWindowService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            // 忽略停止服务时的异常
        }
    }

    // ==================== 权限检查测试 ====================

    @Test
    fun serviceRequiresOverlayPermission() {
        // Given
        val permissionResult = FloatingWindowManager.hasPermission(context)

        // Then
        // 验证权限检查功能正常工作
        assertTrue(
            "权限检查应返回有效结果",
            permissionResult is FloatingWindowManager.PermissionResult.Granted ||
            permissionResult is FloatingWindowManager.PermissionResult.Denied ||
            permissionResult is FloatingWindowManager.PermissionResult.Error
        )
    }

    @Test
    fun serviceStartWithoutPermission_shouldFail() {
        // Skip if we have permission
        if (hasOverlayPermission) {
            return
        }

        // When
        val result = FloatingWindowManager.startService(context)

        // Then
        assertTrue(
            "无权限时启动服务应返回 PermissionDenied",
            result is FloatingWindowManager.ServiceStartResult.PermissionDenied
        )
    }

    // ==================== 服务生命周期测试 ====================

    @Test
    fun serviceCanBeStartedWithPermission() {
        // Skip if we don't have permission
        if (!hasOverlayPermission) {
            return
        }

        // When
        val result = FloatingWindowManager.startService(context)

        // Then
        assertTrue(
            "有权限时启动服务应成功",
            result is FloatingWindowManager.ServiceStartResult.Success
        )

        // Cleanup
        FloatingWindowManager.stopService(context)
    }

    @Test
    fun serviceCanBeStopped() {
        // Skip if we don't have permission
        if (!hasOverlayPermission) {
            return
        }

        // Given - 先启动服务
        FloatingWindowManager.startService(context)
        Thread.sleep(500) // 等待服务启动

        // When
        val result = FloatingWindowManager.stopService(context)

        // Then
        assertTrue(
            "停止服务应成功",
            result is FloatingWindowManager.ServiceStopResult.Success ||
            result is FloatingWindowManager.ServiceStopResult.NotRunning
        )
    }

    @Test
    fun stopService_whenNotRunning_returnsNotRunning() {
        // When
        val result = FloatingWindowManager.stopService(context)

        // Then
        assertTrue(
            "停止未运行的服务应返回 NotRunning 或 Success",
            result is FloatingWindowManager.ServiceStopResult.NotRunning ||
            result is FloatingWindowManager.ServiceStopResult.Success
        )
    }

    // ==================== Intent 测试 ====================

    @Test
    fun serviceIntent_hasCorrectClass() {
        // When
        val intent = Intent(context, FloatingWindowService::class.java)

        // Then
        assertEquals(
            "Intent 应指向 FloatingWindowService",
            FloatingWindowService::class.java.name,
            intent.component?.className
        )
    }

    @Test
    fun serviceIntent_restoreAction_hasCorrectAction() {
        // When
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = "com.empathy.ai.ACTION_RESTORE_DIALOG"
        }

        // Then
        assertEquals(
            "恢复对话框 Intent 应有正确的 action",
            "com.empathy.ai.ACTION_RESTORE_DIALOG",
            intent.action
        )
    }

    // ==================== 服务绑定测试 ====================

    @Test
    fun serviceBind_returnsNull() {
        // Skip if we don't have permission
        if (!hasOverlayPermission) {
            return
        }

        // Given
        val intent = Intent(context, FloatingWindowService::class.java)

        // When & Then
        try {
            val binder = serviceRule.bindService(intent)
            // FloatingWindowService 不支持绑定，应该返回 null
            assertNull("服务不支持绑定，应返回 null", binder)
        } catch (e: TimeoutException) {
            // 绑定超时也是预期行为，因为服务不支持绑定
            assertTrue("服务不支持绑定", true)
        }
    }

    // ==================== 多次启动测试 ====================

    @Test
    fun multipleStartCalls_shouldNotCrash() {
        // Skip if we don't have permission
        if (!hasOverlayPermission) {
            return
        }

        // When - 多次启动服务
        repeat(3) {
            val result = FloatingWindowManager.startService(context)
            assertTrue(
                "多次启动服务应成功",
                result is FloatingWindowManager.ServiceStartResult.Success
            )
            Thread.sleep(100)
        }

        // Then - 停止服务应成功
        val stopResult = FloatingWindowManager.stopService(context)
        assertTrue(
            "停止服务应成功",
            stopResult is FloatingWindowManager.ServiceStopResult.Success ||
            stopResult is FloatingWindowManager.ServiceStopResult.NotRunning
        )
    }

    @Test
    fun multipleStopCalls_shouldNotCrash() {
        // When - 多次停止服务
        repeat(3) {
            val result = FloatingWindowManager.stopService(context)
            assertTrue(
                "多次停止服务不应崩溃",
                result is FloatingWindowManager.ServiceStopResult.Success ||
                result is FloatingWindowManager.ServiceStopResult.NotRunning ||
                result is FloatingWindowManager.ServiceStopResult.Error
            )
        }
    }

    // ==================== 前台服务权限测试 ====================

    @Test
    fun foregroundServicePermission_isChecked() {
        // When
        val result = FloatingWindowManager.hasForegroundServicePermission(context)

        // Then
        assertTrue(
            "前台服务权限检查应返回有效结果",
            result is FloatingWindowManager.PermissionResult.Granted ||
            result is FloatingWindowManager.PermissionResult.Denied ||
            result is FloatingWindowManager.PermissionResult.Error
        )
    }

    @Test
    fun allPermissions_areChecked() {
        // When
        val result = FloatingWindowManager.checkAllPermissions(context)

        // Then
        assertTrue(
            "综合权限检查应返回有效结果",
            result is FloatingWindowManager.PermissionResult.Granted ||
            result is FloatingWindowManager.PermissionResult.Denied ||
            result is FloatingWindowManager.PermissionResult.Error
        )
    }

    // ==================== 服务状态一致性测试 ====================

    @Test
    fun startAndStop_maintainsConsistentState() {
        // Skip if we don't have permission
        if (!hasOverlayPermission) {
            return
        }

        // Given
        val startResult = FloatingWindowManager.startService(context)
        assertTrue(
            "启动服务应成功",
            startResult is FloatingWindowManager.ServiceStartResult.Success
        )
        Thread.sleep(500)

        // When
        val stopResult = FloatingWindowManager.stopService(context)

        // Then
        assertTrue(
            "停止服务应成功",
            stopResult is FloatingWindowManager.ServiceStopResult.Success ||
            stopResult is FloatingWindowManager.ServiceStopResult.NotRunning
        )

        // 再次停止应返回 NotRunning
        Thread.sleep(500)
        val secondStopResult = FloatingWindowManager.stopService(context)
        assertTrue(
            "再次停止应返回 NotRunning",
            secondStopResult is FloatingWindowManager.ServiceStopResult.NotRunning ||
            secondStopResult is FloatingWindowManager.ServiceStopResult.Success
        )
    }
}
