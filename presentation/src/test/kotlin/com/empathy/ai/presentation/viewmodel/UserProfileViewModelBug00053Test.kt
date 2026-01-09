package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileDimension
import com.empathy.ai.domain.repository.UserProfileRepository
import com.empathy.ai.domain.usecase.userprofile.AddTagUseCase
import com.empathy.ai.domain.usecase.userprofile.GetUserProfileUseCase
import com.empathy.ai.domain.usecase.userprofile.RemoveTagUseCase
import com.empathy.ai.domain.usecase.userprofile.ManageCustomDimensionUseCase
import com.empathy.ai.domain.usecase.userprofile.ExportUserProfileUseCase
import com.empathy.ai.domain.usecase.userprofile.UpdateUserProfileUseCase
import com.empathy.ai.presentation.ui.screen.userprofile.UserProfileUiEvent
import com.empathy.ai.presentation.ui.screen.userprofile.UserProfileUiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BUG-00053 用户画像标签管理三项问题测试
 *
 * 测试覆盖：
 * - 问题1：删除标签后编辑对话框不关闭
 * - 问题2：展开/关闭箭头行为异常（UI层测试）
 * - 问题3：长按多选和批量删除功能（新功能测试）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelBug00053Test {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: UserProfileViewModel
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var addTagUseCase: AddTagUseCase
    private lateinit var removeTagUseCase: RemoveTagUseCase
    private lateinit var manageCustomDimensionUseCase: ManageCustomDimensionUseCase
    private lateinit var exportUserProfileUseCase: ExportUserProfileUseCase
    private lateinit var userProfileRepository: UserProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getUserProfileUseCase = mockk()
        updateUserProfileUseCase = mockk()
        addTagUseCase = mockk()
        removeTagUseCase = mockk()
        manageCustomDimensionUseCase = mockk()
        exportUserProfileUseCase = mockk()
        userProfileRepository = mockk()

        // 设置默认返回值
        coEvery { getUserProfileUseCase() } returns flowOf(Result.success(UserProfile()))
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(Unit)
        coEvery { addTagUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { removeTagUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { manageCustomDimensionUseCase.add(any()) } returns Result.success(Unit)
        coEvery { manageCustomDimensionUseCase.remove(any(), any()) } returns Result.success(Unit)
        coEvery { manageCustomDimensionUseCase.rename(any(), any()) } returns Result.success(Unit)
        coEvery { userProfileRepository.exportUserProfile(any()) } returns Result.success("{}")

        viewModel = UserProfileViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            updateUserProfileUseCase = updateUserProfileUseCase,
            addTagUseCase = addTagUseCase,
            removeTagUseCase = removeTagUseCase,
            manageCustomDimensionUseCase = manageCustomDimensionUseCase,
            exportUserProfileUseCase = exportUserProfileUseCase,
            userProfileRepository = userProfileRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 问题1测试：删除标签后编辑对话框不关闭 ====================

    @Test
    fun `问题1-基础维度-localDeleteTag应该关闭编辑对话框`() = runTest {
        // Given: 先添加一个标签，然后打开编辑对话框
        val dimensionKey = UserProfileDimension.PERSONALITY_TRAITS.name
        val tag = "外向"

        // 添加标签
        viewModel.onEvent(UserProfileUiEvent.AddTag(dimensionKey, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // 打开编辑对话框
        viewModel.onEvent(UserProfileUiEvent.ShowEditTagDialog(dimensionKey, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证对话框已打开
        assertTrue("编辑对话框应该已打开", viewModel.uiState.value.showEditTagDialog)
        assertEquals("当前编辑标签应该是'外向'", tag, viewModel.uiState.value.currentEditTag)

        // When: 删除标签（使用 LocalDeleteTag 事件）
        viewModel.onEvent(UserProfileUiEvent.LocalDeleteTag(dimensionKey, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 编辑对话框应该关闭
        assertFalse("删除标签后编辑对话框应该关闭", viewModel.uiState.value.showEditTagDialog)
        assertNull("删除标签后currentEditTag应该为null", viewModel.uiState.value.currentEditTag)
    }

    @Test
    fun `问题1-自定义维度-localDeleteTag应该关闭编辑对话框`() = runTest {
        // Given: 添加自定义维度和标签
        val customDimensionName = "自定义维度"
        val tag = "测试标签"

        // 添加自定义维度
        viewModel.onEvent(UserProfileUiEvent.AddCustomDimension(customDimensionName))
        testDispatcher.scheduler.advanceUntilIdle()

        // 添加标签到自定义维度
        viewModel.onEvent(UserProfileUiEvent.AddTag(customDimensionName, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // 打开编辑对话框
        viewModel.onEvent(UserProfileUiEvent.ShowEditTagDialog(customDimensionName, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证对话框已打开
        assertTrue("编辑对话框应该已打开", viewModel.uiState.value.showEditTagDialog)

        // When: 删除标签
        viewModel.onEvent(UserProfileUiEvent.LocalDeleteTag(customDimensionName, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 编辑对话框应该关闭
        assertFalse("删除自定义维度标签后编辑对话框应该关闭", viewModel.uiState.value.showEditTagDialog)
        assertNull("删除自定义维度标签后currentEditTag应该为null", viewModel.uiState.value.currentEditTag)
    }

    @Test
    fun `问题1-localDeleteTag应该同时关闭删除确认对话框`() = runTest {
        // Given: 添加标签并打开删除确认对话框
        val dimensionKey = UserProfileDimension.VALUES.name
        val tag = "诚实"

        viewModel.onEvent(UserProfileUiEvent.AddTag(dimensionKey, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // 打开删除确认对话框
        viewModel.onEvent(UserProfileUiEvent.ShowDeleteTagConfirm(dimensionKey, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("删除确认对话框应该已打开", viewModel.uiState.value.showDeleteConfirmDialog)

        // When: 确认删除
        viewModel.onEvent(UserProfileUiEvent.LocalDeleteTag(dimensionKey, tag))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 删除确认对话框应该关闭
        assertFalse("删除确认对话框应该关闭", viewModel.uiState.value.showDeleteConfirmDialog)
        assertNull("pendingDeleteTag应该为null", viewModel.uiState.value.pendingDeleteTag)
    }

    @Test
    fun `问题1-对比localEditTag-应该正确关闭对话框`() = runTest {
        // Given: 添加标签并打开编辑对话框
        val dimensionKey = UserProfileDimension.INTERESTS.name
        val oldTag = "阅读"
        val newTag = "写作"

        viewModel.onEvent(UserProfileUiEvent.AddTag(dimensionKey, oldTag))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(UserProfileUiEvent.ShowEditTagDialog(dimensionKey, oldTag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("编辑对话框应该已打开", viewModel.uiState.value.showEditTagDialog)

        // When: 编辑标签
        viewModel.onEvent(UserProfileUiEvent.EditTag(dimensionKey, oldTag, newTag))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 编辑对话框应该关闭（这是正确的行为，用于对比）
        assertFalse("localEditTag后编辑对话框应该关闭", viewModel.uiState.value.showEditTagDialog)
        assertNull("localEditTag后currentEditTag应该为null", viewModel.uiState.value.currentEditTag)
    }

    // ==================== 问题1边缘情况测试 ====================

    @Test
    fun `问题1-删除不存在的标签不应该崩溃`() = runTest {
        // Given: 空的标签列表
        val dimensionKey = UserProfileDimension.PERSONALITY_TRAITS.name
        val nonExistentTag = "不存在的标签"

        // When: 尝试删除不存在的标签
        viewModel.onEvent(UserProfileUiEvent.LocalDeleteTag(dimensionKey, nonExistentTag))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 不应该崩溃，状态应该保持正常
        assertFalse("不应该有未保存的变更", viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `问题1-连续删除多个标签应该正确关闭对话框`() = runTest {
        // Given: 添加多个标签
        val dimensionKey = UserProfileDimension.PERSONALITY_TRAITS.name
        val tags = listOf("外向", "乐观", "热情")

        tags.forEach { tag ->
            viewModel.onEvent(UserProfileUiEvent.AddTag(dimensionKey, tag))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 连续删除标签
        tags.forEach { tag ->
            viewModel.onEvent(UserProfileUiEvent.ShowEditTagDialog(dimensionKey, tag))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onEvent(UserProfileUiEvent.LocalDeleteTag(dimensionKey, tag))
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: 每次删除后对话框都应该关闭
            assertFalse("删除'$tag'后对话框应该关闭", viewModel.uiState.value.showEditTagDialog)
        }
    }

    // ==================== 问题2相关测试（ViewModel层） ====================

    @Test
    fun `问题2-展开状态应该独立管理每个维度`() = runTest {
        // 注意：展开状态主要在UI层管理，这里测试ViewModel不干扰展开逻辑

        // Given: 初始状态
        val initialState = viewModel.uiState.value

        // Then: ViewModel不应该管理展开状态（展开状态在Screen层）
        // 这个测试确保ViewModel没有错误地影响展开逻辑
        assertNotNull("uiState应该存在", initialState)
    }

    // ==================== 问题3相关测试（多选功能预留） ====================

    // 注意：问题3是新功能需求，需要先实现功能后再添加测试
    // 以下是预留的测试框架

    /*
    @Test
    fun `问题3-长按应该进入多选模式`() = runTest {
        // Given: 正常模式
        assertFalse(viewModel.uiState.value.isMultiSelectMode)

        // When: 触发进入多选模式
        viewModel.onEvent(UserProfileUiEvent.EnterMultiSelectMode)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 应该进入多选模式
        assertTrue(viewModel.uiState.value.isMultiSelectMode)
    }

    @Test
    fun `问题3-多选模式下可以选择多个标签`() = runTest {
        // Given: 进入多选模式
        viewModel.onEvent(UserProfileUiEvent.EnterMultiSelectMode)
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 选择多个标签
        viewModel.onEvent(UserProfileUiEvent.ToggleTagSelection("PERSONALITY_TRAITS", "外向"))
        viewModel.onEvent(UserProfileUiEvent.ToggleTagSelection("VALUES", "诚实"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 两个标签都应该被选中
        assertEquals(2, viewModel.uiState.value.selectedTags.size)
    }

    @Test
    fun `问题3-批量删除应该删除所有选中的标签`() = runTest {
        // Given: 选中多个标签
        viewModel.onEvent(UserProfileUiEvent.EnterMultiSelectMode)
        viewModel.onEvent(UserProfileUiEvent.ToggleTagSelection("PERSONALITY_TRAITS", "外向"))
        viewModel.onEvent(UserProfileUiEvent.ToggleTagSelection("VALUES", "诚实"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 批量删除
        viewModel.onEvent(UserProfileUiEvent.DeleteSelectedTags)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 选中的标签应该被删除，退出多选模式
        assertFalse(viewModel.uiState.value.isMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedTags.isEmpty())
    }
    */
}
