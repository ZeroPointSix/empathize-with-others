package com.empathy.ai.presentation.ui.screen.contact.factstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FactStreamTab 单元测试
 * 
 * TD-00020 T069: 测试视图切换、筛选功能、时光轴渲染
 * 
 * 关键测试场景:
 * - 时光轴/清单视图切换
 * - 情绪类型筛选（全部/甜蜜/冲突等）
 * - 空数据状态显示
 */
class FactStreamTabTest {

    // ============================================================
    // ViewMode 测试
    // ============================================================

    @Test
    fun `ViewMode enum has correct values`() {
        val modes = ViewMode.values()
        
        assertEquals(2, modes.size)
        assertTrue(modes.contains(ViewMode.TIMELINE))
        assertTrue(modes.contains(ViewMode.LIST))
    }

    @Test
    fun `ViewMode TIMELINE is default`() {
        val state = FactStreamUiState()
        
        assertEquals(ViewMode.TIMELINE, state.viewMode)
    }

    // ============================================================
    // FilterType 测试
    // ============================================================

    @Test
    fun `FilterType enum has correct values`() {
        val filters = FilterType.values()
        
        assertEquals(7, filters.size)
        assertTrue(filters.contains(FilterType.ALL))
        assertTrue(filters.contains(FilterType.SWEET))
        assertTrue(filters.contains(FilterType.CONFLICT))
        assertTrue(filters.contains(FilterType.NEUTRAL))
        assertTrue(filters.contains(FilterType.GIFT))
        assertTrue(filters.contains(FilterType.DATE))
        assertTrue(filters.contains(FilterType.DEEP_TALK))
    }

    @Test
    fun `FilterType ALL is default`() {
        val state = FactStreamUiState()
        
        assertEquals(FilterType.ALL, state.selectedFilter)
    }

    @Test
    fun `FilterType has correct display names`() {
        assertEquals("全部", FilterType.ALL.displayName)
        assertEquals("甜蜜", FilterType.SWEET.displayName)
        assertEquals("冲突", FilterType.CONFLICT.displayName)
        assertEquals("中性", FilterType.NEUTRAL.displayName)
        assertEquals("礼物", FilterType.GIFT.displayName)
        assertEquals("约会", FilterType.DATE.displayName)
        assertEquals("深谈", FilterType.DEEP_TALK.displayName)
    }

    // ============================================================
    // FactStreamUiState 测试
    // ============================================================

    @Test
    fun `FactStreamUiState default values are correct`() {
        val state = FactStreamUiState()
        
        assertEquals(ViewMode.TIMELINE, state.viewMode)
        assertEquals(FilterType.ALL, state.selectedFilter)
        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `FactStreamUiState with items is valid`() {
        val items = listOf(
            FactStreamItem(id = "1", content = "Item 1", emotionType = "SWEET", timestamp = 1000L),
            FactStreamItem(id = "2", content = "Item 2", emotionType = "CONFLICT", timestamp = 2000L)
        )
        val state = FactStreamUiState(items = items)
        
        assertEquals(2, state.items.size)
        assertTrue(state.hasItems)
    }

    @Test
    fun `FactStreamUiState with empty items has no items`() {
        val state = FactStreamUiState(items = emptyList())
        
        assertTrue(state.items.isEmpty())
        assertFalse(state.hasItems)
    }

    @Test
    fun `FactStreamUiState loading state is correct`() {
        val state = FactStreamUiState(isLoading = true)
        
        assertTrue(state.isLoading)
    }

    @Test
    fun `FactStreamUiState error state is correct`() {
        val errorMessage = "Failed to load facts"
        val state = FactStreamUiState(error = errorMessage)
        
        assertEquals(errorMessage, state.error)
        assertTrue(state.hasError)
    }

    // ============================================================
    // 筛选逻辑测试
    // ============================================================

    @Test
    fun `filteredItems returns all items when filter is ALL`() {
        val items = listOf(
            FactStreamItem(id = "1", content = "Sweet item", emotionType = "SWEET", timestamp = 1000L),
            FactStreamItem(id = "2", content = "Conflict item", emotionType = "CONFLICT", timestamp = 2000L),
            FactStreamItem(id = "3", content = "Neutral item", emotionType = "NEUTRAL", timestamp = 3000L)
        )
        val state = FactStreamUiState(items = items, selectedFilter = FilterType.ALL)
        
        assertEquals(3, state.filteredItems.size)
    }

    @Test
    fun `filteredItems returns only SWEET items when filter is SWEET`() {
        val items = listOf(
            FactStreamItem(id = "1", content = "Sweet item", emotionType = "SWEET", timestamp = 1000L),
            FactStreamItem(id = "2", content = "Conflict item", emotionType = "CONFLICT", timestamp = 2000L),
            FactStreamItem(id = "3", content = "Another sweet", emotionType = "SWEET", timestamp = 3000L)
        )
        val state = FactStreamUiState(items = items, selectedFilter = FilterType.SWEET)
        
        assertEquals(2, state.filteredItems.size)
        assertTrue(state.filteredItems.all { it.emotionType == "SWEET" })
    }

    @Test
    fun `filteredItems returns only CONFLICT items when filter is CONFLICT`() {
        val items = listOf(
            FactStreamItem(id = "1", content = "Sweet item", emotionType = "SWEET", timestamp = 1000L),
            FactStreamItem(id = "2", content = "Conflict item", emotionType = "CONFLICT", timestamp = 2000L)
        )
        val state = FactStreamUiState(items = items, selectedFilter = FilterType.CONFLICT)
        
        assertEquals(1, state.filteredItems.size)
        assertEquals("CONFLICT", state.filteredItems.first().emotionType)
    }

    @Test
    fun `filteredItems returns empty list when no items match filter`() {
        val items = listOf(
            FactStreamItem(id = "1", content = "Sweet item", emotionType = "SWEET", timestamp = 1000L)
        )
        val state = FactStreamUiState(items = items, selectedFilter = FilterType.CONFLICT)
        
        assertTrue(state.filteredItems.isEmpty())
    }

    // ============================================================
    // FactStreamItem 测试
    // ============================================================

    @Test
    fun `FactStreamItem creation is correct`() {
        val item = FactStreamItem(
            id = "test-id",
            content = "Test content",
            emotionType = "SWEET",
            timestamp = 1234567890L,
            aiSuggestion = "AI suggestion",
            isAiSummary = false,
            scoreChange = 5,
            tags = listOf("tag1", "tag2")
        )
        
        assertEquals("test-id", item.id)
        assertEquals("Test content", item.content)
        assertEquals("SWEET", item.emotionType)
        assertEquals(1234567890L, item.timestamp)
        assertEquals("AI suggestion", item.aiSuggestion)
        assertFalse(item.isAiSummary)
        assertEquals(5, item.scoreChange)
        assertEquals(2, item.tags.size)
    }

    @Test
    fun `FactStreamItem with AI summary flag is correct`() {
        val item = FactStreamItem(
            id = "ai-summary",
            content = "AI generated summary",
            emotionType = "NEUTRAL",
            timestamp = 1000L,
            isAiSummary = true
        )
        
        assertTrue(item.isAiSummary)
    }

    @Test
    fun `FactStreamItem with positive score change is correct`() {
        val item = FactStreamItem(
            id = "1",
            content = "Positive event",
            emotionType = "SWEET",
            timestamp = 1000L,
            scoreChange = 10
        )
        
        assertEquals(10, item.scoreChange)
        assertTrue(item.hasPositiveScoreChange)
    }

    @Test
    fun `FactStreamItem with negative score change is correct`() {
        val item = FactStreamItem(
            id = "1",
            content = "Negative event",
            emotionType = "CONFLICT",
            timestamp = 1000L,
            scoreChange = -5
        )
        
        assertEquals(-5, item.scoreChange)
        assertTrue(item.hasNegativeScoreChange)
    }

    @Test
    fun `FactStreamItem with zero score change has no change`() {
        val item = FactStreamItem(
            id = "1",
            content = "Neutral event",
            emotionType = "NEUTRAL",
            timestamp = 1000L,
            scoreChange = 0
        )
        
        assertEquals(0, item.scoreChange)
        assertFalse(item.hasPositiveScoreChange)
        assertFalse(item.hasNegativeScoreChange)
    }
}
