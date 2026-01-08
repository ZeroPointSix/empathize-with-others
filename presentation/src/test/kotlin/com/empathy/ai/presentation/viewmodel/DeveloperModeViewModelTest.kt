package com.empathy.ai.presentation.viewmodel

import app.cash.turbine.test
import com.empathy.ai.domain.repository.DeveloperModeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * DeveloperModeViewModel 单元测试
 *
 * 测试开发者模式的状态管理和持久化功能
 * 
 * @see BUG-00050 开发者模式状态持久化问题
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperModeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DeveloperModeRepository
    private lateinit var viewModel: DeveloperModeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 初始化测试 ====================

    @Test
    fun `初始化时应从Repository读取开发者模式状态`() = runTest {
        // Given: Repository中保存了开发者模式已解锁
        every { repository.isDeveloperModeUnlocked() } returns true
        every { repository.isCurrentSession() } returns true

        // When: 创建ViewModel
        viewModel = DeveloperModeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 开发者模式应该是激活状态
        assertTrue(viewModel.isDeveloperMode.value)
    }

    @Test
    fun `初始化时如果Repository中未解锁则开发者模式应为关闭`() = runTest {
        // Given: Repository中开发者模式未解锁
        every { repository.isDeveloperModeUnlocked() } returns false
        every { repository.isCurrentSession() } returns true

        // When: 创建ViewModel
        viewModel = DeveloperModeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 开发者模式应该是关闭状态
        assertFalse(viewModel.isDeveloperMode.value)
    }

    @Test
    fun `初始化时如果Session已过期则开发者模式应为关闭`() = runTest {
        // Given: Repository中开发者模式已解锁，但Session已过期
        every { repository.isDeveloperModeUnlocked() } returns true
        every { repository.isCurrentSession() } returns false

        // When: 创建ViewModel
        viewModel = DeveloperModeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 开发者模式应该是关闭状态（因为Session过期）
        assertFalse(viewModel.isDeveloperMode.value)
        // 并且应该重置Repository
        verify { repository.reset() }
    }

    // ==================== 版本号点击测试 ====================

    @Test
    fun `点击版本号7次应解锁开发者模式`() = runTest {
        // Given: 初始状态未解锁
        every { repository.isDeveloperModeUnlocked() } returns false
        every { repository.isCurrentSession() } returns true
        viewModel = DeveloperModeViewModel(repository)

        // When: 点击7次
        repeat(7) {
            viewModel.onVersionClick()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Then: 开发者模式应该激活
        assertTrue(viewModel.isDeveloperMode.value)
        // 并且应该持久化状态
        verify { repository.setDeveloperModeUnlocked(true) }
    }

    @Test
    fun `点击版本号少于7次不应解锁开发者模式`() = runTest {
        // Given: 初始状态未解锁
        every { repository.isDeveloperModeUnlocked() } returns false
        every { repository.isCurrentSession() } returns true
        viewModel = DeveloperModeViewModel(repository)

        // When: 点击6次
        repeat(6) {
            viewModel.onVersionClick()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Then: 开发者模式应该仍然关闭
        assertFalse(viewModel.isDeveloperMode.value)
        // 不应该持久化
        verify(exactly = 0) { repository.setDeveloperModeUnlocked(true) }
    }

    @Test
    fun `点击版本号3次后应显示提示信息`() = runTest {
        // Given: 初始状态未解锁
        every { repository.isDeveloperModeUnlocked() } returns false
        every { repository.isCurrentSession() } returns true
        viewModel = DeveloperModeViewModel(repository)

        // When & Then: 收集toast消息
        viewModel.toastMessage.test {
            // 点击3次
            repeat(3) {
                viewModel.onVersionClick()
                testDispatcher.scheduler.advanceUntilIdle()
            }

            // 应该收到提示消息
            val message = awaitItem()
            assertTrue(message.contains("再点击"))
            assertTrue(message.contains("4"))
        }
    }

    @Test
    fun `已解锁状态下点击版本号不应有任何效果`() = runTest {
        // Given: 已解锁状态
        every { repository.isDeveloperModeUnlocked() } returns true
        every { repository.isCurrentSession() } returns true
        viewModel = DeveloperModeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialClickCount = viewModel.clickCount.value

        // When: 点击版本号
        viewModel.onVersionClick()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 点击计数不应改变
        assertEquals(initialClickCount, viewModel.clickCount.value)
    }

    // ==================== 退出开发者模式测试 ====================

    @Test
    fun `退出开发者模式应重置状态并清除持久化`() = runTest {
        // Given: 已解锁状态
        every { repository.isDeveloperModeUnlocked() } returns true
        every { repository.isCurrentSession() } returns true
        viewModel = DeveloperModeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When: 退出开发者模式
        viewModel.exitDeveloperMode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 状态应该重置
        assertFalse(viewModel.isDeveloperMode.value)
        assertEquals(0, viewModel.clickCount.value)
        // 应该调用 reset() 完整清除状态（包括 Session ID）
        verify { repository.reset() }
    }

    // ==================== 持久化测试 ====================

    @Test
    fun `解锁开发者模式后应持久化状态`() = runTest {
        // Given: 初始状态未解锁
        every { repository.isDeveloperModeUnlocked() } returns false
        every { repository.isCurrentSession() } returns true
        viewModel = DeveloperModeViewModel(repository)

        // When: 解锁开发者模式
        repeat(7) {
            viewModel.onVersionClick()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Then: 应该调用持久化方法
        verify { repository.setDeveloperModeUnlocked(true) }
        verify { repository.updateSessionId() }
    }

    @Test
    fun `导航离开后返回应保持开发者模式状态`() = runTest {
        // Given: 已解锁状态
        every { repository.isDeveloperModeUnlocked() } returns true
        every { repository.isCurrentSession() } returns true

        // When: 创建新的ViewModel实例（模拟导航返回）
        val newViewModel = DeveloperModeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 开发者模式应该仍然激活
        assertTrue(newViewModel.isDeveloperMode.value)
    }

    // ==================== 重置点击计数测试 ====================

    @Test
    fun `重置点击计数应将计数归零`() = runTest {
        // Given: 已点击几次
        every { repository.isDeveloperModeUnlocked() } returns false
        every { repository.isCurrentSession() } returns true
        viewModel = DeveloperModeViewModel(repository)
        
        repeat(3) {
            viewModel.onVersionClick()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // When: 重置点击计数
        viewModel.resetClickCount()

        // Then: 计数应该归零
        assertEquals(0, viewModel.clickCount.value)
    }
}
