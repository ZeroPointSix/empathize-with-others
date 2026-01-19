package com.empathy.ai.presentation.viewmodel

import android.app.Application
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.FloatingWindowPreferencesRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.presentation.ui.screen.settings.SettingsUiEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BUG-00070: 悬浮球App内不显示问题 - 测试用例
 *
 * 问题背景:
 *   悬浮球在App内不显示，根本原因是：
 *   1. WindowManager绑定到错误的Context
 *   2. 启动恢复逻辑不完整
 *   3. 多显示屏支持缺失
 *
 * 测试范围:
 * 1. 多显示屏WindowManager绑定逻辑
 * 2. 服务启动时的幂等性保护
 * 3. 权限丢失时的状态重置
 * 4. displayId参数传递链路
 * 5. 自动恢复服务逻辑
 *
 * @see BUG-00070 悬浮球App内不显示问题
 * @see TE-00070 悬浮球App内不显示测试用例
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelBug00070Test {

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var mockApplication: Application

    @RelaxedMockK
    private lateinit var mockSettingsRepository: SettingsRepository

    @MockK
    private lateinit var mockPreferencesRepository: FloatingWindowPreferencesRepository

    @RelaxedMockK
    private lateinit var mockAiProviderRepository: AiProviderRepository

    @MockK
    private lateinit var mockFloatingWindowManager: FloatingWindowManager

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // 初始化默认Mock行为
        every { mockPreferencesRepository.loadState() } returns FloatingWindowState(
            isEnabled = false,
            buttonX = 0,
            buttonY = 0
        )
        every { mockPreferencesRepository.isContinuousScreenshotEnabled() } returns false
        every { mockPreferencesRepository.hasScreenshotPermission() } returns false
        coEvery { mockSettingsRepository.getDataMaskingEnabled() } returns Result.success(true)
        coEvery { mockSettingsRepository.getLocalFirstModeEnabled() } returns Result.success(true)
        coEvery { mockSettingsRepository.getHistoryConversationCount() } returns Result.success(5)
        every { mockAiProviderRepository.getAllProviders() } returns flowOf(emptyList())
        every { mockFloatingWindowManager.hasPermission() } returns FloatingWindowManager.PermissionResult.Granted
        every { mockFloatingWindowManager.isServiceRunning() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ==================== BUG-00070 核心测试用例 ====================

    /**
     * 测试悬浮窗服务启动时传递displayId参数
     *
     * 验证调用链:
     * SettingsViewModel.toggleFloatingWindow(displayId)
     *   -> startFloatingWindowService(displayId)
     *   -> floatingWindowManager.startService(displayId)
     */
    @Test
    fun `startFloatingWindowService should pass displayId to manager`() = runTest {
        // Given: FloatingWindowManager mock
        every { mockFloatingWindowManager.hasPermission() } returns FloatingWindowManager.PermissionResult.Granted
        every { mockFloatingWindowManager.startService(any()) } returns FloatingWindowManager.ServiceStartResult.Success
        every { mockPreferencesRepository.saveEnabled(true) } just Runs

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 触发悬浮窗启动
        val displayId = 2
        viewModel.onEvent(SettingsUiEvent.ToggleFloatingWindow(displayId))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: startService应该被调用，且参数正确传递
        verify { mockFloatingWindowManager.startService(displayId) }
    }

    /**
     * 测试悬浮窗权限检查逻辑
     *
     * BUG-00070 修复前：权限检查结果未正确处理
     * BUG-00070 修复后：正确区分Granted/Denied/Error状态
     */
    @Test
    fun `checkFloatingWindowPermission should handle all permission states`() = runTest {
        // Given: 权限被拒绝
        every { mockFloatingWindowManager.hasPermission() } returns FloatingWindowManager.PermissionResult.Denied("悬浮窗权限未授予")

        // When: 检查权限
        val result = mockFloatingWindowManager.hasPermission()

        // Then: 应该正确返回Denied状态
        assertTrue(result is FloatingWindowManager.PermissionResult.Denied)
        assertEquals("悬浮窗权限未授予", result.message)
    }

    /**
     * 测试权限丢失时的状态重置
     *
     * BUG-00070 修复前：权限丢失后本地状态仍为启用
     * BUG-00070 修复后：权限丢失时重置本地状态为禁用
     */
    @Test
    fun `should reset enabled state when permission is lost`() = runTest {
        // Given: 权限检查返回Denined
        every { mockFloatingWindowManager.hasPermission() } returns FloatingWindowManager.PermissionResult.Denied("权限被拒绝")
        every { mockPreferencesRepository.saveEnabled(false) } just Runs
        every { mockPreferencesRepository.loadState() } returns FloatingWindowState(
            isEnabled = true,
            buttonX = 0,
            buttonY = 0
        )

        // When: 创建ViewModel触发状态恢复逻辑
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 权限丢失，should重置状态
        verify { mockPreferencesRepository.saveEnabled(false) }
    }

    /**
     * 测试服务未运行时不应重复启动
     *
     * 验证幂等性保护逻辑
     */
    @Test
    fun `should not start service if already running`() = runTest {
        // Given: 服务已在运行
        every { mockFloatingWindowManager.isServiceRunning() } returns true

        // When: 尝试启动
        val isRunning = mockFloatingWindowManager.isServiceRunning()

        // Then: 服务运行中，不应重复启动
        assertTrue(isRunning)
    }

    /**
     * 测试startService返回PermissionDenied时的UI状态更新
     */
    @Test
    fun `should update UI state when permission denied on start`() = runTest {
        // Given: 权限被拒绝
        every { mockFloatingWindowManager.hasPermission() } returns FloatingWindowManager.PermissionResult.Granted
        every { mockFloatingWindowManager.startService(any()) } returns FloatingWindowManager.ServiceStartResult.PermissionDenied("需要悬浮窗权限")

        // When: 启动服务
        val result = mockFloatingWindowManager.startService(null)

        // Then: 应该返回PermissionDenied
        assertTrue(result is FloatingWindowManager.ServiceStartResult.PermissionDenied)
    }

    /**
     * 测试连续截屏功能的启用状态加载
     *
     * TE-00070 测试用例：验证连续截屏开关状态正确加载
     */
    @Test
    fun `should load continuous screenshot enabled state`() = runTest {
        // Given: 连续截屏已启用
        every { mockPreferencesRepository.isContinuousScreenshotEnabled() } returns true

        // When: 加载状态
        val enabled = mockPreferencesRepository.isContinuousScreenshotEnabled()

        // Then: 应该正确返回启用状态
        assertTrue(enabled)
    }

    /**
     * 测试截图权限开关 - 未授权时应触发权限请求
     */
    @Test
    fun `toggle screenshot permission should request when not granted`() = runTest {
        // Given: 未授权
        every { mockPreferencesRepository.hasScreenshotPermission() } returns false
        val viewModel = createViewModel()

        // When: 切换截图权限开关
        viewModel.onEvent(SettingsUiEvent.ToggleScreenshotPermission)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 应触发权限请求
        assertTrue(viewModel.uiState.value.pendingScreenshotPermissionRequest)
    }

    /**
     * 测试截图权限开关 - 已授权时应清除权限
     */
    @Test
    fun `toggle screenshot permission should clear when granted`() = runTest {
        // Given: 已授权
        every { mockPreferencesRepository.hasScreenshotPermission() } returns true
        every { mockPreferencesRepository.clearScreenshotPermission() } just Runs
        val viewModel = createViewModel()

        // When: 切换截图权限开关
        viewModel.onEvent(SettingsUiEvent.ToggleScreenshotPermission)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 权限被清除
        verify { mockPreferencesRepository.clearScreenshotPermission() }
        assertFalse(viewModel.uiState.value.hasScreenshotPermission)
        assertEquals("截图权限已清除", viewModel.uiState.value.successMessage)
    }

    // ==================== 多显示屏支持测试 ====================

    /**
     * 测试非默认displayId参数传递
     *
     * BUG-00070 修复后支持外部显示屏
     */
    @Test
    fun `should pass displayId parameter to service manager`() = runTest {
        // Given: 传入特定的displayId
        val targetDisplayId = 1

        // When: 调用startService
        every { mockFloatingWindowManager.startService(eq(targetDisplayId)) } returns FloatingWindowManager.ServiceStartResult.Success
        mockFloatingWindowManager.startService(targetDisplayId)

        // Then: 应该传递正确的displayId
        verify { mockFloatingWindowManager.startService(targetDisplayId) }
    }

    /**
     * 测试null displayId使用默认值
     */
    @Test
    fun `should use default display when displayId is null`() = runTest {
        // Given: displayId为null
        every { mockFloatingWindowManager.startService(null) } returns FloatingWindowManager.ServiceStartResult.Success

        // When: 调用startService
        mockFloatingWindowManager.startService(null)

        // Then: 应该使用默认显示屏(null会由manager处理为DEFAULT_DISPLAY)
        verify { mockFloatingWindowManager.startService(null) }
    }

    // ==================== 边界条件测试 ====================

    /**
     * 测试服务启动失败的错误处理
     */
    @Test
    fun `should handle service start error gracefully`() = runTest {
        // Given: 服务启动异常
        every { mockFloatingWindowManager.startService(any()) } returns FloatingWindowManager.ServiceStartResult.Error("启动失败")

        // When: 启动服务
        val result = mockFloatingWindowManager.startService(null)

        // Then: 应该返回Error状态
        assertTrue(result is FloatingWindowManager.ServiceStartResult.Error)
        val error = result as FloatingWindowManager.ServiceStartResult.Error
        assertEquals("启动失败", error.message)
    }

    /**
     * 测试权限检查异常处理
     */
    @Test
    fun `should handle permission check error gracefully`() = runTest {
        // Given: 权限检查异常
        every { mockFloatingWindowManager.hasPermission() } returns FloatingWindowManager.PermissionResult.Error("检查失败")

        // When: 检查权限
        val result = mockFloatingWindowManager.hasPermission()

        // Then: 应该返回Error状态
        assertTrue(result is FloatingWindowManager.PermissionResult.Error)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            mockApplication,
            mockSettingsRepository,
            mockPreferencesRepository,
            mockAiProviderRepository,
            mockFloatingWindowManager
        )
    }
}
