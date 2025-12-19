package com.empathy.ai.domain.service

import com.empathy.ai.domain.model.ConversationContextConfig
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.util.ConversationContextBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SessionContextService 单元测试
 *
 * 测试会话上下文服务的核心功能：
 * 1. 正常获取历史上下文
 * 2. 历史条数为0时返回空
 * 3. 没有历史记录时返回空
 * 4. 异常处理和降级
 * 5. 自定义历史条数
 * 6. 检查是否有历史对话
 */
class SessionContextServiceTest {

    private lateinit var conversationRepository: ConversationRepository
    private lateinit var conversationContextBuilder: ConversationContextBuilder
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var sessionContextService: SessionContextService

    private val testContactId = "contact-123"
    private val testTimestamp = System.currentTimeMillis()

    @Before
    fun setup() {
        conversationRepository = mockk()
        conversationContextBuilder = ConversationContextBuilder()
        settingsRepository = mockk()

        sessionContextService = SessionContextService(
            conversationRepository = conversationRepository,
            conversationContextBuilder = conversationContextBuilder,
            settingsRepository = settingsRepository
        )
    }

    // ==================== getHistoryContext 测试 ====================

    @Test
    fun `getHistoryContext 正常返回历史上下文`() = runTest {
        // Given: 配置历史条数为5，有3条历史记录
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(testContactId, 5) } returns Result.success(
            listOf(
                createConversationLog(1, "【对方说】：你好"),
                createConversationLog(2, "【我正在回复】：你好啊"),
                createConversationLog(3, "【对方说】：最近怎么样")
            )
        )

        // When
        val result = sessionContextService.getHistoryContext(testContactId)

        // Then
        assertTrue("应该包含历史对话标题", result.contains("【历史对话】"))
        assertTrue("应该包含第一条消息", result.contains("你好"))
        assertTrue("应该包含第二条消息", result.contains("你好啊"))
        assertTrue("应该包含第三条消息", result.contains("最近怎么样"))
    }

    @Test
    fun `getHistoryContext 历史条数为0时返回空字符串`() = runTest {
        // Given: 配置历史条数为0
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(0)

        // When
        val result = sessionContextService.getHistoryContext(testContactId)

        // Then
        assertEquals("历史条数为0时应返回空字符串", "", result)
        // 不应该查询数据库
        coVerify(exactly = 0) { conversationRepository.getRecentConversations(any(), any()) }
    }

    @Test
    fun `getHistoryContext 没有历史记录时返回空字符串`() = runTest {
        // Given: 配置历史条数为5，但没有历史记录
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(testContactId, 5) } returns Result.success(emptyList())

        // When
        val result = sessionContextService.getHistoryContext(testContactId)

        // Then
        assertEquals("没有历史记录时应返回空字符串", "", result)
    }

    @Test
    fun `getHistoryContext 设置读取失败时使用默认值`() = runTest {
        // Given: 设置读取失败
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.failure(Exception("设置读取失败"))
        coEvery {
            conversationRepository.getRecentConversations(
                testContactId,
                ConversationContextConfig.DEFAULT_HISTORY_COUNT
            )
        } returns Result.success(
            listOf(createConversationLog(1, "测试消息"))
        )

        // When
        val result = sessionContextService.getHistoryContext(testContactId)

        // Then: 应该使用默认历史条数
        coVerify {
            conversationRepository.getRecentConversations(
                testContactId,
                ConversationContextConfig.DEFAULT_HISTORY_COUNT
            )
        }
        assertTrue("应该包含历史对话", result.contains("测试消息"))
    }

    @Test
    fun `getHistoryContext 数据库查询失败时返回空字符串`() = runTest {
        // Given: 数据库查询失败
        coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(5)
        coEvery { conversationRepository.getRecentConversations(testContactId, 5) } returns Result.failure(
            Exception("数据库错误")
        )

        // When
        val result = sessionContextService.getHistoryContext(testContactId)

        // Then: 降级返回空字符串
        assertEquals("数据库查询失败时应返回空字符串", "", result)
    }

    @Test
    fun `getHistoryContext 异常时降级返回空字符串`() = runTest {
        // Given: 抛出异常
        coEvery { settingsRepository.getHistoryConversationCount() } throws RuntimeException("未知错误")

        // When
        val result = sessionContextService.getHistoryContext(testContactId)

        // Then: 降级返回空字符串
        assertEquals("异常时应返回空字符串", "", result)
    }

    // ==================== getHistoryContext(contactId, limit) 测试 ====================

    @Test
    fun `getHistoryContext 自定义条数正常工作`() = runTest {
        // Given
        val customLimit = 3
        coEvery { conversationRepository.getRecentConversations(testContactId, customLimit) } returns Result.success(
            listOf(
                createConversationLog(1, "消息1"),
                createConversationLog(2, "消息2")
            )
        )

        // When
        val result = sessionContextService.getHistoryContext(testContactId, customLimit)

        // Then
        assertTrue("应该包含历史对话", result.contains("消息1"))
        assertTrue("应该包含历史对话", result.contains("消息2"))
        coVerify { conversationRepository.getRecentConversations(testContactId, customLimit) }
    }

    @Test
    fun `getHistoryContext 自定义条数为0时返回空`() = runTest {
        // When
        val result = sessionContextService.getHistoryContext(testContactId, 0)

        // Then
        assertEquals("", result)
        coVerify(exactly = 0) { conversationRepository.getRecentConversations(any(), any()) }
    }

    @Test
    fun `getHistoryContext 自定义条数为负数时返回空`() = runTest {
        // When
        val result = sessionContextService.getHistoryContext(testContactId, -1)

        // Then
        assertEquals("", result)
        coVerify(exactly = 0) { conversationRepository.getRecentConversations(any(), any()) }
    }

    // ==================== hasHistoryContext 测试 ====================

    @Test
    fun `hasHistoryContext 有历史记录时返回true`() = runTest {
        // Given
        coEvery { conversationRepository.getRecentConversations(testContactId, 1) } returns Result.success(
            listOf(createConversationLog(1, "消息"))
        )

        // When
        val result = sessionContextService.hasHistoryContext(testContactId)

        // Then
        assertTrue("有历史记录时应返回true", result)
    }

    @Test
    fun `hasHistoryContext 没有历史记录时返回false`() = runTest {
        // Given
        coEvery { conversationRepository.getRecentConversations(testContactId, 1) } returns Result.success(emptyList())

        // When
        val result = sessionContextService.hasHistoryContext(testContactId)

        // Then
        assertFalse("没有历史记录时应返回false", result)
    }

    @Test
    fun `hasHistoryContext 查询失败时返回false`() = runTest {
        // Given
        coEvery { conversationRepository.getRecentConversations(testContactId, 1) } returns Result.failure(
            Exception("查询失败")
        )

        // When
        val result = sessionContextService.hasHistoryContext(testContactId)

        // Then
        assertFalse("查询失败时应返回false", result)
    }

    // ==================== 辅助方法 ====================

    private fun createConversationLog(id: Long, userInput: String): ConversationLog {
        return ConversationLog(
            id = id,
            contactId = testContactId,
            userInput = userInput,
            aiResponse = null,
            timestamp = testTimestamp + id * 1000
        )
    }
}
