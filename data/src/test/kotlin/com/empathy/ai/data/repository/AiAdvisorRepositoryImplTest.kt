package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.AiAdvisorDao
import com.empathy.ai.data.local.entity.AiAdvisorConversationEntity
import com.empathy.ai.data.local.entity.AiAdvisorSessionEntity
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AiAdvisorRepositoryImpl单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAdvisorRepositoryImplTest {

    private lateinit var repository: AiAdvisorRepositoryImpl
    private lateinit var mockDao: AiAdvisorDao
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockDao = mockk(relaxed = true)
        repository = AiAdvisorRepositoryImpl(mockDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 会话管理测试 ====================

    @Test
    fun `createSession should insert session entity`() = runTest {
        // Given
        val session = AiAdvisorSession.create("contact-1", "Test Session")
        val entitySlot = slot<AiAdvisorSessionEntity>()
        coEvery { mockDao.insertSession(capture(entitySlot)) } returns Unit

        // When
        val result = repository.createSession(session)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.insertSession(any()) }
        assertEquals(session.id, entitySlot.captured.id)
        assertEquals(session.contactId, entitySlot.captured.contactId)
        assertEquals(session.title, entitySlot.captured.title)
    }

    @Test
    fun `getSessions should return sessions for contact`() = runTest {
        // Given
        val contactId = "contact-1"
        val entities = listOf(
            createSessionEntity("session-1", contactId, "Session 1"),
            createSessionEntity("session-2", contactId, "Session 2")
        )
        coEvery { mockDao.getSessionsByContact(contactId) } returns entities

        // When
        val result = repository.getSessions(contactId)

        // Then
        assertTrue(result.isSuccess)
        val sessions = result.getOrNull()!!
        assertEquals(2, sessions.size)
        assertEquals("Session 1", sessions[0].title)
        assertEquals("Session 2", sessions[1].title)
    }

    @Test
    fun `getActiveSession should return active session`() = runTest {
        // Given
        val contactId = "contact-1"
        val entity = createSessionEntity("session-1", contactId, "Active Session", isActive = true)
        coEvery { mockDao.getActiveSession(contactId) } returns entity

        // When
        val result = repository.getActiveSession(contactId)

        // Then
        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("Active Session", session?.title)
        assertTrue(session?.isActive == true)
    }

    @Test
    fun `getActiveSession should return null when no active session`() = runTest {
        // Given
        val contactId = "contact-1"
        coEvery { mockDao.getActiveSession(contactId) } returns null

        // When
        val result = repository.getActiveSession(contactId)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrCreateActiveSession should return existing session`() = runTest {
        // Given
        val contactId = "contact-1"
        val existingEntity = createSessionEntity("session-1", contactId, "Existing Session")
        coEvery { mockDao.getActiveSession(contactId) } returns existingEntity

        // When
        val result = repository.getOrCreateActiveSession(contactId, "New Session")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Existing Session", result.getOrNull()?.title)
        coVerify(exactly = 0) { mockDao.insertSession(any()) }
    }

    @Test
    fun `getOrCreateActiveSession should create new session when none exists`() = runTest {
        // Given
        val contactId = "contact-1"
        coEvery { mockDao.getActiveSession(contactId) } returns null
        coEvery { mockDao.insertSession(any()) } returns Unit

        // When
        val result = repository.getOrCreateActiveSession(contactId, "New Session")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("New Session", result.getOrNull()?.title)
        coVerify { mockDao.insertSession(any()) }
    }

    @Test
    fun `updateSessionTitle should call dao with correct parameters`() = runTest {
        // Given
        val sessionId = "session-1"
        val newTitle = "Updated Title"
        coEvery { mockDao.updateSessionTitle(sessionId, newTitle, any()) } returns Unit

        // When
        val result = repository.updateSessionTitle(sessionId, newTitle)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.updateSessionTitle(sessionId, newTitle, any()) }
    }

    @Test
    fun `deleteSession should call dao delete`() = runTest {
        // Given
        val sessionId = "session-1"
        coEvery { mockDao.deleteSession(sessionId) } returns Unit

        // When
        val result = repository.deleteSession(sessionId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.deleteSession(sessionId) }
    }

    // ==================== 对话管理测试 ====================

    @Test
    fun `saveMessage should insert conversation and increment count`() = runTest {
        // Given
        val conversation = AiAdvisorConversation.createUserMessage(
            sessionId = "session-1",
            contactId = "contact-1",
            content = "Hello"
        )
        coEvery { mockDao.insertConversation(any()) } returns Unit
        coEvery { mockDao.incrementMessageCount(any(), any()) } returns Unit

        // When
        val result = repository.saveMessage(conversation)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.insertConversation(any()) }
        coVerify { mockDao.incrementMessageCount("session-1", any()) }
    }

    @Test
    fun `getConversations should return conversations for session`() = runTest {
        // Given
        val sessionId = "session-1"
        val entities = listOf(
            createConversationEntity("conv-1", "contact-1", sessionId, MessageType.USER, "Hello"),
            createConversationEntity("conv-2", "contact-1", sessionId, MessageType.AI, "Hi there")
        )
        coEvery { mockDao.getConversationsBySession(sessionId) } returns entities

        // When
        val result = repository.getConversations(sessionId)

        // Then
        assertTrue(result.isSuccess)
        val conversations = result.getOrNull()!!
        assertEquals(2, conversations.size)
        assertEquals("Hello", conversations[0].content)
        assertEquals(MessageType.USER, conversations[0].messageType)
        assertEquals("Hi there", conversations[1].content)
        assertEquals(MessageType.AI, conversations[1].messageType)
    }

    @Test
    fun `getConversationsFlow should emit conversations`() = runTest {
        // Given
        val sessionId = "session-1"
        val entities = listOf(
            createConversationEntity("conv-1", "contact-1", sessionId, MessageType.USER, "Hello")
        )
        every { mockDao.getConversationsBySessionFlow(sessionId) } returns flowOf(entities)

        // When
        val flow = repository.getConversationsFlow(sessionId)
        val conversations = flow.first()

        // Then
        assertEquals(1, conversations.size)
        assertEquals("Hello", conversations[0].content)
    }

    @Test
    fun `getRecentConversations should return limited conversations`() = runTest {
        // Given
        val contactId = "contact-1"
        val limit = 10
        val entities = listOf(
            createConversationEntity("conv-1", contactId, "session-1", MessageType.USER, "Message 1"),
            createConversationEntity("conv-2", contactId, "session-1", MessageType.AI, "Message 2")
        )
        coEvery { mockDao.getRecentConversations(contactId, limit) } returns entities

        // When
        val result = repository.getRecentConversations(contactId, limit)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify { mockDao.getRecentConversations(contactId, limit) }
    }

    @Test
    fun `deleteConversation should call dao delete`() = runTest {
        // Given
        val conversationId = "conv-1"
        coEvery { mockDao.deleteConversation(conversationId) } returns Unit

        // When
        val result = repository.deleteConversation(conversationId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.deleteConversation(conversationId) }
    }

    @Test
    fun `clearSession should call dao clear`() = runTest {
        // Given
        val sessionId = "session-1"
        coEvery { mockDao.clearSessionConversations(sessionId) } returns Unit

        // When
        val result = repository.clearSession(sessionId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.clearSessionConversations(sessionId) }
    }

    @Test
    fun `getConversationCount should return count`() = runTest {
        // Given
        val sessionId = "session-1"
        coEvery { mockDao.getConversationCount(sessionId) } returns 5

        // When
        val result = repository.getConversationCount(sessionId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
    }

    // ==================== 辅助方法 ====================

    private fun createSessionEntity(
        id: String,
        contactId: String,
        title: String,
        isActive: Boolean = true
    ): AiAdvisorSessionEntity {
        val now = System.currentTimeMillis()
        return AiAdvisorSessionEntity(
            id = id,
            contactId = contactId,
            title = title,
            createdAt = now,
            updatedAt = now,
            messageCount = 0,
            isActive = isActive
        )
    }

    private fun createConversationEntity(
        id: String,
        contactId: String,
        sessionId: String,
        messageType: MessageType,
        content: String
    ): AiAdvisorConversationEntity {
        val now = System.currentTimeMillis()
        return AiAdvisorConversationEntity(
            id = id,
            contactId = contactId,
            sessionId = sessionId,
            messageType = messageType.name,
            content = content,
            timestamp = now,
            createdAt = now,
            sendStatus = SendStatus.SUCCESS.name
        )
    }
}
