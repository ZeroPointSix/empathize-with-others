package com.empathy.ai.presentation.ui.screen.contact.summary

import com.empathy.ai.domain.model.GenerationSource
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SummarySourceBadge 组件单元测试
 *
 * 测试GenerationSource枚举的行为，不测试Composable函数本身
 */
class SummarySourceBadgeTest {

    @Test
    fun `自动生成来源应该显示正确的图标和文本`() {
        val source = GenerationSource.AUTO
        assertEquals("🤖", source.icon)
        assertEquals("自动", source.displayName)
    }

    @Test
    fun `手动生成来源应该显示正确的图标和文本`() {
        val source = GenerationSource.MANUAL
        assertEquals("👤", source.icon)
        assertEquals("手动", source.displayName)
    }

    @Test
    fun `GenerationSource枚举应该包含2个值`() {
        val sources = GenerationSource.entries
        assertEquals(2, sources.size)
    }
}
