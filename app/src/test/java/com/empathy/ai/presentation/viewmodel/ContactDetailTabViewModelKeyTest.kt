package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ContactDetailTabViewModel 时间线Key唯一性测试
 *
 * 问题背景 (BUG-00025/BUG-00026):
 *   LazyColumn key 重复导致Android系统回收组件时发生ArrayIndexOutOfBoundsException
 *   严重程度：P0 - 导致应用闪退
 *
 * 问题根因：
 * 1. buildTimelineItems() 中 UserFact 的 ID 生成方式依赖 timestamp + key 的组合
 *    - 相同timestamp和key产生相同ID
 *    - hashCode碰撞导致ID重复（如"Aa"和"BB"）
 * 2. TopTagsSection 直接使用 timestamp 作为 key
 *
 * 修复方案 (TDD-00014):
 *   为 Fact 模型添加唯一 id 字段（UUID）
 *   - Fact 创建时自动生成唯一 UUID
 *   - TimelineItem ID 统一使用 "type_uuid" 格式
 *   - Conversation: "conv_${log.id}"
 *   - Summary: "summary_${summary.id}"
 *   - UserFact: "fact_${fact.id}"
 *
 * 测试策略:
 * 1. 验证相同timestamp和key的facts生成不同ID
 * 2. 验证不同类型项目ID有正确前缀
 * 3. 验证大量facts（100个）无ID重复
 * 4. 验证hashCode碰撞不导致ID重复
 * 5. 验证Fact默认构造生成唯一ID
 *
 * 设计权衡:
 *   - 使用UUID替代timestamp+key组合，确保全局唯一
 *   - ID前缀区分数据类型，便于调试
 *   - 测试直接模拟buildTimelineItems逻辑，避免ViewModel依赖
 *
 * 任务追踪:
 *   - BUG-00025 LazyColumn key 重复导致闪退（时间线）
 *   - BUG-00026 LazyColumn key 重复导致闪退（TopTagsSection）
 *   - TD-00014 标签画像V2功能
 */
class ContactDetailTabViewModelKeyTest {

    /**
     * 测试：相同 timestamp 和 key 的 facts 应该生成不同的 ID
     */
    @Test
    fun `buildTimelineItems should generate unique IDs for facts with same timestamp and key`() {
        // Given: 多个具有相同 timestamp 和 key 的 facts
        val sameTimestamp = System.currentTimeMillis()
        val facts = listOf(
            Fact(key = "兴趣", value = "音乐", timestamp = sameTimestamp, source = FactSource.MANUAL),
            Fact(key = "兴趣", value = "电影", timestamp = sameTimestamp, source = FactSource.MANUAL),
            Fact(key = "兴趣", value = "游戏", timestamp = sameTimestamp, source = FactSource.MANUAL)
        )

        // When: 构建时间线项目
        val timelineItems = buildTimelineItemsForTest(emptyList(), emptyList(), facts)

        // Then: 所有 ID 应该唯一
        val ids = timelineItems.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("所有 ID 应该唯一", ids.size, uniqueIds.size)
    }

    /**
     * 测试：不同类型的时间线项目应该有不同的 ID 前缀
     */
    @Test
    fun `buildTimelineItems should generate IDs with correct prefixes`() {
        // Given
        val conversation = ConversationLog(
            id = 1,
            contactId = "contact_1",
            userInput = "测试输入",
            aiResponse = "测试响应",
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )
        val summary = DailySummary(
            id = 1,
            contactId = "contact_1",
            summaryDate = "2025-12-20",
            content = "测试总结",
            keyEvents = listOf(KeyEvent("事件", 3)),
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = 1,
            relationshipTrend = RelationshipTrend.STABLE
        )
        val fact = Fact(
            key = "测试",
            value = "值",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )

        // When
        val timelineItems = buildTimelineItemsForTest(
            listOf(conversation),
            listOf(summary),
            listOf(fact)
        )

        // Then
        val convItem = timelineItems.find { it is TimelineItem.Conversation }
        val summaryItem = timelineItems.find { it is TimelineItem.AiSummary }
        val factItem = timelineItems.find { it is TimelineItem.UserFact }

        assertTrue("对话项目 ID 应以 conv_ 开头", convItem?.id?.startsWith("conv_") == true)
        assertTrue("总结项目 ID 应以 summary_ 开头", summaryItem?.id?.startsWith("summary_") == true)
        assertTrue("事实项目 ID 应以 fact_ 开头", factItem?.id?.startsWith("fact_") == true)
    }

    /**
     * 测试：大量 facts 应该都有唯一 ID
     */
    @Test
    fun `buildTimelineItems should handle large number of facts with unique IDs`() {
        // Given: 100 个 facts，部分具有相同的 timestamp 和 key
        val baseTimestamp = System.currentTimeMillis()
        val facts = (0 until 100).map { index ->
            Fact(
                key = "key_${index % 10}", // 只有 10 种不同的 key
                value = "value_$index",
                timestamp = baseTimestamp + (index / 10), // 每 10 个共享一个 timestamp
                source = FactSource.MANUAL
            )
        }

        // When
        val timelineItems = buildTimelineItemsForTest(emptyList(), emptyList(), facts)

        // Then
        val ids = timelineItems.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("100 个 facts 应该生成 100 个唯一 ID", 100, uniqueIds.size)
    }

    /**
     * 测试：hashCode 冲突不应导致 ID 重复（现在使用 UUID，不再依赖 hashCode）
     */
    @Test
    fun `buildTimelineItems should handle hashCode collisions`() {
        // Given: 构造可能产生 hashCode 冲突的 keys
        // 在 Java 中，"Aa" 和 "BB" 的 hashCode 相同
        val sameTimestamp = System.currentTimeMillis()
        val facts = listOf(
            Fact(key = "Aa", value = "值1", timestamp = sameTimestamp, source = FactSource.MANUAL),
            Fact(key = "BB", value = "值2", timestamp = sameTimestamp, source = FactSource.MANUAL)
        )

        // 验证 hashCode 确实相同
        assertEquals("Aa".hashCode(), "BB".hashCode())

        // When
        val timelineItems = buildTimelineItemsForTest(emptyList(), emptyList(), facts)

        // Then: 即使 hashCode 相同，ID 也应该不同（因为使用了 UUID）
        val ids = timelineItems.map { it.id }
        assertEquals("即使 hashCode 相同，ID 也应该唯一", 2, ids.toSet().size)
    }

    /**
     * 测试：Fact 的 id 字段应该是唯一的
     */
    @Test
    fun `Fact should have unique id by default`() {
        // Given & When: 创建多个相同内容的 Fact
        val sameTimestamp = System.currentTimeMillis()
        val facts = (0 until 10).map {
            Fact(key = "兴趣", value = "音乐", timestamp = sameTimestamp, source = FactSource.MANUAL)
        }

        // Then: 所有 id 应该唯一
        val ids = facts.map { it.id }
        assertEquals("所有 Fact 的 id 应该唯一", 10, ids.toSet().size)
    }

    /**
     * 模拟 buildTimelineItems 方法的逻辑
     *
     * 这是 ContactDetailTabViewModel.buildTimelineItems() 的测试版本
     * 使用 Fact 的唯一 id 字段生成 TimelineItem ID
     */
    private fun buildTimelineItemsForTest(
        conversations: List<ConversationLog>,
        summaries: List<DailySummary>,
        facts: List<Fact>
    ): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        // 添加对话记录
        conversations.forEach { log ->
            items.add(
                TimelineItem.Conversation(
                    id = "conv_${log.id}",
                    timestamp = log.timestamp,
                    emotionType = EmotionType.NEUTRAL,
                    log = log
                )
            )
        }

        // 添加AI总结
        summaries.forEach { summary ->
            val timestamp = try {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .parse(summary.summaryDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            items.add(
                TimelineItem.AiSummary(
                    id = "summary_${summary.id}",
                    timestamp = timestamp,
                    emotionType = EmotionType.NEUTRAL,
                    summary = summary
                )
            )
        }

        // 添加用户手动添加的事实
        // 使用 Fact 的唯一 id 字段确保唯一性
        facts.forEach { fact ->
            items.add(
                TimelineItem.UserFact(
                    id = "fact_${fact.id}",
                    timestamp = fact.timestamp,
                    emotionType = EmotionType.NEUTRAL,
                    fact = fact
                )
            )
        }

        return items.sortedByDescending { it.timestamp }
    }
}
