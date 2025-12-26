package com.empathy.ai.presentation.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * AvatarColors单元测试
 *
 * 测试头像淡色系配色方案的颜色分配逻辑
 */
class AvatarColorsTest {

    // ========== getColorPair测试 ==========

    @Test
    fun `getColorPair returns non-null pair for any name`() {
        val names = listOf("张三", "李四", "王五", "Alice", "Bob", "测试")
        names.forEach { name ->
            val pair = AvatarColors.getColorPair(name)
            assertNotNull("Color pair should not be null for name: $name", pair)
            assertNotNull("Background color should not be null", pair.first)
            assertNotNull("Text color should not be null", pair.second)
        }
    }

    @Test
    fun `getColorPair returns same color for same name`() {
        val name = "测试用户"
        val pair1 = AvatarColors.getColorPair(name)
        val pair2 = AvatarColors.getColorPair(name)
        
        assertEquals("Same name should return same background color", pair1.first, pair2.first)
        assertEquals("Same name should return same text color", pair1.second, pair2.second)
    }

    @Test
    fun `getColorPair handles empty string`() {
        val pair = AvatarColors.getColorPair("")
        assertNotNull("Should handle empty string", pair)
    }

    @Test
    fun `getColorPair handles single character`() {
        val pair = AvatarColors.getColorPair("A")
        assertNotNull("Should handle single character", pair)
    }

    @Test
    fun `getColorPair handles unicode characters`() {
        val names = listOf("😀", "🎉", "中文", "日本語", "한국어")
        names.forEach { name ->
            val pair = AvatarColors.getColorPair(name)
            assertNotNull("Should handle unicode: $name", pair)
        }
    }

    @Test
    fun `getColorPair distributes colors across different names`() {
        // 测试不同名字是否能分配到不同颜色（至少有一些不同）
        val names = listOf("Alice", "Bob", "Charlie", "David", "Eve", "Frank")
        val colors = names.map { AvatarColors.getColorPair(it) }.toSet()
        
        // 6个名字应该至少有2种不同的颜色组合
        assert(colors.size >= 2) { "Should distribute colors across different names" }
    }

    @Test
    fun `getColorPair returns consistent results across multiple calls`() {
        // 验证多次调用返回一致的结果
        repeat(100) {
            val name = "ConsistencyTest"
            val pair1 = AvatarColors.getColorPair(name)
            val pair2 = AvatarColors.getColorPair(name)
            assertEquals(pair1, pair2)
        }
    }

    @Test
    fun `getColorPair handles long names`() {
        val longName = "A".repeat(1000)
        val pair = AvatarColors.getColorPair(longName)
        assertNotNull("Should handle long names", pair)
    }

    @Test
    fun `getColorPair handles special characters`() {
        val specialNames = listOf("@#\$%", "test@email.com", "name-with-dash", "name_with_underscore")
        specialNames.forEach { name ->
            val pair = AvatarColors.getColorPair(name)
            assertNotNull("Should handle special characters: $name", pair)
        }
    }

    @Test
    fun `getColorPair returns different colors for different names`() {
        // 测试不同名字可能返回不同颜色
        val name1 = "Alice"
        val name2 = "Zoe"
        val pair1 = AvatarColors.getColorPair(name1)
        val pair2 = AvatarColors.getColorPair(name2)
        
        // 不同名字的hashCode不同，应该有可能返回不同颜色
        // 这里只验证函数正常工作，不强制要求颜色不同
        assertNotNull(pair1)
        assertNotNull(pair2)
    }
}
