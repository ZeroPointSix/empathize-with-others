package com.empathy.ai.testutil

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import com.empathy.ai.presentation.ui.screen.contact.DetailTab

/**
 * 统一的测试数据工厂（Android测试版本）
 *
 * 提供标准化的测试数据创建方法，确保测试数据一致性和可维护性
 *
 * 参考标准：
 * - [CR-00010] 测试数据管理改进建议
 */
object TestDataFactory {

    // ==================== ContactProfile ====================

    fun createContactProfile(
        id: String = "contact_test_1",
        name: String = "测试联系人",
        targetGoal: String = "测试目标",
        contextDepth: Int = 10,
        facts: List<Fact> = emptyList(),
        relationshipScore: Int = 75,
        lastInteractionDate: String? = null,
        avatarUrl: String? = null
    ) = ContactProfile(
        id = id,
        name = name,
        targetGoal = targetGoal,
        contextDepth = contextDepth,
        facts = facts,
        relationshipScore = relationshipScore,
        lastInteractionDate = lastInteractionDate,
        avatarUrl = avatarUrl
    )

    // ==================== ContactDetailUiState ====================

    fun createContactDetailUiState(
        currentTab: DetailTab = DetailTab.Overview,
        isLoading: Boolean = false,
        error: String? = null,
        contact: ContactProfile? = createContactProfile(),
        viewMode: ViewMode = ViewMode.Timeline,
        selectedFilters: Set<FilterType> = emptySet(),
        timelineItems: List<TimelineItem> = emptyList(),
        facts: List<Fact> = emptyList(),
        conversationCount: Int = 0,
        summaryCount: Int = 0
    ) = ContactDetailUiState(
        currentTab = currentTab,
        isLoading = isLoading,
        error = error,
        contact = contact,
        viewMode = viewMode,
        selectedFilters = selectedFilters,
        timelineItems = timelineItems,
        facts = facts,
        conversationCount = conversationCount,
        summaryCount = summaryCount
    )

    // ==================== TimelineItem ====================

    fun createTimelineItems(
        count: Int = 5,
        startTimestamp: Long = System.currentTimeMillis()
    ): List<TimelineItem> = (1..count).map { index ->
        when (index % 4) {
            0 -> createPhotoMoment(id = "photo_$index", timestamp = startTimestamp - index * 3600000L)
            1 -> createAiSummary(id = "summary_$index", timestamp = startTimestamp - index * 3600000L)
            2 -> createMilestone(id = "milestone_$index", timestamp = startTimestamp - index * 3600000L)
            else -> createConversation(id = "conv_$index", timestamp = startTimestamp - index * 3600000L)
        }
    }

    fun createPhotoMoment(
        id: String = "photo_test_1",
        timestamp: Long = System.currentTimeMillis(),
        emotionType: EmotionType = EmotionType.SWEET,
        imageUrl: String = "https://example.com/photo.jpg",
        caption: String = "测试照片描述"
    ) = TimelineItem.PhotoMoment(
        id = id,
        timestamp = timestamp,
        emotionType = emotionType,
        imageUrl = imageUrl,
        caption = caption
    )

    fun createAiSummary(
        id: String = "summary_test_1",
        timestamp: Long = System.currentTimeMillis(),
        emotionType: EmotionType = EmotionType.NEUTRAL,
        content: String = "今天聊得很开心",
        keyEvents: List<KeyEvent> = listOf(
            KeyEvent(event = "讨论周末计划", importance = 7),
            KeyEvent(event = "分享美食照片", importance = 5)
        )
    ) = TimelineItem.AiSummary(
        id = id,
        timestamp = timestamp,
        emotionType = emotionType,
        summary = DailySummary(
            id = 0,
            contactId = "contact_test_1",
            summaryDate = java.time.LocalDate.now().toString(),
            content = content,
            keyEvents = keyEvents,
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = 0,
            relationshipTrend = RelationshipTrend.STABLE
        )
    )

    fun createMilestone(
        id: String = "milestone_test_1",
        timestamp: Long = System.currentTimeMillis(),
        emotionType: EmotionType = EmotionType.GIFT,
        title: String = "相识100天",
        description: String = "从陌生到熟悉",
        icon: String = "🏆"
    ) = TimelineItem.Milestone(
        id = id,
        timestamp = timestamp,
        emotionType = emotionType,
        title = title,
        description = description,
        icon = icon
    )

    fun createConversation(
        id: String = "conv_test_1",
        timestamp: Long = System.currentTimeMillis(),
        emotionType: EmotionType = EmotionType.NEUTRAL,
        preview: String = "你好，最近怎么样？",
        messageCount: Int = 10
    ) = TimelineItem.Conversation(
        id = id,
        timestamp = timestamp,
        emotionType = emotionType,
        preview = preview,
        messageCount = messageCount
    )

    // ==================== BrainTag ====================

    fun createBrainTags(
        count: Int = 5,
        contactId: String = "contact_test_1"
    ): List<BrainTag> = (1..count).map { index ->
        createBrainTag(
            id = index.toLong(),
            contactId = contactId,
            content = "测试标签$index",
            type = if (index % 2 == 0) TagType.RISK_RED else TagType.STRATEGY_GREEN,
            isConfirmed = index % 3 == 0
        )
    }

    fun createBrainTag(
        id: Long = 1L,
        contactId: String = "contact_test_1",
        content: String = "测试标签",
        type: TagType = TagType.STRATEGY_GREEN,
        isConfirmed: Boolean = false,
        source: String = "ai"
    ) = BrainTag(
        id = id,
        contactId = contactId,
        content = content,
        type = type,
        isConfirmed = isConfirmed,
        source = source
    )

    // ==================== Fact ====================

    fun createFacts(count: Int = 5): List<Fact> = (1..count).map { index ->
        createFact(
            key = "测试键$index",
            value = "测试值$index",
            source = if (index % 2 == 0) FactSource.MANUAL else FactSource.AI_INFERRED
        )
    }

    fun createFact(
        key: String = "兴趣爱好",
        value: String = "喜欢吃辣",
        source: FactSource = FactSource.MANUAL,
        timestamp: Long = System.currentTimeMillis()
    ) = Fact(
        key = key,
        value = value,
        timestamp = timestamp,
        source = source
    )

    // ==================== DataStatus ====================

    fun createDataSourceStatuses(): Map<String, DataStatus> = mapOf(
        "wechat" to DataStatus.COMPLETED,
        "sms" to DataStatus.NOT_AVAILABLE,
        "call" to DataStatus.PROCESSING,
        "photo" to DataStatus.FAILED
    )
}
