package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 导入DailySummary相关类型
// KeyEvent和RelationshipTrend在同一包下，无需额外导入

/**
 * TimelineItem 数据模型测试
 *
 * 测试内容：
 * - 各种TimelineItem子类的创建
 * - 属性访问
 * - 数据完整性
 */
class TimelineItemTest {
    
    @Test
    fun `PhotoMoment should have correct properties`() {
        val item = TimelineItem.PhotoMoment(
            id = "photo_1",
            timestamp = 1702656000000L,
            emotionType = EmotionType.SWEET,
            photoUrl = "https://example.com/photo.jpg",
            description = "美好的一天"
        )
        
        assertEquals("photo_1", item.id)
        assertEquals(1702656000000L, item.timestamp)
        assertEquals(EmotionType.SWEET, item.emotionType)
        assertEquals("https://example.com/photo.jpg", item.photoUrl)
        assertEquals("美好的一天", item.description)
    }
    
    @Test
    fun `AiSummary should have correct properties`() {
        val summary = DailySummary(
            id = 1,
            contactId = "contact_1",
            summaryDate = "2025-12-15",
            content = "今天的互动很愉快",
            keyEvents = listOf(
                KeyEvent(event = "讨论周末计划", importance = 7),
                KeyEvent(event = "分享美食", importance = 5)
            ),
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = 2,
            relationshipTrend = RelationshipTrend.IMPROVING
        )
        
        val item = TimelineItem.AiSummary(
            id = "summary_1",
            timestamp = 1702656000000L,
            emotionType = EmotionType.NEUTRAL,
            summary = summary
        )
        
        assertEquals("summary_1", item.id)
        assertEquals(EmotionType.NEUTRAL, item.emotionType)
        assertEquals("今天的互动很愉快", item.summary.content)
        assertEquals(2, item.summary.keyEvents.size)
    }
    
    @Test
    fun `Milestone should have correct properties`() {
        val item = TimelineItem.Milestone(
            id = "milestone_1",
            timestamp = 1702656000000L,
            emotionType = EmotionType.GIFT,
            title = "相识100天",
            description = "感谢每一天的陪伴",
            icon = "🏆"
        )
        
        assertEquals("milestone_1", item.id)
        assertEquals(EmotionType.GIFT, item.emotionType)
        assertEquals("相识100天", item.title)
        assertEquals("感谢每一天的陪伴", item.description)
        assertEquals("🏆", item.icon)
    }
    
    @Test
    fun `Conversation should have correct properties`() {
        val log = ConversationLog(
            id = 1,
            contactId = "contact_1",
            userInput = "今天想约她出去",
            aiResponse = "建议用轻松的方式邀请",
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )
        
        val item = TimelineItem.Conversation(
            id = "conv_1",
            timestamp = 1702656000000L,
            emotionType = EmotionType.DATE,
            log = log
        )
        
        assertEquals("conv_1", item.id)
        assertEquals(EmotionType.DATE, item.emotionType)
        assertEquals("今天想约她出去", item.log.userInput)
        assertEquals("建议用轻松的方式邀请", item.log.aiResponse)
    }
    
    @Test
    fun `different TimelineItem types should be distinguishable`() {
        val photo = TimelineItem.PhotoMoment(
            id = "1",
            timestamp = 0L,
            emotionType = EmotionType.SWEET,
            imageUrl = "",
            caption = null
        )
        
        val milestone = TimelineItem.Milestone(
            id = "2",
            timestamp = 0L,
            emotionType = EmotionType.GIFT,
            title = "",
            description = null,
            icon = null
        )
        
        assertTrue(photo is TimelineItem.PhotoMoment)
        assertTrue(milestone is TimelineItem.Milestone)
        assertNotEquals(photo::class, milestone::class)
    }
    
    @Test
    fun `TimelineItem should support nullable fields`() {
        val photo = TimelineItem.PhotoMoment(
            id = "1",
            timestamp = 0L,
            emotionType = EmotionType.NEUTRAL,
            imageUrl = "url",
            caption = null
        )
        
        assertEquals(null, photo.caption)
        
        val milestone = TimelineItem.Milestone(
            id = "2",
            timestamp = 0L,
            emotionType = EmotionType.NEUTRAL,
            title = "标题",
            description = null,
            icon = null
        )
        
        assertEquals(null, milestone.description)
        assertEquals(null, milestone.icon)
    }
}
