package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetContactSortOptionUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.SaveContactSortOptionUseCase
import com.empathy.ai.domain.usecase.SortContactsUseCase
import com.empathy.ai.domain.usecase.GetContactSearchHistoryUseCase
import com.empathy.ai.domain.usecase.SaveContactSearchQueryUseCase
import com.empathy.ai.domain.usecase.ClearContactSearchHistoryUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactListUiEvent
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00063：联系人搜索功能测试
 * 
 * 验证联系人搜索功能的完整性和正确性
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00063ContactSearchTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getAllContactsUseCase: GetAllContactsUseCase
    private lateinit var deleteContactUseCase: DeleteContactUseCase
    private lateinit var getContactSortOptionUseCase: GetContactSortOptionUseCase
    private lateinit var saveContactSortOptionUseCase: SaveContactSortOptionUseCase
    private lateinit var sortContactsUseCase: SortContactsUseCase
    private lateinit var getContactSearchHistoryUseCase: GetContactSearchHistoryUseCase
    private lateinit var saveContactSearchQueryUseCase: SaveContactSearchQueryUseCase
    private lateinit var clearContactSearchHistoryUseCase: ClearContactSearchHistoryUseCase

    private val sampleContacts = listOf(
        ContactProfile(
            id = "1",
            name = "张三",
            targetGoal = "建立良好的合作关系",
            contextDepth = 10,
            facts = listOf(
                Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(key = "爱好", value = "摄影", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL)
            )
        ),
        ContactProfile(
            id = "2",
            name = "李四",
            targetGoal = "成为好朋友",
            contextDepth = 15,
            facts = listOf(
                Fact(key = "职业", value = "设计师", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL)
            )
        ),
        ContactProfile(
            id = "3",
            name = "王五",
            targetGoal = "保持联系",
            contextDepth = 8,
            facts = emptyList()
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAllContactsUseCase = mockk()
        deleteContactUseCase = mockk()
        getContactSortOptionUseCase = mockk()
        saveContactSortOptionUseCase = mockk()
        sortContactsUseCase = SortContactsUseCase()
        getContactSearchHistoryUseCase = mockk()
        saveContactSearchQueryUseCase = mockk()
        clearContactSearchHistoryUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(contacts: List<ContactProfile> = sampleContacts): ContactListViewModel {
        coEvery { getAllContactsUseCase() } returns flowOf(contacts)
        coEvery { getContactSortOptionUseCase() } returns Result.success(ContactSortOption.NAME)
        coEvery { saveContactSortOptionUseCase(any()) } returns Result.success(Unit)
        coEvery { getContactSearchHistoryUseCase() } returns Result.success(emptyList())
        coEvery { saveContactSearchQueryUseCase(any()) } returns Result.success(emptyList())
        coEvery { clearContactSearchHistoryUseCase() } returns Result.success(Unit)
        return ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            getContactSortOptionUseCase,
            saveContactSortOptionUseCase,
            sortContactsUseCase,
            getContactSearchHistoryUseCase,
            saveContactSearchQueryUseCase,
            clearContactSearchHistoryUseCase
        )
    }

    // ==================== TC-001: 点击搜索图标展开搜索框 ====================

    @Test
    fun `TC-001 StartSearch事件应该设置isSearching为true`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("isSearching应该为true", state.isSearching)
    }

    // ==================== TC-002: 输入搜索词过滤联系人 ====================

    @Test
    fun `TC-002 UpdateSearchQuery事件应该过滤联系人列表`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("张"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("搜索结果应该只有1个", 1, state.searchResults.size)
        assertEquals("搜索结果应该是张三", "张三", state.searchResults[0].name)
    }

    // ==================== TC-003: 按目标搜索 ====================

    @Test
    fun `TC-003 搜索应该匹配targetGoal字段`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("合作"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("搜索结果应该只有1个", 1, state.searchResults.size)
        assertEquals("搜索结果应该是张三", "张三", state.searchResults[0].name)
    }

    // ==================== TC-004: 按事实搜索 ====================

    @Test
    fun `TC-004 搜索应该匹配facts字段`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("产品经理"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("搜索结果应该只有1个", 1, state.searchResults.size)
        assertEquals("搜索结果应该是张三", "张三", state.searchResults[0].name)
    }

    // ==================== TC-005: 清空搜索词 ====================

    @Test
    fun `TC-005 清空搜索词应该清除搜索结果`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("张"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery(""))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("搜索结果应该为空", state.searchResults.isEmpty())
    }

    // ==================== TC-006: 取消搜索 ====================

    @Test
    fun `TC-006 CancelSearch事件应该退出搜索模式`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("张"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.CancelSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isSearching应该为false", state.isSearching)
        assertEquals("searchQuery应该为空", "", state.searchQuery)
        assertTrue("searchResults应该为空", state.searchResults.isEmpty())
    }

    // ==================== TC-007: 搜索无结果 ====================

    @Test
    fun `TC-007 搜索无匹配时searchResults应该为空`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("不存在的联系人"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("搜索结果应该为空", state.searchResults.isEmpty())
        assertTrue("isSearching应该为true", state.isSearching)
    }

    // ==================== TC-008: 搜索不区分大小写 ====================

    @Test
    fun `TC-008 搜索应该不区分大小写`() = runTest {
        // Given
        val contacts = listOf(
            ContactProfile(id = "1", name = "Test User", targetGoal = "合作")
        )
        val viewModel = createViewModel(contacts)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("test"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("搜索结果应该有1个", 1, state.searchResults.size)
    }

    // ==================== 边界测试 ====================

    @Test
    fun `TC-EDGE-001 搜索特殊字符不应该崩溃`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When & Then (不应该抛出异常)
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("@#\$%^&*()"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("搜索结果应该为空", state.searchResults.isEmpty())
    }

    @Test
    fun `TC-EDGE-002 搜索超长字符串不应该崩溃`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()
        val longString = "a".repeat(1000)

        // When & Then (不应该抛出异常)
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery(longString))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("搜索结果应该为空", state.searchResults.isEmpty())
    }

    @Test
    fun `TC-EDGE-003 联系人列表为空时搜索应该返回空结果`() = runTest {
        // Given
        val viewModel = createViewModel(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("张三"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("搜索结果应该为空", state.searchResults.isEmpty())
    }

    @Test
    fun `TC-EDGE-004 搜索事实的key字段`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 搜索事实的key
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("职业"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("搜索结果应该有2个（张三和李四都有职业字段）", 2, state.searchResults.size)
    }

    @Test
    fun `TC-EDGE-005 多次搜索应该正确更新结果`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 第一次搜索
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("张"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("第一次搜索应该有1个结果", 1, viewModel.uiState.value.searchResults.size)

        // When - 第二次搜索
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("李"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("第二次搜索应该有1个结果", 1, viewModel.uiState.value.searchResults.size)
        assertEquals("第二次搜索结果应该是李四", "李四", viewModel.uiState.value.searchResults[0].name)
    }
}
