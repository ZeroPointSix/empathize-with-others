package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * PersonaSearchState 模型单元测试
 */
class PersonaSearchStateTest {

    @Test
    fun `初始状态_所有字段为默认值`() {
        val state = PersonaSearchState()

        assertEquals("", state.query)
        assertFalse(state.isSearching)
        assertFalse(state.hasQuery)
    }

    @Test
    fun `hasQuery_有非空查询返回true`() {
        val state = PersonaSearchState(query = "测试")

        assertTrue(state.hasQuery)
    }

    @Test
    fun `hasQuery_空查询返回false`() {
        val state = PersonaSearchState(query = "")

        assertFalse(state.hasQuery)
    }

    @Test
    fun `hasQuery_空白查询返回false`() {
        val state = PersonaSearchState(query = "   ")

        assertFalse(state.hasQuery)
    }

    @Test
    fun `updateQuery_更新查询并设置isSearching为true`() {
        val state = PersonaSearchState()

        val newState = state.updateQuery("性格")

        assertEquals("性格", newState.query)
        assertTrue(newState.isSearching)
    }

    @Test
    fun `updateQuery_空查询设置isSearching为false`() {
        val state = PersonaSearchState(query = "测试", isSearching = true)

        val newState = state.updateQuery("")

        assertEquals("", newState.query)
        assertFalse(newState.isSearching)
    }

    @Test
    fun `updateQuery_空白查询设置isSearching为false`() {
        val state = PersonaSearchState(query = "测试", isSearching = true)

        val newState = state.updateQuery("   ")

        assertEquals("   ", newState.query)
        assertFalse(newState.isSearching)
    }

    @Test
    fun `clear_重置为初始状态`() {
        val state = PersonaSearchState(
            query = "测试查询",
            isSearching = true
        )

        val newState = state.clear()

        assertEquals("", newState.query)
        assertFalse(newState.isSearching)
        assertFalse(newState.hasQuery)
    }

    @Test
    fun `searchCompleted_设置isSearching为false`() {
        val state = PersonaSearchState(
            query = "测试",
            isSearching = true
        )

        val newState = state.searchCompleted()

        assertEquals("测试", newState.query)
        assertFalse(newState.isSearching)
    }

    @Test
    fun `searchCompleted_保持query不变`() {
        val state = PersonaSearchState(
            query = "性格特点",
            isSearching = true
        )

        val newState = state.searchCompleted()

        assertEquals("性格特点", newState.query)
    }

    @Test
    fun `连续更新查询_状态正确变化`() {
        var state = PersonaSearchState()

        // 第一次更新
        state = state.updateQuery("性")
        assertEquals("性", state.query)
        assertTrue(state.isSearching)

        // 第二次更新
        state = state.updateQuery("性格")
        assertEquals("性格", state.query)
        assertTrue(state.isSearching)

        // 搜索完成
        state = state.searchCompleted()
        assertEquals("性格", state.query)
        assertFalse(state.isSearching)

        // 清除
        state = state.clear()
        assertEquals("", state.query)
        assertFalse(state.isSearching)
    }

    @Test
    fun `data_class_equals_正确比较`() {
        val state1 = PersonaSearchState(query = "测试", isSearching = true)
        val state2 = PersonaSearchState(query = "测试", isSearching = true)
        val state3 = PersonaSearchState(query = "其他", isSearching = true)

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }

    @Test
    fun `data_class_copy_正确复制`() {
        val original = PersonaSearchState(query = "原始", isSearching = true)
        val copied = original.copy(query = "复制")

        assertEquals("复制", copied.query)
        assertTrue(copied.isSearching)
    }
}
