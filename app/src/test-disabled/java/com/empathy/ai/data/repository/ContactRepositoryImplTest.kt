package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.entity.ContactProfileEntity
import com.empathy.ai.domain.model.ContactProfile
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ContactRepositoryImpl 测试
 *
 * 使用 MockK 模拟 DAO 层,测试 Repository 的业务逻辑和映射函数。
 *
 * 测试场景:
 * 1. Entity → Domain Model 转换
 * 2. Domain Model → Entity 转换
 * 3. Flow 映射
 * 4. saveProfile 操作
 * 5. updateContactFacts 增量更新
 * 6. 错误处理
 */
class ContactRepositoryImplTest {

    private lateinit var mockDao: ContactDao
    private lateinit var repository: ContactRepositoryImpl

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        repository = ContactRepositoryImpl(mockDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `toDomain - 空factsJson应该返回空Map`() = runBlocking {
        // Given
        val entity = ContactProfileEntity(
            id = "test-1",
            name = "Test",
            targetGoal = "Goal",
            factsJson = "{}"
        )
        every { mockDao.getProfileById("test-1") } returns entity

        // When
        val result = repository.getProfile("test-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(emptyMap<String, String>(), result.getOrNull()?.facts)
    }

    @Test
    fun `toDomain - 有效的factsJson应该正确反序列化`() = runBlocking {
        // Given
        val factsJson = "{\"phone\":\"13812345678\",\"address\":\"Beijing\"}"
        val entity = ContactProfileEntity(
            id = "test-1",
            name = "Test",
            targetGoal = "Goal",
            factsJson = factsJson
        )
        every { mockDao.getProfileById("test-1") } returns entity

        // When
        val result = repository.getProfile("test-1")

        // Then
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals("13812345678", profile!!.facts["phone"])
        assertEquals("Beijing", profile.facts["address"])
    }

    @Test
    fun `toDomain - 损坏的factsJson应该返回空Map`() = runBlocking {
        // Given
        val entity = ContactProfileEntity(
            id = "test-1",
            name = "Test",
            targetGoal = "Goal",
            factsJson = "{invalid json}"
        )
        every { mockDao.getProfileById("test-1") } returns entity

        // When
        val result = repository.getProfile("test-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(emptyMap<String, String>(), result.getOrNull()?.facts)
    }

    @Test
    fun `toEntity - Domain对象应该正确序列化为Entity`() = runBlocking {
        // Given
        val profile = ContactProfile(
            id = "test-1",
            name = "Test User",
            targetGoal = "Test Goal",
            contextDepth = 15,
            facts = mapOf("hobby" to "reading", "food" to "pizza")
        )
        every { mockDao.insertOrUpdate(any()) } just Runs

        // When
        val result = repository.saveProfile(profile)

        // Then
        assertTrue(result.isSuccess)
        verify { mockDao.insertOrUpdate(any()) }
    }

    @Test
    fun `getAllProfiles - Flow应该正确映射Entity列表到Domain列表`() = runBlocking {
        // Given
        val entities = listOf(
            ContactProfileEntity(
                id = "test-1",
                name = "User 1",
                targetGoal = "Goal 1",
                factsJson = "{\"hobby\":\"reading\"}"
            ),
            ContactProfileEntity(
                id = "test-2",
                name = "User 2",
                targetGoal = "Goal 2",
                factsJson = "{}"
            )
        )
        every { mockDao.getAllProfiles() } returns flowOf(entities)

        // When
        val flow = repository.getAllProfiles()
        val result = flow.toList().flatten()

        // Then
        assertEquals(2, result.size)
        assertEquals("test-1", result[0].id)
        assertEquals("User 1", result[0].name)
        assertEquals("reading", result[0].facts["hobby"])
        assertEquals("test-2", result[1].id)
        assertTrue(result[1].facts.isEmpty())
    }

    @Test
    fun `saveProfile - 保存联系人应该调用DAO的insertOrUpdate`() = runBlocking {
        // Given
        val profile = ContactProfile(
            id = "test-save",
            name = "Save Me",
            targetGoal = "Save Goal",
            facts = emptyMap()
        )
        every { mockDao.insertOrUpdate(any()) } just Runs

        // When
        val result = repository.saveProfile(profile)

        // Then
        assertTrue(result.isSuccess)
        verify(exactly = 1) { mockDao.insertOrUpdate(any()) }
    }

    @Test
    fun `saveProfile - DAO抛出异常应该返回Failure`() = runBlocking {
        // Given
        val profile = ContactProfile(
            id = "test-error",
            name = "Error",
            targetGoal = "Error",
            facts = emptyMap()
        )
        every { mockDao.insertOrUpdate(any()) } throws Exception("Database error")

        // When
        val result = repository.saveProfile(profile)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }

    @Test
    fun `updateContactFacts - 应该正确合并新的facts`() = runBlocking {
        // Given - 模拟已存在的联系人
        val existingFactsJson = "{\"phone\":\"13812345678\",\"address\":\"Beijing\"}"
        val existingEntity = ContactProfileEntity(
            id = "test-update",
            name = "Test",
            targetGoal = "Goal",
            factsJson = existingFactsJson
        )
        every { mockDao.getProfileById("test-update") } returns existingEntity
        every { mockDao.insertOrUpdate(any()) } just Runs

        // When - 更新facts,添加新字段并修改旧字段
        val newFacts = mapOf("email" to "test@example.com", "phone" to "13999999999")
        val result = repository.updateContactFacts("test-update", newFacts)

        // Then
        assertTrue(result.isSuccess)
        verify { mockDao.insertOrUpdate(any()) }
    }

    @Test
    fun `updateContactFacts - 不存在的ID应该返回Failure`() = runBlocking {
        // Given
        every { mockDao.getProfileById("non-existent") } returns null

        // When
        val result = repository.updateContactFacts("non-existent", emptyMap())

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Contact not found") == true)
    }

    @Test
    fun `deleteProfile - 删除联系人应该调用DAO的deleteById`() = runBlocking {
        // Given
        every { mockDao.deleteById("test-delete") } just Runs

        // When
        val result = repository.deleteProfile("test-delete")

        // Then
        assertTrue(result.isSuccess)
        verify(exactly = 1) { mockDao.deleteById("test-delete") }
    }

    @Test
    fun `deleteProfile - DAO抛出异常应该返回Failure`() = runBlocking {
        // Given
        every { mockDao.deleteById("test-error") } throws Exception("Delete failed")

        // When
        val result = repository.deleteProfile("test-error")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }
}
