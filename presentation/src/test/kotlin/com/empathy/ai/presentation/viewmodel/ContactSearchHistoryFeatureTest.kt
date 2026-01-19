package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.usecase.ClearContactSearchHistoryUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.GetContactSearchHistoryUseCase
import com.empathy.ai.domain.usecase.GetContactSortOptionUseCase
import com.empathy.ai.domain.usecase.SaveContactSearchQueryUseCase
import com.empathy.ai.domain.usecase.SaveContactSortOptionUseCase
import com.empathy.ai.domain.usecase.SortContactsUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 联系人搜索历史功能测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactSearchHistoryFeatureTest {

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
            targetGoal = "建立良好的合作关系"
        ),
        ContactProfile(
            id = "2",
            name = "李四",
            targetGoal = "成为好朋友"
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

    private fun createViewModel(): ContactListViewModel {
        coEvery { getAllContactsUseCase() } returns flowOf(sampleContacts)
        coEvery { getContactSortOptionUseCase() } returns Result.success(ContactSortOption.NAME)
        coEvery { saveContactSortOptionUseCase(any()) } returns Result.success(Unit)
        coEvery { getContactSearchHistoryUseCase() } returns Result.success(listOf("合作"))
        coEvery { saveContactSearchQueryUseCase(any()) } returns Result.success(listOf("张", "合作"))
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

    @Test
    fun `保存搜索历史应更新最近搜索列表`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.StartSearch)
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("张"))
        viewModel.onEvent(ContactListUiEvent.SaveSearchHistory)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.searchHistory.contains("张"))
        assertEquals("张", state.searchHistory.first())
    }

    @Test
    fun `清空搜索历史应置空列表`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.ClearSearchHistory)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.searchHistory.isEmpty())
    }
}
