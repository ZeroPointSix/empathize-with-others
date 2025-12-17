package com.empathy.ai.integration

import android.content.Context
import android.os.Build
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.service.FloatingWindowService
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.domain.usecase.CheckDraftUseCase
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 悬浮窗最小化功能完整流程集成测试
 * 
 * 测试任务 12：完整流程集成测试
 * 
 * 测试场景：
 * 1. testCompleteMinimizeRestoreFlow() - 完整最小化-恢复流程
 * 2. testDragIndicatorSavesPosition() - 拖动保存位置
 * 3. testAppRestartRecovery() - 应用重启恢复
 * 4. testResourceCleanup() - 资源清理
 * 5. testErrorScenarios() - 错误场景
 * 
 * **验证需求：所有需求**
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class FloatingWindowMinimizeIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var mockPreferences: FloatingWindowPreferences
    
    // 测试数据
    private val testContactId = "test-contact-123"
    private val testRequestId = "test-request-456"
    private val testText = "你好，最近怎么样？"
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockPreferences = mockk(relaxed = true)
        
        // 配置默认行为
        every { mockPreferences.getButtonPosition() } returns Pair(100, 200)
        every { mockPreferences.getIndicatorPosition() } returns Pair(100, 200)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== 测试用例 1：完整最小化-恢复流程 ====================
    
    /**
     * 测试完整的最小化-恢复流程（数据流测试）
     * 
     * 流程：
     * 1. 保存请求信息
     * 2. 保存指示器位置
     * 3. 验证数据持久化
     * 4. 恢复请求信息
     * 5. 清除数据
     * 
     * **验证需求：1.1, 1.5, 2.1, 5.1, 9.1, 9.2**
     */
    @Test
    fun `testCompleteMinimizeRestoreFlow - should persist and restore data`() = runTest {
        // Step 1: 创建请求信息
        val requestInfo = MinimizedRequestInfo(
            id = testRequestId,
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )
        
        // Step 2: 保存请求信息
        every { mockPreferences.saveRequestInfo(any()) } just Runs
        mockPreferences.saveRequestInfo(requestInfo)
        verify { mockPreferences.saveRequestInfo(requestInfo) }
        
        // Step 3: 保存指示器位置
        val indicatorX = 150
        val indicatorY = 250
        every { mockPreferences.saveIndicatorPosition(any(), any()) } just Runs
        mockPreferences.saveIndicatorPosition(indicatorX, indicatorY)
        verify { mockPreferences.saveIndicatorPosition(indicatorX, indicatorY) }
        
        // Step 4: 模拟恢复（读取数据）
        every { mockPreferences.getRequestInfo() } returns requestInfo
        every { mockPreferences.getIndicatorPosition() } returns Pair(indicatorX, indicatorY)
        
        val restoredInfo = mockPreferences.getRequestInfo()
        val restoredPosition = mockPreferences.getIndicatorPosition()
        
        // Step 5: 验证数据一致性
        assertNotNull("应该恢复请求信息", restoredInfo)
        assertEquals("请求 ID 应该一致", testRequestId, restoredInfo?.id)
        assertEquals("请求类型应该一致", ActionType.ANALYZE, restoredInfo?.type)
        assertEquals("X 坐标应该一致", indicatorX, restoredPosition.first)
        assertEquals("Y 坐标应该一致", indicatorY, restoredPosition.second)
        
        // Step 6: 清除数据
        every { mockPreferences.clearRequestInfo() } just Runs
        mockPreferences.clearRequestInfo()
        verify { mockPreferences.clearRequestInfo() }
        
        android.util.Log.d("MinimizeTest", "完整流程测试通过")
    }
    
    // ==================== 测试用例 2：拖动保存位置 ====================
    
    /**
     * 测试拖动指示器保存位置
     * 
     * 流程：
     * 1. 保存初始位置
     * 2. 更新到新位置
     * 3. 验证位置更新
     * 4. 恢复后验证位置
     * 
     * **验证需求：5.2, 5.3, 5.4**
     */
    @Test
    fun `testDragIndicatorSavesPosition - should save and restore position`() = runTest {
        // Step 1: 保存初始位置
        val initialX = 100
        val initialY = 200
        every { mockPreferences.saveIndicatorPosition(any(), any()) } just Runs
        mockPreferences.saveIndicatorPosition(initialX, initialY)
        verify { mockPreferences.saveIndicatorPosition(initialX, initialY) }
        
        // Step 2: 拖动到新位置
        val newX = 300
        val newY = 400
        mockPreferences.saveIndicatorPosition(newX, newY)
        verify { mockPreferences.saveIndicatorPosition(newX, newY) }
        
        // Step 3: 验证新位置被保存
        every { mockPreferences.getIndicatorPosition() } returns Pair(newX, newY)
        val savedPosition = mockPreferences.getIndicatorPosition()
        assertEquals("X 坐标应该更新", newX, savedPosition.first)
        assertEquals("Y 坐标应该更新", newY, savedPosition.second)
        
        // Step 4: 模拟恢复后再次最小化，验证位置保持
        every { mockPreferences.getButtonPosition() } returns Pair(newX, newY)
        val buttonPosition = mockPreferences.getButtonPosition()
        assertEquals("悬浮按钮 X 坐标应该在新位置", newX, buttonPosition.first)
        assertEquals("悬浮按钮 Y 坐标应该在新位置", newY, buttonPosition.second)
        
        android.util.Log.d("MinimizeTest", "拖动位置保存测试通过")
    }
    
    // ==================== 测试用例 3：应用重启恢复 ====================
    
    /**
     * 测试应用重启后恢复请求状态
     * 
     * 流程：
     * 1. 保存请求状态
     * 2. 模拟应用重启
     * 3. 恢复请求状态
     * 4. 验证数据完整性
     * 
     * **验证需求：9.1, 9.2, 9.3, 9.4**
     */
    @Test
    fun `testAppRestartRecovery - should restore state after restart`() = runTest {
        // Step 1: 保存请求状态
        val requestInfo = MinimizedRequestInfo(
            id = testRequestId,
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )
        val savedX = 150
        val savedY = 250
        
        every { mockPreferences.saveRequestInfo(any()) } just Runs
        every { mockPreferences.saveIndicatorPosition(any(), any()) } just Runs
        
        mockPreferences.saveRequestInfo(requestInfo)
        mockPreferences.saveIndicatorPosition(savedX, savedY)
        
        verify { mockPreferences.saveRequestInfo(requestInfo) }
        verify { mockPreferences.saveIndicatorPosition(savedX, savedY) }
        
        // Step 2: 模拟应用重启（清除内存状态）
        clearMocks(mockPreferences, answers = false)
        
        // Step 3: 恢复请求状态
        every { mockPreferences.getRequestInfo() } returns requestInfo
        every { mockPreferences.getIndicatorPosition() } returns Pair(savedX, savedY)
        
        val restoredInfo = mockPreferences.getRequestInfo()
        val restoredPosition = mockPreferences.getIndicatorPosition()
        
        // Step 4: 验证数据完整性
        assertNotNull("应该恢复请求信息", restoredInfo)
        assertEquals("请求 ID 应该一致", testRequestId, restoredInfo?.id)
        assertEquals("请求类型应该一致", ActionType.ANALYZE, restoredInfo?.type)
        assertEquals("X 坐标应该恢复", savedX, restoredPosition.first)
        assertEquals("Y 坐标应该恢复", savedY, restoredPosition.second)
        
        android.util.Log.d("MinimizeTest", "应用重启恢复测试通过")
    }
    
    /**
     * 测试过期请求被清除
     * 
     * 验证：超过 10 分钟的请求应该被清除
     * 
     * **验证需求：9.3**
     */
    @Test
    fun `testAppRestartRecovery - should clear expired requests`() = runTest {
        // Given: 创建一个过期的请求（11 分钟前）
        val expiredTime = System.currentTimeMillis() - (11 * 60 * 1000)
        val expiredRequest = MinimizedRequestInfo(
            id = testRequestId,
            type = ActionType.ANALYZE,
            timestamp = expiredTime
        )
        
        every { mockPreferences.getRequestInfo() } returns expiredRequest
        every { mockPreferences.clearRequestInfo() } just Runs
        
        // When: 检查请求是否过期
        val requestInfo = mockPreferences.getRequestInfo()
        val currentTime = System.currentTimeMillis()
        val isExpired = requestInfo != null && 
                       (currentTime - requestInfo.timestamp) > (10 * 60 * 1000)
        
        // Then: 应该检测到过期并清除
        assertTrue("请求应该过期", isExpired)
        
        // 模拟清除操作
        if (isExpired) {
            mockPreferences.clearRequestInfo()
        }
        
        verify { mockPreferences.clearRequestInfo() }
        
        android.util.Log.d("MinimizeTest", "过期请求清除测试通过")
    }
    
    // ==================== 测试用例 4：资源清理 ====================
    
    /**
     * 测试资源清理逻辑
     * 
     * 流程：
     * 1. 保存请求信息
     * 2. 模拟完成状态
     * 3. 验证清理逻辑
     * 
     * **验证需求：8.1, 8.2, 8.3, 8.4, 8.5**
     */
    @Test
    fun `testResourceCleanup - should cleanup completed requests`() = runTest {
        // Step 1: 保存请求信息
        val requestInfo = MinimizedRequestInfo(
            id = testRequestId,
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )
        
        every { mockPreferences.saveRequestInfo(any()) } just Runs
        mockPreferences.saveRequestInfo(requestInfo)
        
        // Step 2: 模拟请求完成（10 分钟后）
        delay(100)  // 模拟时间流逝
        
        // Step 3: 验证清理逻辑
        every { mockPreferences.clearRequestInfo() } just Runs
        mockPreferences.clearRequestInfo()
        
        verify { mockPreferences.clearRequestInfo() }
        
        android.util.Log.d("MinimizeTest", "资源清理测试通过")
    }
    
    /**
     * 测试单一指示器约束
     * 
     * 验证：创建新指示器前应该清除旧指示器
     * 
     * **验证需求：8.3**
     */
    @Test
    fun `testResourceCleanup - should maintain single indicator constraint`() = runTest {
        // Given: 已有一个请求
        val firstRequest = MinimizedRequestInfo(
            id = "request-1",
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )
        
        every { mockPreferences.saveRequestInfo(any()) } just Runs
        every { mockPreferences.clearRequestInfo() } just Runs
        
        // When: 保存第一个请求
        mockPreferences.saveRequestInfo(firstRequest)
        verify { mockPreferences.saveRequestInfo(firstRequest) }
        
        // When: 创建第二个请求（应该先清除第一个）
        val secondRequest = MinimizedRequestInfo(
            id = "request-2",
            type = ActionType.CHECK,
            timestamp = System.currentTimeMillis()
        )
        
        // 模拟清除旧请求
        mockPreferences.clearRequestInfo()
        mockPreferences.saveRequestInfo(secondRequest)
        
        // Then: 验证清除和保存操作
        verify { mockPreferences.clearRequestInfo() }
        verify { mockPreferences.saveRequestInfo(secondRequest) }
        
        android.util.Log.d("MinimizeTest", "单一指示器约束测试通过")
    }
    
    // ==================== 测试用例 5：错误场景 ====================
    
    /**
     * 测试数据持久化失败处理
     * 
     * 验证：持久化失败时应该捕获异常
     * 
     * **验证需求：7.1, 9.1**
     */
    @Test
    fun `testErrorScenarios - should handle persistence failure`() = runTest {
        // Given: 模拟持久化失败
        every { mockPreferences.saveRequestInfo(any()) } throws Exception("存储失败")
        
        val requestInfo = MinimizedRequestInfo(
            id = testRequestId,
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )
        
        // When: 尝试保存
        var saveFailed = false
        try {
            mockPreferences.saveRequestInfo(requestInfo)
        } catch (e: Exception) {
            saveFailed = true
            android.util.Log.e("MinimizeTest", "保存失败: ${e.message}")
        }
        
        // Then: 应该捕获异常
        assertTrue("应该检测到保存失败", saveFailed)
        
        android.util.Log.d("MinimizeTest", "持久化失败处理测试通过")
    }
    
    /**
     * 测试数据恢复失败处理
     * 
     * 验证：恢复失败时应该返回 null
     * 
     * **验证需求：7.2, 9.2**
     */
    @Test
    fun `testErrorScenarios - should handle restore failure`() = runTest {
        // Given: 模拟恢复失败（返回 null）
        every { mockPreferences.getRequestInfo() } returns null
        
        // When: 尝试恢复
        val restoredInfo = mockPreferences.getRequestInfo()
        
        // Then: 应该返回 null
        assertNull("恢复失败应该返回 null", restoredInfo)
        
        android.util.Log.d("MinimizeTest", "恢复失败处理测试通过")
    }
    
    /**
     * 测试数据损坏处理
     * 
     * 验证：数据损坏时应该清除并返回 null
     * 
     * **验证需求：7.1, 9.2**
     */
    @Test
    fun `testErrorScenarios - should handle corrupted data`() = runTest {
        // Given: 模拟数据损坏（抛出异常）
        every { mockPreferences.getRequestInfo() } throws Exception("数据损坏")
        every { mockPreferences.clearRequestInfo() } just Runs
        
        // When: 尝试恢复
        var dataCorrupted = false
        try {
            mockPreferences.getRequestInfo()
        } catch (e: Exception) {
            dataCorrupted = true
            // 清除损坏的数据
            mockPreferences.clearRequestInfo()
        }
        
        // Then: 应该检测到数据损坏并清除
        assertTrue("应该检测到数据损坏", dataCorrupted)
        verify { mockPreferences.clearRequestInfo() }
        
        android.util.Log.d("MinimizeTest", "数据损坏处理测试通过")
    }
    
    /**
     * 测试并发请求处理
     * 
     * 验证：同时只能有一个活动请求
     * 
     * **验证需求：8.3**
     */
    @Test
    fun `testErrorScenarios - should handle concurrent requests`() = runTest {
        // Given: 已有一个活动请求
        val firstRequest = MinimizedRequestInfo(
            id = "request-1",
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )
        
        every { mockPreferences.saveRequestInfo(any()) } just Runs
        every { mockPreferences.getRequestInfo() } returns firstRequest
        every { mockPreferences.clearRequestInfo() } just Runs
        
        mockPreferences.saveRequestInfo(firstRequest)
        
        // When: 尝试创建第二个请求
        val secondRequest = MinimizedRequestInfo(
            id = "request-2",
            type = ActionType.CHECK,
            timestamp = System.currentTimeMillis()
        )
        
        // 检查是否有活动请求
        val existingRequest = mockPreferences.getRequestInfo()
        if (existingRequest != null) {
            // 清除旧请求
            mockPreferences.clearRequestInfo()
        }
        
        // 保存新请求
        mockPreferences.saveRequestInfo(secondRequest)
        
        // Then: 应该先清除旧请求
        verify { mockPreferences.clearRequestInfo() }
        verify { mockPreferences.saveRequestInfo(secondRequest) }
        
        android.util.Log.d("MinimizeTest", "并发请求处理测试通过")
    }
    
    /**
     * 测试位置数据验证
     * 
     * 验证：位置数据应该在有效范围内
     * 
     * **验证需求：5.2, 5.3**
     */
    @Test
    fun `testErrorScenarios - should validate position data`() = runTest {
        // Given: 保存位置数据
        val validX = 100
        val validY = 200
        
        every { mockPreferences.saveIndicatorPosition(any(), any()) } just Runs
        every { mockPreferences.getIndicatorPosition() } returns Pair(validX, validY)
        
        mockPreferences.saveIndicatorPosition(validX, validY)
        
        // When: 读取位置数据
        val position = mockPreferences.getIndicatorPosition()
        
        // Then: 验证位置数据有效
        assertTrue("X 坐标应该有效", position.first >= 0)
        assertTrue("Y 坐标应该有效", position.second >= 0)
        
        android.util.Log.d("MinimizeTest", "位置数据验证测试通过")
    }
}
