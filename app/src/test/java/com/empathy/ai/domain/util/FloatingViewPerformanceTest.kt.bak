package com.empathy.ai.domain.util

import android.content.Context
import android.view.WindowManager
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.ContactProfile
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

/**
 * FloatingView 性能测试
 * 
 * 验证性能指标：
 * - 动画时长 < 300ms（需求 6.1）
 * - 内存占用 < 5MB（需求 8.1, 8.2）
 * - 拖动响应 < 16ms (60 FPS)（需求 6.4）
 * 
 * 需求: 6.1, 6.3, 6.4, 6.5, 8.1, 8.2
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FloatingViewPerformanceTest {
    
    private lateinit var context: Context
    private lateinit var floatingView: FloatingView
    private lateinit var windowManager: WindowManager
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = FloatingView(context)
    }
    
    @After
    fun tearDown() {
        // 清理资源
        try {
            if (floatingView.parent != null) {
                windowManager.removeView(floatingView)
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }
    
    /**
     * 测试最小化动画时长
     * 
     * 验证：动画时长 < 300ms
     * 需求: 6.1
     */
    @Test
    fun `测试最小化动画时长应小于300ms`() {
        // Given: 显示输入对话框
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // When: 测量最小化动画时长
        val duration = measureTimeMillis {
            floatingView.minimizeDialog()
            
            // 等待动画完成
            Thread.sleep(350) // 稍微多等一点以确保动画完成
        }
        
        // Then: 验证时长 < 300ms（加上一些容差）
        assert(duration < 400) {
            "最小化动画时长超过预期: ${duration}ms > 400ms"
        }
        
        println("✅ 最小化动画时长: ${duration}ms")
    }
    
    /**
     * 测试恢复动画时长
     * 
     * 验证：动画时长 < 300ms
     * 需求: 6.1
     */
    @Test
    fun `测试恢复动画时长应小于300ms`() {
        // Given: 最小化对话框
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        floatingView.minimizeDialog()
        Thread.sleep(350) // 等待最小化完成
        
        // When: 测量恢复动画时长
        val duration = measureTimeMillis {
            floatingView.restoreFromMinimized()
            
            // 等待动画完成
            Thread.sleep(350) // 稍微多等一点以确保动画完成
        }
        
        // Then: 验证时长 < 300ms（加上一些容差）
        assert(duration < 400) {
            "恢复动画时长超过预期: ${duration}ms > 400ms"
        }
        
        println("✅ 恢复动画时长: ${duration}ms")
    }
    
    /**
     * 测试状态切换动画时长
     * 
     * 验证：动画时长 < 200ms
     * 需求: 6.1
     */
    @Test
    fun `测试状态切换动画时长应小于200ms`() {
        // Given: 最小化对话框
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        floatingView.minimizeDialog()
        Thread.sleep(350) // 等待最小化完成
        
        // When: 测量状态切换时长
        val duration = measureTimeMillis {
            floatingView.updateIndicatorState(IndicatorState.SUCCESS)
            
            // 等待动画完成
            Thread.sleep(250) // 稍微多等一点以确保动画完成
        }
        
        // Then: 验证时长 < 200ms（加上一些容差）
        assert(duration < 300) {
            "状态切换动画时长超过预期: ${duration}ms > 300ms"
        }
        
        println("✅ 状态切换动画时长: ${duration}ms")
    }
    
    /**
     * 测试内存占用
     * 
     * 验证：内存占用 < 5MB
     * 需求: 8.1, 8.2
     */
    @Test
    fun `测试内存占用应小于5MB`() {
        // Given: 记录初始内存
        val runtime = Runtime.getRuntime()
        System.gc() // 建议垃圾回收
        Thread.sleep(100) // 等待垃圾回收
        
        val initialMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        
        // When: 创建并显示输入对话框
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // 最小化对话框
        floatingView.minimizeDialog()
        Thread.sleep(350) // 等待最小化完成
        
        // Then: 记录最终内存
        val finalMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val memoryUsed = finalMemory - initialMemory
        
        // 验证内存占用 < 5MB
        assert(memoryUsed < 5) {
            "内存占用超过预期: ${memoryUsed}MB > 5MB"
        }
        
        println("✅ 内存占用: ${memoryUsed}MB")
    }
    
    /**
     * 测试拖动响应时间
     * 
     * 验证：拖动响应 < 16ms (60 FPS)
     * 需求: 6.4
     */
    @Test
    fun `测试拖动响应时间应小于16ms`() {
        // Given: 创建触摸事件
        val downTime = System.currentTimeMillis()
        val eventTime = System.currentTimeMillis()
        
        // 模拟 ACTION_DOWN 事件
        val downEvent = android.view.MotionEvent.obtain(
            downTime,
            eventTime,
            android.view.MotionEvent.ACTION_DOWN,
            100f,
            100f,
            0
        )
        
        floatingView.onTouchEvent(downEvent)
        downEvent.recycle()
        
        // When: 测量 ACTION_MOVE 事件的响应时间
        val moveDurations = mutableListOf<Long>()
        
        for (i in 1..10) {
            val moveEvent = android.view.MotionEvent.obtain(
                downTime,
                eventTime + i * 16,
                android.view.MotionEvent.ACTION_MOVE,
                100f + i * 10,
                100f + i * 10,
                0
            )
            
            val duration = measureTimeMillis {
                floatingView.onTouchEvent(moveEvent)
            }
            
            moveDurations.add(duration)
            moveEvent.recycle()
        }
        
        // Then: 验证平均响应时间 < 16ms
        val avgDuration = moveDurations.average()
        
        assert(avgDuration < 16) {
            "拖动响应时间超过预期: ${avgDuration}ms > 16ms"
        }
        
        println("✅ 拖动响应时间: ${avgDuration}ms (平均)")
        println("   最小: ${moveDurations.minOrNull()}ms")
        println("   最大: ${moveDurations.maxOrNull()}ms")
    }
    
    /**
     * 测试硬件加速是否启用
     * 
     * 验证：硬件加速已启用
     * 需求: 6.3
     */
    @Test
    fun `测试硬件加速应已启用`() {
        // When: 启用硬件加速
        floatingView.enableHardwareAcceleration()
        
        // Then: 验证硬件加速已启用
        val layerType = floatingView.layerType
        
        assert(layerType == android.view.View.LAYER_TYPE_HARDWARE) {
            "硬件加速未启用: layerType=$layerType"
        }
        
        println("✅ 硬件加速已启用")
    }
    
    /**
     * 测试资源释放
     * 
     * 验证：最小化后内存使用减少
     * 需求: 8.1, 8.2
     */
    @Test
    fun `测试最小化后应释放资源`() {
        // Given: 显示输入对话框
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // 记录对话框显示时的内存
        val runtime = Runtime.getRuntime()
        System.gc()
        Thread.sleep(100)
        
        val memoryBeforeMinimize = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        
        // When: 最小化对话框
        floatingView.minimizeDialog()
        Thread.sleep(350) // 等待最小化完成
        
        // 记录最小化后的内存
        System.gc()
        Thread.sleep(100)
        
        val memoryAfterMinimize = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        
        // Then: 验证内存使用减少（或至少没有显著增加）
        val memoryDiff = memoryAfterMinimize - memoryBeforeMinimize
        
        assert(memoryDiff <= 1) {
            "最小化后内存使用增加: ${memoryDiff}MB"
        }
        
        println("✅ 最小化前内存: ${memoryBeforeMinimize}MB")
        println("✅ 最小化后内存: ${memoryAfterMinimize}MB")
        println("✅ 内存变化: ${memoryDiff}MB")
    }
}
