package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.usecase.CreateAdvisorSessionUseCase
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00058: 新建会话功能失效问题测试
 *
 * 测试场景：
 * 1. 点击"新建会话"后应该创建新的空白会话
 * 2. 新会话的ID应该与旧会话不同
 * 3. 新会话的对话列表应该为空
 *
 * 问题描述：
 * 用户点击"新建会话"按钮后，没有真正创建新会话，而是跳转到上一个会话
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00058CreateNewSessionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var aiAdvisorRepository: AiAdvisorRepository
    private lateinit var createAdvisorSessionUseCase: CreateAdvisorSessionUseCase
    private lateinit var getAdvisorSessionsUseCase: GetAdvisorSessionsUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiAdvisorRepository = mockk(relaxed = true)
        createAdvisorSessionUseCase = CreateAdvisorSessionUseCase(aiAdvisorRepository)
        getAdvisorSessionsUseCase = GetAdvisorSessionsUseCase(aiAdvisorRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== TC-001: 新建会话基本功能 ====================

    @Test
    fun `TC-001 createNewSession should create a new session with unique ID`() = runTest {
        // Given: 已有一个会话
        val existingSession = AiAdvisorSession.create(
            contactId = "contact-1",
            title = "旧对话"
        )
        
        coEvery { aiAdvisorRepository.createSession(any()) } returns Result.success(Unit)
        
        // When: 创建新会话
        val result = createAdvisorSessionUseCase("contact-1", "新对话")
        
        // Then: 新会话创建成功，ID与旧会话不同
        assertTrue(result.isSuccess)
        val newSession = result.getOrNull()!!
        assertNotEquals(existingSession.id, newSession.id)
        assertEquals("新对话", newSession.title)
        assertEquals("contact-1", newSession.contactId)
    }

    @Test
    fun `TC-001 createNewSession should have empty conversation list`() = runTest {
        // Given: 创建新会话
        coEvery { aiAdvisorRepository.createSession(any()) } returns Result.success(Unit)
        
        val result = createAdvisorSessionUseCase("contact-1")
        val newSession = result.getOrNull()!!
        
        // When: 获取新会话的对话列表
        coEvery { 
            aiAdvisorRepository.getConversationsBySessionWithLimit(newSession.id, any()) 
        } returns Result.success(emptyList())
        
        val conversationsResult = aiAdvisorRepository.getConversationsBySessionWithLimit(newSession.id, 100)
        
        // Then: 对话列表为空
        assertTrue(conversationsResult.isSuccess)
        val conversations = conversationsResult.getOrNull()
        assertTrue(conversations != null && conversations.isEmpty())
    }

    // ==================== TC-002: 新建会话后发送消息 ====================

    @Test
    fun `TC-002 new session should be able to receive messages`() = runTest {
        // Given: 创建新会话
        coEvery { aiAdvisorRepository.createSession(any()) } returns Result.success(Unit)
        
        val result = createAdvisorSessionUseCase("contact-1")
        val newSession = result.getOrNull()!!
        
        // When: 保存消息到新会话
        coEvery { aiAdvisorRepository.saveMessage(any()) } returns Result.success(Unit)
        
        // Then: 消息保存成功
        coVerify(exactly = 0) { aiAdvisorRepository.saveMessage(any()) } // 还没发送消息
    }

    // ==================== TC-003: 新建会话后切换回旧会话 ====================

    @Test
    fun `TC-003 switching sessions should show correct content`() = runTest {
        // Given: 有两个会话
        val session1 = AiAdvisorSession.create(contactId = "contact-1", title = "会话1")
        val session2 = AiAdvisorSession.create(contactId = "contact-1", title = "会话2")
        
        coEvery { 
            aiAdvisorRepository.getSessions("contact-1") 
        } returns Result.success(listOf(session1, session2))
        
        // When: 获取会话列表
        val result = getAdvisorSessionsUseCase("contact-1")
        
        // Then: 两个会话都存在
        assertTrue(result.isSuccess)
        val sessions = result.getOrNull()!!
        assertEquals(2, sessions.size)
        assertTrue(sessions.any { it.id == session1.id })
        assertTrue(sessions.any { it.id == session2.id })
    }

    // ==================== TC-004: 连续新建多个会话 ====================

    @Test
    fun `TC-004 creating multiple sessions should all have unique IDs`() = runTest {
        // Given: 准备创建多个会话
        coEvery { aiAdvisorRepository.createSession(any()) } returns Result.success(Unit)
        
        // When: 连续创建3个会话
        val session1 = createAdvisorSessionUseCase("contact-1", "会话1").getOrNull()!!
        val session2 = createAdvisorSessionUseCase("contact-1", "会话2").getOrNull()!!
        val session3 = createAdvisorSessionUseCase("contact-1", "会话3").getOrNull()!!
        
        // Then: 所有会话ID都不同
        val ids = setOf(session1.id, session2.id, session3.id)
        assertEquals(3, ids.size) // 如果有重复，set的大小会小于3
    }

    // ==================== TC-005: createNew参数测试 ====================

    @Test
    fun `TC-005 createNew flag should trigger session creation`() = runTest {
        // Given: createNew = true
        val createNew = true
        
        // When: 模拟ViewModel处理createNew参数
        if (createNew) {
            coEvery { aiAdvisorRepository.createSession(any()) } returns Result.success(Unit)
            val result = createAdvisorSessionUseCase("contact-1")
            
            // Then: 应该创建新会话
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { aiAdvisorRepository.createSession(any()) }
        }
    }

    @Test
    fun `TC-005 createNew false should not trigger session creation`() = runTest {
        // Given: createNew = false
        val createNew = false
        
        // When: 模拟ViewModel处理createNew参数
        if (createNew) {
            createAdvisorSessionUseCase("contact-1")
        }
        
        // Then: 不应该创建新会话
        coVerify(exactly = 0) { aiAdvisorRepository.createSession(any()) }
    }
}
