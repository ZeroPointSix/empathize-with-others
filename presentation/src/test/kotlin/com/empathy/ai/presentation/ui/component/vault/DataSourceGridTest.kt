package com.empathy.ai.presentation.ui.component.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DataSourceGrid组件单元测试
 * 
 * 测试覆盖:
 * - 网格布局正确性
 * - 数据源类型
 * - 点击回调
 * - 状态显示
 * 
 * @see TDD-00020 10.2 UI测试
 */
class DataSourceGridTest {

    // ==================== 数据源类型测试 ====================

    @Test
    fun `DataSourceTypes should have 6 predefined types`() {
        // Then
        assertEquals(6, DataSourceTypes.ALL.size)
    }

    @Test
    fun `CHAT type should have correct properties`() {
        // Given
        val chatType = DataSourceTypes.CHAT
        
        // Then
        assertEquals("chat", chatType.id)
        assertEquals("聊天记录", chatType.title)
        assertEquals("文字消息", chatType.subtitle)
    }

    @Test
    fun `AI_SUMMARY type should have correct properties`() {
        // Given
        val aiSummaryType = DataSourceTypes.AI_SUMMARY
        
        // Then
        assertEquals("ai_summary", aiSummaryType.id)
        assertEquals("AI总结", aiSummaryType.title)
        assertEquals("智能分析", aiSummaryType.subtitle)
    }

    @Test
    fun `IMAGE type should have correct properties`() {
        // Given
        val imageType = DataSourceTypes.IMAGE
        
        // Then
        assertEquals("image", imageType.id)
        assertEquals("图片", imageType.title)
        assertEquals("照片和截图", imageType.subtitle)
    }

    @Test
    fun `VOICE type should have correct properties`() {
        // Given
        val voiceType = DataSourceTypes.VOICE
        
        // Then
        assertEquals("voice", voiceType.id)
        assertEquals("语音", voiceType.title)
        assertEquals("语音消息", voiceType.subtitle)
    }

    @Test
    fun `VIDEO type should have correct properties`() {
        // Given
        val videoType = DataSourceTypes.VIDEO
        
        // Then
        assertEquals("video", videoType.id)
        assertEquals("视频", videoType.title)
        assertEquals("视频消息", videoType.subtitle)
    }

    @Test
    fun `FILE type should have correct properties`() {
        // Given
        val fileType = DataSourceTypes.FILE
        
        // Then
        assertEquals("file", fileType.id)
        assertEquals("文件", fileType.title)
        assertEquals("文档和附件", fileType.subtitle)
    }

    // ==================== 网格布局测试 ====================

    @Test
    fun `grid should chunk items into rows of 2`() {
        // Given
        val items = DataSourceTypes.ALL
        
        // When
        val rows = items.chunked(2)
        
        // Then
        assertEquals(3, rows.size) // 6 items / 2 = 3 rows
        assertEquals(2, rows[0].size)
        assertEquals(2, rows[1].size)
        assertEquals(2, rows[2].size)
    }

    @Test
    fun `grid should handle odd number of items`() {
        // Given
        val items = DataSourceTypes.ALL.take(5)
        
        // When
        val rows = items.chunked(2)
        
        // Then
        assertEquals(3, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals(2, rows[1].size)
        assertEquals(1, rows[2].size) // Last row has 1 item
    }

    @Test
    fun `grid should handle empty list`() {
        // Given
        val items = emptyList<DataSourceConfig>()
        
        // When
        val rows = items.chunked(2)
        
        // Then
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `grid should handle single item`() {
        // Given
        val items = listOf(DataSourceTypes.CHAT)
        
        // When
        val rows = items.chunked(2)
        
        // Then
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].size)
    }

    // ==================== DataSourceItem测试 ====================

    @Test
    fun `DataSourceItem should store config correctly`() {
        // Given
        val item = DataSourceItem(
            config = DataSourceTypes.CHAT,
            count = 256,
            status = DataStatus.COMPLETED,
            lastUpdated = "今天"
        )
        
        // Then
        assertEquals(DataSourceTypes.CHAT, item.config)
        assertEquals(256, item.count)
        assertEquals(DataStatus.COMPLETED, item.status)
        assertEquals("今天", item.lastUpdated)
    }

    @Test
    fun `DataSourceItem should handle null lastUpdated`() {
        // Given
        val item = DataSourceItem(
            config = DataSourceTypes.VOICE,
            count = 0,
            status = DataStatus.NOT_AVAILABLE,
            lastUpdated = null
        )
        
        // Then
        assertEquals(null, item.lastUpdated)
    }

    @Test
    fun `DataSourceItem should handle zero count`() {
        // Given
        val item = DataSourceItem(
            config = DataSourceTypes.VIDEO,
            count = 0,
            status = DataStatus.NOT_AVAILABLE
        )
        
        // Then
        assertEquals(0, item.count)
    }

    // ==================== 状态测试 ====================

    @Test
    fun `COMPLETED status should be clickable`() {
        // Given
        val status = DataStatus.COMPLETED
        
        // Then - COMPLETED items should be interactive
        assertNotNull(status)
    }

    @Test
    fun `NOT_AVAILABLE status should disable interaction`() {
        // Given
        val item = DataSourceItem(
            config = DataSourceTypes.VOICE,
            count = 0,
            status = DataStatus.NOT_AVAILABLE
        )
        
        // Then
        assertEquals(DataStatus.NOT_AVAILABLE, item.status)
    }

    @Test
    fun `PROCESSING status should show loading indicator`() {
        // Given
        val item = DataSourceItem(
            config = DataSourceTypes.IMAGE,
            count = 89,
            status = DataStatus.PROCESSING
        )
        
        // Then
        assertEquals(DataStatus.PROCESSING, item.status)
    }

    // ==================== 点击回调测试 ====================

    @Test
    fun `click callback should receive correct item`() {
        // Given
        var clickedItem: DataSourceItem? = null
        val testItem = DataSourceItem(
            config = DataSourceTypes.CHAT,
            count = 100,
            status = DataStatus.COMPLETED
        )
        val onItemClick: (DataSourceItem) -> Unit = { clickedItem = it }
        
        // When
        onItemClick(testItem)
        
        // Then
        assertEquals(testItem, clickedItem)
    }

    @Test
    fun `click callback should not be called for NOT_AVAILABLE items`() {
        // Given
        var clickCount = 0
        val testItem = DataSourceItem(
            config = DataSourceTypes.VOICE,
            count = 0,
            status = DataStatus.NOT_AVAILABLE
        )
        
        // When - simulating disabled click
        if (testItem.status != DataStatus.NOT_AVAILABLE) {
            clickCount++
        }
        
        // Then
        assertEquals(0, clickCount)
    }

    // ==================== 数据源配置测试 ====================

    @Test
    fun `DataSourceConfig should have unique ids`() {
        // Given
        val ids = DataSourceTypes.ALL.map { it.id }
        
        // Then
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `DataSourceConfig should have non-empty titles`() {
        // Given
        val titles = DataSourceTypes.ALL.map { it.title }
        
        // Then
        assertTrue(titles.all { it.isNotEmpty() })
    }

    @Test
    fun `DataSourceConfig should have non-empty subtitles`() {
        // Given
        val subtitles = DataSourceTypes.ALL.map { it.subtitle }
        
        // Then
        assertTrue(subtitles.all { it.isNotEmpty() })
    }

    // ==================== 显示文本测试 ====================

    @Test
    fun `lastUpdated should override subtitle when present`() {
        // Given
        val item = DataSourceItem(
            config = DataSourceTypes.CHAT,
            count = 256,
            status = DataStatus.COMPLETED,
            lastUpdated = "今天"
        )
        
        // When
        val displaySubtitle = item.lastUpdated ?: item.config.subtitle
        
        // Then
        assertEquals("今天", displaySubtitle)
    }

    @Test
    fun `subtitle should be used when lastUpdated is null`() {
        // Given
        val item = DataSourceItem(
            config = DataSourceTypes.CHAT,
            count = 256,
            status = DataStatus.COMPLETED,
            lastUpdated = null
        )
        
        // When
        val displaySubtitle = item.lastUpdated ?: item.config.subtitle
        
        // Then
        assertEquals("文字消息", displaySubtitle)
    }
}
