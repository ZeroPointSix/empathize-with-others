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
 * BatchMoveFactsUseCase 单元测试
 */
class BatchMoveFactsUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var useCase: BatchMoveFactsUseCase

    private val testContactId = "contact_123"

    @Before
    fun setup() {
        contactRepository = mockk()
        useCase = BatchMoveFactsUseCase(contactRepository)
    }

    private fun createFact(
        id: String = "fact_1",
        key: String = "性格特点",
        value: String = "开朗",
        isUserModified: Boolean = false,
        originalKey: String? = null
    ) = Fact(
        id = id,
        key = key,
        value = value,
        timestamp = System.currentTimeMillis(),
        source = FactSource.MANUAL,
        isUserModified = isUserModified,
        originalKey = originalKey
    )

    private fun createProfile(facts: List<Fact> = emptyList()) = ContactProfile(
        id = testContactId,
        name = "测试联系人",
        targetGoal = "测试目标",
        facts = facts
    )

    @Test
    fun `空ID列表返回0`() = runTest {
        val result = useCase(testContactId, emptyList(), "新分类")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        coVerify(exactly = 0) { contactRepository.getProfile(any()) }
    }

    @Test
    fun `目标分类为空返回错误`() = runTest {
        val result = useCase(testContactId, listOf("fact_1"), "")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("不能为空") == true)
    }

    @Test
    fun `目标分类为空白返回错误`() = runTest {
        val result = useCase(testContactId, listOf("fact_1"), "   ")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("不能为空") == true)
    }

    @Test
    fun `目标分类过长返回错误`() = runTest {
        val longCategory = "a".repeat(21)
        
        val result = useCase(testContactId, listOf("fact_1"), longCategory)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("不能超过") == true)
    }

    @Test
    fun `目标分类刚好20字符成功`() = runTest {
        val category20 = "a".repeat(20)
        val facts = listOf(createFact("fact_1", "原分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val result = useCase(testContactId, listOf("fact_1"), category20)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun `联系人不存在返回错误`() = runTest {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        val result = useCase(testContactId, listOf("fact_1"), "新分类")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("联系人不存在") == true)
    }

    @Test
    fun `成功移动指定Facts`() = runTest {
        val facts = listOf(
            createFact("fact_1", "性格特点", "开朗"),
            createFact("fact_2", "兴趣爱好", "读书"),
            createFact("fact_3", "工作信息", "程序员")
        )
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val result = useCase(testContactId, listOf("fact_1", "fact_2"), "新分类")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        
        val updatedFacts = factsSlot.captured
        assertEquals(3, updatedFacts.size)
        
        val movedFact1 = updatedFacts.find { it.id == "fact_1" }
        assertNotNull(movedFact1)
        assertEquals("新分类", movedFact1!!.key)
        assertTrue(movedFact1.isUserModified)
        assertEquals("性格特点", movedFact1.originalKey)
        
        val movedFact2 = updatedFacts.find { it.id == "fact_2" }
        assertNotNull(movedFact2)
        assertEquals("新分类", movedFact2!!.key)
        assertTrue(movedFact2.isUserModified)
        assertEquals("兴趣爱好", movedFact2.originalKey)
        
        val unchangedFact = updatedFacts.find { it.id == "fact_3" }
        assertNotNull(unchangedFact)
        assertEquals("工作信息", unchangedFact!!.key)
    }

    @Test
    fun `验证key字段更新`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        useCase(testContactId, listOf("fact_1"), "新分类")

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        
        assertEquals("新分类", factsSlot.captured[0].key)
    }

    @Test
    fun `验证isUserModified设置为true`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类", isUserModified = false))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        useCase(testContactId, listOf("fact_1"), "新分类")

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        
        assertTrue(factsSlot.captured[0].isUserModified)
    }

    @Test
    fun `验证lastModifiedTime更新`() = runTest {
        val oldTime = System.currentTimeMillis() - 10000
        val facts = listOf(
            Fact(
                id = "fact_1",
                key = "原分类",
                value = "值",
                timestamp = oldTime,
                source = FactSource.MANUAL,
                lastModifiedTime = oldTime
            )
        )
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val beforeMove = System.currentTimeMillis()
        useCase(testContactId, listOf("fact_1"), "新分类")
        val afterMove = System.currentTimeMillis()

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        
        val modifiedTime = factsSlot.captured[0].lastModifiedTime
        assertTrue(modifiedTime >= beforeMove)
        assertTrue(modifiedTime <= afterMove)
    }

    @Test
    fun `首次编辑保存originalKey`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类", originalKey = null))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        useCase(testContactId, listOf("fact_1"), "新分类")

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        
        assertEquals("原分类", factsSlot.captured[0].originalKey)
    }

    @Test
    fun `再次编辑保持originalKey不变`() = runTest {
        val facts = listOf(createFact("fact_1", "当前分类", originalKey = "最初分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        useCase(testContactId, listOf("fact_1"), "新分类")

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        
        // originalKey应该保持为"最初分类"，而不是"当前分类"
        assertEquals("最初分类", factsSlot.captured[0].originalKey)
    }

    @Test
    fun `移动到相同分类不计数`() = runTest {
        val facts = listOf(createFact("fact_1", "同一分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)

        val result = useCase(testContactId, listOf("fact_1"), "同一分类")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        // 不应该调用updateFacts
        coVerify(exactly = 0) { contactRepository.updateFacts(any(), any()) }
    }

    @Test
    fun `部分ID不存在时正确处理`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val result = useCase(testContactId, listOf("fact_1", "fact_999"), "新分类")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun `所有ID都不存在时返回0`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)

        val result = useCase(testContactId, listOf("fact_999", "fact_888"), "新分类")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun `Repository更新失败时返回错误`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns 
            Result.failure(RuntimeException("数据库错误"))

        val result = useCase(testContactId, listOf("fact_1"), "新分类")

        assertTrue(result.isFailure)
    }

    @Test
    fun `中文分类名称正确处理`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val result = useCase(testContactId, listOf("fact_1"), "新的中文分类名称")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        val factsSlot = slot<List<Fact>>()
        coVerify { contactRepository.updateFacts(testContactId, capture(factsSlot)) }
        assertEquals("新的中文分类名称", factsSlot.captured[0].key)
    }

    @Test
    fun `特殊字符分类名称正确处理`() = runTest {
        val facts = listOf(createFact("fact_1", "原分类"))
        val profile = createProfile(facts)
        
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(profile)
        coEvery { contactRepository.updateFacts(testContactId, any()) } returns Result.success(Unit)

        val result = useCase(testContactId, listOf("fact_1"), "分类-测试_123")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }
}
