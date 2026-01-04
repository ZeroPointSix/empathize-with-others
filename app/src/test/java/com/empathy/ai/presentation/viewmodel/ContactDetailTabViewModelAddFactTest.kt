package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TimelineItem
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ContactDetailTabViewModel 添加事实功能测试
 *
 * 问题背景 (BUG-00006):
 *   添加事实后清空现有的AI对话记录和总结，导致数据丢失
 *   需要确保时间线项目的完整性
 *
 * 测试范围：
 * 1. 添加事实后保留现有对话记录（Conversation）
 * 2. 添加事实后保留现有AI总结（AiSummary）
 * 3. 添加的事实正确显示在时间线中（UserFact）
 * 4. facts列表和latestFact正确更新
 * 5. 添加成功后关闭对话框并显示成功消息
 * 6. 空key或空value不应该添加事实
 * 7. 时间线项目按时间倒序排列
 *
 * 业务规则 (PRD-00004/AC-006):
 *   - 用户可以手动向事实流中添加新的事实
 *   - 添加的事实立即显示在时间线中
 *   - 时间线按时间倒序排列，最新的在最前面
 *
 * 设计权衡:
 *   - 使用独立的facts列表和timelineItems分开管理
 *   - buildTimelineItems() 合并对话、总结、事实构建时间线
 *   - 时间线排序在构建时统一处理，避免UI层排序
 *
 * 任务追踪:
 *   - BUG-00006 添加事实后清空AI对话记录
 *   - TD-00014 标签画像V2功能
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailTabViewModelAddFactTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ContactDetailTabViewModel
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
    // TD-00014: 新增UseCase
    private lateinit var groupFactsByCategoryUseCase: GroupFactsByCategoryUseCase
    private lateinit var batchDeleteFactsUseCase: BatchDeleteFactsUseCase
    private lateinit var batchMoveFactsUseCase: BatchMoveFactsUseCase
    private lateinit var factSearchFilter: FactSearchFilter

    private val testContact = ContactProfile(
        id = "test_contact_1",
        name = "测试联系人",
        targetGoal = "测试目标",
        facts = emptyList()
    )

    private val testConversation = ConversationLog(
        id = 1L,
        contactId = "test_contact_1",
        userInput = "今天想约她出去吃饭",
        aiResponse = "建议用轻松的方式邀请",
        timestamp = System.currentTimeMillis() - 3600000,
        isSummarized = false
    )

    private val testSummary = DailySummary(
        id = 1L,
        contactId = "test_contact_1",
        summaryDate = "2025-12-14",
        content = "今天的互动整体氛围不错",
        keyEvents = listOf(KeyEvent(event = "讨论周末计划", importance = 7)),
        newFacts = emptyList(),
        updatedTags = emptyList(),
        relationshipScoreChange = 2,
        relationshipTrend = RelationshipTrend.IMPROVING
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

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
        // TD-00014: 新增UseCase
        groupFactsByCategoryUseCase = mockk()
        batchDeleteFactsUseCase = mockk()
        batchMoveFactsUseCase = mockk()
        factSearchFilter = mockk()

        // 设置默认mock行为
        coEvery { getContactUseCase(any()) } returns Result.success(testContact)
        coEvery { getBrainTagsUseCase(any()) } returns flowOf(emptyList())
        coEvery { saveProfileUseCase(any()) } returns Result.success(Unit)
        coEvery { conversationRepository.getConversationsByContact(any()) } returns Result.success(listOf(testConversation))
        coEvery { dailySummaryRepository.getSummariesByContact(any()) } returns Result.success(listOf(testSummary))
        coEvery { groupFactsByCategoryUseCase(any(), any()) } returns emptyList()
        coEvery { factSearchFilter.filter(any(), any()) } answers { firstArg() }

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

    /**
     * 测试：添加事实后应保留现有的对话记录
     *
     * 验证 BUG-00006 修复：添加事实不应清空AI对话
     */
    @Test
    fun `addFactToStream should preserve existing conversation timeline items`() = runTest {
        // Given: 加载联系人数据（包含对话记录）
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        // 验证初始状态有对话记录
        val initialState = viewModel.uiState.value
        val initialConversations = initialState.timelineItems.filterIsInstance<TimelineItem.Conversation>()
        assertTrue("初始状态应该有对话记录", initialConversations.isNotEmpty())

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("兴趣爱好", "喜欢摄影"))
        advanceUntilIdle()

        // Then: 对话记录应该仍然存在
        val finalState = viewModel.uiState.value
        val finalConversations = finalState.timelineItems.filterIsInstance<TimelineItem.Conversation>()
        assertEquals("添加事实后对话记录数量应该保持不变", initialConversations.size, finalConversations.size)
    }

    /**
     * 测试：添加事实后应保留现有的AI总结
     *
     * 验证 BUG-00006 修复：添加事实不应清空AI总结
     */
    @Test
    fun `addFactToStream should preserve existing AI summary timeline items`() = runTest {
        // Given: 加载联系人数据（包含AI总结）
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        // 验证初始状态有AI总结
        val initialState = viewModel.uiState.value
        val initialSummaries = initialState.timelineItems.filterIsInstance<TimelineItem.AiSummary>()
        assertTrue("初始状态应该有AI总结", initialSummaries.isNotEmpty())

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("性格特点", "外向开朗"))
        advanceUntilIdle()

        // Then: AI总结应该仍然存在
        val finalState = viewModel.uiState.value
        val finalSummaries = finalState.timelineItems.filterIsInstance<TimelineItem.AiSummary>()
        assertEquals("添加事实后AI总结数量应该保持不变", initialSummaries.size, finalSummaries.size)
    }

    /**
     * 测试：添加的事实应该显示在时间线中
     *
     * 验证 BUG-00006 修复：用户添加的事实应该可见
     */
    @Test
    fun `addFactToStream should add UserFact to timeline items`() = runTest {
        // Given: 加载联系人数据
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("工作信息", "软件工程师"))
        advanceUntilIdle()

        // Then: 时间线中应该包含新添加的UserFact
        val finalState = viewModel.uiState.value
        val userFacts = finalState.timelineItems.filterIsInstance<TimelineItem.UserFact>()
        assertTrue("时间线中应该包含用户添加的事实", userFacts.isNotEmpty())
        assertEquals("工作信息", userFacts[0].fact.key)
        assertEquals("软件工程师", userFacts[0].fact.value)
    }

    /**
     * 测试：添加事实后facts列表应该更新
     */
    @Test
    fun `addFactToStream should update facts list`() = runTest {
        // Given: 加载联系人数据
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        val initialFactsCount = viewModel.uiState.value.facts.size

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("家庭情况", "已婚"))
        advanceUntilIdle()

        // Then: facts列表应该增加一条
        val finalState = viewModel.uiState.value
        assertEquals("facts列表应该增加一条", initialFactsCount + 1, finalState.facts.size)
        assertTrue("新添加的事实应该在列表中", finalState.facts.any { it.key == "家庭情况" && it.value == "已婚" })
    }

    /**
     * 测试：添加事实后latestFact应该更新
     */
    @Test
    fun `addFactToStream should update latestFact`() = runTest {
        // Given: 加载联系人数据
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("重要日期", "生日是5月1日"))
        advanceUntilIdle()

        // Then: latestFact应该是新添加的事实
        val finalState = viewModel.uiState.value
        assertEquals("latestFact应该是新添加的事实", "重要日期", finalState.latestFact?.key)
        assertEquals("latestFact的值应该正确", "生日是5月1日", finalState.latestFact?.value)
    }

    /**
     * 测试：添加事实后对话框应该关闭
     */
    @Test
    fun `addFactToStream should close dialog after success`() = runTest {
        // Given: 加载联系人数据并打开对话框
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()
        viewModel.onEvent(ContactDetailUiEvent.ShowAddFactToStreamDialog)
        advanceUntilIdle()
        assertTrue("对话框应该打开", viewModel.uiState.value.showAddFactToStreamDialog)

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("禁忌话题", "不要提前任"))
        advanceUntilIdle()

        // Then: 对话框应该关闭
        val finalState = viewModel.uiState.value
        assertTrue("对话框应该关闭", !finalState.showAddFactToStreamDialog)
    }

    /**
     * 测试：添加事实后应该显示成功消息
     */
    @Test
    fun `addFactToStream should show success message`() = runTest {
        // Given: 加载联系人数据
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("沟通策略", "喜欢直接沟通"))
        advanceUntilIdle()

        // Then: 应该显示成功消息
        val finalState = viewModel.uiState.value
        assertEquals("应该显示成功消息", "事实已添加", finalState.successMessage)
    }

    /**
     * 测试：空key或空value不应该添加事实
     */
    @Test
    fun `addFactToStream should not add fact with empty key or value`() = runTest {
        // Given: 加载联系人数据
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        val initialFactsCount = viewModel.uiState.value.facts.size

        // When: 尝试添加空key的事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("", "某个值"))
        advanceUntilIdle()

        // Then: facts列表不应该变化
        assertEquals("空key不应该添加事实", initialFactsCount, viewModel.uiState.value.facts.size)

        // When: 尝试添加空value的事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("某个键", ""))
        advanceUntilIdle()

        // Then: facts列表不应该变化
        assertEquals("空value不应该添加事实", initialFactsCount, viewModel.uiState.value.facts.size)
    }

    /**
     * 测试：时间线项目应该按时间倒序排列
     */
    @Test
    fun `timeline items should be sorted by timestamp descending`() = runTest {
        // Given: 加载联系人数据
        viewModel.loadContactDetail("test_contact_1")
        advanceUntilIdle()

        // When: 添加新事实
        viewModel.onEvent(ContactDetailUiEvent.AddFactToStream("测试", "测试值"))
        advanceUntilIdle()

        // Then: 时间线项目应该按时间倒序排列
        val timelineItems = viewModel.uiState.value.timelineItems
        for (i in 0 until timelineItems.size - 1) {
            assertTrue(
                "时间线项目应该按时间倒序排列",
                timelineItems[i].timestamp >= timelineItems[i + 1].timestamp
            )
        }
    }
}
