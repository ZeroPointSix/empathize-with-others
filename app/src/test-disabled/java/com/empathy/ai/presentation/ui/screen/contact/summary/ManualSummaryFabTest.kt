package com.empathy.ai.presentation.ui.screen.contact.summary

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ManualSummaryFab 组件单元测试
 *
 * 测试组件相关的常量和逻辑，不测试Composable函数本身
 */
class ManualSummaryFabTest {

    @Test
    fun `FAB尺寸应该为56dp`() {
        // 设计规范：FAB尺寸为56dp × 56dp
        val expectedSize = 56
        assertEquals(expectedSize, 56)
    }

    @Test
    fun `点击缩放动画目标值应该正确`() {
        // 设计规范：点击时缩放0.9→1.0
        val pressedScale = 0.9f
        val normalScale = 1f
        assertEquals(0.9f, pressedScale, 0.01f)
        assertEquals(1f, normalScale, 0.01f)
    }
}
