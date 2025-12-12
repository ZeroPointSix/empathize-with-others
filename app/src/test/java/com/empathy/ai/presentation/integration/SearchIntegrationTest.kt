package com.empathy.ai.presentation.integration

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactListUiEvent
import com.empathy.ai.presentation.viewmodel.ContactListViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * 搜索完整流程集成测试
 *
 * 测试从点击搜索到显示结果的完整用户流程
 * 验证需求: 2.1, 2.2, 2.6
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var getAllContactsUseCase: GetAllContactsUseCase
    private lateinit var deleteContactUseCase: DeleteContactUseCase
    private lateinit var saveProfileUseCase: SaveProfileUseCase

    private lateinit var viewModel: ContactListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 创建 mock 对象
        getAllContactsUseCase = mockk()
        deleteContactUseCase = mockk(relaxed = true)
        saveProfileUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 集成测试 - 搜索完整流程（成功场景）
     *
     * 测试步骤：
     * 1. 用户进入联系人列表页
     * 2. 系统加载并显示所有联系人
     * 3. 用户点击搜索图标
     * 4. 系统显示搜索输入框
     * 5. 用户输入搜索文本
     * 6. 系统实时过滤联系人列表
     * 7. 系统显示匹配的联系人
     * 8. 系统显示结果计数
     */
    @Test
    fun `完整流程 - 成功搜索联系人`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的工作关系",
                contextDepth = 10,
                facts = mapOf("职位" to "产品经理", "爱好" to "阅读")
            ),
            ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "提升沟通效率",
                contextDepth = 10,
                facts = mapOf("职位" to "工程师", "爱好" to "游泳")
            ),
            ContactProfile(
                id = "3",
                name = "王五",
                targetGoal = "保持友好关系",
                contextDepth = 10,
                facts = mapOf("职位" to "设计师", "爱好" to "绘画")
            )
        )

        // Mock 联系人加载
        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        // Step 1 & 2: 创建 ViewModel，加载联系人列表
        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 验证所有联系人已加载
        assertEquals(contacts.size, viewModel.uiState.value.contacts.size)
        assertEquals(contacts.size, viewModel.uiState.value.displayContacts.size)

        // 验证初始状态：搜索未激活
        assertFalse(viewModel.uiState.value.searchState.isActive, "初始状态搜索应该未激活")

        // Step 3 & 4: 点击搜索图标，显示搜索输入框
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true))
        advanceUntilIdle()

        // 验证搜索已激活
        assertTrue(viewModel.uiState.value.searchState.isActive, "搜索应该激活")
        assertEquals("", viewModel.uiState.value.searchState.query, "初始查询应该为空")

        // Step 5: 输入搜索文本（搜索"张三"）
        val searchQuery = "张三"
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = searchQuery))
        
        // 等待防抖延迟
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        // Step 6 & 7: 验证搜索结果
        val searchState = viewModel.uiState.value.searchState
        assertEquals(searchQuery, searchState.query, "查询应该被设置")
        assertTrue(searchState.hasResults, "应该有搜索结果")

        val displayContacts = viewModel.uiState.value.displayContacts
        assertTrue(displayContacts.any { it.name.contains(searchQuery) },
            "显示的联系人应该包含匹配的联系人")

        // Step 8: 验证结果计数
        assertTrue(searchState.resultCount > 0, "结果计数应该大于0")
        assertEquals(displayContacts.size, searchState.resultCount, "结果计数应该与显示的联系人数量一致")
    }

    /**
     * 集成测试 - 搜索多个字段
     *
     * 测试搜索应该匹配：
     * 1. 联系人姓名
     * 2. 联系人目标
     * 3. 联系人事实信息
     */
    @Test
    fun `完整流程 - 搜索匹配多个字段`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的工作关系",
                contextDepth = 10,
                facts = mapOf("职位" to "产品经理")
            ),
            ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "提升沟通效率",
                contextDepth = 10,
                facts = mapOf("职位" to "工程师")
            ),
            ContactProfile(
                id = "3",
                name = "王五",
                targetGoal = "保持友好关系",
                contextDepth = 10,
                facts = mapOf("职位" to "产品经理")
            )
        )

        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 测试场景1：搜索姓名
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "张三"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        var displayContacts = viewModel.uiState.value.displayContacts
        assertTrue(displayContacts.any { it.name == "张三" }, "应该找到姓名匹配的联系人")

        // 测试场景2：搜索目标
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "沟通效率"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        displayContacts = viewModel.uiState.value.displayContacts
        assertTrue(displayContacts.any { it.targetGoal.contains("沟通效率") },
            "应该找到目标匹配的联系人")

        // 测试场景3：搜索事实信息
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "产品经理"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        displayContacts = viewModel.uiState.value.displayContacts
        assertTrue(displayContacts.size >= 2, "应该找到至少2个职位为产品经理的联系人")
        assertTrue(displayContacts.all { contact ->
            contact.facts.values.any { it.contains("产品经理") }
        }, "所有结果都应该包含'产品经理'")
    }

    /**
     * 集成测试 - 搜索空结果
     *
     * 测试当搜索没有匹配结果时，系统应该：
     * 1. 显示空结果提示
     * 2. 不显示任何联系人
     */
    @Test
    fun `完整流程 - 搜索无结果显示空状态`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的工作关系",
                contextDepth = 10,
                facts = mapOf("职位" to "产品经理")
            ),
            ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "提升沟通效率",
                contextDepth = 10,
                facts = mapOf("职位" to "工程师")
            )
        )

        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 激活搜索
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true))
        advanceUntilIdle()

        // 搜索一个不存在的内容
        val unmatchableQuery = "这是一个不存在的搜索内容XYZ123"
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = unmatchableQuery))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        // 验证搜索状态
        val searchState = viewModel.uiState.value.searchState
        assertEquals(unmatchableQuery, searchState.query, "查询应该被设置")
        assertFalse(searchState.hasResults, "应该没有搜索结果")
        assertEquals(0, searchState.resultCount, "结果计数应该为0")

        // 验证显示空状态
        assertTrue(searchState.showEmptyState, "应该显示空状态提示")

        // 验证没有显示联系人
        val displayContacts = viewModel.uiState.value.displayContacts
        assertEquals(0, displayContacts.size, "不应该显示任何联系人")
    }

    /**
     * 集成测试 - 清空搜索
     *
     * 测试当用户清空搜索文本时，系统应该：
     * 1. 显示所有联系人
     * 2. 重置搜索状态
     */
    @Test
    fun `完整流程 - 清空搜索恢复所有联系人`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的工作关系",
                contextDepth = 10,
                facts = emptyMap()
            ),
            ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "提升沟通效率",
                contextDepth = 10,
                facts = emptyMap()
            ),
            ContactProfile(
                id = "3",
                name = "王五",
                targetGoal = "保持友好关系",
                contextDepth = 10,
                facts = emptyMap()
            )
        )

        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 激活搜索并输入查询
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "张三"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        // 验证搜索结果被过滤
        assertTrue(viewModel.uiState.value.displayContacts.size < contacts.size,
            "搜索应该过滤联系人")

        // 清空搜索（设置空查询）
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = ""))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        // 验证显示所有联系人
        assertEquals(contacts.size, viewModel.uiState.value.displayContacts.size,
            "清空搜索后应该显示所有联系人")
    }

    /**
     * 集成测试 - 关闭搜索
     *
     * 测试当用户关闭搜索模式时，系统应该：
     * 1. 隐藏搜索输入框
     * 2. 清空搜索查询
     * 3. 显示所有联系人
     */
    @Test
    fun `完整流程 - 关闭搜索恢复正常状态`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的工作关系",
                contextDepth = 10,
                facts = emptyMap()
            ),
            ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "提升沟通效率",
                contextDepth = 10,
                facts = emptyMap()
            )
        )

        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 激活搜索并输入查询
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "张三"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        // 验证搜索已激活
        assertTrue(viewModel.uiState.value.searchState.isActive, "搜索应该激活")
        assertTrue(viewModel.uiState.value.searchState.query.isNotEmpty(), "查询应该不为空")

        // 关闭搜索
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = false))
        advanceUntilIdle()

        // 验证搜索已关闭
        val searchState = viewModel.uiState.value.searchState
        assertFalse(searchState.isActive, "搜索应该关闭")
        assertEquals("", searchState.query, "查询应该被清空")
        assertFalse(searchState.showEmptyState, "不应该显示空状态")

        // 验证显示所有联系人
        assertEquals(contacts.size, viewModel.uiState.value.displayContacts.size,
            "关闭搜索后应该显示所有联系人")
    }

    /**
     * 集成测试 - 搜索大小写不敏感
     *
     * 测试搜索应该不区分大小写
     */
    @Test
    fun `完整流程 - 搜索不区分大小写`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "Zhang San",
                targetGoal = "Build Good Relationship",
                contextDepth = 10,
                facts = mapOf("Position" to "Product Manager")
            )
        )

        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 测试小写搜索
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "zhang"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.displayContacts.isNotEmpty(),
            "小写搜索应该找到结果")

        // 测试大写搜索
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "ZHANG"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.displayContacts.isNotEmpty(),
            "大写搜索应该找到结果")

        // 测试混合大小写搜索
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "ZhAnG"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.displayContacts.isNotEmpty(),
            "混合大小写搜索应该找到结果")
    }

    /**
     * 集成测试 - 搜索部分匹配
     *
     * 测试搜索应该支持部分匹配
     */
    @Test
    fun `完整流程 - 搜索支持部分匹配`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "张三丰",
                targetGoal = "建立长期合作关系",
                contextDepth = 10,
                facts = emptyMap()
            )
        )

        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 搜索部分姓名
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "张三"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.displayContacts.isNotEmpty(),
            "部分姓名搜索应该找到结果")

        // 搜索部分目标
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "长期合作"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.displayContacts.isNotEmpty(),
            "部分目标搜索应该找到结果")
    }

    /**
     * 集成测试 - 快速连续搜索（防抖测试）
     *
     * 测试快速连续输入时，系统应该：
     * 1. 只执行最后一次搜索
     * 2. 不会因为频繁搜索导致性能问题
     */
    @Test
    fun `完整流程 - 快速连续搜索应用防抖`() = runTest {
        // Given: 准备测试数据
        val contacts = listOf(
            ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的工作关系",
                contextDepth = 10,
                facts = emptyMap()
            )
        )

        coEvery { getAllContactsUseCase() } returns flowOf(contacts)

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 激活搜索
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true))
        advanceUntilIdle()

        // 快速连续输入（模拟用户快速打字）
        val queries = listOf("张", "张三", "张三丰")
        queries.forEach { query ->
            viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = query))
            // 每次输入间隔很短，不足以触发防抖
            testDispatcher.scheduler.advanceTimeBy(50)
        }

        // 等待防抖延迟
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        // 验证最终查询是最后一个
        val finalQuery = viewModel.uiState.value.searchState.query
        assertEquals(queries.last(), finalQuery, "应该使用最后一次输入的查询")
    }

    /**
     * 集成测试 - 空联系人列表搜索
     *
     * 测试当联系人列表为空时，搜索功能应该正常工作
     */
    @Test
    fun `完整流程 - 空联系人列表搜索`() = runTest {
        // Given: 空联系人列表
        coEvery { getAllContactsUseCase() } returns flowOf(emptyList())

        viewModel = ContactListViewModel(
            getAllContactsUseCase,
            deleteContactUseCase,
            saveProfileUseCase
        )
        advanceUntilIdle()

        // 验证初始状态
        assertEquals(0, viewModel.uiState.value.contacts.size)

        // 激活搜索并输入查询
        viewModel.onEvent(ContactListUiEvent.ManageSearch(active = true, query = "测试"))
        testDispatcher.scheduler.advanceTimeBy(301)
        advanceUntilIdle()

        // 验证搜索状态
        val searchState = viewModel.uiState.value.searchState
        assertTrue(searchState.isActive, "搜索应该激活")
        assertEquals("测试", searchState.query, "查询应该被设置")
        assertFalse(searchState.hasResults, "应该没有结果")
        assertTrue(searchState.showEmptyState, "应该显示空状态")

        // 验证没有崩溃或错误
        assertNull(viewModel.uiState.value.error, "不应该有错误")
    }
}
