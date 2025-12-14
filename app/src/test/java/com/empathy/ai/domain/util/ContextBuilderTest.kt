package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.TagType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ContextBuilder工具类单元测试
 */
class ContextBuilderTest {

    private lateinit var contextBuilder: ContextBuilder

    @Before
    fun setup() {
        contextBuilder = ContextBuilder()
    }

    // ==================== selectRelevantFacts 测试 ====================

    @Test
    fun `selectRelevantFacts返回空列表当输入为空`() {
        val result = contextBuilder.selectRelevantFacts(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `selectRelevantFacts返回全部当数量小于maxCount`() {
        val facts = createFacts(5)
        val result = contextBuilder.selectRelevantFacts(facts, maxCount = 20)
        assertEquals(5, result.size)
    }

    @Test
    fun `selectRelevantFacts限制返回数量为maxCount`() {
        val facts = createFacts(30)
        val result = contextBuilder.selectRelevantFacts(facts, maxCount = 20)
        assertEquals(20, result.size)
    }

    @Test
    fun `selectRelevantFacts优先选择手动添加的Facts`() {
        val now = System.currentTimeMillis()
        val manualFacts = (1..10).map { i ->
            Fact("手动$i", "值$i", now - i * 1000, FactSource.MANUAL)
        }
        val aiFacts = (1..15).map { i ->
            Fact("AI$i", "值$i", now - i * 1000, FactSource.AI_INFERRED)
        }
        val allFacts = manualFacts + aiFacts

        val result = contextBuilder.selectRelevantFacts(allFacts, maxCount = 15)

        // 所有手动Facts应该被选中
        val manualCount = result.count { it.source == FactSource.MANUAL }
        assertEquals(10, manualCount)
    }

    @Test
    fun `selectRelevantFacts优先选择最近7天的AI Facts`() {
        val now = System.currentTimeMillis()
        val recentThreshold = MemoryConstants.RECENT_DAYS * MemoryConstants.ONE_DAY_MILLIS

        // 最近7天的AI Facts
        val recentAiFacts = (1..5).map { i ->
            Fact("最近AI$i", "值$i", now - i * MemoryConstants.ONE_DAY_MILLIS, FactSource.AI_INFERRED)
        }
        // 超过7天的AI Facts
        val oldAiFacts = (1..10).map { i ->
            Fact("旧AI$i", "值$i", now - (recentThreshold + i * MemoryConstants.ONE_DAY_MILLIS), FactSource.AI_INFERRED)
        }
        val allFacts = recentAiFacts + oldAiFacts

        val result = contextBuilder.selectRelevantFacts(allFacts, maxCount = 8)

        // 最近的AI Facts应该优先被选中
        val recentCount = result.count { it.key.startsWith("最近") }
        assertEquals(5, recentCount)
    }

    @Test
    fun `selectRelevantFacts按时间倒序排列`() {
        val now = System.currentTimeMillis()
        val facts = (1..5).map { i ->
            Fact("键$i", "值$i", now - i * 1000, FactSource.MANUAL)
        }

        val result = contextBuilder.selectRelevantFacts(facts)

        // 验证按时间倒序
        for (i in 0 until result.size - 1) {
            assertTrue(result[i].timestamp >= result[i + 1].timestamp)
        }
    }

    @Test
    fun `selectRelevantFacts使用自定义maxCount`() {
        val facts = createFacts(30)
        val result = contextBuilder.selectRelevantFacts(facts, maxCount = 10)
        assertEquals(10, result.size)
    }

    // ==================== buildAnalysisContext 测试 ====================

    @Test
    fun `buildAnalysisContext包含联系人基本信息`() {
        val profile = createTestProfile()
        val result = contextBuilder.buildAnalysisContext(profile, emptyList(), emptyList())

        assertTrue(result.contains("【联系人信息】"))
        assertTrue(result.contains("姓名: 测试用户"))
        assertTrue(result.contains("关系分数: 50/100"))
    }

    @Test
    fun `buildAnalysisContext包含攻略目标`() {
        val profile = createTestProfile()
        val result = contextBuilder.buildAnalysisContext(profile, emptyList(), emptyList())

        assertTrue(result.contains("【攻略目标】"))
        assertTrue(result.contains("建立良好关系"))
    }

    @Test
    fun `buildAnalysisContext包含已知信息`() {
        val profile = createTestProfile(
            facts = listOf(
                Fact("职业", "产品经理", System.currentTimeMillis(), FactSource.MANUAL)
            )
        )
        val result = contextBuilder.buildAnalysisContext(profile, emptyList(), emptyList())

        assertTrue(result.contains("【已知信息】"))
        assertTrue(result.contains("职业: 产品经理"))
        assertTrue(result.contains("[手动]"))
    }

    @Test
    fun `buildAnalysisContext包含雷区警告`() {
        val profile = createTestProfile()
        val redTags = listOf(
            BrainTag(1, "contact_1", "不要提工作压力", TagType.RISK_RED, "MANUAL")
        )
        val result = contextBuilder.buildAnalysisContext(profile, redTags, emptyList())

        assertTrue(result.contains("【雷区警告】"))
        assertTrue(result.contains("不要提工作压力"))
    }

    @Test
    fun `buildAnalysisContext包含策略建议`() {
        val profile = createTestProfile()
        val greenTags = listOf(
            BrainTag(1, "contact_1", "喜欢聊摄影", TagType.STRATEGY_GREEN, "MANUAL")
        )
        val result = contextBuilder.buildAnalysisContext(profile, greenTags, emptyList())

        assertTrue(result.contains("【策略建议】"))
        assertTrue(result.contains("喜欢聊摄影"))
    }

    @Test
    fun `buildAnalysisContext包含聊天记录`() {
        val profile = createTestProfile()
        val conversations = listOf("你好", "最近怎么样？")
        val result = contextBuilder.buildAnalysisContext(profile, emptyList(), conversations)

        assertTrue(result.contains("【聊天记录】"))
        assertTrue(result.contains("你好"))
        assertTrue(result.contains("最近怎么样？"))
    }

    @Test
    fun `buildAnalysisContext区分手动和AI标签`() {
        val profile = createTestProfile()
        val tags = listOf(
            BrainTag(1, "contact_1", "手动标签", TagType.RISK_RED, "MANUAL"),
            BrainTag(2, "contact_1", "AI标签", TagType.RISK_RED, "AI_INFERRED")
        )
        val result = contextBuilder.buildAnalysisContext(profile, tags, emptyList())

        assertTrue(result.contains("[手动]"))
        assertTrue(result.contains("[AI推断]"))
    }

    @Test
    fun `buildAnalysisContext包含最后互动日期`() {
        val profile = createTestProfile(lastInteractionDate = "2025-12-14")
        val result = contextBuilder.buildAnalysisContext(profile, emptyList(), emptyList())

        assertTrue(result.contains("最后互动: 2025-12-14"))
    }

    // ==================== buildSummaryPrompt 测试 ====================

    @Test
    fun `buildSummaryPrompt包含联系人名称`() {
        val profile = createTestProfile()
        val result = contextBuilder.buildSummaryPrompt(profile, listOf("对话1"))

        assertTrue(result.contains("测试用户"))
    }

    @Test
    fun `buildSummaryPrompt包含对话记录`() {
        val profile = createTestProfile()
        val conversations = listOf("你好", "最近怎么样？")
        val result = contextBuilder.buildSummaryPrompt(profile, conversations)

        assertTrue(result.contains("你好"))
        assertTrue(result.contains("最近怎么样？"))
    }

    @Test
    fun `buildSummaryPrompt包含已知信息`() {
        val profile = createTestProfile(
            facts = listOf(
                Fact("职业", "产品经理", System.currentTimeMillis(), FactSource.MANUAL)
            )
        )
        val result = contextBuilder.buildSummaryPrompt(profile, listOf("对话"))

        assertTrue(result.contains("职业: 产品经理"))
    }

    @Test
    fun `buildSummaryPrompt限制Facts数量为10条`() {
        val facts = (1..15).map { i ->
            Fact("键$i", "值$i", System.currentTimeMillis(), FactSource.MANUAL)
        }
        val profile = createTestProfile(facts = facts)
        val result = contextBuilder.buildSummaryPrompt(profile, listOf("对话"))

        // 验证只包含前10条
        assertTrue(result.contains("键1"))
        assertTrue(result.contains("键10"))
        // 第11条不应该出现
        val count = result.split("键").size - 1
        assertTrue(count <= 10)
    }

    // ==================== 辅助方法 ====================

    private fun createFacts(count: Int): List<Fact> {
        val now = System.currentTimeMillis()
        return (1..count).map { i ->
            Fact(
                key = "键$i",
                value = "值$i",
                timestamp = now - i * 1000,
                source = if (i % 2 == 0) FactSource.MANUAL else FactSource.AI_INFERRED
            )
        }
    }

    private fun createTestProfile(
        facts: List<Fact> = emptyList(),
        lastInteractionDate: String? = null
    ): ContactProfile {
        return ContactProfile(
            id = "contact_1",
            name = "测试用户",
            targetGoal = "建立良好关系",
            contextDepth = 10,
            facts = facts,
            relationshipScore = 50,
            lastInteractionDate = lastInteractionDate
        )
    }
}
