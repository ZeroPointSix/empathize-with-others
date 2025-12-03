package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.BrainTagDao
import com.empathy.ai.data.local.entity.BrainTagEntity
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BrainTagRepositoryImpl 测试
 *
 * 使用 MockK 测试标签的 Repository 逻辑。
 *
 * 测试场景:
 * 1. Entity → Domain Model 转换
 * 2. Domain Model → Entity 转换
 * 3. Flow 映射
 * 4. 获取雷区标签
 * 5. 保存标签
 * 6. 删除标签
 * 7. 错误处理
 */
class BrainTagRepositoryImplTest {

    private lateinit var mockDao: BrainTagDao
    private lateinit var repository: BrainTagRepositoryImpl

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        repository = BrainTagRepositoryImpl(mockDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `toDomain - Entity应该正确转换为Domain对象`() = runBlocking {
        // Given
        val entity = BrainTagEntity(
            id = 1L,
            contactId = "user-123",
            content = "不喜欢吃香菜",
            type = "RISK_RED",
            source = "MANUAL"
        )
        val entities = listOf(entity)
        every { mockDao.getTagsByContactId("user-123") } returns flowOf(entities)

        // When
        val flow = repository.getTagsForContact("user-123")
        val result = flow.toList().flatten()

        // Then
        assertEquals(1, result.size)
        val tag = result[0]
        assertEquals(1L, tag.id)
        assertEquals("user-123", tag.contactId)
        assertEquals("不喜欢吃香菜", tag.content)
        assertEquals(TagType.RISK_RED, tag.type)
        assertEquals("MANUAL", tag.source)
    }

    @Test
    fun `toEntity - Domain对象应该正确转换为Entity`() = runBlocking {
        // Given
        val tag = BrainTag(
            id = 1L,
            contactId = "user-123",
            content = "策略标签",
            type = TagType.STRATEGY_GREEN,
            source = "AI_INFERRED"
        )
        every { mockDao.insertTag(any()) } returns 1L

        // When
        val result = repository.saveTag(tag)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        verify { mockDao.insertTag(any()) }
    }

    @Test
    fun `getTagsForContact - Flow应该正确映射Entity列表`() = runBlocking {
        // Given
        val entities = listOf(
            BrainTagEntity(id = 1L, contactId = "user-1", content = "Tag 1", type = "RISK_RED"),
            BrainTagEntity(id = 2L, contactId = "user-1", content = "Tag 2", type = "STRATEGY_GREEN")
        )
        every { mockDao.getTagsByContactId("user-1") } returns flowOf(entities)

        // When
        val flow = repository.getTagsForContact("user-1")
        val result = flow.toList().flatten()

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(TagType.RISK_RED, result[0].type)
        assertEquals(2L, result[1].id)
        assertEquals(TagType.STRATEGY_GREEN, result[1].type)
    }

    @Test
    fun `getAllRedFlags - 应该只返回RISK_RED类型的标签`() = runBlocking {
        // Given
        val redEntities = listOf(
            BrainTagEntity(id = 1L, contactId = "user-1", content = "Red 1", type = "RISK_RED"),
            BrainTagEntity(id = 2L, contactId = "user-2", content = "Red 2", type = "RISK_RED")
        )
        every { mockDao.getAllRedFlags() } returns redEntities

        // When
        val result = repository.getAllRedFlags()

        // Then
        assertTrue(result.isSuccess)
        val tags = result.getOrNull()
        assertNotNull(tags)
        assertEquals(2, tags!!.size)
        assertTrue(tags.all { it.type == TagType.RISK_RED })
    }

    @Test
    fun `getAllRedFlags - DAO抛出异常应该返回Failure`() = runBlocking {
        // Given
        every { mockDao.getAllRedFlags() } throws Exception("Database error")

        // When
        val result = repository.getAllRedFlags()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }

    @Test
    fun `saveTag - 保存标签应该调用DAO的insertTag`() = runBlocking {
        // Given
        val tag = BrainTag(
            contactId = "user-123",
            content = "新标签",
            type = TagType.RISK_RED
        )
        every { mockDao.insertTag(any()) } returns 100L

        // When
        val result = repository.saveTag(tag)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(100L, result.getOrNull())
        verify(exactly = 1) { mockDao.insertTag(any()) }
    }

    @Test
    fun `saveTag - DAO抛出异常应该返回Failure`() = runBlocking {
        // Given
        val tag = BrainTag(
            contactId = "user-error",
            content = "Error Tag",
            type = TagType.STRATEGY_GREEN
        )
        every { mockDao.insertTag(any()) } throws Exception("Insert failed")

        // When
        val result = repository.saveTag(tag)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }

    @Test
    fun `deleteTag - 删除标签应该调用DAO的deleteTag`() = runBlocking {
        // Given
        every { mockDao.deleteTag(1L) } just Runs

        // When
        val result = repository.deleteTag(1L)

        // Then
        assertTrue(result.isSuccess)
        verify(exactly = 1) { mockDao.deleteTag(1L) }
    }

    @Test
    fun `deleteTag - DAO抛出异常应该返回Failure`() = runBlocking {
        // Given
        every { mockDao.deleteTag(1L) } throws Exception("Delete failed")

        // When
        val result = repository.deleteTag(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }

    @Test
    fun `type转换 - RISK_RED字符串应该转换为RISK_RED枚举`() = runBlocking {
        // Given
        val entity = BrainTagEntity(
            id = 1L,
            contactId = "user-1",
            content = "红色",
            type = "RISK_RED"
        )
        every { mockDao.getTagsByContactId("user-1") } returns flowOf(listOf(entity))

        // When
        val result = repository.getTagsForContact("user-1").toList().flatten()

        // Then
        assertEquals(TagType.RISK_RED, result[0].type)
    }

    @Test
    fun `type转换 - STRATEGY_GREEN字符串应该转换为STRATEGY_GREEN枚举`() = runBlocking {
        // Given
        val entity = BrainTagEntity(
            id = 1L,
            contactId = "user-1",
            content = "绿色",
            type = "STRATEGY_GREEN"
        )
        every { mockDao.getTagsByContactId("user-1") } returns flowOf(listOf(entity))

        // When
        val result = repository.getTagsForContact("user-1").toList().flatten()

        // Then
        assertEquals(TagType.STRATEGY_GREEN, result[0].type)
    }

    @Test
    fun `source字段 - 支持MANUAL来源`() = runBlocking {
        // Given
        val tag = BrainTag(
            contactId = "user-1",
            content = "手动标签",
            type = TagType.RISK_RED,
            source = "MANUAL"
        )
        every { mockDao.insertTag(any()) } returns 1L

        // When
        repository.saveTag(tag)

        // Then
        verify { mockDao.insertTag(match { it.source == "MANUAL" }) }
    }

    @Test
    fun `source字段 - 支持AI_INFERRED来源`() = runBlocking {
        // Given
        val tag = BrainTag(
            contactId = "user-1",
            content = "AI标签",
            type = TagType.RISK_RED,
            source = "AI_INFERRED"
        )
        every { mockDao.insertTag(any()) } returns 1L

        // When
        repository.saveTag(tag)

        // Then
        verify { mockDao.insertTag(match { it.source == "AI_INFERRED" }) }
    }
}
