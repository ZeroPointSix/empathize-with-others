package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * GenerationSource 单元测试
 */
class GenerationSourceTest {

    @Test
    fun `AUTO来源的displayName应该是自动`() {
        assertEquals("自动", GenerationSource.AUTO.displayName)
    }

    @Test
    fun `MANUAL来源的displayName应该是手动`() {
        assertEquals("手动", GenerationSource.MANUAL.displayName)
    }

    @Test
    fun `AUTO来源的icon应该是机器人`() {
        assertEquals("🤖", GenerationSource.AUTO.icon)
    }

    @Test
    fun `MANUAL来源的icon应该是人`() {
        assertEquals("👤", GenerationSource.MANUAL.icon)
    }

    @Test
    fun `应该有两种生成来源`() {
        assertEquals(2, GenerationSource.entries.size)
    }

    @Test
    fun `valueOf应该正确解析AUTO`() {
        assertEquals(GenerationSource.AUTO, GenerationSource.valueOf("AUTO"))
    }

    @Test
    fun `valueOf应该正确解析MANUAL`() {
        assertEquals(GenerationSource.MANUAL, GenerationSource.valueOf("MANUAL"))
    }
}
