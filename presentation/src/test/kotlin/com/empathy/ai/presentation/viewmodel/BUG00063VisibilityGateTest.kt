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
 * BUG-00063 可见性门控与白屏修复测试
 *
 * ## 业务规则 (BUG-00063)
 * - AiAdvisorScreen 不可见时清理 navigationTarget，避免隐藏 Tab 干扰导航
 * - ON_RESUME 且可见时强制刷新导航目标，修复 Tab 缓存场景下白屏问题
 * - SettingsScreen 不可见时不触发权限请求，避免隐藏 Tab 跳转系统设置
 *
 * ## 问题根因 (BUG-00063)
 * - 从 AI 配置页返回时，AiAdvisorScreen 可见但 navigationTarget 为空
 * - 权限请求在不可见 Tab 触发，导致意外的系统设置跳转
 *
 * ## 解决方案
 * - AiAdvisorScreen 使用 DisposableEffect 监听 ON_RESUME
 * - 增加 isVisible 参数传递，实现可见性门控
 * - SettingsScreen 权限请求增加可见性检查
 *
 * ## 测试策略 (TE-00063)
 * - 验证不可见时重置导航目标
 * - 验证 refreshNavigationTarget 可重新触发导航计算
 * - 验证多次可见性切换不导致状态混乱
 *
 * ## 任务追踪
 * - BUG: BUG-00063-导航回退与白屏闪烁问题
 * - TC: TE-00063-TC-001 不可见时清理导航目标
 * - TC: TE-00063-TC-002 ON_RESUME 时刷新导航
 * - TC: TE-00063-TC-003 多次切换不导致状态混乱
 *
 * @see com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorScreen
 * @see com.empathy.ai.presentation.ui.screen.settings.SettingsScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00063VisibilityGateTest {

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
    // TE-00063-TC-001: 不可见时清理导航目标
    // ============================================================

    @Test
    fun `不可见时重置导航目标应清理 navigationTarget`() = runTest {
        // Given: 有历史联系人，导航目标已设置
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 验证导航目标已设置
        val initialTarget = viewModel.uiState.value.navigationTarget
        assertTrue(initialTarget is AiAdvisorNavigationTarget.Chat)

        // When: 模拟不可见时调用 resetNavigationState（UI 层 LaunchedEffect 会调用）
        viewModel.resetNavigationState()

        // Then: navigationTarget 应被清理
        assertNull(viewModel.uiState.value.navigationTarget)
    }

    @Test
    fun `可见时刷新导航目标应重新计算`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 先重置（模拟不可见），再刷新（模拟 ON_RESUME 且可见）
        viewModel.resetNavigationState()
        assertNull(viewModel.uiState.value.navigationTarget)

        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: navigationTarget 应被重新计算
        val target = viewModel.uiState.value.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.Chat)
        assertEquals(testContactId, (target as AiAdvisorNavigationTarget.Chat).contactId)
    }

    // ============================================================
    // TE-00063-TC-002: ON_RESUME 时刷新导航
    // ============================================================

    @Test
    fun `refreshNavigationTarget 应重新检查联系人存在性`() = runTest {
        // Given: 联系人被删除场景
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 验证初始导航到 ContactSelect
        assertTrue(viewModel.uiState.value.navigationTarget is AiAdvisorNavigationTarget.ContactSelect)

        // When: 重置后刷新（模拟 ON_RESUME）
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应清除偏好并导航到 ContactSelect
        verify(exactly = 1) { aiAdvisorPreferences.clear() }
        val target = viewModel.uiState.value.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `refreshNavigationTarget 应处理空历史记录场景`() = runTest {
        // Given: 无历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 重置后刷新
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应导航到 ContactSelect
        val target = viewModel.uiState.value.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.ContactSelect)
    }

    // ============================================================
    // TE-00063-TC-003: 多次切换不导致状态混乱
    // ============================================================

    @Test
    fun `多次可见性切换应保持正确导航目标`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 模拟多次可见性切换（重置-刷新循环）
        repeat(3) {
            viewModel.resetNavigationState()
            assertNull(viewModel.uiState.value.navigationTarget)

            viewModel.refreshNavigationTarget()
            advanceUntilIdle()

            val target = viewModel.uiState.value.navigationTarget
            assertTrue(target is AiAdvisorNavigationTarget.Chat)
            assertEquals(testContactId, (target as AiAdvisorNavigationTarget.Chat).contactId)
        }
    }

    @Test
    fun `快速切换不应导致并发问题`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 连续调用 resetNavigationState 和 refreshNavigationTarget
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 最后一次刷新结果应生效
        val target = viewModel.uiState.value.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.Chat)
        assertEquals(testContactId, (target as AiAdvisorNavigationTarget.Chat).contactId)
    }

    // ============================================================
    // 边界条件测试
    // ============================================================

    @Test
    fun `空 contactId 场景下刷新应导航到 ContactSelect`() = runTest {
        // Given: lastContactId 为空字符串
        every { aiAdvisorPreferences.getLastContactId() } returns ""

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 重置后刷新
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应导航到 ContactSelect
        val target = viewModel.uiState.value.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `仓库错误场景下刷新应清除偏好`() = runTest {
        // Given: 仓库返回错误
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.failure(Exception("网络错误"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 重置后刷新
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: 应清除偏好并导航到 ContactSelect
        verify(atLeast = 1) { aiAdvisorPreferences.clear() }
        val target = viewModel.uiState.value.navigationTarget
        assertTrue(target is AiAdvisorNavigationTarget.ContactSelect)
    }

    @Test
    fun `isLoading 在刷新完成后应为 false`() = runTest {
        // Given: 有历史联系人
        every { aiAdvisorPreferences.getLastContactId() } returns testContactId
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(testContact)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 重置后刷新
        viewModel.resetNavigationState()
        viewModel.refreshNavigationTarget()
        advanceUntilIdle()

        // Then: isLoading 应为 false
        val state = viewModel.uiState.value
        assertTrue(state.isLoading == false)
        assertTrue(state.navigationTarget is AiAdvisorNavigationTarget.Chat)
    }

    private fun createViewModel(): AiAdvisorEntryViewModel {
        return AiAdvisorEntryViewModel(
            aiAdvisorPreferences = aiAdvisorPreferences,
            contactRepository = contactRepository
        )
    }
}
