package com.empathy.ai.presentation.integration

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.presentation.ui.screen.tag.BrainTagUiEvent
import com.empathy.ai.presentation.viewmodel.BrainTagViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * 标签删除完整流程集成测试
 *
 * 测试从点击删除到标签移除的完整用户流程
 * 验证需求: 3.6, 3.7
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteTagIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getBrainTagsUseCase: GetBrainTagsUseCase
    private lateinit var saveBrainTagUseCase: SaveBrainTagUseCase
    private lateinit var deleteBrainTagUseCase: DeleteBrainTagUseCase

    private lateinit var viewModel: BrainTagViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 创建 mock 对象
        getBrainTagsUseCase = mockk()
        saveBrainTagUseCase = mockk(relaxed = true)
        deleteBrainTagUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 集成测试 - 标签删除完整流程（成功场景）
     *
     * 测试步骤：
     * 1. 用户进入脑标签管理页面
     * 2. 系统加载并显示所有标签
     * 3. 用户点击某个标签的删除按钮
     * 4. 系统显示删除确认对话框
     * 5. 对话框显示标签内容
     * 6. 用户点击"确认删除"按钮
     * 7. 系统从数据库删除标签
     * 8. 系统关闭对话框
     * 9. 系统更新标签列表（标签不再显示）
     */
    @Test
    fun `完整流程 - 成功删除标签`() = runTest {
        // Given: 准备测试数据
        val initialTags = listOf(
            BrainTag(
                id = 1L,
                contactId = "contact-1",
                content = "不要在周一早上讨论敏感话题",
                type = TagType.RISK_RED,
                source = "MANUAL"
            ),
            BrainTag(
                id = 2L,
                contactId = "contact-1",
                content = "下午开会效率更高",
                type = TagType.STRATEGY_GREEN,
                source = "MANUAL"
            ),
            BrainTag(
                id = 3L,
                contactId = "contact-2",
                content = "避免讨论家庭话题",
                type = TagType.RISK_RED,
                source = "MANUAL"
            )
        )

        val tagToDelete = initialTags[0]

        // 使用 MutableStateFlow 模拟标签列表的响应式更新
        val tagsFlow = MutableStateFlow(initialTags)
        coEvery { getBrainTagsUseCase("") } returns tagsFlow

        // Mock 删除操作
        coEvery { deleteBrainTagUseCase(tagToDelete.id) } coAnswers {
            // 模拟删除后更新 Flow
            tagsFlow.value = initialTags.filter { it.id != tagToDelete.id }
            Result.success(Unit)
        }

        // Step 1 & 2: 创建 ViewModel，加载标签列表
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 验证所有标签已加载
        assertEquals(initialTags.size, viewModel.uiState.value.tags.size)

        // 验证初始状态：删除对话框未显示
        assertFalse(viewModel.uiState.value.deleteDialog.isVisible, "初始状态删除对话框应该未显示")

        // Step 3 & 4: 点击删除按钮，显示删除确认对话框
        viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = tagToDelete.id))
        advanceUntilIdle()

        // Step 5: 验证对话框已显示，并显示标签内容
        val deleteDialog = viewModel.uiState.value.deleteDialog
        assertTrue(deleteDialog.isVisible, "删除对话框应该显示")
        assertEquals(tagToDelete.id, deleteDialog.tagId, "对话框应该显示正确的标签ID")
        assertEquals(tagToDelete.content, deleteDialog.tagContent, "对话框应该显示标签内容")

        // Step 6: 点击"确认删除"按钮
        viewModel.onEvent(BrainTagUiEvent.DeleteTag)
        advanceUntilIdle()

        // Step 7: 验证标签已从数据库删除
        coVerify { deleteBrainTagUseCase(tagToDelete.id) }

        // Step 8: 验证对话框已关闭
        assertFalse(viewModel.uiState.value.deleteDialog.isVisible, "删除成功后对话框应该关闭")

        // Step 9: 验证标签列表已更新（标签不再显示）
        val updatedTags = viewModel.uiState.value.tags
        assertEquals(initialTags.size - 1, updatedTags.size, "标签列表应该减少一个")
        assertFalse(updatedTags.any { it.id == tagToDelete.id }, "被删除的标签不应该在列表中")
    }

    /**
     * 集成测试 - 取消删除标签
     *
     * 测试当用户取消删除时，系统应该：
     * 1. 关闭对话框
     * 2. 不删除标签
     * 3. 标签仍然在列表中
     */
    @Test
    fun `完整流程 - 取消删除标签`() = runTest {
        // Given: 准备测试数据
        val tags = listOf(
            BrainTag(
                id = 1L,
                contactId = "contact-1",
                content = "测试标签",
                type = TagType.RISK_RED,
                source = "MANUAL"
            )
        )

        val tagToDelete = tags[0]

        coEvery { getBrainTagsUseCase("") } returns flowOf(tags)

        // 创建 ViewModel
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 打开删除确认对话框
        viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = tagToDelete.id))
        advanceUntilIdle()

        // 验证对话框已显示
        assertTrue(viewModel.uiState.value.deleteDialog.isVisible, "对话框应该显示")

        // 取消删除（关闭对话框）
        viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = false))
        advanceUntilIdle()

        // 验证对话框已关闭
        assertFalse(viewModel.uiState.value.deleteDialog.isVisible, "对话框应该关闭")

        // 验证没有调用删除用例
        coVerify(exactly = 0) { deleteBrainTagUseCase(any()) }

        // 验证标签仍然在列表中
        assertEquals(tags.size, viewModel.uiState.value.tags.size, "标签数量应该不变")
        assertTrue(viewModel.uiState.value.tags.any { it.id == tagToDelete.id },
            "标签应该仍然在列表中")
    }

    /**
     * 集成测试 - 删除标签失败
     *
     * 测试当删除失败时，系统应该：
     * 1. 保持对话框打开
     * 2. 显示错误信息
     * 3. 标签仍然在列表中
     */
    @Test
    fun `完整流程 - 删除标签失败时显示错误`() = runTest {
        // Given: 准备测试数据
        val tags = listOf(
            BrainTag(
                id = 1L,
                contactId = "contact-1",
                content = "测试标签",
                type = TagType.RISK_RED,
                source = "MANUAL"
            )
        )

        val tagToDelete = tags[0]

        coEvery { getBrainTagsUseCase("") } returns flowOf(tags)

        // Mock 删除失败
        coEvery { deleteBrainTagUseCase(tagToDelete.id) } returns Result.failure(
            Exception("数据库连接失败")
        )

        // 创建 ViewModel
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 打开删除确认对话框
        viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = tagToDelete.id))
        advanceUntilIdle()

        // 尝试删除标签
        viewModel.onEvent(BrainTagUiEvent.DeleteTag)
        advanceUntilIdle()

        // 验证对话框仍然打开
        assertTrue(viewModel.uiState.value.deleteDialog.isVisible, "删除失败时对话框应该保持打开")

        // 验证显示错误信息
        assertNotNull(viewModel.uiState.value.error, "应该显示错误信息")

        // 验证标签仍然在列表中
        assertEquals(tags.size, viewModel.uiState.value.tags.size, "标签数量应该不变")
        assertTrue(viewModel.uiState.value.tags.any { it.id == tagToDelete.id },
            "标签应该仍然在列表中")
    }

    /**
     * 集成测试 - 删除多个标签
     *
     * 测试用户可以连续删除多个标签
     */
    @Test
    fun `完整流程 - 连续删除多个标签`() = runTest {
        // Given: 准备测试数据
        val initialTags = listOf(
            BrainTag(
                id = 1L,
                contactId = "contact-1",
                content = "标签1",
                type = TagType.RISK_RED,
                source = "MANUAL"
            ),
            BrainTag(
                id = 2L,
                contactId = "contact-1",
                content = "标签2",
                type = TagType.STRATEGY_GREEN,
                source = "MANUAL"
            ),
            BrainTag(
                id = 3L,
                contactId = "contact-2",
                content = "标签3",
                type = TagType.RISK_RED,
                source = "MANUAL"
            )
        )

        val tagsToDelete = listOf(initialTags[0], initialTags[2])

        // 使用 MutableStateFlow 模拟标签列表的响应式更新
        val tagsFlow = MutableStateFlow(initialTags)
        coEvery { getBrainTagsUseCase("") } returns tagsFlow

        // Mock 删除操作
        val deletedIds = mutableListOf<Long>()
        coEvery { deleteBrainTagUseCase(any()) } coAnswers {
            val tagId = firstArg<Long>()
            deletedIds.add(tagId)
            // 模拟删除后更新 Flow
            tagsFlow.value = tagsFlow.value.filter { it.id != tagId }
            Result.success(Unit)
        }

        // 创建 ViewModel
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 验证初始状态
        assertEquals(initialTags.size, viewModel.uiState.value.tags.size)

        // 连续删除多个标签
        tagsToDelete.forEach { tag ->
            // 打开删除确认对话框
            viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = tag.id))
            advanceUntilIdle()

            // 确认删除
            viewModel.onEvent(BrainTagUiEvent.DeleteTag)
            advanceUntilIdle()

            // 验证对话框已关闭
            assertFalse(viewModel.uiState.value.deleteDialog.isVisible,
                "删除标签${tag.id}后对话框应该关闭")
        }

        // 验证所有标签都被删除
        assertEquals(tagsToDelete.size, deletedIds.size, "应该删除${tagsToDelete.size}个标签")
        tagsToDelete.forEach { tag ->
            assertTrue(deletedIds.contains(tag.id), "标签${tag.id}应该被删除")
        }

        // 验证标签列表已更新
        val remainingTags = viewModel.uiState.value.tags
        assertEquals(initialTags.size - tagsToDelete.size, remainingTags.size,
            "应该剩余${initialTags.size - tagsToDelete.size}个标签")
        tagsToDelete.forEach { tag ->
            assertFalse(remainingTags.any { it.id == tag.id },
                "被删除的标签${tag.id}不应该在列表中")
        }
    }

    /**
     * 集成测试 - 删除最后一个标签
     *
     * 测试当删除最后一个标签时，系统应该：
     * 1. 成功删除标签
     * 2. 显示空状态
     */
    @Test
    fun `完整流程 - 删除最后一个标签显示空状态`() = runTest {
        // Given: 只有一个标签
        val initialTags = listOf(
            BrainTag(
                id = 1L,
                contactId = "contact-1",
                content = "唯一的标签",
                type = TagType.RISK_RED,
                source = "MANUAL"
            )
        )

        val tagToDelete = initialTags[0]

        // 使用 MutableStateFlow 模拟标签列表的响应式更新
        val tagsFlow = MutableStateFlow(initialTags)
        coEvery { getBrainTagsUseCase("") } returns tagsFlow

        // Mock 删除操作
        coEvery { deleteBrainTagUseCase(tagToDelete.id) } coAnswers {
            // 模拟删除后更新 Flow
            tagsFlow.value = emptyList()
            Result.success(Unit)
        }

        // 创建 ViewModel
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 验证初始状态
        assertEquals(1, viewModel.uiState.value.tags.size)
        assertFalse(viewModel.uiState.value.isEmptyState, "初始状态不应该是空状态")

        // 删除标签
        viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = tagToDelete.id))
        advanceUntilIdle()

        viewModel.onEvent(BrainTagUiEvent.DeleteTag)
        advanceUntilIdle()

        // 验证标签列表为空
        assertEquals(0, viewModel.uiState.value.tags.size, "标签列表应该为空")

        // 验证显示空状态
        assertTrue(viewModel.uiState.value.isEmptyState, "应该显示空状态")
    }

    /**
     * 集成测试 - 删除标签后搜索状态保持
     *
     * 测试在搜索模式下删除标签时，搜索状态应该保持
     */
    @Test
    fun `完整流程 - 删除标签后搜索状态保持`() = runTest {
        // Given: 准备测试数据
        val initialTags = listOf(
            BrainTag(
                id = 1L,
                contactId = "contact-1",
                content = "测试标签A",
                type = TagType.RISK_RED,
                source = "MANUAL"
            ),
            BrainTag(
                id = 2L,
                contactId = "contact-1",
                content = "测试标签B",
                type = TagType.STRATEGY_GREEN,
                source = "MANUAL"
            )
        )

        val tagToDelete = initialTags[0]
        val searchQuery = "测试"

        // 使用 MutableStateFlow 模拟标签列表的响应式更新
        val tagsFlow = MutableStateFlow(initialTags)
        coEvery { getBrainTagsUseCase("") } returns tagsFlow

        // Mock 删除操作
        coEvery { deleteBrainTagUseCase(tagToDelete.id) } coAnswers {
            tagsFlow.value = initialTags.filter { it.id != tagToDelete.id }
            Result.success(Unit)
        }

        // 创建 ViewModel
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 激活搜索
        viewModel.onEvent(BrainTagUiEvent.ManageSearch(active = true, query = searchQuery))
        advanceUntilIdle()

        // 验证搜索已激活
        assertTrue(viewModel.uiState.value.searchState.isActive, "搜索应该激活")
        assertEquals(searchQuery, viewModel.uiState.value.searchState.query, "查询应该被设置")

        // 删除标签
        viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = tagToDelete.id))
        advanceUntilIdle()

        viewModel.onEvent(BrainTagUiEvent.DeleteTag)
        advanceUntilIdle()

        // 验证搜索状态仍然保持
        assertTrue(viewModel.uiState.value.searchState.isActive, "删除后搜索应该仍然激活")
        assertEquals(searchQuery, viewModel.uiState.value.searchState.query,
            "删除后查询应该保持不变")

        // 验证搜索结果已更新（不包含被删除的标签）
        val displayTags = viewModel.uiState.value.displayTags
        assertFalse(displayTags.any { it.id == tagToDelete.id },
            "搜索结果不应该包含被删除的标签")
    }

    /**
     * 集成测试 - 级联删除验证
     *
     * 测试删除标签时，应该从所有关联的联系人中移除该标签
     * 注意：这个测试主要验证 ViewModel 正确调用了 UseCase
     * 实际的级联删除逻辑在 Repository 层实现
     */
    @Test
    fun `完整流程 - 级联删除标签`() = runTest {
        // Given: 准备测试数据
        val sharedTag = BrainTag(
            id = 1L,
            contactId = "contact-1",
            content = "共享标签",
            type = TagType.RISK_RED,
            source = "MANUAL"
        )

        val tags = listOf(sharedTag)

        coEvery { getBrainTagsUseCase("") } returns flowOf(tags)

        // Mock 删除操作（验证级联删除）
        val deletedTagIdSlot = slot<Long>()
        coEvery { deleteBrainTagUseCase(capture(deletedTagIdSlot)) } returns Result.success(Unit)

        // 创建 ViewModel
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 删除标签
        viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = sharedTag.id))
        advanceUntilIdle()

        viewModel.onEvent(BrainTagUiEvent.DeleteTag)
        advanceUntilIdle()

        // 验证调用了删除用例
        coVerify { deleteBrainTagUseCase(sharedTag.id) }
        assertTrue(deletedTagIdSlot.isCaptured, "应该调用删除用例")
        assertEquals(sharedTag.id, deletedTagIdSlot.captured, "应该删除正确的标签")

        // 注意：实际的级联删除逻辑（从所有联系人中移除标签）
        // 应该在 DeleteBrainTagUseCase 和 Repository 层实现
        // 这里我们只验证 ViewModel 正确调用了 UseCase
    }

    /**
     * 集成测试 - 删除对话框显示正确的标签信息
     *
     * 测试删除确认对话框应该显示正确的标签内容
     */
    @Test
    fun `完整流程 - 删除对话框显示正确的标签信息`() = runTest {
        // Given: 准备测试数据
        val tags = listOf(
            BrainTag(
                id = 1L,
                contactId = "contact-1",
                content = "这是一个很长的标签内容，用于测试对话框显示",
                type = TagType.RISK_RED,
                source = "MANUAL"
            ),
            BrainTag(
                id = 2L,
                contactId = "contact-1",
                content = "短标签",
                type = TagType.STRATEGY_GREEN,
                source = "MANUAL"
            )
        )

        coEvery { getBrainTagsUseCase("") } returns flowOf(tags)

        // 创建 ViewModel
        viewModel = BrainTagViewModel(
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase
        )

        // 加载标签
        viewModel.onEvent(BrainTagUiEvent.LoadTags)
        advanceUntilIdle()

        // 测试每个标签的删除对话框
        tags.forEach { tag ->
            // 打开删除确认对话框
            viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = true, tagId = tag.id))
            advanceUntilIdle()

            // 验证对话框显示正确的信息
            val deleteDialog = viewModel.uiState.value.deleteDialog
            assertTrue(deleteDialog.isVisible, "对话框应该显示")
            assertEquals(tag.id, deleteDialog.tagId, "应该显示正确的标签ID")
            assertEquals(tag.content, deleteDialog.tagContent, "应该显示正确的标签内容")

            // 关闭对话框
            viewModel.onEvent(BrainTagUiEvent.ManageDeleteDialog(show = false))
            advanceUntilIdle()
        }
    }
}
