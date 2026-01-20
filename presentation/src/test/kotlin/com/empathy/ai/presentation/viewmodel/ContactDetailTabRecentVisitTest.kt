package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.BatchDeleteFactsUseCase
import com.empathy.ai.domain.usecase.BatchMoveFactsUseCase
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.EditBrainTagUseCase
import com.empathy.ai.domain.usecase.EditContactInfoUseCase
import com.empathy.ai.domain.usecase.EditConversationUseCase
import com.empathy.ai.domain.usecase.EditFactUseCase
import com.empathy.ai.domain.usecase.EditSummaryUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.GroupFactsByCategoryUseCase
import com.empathy.ai.domain.usecase.RecordContactVisitUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.domain.util.FactSearchFilter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailTabRecentVisitTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getContactUseCase: GetContactUseCase
    private lateinit var recordContactVisitUseCase: RecordContactVisitUseCase
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
    private lateinit var editBrainTagUseCase: EditBrainTagUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getContactUseCase = mockk()
        recordContactVisitUseCase = mockk()
        getBrainTagsUseCase = mockk()
        saveBrainTagUseCase = mockk(relaxed = true)
        saveProfileUseCase = mockk(relaxed = true)
        deleteBrainTagUseCase = mockk(relaxed = true)
        conversationRepository = mockk()
        dailySummaryRepository = mockk()
        editFactUseCase = mockk(relaxed = true)
        editConversationUseCase = mockk(relaxed = true)
        editSummaryUseCase = mockk(relaxed = true)
        editContactInfoUseCase = mockk(relaxed = true)
        groupFactsByCategoryUseCase = mockk(relaxed = true)
        batchDeleteFactsUseCase = mockk(relaxed = true)
        batchMoveFactsUseCase = mockk(relaxed = true)
        factSearchFilter = mockk(relaxed = true)
        editBrainTagUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `加载联系人详情后应记录最近访问`() = runTest {
        val contactId = "1"
        val contact = ContactProfile(id = contactId, name = "张三", targetGoal = "目标")

        coEvery { getContactUseCase(contactId) } returns Result.success(contact)
        coEvery { dailySummaryRepository.getSummariesByContact(contactId) } returns Result.success(emptyList())
        coEvery { conversationRepository.getConversationsByContact(contactId) } returns Result.success(emptyList())
        coEvery { getBrainTagsUseCase(contactId) } returns flowOf(emptyList())
        coEvery { recordContactVisitUseCase(contactId) } returns Result.success(emptyList())

        val viewModel = ContactDetailTabViewModel(
            getContactUseCase = getContactUseCase,
            recordContactVisitUseCase = recordContactVisitUseCase,
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
            factSearchFilter = factSearchFilter,
            editBrainTagUseCase = editBrainTagUseCase
        )

        viewModel.loadContactDetail(contactId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { recordContactVisitUseCase(contactId) }
    }
}
