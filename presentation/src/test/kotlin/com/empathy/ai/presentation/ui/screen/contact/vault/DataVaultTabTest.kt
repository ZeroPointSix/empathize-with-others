package com.empathy.ai.presentation.ui.screen.contact.vault

import com.empathy.ai.presentation.ui.component.vault.DataStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DataVaultTab 单元测试
 * 
 * TD-00020 T071: 测试统计显示、数据网格点击
 * 
 * 关键测试场景:
 * - 数据总量统计正确性
 * - 数据源卡片点击回调
 * - 不可用状态禁用交互
 */
class DataVaultTabTest {

    // ============================================================
    // DataVaultUiState 测试
    // ============================================================

    @Test
    fun `DataVaultUiState default values are correct`() {
        val state = DataVaultUiState()
        
        assertEquals(0, state.totalCount)
        assertEquals(0, state.chatCount)
        assertEquals(0, state.summaryCount)
        assertEquals(0, state.imageCount)
        assertEquals(0, state.audioCount)
        assertEquals(0, state.videoCount)
        assertEquals(0, state.fileCount)
        assertFalse(state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `DataVaultUiState with counts is valid`() {
        val state = DataVaultUiState(
            totalCount = 100,
            chatCount = 50,
            summaryCount = 20,
            imageCount = 15,
            audioCount = 10,
            videoCount = 3,
            fileCount = 2
        )
        
        assertEquals(100, state.totalCount)
        assertEquals(50, state.chatCount)
        assertEquals(20, state.summaryCount)
        assertEquals(15, state.imageCount)
        assertEquals(10, state.audioCount)
        assertEquals(3, state.videoCount)
        assertEquals(2, state.fileCount)
    }

    @Test
    fun `DataVaultUiState calculates total correctly`() {
        val state = DataVaultUiState(
            chatCount = 50,
            summaryCount = 20,
            imageCount = 15,
            audioCount = 10,
            videoCount = 3,
            fileCount = 2
        )
        
        assertEquals(100, state.calculatedTotal)
    }

    @Test
    fun `DataVaultUiState with zero counts has zero total`() {
        val state = DataVaultUiState()
        
        assertEquals(0, state.calculatedTotal)
    }

    // ============================================================
    // 数据源状态测试
    // ============================================================

    @Test
    fun `DataVaultUiState with data source statuses is valid`() {
        val state = DataVaultUiState(
            chatStatus = DataStatus.COMPLETED,
            summaryStatus = DataStatus.PROCESSING,
            imageStatus = DataStatus.NOT_AVAILABLE,
            audioStatus = DataStatus.FAILED
        )
        
        assertEquals(DataStatus.COMPLETED, state.chatStatus)
        assertEquals(DataStatus.PROCESSING, state.summaryStatus)
        assertEquals(DataStatus.NOT_AVAILABLE, state.imageStatus)
        assertEquals(DataStatus.FAILED, state.audioStatus)
    }

    @Test
    fun `DataVaultUiState default statuses are COMPLETED`() {
        val state = DataVaultUiState()
        
        assertEquals(DataStatus.COMPLETED, state.chatStatus)
        assertEquals(DataStatus.COMPLETED, state.summaryStatus)
        assertEquals(DataStatus.COMPLETED, state.imageStatus)
        assertEquals(DataStatus.COMPLETED, state.audioStatus)
        assertEquals(DataStatus.COMPLETED, state.videoStatus)
        assertEquals(DataStatus.COMPLETED, state.fileStatus)
    }

    // ============================================================
    // 数据源可用性测试
    // ============================================================

    @Test
    fun `isChatAvailable returns true when status is COMPLETED`() {
        val state = DataVaultUiState(chatStatus = DataStatus.COMPLETED)
        
        assertTrue(state.isChatAvailable)
    }

    @Test
    fun `isChatAvailable returns false when status is NOT_AVAILABLE`() {
        val state = DataVaultUiState(chatStatus = DataStatus.NOT_AVAILABLE)
        
        assertFalse(state.isChatAvailable)
    }

    @Test
    fun `isChatAvailable returns true when status is PROCESSING`() {
        val state = DataVaultUiState(chatStatus = DataStatus.PROCESSING)
        
        assertTrue(state.isChatAvailable)
    }

    @Test
    fun `isChatAvailable returns false when status is FAILED`() {
        val state = DataVaultUiState(chatStatus = DataStatus.FAILED)
        
        assertFalse(state.isChatAvailable)
    }

    // ============================================================
    // 加载和错误状态测试
    // ============================================================

    @Test
    fun `DataVaultUiState loading state is correct`() {
        val state = DataVaultUiState(isLoading = true)
        
        assertTrue(state.isLoading)
    }

    @Test
    fun `DataVaultUiState error state is correct`() {
        val errorMessage = "Failed to load data"
        val state = DataVaultUiState(error = errorMessage)
        
        assertEquals(errorMessage, state.error)
        assertTrue(state.hasError)
    }

    @Test
    fun `DataVaultUiState without error has no error`() {
        val state = DataVaultUiState(error = null)
        
        assertEquals(null, state.error)
        assertFalse(state.hasError)
    }

    // ============================================================
    // DataStatus 测试
    // ============================================================

    @Test
    fun `DataStatus enum has correct values`() {
        val statuses = DataStatus.values()
        
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(DataStatus.COMPLETED))
        assertTrue(statuses.contains(DataStatus.PROCESSING))
        assertTrue(statuses.contains(DataStatus.NOT_AVAILABLE))
        assertTrue(statuses.contains(DataStatus.FAILED))
    }

    @Test
    fun `DataStatus COMPLETED has correct display name`() {
        assertEquals("已完成", DataStatus.COMPLETED.displayName)
    }

    @Test
    fun `DataStatus PROCESSING has correct display name`() {
        assertEquals("处理中", DataStatus.PROCESSING.displayName)
    }

    @Test
    fun `DataStatus NOT_AVAILABLE has correct display name`() {
        assertEquals("不可用", DataStatus.NOT_AVAILABLE.displayName)
    }

    @Test
    fun `DataStatus FAILED has correct display name`() {
        assertEquals("失败", DataStatus.FAILED.displayName)
    }

    // ============================================================
    // 数据源配置测试
    // ============================================================

    @Test
    fun `DataSourceType enum has correct values`() {
        val types = DataSourceType.values()
        
        assertEquals(6, types.size)
        assertTrue(types.contains(DataSourceType.CHAT))
        assertTrue(types.contains(DataSourceType.SUMMARY))
        assertTrue(types.contains(DataSourceType.IMAGE))
        assertTrue(types.contains(DataSourceType.AUDIO))
        assertTrue(types.contains(DataSourceType.VIDEO))
        assertTrue(types.contains(DataSourceType.FILE))
    }

    @Test
    fun `DataSourceType CHAT has correct display name`() {
        assertEquals("聊天记录", DataSourceType.CHAT.displayName)
    }

    @Test
    fun `DataSourceType SUMMARY has correct display name`() {
        assertEquals("AI总结", DataSourceType.SUMMARY.displayName)
    }

    @Test
    fun `DataSourceType IMAGE has correct display name`() {
        assertEquals("图片", DataSourceType.IMAGE.displayName)
    }

    // ============================================================
    // 格式化显示测试
    // ============================================================

    @Test
    fun `formatCount returns correct format for small numbers`() {
        val state = DataVaultUiState(totalCount = 100)
        
        assertEquals("100", state.formattedTotalCount)
    }

    @Test
    fun `formatCount returns correct format for thousands`() {
        val state = DataVaultUiState(totalCount = 1500)
        
        assertEquals("1.5K", state.formattedTotalCount)
    }

    @Test
    fun `formatCount returns correct format for millions`() {
        val state = DataVaultUiState(totalCount = 1500000)
        
        assertEquals("1.5M", state.formattedTotalCount)
    }

    @Test
    fun `formatCount returns zero for zero count`() {
        val state = DataVaultUiState(totalCount = 0)
        
        assertEquals("0", state.formattedTotalCount)
    }
}
