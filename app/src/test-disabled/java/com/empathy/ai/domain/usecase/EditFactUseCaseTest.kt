package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.EditResult
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.ContentValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * EditFactUseCase 单元测试
 */
class EditFactUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var contentValidator: ContentValidator
    private lateinit var useCase: EditFactUseCase
    private val testDispatcher = StandardTestDispatcher()

    private val testContactId = "contact-123"
    private val testFactId = "fact-456"
    private val testTimestamp = System.currentTimeMillis()

    private val testFact = Fact(
        id = testFactId,
        key = "性格特点",
        value = "开朗乐观",
        timestamp = testTimestamp,
        source = FactSource.MANUAL
    )

    private val testContact = ContactProfile(
        id = testContactId,
        name = "测试联系人",
        targetGoal = "保持良好关系",
        facts = listOf(testFact)
    )

    @Before
    fun setup() {
        contactRepository = mockk(relaxed = true)
        contentValidator = ContentValidator()
        useCase = EditFactUseCase(
            contactRepository = contactRepository,
            contentValidator = contentValidator,
            ioDispatcher = testDispatcher
        )
    }


    // ==================== 验证失败场景 ====================

    @Test
    fun `空键返回ValidationError`() = runTest(testDispatcher) {
        val result = useCase(testContactId, testFactId, "", "新内容")
        assertTrue(result.isSuccess)
        val editResult = result.getOrNull()
        assertTrue(editResult is EditResult.ValidationError)
        assertEquals("事实类型不能为空", (editResult as EditResult.ValidationError).message)
    }

    @Test
    fun `空白键返回ValidationError`() = runTest(testDispatcher) {
        val result = useCase(testContactId, testFactId, "   ", "新内容")
        assertTrue(result.isSuccess)
        val editResult = result.getOrNull()
        assertTrue(editResult is EditResult.ValidationError)
    }

    @Test
    fun `超长键返回ValidationError`() = runTest(testDispatcher) {
        val longKey = "a".repeat(51)
        val result = useCase(testContactId, testFactId, longKey, "新内容")
        assertTrue(result.isSuccess)
        val editResult = result.getOrNull()
        assertTrue(editResult is EditResult.ValidationError)
        assertEquals("事实类型不能超过50字", (editResult as EditResult.ValidationError).message)
    }

    @Test
    fun `空值返回ValidationError`() = runTest(testDispatcher) {
        val result = useCase(testContactId, testFactId, "新类型", "")
        assertTrue(result.isSuccess)
        val editResult = result.getOrNull()
        assertTrue(editResult is EditResult.ValidationError)
        assertEquals("事实内容不能为空", (editResult as EditResult.ValidationError).message)
    }

    @Test
    fun `超长值返回ValidationError`() = runTest(testDispatcher) {
        val longValue = "a".repeat(501)
        val result = useCase(testContactId, testFactId, "新类型", longValue)
        assertTrue(result.isSuccess)
        val editResult = result.getOrNull()
        assertTrue(editResult is EditResult.ValidationError)
    }

    // ==================== 联系人不存在场景 ====================

    @Test
    fun `联系人不存在返回NotFound`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)
        val result = useCase(testContactId, testFactId, "新类型", "新内容")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NotFound)
    }

    @Test
    fun `获取联系人失败返回NotFound`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.failure(Exception("数据库错误"))
        val result = useCase(testContactId, testFactId, "新类型", "新内容")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NotFound)
    }

    // ==================== 事实不存在场景 ====================

    @Test
    fun `事实不存在返回NotFound`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        val result = useCase(testContactId, "non-existent-fact", "新类型", "新内容")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NotFound)
    }

    @Test
    fun `联系人无事实返回NotFound`() = runTest(testDispatcher) {
        val contactWithoutFacts = testContact.copy(facts = emptyList())
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(contactWithoutFacts)
        val result = useCase(testContactId, testFactId, "新类型", "新内容")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NotFound)
    }


    // ==================== 内容无变化场景 ====================

    @Test
    fun `内容无变化返回NoChanges`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        val result = useCase(testContactId, testFactId, "性格特点", "开朗乐观")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NoChanges)
    }

    @Test
    fun `仅空格差异视为无变化`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        val result = useCase(testContactId, testFactId, "  性格特点  ", "  开朗乐观  ")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NoChanges)
    }

    // ==================== 编辑成功场景 ====================

    @Test
    fun `编辑键成功`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        val profileSlot = slot<ContactProfile>()
        coEvery { contactRepository.updateProfile(capture(profileSlot)) } returns Result.success(Unit)

        val result = useCase(testContactId, testFactId, "兴趣爱好", "开朗乐观")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.Success)

        val updatedFact = profileSlot.captured.facts.find { it.id == testFactId }!!
        assertEquals("兴趣爱好", updatedFact.key)
        assertEquals("开朗乐观", updatedFact.value)
        assertTrue(updatedFact.isUserModified)
        assertEquals("性格特点", updatedFact.originalKey)
    }

    @Test
    fun `编辑值成功`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        val profileSlot = slot<ContactProfile>()
        coEvery { contactRepository.updateProfile(capture(profileSlot)) } returns Result.success(Unit)

        val result = useCase(testContactId, testFactId, "性格特点", "内向沉稳")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.Success)

        val updatedFact = profileSlot.captured.facts.find { it.id == testFactId }!!
        assertEquals("内向沉稳", updatedFact.value)
        assertEquals("开朗乐观", updatedFact.originalValue)
    }

    @Test
    fun `同时编辑键和值成功`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        val profileSlot = slot<ContactProfile>()
        coEvery { contactRepository.updateProfile(capture(profileSlot)) } returns Result.success(Unit)

        val result = useCase(testContactId, testFactId, "工作信息", "软件工程师")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.Success)

        val updatedFact = profileSlot.captured.facts.find { it.id == testFactId }!!
        assertEquals("工作信息", updatedFact.key)
        assertEquals("软件工程师", updatedFact.value)
        assertEquals("性格特点", updatedFact.originalKey)
        assertEquals("开朗乐观", updatedFact.originalValue)
    }

    @Test
    fun `编辑成功后调用updateProfile`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        coEvery { contactRepository.updateProfile(any()) } returns Result.success(Unit)

        useCase(testContactId, testFactId, "新类型", "新内容")

        coVerify(exactly = 1) { contactRepository.updateProfile(any()) }
    }


    // ==================== 首次编辑保存原始值 ====================

    @Test
    fun `首次编辑保存原始键和值`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        val profileSlot = slot<ContactProfile>()
        coEvery { contactRepository.updateProfile(capture(profileSlot)) } returns Result.success(Unit)

        useCase(testContactId, testFactId, "新类型", "新内容")

        val updatedFact = profileSlot.captured.facts.find { it.id == testFactId }!!
        assertEquals("性格特点", updatedFact.originalKey)
        assertEquals("开朗乐观", updatedFact.originalValue)
    }

    // ==================== 再次编辑保留首次原始值 ====================

    @Test
    fun `再次编辑保留首次原始值`() = runTest(testDispatcher) {
        val editedFact = testFact.copy(
            key = "第一次编辑的键",
            value = "第一次编辑的值",
            isUserModified = true,
            originalKey = "性格特点",
            originalValue = "开朗乐观"
        )
        val contactWithEditedFact = testContact.copy(facts = listOf(editedFact))
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(contactWithEditedFact)
        val profileSlot = slot<ContactProfile>()
        coEvery { contactRepository.updateProfile(capture(profileSlot)) } returns Result.success(Unit)

        useCase(testContactId, testFactId, "第二次编辑的键", "第二次编辑的值")

        val updatedFact = profileSlot.captured.facts.find { it.id == testFactId }!!
        assertEquals("第二次编辑的键", updatedFact.key)
        assertEquals("第二次编辑的值", updatedFact.value)
        assertEquals("性格特点", updatedFact.originalKey)
        assertEquals("开朗乐观", updatedFact.originalValue)
    }

    // ==================== 数据库错误场景 ====================

    @Test
    fun `保存时数据库错误返回DatabaseError`() = runTest(testDispatcher) {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        coEvery { contactRepository.updateProfile(any()) } throws RuntimeException("数据库写入失败")

        val result = useCase(testContactId, testFactId, "新类型", "新内容")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.DatabaseError)
    }

    // ==================== 边界情况 ====================

    @Test
    fun `最大长度键验证通过`() = runTest(testDispatcher) {
        val maxLengthKey = "a".repeat(50)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        coEvery { contactRepository.updateProfile(any()) } returns Result.success(Unit)

        val result = useCase(testContactId, testFactId, maxLengthKey, "新内容")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.Success)
    }

    @Test
    fun `最大长度值验证通过`() = runTest(testDispatcher) {
        val maxLengthValue = "a".repeat(500)
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)
        coEvery { contactRepository.updateProfile(any()) } returns Result.success(Unit)

        val result = useCase(testContactId, testFactId, "新类型", maxLengthValue)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.Success)
    }

    @Test
    fun `多个事实中正确更新目标事实`() = runTest(testDispatcher) {
        val fact1 = Fact(id = "fact-1", key = "事实1", value = "值1", timestamp = testTimestamp, source = FactSource.MANUAL)
        val fact2 = testFact
        val fact3 = Fact(id = "fact-3", key = "事实3", value = "值3", timestamp = testTimestamp, source = FactSource.AI_INFERRED)
        val contactWithMultipleFacts = testContact.copy(facts = listOf(fact1, fact2, fact3))
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(contactWithMultipleFacts)
        val profileSlot = slot<ContactProfile>()
        coEvery { contactRepository.updateProfile(capture(profileSlot)) } returns Result.success(Unit)

        useCase(testContactId, testFactId, "更新的类型", "更新的内容")

        val updatedFacts = profileSlot.captured.facts
        assertEquals(3, updatedFacts.size)

        val updatedFact1 = updatedFacts.find { it.id == "fact-1" }!!
        assertEquals("事实1", updatedFact1.key)
        assertFalse(updatedFact1.isUserModified)

        val updatedFact2 = updatedFacts.find { it.id == testFactId }!!
        assertEquals("更新的类型", updatedFact2.key)
        assertTrue(updatedFact2.isUserModified)

        val updatedFact3 = updatedFacts.find { it.id == "fact-3" }!!
        assertEquals("事实3", updatedFact3.key)
        assertFalse(updatedFact3.isUserModified)
    }

    // ==================== EditResult 方法测试 ====================

    @Test
    fun `EditResult Success isSuccess返回true`() {
        assertTrue(EditResult.Success.isSuccess())
    }

    @Test
    fun `EditResult ValidationError isSuccess返回false`() {
        assertFalse(EditResult.ValidationError("错误").isSuccess())
    }

    @Test
    fun `EditResult DatabaseError isSuccess返回false`() {
        assertFalse(EditResult.DatabaseError(Exception()).isSuccess())
    }

    @Test
    fun `EditResult NotFound isSuccess返回false`() {
        assertFalse(EditResult.NotFound.isSuccess())
    }

    @Test
    fun `EditResult NoChanges isSuccess返回false`() {
        assertFalse(EditResult.NoChanges.isSuccess())
    }

    @Test
    fun `EditResult Success getErrorMessage返回null`() {
        assertEquals(null, EditResult.Success.getErrorMessage())
    }

    @Test
    fun `EditResult ValidationError getErrorMessage返回错误消息`() {
        assertEquals("测试错误", EditResult.ValidationError("测试错误").getErrorMessage())
    }

    @Test
    fun `EditResult DatabaseError getErrorMessage返回固定消息`() {
        assertEquals("保存失败，请重试", EditResult.DatabaseError(Exception()).getErrorMessage())
    }

    @Test
    fun `EditResult NotFound getErrorMessage返回固定消息`() {
        assertEquals("内容已被删除", EditResult.NotFound.getErrorMessage())
    }

    @Test
    fun `EditResult NoChanges getErrorMessage返回null`() {
        assertEquals(null, EditResult.NoChanges.getErrorMessage())
    }
}
