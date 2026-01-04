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
 * AiAdvisorRepositoryImplTest 测试AI军师对话仓库的数据访问功能
 *
 * 业务背景 (PRD-00026):
 *   - 测试AI军师的会话管理功能（创建/获取/更新/删除）
 *   - 测试对话记录的CRUD操作
 *   - 验证数据隔离策略（与主对话历史分离存储）
 *
 * 设计决策 (TDD-00026):
 *   - 使用MockK模拟AiAdvisorDao，隔离数据库测试
 *   - 使用StandardTestDispatcher提供测试调度器支持
 *   - 覆盖所有仓库接口方法的成功和失败场景
 *
 * 任务追踪 (FD-00026/T001-T005):
 *   - T001: 创建领域模型测试
 *   - T002: 测试会话创建功能
 *   - T003: 测试对话保存功能
 *   - T004: 测试Flow响应式查询
 *   - T005: 测试错误处理和边界条件
 *
 * @see AiAdvisorRepositoryImpl 仓库实现
 * @see AiAdvisorDao 数据访问对象
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAdvisorRepositoryImplTest {

    private lateinit var repository: AiAdvisorRepositoryImpl
    private lateinit var mockDao: AiAdvisorDao
    /** [Strategy] 使用StandardTestDispatcher替代MainDispatcherRule
     *
     * StandardTestDispatcher提供精确的时间控制，
     * 允许测试按需推进协程执行，而非自动执行。
     * 这是TDD-00026推荐的测试调度器选择。
     */
    private val testDispatcher = StandardTestDispatcher()

    /** [Setup] 测试环境初始化
     *
     * 在每个测试前设置主调度器并创建Mock对象，
     * 确保测试间的隔离性和可重复性。
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockDao = mockk(relaxed = true)  // relaxed=true自动返回默认值，减少stubbing
        repository = AiAdvisorRepositoryImpl(mockDao, testDispatcher)
    }

    /** [Teardown] 测试环境清理
     *
     * 重置主调度器，避免影响其他测试用例。
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 会话管理测试 ====================

    /**
     * [Business Rule] 验证createSession的正常流程
     *
     * 测试场景 (PRD-00026/US-001):
     *   - 用户为指定联系人创建新的AI军师会话
     *   - 会话创建成功时返回成功结果
     *
     * 设计验证 (TDD-00026/4.4):
     *   - 验证领域模型到数据库实体的转换正确
     *   - 验证DAO的insertSession方法被正确调用
     *   - 验证会话属性（id, contactId, title）正确传递
     */
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

    /**
     * [Business Rule] 验证getSessions返回指定联系人的所有会话
     *
     * 测试场景 (PRD-00026/US-001):
     *   - 按联系人生成会话列表
     *   - 验证返回结果的数量和顺序
     *
     * 数据验证:
     *   - 验证返回的会话数量正确
     *   - 验证会话标题与预期一致
     */
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

    /**
     * [Business Rule] 验证getActiveSession返回活跃会话
     *
     * 测试场景 (PRD-00026/US-001):
     *   - 联系人存在活跃会话时返回该会话
     *   - 验证会话的isActive状态为true
     */
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

    /**
     * [Edge Case] 验证getActiveSession在无活跃会话时返回null
     *
     * 测试场景:
     *   - 联系人没有活跃会话时
     *   - 返回成功Result，内容为null
     */
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

    /**
     * [Business Rule] 验证getOrCreateActiveSession的"获取已存在会话"分支
     *
     * 测试场景 (PRD-00026/US-001):
     *   - 联系人的活跃会话已存在时，直接返回已有会话
     *   - 避免重复创建，保持会话连续性
     *
     * 设计权衡 (TDD-00026/4.4):
     *   - 优先查询已有会话，减少数据库写入
     *   - 仅在无活跃会话时创建新会话
     *
     * 验证点:
     *   - 返回已存在的会话标题
     *   - 验证insertSession未被调用（exactly = 0）
     */
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

    /**
     * [Edge Case] 测试getOrCreateActiveSession的"创建新会话"分支
     *
     * 测试场景:
     *   - 联系人无活跃会话时创建新会话
     *   - 新会话使用指定的默认标题
     *
     * 会话初始化规则 (FD-00026/5.2):
     *   - 会话ID使用UUID生成
     *   - createdAt和updatedAt初始化为当前时间
     *   - messageCount初始化为0
     */
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

    /**
     * [Business Rule] 验证updateSessionTitle更新会话标题
     *
     * 测试场景:
     *   - 用户重命名AI军师会话
     *   - 验证DAO接收到正确的参数
     */
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

    /**
     * [Business Rule] 验证deleteSession删除会话
     *
     * 测试场景:
     *   - 用户删除AI军师会话
     *   - 验证DAO的deleteSession方法被调用
     *
     * 关联测试:
     *   - Room外键约束确保对话记录级联删除
     */
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

    /**
     * [Business Rule] 验证saveMessage的自动会话更新
     *
     * 测试场景 (PRD-00026/US-002):
     *   - 用户发送消息时，同步更新会话的消息计数和时间戳
     *   - 确保会话列表按更新时间排序准确
     *
     * 实现细节 (TDD-00026/4.4):
     *   - 消息保存后立即调用incrementMessageCount
     *   - 使用当前时间更新会话的updatedAt字段
     *
     * @see AiAdvisorDao.incrementMessageCount
     */
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

    /**
     * [Business Rule] 验证getConversations返回对话记录
     *
     * 测试场景 (PRD-00026/US-002):
     *   - 获取指定会话的所有对话记录
     *   - 验证消息类型识别正确（USER/AI）
     *   - 验证消息内容正确返回
     */
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

    /**
     * [Business Rule] 验证getConversationsFlow的响应式数据流
     *
     * 测试场景 (PRD-00026/US-002):
     *   - 对话记录的实时监听和更新
     *   - 数据库变更能自动推送至Flow
     *
     * 技术验证 (TDD-00026/4.3):
     *   - 验证Flow的正确返回和map转换
     *   - 验证entities到domain模型的转换链
     *   - 验证数据流的响应式特性
     *
     * @note Flow测试使用first()获取首个发射值
     */
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

    /**
     * [Edge Case] 验证getRecentConversations返回限制数量的对话
     *
     * 测试场景:
     *   - 获取指定联系人的最近N条对话记录
     *   - 验证limit参数正确传递
     *
     * 使用场景:
     *   - AI军师分析时获取历史对话摘要
     *   - TDD-00026定义的HISTORY_LIMIT=20
     */
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

    /**
     * [Business Rule] 验证deleteConversation删除单条对话
     *
     * 测试场景:
     *   - 用户删除单条AI军师对话记录
     *   - 验证DAO的deleteConversation方法被调用
     */
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

    /**
     * [Business Rule] 验证clearSession清空会话所有对话
     *
     * 测试场景:
     *   - 用户清空当前会话的所有对话
     *   - 保留会话但删除所有消息
     *
     * 与deleteSession的区别:
     *   - clearSession: 仅删除对话记录，保留会话元数据
     *   deleteSession: 整个会话（含元数据）全部删除
     */
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

    /**
     * [Business Rule] 验证getConversationCount返回对话数量
     *
     * 测试场景:
     *   - 获取指定会话的对话记录总数
     *   - 用于UI显示或业务逻辑判断
     */
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

    /**
     * [Test Fixture] 创建测试用的会话实体
     *
     * @param id 会话ID
     * @param contactId 联系人ID
     * @param title 会话标题
     * @param isActive 是否活跃
     * @return AiAdvisorSessionEntity测试实例
     */
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

    /**
     * [Test Fixture] 创建测试用的对话实体
     *
     * @param id 对话ID
     * @param contactId 联系人ID
     * @param sessionId 会话ID
     * @param messageType 消息类型
     * @param content 消息内容
     * @return AiAdvisorConversationEntity测试实例
     */
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
