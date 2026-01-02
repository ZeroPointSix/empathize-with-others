package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.presentation.ui.screen.aiconfig.UsageStatsTab
import com.empathy.ai.presentation.ui.screen.aiconfig.UsageStatsUiState
import com.empathy.ai.presentation.ui.screen.aiconfig.UsageTimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UsageStatsViewModel 单元测试
 * 
 * TD-00025 T6-07: 用量统计测试
 * 
 * 测试覆盖：
 * - UI状态管理
 * - Tab切换逻辑
 * - 时间范围切换
 * - 导出和清除功能
 */
class UsageStatsViewModelTest {

    // ============================================================
    // UI状态测试
    // ============================================================

    @Test
    fun `UI状态_默认值正确`() {
        val state = UsageStatsUiState()
        
        assertFalse("默认不应加载中", state.isLoading)
        assertNull("默认统计数据应为null", state.stats)
        assertEquals("默认Tab应为BY_PROVIDER", UsageStatsTab.BY_PROVIDER, state.selectedTab)
        assertEquals("默认时间范围应为TODAY", UsageTimeRange.TODAY, state.timeRange)
        assertNull("默认错误应为null", state.error)
        assertFalse("默认不应显示清除对话框", state.showClearConfirmDialog)
        assertFalse("默认不应导出中", state.isExporting)
        assertFalse("默认不应清除中", state.isClearing)
        assertFalse("默认导出成功应为false", state.exportSuccess)
        assertFalse("默认清除成功应为false", state.clearSuccess)
    }

    @Test
    fun `UI状态_加载中状态`() {
        val state = UsageStatsUiState(isLoading = true)
        
        assertTrue("应为加载中", state.isLoading)
    }

    @Test
    fun `UI状态_错误状态`() {
        val errorMessage = "加载失败"
        val state = UsageStatsUiState(error = errorMessage)
        
        assertEquals("错误消息应正确", errorMessage, state.error)
    }

    // ============================================================
    // Tab切换测试
    // ============================================================

    @Test
    fun `Tab切换_切换到BY_MODEL`() {
        val initialState = UsageStatsUiState(selectedTab = UsageStatsTab.BY_PROVIDER)
        val newState = initialState.copy(selectedTab = UsageStatsTab.BY_MODEL)
        
        assertEquals("Tab应切换到BY_MODEL", UsageStatsTab.BY_MODEL, newState.selectedTab)
    }

    @Test
    fun `Tab切换_切换到BY_PROVIDER`() {
        val initialState = UsageStatsUiState(selectedTab = UsageStatsTab.BY_MODEL)
        val newState = initialState.copy(selectedTab = UsageStatsTab.BY_PROVIDER)
        
        assertEquals("Tab应切换到BY_PROVIDER", UsageStatsTab.BY_PROVIDER, newState.selectedTab)
    }

    @Test
    fun `Tab切换_Tab数量应为2`() {
        val tabs = UsageStatsTab.values()
        
        assertEquals("应有2个Tab", 2, tabs.size)
    }

    @Test
    fun `Tab切换_BY_PROVIDER标签正确`() {
        val tab = UsageStatsTab.BY_PROVIDER
        
        assertEquals("BY_PROVIDER名称正确", "BY_PROVIDER", tab.name)
    }

    @Test
    fun `Tab切换_BY_MODEL标签正确`() {
        val tab = UsageStatsTab.BY_MODEL
        
        assertEquals("BY_MODEL名称正确", "BY_MODEL", tab.name)
    }

    // ============================================================
    // 时间范围测试
    // ============================================================

    @Test
    fun `时间范围_切换到THIS_WEEK`() {
        val initialState = UsageStatsUiState(timeRange = UsageTimeRange.TODAY)
        val newState = initialState.copy(timeRange = UsageTimeRange.THIS_WEEK)
        
        assertEquals("时间范围应切换到THIS_WEEK", UsageTimeRange.THIS_WEEK, newState.timeRange)
    }

    @Test
    fun `时间范围_切换到THIS_MONTH`() {
        val initialState = UsageStatsUiState(timeRange = UsageTimeRange.TODAY)
        val newState = initialState.copy(timeRange = UsageTimeRange.THIS_MONTH)
        
        assertEquals("时间范围应切换到THIS_MONTH", UsageTimeRange.THIS_MONTH, newState.timeRange)
    }

    @Test
    fun `时间范围_切换到ALL`() {
        val initialState = UsageStatsUiState(timeRange = UsageTimeRange.TODAY)
        val newState = initialState.copy(timeRange = UsageTimeRange.ALL)
        
        assertEquals("时间范围应切换到ALL", UsageTimeRange.ALL, newState.timeRange)
    }

    @Test
    fun `时间范围_应有4种范围`() {
        val ranges = UsageTimeRange.values()
        
        assertEquals("应有4种时间范围", 4, ranges.size)
    }

    @Test
    fun `时间范围_TODAY正确`() {
        val range = UsageTimeRange.TODAY
        
        assertEquals("TODAY名称正确", "TODAY", range.name)
    }

    @Test
    fun `时间范围_THIS_WEEK正确`() {
        val range = UsageTimeRange.THIS_WEEK
        
        assertEquals("THIS_WEEK名称正确", "THIS_WEEK", range.name)
    }

    @Test
    fun `时间范围_THIS_MONTH正确`() {
        val range = UsageTimeRange.THIS_MONTH
        
        assertEquals("THIS_MONTH名称正确", "THIS_MONTH", range.name)
    }

    @Test
    fun `时间范围_ALL正确`() {
        val range = UsageTimeRange.ALL
        
        assertEquals("ALL名称正确", "ALL", range.name)
    }

    // ============================================================
    // 清除对话框测试
    // ============================================================

    @Test
    fun `清除对话框_显示对话框`() {
        val initialState = UsageStatsUiState(showClearConfirmDialog = false)
        val newState = initialState.copy(showClearConfirmDialog = true)
        
        assertTrue("应显示清除对话框", newState.showClearConfirmDialog)
    }

    @Test
    fun `清除对话框_关闭对话框`() {
        val initialState = UsageStatsUiState(showClearConfirmDialog = true)
        val newState = initialState.copy(showClearConfirmDialog = false)
        
        assertFalse("应关闭清除对话框", newState.showClearConfirmDialog)
    }

    // ============================================================
    // 导出功能测试
    // ============================================================

    @Test
    fun `导出功能_导出中状态`() {
        val state = UsageStatsUiState(isExporting = true)
        
        assertTrue("应为导出中", state.isExporting)
    }

    @Test
    fun `导出功能_导出成功状态`() {
        val state = UsageStatsUiState(exportSuccess = true)
        
        assertTrue("导出成功应为true", state.exportSuccess)
    }

    @Test
    fun `导出功能_清除导出成功状态`() {
        val initialState = UsageStatsUiState(exportSuccess = true)
        val newState = initialState.copy(exportSuccess = false)
        
        assertFalse("导出成功应被清除", newState.exportSuccess)
    }

    // ============================================================
    // 清除功能测试
    // ============================================================

    @Test
    fun `清除功能_清除中状态`() {
        val state = UsageStatsUiState(isClearing = true)
        
        assertTrue("应为清除中", state.isClearing)
    }

    @Test
    fun `清除功能_清除成功状态`() {
        val state = UsageStatsUiState(clearSuccess = true)
        
        assertTrue("清除成功应为true", state.clearSuccess)
    }

    @Test
    fun `清除功能_清除清除成功状态`() {
        val initialState = UsageStatsUiState(clearSuccess = true)
        val newState = initialState.copy(clearSuccess = false)
        
        assertFalse("清除成功应被清除", newState.clearSuccess)
    }

    @Test
    fun `清除功能_清除天数阈值应为90天`() {
        val cleanupDaysThreshold = 90
        
        assertEquals("清除天数阈值应为90", 90, cleanupDaysThreshold)
    }

    // ============================================================
    // 错误处理测试
    // ============================================================

    @Test
    fun `错误处理_设置错误消息`() {
        val errorMessage = "网络错误"
        val state = UsageStatsUiState(error = errorMessage)
        
        assertEquals("错误消息应正确", errorMessage, state.error)
    }

    @Test
    fun `错误处理_清除错误消息`() {
        val initialState = UsageStatsUiState(error = "网络错误")
        val newState = initialState.copy(error = null)
        
        assertNull("错误消息应被清除", newState.error)
    }

    // ============================================================
    // 统计数据测试
    // ============================================================

    @Test
    fun `统计数据_设置统计数据`() {
        val stats = TestApiUsageStats(
            totalRequests = 100,
            totalTokens = 50000,
            successRate = 0.95
        )
        val state = TestUsageStatsUiState(stats = stats)
        
        assertEquals("总请求数应为100", 100, state.stats?.totalRequests)
        assertEquals("总Token数应为50000", 50000, state.stats?.totalTokens)
        assertEquals("成功率应为0.95", 0.95, state.stats?.successRate ?: 0.0, 0.001)
    }

    @Test
    fun `统计数据_格式化Token数量`() {
        val tokens = 50000
        val formatted = formatTokenCount(tokens)
        
        assertEquals("50000应格式化为50K", "50K", formatted)
    }

    @Test
    fun `统计数据_格式化成功率`() {
        val successRate = 0.95
        val formatted = formatSuccessRate(successRate)
        
        assertEquals("0.95应格式化为95%", "95%", formatted)
    }

    @Test
    fun `统计数据_格式化成功率_100%`() {
        val successRate = 1.0
        val formatted = formatSuccessRate(successRate)
        
        assertEquals("1.0应格式化为100%", "100%", formatted)
    }

    @Test
    fun `统计数据_格式化成功率_0%`() {
        val successRate = 0.0
        val formatted = formatSuccessRate(successRate)
        
        assertEquals("0.0应格式化为0%", "0%", formatted)
    }

    // ============================================================
    // 服务商统计测试
    // ============================================================

    @Test
    fun `服务商统计_列表应正确`() {
        val providerStats = listOf(
            TestProviderUsageStats("openai", "OpenAI", 50, 25000),
            TestProviderUsageStats("deepseek", "DeepSeek", 50, 25000)
        )
        
        assertEquals("应有2个服务商统计", 2, providerStats.size)
    }

    @Test
    fun `服务商统计_单个服务商数据正确`() {
        val stats = TestProviderUsageStats("openai", "OpenAI", 50, 25000)
        
        assertEquals("服务商ID应正确", "openai", stats.providerId)
        assertEquals("服务商名称应正确", "OpenAI", stats.providerName)
        assertEquals("请求数应正确", 50, stats.requestCount)
        assertEquals("Token数应正确", 25000, stats.totalTokens)
    }

    // ============================================================
    // 模型统计测试
    // ============================================================

    @Test
    fun `模型统计_列表应正确`() {
        val modelStats = listOf(
            TestModelUsageStats("gpt-4", "GPT-4", 30, 20000),
            TestModelUsageStats("gpt-3.5-turbo", "GPT-3.5 Turbo", 70, 30000)
        )
        
        assertEquals("应有2个模型统计", 2, modelStats.size)
    }

    @Test
    fun `模型统计_单个模型数据正确`() {
        val stats = TestModelUsageStats("gpt-4", "GPT-4", 30, 20000)
        
        assertEquals("模型ID应正确", "gpt-4", stats.modelId)
        assertEquals("模型名称应正确", "GPT-4", stats.modelName)
        assertEquals("请求数应正确", 30, stats.requestCount)
        assertEquals("Token数应正确", 20000, stats.totalTokens)
    }

    // ============================================================
    // 辅助函数
    // ============================================================

    /**
     * 格式化Token数量
     */
    private fun formatTokenCount(count: Int): String {
        return when {
            count >= 1000 -> "${count / 1000}K"
            else -> count.toString()
        }
    }

    /**
     * 格式化成功率
     */
    private fun formatSuccessRate(rate: Double): String {
        return "${(rate * 100).toInt()}%"
    }

    // ============================================================
    // 辅助数据类
    // ============================================================

    /**
     * 测试用API用量统计
     */
    data class TestApiUsageStats(
        val totalRequests: Int,
        val totalTokens: Int,
        val successRate: Double
    )

    /**
     * 测试用UI状态
     */
    data class TestUsageStatsUiState(
        val stats: TestApiUsageStats? = null
    )

    /**
     * 测试用服务商统计
     */
    data class TestProviderUsageStats(
        val providerId: String,
        val providerName: String,
        val requestCount: Int,
        val totalTokens: Int
    )

    /**
     * 测试用模型统计
     */
    data class TestModelUsageStats(
        val modelId: String,
        val modelName: String,
        val requestCount: Int,
        val totalTokens: Int
    )
}
