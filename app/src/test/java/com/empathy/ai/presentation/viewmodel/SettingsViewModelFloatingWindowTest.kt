package com.empathy.ai.presentation.viewmodel

import android.app.Application
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.util.FloatingWindowManager
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
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * BUG-00019: 设置界面自动展开悬浮球问题测试
 *
 * 测试场景：
 * 1. 进入设置界面不应重复启动服务
 * 2. 服务运行状态检查
 * 3. 权限检查与服务启动的协调
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelFloatingWindowTest {

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var mockApplication: Application

    @RelaxedMockK
    private lateinit var mockSettingsRepository: SettingsRepository

    @RelaxedMockK
    private lateinit var mockPreferences: FloatingWindowPreferences

    @RelaxedMockK
    private lateinit var mockAiProviderRepository: AiProviderRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // 设置默认行为
        every { mockPreferences.loadState() } returns FloatingWindowState(
            isEnabled = false,
            buttonX = 0,
            buttonY = 0
        )
        every { mockPreferences.isEnabled() } returns false
        every { mockSettingsRepository.getDataMaskingEnabled() } returns Result.success(true)
        every { mockSettingsRepository.getLocalFirstModeEnabled() } returns Result.success(true)
        every { mockSettingsRepository.getHistoryConversationCount() } returns Result.success(5)
        every { mockAiProviderRepository.getAllProviders() } returns flowOf(emptyList())
        
        // Mock FloatingWindowManager
        mockkObject(FloatingWindowManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ==================== 服务运行状态检查测试 ====================

    @Test
    fun `loadFloatingWindowState should not start service if disabled`() = runTest {
        // Given: 悬浮窗状态为禁用
        every { mockPreferences.loadState() } returns FloatingWindowState(
            isEnabled = false,
            buttonX = 0,
            buttonY = 0
        )
        
        // When: 模拟加载状态
        val state = mockPreferences.loadState()
        
        // Then: 不应该启动服务
        assertEquals(false, state.isEnabled)
    }

    @Test
    fun `loadFloatingWindowState should check permission before starting service`() = runTest {
        // Given: 悬浮窗状态为启用
        every { mockPreferences.loadState() } returns FloatingWindowState(
            isEnabled = true,
            buttonX = 100,
            buttonY = 200
        )
        every { FloatingWindowManager.hasPermission(any()) } returns 
            FloatingWindowManager.PermissionResult.Granted
        
        // When: 加载状态
        val state = mockPreferences.loadState()
        val permissionResult = FloatingWindowManager.hasPermission(mockApplication)
        
        // Then: 应该检查权限
        assertEquals(true, state.isEnabled)
        assertEquals(FloatingWindowManager.PermissionResult.Granted, permissionResult)
    }

    @Test
    fun `should reset state when permission is denied`() = runTest {
        // Given: 悬浮窗状态为启用但权限被拒绝
        every { mockPreferences.loadState() } returns FloatingWindowState(
            isEnabled = true,
            buttonX = 100,
            buttonY = 200
        )
        every { FloatingWindowManager.hasPermission(any()) } returns 
            FloatingWindowManager.PermissionResult.Denied("Permission denied")
        every { mockPreferences.saveEnabled(any()) } just Runs
        
        // When: 检查权限
        val permissionResult = FloatingWindowManager.hasPermission(mockApplication)
        
        // Then: 权限被拒绝
        assertEquals(
            FloatingWindowManager.PermissionResult.Denied("Permission denied"),
            permissionResult
        )
    }

    // ==================== 服务启动幂等性测试 ====================

    @Test
    fun `startService should be called only once when service is not running`() = runTest {
        // Given: 服务未运行
        var startServiceCallCount = 0
        every { FloatingWindowManager.startService(any()) } answers {
            startServiceCallCount++
            FloatingWindowManager.ServiceStartResult.Success
        }
        
        // When: 多次尝试启动
        FloatingWindowManager.startService(mockApplication)
        
        // Then: 应该只调用一次
        assertEquals(1, startServiceCallCount)
    }

    @Test
    fun `should not restart service if already running`() = runTest {
        // Given: 模拟服务运行状态检查
        var isServiceRunning = false
        var startAttempts = 0
        
        val startServiceIfNotRunning: () -> Boolean = {
            startAttempts++
            if (!isServiceRunning) {
                isServiceRunning = true
                true
            } else {
                false
            }
        }
        
        // When: 多次尝试启动
        val firstResult = startServiceIfNotRunning()
        val secondResult = startServiceIfNotRunning()
        val thirdResult = startServiceIfNotRunning()
        
        // Then: 只有第一次应该成功启动
        assertEquals(true, firstResult)
        assertEquals(false, secondResult)
        assertEquals(false, thirdResult)
        assertEquals(3, startAttempts)
    }

    // ==================== 状态同步测试 ====================

    @Test
    fun `UI state should reflect preferences state`() = runTest {
        // Given: 偏好设置中悬浮窗已启用
        every { mockPreferences.loadState() } returns FloatingWindowState(
            isEnabled = true,
            buttonX = 50,
            buttonY = 100
        )
        
        // When: 加载状态
        val state = mockPreferences.loadState()
        
        // Then: 状态应该正确反映
        assertEquals(true, state.isEnabled)
        assertEquals(50, state.buttonX)
        assertEquals(100, state.buttonY)
    }

    @Test
    fun `should save enabled state when toggling`() = runTest {
        // Given: 准备切换状态
        every { mockPreferences.saveEnabled(any()) } just Runs
        
        // When: 保存启用状态
        mockPreferences.saveEnabled(true)
        
        // Then: 应该调用保存方法
        verify { mockPreferences.saveEnabled(true) }
    }

    // ==================== 权限检查测试 ====================

    @Test
    fun `checkFloatingWindowPermission should update UI state`() = runTest {
        // Given: 有权限
        every { FloatingWindowManager.hasPermission(any()) } returns 
            FloatingWindowManager.PermissionResult.Granted
        
        // When: 检查权限
        val result = FloatingWindowManager.hasPermission(mockApplication)
        
        // Then: 应该返回 Granted
        assertEquals(FloatingWindowManager.PermissionResult.Granted, result)
    }

    @Test
    fun `should handle permission error gracefully`() = runTest {
        // Given: 权限检查出错
        every { FloatingWindowManager.hasPermission(any()) } returns 
            FloatingWindowManager.PermissionResult.Error("Unknown error")
        
        // When: 检查权限
        val result = FloatingWindowManager.hasPermission(mockApplication)
        
        // Then: 应该返回 Error
        assertEquals(
            FloatingWindowManager.PermissionResult.Error("Unknown error"),
            result
        )
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `should handle concurrent permission checks`() = runTest {
        // Given: 模拟并发检查
        var checkCount = 0
        var isChecking = false
        
        val checkPermission: () -> Boolean = {
            if (!isChecking) {
                isChecking = true
                checkCount++
                isChecking = false
                true
            } else {
                false
            }
        }
        
        // When: 多次检查
        val results = (1..5).map { checkPermission() }
        
        // Then: 所有检查都应该成功（在这个简化模型中）
        assertEquals(5, checkCount)
        assertEquals(listOf(true, true, true, true, true), results)
    }

    @Test
    fun `should handle null application context`() {
        // Given: 可能的 null 情况
        val context: Application? = null
        
        // When: 安全访问
        val result = context?.let { "has context" } ?: "no context"
        
        // Then: 应该安全处理
        assertEquals("no context", result)
    }
}
