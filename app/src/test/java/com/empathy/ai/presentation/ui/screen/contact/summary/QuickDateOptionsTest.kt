package com.empathy.ai.presentation.ui.screen.contact.summary

import com.empathy.ai.presentation.viewmodel.QuickDateOption
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * QuickDateOptions 组件单元测试
 *
 * 测试快捷选项枚举的行为，不测试Composable函数本身
 */
class QuickDateOptionsTest {

    @Test
    fun `快捷选项枚举应该包含5个选项`() {
        val options = QuickDateOption.entries
        assertEquals(5, options.size)
    }

    @Test
    fun `快捷选项显示名称应该正确`() {
        assertEquals("最近7天", QuickDateOption.LAST_7_DAYS.displayName)
        assertEquals("本月", QuickDateOption.THIS_MONTH.displayName)
        assertEquals("上月", QuickDateOption.LAST_MONTH.displayName)
        assertEquals("最近30天", QuickDateOption.LAST_30_DAYS.displayName)
        assertEquals("未总结时段", QuickDateOption.MISSING_DATES.displayName)
    }
}
