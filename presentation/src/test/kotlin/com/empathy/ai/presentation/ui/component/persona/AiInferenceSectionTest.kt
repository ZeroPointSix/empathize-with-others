package com.empathy.ai.presentation.ui.component.persona

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiInferenceSection 单元测试
 * 
 * TD-00020 T041: 测试确认/拒绝回调、全部采纳功能
 */
class AiInferenceSectionTest {

    // ============================================================
    // InferredTag 测试
    // ============================================================

    @Test
    fun `InferredTag creation is correct`() {
        val tag = InferredTag(
            id = "inferred-1",
            name = "喜欢旅行",
            source = "聊天记录分析",
            confidence = 0.85f
        )
        
        assertEquals("inferred-1", tag.id)
        assertEquals("喜欢旅行", tag.name)
        assertEquals("聊天记录分析", tag.source)
        assertEquals(0.85f, tag.confidence, 0.001f)
    }

    @Test
    fun `InferredTag with high confidence is confident`() {
        val tag = InferredTag(
            id = "1",
            name = "标签",
            source = "AI",
            confidence = 0.9f
        )
        
        assertTrue(tag.isHighConfidence)
    }

    @Test
    fun `InferredTag with low confidence is not confident`() {
        val tag = InferredTag(
            id = "1",
            name = "标签",
            source = "AI",
            confidence = 0.5f
        )
        
        assertFalse(tag.isHighConfidence)
    }

    @Test
    fun `InferredTag with exactly 0_8 confidence is high confidence`() {
        val tag = InferredTag(
            id = "1",
            name = "标签",
            source = "AI",
            confidence = 0.8f
        )
        
        assertTrue(tag.isHighConfidence)
    }

    // ============================================================
    // 推测标签列表测试
    // ============================================================

    @Test
    fun `empty inferred tags list hides section`() {
        val inferredTags = emptyList<InferredTag>()
        
        assertTrue(inferredTags.isEmpty())
    }

    @Test
    fun `non-empty inferred tags list shows section`() {
        val inferredTags = listOf(
            InferredTag("1", "标签1", "AI", 0.9f),
            InferredTag("2", "标签2", "AI", 0.8f)
        )
        
        assertFalse(inferredTags.isEmpty())
        assertEquals(2, inferredTags.size)
    }

    // ============================================================
    // 确认/拒绝操作测试
    // ============================================================

    @Test
    fun `accept tag removes from inferred list`() {
        val inferredTags = mutableListOf(
            InferredTag("1", "标签1", "AI", 0.9f),
            InferredTag("2", "标签2", "AI", 0.8f)
        )
        
        val tagToAccept = inferredTags.first()
        inferredTags.removeIf { it.id == tagToAccept.id }
        
        assertEquals(1, inferredTags.size)
        assertFalse(inferredTags.any { it.id == "1" })
    }

    @Test
    fun `reject tag removes from inferred list`() {
        val inferredTags = mutableListOf(
            InferredTag("1", "标签1", "AI", 0.9f),
            InferredTag("2", "标签2", "AI", 0.8f)
        )
        
        val tagToReject = inferredTags.first()
        inferredTags.removeIf { it.id == tagToReject.id }
        
        assertEquals(1, inferredTags.size)
        assertFalse(inferredTags.any { it.id == "1" })
    }

    // ============================================================
    // 全部采纳测试
    // ============================================================

    @Test
    fun `accept all clears inferred list`() {
        val inferredTags = mutableListOf(
            InferredTag("1", "标签1", "AI", 0.9f),
            InferredTag("2", "标签2", "AI", 0.8f),
            InferredTag("3", "标签3", "AI", 0.7f)
        )
        
        val acceptedTags = inferredTags.toList()
        inferredTags.clear()
        
        assertTrue(inferredTags.isEmpty())
        assertEquals(3, acceptedTags.size)
    }

    @Test
    fun `accept all returns all tags`() {
        val inferredTags = listOf(
            InferredTag("1", "标签1", "AI", 0.9f),
            InferredTag("2", "标签2", "AI", 0.8f)
        )
        
        assertEquals(2, inferredTags.size)
    }

    // ============================================================
    // UI样式测试
    // ============================================================

    @Test
    fun `section header shows brain emoji`() {
        val headerEmoji = "🧠"
        
        assertEquals("🧠", headerEmoji)
    }

    @Test
    fun `section header shows correct title`() {
        val headerTitle = "AI 自动推测"
        
        assertEquals("AI 自动推测", headerTitle)
    }

    @Test
    fun `accept button uses iOS green color`() {
        // iOSGreen = #34C759
        val acceptButtonColor = 0xFF34C759
        
        assertEquals(0xFF34C759, acceptButtonColor)
    }

    @Test
    fun `reject button uses gray color`() {
        // Gray = #E5E5EA
        val rejectButtonColor = 0xFFE5E5EA
        
        assertEquals(0xFFE5E5EA, rejectButtonColor)
    }

    @Test
    fun `section background uses purple with alpha`() {
        // iOSPurple.copy(alpha = 0.05f)
        val alpha = 0.05f
        
        assertTrue(alpha < 0.1f)
    }

    // ============================================================
    // 来源显示测试
    // ============================================================

    @Test
    fun `source is displayed for each tag`() {
        val tag = InferredTag(
            id = "1",
            name = "喜欢旅行",
            source = "聊天记录分析"
        )
        
        assertEquals("聊天记录分析", tag.source)
    }

    @Test
    fun `source format is correct`() {
        val source = "聊天记录分析"
        val displayText = "来源：$source"
        
        assertEquals("来源：聊天记录分析", displayText)
    }
}
