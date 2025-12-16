package com.empathy.ai.domain.model

import com.empathy.ai.testutil.PromptTestDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptContext单元测试
 */
class PromptContextTest {

    @Test
    fun `getVariable should return contactName for contact_name`() {
        val context = PromptTestDataFactory.createPromptContext(contactName = "小明")
        assertEquals("小明", context.getVariable("contact_name"))
    }

    @Test
    fun `getVariable should be case insensitive`() {
        val context = PromptTestDataFactory.createPromptContext(contactName = "小红")
        assertEquals("小红", context.getVariable("CONTACT_NAME"))
        assertEquals("小红", context.getVariable("Contact_Name"))
    }

    @Test
    fun `getVariable should return relationshipStatus`() {
        val context = PromptTestDataFactory.createPromptContext(relationshipStatus = "好友")
        assertEquals("好友", context.getVariable("relationship_status"))
    }

    @Test
    fun `getVariable should return joined riskTags`() {
        val context = PromptTestDataFactory.createPromptContext(
            riskTags = listOf("不喜欢被催", "讨厌迟到")
        )
        assertEquals("不喜欢被催、讨厌迟到", context.getVariable("risk_tags"))
    }

    @Test
    fun `getVariable should return 无 for empty riskTags`() {
        val context = PromptTestDataFactory.createPromptContext(riskTags = emptyList())
        assertEquals("无", context.getVariable("risk_tags"))
    }

    @Test
    fun `getVariable should return joined strategyTags`() {
        val context = PromptTestDataFactory.createPromptContext(
            strategyTags = listOf("喜欢被夸", "爱聊美食")
        )
        assertEquals("喜欢被夸、爱聊美食", context.getVariable("strategy_tags"))
    }

    @Test
    fun `getVariable should return 无 for empty strategyTags`() {
        val context = PromptTestDataFactory.createPromptContext(strategyTags = emptyList())
        assertEquals("无", context.getVariable("strategy_tags"))
    }

    @Test
    fun `getVariable should return factsCount as string`() {
        val context = PromptTestDataFactory.createPromptContext(factsCount = 10)
        assertEquals("10", context.getVariable("facts_count"))
    }

    @Test
    fun `getVariable should return todayDate`() {
        val context = PromptTestDataFactory.createPromptContext(todayDate = "2025-12-16")
        assertEquals("2025-12-16", context.getVariable("today_date"))
    }

    @Test
    fun `getVariable should return null for unknown variable`() {
        val context = PromptTestDataFactory.createPromptContext()
        assertNull(context.getVariable("unknown_variable"))
    }

    @Test
    fun `fromContact should extract contact name`() {
        val profile = ContactProfile(
            id = "test_id",
            name = "测试联系人",
            targetGoal = "成为好朋友",
            facts = emptyList(),
            relationshipScore = 50
        )
        val context = PromptContext.fromContact(profile)
        assertEquals("测试联系人", context.contactName)
    }

    @Test
    fun `fromContact should extract relationship status`() {
        val profile = ContactProfile(
            id = "test_id",
            name = "测试",
            targetGoal = "目标",
            facts = emptyList(),
            relationshipScore = 60
        )
        val context = PromptContext.fromContact(profile)
        assertTrue(context.relationshipStatus?.isNotBlank() == true)
    }

    @Test
    fun `fromContact should extract risk tags from facts`() {
        val profile = ContactProfile(
            id = "test_id",
            name = "测试",
            targetGoal = "目标",
            facts = listOf(
                Fact("雷区", "不要提前任", System.currentTimeMillis(), FactSource.USER_INPUT),
                Fact("其他", "普通信息", System.currentTimeMillis(), FactSource.USER_INPUT)
            ),
            relationshipScore = 50
        )
        val context = PromptContext.fromContact(profile)
        assertTrue(context.riskTags.contains("不要提前任"))
        assertEquals(1, context.riskTags.size)
    }

    @Test
    fun `fromContact should extract strategy tags from facts`() {
        val profile = ContactProfile(
            id = "test_id",
            name = "测试",
            targetGoal = "目标",
            facts = listOf(
                Fact("策略", "多聊共同爱好", System.currentTimeMillis(), FactSource.USER_INPUT),
                Fact("其他", "普通信息", System.currentTimeMillis(), FactSource.USER_INPUT)
            ),
            relationshipScore = 50
        )
        val context = PromptContext.fromContact(profile)
        assertTrue(context.strategyTags.contains("多聊共同爱好"))
        assertEquals(1, context.strategyTags.size)
    }

    @Test
    fun `fromContact should count all facts`() {
        val profile = ContactProfile(
            id = "test_id",
            name = "测试",
            targetGoal = "目标",
            facts = listOf(
                Fact("雷区", "值1", System.currentTimeMillis(), FactSource.USER_INPUT),
                Fact("策略", "值2", System.currentTimeMillis(), FactSource.USER_INPUT),
                Fact("其他", "值3", System.currentTimeMillis(), FactSource.USER_INPUT)
            ),
            relationshipScore = 50
        )
        val context = PromptContext.fromContact(profile)
        assertEquals(3, context.factsCount)
    }

    @Test
    fun `fromContact should set todayDate`() {
        val profile = ContactProfile(
            id = "test_id",
            name = "测试",
            targetGoal = "目标",
            facts = emptyList(),
            relationshipScore = 50
        )
        val context = PromptContext.fromContact(profile)
        assertTrue(context.todayDate.isNotBlank())
    }

    @Test
    fun `empty context should have default values`() {
        val context = PromptTestDataFactory.createEmptyPromptContext()
        assertNull(context.contactName)
        assertNull(context.relationshipStatus)
        assertTrue(context.riskTags.isEmpty())
        assertTrue(context.strategyTags.isEmpty())
        assertEquals(0, context.factsCount)
    }
}
