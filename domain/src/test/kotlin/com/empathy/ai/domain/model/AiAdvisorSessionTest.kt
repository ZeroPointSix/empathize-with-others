package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiAdvisorSession领域模型单元测试
 *
 * 业务背景 (PRD-00026):
 * - AI军师会话是组织对话的容器，每个联系人可有多个独立会话
 * - 会话支持多轮对话，具有独立的创建时间和最后更新时间
 * - 会话标题用于区分不同主题的对话（如"约会策略分析"、"关系复盘"等）
 *
 * 设计决策 (TDD-00026):
 * - 使用UUID作为会话唯一标识符
 * - 提供工厂方法create()封装创建逻辑，设置合理默认值
 * - messageCount和isActive字段支持会话管理功能
 *
 * 任务: TD-00026/T013
 */
class AiAdvisorSessionTest {

    @Test
    fun `create should generate unique id`() {
        val session1 = AiAdvisorSession.create(contactId = "contact1")
        val session2 = AiAdvisorSession.create(contactId = "contact1")

        assertNotEquals(session1.id, session2.id)
    }

    /**
     * 验证工厂方法默认值：新会话使用"新对话"作为默认标题
     *
     * 业务规则 (PRD-00026):
     * - 新建会话时自动生成一个通用标题，用户可后续修改
     * - 默认标题简洁明了，适用于大多数首次对话场景
     */
    @Test
    fun `create should set default title`() {
        val session = AiAdvisorSession.create(contactId = "contact1")

        assertEquals("新对话", session.title)
    }

    @Test
    fun `create should set custom title`() {
        val session = AiAdvisorSession.create(
            contactId = "contact1",
            title = "关于约会的建议"
        )

        assertEquals("关于约会的建议", session.title)
    }

    @Test
    fun `create should set contactId correctly`() {
        val contactId = "test-contact-123"
        val session = AiAdvisorSession.create(contactId = contactId)

        assertEquals(contactId, session.contactId)
    }

    /**
     * 验证工厂方法默认值：新创建的消息计数为0
     *
     * 设计权衡 (TDD-00026):
     * - messageCount由数据库触发器或Repository层维护
     * - 工厂方法只设置初始值0，实际计数在消息保存时递增
     */
    @Test
    fun `create should initialize messageCount to zero`() {
        val session = AiAdvisorSession.create(contactId = "contact1")

        assertEquals(0, session.messageCount)
    }

    @Test
    fun `create should set isActive to true by default`() {
        val session = AiAdvisorSession.create(contactId = "contact1")

        assertTrue(session.isActive)
    }

    @Test
    fun `create should set createdAt and updatedAt`() {
        val beforeCreate = System.currentTimeMillis()
        val session = AiAdvisorSession.create(contactId = "contact1")
        val afterCreate = System.currentTimeMillis()

        assertTrue(session.createdAt >= beforeCreate)
        assertTrue(session.createdAt <= afterCreate)
        assertEquals(session.createdAt, session.updatedAt)
    }

    @Test
    fun `session with same id should be equal`() {
        val id = "test-session-id"
        val session1 = AiAdvisorSession(
            id = id,
            contactId = "contact1",
            title = "Title 1",
            createdAt = 1000L,
            updatedAt = 1000L,
            messageCount = 0,
            isActive = true
        )
        val session2 = AiAdvisorSession(
            id = id,
            contactId = "contact2",
            title = "Title 2",
            createdAt = 2000L,
            updatedAt = 2000L,
            messageCount = 5,
            isActive = false
        )

        assertEquals(session1, session2)
    }

    @Test
    fun `session with different id should not be equal`() {
        val session1 = AiAdvisorSession.create(contactId = "contact1")
        val session2 = AiAdvisorSession.create(contactId = "contact1")

        assertNotEquals(session1, session2)
    }

    @Test
    fun `copy should create new instance with updated values`() {
        val original = AiAdvisorSession.create(contactId = "contact1")
        val updated = original.copy(
            title = "Updated Title",
            messageCount = 10,
            isActive = false
        )

        assertEquals(original.id, updated.id)
        assertEquals(original.contactId, updated.contactId)
        assertEquals("Updated Title", updated.title)
        assertEquals(10, updated.messageCount)
        assertFalse(updated.isActive)
    }

    @Test
    fun `id should not be empty`() {
        val session = AiAdvisorSession.create(contactId = "contact1")

        assertTrue(session.id.isNotEmpty())
    }

    /**
     * 验证唯一标识符格式：使用标准UUID格式
     *
     * 设计决策 (TDD-00026):
     * - UUID格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     * - 使用Java UUID.randomUUID()生成，确保全局唯一性
     *
     * 业务意义:
     * - 避免分布式环境下的ID冲突
     * - 支持离线创建会话，后合并时不会产生冲突
     */
    @Test
    fun `id should be valid UUID format`() {
        val session = AiAdvisorSession.create(contactId = "contact1")

        // UUID格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(session.id.matches(uuidRegex))
    }
}
