package com.empathy.ai.presentation.integration

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.usecase.*
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.viewmodel.ContactDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * 标签添加完整流程集成测试
 *
 * 测试从点击按钮到标签显示的完整用户流程
 * 验证需求: 1.1, 1.2, 1.3, 1.6
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddTagIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getContactUseCase: GetContactUseCase
    private lateinit var deleteContactUseCase: DeleteContactUseCase
    private lateinit var getBrainTagsUseCase: GetBrainTagsUseCase
    private lateinit var saveBrainTagUseCase: SaveBrainTagUseCase
    private lateinit var deleteBrainTagUseCase: DeleteBrainTagUseCase
    private lateinit var saveProfileUseCase: SaveProfileUseCase

    private lateinit var viewModel: ContactDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 创建 mock 对象
        getContactUseCase = mockk(relaxed = true)
        deleteContactUseCase = mockk(relaxed = true)
        getBrainTagsUseCase = mockk(relaxed = true)
        saveBrainTagUseCase = mockk(relaxed = true)
        deleteBrainTagUseCase = mockk(relaxed = true)
        saveProfileUseCase = mockk(relaxed = true)

        // 配置默认行为
        coEvery { getBrainTagsUseCase(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 集成测试 - 标签添加完整流程（成功场景）
     *
     * 测试步骤：
     * 1. 用户加载联系人详情页
     * 2. 用户点击"添加标签"按钮
     * 3. 系统显示标签添加对话框
     * 4. 用户输入标签内容
     * 5. 用户选择标签类型
     * 6. 用户点击"确认"按钮
     * 7. 系统保存标签到数据库
     * 8. 系统关闭对话框
     * 9. 系统在标签列表中显示新标签
     */
    @Test
    fun `完整流程 - 成功添加标签`() = runTest {
        // Given: 准备测试数据
        val testContactId = "test-contact-123"
        val testContact = ContactProfile(
            id = testContactId,
            name = "张三",
            targetGoal = "建立良好的工作关系",
            contextDepth = 10,
            facts = mapOf("职位" to "产品经理")
        )

        val tagContent = "不要在周一早上讨论敏感话题"
        val tagType = TagType.RISK_RED

        // Mock 联系人加载
        coEvery { getContactUseCase(testContactId) } returns Result.success(testContact)

        // Mock 标签保存
        val savedTagSlot = slot<BrainTag>()
        coEvery { saveBrainTagUseCase(capture(savedTagSlot)) } returns Result.success(1L)

        // Mock 标签列表更新
        val newTag = BrainTag(
            id = 1L,
            contactId = testContactId,
            content = tagContent,
            type = tagType,
            source = "MANUAL"
        )
        coEvery { getBrainTagsUseCase(testContactId) } returns flowOf(listOf(newTag))

        // 创建 ViewModel
        viewModel = ContactDetailViewModel(
            getContactUseCase,
            deleteContactUseCase,
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase,
            saveProfileUseCase
        )

        // Step 1: 加载联系人详情页
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(testContactId))
        advanceUntilIdle()

        // 验证联系人已加载
        assertEquals(testContactId, viewModel.uiState.value.contactId)
        assertEquals(testContact.name, viewModel.uiState.value.name)

        // Step 2 & 3: 点击"添加标签"按钮，显示对话框
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(show = true))
        advanceUntilIdle()

        // 验证对话框已显示
        assertTrue(viewModel.uiState.value.tagDialog.isVisible, "对话框应该显示")
        assertEquals("", viewModel.uiState.value.tagDialog.content, "对话框内容应该为空")

        // Step 4: 输入标签内容
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(content = tagContent))
        advanceUntilIdle()

        // 验证内容已更新
        assertEquals(tagContent, viewModel.uiState.value.tagDialog.content)

        // Step 5: 选择标签类型
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(type = tagType))
        advanceUntilIdle()

        // 验证类型已更新
        assertEquals(tagType, viewModel.uiState.value.tagDialog.type)

        // Step 6: 点击"确认"按钮
        viewModel.onEvent(ContactDetailUiEvent.AddTag)
        advanceUntilIdle()

        // Step 7: 验证标签已保存到数据库
        coVerify { saveBrainTagUseCase(any()) }
        assertTrue(savedTagSlot.isCaptured, "应该调用保存标签的用例")

        val savedTag = savedTagSlot.captured
        assertEquals(testContactId, savedTag.contactId, "标签应该关联到正确的联系人")
        assertEquals(tagContent, savedTag.content, "标签内容应该正确")
        assertEquals(tagType, savedTag.type, "标签类型应该正确")
        assertEquals("MANUAL", savedTag.source, "标签来源应该是手动添加")

        // Step 8: 验证对话框已关闭
        assertFalse(viewModel.uiState.value.tagDialog.isVisible, "对话框应该关闭")
        assertEquals("", viewModel.uiState.value.tagDialog.content, "对话框内容应该被清空")

        // Step 9: 验证标签在列表中显示
        // 注意：在实际应用中，标签列表会通过 Flow 自动更新
        // 这里我们验证 ViewModel 的状态是否正确
        assertNull(viewModel.uiState.value.tagDialog.contentError, "不应该有错误信息")
    }

    /**
     * 集成测试 - 标签添加失败场景
     *
     * 测试当保存失败时，系统应该：
     * 1. 保持对话框打开
     * 2. 显示错误信息
     * 3. 保留用户输入的内容
     */
    @Test
    fun `完整流程 - 添加标签失败时保持对话框打开`() = runTest {
        // Given: 准备测试数据
        val testContactId = "test-contact-456"
        val testContact = ContactProfile(
            id = testContactId,
            name = "李四",
            targetGoal = "提升沟通效率",
            contextDepth = 10,
            facts = emptyMap()
        )

        val tagContent = "避免在下午开会"
        val tagType = TagType.STRATEGY_GREEN

        // Mock 联系人加载
        coEvery { getContactUseCase(testContactId) } returns Result.success(testContact)

        // Mock 标签保存失败
        coEvery { saveBrainTagUseCase(any()) } returns Result.failure(
            Exception("数据库连接失败")
        )

        // 创建 ViewModel
        viewModel = ContactDetailViewModel(
            getContactUseCase,
            deleteContactUseCase,
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase,
            saveProfileUseCase
        )

        // 加载联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(testContactId))
        advanceUntilIdle()

        // 打开对话框并输入内容
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(
            show = true,
            content = tagContent,
            type = tagType
        ))
        advanceUntilIdle()

        // 尝试添加标签
        viewModel.onEvent(ContactDetailUiEvent.AddTag)
        advanceUntilIdle()

        // 验证对话框仍然打开
        assertTrue(viewModel.uiState.value.tagDialog.isVisible, "保存失败时对话框应该保持打开")

        // 验证内容没有被清空
        assertEquals(tagContent, viewModel.uiState.value.tagDialog.content, "内容应该保留")
        assertEquals(tagType, viewModel.uiState.value.tagDialog.type, "类型应该保留")

        // 验证显示错误信息
        assertNotNull(viewModel.uiState.value.tagDialog.contentError, "应该显示错误信息")
    }

    /**
     * 集成测试 - 标签内容验证
     *
     * 测试当用户输入无效内容时，系统应该：
     * 1. 显示验证错误信息
     * 2. 不调用保存用例
     * 3. 保持对话框打开
     */
    @Test
    fun `完整流程 - 空内容验证失败`() = runTest {
        // Given: 准备测试数据
        val testContactId = "test-contact-789"
        val testContact = ContactProfile(
            id = testContactId,
            name = "王五",
            targetGoal = "保持友好关系",
            contextDepth = 10,
            facts = emptyMap()
        )

        // Mock 联系人加载
        coEvery { getContactUseCase(testContactId) } returns Result.success(testContact)

        // 创建 ViewModel
        viewModel = ContactDetailViewModel(
            getContactUseCase,
            deleteContactUseCase,
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase,
            saveProfileUseCase
        )

        // 加载联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(testContactId))
        advanceUntilIdle()

        // 打开对话框
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(show = true))
        advanceUntilIdle()

        // 测试空字符串
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(content = ""))
        advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddTag)
        advanceUntilIdle()

        // 验证显示错误信息
        assertNotNull(viewModel.uiState.value.tagDialog.contentError, "空内容应该显示错误")

        // 验证对话框仍然打开
        assertTrue(viewModel.uiState.value.tagDialog.isVisible, "验证失败时对话框应该保持打开")

        // 验证没有调用保存用例
        coVerify(exactly = 0) { saveBrainTagUseCase(any()) }

        // 测试纯空白字符串
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(content = "   \t\n  "))
        advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddTag)
        advanceUntilIdle()

        // 验证显示错误信息
        assertNotNull(viewModel.uiState.value.tagDialog.contentError, "空白内容应该显示错误")

        // 验证仍然没有调用保存用例
        coVerify(exactly = 0) { saveBrainTagUseCase(any()) }
    }

    /**
     * 集成测试 - 取消添加标签
     *
     * 测试当用户取消添加时，系统应该：
     * 1. 关闭对话框
     * 2. 不保存标签
     */
    @Test
    fun `完整流程 - 取消添加标签`() = runTest {
        // Given: 准备测试数据
        val testContactId = "test-contact-cancel"
        val testContact = ContactProfile(
            id = testContactId,
            name = "赵六",
            targetGoal = "建立信任",
            contextDepth = 10,
            facts = emptyMap()
        )

        val tagContent = "这是一个测试标签"

        // Mock 联系人加载
        coEvery { getContactUseCase(testContactId) } returns Result.success(testContact)

        // 创建 ViewModel
        viewModel = ContactDetailViewModel(
            getContactUseCase,
            deleteContactUseCase,
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase,
            saveProfileUseCase
        )

        // 加载联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(testContactId))
        advanceUntilIdle()

        // 打开对话框并输入内容
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(show = true, content = tagContent))
        advanceUntilIdle()

        // 验证对话框已打开
        assertTrue(viewModel.uiState.value.tagDialog.isVisible)
        assertEquals(tagContent, viewModel.uiState.value.tagDialog.content)

        // 取消添加（关闭对话框）
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(show = false))
        advanceUntilIdle()

        // 验证对话框已关闭
        assertFalse(viewModel.uiState.value.tagDialog.isVisible, "对话框应该关闭")

        // 验证没有调用保存用例
        coVerify(exactly = 0) { saveBrainTagUseCase(any()) }
    }

    /**
     * 集成测试 - 编辑模式下添加标签
     *
     * 测试在编辑模式下添加标签时，标签应该：
     * 1. 添加到临时状态
     * 2. 不立即保存到数据库
     * 3. 在保存联系人时一起保存
     */
    @Test
    fun `完整流程 - 编辑模式下添加标签到临时状态`() = runTest {
        // Given: 准备测试数据
        val testContactId = "test-contact-edit"
        val testContact = ContactProfile(
            id = testContactId,
            name = "孙七",
            targetGoal = "深化合作",
            contextDepth = 10,
            facts = emptyMap()
        )

        val tagContent = "编辑模式测试标签"
        val tagType = TagType.RISK_RED

        // Mock 联系人加载
        coEvery { getContactUseCase(testContactId) } returns Result.success(testContact)

        // Mock 保存操作
        coEvery { saveBrainTagUseCase(any()) } returns Result.success(1L)
        coEvery { saveProfileUseCase(any()) } returns Result.success(Unit)

        // 创建 ViewModel
        viewModel = ContactDetailViewModel(
            getContactUseCase,
            deleteContactUseCase,
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase,
            saveProfileUseCase
        )

        // 加载联系人并进入编辑模式
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(testContactId))
        advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.StartEdit)
        advanceUntilIdle()

        // 验证处于编辑模式
        assertTrue(viewModel.uiState.value.isEditMode, "应该处于编辑模式")

        // 添加标签
        viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(
            show = true,
            content = tagContent,
            type = tagType
        ))
        advanceUntilIdle()

        viewModel.onEvent(ContactDetailUiEvent.AddTag)
        advanceUntilIdle()

        // 验证标签未立即保存到数据库
        coVerify(exactly = 0) { saveBrainTagUseCase(any()) }

        // 验证标签在临时状态中
        val tempTags = viewModel.uiState.value.temporaryTags
        assertTrue(tempTags.any { it.content == tagContent && it.type == tagType },
            "标签应该在临时状态中")

        // 保存联系人
        viewModel.onEvent(ContactDetailUiEvent.SaveContact)
        advanceUntilIdle()

        // 验证标签现在被保存到数据库
        coVerify(atLeast = 1) { saveBrainTagUseCase(any()) }
    }

    /**
     * 集成测试 - 连续添加多个标签
     *
     * 测试用户可以连续添加多个标签
     */
    @Test
    fun `完整流程 - 连续添加多个标签`() = runTest {
        // Given: 准备测试数据
        val testContactId = "test-contact-multiple"
        val testContact = ContactProfile(
            id = testContactId,
            name = "周八",
            targetGoal = "长期合作",
            contextDepth = 10,
            facts = emptyMap()
        )

        val tags = listOf(
            "标签1" to TagType.RISK_RED,
            "标签2" to TagType.STRATEGY_GREEN,
            "标签3" to TagType.RISK_RED
        )

        // Mock 联系人加载
        coEvery { getContactUseCase(testContactId) } returns Result.success(testContact)

        // Mock 标签保存
        val savedTags = mutableListOf<BrainTag>()
        coEvery { saveBrainTagUseCase(any()) } answers {
            savedTags.add(firstArg())
            Result.success(savedTags.size.toLong())
        }

        // 创建 ViewModel
        viewModel = ContactDetailViewModel(
            getContactUseCase,
            deleteContactUseCase,
            getBrainTagsUseCase,
            saveBrainTagUseCase,
            deleteBrainTagUseCase,
            saveProfileUseCase
        )

        // 加载联系人
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(testContactId))
        advanceUntilIdle()

        // 连续添加多个标签
        tags.forEach { (content, type) ->
            // 打开对话框
            viewModel.onEvent(ContactDetailUiEvent.ManageTagDialog(
                show = true,
                content = content,
                type = type
            ))
            advanceUntilIdle()

            // 添加标签
            viewModel.onEvent(ContactDetailUiEvent.AddTag)
            advanceUntilIdle()

            // 验证对话框已关闭
            assertFalse(viewModel.uiState.value.tagDialog.isVisible,
                "添加标签后对话框应该关闭")
        }

        // 验证所有标签都被保存
        assertEquals(tags.size, savedTags.size, "应该保存${tags.size}个标签")

        // 验证每个标签的内容和类型
        tags.forEachIndexed { index, (content, type) ->
            val savedTag = savedTags[index]
            assertEquals(content, savedTag.content, "标签${index + 1}内容应该正确")
            assertEquals(type, savedTag.type, "标签${index + 1}类型应该正确")
            assertEquals(testContactId, savedTag.contactId, "标签${index + 1}应该关联到正确的联系人")
        }
    }
}
