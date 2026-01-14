package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BatchDeleteFactsUseCase 单元测试
 */
class BatchDeleteFactsUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var useCase: BatchDeleteFactsUseCase

    private val testContactId = "contact_123"

    @Before
    fun setup() {
        contactRepository = mockk()
        useCase = BatchDeleteFactsUseCase(contactRepository)
    }

    private fun createFact(
        id: String = "fact_1",
        key: String = "性格特点",
        value: String = "开朗"
    ) = Fact(
        id = id,
        key = key,
        value = value,
        timestamp = System.currentTimeMillis(),
        source = FactSource.MANUAL
    )

    private fun createProfile(facts: List<Fact> = emptyList()) = ContactProfile(
        id = testContactId,
        name = "测试联系人",
        targetGoal = "测试目标",
        facts = facts
    )

    @Test
    fun `空ID列表返回0`() = runTest {
        val result = useCase(testContactId, emptyList())

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        // 不应该调用Repository
        coVerify(exactly = 0) { contactRepository.getProfile(any()) }
    }

    @Test
    fun `联系人不存在返回错误`() = runTest {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        val result = useCase(testContactId, listOf("fact_1"))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("联系人不存在") == true)
    }

    @Test
    fun `成功删除指定Facts`() = runTest {
        val facts = listOf(
            createFact("fact_1", "性格特点", "开朗"),
            createFact("fact_2", "兴趣爱好", "读书"),
            createFact("fact_3", "工作信息", "程序员")
        )
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val result = useCase(testContactId, listOf("fact_1", "fact_2"))

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        // 验证更新的Facts列表
        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        
        val remainingFacts = factsSlot.captured
        assertEquals(1, remainingFacts.size)
        assertEquals("fact_3", remainingFacts[0].id)
    }

    @Test
    fun `删除所有Facts`() = runTest {
        val facts = listOf(
            createFact("fact_1"),
            createFact("fact_2")
        )
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val result = useCase(testContactId, listOf("fact_1", "fact_2"))

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        assertTrue(factsSlot.captured.isEmpty())
    }

    @Test
    fun `部分ID不存在时正确处理`() = runTest {
        val facts = listOf(
            createFact("fact_1"),
            createFact("fact_2")
        )
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        // 包含不存在的ID
        val result = useCase(testContactId, listOf("fact_1", "fact_999", "fact_888"))

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()) // 只删除了fact_1

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        assertEquals(1, factsSlot.captured.size)
        assertEquals("fact_2", factsSlot.captured[0].id)
    }

    @Test
    fun `所有ID都不存在时返回0`() = runTest {
        val facts = listOf(createFact("fact_1"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)

        val result = useCase(testContactId, listOf("fact_999", "fact_888"))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        // 不应该调用updateFacts
        coVerify(exactly = 0) { contactRepository.updateFacts(any(), any()) }
    }

    @Test
    fun `Repository更新失败时返回错误`() = runTest {
        val facts = listOf(createFact("fact_1"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns 
            Result.failure(RuntimeException("数据库错误"))

        val result = useCase(testContactId, listOf("fact_1"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("数据库错误") == true)
    }

    @Test
    fun `Repository获取联系人失败时返回错误`() = runTest {
        coEvery { contactRepository.getProfile(testContactId) } returns 
            Result.failure(RuntimeException("网络错误"))

        val result = useCase(testContactId, listOf("fact_1"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `重复ID只删除一次`() = runTest {
        val facts = listOf(
            createFact("fact_1"),
            createFact("fact_2")
        )
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        // 包含重复ID
        val result = useCase(testContactId, listOf("fact_1", "fact_1", "fact_1"))

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun `幂等性_多次删除同一ID`() = runTest {
        val facts = listOf(
            createFact("fact_1"),
            createFact("fact_2")
        )
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        // 第一次删除
        val result1 = useCase(testContactId, listOf("fact_1"))
        assertTrue(result1.isSuccess)
        assertEquals(1, result1.getOrNull())

        // 模拟第一次删除后的状态
        val updatedProfile = createProfile(listOf(createFact("fact_2")))
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(updatedProfile)

        // 第二次删除同一ID
        val result2 = useCase(testContactId, listOf("fact_1"))
        assertTrue(result2.isSuccess)
        assertEquals(0, result2.getOrNull()) // 已经不存在，返回0
    }

    @Test
    fun `空Facts列表的联系人`() = runTest {
        val profile = createProfile(emptyList())
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)

        val result = useCase(testContactId, listOf("fact_1"))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }
}
