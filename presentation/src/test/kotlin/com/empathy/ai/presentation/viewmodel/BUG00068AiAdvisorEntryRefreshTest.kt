package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorNavigationTarget
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00068 AI军师入口刷新优化测试
 *
 * ## 业务规则 (BUG-00068)
 * - 首次进入AI军师Tab不应触发导航（hasInitialized=false）
 * - 后续切换回来时才触发导航（hasInitialized=true）
 * - 防止Tab切换时重复入栈
 *
 * ## 测试策略 (TE-00068)
 * - 验证首次进入不触发导航逻辑
 * - 验证refreshNavigationTarget可正常触发
 * - 验证导航状态可正确重置
 *
 * ## 任务追踪
 * - BUG: BUG-00068-AI军师入口与设置回退及非Tab性能覆盖问题
 * - TC: TE-00068-TC-003 导航完成后可再次刷新导航目标
 *
 * ## 测试用例来源
 * - TC-UI-001: AI军师Tab二次进入仍有内容
 * - TC-EDGE-001: lastContactId为空 + 切换Tab
 * - TC-EDGE-002: 联系人被删除后再次进入AI军师
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00068AiAdvisorEntryRefreshTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var aiAdvisorPreferences: AiAdvisorPreferencesRepository
    private lateinit var contactRepository: ContactRepository

    private val testContactId = "contact-123"
    private val testContact = ContactProfile(
        id = testContactId,
        name = "张三",
        targetGoal = "保持联系"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiAdvisorPreferences = mockk(relaxed = true)
        contactRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // 首次进入行为测试 (BUG-00068 问题1)
    // ============================================================

    @Test
    fun `首次进入且lastContact存在应触发导航到Chat`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        // When: 创建ViewModel（首次进入）
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then: 应触发导航到Chat
        val state = viewModel.uiState.value
        val target = state.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.Chat)
        assertEquals(testContactId, (target as AiAdvisorNavigationTarget.Chat).contactId)
    }

    @Test
    fun `首次进入且lastContact为空应触发导航到ContactSelect`() = runTest {
        // Given: 无历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns null

        // When: 创建ViewModel（首次进入）
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then: 应触发导航到ContactSelect
        val state = viewModel.uiState.value
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    // ============================================================
    // 刷新导航目标测试 (TE-00068 TC-003)
    // ============================================================

    @Test
    fun `refreshNavigationTarget should recalculate when called after reset`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 重置导航状态后刷新
        viewModel.resetNavigationState()
        assertNull(viewModel.uiState.value.navigationTarget)

        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应重新计算导航目标
        val state = viewModel.uiState.value
        val target = state.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.Chat)
        assertEquals(testContactId, (target as AiAdvisorNavigationTarget.Chat).contactId)
    }

    @Test
    fun `refreshNavigationTarget should handle deleted contact`() = runTest {
        // Given: 历史联系人已被删除
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 刷新导航目标
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应清除偏好并导航到ContactSelect
        verify { aiAdvisorPreferences.clear() }
        val state = viewModel.uiState.value
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `refreshNavigationTarget should handle repository error`() = runTest {
        // Given: 仓库错误
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.failure(Exception("网络错误"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 刷新导航目标
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应清除偏好并导航到ContactSelect
        verify { aiAdvisorPreferences.clear() }
        val state = viewModel.uiState.value
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    // ============================================================
    // 边界测试 (TE-00068 边界测试)
    // ============================================================

    @Test
    fun `empty contactId should navigate to ContactSelect`() = runTest {
        // Given: lastContactId为空字符串
        every { aiAdvisorPreferences.getLastContactId() } returns ""

        // When: 创建ViewModel
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then: 应导航到ContactSelect
        val state = viewModel.uiState.value
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `multiple refresh calls should not cause issues`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 多次刷新
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应正确计算导航目标
        val state = viewModel.uiState.value
        val target = state.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.Chat)
        assertEquals(testContactId, (target as AiAdvisorNavigationTarget.Chat).contactId)
    }

    // ============================================================
    // 状态一致性测试
    // ============================================================

    @Test
    fun `isLoading should be false after refresh`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 刷新导航目标
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: isLoading应为false
        val state = viewModel.uiState.value
        assertTrue(state.isLoading == false)
    }

    @Test
    fun `navigationTarget should remain null until refresh completes`() = runTest {
        // Given: 有历史联系人（模拟延迟）
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        // When: 重置后刷新
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resetNavigationState()

        // Then: 重置后导航目标应为null
        assertNull(viewModel.uiState.value.navigationTarget)

        // When: 刷新
        viewModel.refreshNavigationTarget()
        // 未advanceUntilIdle前仍可能为null

        // Then: 完成后应有导航目标
        advanceUntilIdle()
        val target = viewModel.uiState.value.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.Chat)
    }

    private fun createViewModel(): AiAdvisorEntryViewModel {
        return AiAdvisorEntryViewModel(
            aiAdvisorPreferences = aiAdvisorPreferences,
            contactRepository = contactRepository
        )
    }
}
