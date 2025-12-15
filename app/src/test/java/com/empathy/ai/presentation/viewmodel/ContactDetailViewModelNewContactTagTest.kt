package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 新建联系人添加标签功能测试
 *
 * 测试场景：
 * 1. 新建联系人时生成临时 contactId
 * 2. 新建联系人时启动标签 Flow 监听
 * 3. 新建联系人添加标签后 UI 正确更新
 *
 * 相关 BUG：BUG-00007
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailViewModelNewContactTagTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ContactDetailViewModel
    private lateinit var getContactUseCase: GetContactUseCase
    private lateinit var deleteContactUseCase: DeleteContactUseCase
    private lateinit var getBrainTagsUseCase: GetBrainTagsUseCase
    private lateinit var saveBrainTagUseCase: SaveBrainTagUseCase
    private lateinit var deleteBrainTagUseCase: DeleteBrainTagUseCase
    private lateinit var saveProfileUseCase: SaveProfileUseCase
    private lateinit var dailySummaryRepository: DailySummaryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getContactUseCase = mockk()
        deleteContactUseCase = mockk()
        getBrainTagsUseCase = mockk()
        saveBrainTagUseCase = mockk()
        deleteBrainTagUseCase = mockk()
        saveProfileUseCase = mockk()
        dailySummaryRepository = mockk()

        // 默认返回空标签列表
        coEvery { getBrainTagsUseCase(any()) } returns flowOf(emptyList())

        viewModel = ContactDetailViewModel(
            getContactUseCase = getContactUseCase,
            deleteContactUseCase = deleteContactUseCase,
            getBrainTagsUseCase = getBrainTagsUseCase,
            saveBrainTagUseCase = saveBrainTagUseCase,
            deleteBrainTagUseCase = deleteBrainTagUseCase,
            saveProfileUseCase = saveProfileUseCase,
            dailySummaryRepository = dailySummaryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 测试：新建联系人时应生成临时 contactId
     */
    @Test
    fun `loadContact with blank id should generate temporary contactId`() = runTest {
        // When: 加载空 contactId（新建联系人场景）
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        // Then: 应该生成非空的临时 contactId
        val state = viewModel.uiState.value
        assertTrue("contactId 不应为空", state.contactId.isNotBlank())
        assertTrue("应该是新建联系人模式", state.isNewContact)
        assertTrue("应该是编辑模式", state.isEditMode)
    }

    /**
     * 测试：新建联系人时应启动标签 Flow 监听
     */
    @Test
    fun `loadContact with blank id should start tag flow collection`() = runTest {
        // When: 加载空 contactId（新建联系人场景）
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        // Then: 应该调用 getBrainTagsUseCase 启动 Flow 监听
        val generatedContactId = viewModel.uiState.value.contactId
        coVerify { getBrainTagsUseCase(generatedContactId) }
    }

    /**
     * 测试：新建联系人添加标签后 UI 应正确更新
     */
    @Test
    fun `adding tag to new contact should update UI`() = runTest {
        // Given: 模拟标签保存成功
        coEvery { saveBrainTagUseCase(any()) } returns Result.success(1L)

        // 模拟 Flow 返回添加后的标签
        val testTag = BrainTag(
            id = 1L,
            contactId = "test-id",
            content = "测试标签",
            type = TagType.RISK_RED,
            source = "MANUAL"
        )
        coEvery { getBrainTagsUseCase(any()) } returns flowOf(listOf(testTag))

        // When: 新建联系人并添加标签
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagContent("测试标签"))
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagType(TagType.RISK_RED))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmAddTag)
        advanceUntilIdle()

        // Then: UI 应该显示标签
        val state = viewModel.uiState.value
        assertEquals("应该有1个标签", 1, state.brainTags.size)
        assertEquals("标签内容应该正确", "测试标签", state.brainTags[0].content)
    }

    /**
     * 测试：新建联系人的 contactId 应该是有效的 UUID 格式
     */
    @Test
    fun `generated contactId should be valid UUID format`() = runTest {
        // When: 加载空 contactId（新建联系人场景）
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        // Then: contactId 应该是有效的 UUID 格式
        val contactId = viewModel.uiState.value.contactId
        assertTrue("contactId 应该是有效的 UUID", contactId.matches(
            Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        ))
    }

    /**
     * 测试：多次新建联系人应生成不同的 contactId
     */
    @Test
    fun `multiple new contacts should have different contactIds`() = runTest {
        // When: 第一次新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()
        val firstContactId = viewModel.uiState.value.contactId

        // 创建新的 ViewModel 模拟第二次新建
        val viewModel2 = ContactDetailViewModel(
            getContactUseCase = getContactUseCase,
            deleteContactUseCase = deleteContactUseCase,
            getBrainTagsUseCase = getBrainTagsUseCase,
            saveBrainTagUseCase = saveBrainTagUseCase,
            deleteBrainTagUseCase = deleteBrainTagUseCase,
            saveProfileUseCase = saveProfileUseCase,
            dailySummaryRepository = dailySummaryRepository
        )
        viewModel2.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()
        val secondContactId = viewModel2.uiState.value.contactId

        // Then: 两个 contactId 应该不同
        assertNotEquals("两次新建应生成不同的 contactId", firstContactId, secondContactId)
    }

    /**
     * 测试：添加标签时使用正确的 contactId
     */
    @Test
    fun `addBrainTag should use generated contactId`() = runTest {
        // Given
        coEvery { saveBrainTagUseCase(any()) } returns Result.success(1L)

        // When: 新建联系人并添加标签
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        val generatedContactId = viewModel.uiState.value.contactId

        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagContent("测试标签"))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmAddTag)
        advanceUntilIdle()

        // Then: 保存标签时应使用生成的 contactId
        coVerify {
            saveBrainTagUseCase(match { tag ->
                tag.contactId == generatedContactId && tag.content == "测试标签"
            })
        }
    }
}
