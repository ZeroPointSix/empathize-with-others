package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ContactDetailViewModel 事实管理测试
 *
 * 测试场景：
 * 1. 添加多个相同类型的事实应该全部保留
 * 2. 添加完全相同的事实应该去重
 * 3. 添加不同类型的事实应该全部保留
 *
 * @see BUG-00017 三个UI交互问题系统性分析
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailViewModelFactTest {

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

        // 默认mock行为
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
     * 测试：添加多个相同类型的事实应该全部保留
     *
     * 场景：用户添加两个"性格特点"类型的事实
     * 预期：两个事实都应该存在于列表中
     */
    @Test
    fun `添加多个相同类型的事实应该全部保留`() = runTest {
        // Given - 加载新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 添加第一个性格特点
        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 添加第二个性格特点
        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", "热情"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - 两个事实都应该存在
        val facts = viewModel.uiState.value.facts
        assertEquals("应该有2个事实", 2, facts.size)
        assertTrue(
            "应该包含'开朗'",
            facts.any { it.key == "性格特点" && it.value == "开朗" }
        )
        assertTrue(
            "应该包含'热情'",
            facts.any { it.key == "性格特点" && it.value == "热情" }
        )
    }

    /**
     * 测试：添加完全相同的事实应该去重
     *
     * 场景：用户添加两次完全相同的事实（key和value都相同）
     * 预期：只保留一个事实
     */
    @Test
    fun `添加完全相同的事实应该去重`() = runTest {
        // Given - 加载新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 添加相同的事实两次
        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - 只保留一个
        val facts = viewModel.uiState.value.facts
        assertEquals("应该只有1个事实", 1, facts.size)
        assertEquals("性格特点", facts[0].key)
        assertEquals("开朗", facts[0].value)
    }

    /**
     * 测试：添加不同类型的事实应该全部保留
     *
     * 场景：用户添加不同类型的事实
     * 预期：所有事实都应该存在
     */
    @Test
    fun `添加不同类型的事实应该全部保留`() = runTest {
        // Given - 加载新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 添加不同类型的事实
        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddFact("兴趣爱好", "读书"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddFact("工作信息", "程序员"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - 三个事实都应该存在
        val facts = viewModel.uiState.value.facts
        assertEquals("应该有3个事实", 3, facts.size)
        assertTrue(facts.any { it.key == "性格特点" && it.value == "开朗" })
        assertTrue(facts.any { it.key == "兴趣爱好" && it.value == "读书" })
        assertTrue(facts.any { it.key == "工作信息" && it.value == "程序员" })
    }

    /**
     * 测试：添加空key或空value的事实应该被忽略
     *
     * 场景：用户尝试添加空内容的事实
     * 预期：事实不会被添加
     */
    @Test
    fun `添加空key或空value的事实应该被忽略`() = runTest {
        // Given - 加载新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 尝试添加空key的事实
        viewModel.onEvent(ContactDetailUiEvent.AddFact("", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 尝试添加空value的事实
        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", ""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 尝试添加空白key的事实
        viewModel.onEvent(ContactDetailUiEvent.AddFact("   ", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - 没有事实被添加
        val facts = viewModel.uiState.value.facts
        assertEquals("不应该有任何事实", 0, facts.size)
    }

    /**
     * 测试：添加事实后hasUnsavedChanges应该为true
     *
     * 场景：用户添加事实
     * 预期：hasUnsavedChanges标志应该为true
     */
    @Test
    fun `添加事实后hasUnsavedChanges应该为true`() = runTest {
        // Given - 加载新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        testDispatcher.scheduler.advanceUntilIdle()

        // 初始状态
        assertEquals(false, viewModel.uiState.value.hasUnsavedChanges)

        // When - 添加事实
        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - hasUnsavedChanges应该为true
        assertEquals(true, viewModel.uiState.value.hasUnsavedChanges)
    }

    /**
     * 测试：添加事实后newFactKey和newFactValue应该被清空
     *
     * 场景：用户添加事实
     * 预期：输入字段应该被清空
     */
    @Test
    fun `添加事实后输入字段应该被清空`() = runTest {
        // Given - 加载新建联系人并设置输入
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.UpdateNewFactKey("性格特点"))
        viewModel.onEvent(ContactDetailUiEvent.UpdateNewFactValue("开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 添加事实
        viewModel.onEvent(ContactDetailUiEvent.AddFact("性格特点", "开朗"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - 输入字段应该被清空
        assertEquals("", viewModel.uiState.value.newFactKey)
        assertEquals("", viewModel.uiState.value.newFactValue)
    }

    /**
     * 测试：连续添加多个事实的顺序应该保持
     *
     * 场景：用户连续添加多个事实
     * 预期：事实的顺序应该与添加顺序一致
     */
    @Test
    fun `连续添加多个事实的顺序应该保持`() = runTest {
        // Given - 加载新建联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 按顺序添加事实
        viewModel.onEvent(ContactDetailUiEvent.AddFact("第一", "A"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddFact("第二", "B"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddFact("第三", "C"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - 顺序应该保持
        val facts = viewModel.uiState.value.facts
        assertEquals(3, facts.size)
        assertEquals("第一", facts[0].key)
        assertEquals("第二", facts[1].key)
        assertEquals("第三", facts[2].key)
    }
}
