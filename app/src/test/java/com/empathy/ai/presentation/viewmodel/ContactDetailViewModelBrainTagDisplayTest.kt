package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00017 测试：新建联系人时连续添加多个标签应该全部显示
 *
 * 问题背景 (BUG-00017):
 *   新建联系人场景下，通过Flow响应式更新标签时，
 *   连续添加多个标签可能因状态更新不及时导致部分标签丢失
 *
 * 测试策略:
 *   使用 MutableStateFlow 模拟数据库返回，验证:
 *   1. 单个标签添加后UI正确更新
 *   2. 连续添加多个标签时所有标签都应该显示
 *   3. displayTags计算属性正确返回标签列表
 *
 * 设计权衡:
 *   - 使用MutableStateFlow模拟真实Flow行为
 *   - 在saveBrainTagUseCase mock中手动更新Flow值
 *   - 模拟数据库保存后返回新ID的场景
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailViewModelBrainTagDisplayTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ContactDetailViewModel
    private lateinit var getContactUseCase: GetContactUseCase
    private lateinit var deleteContactUseCase: DeleteContactUseCase
    private lateinit var getBrainTagsUseCase: GetBrainTagsUseCase
    private lateinit var saveBrainTagUseCase: SaveBrainTagUseCase
    private lateinit var deleteBrainTagUseCase: DeleteBrainTagUseCase
    private lateinit var saveProfileUseCase: SaveProfileUseCase
    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var brainTagRepository: BrainTagRepository

    // 模拟标签数据流
    private val tagsFlow = MutableStateFlow<List<BrainTag>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 初始化Mock对象
        getContactUseCase = mockk()
        deleteContactUseCase = mockk()
        brainTagRepository = mockk()
        getBrainTagsUseCase = GetBrainTagsUseCase(brainTagRepository)
        saveBrainTagUseCase = mockk()
        deleteBrainTagUseCase = mockk()
        saveProfileUseCase = mockk()
        dailySummaryRepository = mockk()

        // 配置Mock行为
        coEvery { getContactUseCase(any()) } returns Result.success(null)
        every { brainTagRepository.getTagsForContact(any()) } returns tagsFlow
        coEvery { saveBrainTagUseCase(any()) } answers {
            val tag = firstArg<BrainTag>()
            // 模拟数据库保存后返回新ID，并更新Flow
            val newId = (tagsFlow.value.maxOfOrNull { it.id } ?: 0) + 1
            val savedTag = tag.copy(id = newId)
            tagsFlow.value = tagsFlow.value + savedTag
            Result.success(newId)
        }

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
        tagsFlow.value = emptyList()
    }

    @Test
    fun `新建联系人时添加单个标签应该显示`() = runTest(testDispatcher) {
        // Given: 新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        // 验证初始状态
        assertTrue(viewModel.uiState.value.isNewContact)
        assertTrue(viewModel.uiState.value.brainTags.isEmpty())

        // When: 添加一个标签
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagContent("不要提前任"))
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagType(TagType.RISK_RED))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmAddTag)
        advanceUntilIdle()

        // Then: 标签应该显示
        assertEquals(1, viewModel.uiState.value.brainTags.size)
        assertEquals("不要提前任", viewModel.uiState.value.brainTags[0].content)
        assertEquals(TagType.RISK_RED, viewModel.uiState.value.brainTags[0].type)
    }

    @Test
    fun `新建联系人时连续添加多个标签应该全部显示`() = runTest(testDispatcher) {
        // Given: 新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        // When: 连续添加3个标签
        // 标签1
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagContent("不要提前任"))
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagType(TagType.RISK_RED))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmAddTag)
        advanceUntilIdle()

        // 标签2
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagContent("喜欢聊摄影"))
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagType(TagType.STRATEGY_GREEN))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmAddTag)
        advanceUntilIdle()

        // 标签3
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagContent("忌讳迟到"))
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagType(TagType.RISK_RED))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmAddTag)
        advanceUntilIdle()

        // Then: 应该有3个标签
        assertEquals(3, viewModel.uiState.value.brainTags.size)
        
        // 验证标签内容
        val tagContents = viewModel.uiState.value.brainTags.map { it.content }
        assertTrue(tagContents.contains("不要提前任"))
        assertTrue(tagContents.contains("喜欢聊摄影"))
        assertTrue(tagContents.contains("忌讳迟到"))
    }

    @Test
    fun `displayTags应该返回正确的标签列表`() = runTest(testDispatcher) {
        // Given: 新建联系人并添加标签
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagContent("测试标签"))
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewTagType(TagType.STRATEGY_GREEN))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmAddTag)
        advanceUntilIdle()

        // Then: displayTags应该返回标签
        assertEquals(1, viewModel.uiState.value.displayTags.size)
        assertEquals("测试标签", viewModel.uiState.value.displayTags[0].content)
    }
}
