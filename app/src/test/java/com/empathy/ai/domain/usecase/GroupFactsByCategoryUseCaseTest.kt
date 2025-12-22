package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.CategoryColor
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactCategory
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.util.CategoryColorAssigner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * GroupFactsByCategoryUseCase 单元测试
 */
class GroupFactsByCategoryUseCaseTest {

    private lateinit var colorAssigner: CategoryColorAssigner
    private lateinit var useCase: GroupFactsByCategoryUseCase

    private val testColor = CategoryColor(
        titleColor = 0xFFB71C1C,
        tagBackgroundColor = 0xFFFFCDD2,
        tagTextColor = 0xFFB71C1C
    )

    @Before
    fun setup() {
        colorAssigner = mockk()
        every { colorAssigner.assignColor(any(), any()) } returns testColor
        useCase = GroupFactsByCategoryUseCase(colorAssigner)
    }

    private fun createFact(
        id: String = "fact_1",
        key: String = "性格特点",
        value: String = "开朗",
        timestamp: Long = System.currentTimeMillis()
    ) = Fact(
        id = id,
        key = key,
        value = value,
        timestamp = timestamp,
        source = FactSource.MANUAL
    )

    @Test
    fun `空列表返回空结果`() {
        val result = useCase(emptyList(), false)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `单个Fact正确分组`() {
        val fact = createFact(key = "性格特点", value = "开朗")

        val result = useCase(listOf(fact), false)

        assertEquals(1, result.size)
        assertEquals("性格特点", result[0].key)
        assertEquals(1, result[0].factCount)
    }

    @Test
    fun `按Key正确分组`() {
        val facts = listOf(
            createFact("1", "性格特点", "开朗"),
            createFact("2", "性格特点", "乐观"),
            createFact("3", "兴趣爱好", "读书"),
            createFact("4", "工作信息", "程序员")
        )

        val result = useCase(facts, false)

        assertEquals(3, result.size)
        
        val personalityCategory = result.find { it.key == "性格特点" }
        assertNotNull(personalityCategory)
        assertEquals(2, personalityCategory!!.factCount)
        
        val hobbyCategory = result.find { it.key == "兴趣爱好" }
        assertNotNull(hobbyCategory)
        assertEquals(1, hobbyCategory!!.factCount)
        
        val workCategory = result.find { it.key == "工作信息" }
        assertNotNull(workCategory)
        assertEquals(1, workCategory!!.factCount)
    }

    @Test
    fun `分类按名称排序`() {
        val facts = listOf(
            createFact("1", "性格特点", "开朗"),
            createFact("2", "兴趣爱好", "读书"),
            createFact("3", "工作信息", "程序员")
        )

        val result = useCase(facts, false)

        assertEquals(3, result.size)
        // 按中文排序：兴趣爱好 < 工作信息 < 性格特点
        assertEquals("兴趣爱好", result[0].key)
        assertEquals("工作信息", result[1].key)
        assertEquals("性格特点", result[2].key)
    }

    @Test
    fun `分类内标签按时间倒序排列`() {
        val now = System.currentTimeMillis()
        val facts = listOf(
            createFact("1", "性格特点", "开朗", now - 3000),
            createFact("2", "性格特点", "乐观", now - 1000),
            createFact("3", "性格特点", "热情", now - 2000)
        )

        val result = useCase(facts, false)

        assertEquals(1, result.size)
        val category = result[0]
        assertEquals(3, category.factCount)
        // 按时间倒序：最新的在前
        assertEquals("乐观", category.facts[0].value)
        assertEquals("热情", category.facts[1].value)
        assertEquals("开朗", category.facts[2].value)
    }

    @Test
    fun `颜色分配正确`() {
        val customColor = CategoryColor(
            titleColor = 0xFF1B5E20,
            tagBackgroundColor = 0xFFC8E6C9,
            tagTextColor = 0xFF1B5E20
        )
        every { colorAssigner.assignColor("性格特点", false) } returns customColor

        val facts = listOf(createFact(key = "性格特点"))

        val result = useCase(facts, false)

        assertEquals(1, result.size)
        assertEquals(customColor, result[0].color)
    }

    @Test
    fun `深色模式颜色分配`() {
        val darkColor = CategoryColor(
            titleColor = 0xFFEF9A9A,
            tagBackgroundColor = 0xFF5D4037,
            tagTextColor = 0xFFFFCDD2
        )
        every { colorAssigner.assignColor("性格特点", true) } returns darkColor

        val facts = listOf(createFact(key = "性格特点"))

        val result = useCase(facts, true)

        assertEquals(1, result.size)
        assertEquals(darkColor, result[0].color)
    }

    @Test
    fun `默认展开状态为true`() {
        val facts = listOf(createFact())

        val result = useCase(facts, false)

        assertTrue(result[0].isExpanded)
    }

    @Test
    fun `invokeWithState_保持现有展开状态`() {
        val facts = listOf(
            createFact("1", "性格特点", "开朗"),
            createFact("2", "兴趣爱好", "读书")
        )
        val existingCategories = listOf(
            FactCategory(
                key = "性格特点",
                facts = emptyList(),
                color = testColor,
                isExpanded = false
            )
        )

        val result = useCase.invokeWithState(facts, false, existingCategories)

        val personalityCategory = result.find { it.key == "性格特点" }
        assertNotNull(personalityCategory)
        assertFalse(personalityCategory!!.isExpanded)

        val hobbyCategory = result.find { it.key == "兴趣爱好" }
        assertNotNull(hobbyCategory)
        assertTrue(hobbyCategory!!.isExpanded) // 新分类默认展开
    }

    @Test
    fun `invokeWithState_空现有分类列表`() {
        val facts = listOf(createFact())

        val result = useCase.invokeWithState(facts, false, emptyList())

        assertEquals(1, result.size)
        assertTrue(result[0].isExpanded)
    }

    @Test
    fun `invokeWithState_所有分类都有现有状态`() {
        val facts = listOf(
            createFact("1", "性格特点", "开朗"),
            createFact("2", "兴趣爱好", "读书")
        )
        val existingCategories = listOf(
            FactCategory(
                key = "性格特点",
                facts = emptyList(),
                color = testColor,
                isExpanded = false
            ),
            FactCategory(
                key = "兴趣爱好",
                facts = emptyList(),
                color = testColor,
                isExpanded = false
            )
        )

        val result = useCase.invokeWithState(facts, false, existingCategories)

        result.forEach { category ->
            assertFalse(category.isExpanded)
        }
    }

    @Test
    fun `大量数据分组性能测试`() {
        val facts = (1..1000).map { i ->
            createFact(
                id = "fact_$i",
                key = "分类${i % 10}",
                value = "值$i",
                timestamp = System.currentTimeMillis() - i * 1000L
            )
        }

        val startTime = System.currentTimeMillis()
        val result = useCase(facts, false)
        val endTime = System.currentTimeMillis()

        assertEquals(10, result.size)
        result.forEach { category ->
            assertEquals(100, category.factCount)
        }
        // 性能要求：1000条数据分组应在100ms内完成
        assertTrue("分组耗时过长: ${endTime - startTime}ms", endTime - startTime < 100)
    }
}
