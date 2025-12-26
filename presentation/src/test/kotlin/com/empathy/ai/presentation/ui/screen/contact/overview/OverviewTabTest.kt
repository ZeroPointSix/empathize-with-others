package com.empathy.ai.presentation.ui.screen.contact.overview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OverviewTab 单元测试
 * 
 * TD-00020 T068: 测试健康度显示、趋势图渲染、快速操作
 * 
 * 关键测试场景:
 * - 健康度分数0-100边界值
 * - 趋势数据为空时的降级显示
 * - 快速操作按钮点击回调
 */
class OverviewTabTest {

    // ============================================================
    // OverviewUiState 测试
    // ============================================================

    @Test
    fun `OverviewUiState default values are correct`() {
        val state = OverviewUiState()
        
        assertEquals(0, state.healthScore)
        assertTrue(state.trendData.isEmpty())
        assertTrue(state.recentFacts.isEmpty())
        assertFalse(state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `OverviewUiState with health score 0 is valid`() {
        val state = OverviewUiState(healthScore = 0)
        
        assertEquals(0, state.healthScore)
        assertEquals(0f, state.healthProgress, 0.001f)
    }

    @Test
    fun `OverviewUiState with health score 100 is valid`() {
        val state = OverviewUiState(healthScore = 100)
        
        assertEquals(100, state.healthScore)
        assertEquals(1f, state.healthProgress, 0.001f)
    }

    @Test
    fun `OverviewUiState with health score 50 has correct progress`() {
        val state = OverviewUiState(healthScore = 50)
        
        assertEquals(50, state.healthScore)
        assertEquals(0.5f, state.healthProgress, 0.001f)
    }

    @Test
    fun `OverviewUiState with negative health score clamps to 0`() {
        // 负数应该被视为无效，但UI层应该处理
        val state = OverviewUiState(healthScore = -10)
        
        // 进度应该被限制在0-1范围内
        assertTrue(state.healthProgress >= 0f)
    }

    @Test
    fun `OverviewUiState with health score over 100 clamps to 1`() {
        // 超过100应该被视为无效，但UI层应该处理
        val state = OverviewUiState(healthScore = 150)
        
        // 进度应该被限制在0-1范围内
        assertTrue(state.healthProgress <= 1f)
    }

    // ============================================================
    // 趋势数据测试
    // ============================================================

    @Test
    fun `OverviewUiState with empty trend data is valid`() {
        val state = OverviewUiState(trendData = emptyList())
        
        assertTrue(state.trendData.isEmpty())
        assertFalse(state.hasTrendData)
    }

    @Test
    fun `OverviewUiState with trend data is valid`() {
        val trendData = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f)
        val state = OverviewUiState(trendData = trendData)
        
        assertEquals(5, state.trendData.size)
        assertTrue(state.hasTrendData)
    }

    @Test
    fun `OverviewUiState with single trend point is valid`() {
        val trendData = listOf(0.5f)
        val state = OverviewUiState(trendData = trendData)
        
        assertEquals(1, state.trendData.size)
        assertTrue(state.hasTrendData)
    }

    // ============================================================
    // 最近事实测试
    // ============================================================

    @Test
    fun `OverviewUiState with empty recent facts is valid`() {
        val state = OverviewUiState(recentFacts = emptyList())
        
        assertTrue(state.recentFacts.isEmpty())
        assertFalse(state.hasRecentFacts)
    }

    @Test
    fun `OverviewUiState with recent facts is valid`() {
        val facts = listOf(
            RecentFactItem(id = "1", content = "Fact 1", timestamp = 1000L),
            RecentFactItem(id = "2", content = "Fact 2", timestamp = 2000L)
        )
        val state = OverviewUiState(recentFacts = facts)
        
        assertEquals(2, state.recentFacts.size)
        assertTrue(state.hasRecentFacts)
    }

    // ============================================================
    // 加载状态测试
    // ============================================================

    @Test
    fun `OverviewUiState loading state is correct`() {
        val state = OverviewUiState(isLoading = true)
        
        assertTrue(state.isLoading)
    }

    @Test
    fun `OverviewUiState error state is correct`() {
        val errorMessage = "Network error"
        val state = OverviewUiState(error = errorMessage)
        
        assertEquals(errorMessage, state.error)
        assertTrue(state.hasError)
    }

    @Test
    fun `OverviewUiState without error has no error`() {
        val state = OverviewUiState(error = null)
        
        assertEquals(null, state.error)
        assertFalse(state.hasError)
    }

    // ============================================================
    // 快速操作测试
    // ============================================================

    @Test
    fun `QuickAction enum has correct values`() {
        val actions = QuickAction.values()
        
        assertEquals(4, actions.size)
        assertTrue(actions.contains(QuickAction.CHAT))
        assertTrue(actions.contains(QuickAction.CALL))
        assertTrue(actions.contains(QuickAction.GIFT))
        assertTrue(actions.contains(QuickAction.NOTE))
    }

    @Test
    fun `QuickAction CHAT has correct icon and label`() {
        val action = QuickAction.CHAT
        
        assertNotNull(action.iconRes)
        assertNotNull(action.labelRes)
    }

    // ============================================================
    // RecentFactItem 测试
    // ============================================================

    @Test
    fun `RecentFactItem creation is correct`() {
        val item = RecentFactItem(
            id = "test-id",
            content = "Test content",
            timestamp = 1234567890L
        )
        
        assertEquals("test-id", item.id)
        assertEquals("Test content", item.content)
        assertEquals(1234567890L, item.timestamp)
    }

    @Test
    fun `RecentFactItem with empty content is valid`() {
        val item = RecentFactItem(
            id = "test-id",
            content = "",
            timestamp = 0L
        )
        
        assertEquals("", item.content)
    }
}
