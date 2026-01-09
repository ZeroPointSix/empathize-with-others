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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00060: 会话管理增强功能测试
 *
 * 测试场景：
 * 1. 空会话复用 - 存在空会话时不创建新会话
 * 2. 空会话复用 - 不存在空会话时创建新会话
 * 3. 会话自动命名 - 第一条消息更新标题
 * 4. 会话自动命名 - 长消息截断
 * 5. 会话自动命名 - 非第一条消息不更新标题
 * 6. 会话重命名功能
 * 7. 会话置顶功能
 * 8. 会话取消置顶功能
 *
 * 问题描述：
 * - 每次进入AI军师都创建新会话，即使上一个会话是空的
 * - 所有会话都叫"新对话"，难以区分
 * - 缺少重命名和置顶功能
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00060SessionManagementTest {

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

    // ==================== 功能1：空会话复用 ====================
    // 注意：以下测试需要在AiAdvisorRepository添加getLatestEmptySession方法后启用

    @Test
    fun `TC-001 should identify empty session by messageCount`() = runTest {
        // Given: 一个空会话（messageCount=0）
        val emptySession = AiAdvisorSession(
            id = "empty-session-1",
            contactId = "contact-1",
            title = "新对话",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 0,
            isActive = true
        )
        
        // When: 检查是否为空会话
        val isEmpty = emptySession.messageCount == 0
        
        // Then: 应该识别为空会话
        assertTrue(isEmpty)
    }

    @Test
    fun `TC-002 should identify non-empty session by messageCount`() = runTest {
        // Given: 一个非空会话（messageCount > 0）
        val nonEmptySession = AiAdvisorSession(
            id = "session-1",
            contactId = "contact-1",
            title = "已有对话",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 5,
            isActive = true
        )
        
        // When: 检查是否为空会话
        val isEmpty = nonEmptySession.messageCount == 0
        
        // Then: 不应该识别为空会话
        assertFalse(isEmpty)
    }

    @Test
    fun `TC-001b empty session reuse logic`() = runTest {
        // Given: 会话列表中有一个空会话
        val emptySession = createSession("empty-1", messageCount = 0)
        val nonEmptySession = createSession("non-empty-1", messageCount = 3)
        val sessions = listOf(emptySession, nonEmptySession)
        
        // When: 查找可复用的空会话
        val reusableSession = sessions.find { it.messageCount == 0 }
        
        // Then: 应该找到空会话
        assertNotNull(reusableSession)
        assertEquals("empty-1", reusableSession?.id)
    }

    // ==================== 功能2：会话自动命名 ====================

    @Test
    fun `TC-003 should generate title from first message`() = runTest {
        // Given: 用户消息
        val userMessage = "帮我分析一下荔枝的性格"
        
        // When: 生成标题
        val title = generateSessionTitle(userMessage)
        
        // Then: 标题应该是用户消息（不超过20字符）
        assertEquals("帮我分析一下荔枝的性格", title)
    }

    @Test
    fun `TC-004 should truncate long message for title`() = runTest {
        // Given: 长消息
        val longMessage = "这是一条非常非常非常非常非常长的消息内容，超过了20个字符"
        
        // When: 生成标题
        val title = generateSessionTitle(longMessage)
        
        // Then: 标题应该被截断并添加"..."
        assertEquals("这是一条非常非常非常非常非常...", title)
        assertTrue(title.length <= 23) // 20 + "..."
    }

    @Test
    fun `TC-005 should not update title for non-first message`() = runTest {
        // Given: 会话已有消息（messageCount > 0）
        val session = AiAdvisorSession(
            id = "session-1",
            contactId = "contact-1",
            title = "原始标题",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 5,  // 已有5条消息
            isActive = true
        )
        
        // When: 检查是否需要更新标题
        val shouldUpdateTitle = session.messageCount == 0
        
        // Then: 不应该更新标题
        assertFalse(shouldUpdateTitle)
    }

    @Test
    fun `TC-003b should update title for first message`() = runTest {
        // Given: 空会话（messageCount = 0）
        val session = AiAdvisorSession(
            id = "session-1",
            contactId = "contact-1",
            title = "新对话",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 0,  // 空会话
            isActive = true
        )
        
        // When: 检查是否需要更新标题
        val shouldUpdateTitle = session.messageCount == 0
        
        // Then: 应该更新标题
        assertTrue(shouldUpdateTitle)
    }

    @Test
    fun `TC-004b should remove newlines from title`() = runTest {
        // Given: 包含换行符的消息
        val messageWithNewlines = "第一行\n第二行\n第三行"
        
        // When: 生成标题
        val title = generateSessionTitle(messageWithNewlines)
        
        // Then: 换行符应该被替换为空格
        assertFalse(title.contains("\n"))
        assertEquals("第一行 第二行 第三行", title)
    }

    // ==================== 功能3：会话重命名 ====================

    @Test
    fun `TC-006 should rename session successfully`() = runTest {
        // Given: 现有会话
        val sessionId = "session-1"
        val newTitle = "关于工作的讨论"
        
        coEvery { 
            aiAdvisorRepository.updateSessionTitle(sessionId, newTitle) 
        } returns Result.success(Unit)
        
        // When: 重命名会话
        val result = aiAdvisorRepository.updateSessionTitle(sessionId, newTitle)
        
        // Then: 重命名成功
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { aiAdvisorRepository.updateSessionTitle(sessionId, newTitle) }
    }

    @Test
    fun `TC-006b should handle rename failure`() = runTest {
        // Given: 重命名失败
        val sessionId = "session-1"
        val newTitle = "新标题"
        
        coEvery { 
            aiAdvisorRepository.updateSessionTitle(sessionId, newTitle) 
        } returns Result.failure(Exception("更新失败"))
        
        // When: 重命名会话
        val result = aiAdvisorRepository.updateSessionTitle(sessionId, newTitle)
        
        // Then: 应该返回失败
        assertTrue(result.isFailure)
    }

    // ==================== 功能4：会话置顶 ====================

    @Test
    fun `TC-007 pin session should update isPinned to true`() = runTest {
        // Given: 会话置顶状态变更
        val sessionId = "session-1"
        val isPinned = true
        
        coEvery { 
            aiAdvisorRepository.updateSessionPinned(sessionId, isPinned) 
        } returns Result.success(Unit)
        
        // When: 置顶会话
        val result = aiAdvisorRepository.updateSessionPinned(sessionId, isPinned)
        
        // Then: 置顶成功
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { aiAdvisorRepository.updateSessionPinned(sessionId, isPinned) }
    }

    @Test
    fun `TC-008 unpin session should update isPinned to false`() = runTest {
        // Given: 会话取消置顶
        val sessionId = "session-1"
        val isPinned = false
        
        coEvery { 
            aiAdvisorRepository.updateSessionPinned(sessionId, isPinned) 
        } returns Result.success(Unit)
        
        // When: 取消置顶
        val result = aiAdvisorRepository.updateSessionPinned(sessionId, isPinned)
        
        // Then: 取消置顶成功
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { aiAdvisorRepository.updateSessionPinned(sessionId, isPinned) }
    }

    @Test
    fun `TC-007b pinned sessions should appear first in list`() = runTest {
        // Given: 会话列表，包含置顶和非置顶会话
        val pinnedSession = createSession("pinned-1", isPinned = true, updatedAt = 1000)
        val normalSession1 = createSession("normal-1", isPinned = false, updatedAt = 3000)
        val normalSession2 = createSession("normal-2", isPinned = false, updatedAt = 2000)
        
        val sessions = listOf(normalSession1, pinnedSession, normalSession2)
        
        // When: 按置顶和更新时间排序
        val sortedSessions = sessions.sortedWith(
            compareByDescending<AiAdvisorSession> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
        
        // Then: 置顶会话应该在最前面
        assertEquals("pinned-1", sortedSessions[0].id)
        assertTrue(sortedSessions[0].isPinned)
        // 非置顶会话按更新时间排序
        assertEquals("normal-1", sortedSessions[1].id)
        assertEquals("normal-2", sortedSessions[2].id)
    }

    @Test
    fun `TC-009 empty session reuse should call getLatestEmptySession`() = runTest {
        // Given: 存在空会话
        val emptySession = createSession("empty-1", messageCount = 0)
        
        coEvery { 
            aiAdvisorRepository.getLatestEmptySession("contact-1") 
        } returns Result.success(emptySession)
        
        // When: 查询空会话
        val result = aiAdvisorRepository.getLatestEmptySession("contact-1")
        
        // Then: 应该返回空会话
        assertTrue(result.isSuccess)
        assertEquals("empty-1", result.getOrNull()?.id)
        assertEquals(0, result.getOrNull()?.messageCount)
    }

    @Test
    fun `TC-010 no empty session should return null`() = runTest {
        // Given: 不存在空会话
        coEvery { 
            aiAdvisorRepository.getLatestEmptySession("contact-1") 
        } returns Result.success(null)
        
        // When: 查询空会话
        val result = aiAdvisorRepository.getLatestEmptySession("contact-1")
        
        // Then: 应该返回null
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成会话标题
     * 
     * 规则：
     * 1. 取用户消息的前20个字符
     * 2. 如果超过20字符，添加"..."后缀
     * 3. 去除换行符，替换为空格
     * 4. 去除首尾空白
     */
    private fun generateSessionTitle(userMessage: String): String {
        val maxLength = 20
        val cleaned = userMessage.trim().replace("\n", " ")
        return if (cleaned.length > maxLength) {
            cleaned.take(maxLength) + "..."
        } else {
            cleaned
        }
    }

    /**
     * 创建测试用会话
     */
    private fun createSession(
        id: String,
        updatedAt: Long = System.currentTimeMillis(),
        messageCount: Int = 1,
        isPinned: Boolean = false
    ): AiAdvisorSession {
        return AiAdvisorSession(
            id = id,
            contactId = "contact-1",
            title = "会话 $id",
            createdAt = updatedAt - 1000,
            updatedAt = updatedAt,
            messageCount = messageCount,
            isActive = true,
            isPinned = isPinned
        )
    }
}
