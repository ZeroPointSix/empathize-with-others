package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SaveProfileUseCase 单元测试
 *
 * 测试场景:
 * 1. 成功保存有效的联系人画像
 * 2. 失败场景：id为空
 * 3. 失败场景：name为空
 * 4. 边界场景：profile包含空字段但必填字段有效
 * 5. 失败场景：repository保存失败
 */
class SaveProfileUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var useCase: SaveProfileUseCase

    @Before
    fun setup() {
        contactRepository = mockk()
        useCase = SaveProfileUseCase(contactRepository)
    }

    @Test
    fun `should successfully save valid contact profile`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "contact_123",
            name = "张三",
            targetGoal = "建立长期信任",
            contextDepth = 15,
            facts = mapOf("爱好" to "钓鱼", "职业" to "工程师")
        )
        coEvery { contactRepository.saveProfile(profile) } returns Result.success(Unit)

        // When
        val result = useCase(profile)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should fail when id is blank`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "",
            name = "张三",
            targetGoal = "建立长期信任"
        )

        // When
        val result = useCase(profile)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should fail when name is blank`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "contact_123",
            name = "",
            targetGoal = "建立长期信任"
        )

        // When
        val result = useCase(profile)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("联系人名称不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should succeed when profile has empty optional fields`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "contact_123",
            name = "张三",
            targetGoal = "", // 空的目标
            contextDepth = 10, // 默认值
            facts = emptyMap() // 空的事实
        )
        coEvery { contactRepository.saveProfile(profile) } returns Result.success(Unit)

        // When
        val result = useCase(profile)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should fail when repository save fails`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "contact_123",
            name = "张三",
            targetGoal = "建立长期信任"
        )
        val exception = RuntimeException("数据库错误")
        coEvery { contactRepository.saveProfile(profile) } returns Result.failure(exception)

        // When
        val result = useCase(profile)

        // Then
        assertFalse(result.isSuccess)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `should handle repository exception`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "contact_123",
            name = "张三",
            targetGoal = "建立长期信任"
        )
        val exception = RuntimeException("数据库连接失败")
        coEvery { contactRepository.saveProfile(profile) } throws exception

        // When
        val result = useCase(profile)

        // Then
        assertFalse(result.isSuccess)
        assertEquals(exception, result.exceptionOrNull())
    }
}