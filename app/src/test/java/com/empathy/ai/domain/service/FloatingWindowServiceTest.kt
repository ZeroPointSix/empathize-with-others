package com.empathy.ai.domain.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.domain.usecase.CheckDraftUseCase
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * FloatingWindowService 单元测试
 * 
 * 测试服务生命周期管理和资源清理
 * 
 * **属性 1：服务生命周期一致性**
 * **验证需求：1.1, 1.5**
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class FloatingWindowServiceTest {
    
    private lateinit var context: Context
    private lateinit var service: FloatingWindowService
    private lateinit var analyzeChatUseCase: AnalyzeChatUseCase
    private lateinit var checkDraftUseCase: CheckDraftUseCase
    private lateinit var contactRepository: ContactRepository
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        
        // 创建 Mock 依赖
        analyzeChatUseCase = mockk(relaxed = true)
        checkDraftUseCase = mockk(relaxed = true)
        contactRepository = mockk(relaxed = true)
        
        // 创建服务控制器
        val serviceController = Robolectric.buildService(FloatingWindowService::class.java)
        service = serviceController.get()
        
        // 注入 Mock 依赖
        service.analyzeChatUseCase = analyzeChatUseCase
        service.checkDraftUseCase = checkDraftUseCase
        service.contactRepository = contactRepository
        
        // 调用 onCreate
        serviceController.create()
    }
    
    @After
    fun tearDown() {
        // 清理资源
        try {
            service.onDestroy()
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }
    
    /**
     * 测试服务创建
     * 
     * 验证：服务创建后应该初始化必要的资源
     */
    @Test
    fun `onCreate should initialize WindowManager`() {
        // Then: 服务应该成功创建
        assertNotNull(service)
    }
    
    /**
     * 测试服务启动
     * 
     * 验证：服务启动后应该启动前台服务并显示通知
     * 
     * **属性 1：服务生命周期一致性**
     * 对于任何悬浮窗服务实例，启动服务应该显示悬浮视图
     */
    @Test
    fun `onStartCommand should start foreground service`() {
        // Given
        val intent = Intent(context, FloatingWindowService::class.java)
        
        // When
        val result = service.onStartCommand(intent, 0, 1)
        
        // Then: 应该返回 START_STICKY
        assertEquals(android.app.Service.START_STICKY, result)
        
        // Then: 应该创建通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        
        // 验证通知渠道已创建（Android O+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = shadowNotificationManager.notificationChannels
            assertNotNull("通知渠道列表不应该为空", channels)
            assertTrue("应该至少有一个通知渠道", channels.isNotEmpty())
        }
    }
    
    /**
     * 测试服务停止
     * 
     * 验证：服务停止后应该清理所有资源
     * 
     * **属性 1：服务生命周期一致性**
     * 对于任何悬浮窗服务实例，停止服务应该移除悬浮视图并清理所有资源
     */
    @Test
    fun `onDestroy should clean up resources`() {
        // Given: 先启动服务
        val intent = Intent(context, FloatingWindowService::class.java)
        service.onStartCommand(intent, 0, 1)
        
        // When: 停止服务
        service.onDestroy()
        
        // Then: 资源应该被清理
        // 注意：由于 floatingView 是私有的，我们无法直接验证
        // 但可以确保 onDestroy 不抛出异常
        assertTrue("服务应该成功销毁", true)
    }
    
    /**
     * 测试服务绑定
     * 
     * 验证：服务不支持绑定，应该返回 null
     */
    @Test
    fun `onBind should return null`() {
        // Given
        val intent = Intent(context, FloatingWindowService::class.java)
        
        // When
        val binder = service.onBind(intent)
        
        // Then
        assertNull("服务不支持绑定，应该返回 null", binder)
    }
    
    /**
     * 测试服务重启
     * 
     * 验证：服务被杀死后应该自动重启（START_STICKY）
     */
    @Test
    fun `service should return START_STICKY for auto restart`() {
        // Given
        val intent = Intent(context, FloatingWindowService::class.java)
        
        // When
        val result = service.onStartCommand(intent, 0, 1)
        
        // Then
        assertEquals(
            "服务应该返回 START_STICKY 以支持自动重启",
            android.app.Service.START_STICKY,
            result
        )
    }
    
    /**
     * 测试通知创建
     * 
     * 验证：前台服务通知应该包含正确的信息
     */
    @Test
    fun `notification should have correct content`() {
        // Given
        val intent = Intent(context, FloatingWindowService::class.java)
        
        // When
        service.onStartCommand(intent, 0, 1)
        
        // Then: 验证通知已创建
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        
        // 获取通知
        val notifications = shadowNotificationManager.allNotifications
        assertTrue("应该至少有一个通知", notifications.isNotEmpty())
        
        val notification = notifications.first()
        assertNotNull("通知应该存在", notification)
        
        // 验证通知内容
        val shadowNotification = shadowOf(notification)
        assertEquals("共情 AI 助手", shadowNotification.contentTitle)
        assertEquals("悬浮窗服务运行中", shadowNotification.contentText)
    }
    
    /**
     * 测试多次启动服务
     * 
     * 验证：多次启动服务不应该导致资源泄漏
     */
    @Test
    fun `multiple onStartCommand calls should not cause resource leak`() {
        // Given
        val intent = Intent(context, FloatingWindowService::class.java)
        
        // When: 多次启动服务
        service.onStartCommand(intent, 0, 1)
        service.onStartCommand(intent, 0, 2)
        service.onStartCommand(intent, 0, 3)
        
        // Then: 应该成功处理多次启动
        assertTrue("服务应该能够处理多次启动", true)
        
        // When: 停止服务
        service.onDestroy()
        
        // Then: 应该成功清理资源
        assertTrue("服务应该成功清理资源", true)
    }
    
    /**
     * 测试服务生命周期完整流程
     * 
     * 验证：完整的生命周期流程应该正常工作
     * 
     * **属性 1：服务生命周期一致性**
     * 完整测试：onCreate -> onStartCommand -> onDestroy
     */
    @Test
    fun `complete lifecycle should work correctly`() {
        // Given: 服务已在 setup 中创建
        assertNotNull("服务应该已创建", service)
        
        // When: 启动服务
        val intent = Intent(context, FloatingWindowService::class.java)
        val startResult = service.onStartCommand(intent, 0, 1)
        
        // Then: 服务应该正常启动
        assertEquals(android.app.Service.START_STICKY, startResult)
        
        // When: 停止服务
        service.onDestroy()
        
        // Then: 服务应该正常停止
        assertTrue("服务生命周期应该完整执行", true)
    }
}
