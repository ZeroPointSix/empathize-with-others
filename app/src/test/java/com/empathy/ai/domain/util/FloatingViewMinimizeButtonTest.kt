package com.empathy.ai.domain.util

import android.content.Context
import android.os.Build
import com.empathy.ai.R
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.ContactProfile
import com.google.android.material.button.MaterialButton
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * FloatingView 最小化按钮测试
 * 
 * 验证输入对话框中的最小化按钮功能
 * 
 * **验证需求：1.1**
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class FloatingViewMinimizeButtonTest {
    
    private lateinit var context: Context
    private lateinit var floatingView: FloatingView
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        floatingView = FloatingView(context)
    }
    
    @After
    fun tearDown() {
        // 清理资源
    }
    
    /**
     * 测试用例 1：输入对话框包含最小化按钮
     * 
     * 验证：显示输入对话框时，应该包含最小化按钮
     * 
     * **验证需求：1.1**
     */
    @Test
    fun `showInputDialog should include minimize button`() {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        // When: 显示输入对话框
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // Then: 应该能找到最小化按钮
        val btnMinimize = floatingView.findViewById<MaterialButton>(R.id.btn_minimize)
        assertNotNull("最小化按钮应该存在", btnMinimize)
        assertTrue("最小化按钮应该可见", btnMinimize.visibility == android.view.View.VISIBLE)
    }
    
    /**
     * 测试用例 2：点击最小化按钮触发回调
     * 
     * 验证：点击最小化按钮时，应该触发 onMinimizeClicked 回调
     * 
     * 说明：回调会根据是否有正在处理的请求来决定行为：
     * - 没有请求：关闭对话框，返回悬浮按钮
     * - 有请求：显示加载指示器
     * 
     * **验证需求：1.1**
     */
    @Test
    fun `clicking minimize button should trigger callback`() {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        // Given: 设置回调
        var minimizeClicked = false
        floatingView.onMinimizeClicked = {
            minimizeClicked = true
        }
        
        // When: 显示输入对话框
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // When: 点击最小化按钮
        val btnMinimize = floatingView.findViewById<MaterialButton>(R.id.btn_minimize)
        btnMinimize?.performClick()
        
        // Then: 回调应该被触发
        assertTrue("点击最小化按钮应该触发回调", minimizeClicked)
    }
    
    /**
     * 测试用例 3：最小化按钮在标题栏右侧
     * 
     * 验证：最小化按钮应该位于标题栏的右侧
     * 
     * **验证需求：1.1**
     */
    @Test
    fun `minimize button should be positioned in title bar`() {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        // When: 显示输入对话框
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // Then: 应该能找到标题和最小化按钮
        val dialogTitle = floatingView.findViewById<android.widget.TextView>(R.id.dialog_title)
        val btnMinimize = floatingView.findViewById<MaterialButton>(R.id.btn_minimize)
        
        assertNotNull("标题应该存在", dialogTitle)
        assertNotNull("最小化按钮应该存在", btnMinimize)
        
        // 验证它们在同一个父容器中（标题栏）
        assertEquals("标题和最小化按钮应该在同一个父容器中", 
            dialogTitle?.parent, btnMinimize?.parent)
    }
    
    /**
     * 测试用例 4：更新对话框后最小化按钮仍然可用
     * 
     * 验证：多次显示输入对话框时，最小化按钮应该始终可用
     * 
     * **验证需求：1.1**
     */
    @Test
    fun `minimize button should work after dialog update`() {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "test-1",
                name = "测试联系人",
                targetGoal = "测试目标"
            )
        )
        
        // Given: 设置回调
        var minimizeClickCount = 0
        floatingView.onMinimizeClicked = {
            minimizeClickCount++
        }
        
        // When: 第一次显示输入对话框
        floatingView.showInputDialog(
            actionType = ActionType.ANALYZE,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // When: 点击最小化按钮
        val btnMinimize1 = floatingView.findViewById<MaterialButton>(R.id.btn_minimize)
        btnMinimize1?.performClick()
        
        // When: 第二次显示输入对话框（更新）
        floatingView.showInputDialog(
            actionType = ActionType.CHECK,
            contacts = contacts,
            onConfirm = { _, _ -> }
        )
        
        // When: 再次点击最小化按钮
        val btnMinimize2 = floatingView.findViewById<MaterialButton>(R.id.btn_minimize)
        btnMinimize2?.performClick()
        
        // Then: 回调应该被触发两次
        assertEquals("最小化按钮应该在更新后仍然可用", 2, minimizeClickCount)
    }
}
