package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.DetailTab
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ContactDetailTabViewModel 单元测试
 *
 * 测试内容：
 * - 数据加载
 * - 标签页切换
 * - 视图模式切换
 * - 筛选条件切换
 * - 标签确认/驳回
 * - 错误处理
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailTabViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ContactDetailTabViewModel
    private lateinit var getContactUseCase: GetContactUseCase
    private lateinit var getBrainTagsUseCase: GetBrainTagsUseCase
    private lateinit var saveBrainTagUseCase: SaveBrainTagUseCase
    private lateinit var deleteBrainTagUseCase: DeleteBrainTagUseCase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var dailySummaryRepository: DailySummaryRepository

    private val sampleContact = ContactProfile(
        id = "contact_1",
        name = "测试联系人",
        targetGoal = "测试目标",
        contextDepth = 10,
        relationshipScore = 75,
        facts = listOf(
            Fact(
                key = "兴趣",
                value = "喜欢美食",
                source = FactSource.MANUAL,
                timestamp = System.currentTimeMillis()
            ),
            Fact(
                key = "禁忌",
                value = "不喜欢加班话题",
                source = FactSource.AI_INFERRED,
                timestamp = System.currentTimeMillis()
            )
        ),
        lastInteractionDate = null,
        avatarUrl = null
    )

    private val sampleConversations = listOf(
        ConversationLog(
            id = 1,
            contactId = "contact_1",
            userInput = "今天想约她吃饭",
            aiResponse = "建议用轻松的方式邀请",
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )
    )

    private val sampleSummaries = listOf(
        DailySummary(
            id = 1,
            contactId = "contact_1",
            summaryDate = "2025-12-14",
            content = "今天的互动很愉快",
            keyEvents = listOf(KeyEvent(event = "讨论周末计划", importance = 7)),
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = 2,
            relationshipTrend = RelationshipTrend.IMPROVING
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getContactUseCase = mockk()
        getBrainTagsUseCase = mockk()
        saveBrainTagUseCase = mockk()
        deleteBrainTagUseCase = mockk()
        conversationRepository = mockk()
        dailySummaryRepository = mockk()

        // 默认mock返回
        coEvery { getContactUseCase(any()) } returns Result.success(sampleContact)
        coEvery { getBrainTagsUseCase(any()) } returns flowOf(emptyList())
        coEvery { conversationRepository.getConversationsByContactId(any()) } returns sampleConversations
        coEvery { dailySummaryRepository.getSummariesByContact(any()) } returns Result.success(sampleSummaries)

        viewModel = ContactDetailTabViewModel(
            getContactUseCase = getContactUseCase,
            getBrainTagsUseCase = getBrainTagsUseCase,
            saveBrainTagUseCase = saveBrainTagUseCase,
            deleteBrainTagUseCase = deleteBrainTagUseCase,
            conversationRepository = conversationRepository,
            dailySummaryRepository = dailySummaryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== 数据加载测试 ==========

    @Test
    fun `loadContactDetail should update state with contact data`() = runTest {
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.contact)
        assertEquals("测试联系人", state.contact?.name)
        assertEquals(75, state.contact?.relationshipScore)
    }

    @Test
    fun `loadContactDetail should load conversations and summaries`() = runTest {
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.conversationCount)
        assertEquals(1, state.summaryCount)
    }

    @Test
    fun `loadContactDetail should build timeline items`() = runTest {
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.timelineItems.isNotEmpty())
    }

    @Test
    fun `loadContactDetail should calculate days since first met`() = runTest {
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.daysSinceFirstMet >= 29) // 约30天
    }

    @Test
    fun `loadContactDetail should handle error`() = runTest {
        coEvery { getContactUseCase(any()) } returns Result.failure(Exception("加载失败"))

        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("加载失败"))
    }

    // ========== 标签页切换测试 ==========

    @Test
    fun `switchTab should update current tab`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.SwitchTab(DetailTab.FactStream))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DetailTab.FactStream, viewModel.uiState.value.currentTab)
    }

    @Test
    fun `switchTab to Persona should work`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.SwitchTab(DetailTab.Persona))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DetailTab.Persona, viewModel.uiState.value.currentTab)
    }

    @Test
    fun `switchTab to DataVault should work`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.SwitchTab(DetailTab.DataVault))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DetailTab.DataVault, viewModel.uiState.value.currentTab)
    }

    // ========== 视图模式切换测试 ==========

    @Test
    fun `switchViewMode should update view mode`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.SwitchViewMode(ViewMode.List))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ViewMode.List, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `switchViewMode back to Timeline should work`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.SwitchViewMode(ViewMode.List))
        viewModel.onEvent(ContactDetailUiEvent.SwitchViewMode(ViewMode.Timeline))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ViewMode.Timeline, viewModel.uiState.value.viewMode)
    }

    // ========== 筛选条件测试 ==========

    @Test
    fun `toggleFilter should add filter`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.ToggleFilter(FilterType.AI_SUMMARY))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedFilters.contains(FilterType.AI_SUMMARY))
    }

    @Test
    fun `toggleFilter should remove filter when already selected`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.ToggleFilter(FilterType.AI_SUMMARY))
        viewModel.onEvent(ContactDetailUiEvent.ToggleFilter(FilterType.AI_SUMMARY))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.selectedFilters.contains(FilterType.AI_SUMMARY))
    }

    @Test
    fun `toggleFilter should support multiple filters`() = runTest {
        viewModel.onEvent(ContactDetailUiEvent.ToggleFilter(FilterType.AI_SUMMARY))
        viewModel.onEvent(ContactDetailUiEvent.ToggleFilter(FilterType.CONFLICT))
        testDispatcher.scheduler.advanceUntilIdle()

        val filters = viewModel.uiState.value.selectedFilters
        assertTrue(filters.contains(FilterType.AI_SUMMARY))
        assertTrue(filters.contains(FilterType.CONFLICT))
    }

    // ========== 标签确认/驳回测试 ==========

    @Test
    fun `confirmTag should update fact source to MANUAL`() = runTest {
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.ConfirmTag(2L)) // timestamp = 2
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val confirmedFact = state.facts.find { it.timestamp == 2L }
        assertEquals(FactSource.MANUAL, confirmedFact?.source)
        assertNotNull(state.successMessage)
    }

    @Test
    fun `rejectTag should remove fact from list`() = runTest {
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val initialCount = viewModel.uiState.value.facts.size
        viewModel.onEvent(ContactDetailUiEvent.RejectTag(2L))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(initialCount - 1, state.facts.size)
        assertNull(state.facts.find { it.timestamp == 2L })
        assertNotNull(state.successMessage)
    }

    // ========== 错误处理测试 ==========

    @Test
    fun `clearError should set error to null`() = runTest {
        coEvery { getContactUseCase(any()) } returns Result.failure(Exception("错误"))
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.ClearError)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clearSuccessMessage should set successMessage to null`() = runTest {
        viewModel.loadContactDetail("contact_1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactDetailUiEvent.ConfirmTag(2L))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.ClearSuccessMessage)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.successMessage)
    }

    // ========== 初始状态测试 ==========

    @Test
    fun `initial state should have default values`() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.contact)
        assertNull(state.error)
        assertEquals(DetailTab.Overview, state.currentTab)
        assertEquals(ViewMode.Timeline, state.viewMode)
        assertTrue(state.selectedFilters.isEmpty())
    }
}
