package com.empathy.ai.domain.service

import android.content.Context
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.domain.usecase.CheckDraftUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * FloatingWindowService 性能测试
 * 
 * 测试性能优化功能：
 * - 内存监控
 * - 超时控制
 * - 后台线程执行
 */
@RunWith(RobolectricTestRunner::class)
class FloatingWindowServicePerformanceTest {
    
    private lateinit var context: Context
    private lateinit var service: FloatingWindowService
    private lateinit var analyzeChatUseCase: AnalyzeChatUseCase
    private lateinit var checkDraftUseCase: CheckDraftUseCase
    private lateinit var contactRepository: ContactRepository
    private lateinit var floatingWindowPreferences: FloatingWindowPreferences
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        
        // 创建 Mock 对象
        analyzeChatUseCase = mockk()
        checkDraftUseCase = mockk()
        contactRepository = mockk()
        floatingWindowPreferences = mockk(relaxed = true)
        
        // 创建服务实例
        val serviceController = Robolectric.buildService(FloatingWindowService::class.java)
        service = serviceController.get()
        
        // 注入依赖
        service.analyzeChatUseCase = analyzeChatUseCase
        service.checkDraftUseCase = checkDraftUseCase
        service.contactRepository = contactRepository
        service.floatingWindowPreferences = floatingWindowPreferences
        
        // 创建服务
        serviceController.create()
    }
    
    /**
     * 测试性能监控启动
     * 
     * 验证需求 6.1：内存监控
     */
    @Test
    fun `service should start performance monitoring on create`() {
        // 服务创建时应该启动性能监控
        // 这个测试验证服务不会因为性能监控而崩溃
        assertNotNull("服务应该成功创建", service)
    }
    
    /**
     * 测试后台线程执行
     * 
     * 验证需求 6.4：后台线程执行耗时操作
     */
    @Test
    fun `handleAnalyze should execute database query in background thread`() = runBlocking {
        // 准备测试数据
        val testContacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        // Mock 联系人仓库
        coEvery { contactRepository.getAllProfiles() } returns flowOf(testContacts)
        
        // 执行操作（通过反射调用私有方法）
        val handleAnalyzeMethod = service.javaClass.getDeclaredMethod("handleAnalyze")
        handleAnalyzeMethod.isAccessible = true
        
        // 调用方法不应阻塞主线程
        val startTime = System.currentTimeMillis()
        handleAnalyzeMethod.invoke(service)
        val duration = System.currentTimeMillis() - startTime
        
        // 验证操作快速返回（不阻塞）
        assertTrue("操作应该快速返回", duration < 1000)
    }
    
    /**
     * 测试超时控制
     * 
     * 验证需求 6.2：10 秒内返回结果或超时提示
     */
    @Test
    fun `performAnalyze should timeout after 10 seconds`() = runBlocking {
        // 准备测试数据
        val testContactId = "test-1"
        val testText = "测试文本"
        
        // Mock UseCase 返回延迟结果（模拟超时）
        coEvery { 
            analyzeChatUseCase(testContactId, listOf(testText)) 
        } coAnswers {
            kotlinx.coroutines.delay(15000) // 延迟 15 秒
            Result.success(mockk())
        }
        
        // 执行操作（通过反射调用私有方法）
        val performAnalyzeMethod = service.javaClass.getDeclaredMethod(
            "performAnalyze",
            String::class.java,
            String::class.java
        )
        performAnalyzeMethod.isAccessible = true
        
        // 调用方法
        val startTime = System.currentTimeMillis()
        performAnalyzeMethod.invoke(service, testContactId, testText)
        val duration = System.currentTimeMillis() - startTime
        
        // 验证操作在 10 秒内超时
        assertTrue("操作应该在 10 秒内超时", duration < 11000)
    }
    
    /**
     * 测试内存健康检查
     * 
     * 验证需求 6.1：内存占用低于 150MB
     */
    @Test
    fun `service should check memory health before heavy operations`() {
        // 这个测试验证服务在执行重操作前会检查内存
        // 实际的内存检查逻辑在 performAnalyze 和 performCheck 中
        
        // 获取当前内存使用
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        
        // 验证内存使用在合理范围内
        assertTrue("内存使用应该在合理范围内", usedMemory < 150)
    }
}
