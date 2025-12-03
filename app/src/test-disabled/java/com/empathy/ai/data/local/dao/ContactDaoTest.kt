package com.empathy.ai.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.data.local.AppDatabase
import com.empathy.ai.data.local.entity.ContactProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ContactDao 测试
 *
 * 使用 Room 的 in-memory 数据库进行测试。
 * 每个测试独立运行，测试完成后数据库自动销毁。
 *
 * 测试场景:
 * 1. 插入联系人
 * 2. 查询所有联系人
 * 3. 根据ID查询联系人
 * 4. 更新联系人
 * 5. 删除联系人
 */
@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var contactDao: ContactDao

    @Before
    fun setup() {
        // 创建 in-memory 数据库用于测试
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries() // 测试允许在主线程执行
            .build()

        contactDao = database.contactDao()
    }

    @After
    fun tearDown() {
        // 关闭数据库
        database.close()
    }

    @Test
    fun `insertOrUpdate - 插入新联系人应该成功`() = runBlocking {
        // Given
        val profile = ContactProfileEntity(
            id = "test-123",
            name = "Test User",
            targetGoal = "Test Goal",
            contextDepth = 10,
            factsJson = "{\"hobby\":\"reading\"}"
        )

        // When
        contactDao.insertOrUpdate(profile)

        // Then
        val result = contactDao.getProfileById("test-123")
        assertNotNull(result)
        assertEquals("test-123", result?.id)
        assertEquals("Test User", result?.name)
        assertEquals("Test Goal", result?.targetGoal)
    }

    @Test
    fun `getAllProfiles - 查询所有联系人应该返回正确列表`() = runBlocking {
        // Given
        val profile1 = ContactProfileEntity(
            id = "test-1",
            name = "User 1",
            targetGoal = "Goal 1",
            factsJson = "{}"
        )
        val profile2 = ContactProfileEntity(
            id = "test-2",
            name = "User 2",
            targetGoal = "Goal 2",
            factsJson = "{}"
        )
        contactDao.insertOrUpdate(profile1)
        contactDao.insertOrUpdate(profile2)

        // When
        val result = contactDao.getAllProfiles().first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "test-1" })
        assertTrue(result.any { it.id == "test-2" })
    }

    @Test
    fun `getProfileById - 根据ID查询联系人应该返回正确对象`() = runBlocking {
        // Given
        val profile = ContactProfileEntity(
            id = "test-unique",
            name = "Unique User",
            targetGoal = "Unique Goal",
            factsJson = "{}"
        )
        contactDao.insertOrUpdate(profile)

        // When
        val result = contactDao.getProfileById("test-unique")

        // Then
        assertNotNull(result)
        assertEquals("test-unique", result?.id)
        assertEquals("Unique User", result?.name)
    }

    @Test
    fun `getProfileById - 查询不存在的ID应该返回null`() = runBlocking {
        // When
        val result = contactDao.getProfileById("non-existent")

        // Then
        assertNull(result)
    }

    @Test
    fun `insertOrUpdate - 更新已存在的联系人应该覆盖旧数据`() = runBlocking {
        // Given
        val originalProfile = ContactProfileEntity(
            id = "test-update",
            name = "Original Name",
            targetGoal = "Original Goal",
            factsJson = "{}"
        )
        contactDao.insertOrUpdate(originalProfile)

        // When - 更新同ID的数据
        val updatedProfile = ContactProfileEntity(
            id = "test-update",
            name = "Updated Name",
            targetGoal = "Updated Goal",
            factsJson = "{\"new\":\"fact\"}"
        )
        contactDao.insertOrUpdate(updatedProfile)

        // Then
        val result = contactDao.getProfileById("test-update")
        assertNotNull(result)
        assertEquals("Updated Name", result?.name) // 数据已更新
        assertEquals("Updated Goal", result?.targetGoal)
        assertEquals("{\"new\":\"fact\"}", result?.factsJson)
    }

    @Test
    fun `deleteById - 删除联系人应该成功`() = runBlocking {
        // Given
        val profile = ContactProfileEntity(
            id = "test-delete",
            name = "Delete Me",
            targetGoal = "Delete Goal",
            factsJson = "{}"
        )
        contactDao.insertOrUpdate(profile)
        assertNotNull(contactDao.getProfileById("test-delete"))

        // When
        contactDao.deleteById("test-delete")

        // Then
        val result = contactDao.getProfileById("test-delete")
        assertNull(result)
    }

    @Test
    fun `Flow - 观察联系人列表变化应该自动推送更新`() = runBlocking {
        // Given - 初始数据
        val profile1 = ContactProfileEntity(
            id = "flow-1",
            name = "Flow User 1",
            targetGoal = "Goal",
            factsJson = "{}"
        )
        contactDao.insertOrUpdate(profile1)

        // When - 收集第一个值
        val flow = contactDao.getAllProfiles()
        val firstEmit = flow.first()
        assertEquals(1, firstEmit.size)

        // When - 插入新数据
        val profile2 = ContactProfileEntity(
            id = "flow-2",
            name = "Flow User 2",
            targetGoal = "Goal",
            factsJson = "{}"
        )
        contactDao.insertOrUpdate(profile2)

        // Then - Flow 应该自动推送新值
        val secondEmit = flow.first()
        assertEquals(2, secondEmit.size)
    }
}
