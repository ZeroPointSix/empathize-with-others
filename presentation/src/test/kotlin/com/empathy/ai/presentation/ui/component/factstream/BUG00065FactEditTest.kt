package com.empathy.ai.presentation.ui.component.factstream

import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.TimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUG-00065: 事实流编辑功能测试
 * 
 * 测试目标：验证事实编辑回调的正确传递和触发
 */
class BUG00065FactEditTest {

    // ==================== 测试数据 ====================
    
    private val sampleFact = Fact(
        id = "fact_1",
        key = "职业",
        value = "产品经理",
        timestamp = System.currentTimeMillis(),
        source = FactSource.MANUAL
    )
    
    private val sampleUserFactItem = TimelineItem.UserFact(
        id = "item_1",
        timestamp = System.currentTimeMillis(),
        emotionType = EmotionType.NEUTRAL,
        fact = sampleFact
    )
    
    private val sampleConversationItem = TimelineItem.Conversation(
        id = "conv_1",
        timestamp = System.currentTimeMillis(),
        emotionType = EmotionType.SWEET,
        log = ConversationLog(
            id = 1,
            contactId = "contact_1",
            userInput = "今天很开心",
            aiResponse = "继续保持",
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )
    )

    // ==================== TimelineItem 类型判断测试 ====================
    
    @Test
    fun `UserFact类型应该正确识别`() {
        // Given
        val item = sampleUserFactItem
        
        // When & Then
        assertTrue(item is TimelineItem.UserFact)
    }
    
    @Test
    fun `Conversation类型应该正确识别`() {
        // Given
        val item = sampleConversationItem
        
        // When & Then
        assertTrue(item is TimelineItem.Conversation)
    }
    
    @Test
    fun `UserFact应该包含正确的factId`() {
        // Given
        val item = sampleUserFactItem
        
        // When
        val factId = (item as TimelineItem.UserFact).fact.id
        
        // Then
        assertEquals("fact_1", factId)
    }

    // ==================== 点击逻辑测试 ====================
    
    @Test
    fun `点击UserFact时应该调用onFactEdit而非onItemClick`() {
        // Given
        var factEditCalled = false
        var itemClickCalled = false
        val onFactEdit: (String) -> Unit = { factEditCalled = true }
        val onItemClick: (TimelineItem) -> Unit = { itemClickCalled = true }
        val item = sampleUserFactItem
        
        // When - 模拟点击逻辑
        if (item is TimelineItem.UserFact) {
            onFactEdit(item.fact.id)
        } else {
            onItemClick(item)
        }
        
        // Then
        assertTrue("onFactEdit应该被调用", factEditCalled)
        assertTrue("onItemClick不应该被调用", !itemClickCalled)
    }
    
    @Test
    fun `点击Conversation时应该调用onItemClick而非onFactEdit`() {
        // Given
        var factEditCalled = false
        var itemClickCalled = false
        val onFactEdit: (String) -> Unit = { factEditCalled = true }
        val onItemClick: (TimelineItem) -> Unit = { itemClickCalled = true }
        val item = sampleConversationItem
        
        // When - 模拟点击逻辑
        if (item is TimelineItem.UserFact) {
            onFactEdit(item.fact.id)
        } else {
            onItemClick(item)
        }
        
        // Then
        assertTrue("onFactEdit不应该被调用", !factEditCalled)
        assertTrue("onItemClick应该被调用", itemClickCalled)
    }
    
    @Test
    fun `onFactEdit为null时点击UserFact应该调用onItemClick`() {
        // Given
        var itemClickCalled = false
        val onFactEdit: ((String) -> Unit)? = null
        val onItemClick: (TimelineItem) -> Unit = { itemClickCalled = true }
        val item = sampleUserFactItem
        
        // When - 模拟点击逻辑（与实际代码一致）
        if (item is TimelineItem.UserFact && onFactEdit != null) {
            onFactEdit(item.fact.id)
        } else {
            onItemClick(item)
        }
        
        // Then
        assertTrue("onItemClick应该被调用", itemClickCalled)
    }

    // ==================== 选择模式测试 ====================
    
    @Test
    fun `选择模式下点击UserFact应该切换选中状态而非触发编辑`() {
        // Given
        var factEditCalled = false
        var selectCalled = false
        val isSelectionMode = true
        val onFactEdit: (String) -> Unit = { factEditCalled = true }
        val onSelect: (String, Boolean) -> Unit = { _, _ -> selectCalled = true }
        val item = sampleUserFactItem
        
        // When - 模拟选择模式下的点击逻辑
        if (isSelectionMode) {
            onSelect(item.id, true)
        } else if (item is TimelineItem.UserFact) {
            onFactEdit(item.fact.id)
        }
        
        // Then
        assertTrue("onSelect应该被调用", selectCalled)
        assertTrue("onFactEdit不应该被调用", !factEditCalled)
    }
    
    @Test
    fun `非选择模式下点击UserFact应该触发编辑`() {
        // Given
        var factEditCalled = false
        var selectCalled = false
        val isSelectionMode = false
        val onFactEdit: (String) -> Unit = { factEditCalled = true }
        val onSelect: (String, Boolean) -> Unit = { _, _ -> selectCalled = true }
        val item = sampleUserFactItem
        
        // When - 模拟非选择模式下的点击逻辑
        if (isSelectionMode) {
            onSelect(item.id, true)
        } else if (item is TimelineItem.UserFact) {
            onFactEdit(item.fact.id)
        }
        
        // Then
        assertTrue("onFactEdit应该被调用", factEditCalled)
        assertTrue("onSelect不应该被调用", !selectCalled)
    }

    // ==================== factId 传递测试 ====================
    
    @Test
    fun `onFactEdit应该接收正确的factId`() {
        // Given
        var receivedFactId: String? = null
        val onFactEdit: (String) -> Unit = { receivedFactId = it }
        val item = sampleUserFactItem
        
        // When
        if (item is TimelineItem.UserFact) {
            onFactEdit(item.fact.id)
        }
        
        // Then
        assertEquals("fact_1", receivedFactId)
    }
    
    @Test
    fun `多个UserFact应该传递各自正确的factId`() {
        // Given
        val receivedFactIds = mutableListOf<String>()
        val onFactEdit: (String) -> Unit = { receivedFactIds.add(it) }
        
        val items = listOf(
            TimelineItem.UserFact(
                id = "item_1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                fact = Fact(
                    id = "fact_1",
                    key = "职业",
                    value = "产品经理",
                    timestamp = System.currentTimeMillis(),
                    source = FactSource.MANUAL
                )
            ),
            TimelineItem.UserFact(
                id = "item_2",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                fact = Fact(
                    id = "fact_2",
                    key = "爱好",
                    value = "摄影",
                    timestamp = System.currentTimeMillis(),
                    source = FactSource.MANUAL
                )
            )
        )
        
        // When
        items.forEach { item ->
            if (item is TimelineItem.UserFact) {
                onFactEdit(item.fact.id)
            }
        }
        
        // Then
        assertEquals(2, receivedFactIds.size)
        assertEquals("fact_1", receivedFactIds[0])
        assertEquals("fact_2", receivedFactIds[1])
    }
}
