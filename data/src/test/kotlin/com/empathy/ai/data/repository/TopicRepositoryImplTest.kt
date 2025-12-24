package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.ConversationTopicDao
import com.empathy.ai.data.local.entity.ConversationTopicEntity
import com.empathy.ai.domain.model.ConversationTopic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TopicRepositoryImpl单元测试
 *
 * 测试主题仓库实现的各种场景
 */
class TopicRepositoryImplTest {

    private lateinit var topicDao: ConversationTopicDao
    private lateinit var topicRepository: TopicRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        topicDao = mockk()
        topicRepository = TopicRepositoryImpl(topicDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `获取活跃主题_存在主题_返回主题`() = runTest {
        // Given
        val contactId = "contact-123"
        val entity = ConversationTopicEntity(
            id = "topic-1",
            contactId = contactId,
            content = "讨论项目进度",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isActive = true
        )
        coEvery { topicDao.getActiveTopic(contactId) } returns entity

        // When
        val result = topicRepository.getActiveTopic(contactId)

        // Then
        assertEquals(entity.id, result?.id)
        assertEquals(entity.content, result?.content)
        assertEquals(entity.contactId, result?.contactId)
    }

    @Test
    fun `获取活跃主题_不存在主题_返回null`() = runTest {
        // Given
        val contactId = "contact-123"
        coEvery { topicDao.getActiveTopic(contactId) } returns null

        // When
        val result = topicRepository.getActiveTopic(contactId)

        // Then
        assertNull(result)
    }

    @Test
    fun `观察活跃主题_返回Flow`() = runTest {
        // Given
        val contactId = "contact-123"
        val entity = ConversationTopicEntity(
            id = "topic-1",
            contactId = contactId,
            content = "讨论项目进度",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isActive = true
        )
        every { topicDao.observeActiveTopic(contactId) } returns flowOf(entity)

        // When
        val result = topicRepository.observeActiveTopic(contactId).first()

        // Then
        assertEquals(entity.id, result?.id)
        assertEquals(entity.content, result?.content)
    }

    @Test
    fun `设置主题_成功_返回成功`() = runTest {
        // Given
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = "讨论项目进度",
            isActive = true
        )
        coEvery { topicDao.deactivateAllTopics(topic.contactId, any()) } returns Unit
        coEvery { topicDao.insert(any()) } returns Unit

        // When
        val result = topicRepository.setTopic(topic)

        // Then
        assertTrue(result.isSuccess)
        coVerify { topicDao.deactivateAllTopics(topic.contactId, any()) }
        coVerify { topicDao.insert(any()) }
    }

    @Test
    fun `设置主题_数据库异常_返回失败`() = runTest {
        // Given
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = "讨论项目进度",
            isActive = true
        )
        coEvery { topicDao.deactivateAllTopics(topic.contactId, any()) } returns Unit
        coEvery { topicDao.insert(any()) } throws RuntimeException("数据库错误")

        // When
        val result = topicRepository.setTopic(topic)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `更新主题内容_成功_返回成功`() = runTest {
        // Given
        val topicId = "topic-1"
        val newContent = "新的主题内容"
        coEvery { topicDao.updateContent(topicId, newContent, any()) } returns Unit

        // When
        val result = topicRepository.updateTopicContent(topicId, newContent)

        // Then
        assertTrue(result.isSuccess)
        coVerify { topicDao.updateContent(topicId, newContent, any()) }
    }

    @Test
    fun `清除主题_成功_返回成功`() = runTest {
        // Given
        val contactId = "contact-123"
        coEvery { topicDao.deactivateAllTopics(contactId, any()) } returns Unit

        // When
        val result = topicRepository.clearTopic(contactId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { topicDao.deactivateAllTopics(contactId, any()) }
    }

    @Test
    fun `获取主题历史_返回列表`() = runTest {
        // Given
        val contactId = "contact-123"
        val entities = listOf(
            ConversationTopicEntity(
                id = "topic-1",
                contactId = contactId,
                content = "主题1",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isActive = false
            ),
            ConversationTopicEntity(
                id = "topic-2",
                contactId = contactId,
                content = "主题2",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isActive = true
            )
        )
        coEvery { topicDao.getTopicHistory(contactId, 10) } returns entities

        // When
        val result = topicRepository.getTopicHistory(contactId, 10)

        // Then
        assertEquals(2, result.size)
        assertEquals("主题1", result[0].content)
        assertEquals("主题2", result[1].content)
    }

    @Test
    fun `删除主题_成功_返回成功`() = runTest {
        // Given
        val topicId = "topic-1"
        coEvery { topicDao.deleteById(topicId) } returns Unit

        // When
        val result = topicRepository.deleteTopic(topicId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { topicDao.deleteById(topicId) }
    }
}
