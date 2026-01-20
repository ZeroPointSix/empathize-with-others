package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.usecase.ClearContactRecentHistoryUseCase
import com.empathy.ai.domain.usecase.ClearContactSearchHistoryUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.GetContactRecentHistoryUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRecentContactsFeatureTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAllContactsUseCase: GetAllContactsUseCase
    private lateinit var deleteContactUseCase: DeleteContactUseCase
    private lateinit var getContactSortOptionUseCase: GetContactSortOptionUseCase
    private lateinit var saveContactSortOptionUseCase: SaveContactSortOptionUseCase
    private lateinit var sortContactsUseCase: SortContactsUseCase
    private lateinit var getContactSearchHistoryUseCase: GetContactSearchHistoryUseCase
    private lateinit var saveContactSearchQueryUseCase: SaveContactSearchQueryUseCase
    private lateinit var clearContactSearchHistoryUseCase: ClearContactSearchHistoryUseCase
    private lateinit var getContactRecentHistoryUseCase: GetContactRecentHistoryUseCase
    private lateinit var clearContactRecentHistoryUseCase: ClearContactRecentHistoryUseCase

    private val sampleContacts = listOf(
        ContactProfile(id = "1", name = "张三", targetGoal = "建立良好的合作关系"),
        ContactProfile(id = "2", name = "李四", targetGoal = "成为好朋友")
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
        getContactRecentHistoryUseCase = mockk()
        clearContactRecentHistoryUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(recentIds: List<String> = emptyList()): ContactListViewModel {
        coEvery { getAllContactsUseCase() } returns flowOf(sampleContacts)
        coEvery { getContactSortOptionUseCase() } returns Result.success(ContactSortOption.NAME)
        coEvery { saveContactSortOptionUseCase(any()) } returns Result.success(Unit)
        coEvery { getContactSearchHistoryUseCase() } returns Result.success(emptyList())
        coEvery { saveContactSearchQueryUseCase(any()) } returns Result.success(emptyList())
        coEvery { clearContactSearchHistoryUseCase() } returns Result.success(Unit)
        coEvery { getContactRecentHistoryUseCase() } returns Result.success(recentIds)
        coEvery { clearContactRecentHistoryUseCase() } returns Result.success(Unit)

        return ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            getContactSortOptionUseCase,
            saveContactSortOptionUseCase,
            sortContactsUseCase,
            getContactSearchHistoryUseCase,
            saveContactSearchQueryUseCase,
            clearContactSearchHistoryUseCase,
            getContactRecentHistoryUseCase,
            clearContactRecentHistoryUseCase
        )
    }

    @Test
    fun `加载最近联系人应按历史顺序映射到列表`() = runTest {
        // Given
        val viewModel = createViewModel(recentIds = listOf("2", "1"))

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val recentNames = viewModel.uiState.value.recentContacts.map { it.name }
        assertEquals(listOf("李四", "张三"), recentNames)
    }

    @Test
    fun `清空最近联系人应清空列表`() = runTest {
        // Given
        val viewModel = createViewModel(recentIds = listOf("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ContactListUiEvent.ClearRecentContacts)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.recentContacts.isEmpty())
    }
}
