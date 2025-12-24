package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * FactCategory 模型单元测试
 */
class FactCategoryTest {

    private val testColor = CategoryColor(
        titleColor = 0xFFB71C1C,
        tagBackgroundColor = 0xFFFFCDD2,
        tagTextColor = 0xFFB71C1C
    )

    private fun createTestFact(id: String = "fact_1", key: String = "性格特点", value: String = "开朗") = Fact(
        id = id,
        key = key,
        value = value,
        timestamp = System.currentTimeMillis(),
        source = FactSource.MANUAL
    )

    @Test
    fun `factCount_返回正确的标签数量`() {
        val facts = listOf(
            createTestFact("1"),
            createTestFact("2"),
            createTestFact("3")
        )
        val category = FactCategory(
            key = "性格特点",
            facts = facts,
            color = testColor
        )

        assertEquals(3, category.factCount)
    }

    @Test
    fun `factCount_空列表返回0`() {
        val category = FactCategory(
            key = "性格特点",
            facts = emptyList(),
            color = testColor
        )

        assertEquals(0, category.factCount)
    }

    @Test
    fun `isEmpty_空列表返回true`() {
        val category = FactCategory(
            key = "性格特点",
            facts = emptyList(),
            color = testColor
        )

        assertTrue(category.isEmpty)
    }

    @Test
    fun `isEmpty_非空列表返回false`() {
        val category = FactCategory(
            key = "性格特点",
            facts = listOf(createTestFact()),
            color = testColor
        )

        assertFalse(category.isEmpty)
    }

    @Test
    fun `getFactIds_返回所有Fact的ID`() {
        val facts = listOf(
            createTestFact("id_1"),
            createTestFact("id_2"),
            createTestFact("id_3")
        )
        val category = FactCategory(
            key = "性格特点",
            facts = facts,
            color = testColor
        )

        val ids = category.getFactIds()

        assertEquals(3, ids.size)
        assertTrue(ids.contains("id_1"))
        assertTrue(ids.contains("id_2"))
        assertTrue(ids.contains("id_3"))
    }

    @Test
    fun `getFactIds_空列表返回空列表`() {
        val category = FactCategory(
            key = "性格特点",
            facts = emptyList(),
            color = testColor
        )

        assertTrue(category.getFactIds().isEmpty())
    }

    @Test
    fun `containsFact_存在的ID返回true`() {
        val facts = listOf(
            createTestFact("id_1"),
            createTestFact("id_2")
        )
        val category = FactCategory(
            key = "性格特点",
            facts = facts,
            color = testColor
        )

        assertTrue(category.containsFact("id_1"))
        assertTrue(category.containsFact("id_2"))
    }

    @Test
    fun `containsFact_不存在的ID返回false`() {
        val facts = listOf(createTestFact("id_1"))
        val category = FactCategory(
            key = "性格特点",
            facts = facts,
            color = testColor
        )

        assertFalse(category.containsFact("id_999"))
    }

    @Test
    fun `toggleExpanded_从展开切换到折叠`() {
        val category = FactCategory(
            key = "性格特点",
            facts = listOf(createTestFact()),
            color = testColor,
            isExpanded = true
        )

        val toggled = category.toggleExpanded()

        assertFalse(toggled.isExpanded)
    }

    @Test
    fun `toggleExpanded_从折叠切换到展开`() {
        val category = FactCategory(
            key = "性格特点",
            facts = listOf(createTestFact()),
            color = testColor,
            isExpanded = false
        )

        val toggled = category.toggleExpanded()

        assertTrue(toggled.isExpanded)
    }

    @Test
    fun `toggleExpanded_保持其他属性不变`() {
        val facts = listOf(createTestFact())
        val category = FactCategory(
            key = "性格特点",
            facts = facts,
            color = testColor,
            isExpanded = true
        )

        val toggled = category.toggleExpanded()

        assertEquals(category.key, toggled.key)
        assertEquals(category.facts, toggled.facts)
        assertEquals(category.color, toggled.color)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `构造函数_空key抛出异常`() {
        FactCategory(
            key = "",
            facts = emptyList(),
            color = testColor
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `构造函数_空白key抛出异常`() {
        FactCategory(
            key = "   ",
            facts = emptyList(),
            color = testColor
        )
    }

    @Test
    fun `默认isExpanded为true`() {
        val category = FactCategory(
            key = "性格特点",
            facts = emptyList(),
            color = testColor
        )

        assertTrue(category.isExpanded)
    }
}
