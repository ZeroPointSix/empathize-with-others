package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.GetContactSortOptionUseCase
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
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ContactListViewModel 排序功能测试
 *
 * 测试范围：
 * 1. 排序选项的加载和应用
 * 2. 排序选项的更新和持久化
 * 3. 搜索结果中的排序
 * 4. 错误处理场景
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactListSortFeatureTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAllContactsUseCase: GetAllContactsUseCase
    private lateinit var deleteContactUseCase: DeleteContactUseCase
    private lateinit var getContactSortOptionUseCase: GetContactSortOptionUseCase
    private lateinit var saveContactSortOptionUseCase: SaveContactSortOptionUseCase
    private lateinit var sortContactsUseCase: SortContactsUseCase

    private val sampleContacts = listOf(
        ContactProfile(
            id = "1",
            name = "张三",
            targetGoal = "目标A",
            contextDepth = 10,
            relationshipScore = 50,
            lastInteractionDate = "2026-01-10",
            facts = emptyList()
        ),
        ContactProfile(
            id = "2",
            name = "李四",
            targetGoal = "目标B",
            contextDepth = 20,
            relationshipScore = 80,
            lastInteractionDate = "2026-01-12",
            facts = emptyList()
        ),
        ContactProfile(
            id = "3",
            name = "王五",
            targetGoal = "目标C",
            contextDepth = 15,
            relationshipScore = 30,
            lastInteractionDate = "2026-01-08",
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        contacts: List<ContactProfile> = sampleContacts,
        initialSortOption: ContactSortOption = ContactSortOption.NAME
    ): ContactListViewModel {
        coEvery { getAllContactsUseCase() } returns flowOf(contacts)
        coEvery { getContactSortOptionUseCase() } returns Result.success(initialSortOption)
        coEvery { saveContactSortOptionUseCase(any()) } returns Result.success(Unit)

        return ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            getContactSortOptionUseCase,
            saveContactSortOptionUseCase,
            sortContactsUseCase
        )
    }

    // ==================== 测试1: 初始化时加载排序选项 ====================

    @Test
    fun `初始化时应该加载排序选项并按该选项排序联系人`() = runTest {
        // Given: 排序选项为 LAST_INTERACTION
        coEvery { getContactSortOptionUseCase() } returns Result.success(ContactSortOption.LAST_INTERACTION)

        // When: 创建 ViewModel
        val viewModel = createViewModel(initialSortOption = ContactSortOption.LAST_INTERACTION)

        // Then: 应该按最近互动时间降序排序
        testDispatcher.scheduler.advanceUntilIdle()
        val uiState = viewModel.uiState.value
        assertTrue(uiState.hasLoadedContacts, "应该加载完成联系人")
        assertEquals(ContactSortOption.LAST_INTERACTION, uiState.sortOption)

        // 验证排序结果：李四(2026-01-12) > 张三(2026-01-10) > 王五(2026-01-08)
        val expectedOrder = listOf("李四", "张三", "王五")
        assertEquals(expectedOrder, uiState.filteredContacts.map { it.name })
    }

    @Test
    fun `初始化时排序选项加载失败应使用默认值并继续加载联系人`() = runTest {
        // Given: 排序选项加载失败
        coEvery { getContactSortOptionUseCase() } returns Result.failure(Exception("加载失败"))

        // When: 创建 ViewModel
        val viewModel = createViewModel()

        // Then: 应该使用默认值 NAME 并继续加载联系人
        testDispatcher.scheduler.advanceUntilIdle()
        val uiState = viewModel.uiState.value
        assertTrue(uiState.hasLoadedContacts, "应该加载完成联系人")
        assertEquals(ContactSortOption.NAME, uiState.sortOption)

        // 验证排序结果：按姓名升序
        val expectedOrder = listOf("张三", "李四", "王五")
        assertEquals(expectedOrder, uiState.filteredContacts.map { it.name })
    }

    // ==================== 测试2: 更新排序选项 ====================

    @Test
    fun `更新排序选项应该重新排序联系人列表`() = runTest {
        // Given: 初始排序选项为 NAME
        val viewModel = createViewModel(initialSortOption = ContactSortOption.NAME)
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 切换到 RELATIONSHIP_SCORE 排序
        viewModel.onEvent(ContactListUiEvent.UpdateSortOption(ContactSortOption.RELATIONSHIP_SCORE))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 联系人应该按关系分数降序排序
        val uiState = viewModel.uiState.value
        assertEquals(ContactSortOption.RELATIONSHIP_SCORE, uiState.sortOption)

        // 验证排序结果：李四(80) > 王五(30) > 张三(50应该是50，但顺序应该是80>50>30)
        val expectedOrder = listOf("李四", "张三", "王五")
        assertEquals(expectedOrder, uiState.filteredContacts.map { it.name })
    }

    @Test
    fun `更新排序选项应该保存到持久化存储`() = runTest {
        // Given: 初始排序选项为 NAME
        val viewModel = createViewModel(initialSortOption = ContactSortOption.NAME)
        testDispatcher.scheduler.advanceUntilIdle()

        var savedOption: ContactSortOption? = null
        coEvery { saveContactSortOptionUseCase(any()) } answers {
            savedOption = firstArg()
            Result.success(Unit)
        }

        // When: 切换到 LAST_INTERACTION 排序
        viewModel.onEvent(ContactListUiEvent.UpdateSortOption(ContactSortOption.LAST_INTERACTION))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 应该保存新的排序选项
        assertEquals(ContactSortOption.LAST_INTERACTION, savedOption)
    }

    @Test
    fun `保存排序选项失败应该显示错误提示`() = runTest {
        // Given: 保存排序选项失败
        // 注意：需要在创建 ViewModel 之前设置 mock，或者创建 ViewModel 后重新设置
        // 由于 createViewModel 中已经设置了成功返回，我们需要创建一个不设置保存的场景

        // 重新创建 ViewModel，不设置默认的保存成功行为
        coEvery { getAllContactsUseCase() } returns flowOf(sampleContacts)
        coEvery { getContactSortOptionUseCase() } returns Result.success(ContactSortOption.NAME)
        // 关键：不设置 saveContactSortOptionUseCase 的默认成功行为，而是设置为失败
        coEvery { saveContactSortOptionUseCase(any()) } returns Result.failure(Exception("保存失败"))

        val viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            getContactSortOptionUseCase,
            saveContactSortOptionUseCase,
            sortContactsUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 切换排序选项
        viewModel.onEvent(ContactListUiEvent.UpdateSortOption(ContactSortOption.LAST_INTERACTION))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 应该显示错误提示
        val uiState = viewModel.uiState.value
        // 错误消息应该包含失败信息
        assertTrue(uiState.error?.contains("保存排序偏好失败") == true, "错误消息应该包含'保存排序偏好失败'")
        assertTrue(uiState.error?.contains("保存失败") == true, "错误消息应该包含原始错误信息")
    }

    // ==================== 测试3: 搜索结果中的排序 ====================

    @Test
    fun `搜索结果应该按当前排序选项排序`() = runTest {
        // Given: 初始排序选项为 LAST_INTERACTION
        val viewModel = createViewModel(initialSortOption = ContactSortOption.LAST_INTERACTION)
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 执行搜索
        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("张"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 搜索结果应该按最近互动时间排序
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isSearching, "应该处于搜索模式")
        assertEquals(1, uiState.searchResults.size)
        assertEquals("张三", uiState.searchResults[0].name)
    }

    @Test
    fun `搜索模式切换排序选项应该重新排序搜索结果`() = runTest {
        // Given: 处于搜索模式，初始按 NAME 排序
        val viewModel = createViewModel(initialSortOption = ContactSortOption.NAME)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ContactListUiEvent.UpdateSearchQuery("目标"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 切换到 RELATIONSHIP_SCORE 排序
        viewModel.onEvent(ContactListUiEvent.UpdateSortOption(ContactSortOption.RELATIONSHIP_SCORE))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 搜索结果应该按新的排序选项排序
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isSearching, "应该仍处于搜索模式")

        // 所有联系人都匹配"目标"，应该按关系分数排序
        val expectedOrder = listOf("李四", "张三", "王五")
        assertEquals(expectedOrder, uiState.searchResults.map { it.name })
    }

    // ==================== 测试4: 排序选项的持久化 ====================

    @Test
    fun `排序选项应该在所有排序方式中正确工作`() = runTest {
        val viewModel = createViewModel(initialSortOption = ContactSortOption.NAME)
        testDispatcher.scheduler.advanceUntilIdle()

        // 测试 NAME 排序
        viewModel.onEvent(ContactListUiEvent.UpdateSortOption(ContactSortOption.NAME))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("张三", "李四", "王五"), viewModel.uiState.value.filteredContacts.map { it.name })

        // 测试 LAST_INTERACTION 排序
        viewModel.onEvent(ContactListUiEvent.UpdateSortOption(ContactSortOption.LAST_INTERACTION))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("李四", "张三", "王五"), viewModel.uiState.value.filteredContacts.map { it.name })

        // 测试 RELATIONSHIP_SCORE 排序
        viewModel.onEvent(ContactListUiEvent.UpdateSortOption(ContactSortOption.RELATIONSHIP_SCORE))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("李四", "张三", "王五"), viewModel.uiState.value.filteredContacts.map { it.name })
    }

    // ==================== 测试5: 边界情况 ====================

    @Test
    fun `空联系人列表排序应该正常工作`() = runTest {
        // Given: 空联系人列表
        coEvery { getAllContactsUseCase() } returns flowOf(emptyList())
        coEvery { getContactSortOptionUseCase() } returns Result.success(ContactSortOption.NAME)

        // When: 创建 ViewModel
        val viewModel = createViewModel(contacts = emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 应该正常完成加载
        val uiState = viewModel.uiState.value
        assertTrue(uiState.hasLoadedContacts)
        assertTrue(uiState.isEmptyState)
        assertEquals(0, uiState.filteredContacts.size)
    }

    @Test
    fun `单个联系人排序应该正常工作`() = runTest {
        // Given: 单个联系人
        val singleContact = sampleContacts.take(1)
        coEvery { getAllContactsUseCase() } returns flowOf(singleContact)

        // When: 创建 ViewModel
        val viewModel = createViewModel(contacts = singleContact)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 应该正常排序
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.filteredContacts.size)
        assertEquals("张三", uiState.filteredContacts[0].name)
    }

    @Test
    fun `lastInteractionDate 为 null 的联系人应该排在最后`() = runTest {
        // Given: 包含 null 日期的联系人列表
        val contactsWithNull = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "目标A",
                contextDepth = 10,
                relationshipScore = 50,
                lastInteractionDate = "2026-01-10",
                facts = emptyList()
            ),
            ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "目标B",
                contextDepth = 20,
                relationshipScore = 80,
                lastInteractionDate = null, // null 日期
                facts = emptyList()
            )
        )
        coEvery { getAllContactsUseCase() } returns flowOf(contactsWithNull)

        // When: 按最近互动排序
        val viewModel = createViewModel(
            contacts = contactsWithNull,
            initialSortOption = ContactSortOption.LAST_INTERACTION
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: null 日期的联系人应该排在最后
        val uiState = viewModel.uiState.value
        assertEquals(listOf("张三", "李四"), uiState.filteredContacts.map { it.name })
    }

    @Test
    fun `相同分数的联系人应该按姓名排序`() = runTest {
        // Given: 相同关系分数的联系人
        val contactsWithSameScore = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "目标A",
                contextDepth = 10,
                relationshipScore = 50,
                lastInteractionDate = "2026-01-10",
                facts = emptyList()
            ),
            ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "目标B",
                contextDepth = 20,
                relationshipScore = 50, // 相同分数
                lastInteractionDate = "2026-01-12",
                facts = emptyList()
            )
        )
        coEvery { getAllContactsUseCase() } returns flowOf(contactsWithSameScore)

        // When: 按关系分数排序
        val viewModel = createViewModel(
            contacts = contactsWithSameScore,
            initialSortOption = ContactSortOption.RELATIONSHIP_SCORE
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 相同分数应该按姓名排序
        val uiState = viewModel.uiState.value
        assertEquals(listOf("张三", "李四"), uiState.filteredContacts.map { it.name })
    }
}
