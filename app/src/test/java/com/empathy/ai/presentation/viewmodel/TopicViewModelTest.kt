package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.repository.TopicRepository
import com.empathy.ai.domain.usecase.ClearTopicUseCase
import com.empathy.ai.domain.usecase.GetTopicUseCase
import com.empathy.ai.domain.usecase.SetTopicUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TopicViewModel 对话主题功能测试
 *
 * 测试范围：
 * 1. 初始化状态（当前主题、主题历史）
 * 2. 对话框显示/隐藏与状态管理
 * 3. 输入内容更新与验证
 * 4. 主题保存（成功/失败/空内容校验）
 * 5. 主题清除与历史选择
 * 6. 错误处理与状态清除
 *
 * 业务背景 (PRD-00016):
 *   对话主题功能用于标记当前与联系人的主要交流话题
 *   支持设置主题、清除主题、查看历史主题记录
 *
 * 设计权衡 (TDD-00016):
 *   - 使用SavedStateHandle从导航参数获取contactId
 *   - 主题观察通过GetTopicUseCase的Flow实现响应式更新
 *   - 历史记录限制最近10条，避免数据膨胀
 *
 * 任务追踪:
 *   - FD-00016 对话主题功能设计
 *   - TDD-00016 对话主题功能技术设计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TopicViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var setTopicUseCase: SetTopicUseCase
    private lateinit var getTopicUseCase: GetTopicUseCase
    private lateinit var clearTopicUseCase: ClearTopicUseCase
    private lateinit var topicRepository: TopicRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: TopicViewModel

    private val testContactId = "contact_123"
    private val testTopic = ConversationTopic(
        id = "topic_1",
        contactId = testContactId,
        content = "讨论项目进度",
        isActive = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        setTopicUseCase = mockk()
        getTopicUseCase = mockk()
        clearTopicUseCase = mockk()
        topicRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("contactId" to testContactId))

        // 默认mock行为
        every { getTopicUseCase.observe(any()) } returns flowOf(null)
        coEvery { topicRepository.getTopicHistory(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TopicViewModel {
        return TopicViewModel(
            setTopicUseCase = setTopicUseCase,
            getTopicUseCase = getTopicUseCase,
            clearTopicUseCase = clearTopicUseCase,
            topicRepository = topicRepository,
            savedStateHandle = savedStateHandle
        )
    }

    // ==================== 初始化测试 ====================

    @Test
    fun `初始化时应该加载当前主题`() = runTest {
        every { getTopicUseCase.observe(testContactId) } returns flowOf(testTopic)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(testTopic, viewModel.uiState.value.currentTopic)
    }

    @Test
    fun `初始化时应该加载主题历史`() = runTest {
        val history = listOf(testTopic)
        coEvery { topicRepository.getTopicHistory(testContactId, 10) } returns history

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(history, viewModel.uiState.value.topicHistory)
    }

    @Test
    fun `初始化时contactId为空应该不加载数据`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("contactId" to ""))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.currentTopic)
        assertTrue(viewModel.uiState.value.topicHistory.isEmpty())
    }

    // ==================== 显示/隐藏对话框测试 ====================

    @Test
    fun `ShowSettingDialog事件应该显示对话框`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ShowSettingDialog)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSettingDialog)
    }

    @Test
    fun `ShowSettingDialog事件应该填充当前主题内容`() = runTest {
        every { getTopicUseCase.observe(testContactId) } returns flowOf(testTopic)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ShowSettingDialog)
        advanceUntilIdle()

        assertEquals(testTopic.content, viewModel.uiState.value.inputContent)
    }

    @Test
    fun `HideSettingDialog事件应该隐藏对话框`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ShowSettingDialog)
        viewModel.onEvent(TopicUiEvent.HideSettingDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSettingDialog)
    }

    @Test
    fun `HideSettingDialog事件应该清空输入内容`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput("测试内容"))
        viewModel.onEvent(TopicUiEvent.HideSettingDialog)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.inputContent)
    }

    @Test
    fun `HideSettingDialog事件应该清空错误信息`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // 先触发一个错误
        viewModel.onEvent(TopicUiEvent.SaveTopic) // 空内容会触发错误
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.HideSettingDialog)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }


    // ==================== 输入更新测试 ====================

    @Test
    fun `UpdateInput事件应该更新输入内容`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val newContent = "新的主题内容"
        viewModel.onEvent(TopicUiEvent.UpdateInput(newContent))
        advanceUntilIdle()

        assertEquals(newContent, viewModel.uiState.value.inputContent)
    }

    // ==================== 保存主题测试 ====================

    @Test
    fun `SaveTopic事件空内容应该显示错误`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput(""))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        assertEquals("主题内容不能为空", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `SaveTopic事件空白内容应该显示错误`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput("   "))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        assertEquals("主题内容不能为空", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `SaveTopic事件成功应该关闭对话框`() = runTest {
        coEvery { setTopicUseCase(testContactId, any()) } returns Result.success(testTopic)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ShowSettingDialog)
        viewModel.onEvent(TopicUiEvent.UpdateInput("有效主题"))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSettingDialog)
    }

    @Test
    fun `SaveTopic事件成功应该设置saveSuccess为true`() = runTest {
        coEvery { setTopicUseCase(testContactId, any()) } returns Result.success(testTopic)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput("有效主题"))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun `SaveTopic事件成功应该刷新历史记录`() = runTest {
        coEvery { setTopicUseCase(testContactId, any()) } returns Result.success(testTopic)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput("有效主题"))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        coVerify(atLeast = 2) { topicRepository.getTopicHistory(testContactId, 10) }
    }

    @Test
    fun `SaveTopic事件失败应该显示错误信息`() = runTest {
        val errorMessage = "保存失败：网络错误"
        coEvery { setTopicUseCase(testContactId, any()) } returns Result.failure(Exception(errorMessage))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput("有效主题"))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `SaveTopic事件应该显示加载状态`() = runTest {
        coEvery { setTopicUseCase(testContactId, any()) } coAnswers {
            Result.success(testTopic)
        }

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput("有效主题"))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading) // 完成后应该是false
    }

    // ==================== 清除主题测试 ====================

    @Test
    fun `ClearTopic事件成功应该关闭对话框`() = runTest {
        coEvery { clearTopicUseCase(testContactId) } returns Result.success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ShowSettingDialog)
        viewModel.onEvent(TopicUiEvent.ClearTopic)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSettingDialog)
    }

    @Test
    fun `ClearTopic事件失败应该显示错误信息`() = runTest {
        val errorMessage = "清除失败"
        coEvery { clearTopicUseCase(testContactId) } returns Result.failure(Exception(errorMessage))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ClearTopic)
        advanceUntilIdle()

        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
    }

    // ==================== 从历史选择测试 ====================

    @Test
    fun `SelectFromHistory事件应该填充输入内容`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.SelectFromHistory(testTopic))
        advanceUntilIdle()

        assertEquals(testTopic.content, viewModel.uiState.value.inputContent)
    }

    // ==================== 清除错误测试 ====================

    @Test
    fun `ClearError事件应该清除错误信息`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // 先触发一个错误
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ClearError)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ==================== 清除保存成功状态测试 ====================

    @Test
    fun `ClearSaveSuccess事件应该清除保存成功状态`() = runTest {
        coEvery { setTopicUseCase(testContactId, any()) } returns Result.success(testTopic)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.UpdateInput("有效主题"))
        viewModel.onEvent(TopicUiEvent.SaveTopic)
        advanceUntilIdle()

        viewModel.onEvent(TopicUiEvent.ClearSaveSuccess)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    // ==================== loadTopic方法测试 ====================

    @Test
    fun `loadTopic应该加载指定联系人的主题`() = runTest {
        val newContactId = "contact_456"
        every { getTopicUseCase.observe(newContactId) } returns flowOf(testTopic.copy(contactId = newContactId))
        coEvery { topicRepository.getTopicHistory(newContactId, 10) } returns emptyList()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadTopic(newContactId)
        advanceUntilIdle()

        coVerify { topicRepository.getTopicHistory(newContactId, 10) }
    }

    @Test
    fun `loadTopic空contactId应该不执行任何操作`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadTopic("")
        advanceUntilIdle()

        // 只验证初始化时的调用，不应该有额外调用
        coVerify(exactly = 1) { topicRepository.getTopicHistory(testContactId, 10) }
    }

    // ==================== initWithContactId方法测试 ====================

    @Test
    fun `initWithContactId应该初始化新的联系人数据`() = runTest {
        val newContactId = "contact_789"
        every { getTopicUseCase.observe(newContactId) } returns flowOf(null)
        coEvery { topicRepository.getTopicHistory(newContactId, 10) } returns emptyList()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.initWithContactId(newContactId)
        advanceUntilIdle()

        coVerify { topicRepository.getTopicHistory(newContactId, 10) }
    }

    @Test
    fun `initWithContactId相同contactId应该不重复加载`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.initWithContactId(testContactId)
        advanceUntilIdle()

        // 只验证初始化时的调用
        coVerify(exactly = 1) { topicRepository.getTopicHistory(testContactId, 10) }
    }
}
