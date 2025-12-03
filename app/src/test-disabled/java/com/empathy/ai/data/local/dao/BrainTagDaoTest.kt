package com.empathy.ai.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.data.local.AppDatabase
import com.empathy.ai.data.local.entity.BrainTagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BrainTagDao 测试
 *
 * 使用 Room 的 in-memory 数据库进行测试。
 *
 * 测试场景:
 * 1. 插入标签
 * 2. 查询某人的所有标签
 * 3. 查询所有雷区标签
 * 4. 删除标签
 * 5. Flow 响应式查询
 */
@RunWith(AndroidJUnit4::class)
class BrainTagDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var brainTagDao: BrainTagDao
    private lateinit var contactDao: ContactDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        brainTagDao = database.brainTagDao()
        contactDao = database.contactDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insertTag - 插入新标签应该成功并返回ID`() = runBlocking {
        // Given
        val tag = BrainTagEntity(
            contactId = "user-123",
            content = "不喜欢吃香菜",
            type = "RISK_RED",
            source = "MANUAL"
        )

        // When
        val id = brainTagDao.insertTag(tag)

        // Then
        assertTrue(id > 0)
    }

    @Test
    fun `getTagsByContactId - 查询某人的所有标签应该返回正确列表`() = runBlocking {
        // Given - 为同一联系人插入多个标签
        val tag1 = BrainTagEntity(contactId = "user-1", content = "Tag 1", type = "RISK_RED")
        val tag2 = BrainTagEntity(contactId = "user-1", content = "Tag 2", type = "STRATEGY_GREEN")
        val tag3 = BrainTagEntity(contactId = "user-2", content = "Tag 3", type = "RISK_RED") // 其他人的标签
        brainTagDao.insertTag(tag1)
        brainTagDao.insertTag(tag2)
        brainTagDao.insertTag(tag3)

        // When
        val result = brainTagDao.getTagsByContactId("user-1").first()

        // Then - 只返回 user-1 的标签
        assertEquals(2, result.size)
        assertTrue(result.all { it.contactId == "user-1" })
    }

    @Test
    fun `getAllRedFlags - 查询所有雷区标签应该只返回RISK_RED类型`() = runBlocking {
        // Given
        val redTag1 = BrainTagEntity(contactId = "user-1", content = "Red 1", type = "RISK_RED")
        val redTag2 = BrainTagEntity(contactId = "user-2", content = "Red 2", type = "RISK_RED")
        val greenTag = BrainTagEntity(contactId = "user-1", content = "Green 1", type = "STRATEGY_GREEN")
        brainTagDao.insertTag(redTag1)
        brainTagDao.insertTag(redTag2)
        brainTagDao.insertTag(greenTag)

        // When
        val result = brainTagDao.getAllRedFlags()

        // Then - 只返回 RISK_RED 类型的标签
        assertEquals(2, result.size)
        assertTrue(result.all { it.type == "RISK_RED" })
        assertTrue(result.none { it.type == "STRATEGY_GREEN" })
    }

    @Test
    fun `deleteTag - 删除标签应该成功`() = runBlocking {
        // Given
        val tag = BrainTagEntity(contactId = "user-1", content = "Delete me", type = "RISK_RED")
        val id = brainTagDao.insertTag(tag)
        assertNotNull(id)

        val tagsBeforeDelete = brainTagDao.getTagsByContactId("user-1").first()
        assertEquals(1, tagsBeforeDelete.size)

        // When
        brainTagDao.deleteTag(id)

        // Then
        val tagsAfterDelete = brainTagDao.getTagsByContactId("user-1").first()
        assertEquals(0, tagsAfterDelete.size)
    }

    @Test
    fun `deleteTagsByContactId - 删除某人的所有标签应该成功`() = runBlocking {
        // Given
        brainTagDao.insertTag(BrainTagEntity(contactId = "user-delete", content = "Tag 1", type = "RISK_RED"))
        brainTagDao.insertTag(BrainTagEntity(contactId = "user-delete", content = "Tag 2", type = "STRATEGY_GREEN"))
        brainTagDao.insertTag(BrainTagEntity(contactId = "user-keep", content = "Tag 3", type = "RISK_RED"))

        // When
        brainTagDao.deleteTagsByContactId("user-delete")

        // Then
        val deletedUserTags = brainTagDao.getTagsByContactId("user-delete").first()
        assertEquals(0, deletedUserTags.size)

        val keptUserTags = brainTagDao.getTagsByContactId("user-keep").first()
        assertEquals(1, keptUserTags.size)
    }

    @Test
    fun `Flow - 观察标签变化应该自动推送更新`() = runBlocking {
        // Given
        val flow = brainTagDao.getTagsByContactId("flow-user")
        val initialEmit = flow.first()
        assertEquals(0, initialEmit.size)

        // When - 插入第一条数据
        brainTagDao.insertTag(BrainTagEntity(contactId = "flow-user", content = "Tag 1", type = "RISK_RED"))
        val firstEmit = flow.first()

        // Then
        assertEquals(1, firstEmit.size)

        // When - 插入第二条数据
        brainTagDao.insertTag(BrainTagEntity(contactId = "flow-user", content = "Tag 2", type = "STRATEGY_GREEN"))
        val secondEmit = flow.first()

        // Then - Flow 应该自动推送新值
        assertEquals(2, secondEmit.size)
    }

    @Test
    fun `type字段 - 支持RISK_RED类型`() = runBlocking {
        // When
        val id = brainTagDao.insertTag(
            BrainTagEntity(contactId = "user-1", content = "红色标签", type = "RISK_RED")
        )

        // Then
        val tag = brainTagDao.getTagsByContactId("user-1").first().first()
        assertEquals("RISK_RED", tag.type)
    }

    @Test
    fun `type字段 - 支持STRATEGY_GREEN类型`() = runBlocking {
        // When
        val id = brainTagDao.insertTag(
            BrainTagEntity(contactId = "user-1", content = "绿色标签", type = "STRATEGY_GREEN")
        )

        // Then
        val tag = brainTagDao.getTagsByContactId("user-1").first().first()
        assertEquals("STRATEGY_GREEN", tag.type)
    }

    @Test
    fun `source字段 - 默认为MANUAL`() = runBlocking {
        // When - 不指定source
        brainTagDao.insertTag(BrainTagEntity(contactId = "user-1", content = "标签", type = "RISK_RED"))

        // Then
        val tag = brainTagDao.getTagsByContactId("user-1").first().first()
        assertEquals("MANUAL", tag.source)
    }

    @Test
    fun `source字段 - 支持AI_INFERRED`() = runBlocking {
        // When
        brainTagDao.insertTag(
            BrainTagEntity(contactId = "user-1", content = "AI标签", type = "RISK_RED", source = "AI_INFERRED")
        )

        // Then
        val tag = brainTagDao.getTagsByContactId("user-1").first().first()
        assertEquals("AI_INFERRED", tag.source)
    }
}
