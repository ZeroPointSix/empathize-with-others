package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.CategoryColor
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.EditModeState
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactCategory
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.PersonaSearchState
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.BatchDeleteFactsUseCase
import com.empathy.ai.domain.usecase.BatchMoveFactsUseCase
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.EditContactInfoUseCase
import com.empathy.ai.domain.usecase.EditConversationUseCase
import com.empathy.ai.domain.usecase.EditFactUseCase
import com.empathy.ai.domain.usecase.EditSummaryUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.GroupFactsByCategoryUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.domain.util.FactSearchFilter
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ContactDetailTabViewModel 标签画像V2功能测试
 *
 * 测试范围 (TD-00014):
 * 1. 搜索功能 - UpdatePersonaSearch事件与防抖处理
 * 2. 分类展开/折叠 - ToggleCategoryExpand
 * 3. 编辑模式 - EnterEditMode/ExitEditMode
 * 4. 标签选择 - ToggleFactSelection/SelectAllInCategory
 * 5. 批量删除 - BatchDelete流程（确认、执行、状态重置）
 * 6. 批量移动 - BatchMove流程（确认、执行、状态重置）
 * 7. Feature Flag - SetUsePersonaTabV2控制V1/V2切换
 * 8. 边界条件 - 无选中项不执行批量操作
 *
 * 业务背景 (PRD-00014):
 *   标签画像V2提供更高效的事实管理方式
 *   - 按分类组织事实，便于浏览和管理
 *   - 支持批量选择、删除、移动操作
 *   - 搜索功能快速定位标签
 *   - 编辑模式提供多选操作界面
 *
 * 设计权衡 (TDD-00014):
 *   - V1/V2通过Feature Flag控制，渐进式发布
 *   - 分类数据由GroupFactsByCategoryUseCase聚合
 *   - 批量操作需要选中状态管理和确认流程
 *   - 防抖延迟350ms，避免频繁搜索请求
 *
 * 任务追踪:
 *   - TD-00014 标签画像V2功能
 *   - FD-00014 联系人画像界面升级功能设计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailTabViewModelPersonaTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mock dependencies
    private lateinit var getContactUseCase: GetContactUseCase
    private lateinit var getBrainTagsUseCase: GetBrainTagsUseCase
    private lateinit var saveBrainTagUseCase: SaveBrainTagUseCase
    private lateinit var saveProfileUseCase: SaveProfileUseCase
    private lateinit var deleteBrainTagUseCase: DeleteBrainTagUseCase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var editFactUseCase: EditFactUseCase
    private lateinit var editConversationUseCase: EditConversationUseCase
    private lateinit var editSummaryUseCase: EditSummaryUseCase
    private lateinit var editContactInfoUseCase: EditContactInfoUseCase
    private lateinit var groupFactsByCategoryUseCase: GroupFactsByCategoryUseCase
    private lateinit var batchDeleteFactsUseCase: BatchDeleteFactsUseCase
    private lateinit var batchMoveFactsUseCase: BatchMoveFactsUseCase
    private lateinit var factSearchFilter: FactSearchFilter

    private lateinit var viewModel: ContactDetailTabViewModel

    private val testColor = CategoryColor(
        titleColor = 0xFFB71C1C,
        tagBackgroundColor = 0xFFFFCDD2,
        tagTextColor = 0xFFB71C1C
    )

    private val testContactId = "contact_123"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        getContactUseCase = mockk()
        getBrainTagsUseCase = mockk()
        saveBrainTagUseCase = mockk()
        saveProfileUseCase = mockk()
        deleteBrainTagUseCase = mockk()
        conversationRepository = mockk()
        dailySummaryRepository = mockk()
        editFactUseCase = mockk()
        editConversationUseCase = mockk()
        editSummaryUseCase = mockk()
        editContactInfoUseCase = mockk()
        groupFactsByCategoryUseCase = mockk()
        batchDeleteFactsUseCase = mockk()
        batchMoveFactsUseCase = mockk()
        factSearchFilter = FactSearchFilter()

        // Default mock behaviors
        every { getBrainTagsUseCase(any()) } returns flowOf(emptyList())
        coEvery { conversationRepository.getConversationsByContact(any()) } returns Result.success(emptyList())
        coEvery { dailySummaryRepository.getSummariesByContact(any()) } returns Result.success(emptyList())
        every { groupFactsByCategoryUseCase(any(), any()) } returns emptyList()

        viewModel = ContactDetailTabViewModel(
            getContactUseCase = getContactUseCase,
            getBrainTagsUseCase = getBrainTagsUseCase,
            saveBrainTagUseCase = saveBrainTagUseCase,
            saveProfileUseCase = saveProfileUseCase,
            deleteBrainTagUseCase = deleteBrainTagUseCase,
            conversationRepository = conversationRepository,
            dailySummaryRepository = dailySummaryRepository,
            editFactUseCase = editFactUseCase,
            editConversationUseCase = editConversationUseCase,
            editSummaryUseCase = editSummaryUseCase,
            editContactInfoUseCase = editContactInfoUseCase,
            groupFactsByCategoryUseCase = groupFactsByCategoryUseCase,
            batchDeleteFactsUseCase = batchDeleteFactsUseCase,
            batchMoveFactsUseCase = batchMoveFactsUseCase,
            factSearchFilter = factSearchFilter
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

    private fun createCategory(
        key: String,
        facts: List<Fact>,
        isExpanded: Boolean = true
    ) = FactCategory(
        key = key,
        facts = facts,
        color = testColor,
        isExpanded = isExpanded
    )

    private fun createContact(facts: List<Fact> = emptyList()) = ContactProfile(
        id = testContactId,
        name = "测试联系人",
        targetGoal = "测试目标",
        facts = facts
    )

    // ==================== 搜索事件处理测试 ====================

    @Test
    fun `UpdatePersonaSearch_更新搜索状态`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.UpdatePersonaSearch("性格"))
        
        val state = viewModel.uiState.value
        assertEquals("性格", state.personaSearchState.query)
        assertTrue(state.personaSearchState.isSearching)
    }

    @Test
    fun `UpdatePersonaSearch_防抖处理`() = runTest {
        // 快速连续输入
        viewModel.onEvent(ContactDetailUiEvent.UpdatePersonaSearch("性"))
        viewModel.onEvent(ContactDetailUiEvent.UpdatePersonaSearch("性格"))
        viewModel.onEvent(ContactDetailUiEvent.UpdatePersonaSearch("性格特"))
        
        // 等待防抖时间
        advanceTimeBy(350)
        
        val state = viewModel.uiState.value
        assertEquals("性格特", state.personaSearchState.query)
    }

    @Test
    fun `ClearPersonaSearch_清除搜索状态`() = runTest {
        // 先设置搜索状态
        viewModel.onEvent(ContactDetailUiEvent.UpdatePersonaSearch("测试"))
        advanceUntilIdle()
        
        // 清除搜索
        viewModel.onEvent(ContactDetailUiEvent.ClearPersonaSearch)
        
        val state = viewModel.uiState.value
        assertEquals("", state.personaSearchState.query)
        assertFalse(state.personaSearchState.isSearching)
    }

    // ==================== 分类展开/折叠测试 ====================

    @Test
    fun `ToggleCategoryExpand_切换展开状态`() = runTest {
        // 准备初始分类数据
        val categories = listOf(
            createCategory("性格特点", listOf(createFact()), isExpanded = true)
        )
        every { groupFactsByCategoryUseCase(any(), any()) } returns categories
        
        // 设置初始状态
        viewModel.onEvent(ContactDetailUiEvent.SetUsePersonaTabV2(true))
        advanceUntilIdle()
        
        // 切换展开状态
        viewModel.onEvent(ContactDetailUiEvent.ToggleCategoryExpand("性格特点"))
        
        val state = viewModel.uiState.value
        val category = state.factCategories.find { it.key == "性格特点" }
        assertNotNull(category)
        assertFalse(category!!.isExpanded)
    }

    // ==================== 编辑模式进入/退出测试 ====================

    @Test
    fun `EnterEditMode_进入编辑模式_无初始选中`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode(null))
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.isActive)
        assertTrue(state.editModeState.selectedFactIds.isEmpty())
    }

    @Test
    fun `EnterEditMode_进入编辑模式_有初始选中`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.isActive)
        assertTrue(state.editModeState.selectedFactIds.contains("fact_1"))
        assertEquals(1, state.editModeState.selectedCount)
    }

    @Test
    fun `ExitEditMode_退出编辑模式`() = runTest {
        // 先进入编辑模式
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        
        // 退出编辑模式
        viewModel.onEvent(ContactDetailUiEvent.ExitEditMode)
        
        val state = viewModel.uiState.value
        assertFalse(state.editModeState.isActive)
        assertTrue(state.editModeState.selectedFactIds.isEmpty())
    }

    // ==================== 标签选择/取消选择测试 ====================

    @Test
    fun `ToggleFactSelection_选中标签`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode(null))
        viewModel.onEvent(ContactDetailUiEvent.ToggleFactSelection("fact_1"))
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.selectedFactIds.contains("fact_1"))
    }

    @Test
    fun `ToggleFactSelection_取消选中标签`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ToggleFactSelection("fact_1"))
        
        val state = viewModel.uiState.value
        assertFalse(state.editModeState.selectedFactIds.contains("fact_1"))
    }

    @Test
    fun `SelectAllInCategory_选中分类下所有标签`() = runTest {
        // 准备分类数据
        val facts = listOf(
            createFact("fact_1", "性格特点", "开朗"),
            createFact("fact_2", "性格特点", "乐观")
        )
        val categories = listOf(createCategory("性格特点", facts))
        every { groupFactsByCategoryUseCase(any(), any()) } returns categories
        
        // 设置初始状态
        viewModel.onEvent(ContactDetailUiEvent.SetUsePersonaTabV2(true))
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode(null))
        advanceUntilIdle()
        
        // 选中分类下所有标签
        viewModel.onEvent(ContactDetailUiEvent.SelectAllInCategory("性格特点"))
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.selectedFactIds.contains("fact_1"))
        assertTrue(state.editModeState.selectedFactIds.contains("fact_2"))
    }

    @Test
    fun `DeselectAllFacts_取消选中所有标签`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ToggleFactSelection("fact_2"))
        
        viewModel.onEvent(ContactDetailUiEvent.DeselectAllFacts)
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.selectedFactIds.isEmpty())
    }

    @Test
    fun `SelectAllFacts_选中所有标签`() = runTest {
        // 准备分类数据
        val categories = listOf(
            createCategory("性格特点", listOf(createFact("fact_1"))),
            createCategory("兴趣爱好", listOf(createFact("fact_2", "兴趣爱好", "读书")))
        )
        every { groupFactsByCategoryUseCase(any(), any()) } returns categories
        
        viewModel.onEvent(ContactDetailUiEvent.SetUsePersonaTabV2(true))
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode(null))
        advanceUntilIdle()
        
        viewModel.onEvent(ContactDetailUiEvent.SelectAllFacts)
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.selectedFactIds.contains("fact_1"))
        assertTrue(state.editModeState.selectedFactIds.contains("fact_2"))
    }

    // ==================== 批量删除流程测试 ====================

    @Test
    fun `ShowBatchDeleteConfirm_显示删除确认对话框`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ShowBatchDeleteConfirm)
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.showDeleteConfirm)
    }

    @Test
    fun `HideBatchDeleteConfirm_隐藏删除确认对话框`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ShowBatchDeleteConfirm)
        viewModel.onEvent(ContactDetailUiEvent.HideBatchDeleteConfirm)
        
        val state = viewModel.uiState.value
        assertFalse(state.editModeState.showDeleteConfirm)
    }

    @Test
    fun `ConfirmBatchDelete_成功删除`() = runTest {
        // 准备数据
        val facts = listOf(createFact("fact_1"))
        val contact = createContact(facts)
        
        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { batchDeleteFactsUseCase(testContactId, listOf("fact_1")) } returns Result.success(1)
        every { groupFactsByCategoryUseCase(any(), any()) } returns emptyList()
        
        // 加载联系人
        viewModel.loadContactDetail(testContactId)
        advanceUntilIdle()
        
        // 进入编辑模式并选中
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ShowBatchDeleteConfirm)
        viewModel.onEvent(ContactDetailUiEvent.ConfirmBatchDelete)
        advanceUntilIdle()
        
        // 验证调用
        coVerify { batchDeleteFactsUseCase(testContactId, listOf("fact_1")) }
        
        // 验证状态
        val state = viewModel.uiState.value
        assertFalse(state.editModeState.isActive)
        assertEquals("已删除 1 个标签", state.successMessage)
    }

    @Test
    fun `ConfirmBatchDelete_删除失败`() = runTest {
        // 准备数据
        val facts = listOf(createFact("fact_1"))
        val contact = createContact(facts)
        
        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { batchDeleteFactsUseCase(testContactId, any()) } returns 
            Result.failure(RuntimeException("删除失败"))
        
        // 加载联系人
        viewModel.loadContactDetail(testContactId)
        advanceUntilIdle()
        
        // 进入编辑模式并选中
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmBatchDelete)
        advanceUntilIdle()
        
        // 验证状态
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("删除失败"))
    }

    // ==================== 批量移动流程测试 ====================

    @Test
    fun `ShowBatchMoveDialog_显示移动对话框`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ShowBatchMoveDialog)
        
        val state = viewModel.uiState.value
        assertTrue(state.editModeState.showMoveDialog)
    }

    @Test
    fun `HideBatchMoveDialog_隐藏移动对话框`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ShowBatchMoveDialog)
        viewModel.onEvent(ContactDetailUiEvent.HideBatchMoveDialog)
        
        val state = viewModel.uiState.value
        assertFalse(state.editModeState.showMoveDialog)
    }

    @Test
    fun `ConfirmBatchMove_成功移动`() = runTest {
        // 准备数据
        val facts = listOf(createFact("fact_1", "原分类"))
        val contact = createContact(facts)
        
        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { batchMoveFactsUseCase(testContactId, listOf("fact_1"), "新分类") } returns Result.success(1)
        every { groupFactsByCategoryUseCase(any(), any()) } returns emptyList()
        
        // 加载联系人
        viewModel.loadContactDetail(testContactId)
        advanceUntilIdle()
        
        // 进入编辑模式并选中
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ShowBatchMoveDialog)
        viewModel.onEvent(ContactDetailUiEvent.ConfirmBatchMove("新分类"))
        advanceUntilIdle()
        
        // 验证调用
        coVerify { batchMoveFactsUseCase(testContactId, listOf("fact_1"), "新分类") }
        
        // 验证状态
        val state = viewModel.uiState.value
        assertFalse(state.editModeState.isActive)
        assertEquals("已移动 1 个标签到「新分类」", state.successMessage)
    }

    @Test
    fun `ConfirmBatchMove_移动失败`() = runTest {
        // 准备数据
        val facts = listOf(createFact("fact_1"))
        val contact = createContact(facts)
        
        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        coEvery { batchMoveFactsUseCase(testContactId, any(), any()) } returns 
            Result.failure(RuntimeException("移动失败"))
        
        // 加载联系人
        viewModel.loadContactDetail(testContactId)
        advanceUntilIdle()
        
        // 进入编辑模式并选中
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmBatchMove("新分类"))
        advanceUntilIdle()
        
        // 验证状态
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("移动失败"))
    }

    // ==================== Feature Flag测试 ====================

    @Test
    fun `SetUsePersonaTabV2_启用V2`() = runTest {
        every { groupFactsByCategoryUseCase(any(), any()) } returns emptyList()
        
        viewModel.onEvent(ContactDetailUiEvent.SetUsePersonaTabV2(true))
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state.usePersonaTabV2)
    }

    @Test
    fun `SetUsePersonaTabV2_禁用V2`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.SetUsePersonaTabV2(false))
        
        val state = viewModel.uiState.value
        assertFalse(state.usePersonaTabV2)
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `批量操作_无选中项时不执行`() = runTest {
        val contact = createContact()
        coEvery { getContactUseCase(testContactId) } returns Result.success(contact)
        
        viewModel.loadContactDetail(testContactId)
        advanceUntilIdle()
        
        // 进入编辑模式但不选中任何项
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode(null))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmBatchDelete)
        advanceUntilIdle()
        
        // 不应该调用删除UseCase
        coVerify(exactly = 0) { batchDeleteFactsUseCase(any(), any()) }
    }

    @Test
    fun `批量操作_无联系人时不执行`() = runTest {
        // 不加载联系人，直接尝试批量操作
        viewModel.onEvent(ContactDetailUiEvent.EnterEditMode("fact_1"))
        viewModel.onEvent(ContactDetailUiEvent.ConfirmBatchDelete)
        advanceUntilIdle()
        
        // 不应该调用删除UseCase
        coVerify(exactly = 0) { batchDeleteFactsUseCase(any(), any()) }
    }
}
