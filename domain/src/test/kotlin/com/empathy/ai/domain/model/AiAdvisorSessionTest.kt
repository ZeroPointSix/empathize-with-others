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
 * TD-00026 T013: 测试AI军师会话模型的创建和属性
 */
class AiAdvisorSessionTest {

    @Test
    fun `create should generate unique id`() {
        val session1 = AiAdvisorSession.create(contactId = "contact1")
        val session2 = AiAdvisorSession.create(contactId = "contact1")

        assertNotEquals(session1.id, session2.id)
    }

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

    @Test
    fun `id should be valid UUID format`() {
        val session = AiAdvisorSession.create(contactId = "contact1")

        // UUID格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(session.id.matches(uuidRegex))
    }
}
