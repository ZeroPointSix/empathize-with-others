package com.empathy.ai.data.util

import com.empathy.ai.data.local.dao.AiAdvisorMessageBlockDao
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.MessageBlockType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * BlockUpdateManager智能节流管理器测试
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlockUpdateManagerTest {

    private lateinit var blockUpdateManager: BlockUpdateManager
    private lateinit var mockDao: AiAdvisorMessageBlockDao
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        coEvery { mockDao.updateContentAndStatus(any(), any(), any()) } returns Unit
        blockUpdateManager = BlockUpdateManager(mockDao, testDispatcher)
    }

    @After
    fun tearDown() {
        blockUpdateManager.cleanup()
    }

    @Test
    fun `块类型变化时应立即写入数据库`() = runTest(testDispatcher) {
        // Given
        val blockId = "block-1"

        // When - 先写入THINKING类型
        blockUpdateManager.smartUpdate(
            blockId = blockId,
            content = "Thinking...",
            blockType = MessageBlockType.THINKING,
            status = MessageBlockStatus.STREAMING
        )

        // Then - 切换到MAIN_TEXT类型时应立即写入
        blockUpdateManager.smartUpdate(
            blockId = blockId,
            content = "Response",
            blockType = MessageBlockType.MAIN_TEXT,
            status = MessageBlockStatus.STREAMING
        )

        // 验证立即写入
        coVerify(atLeast = 1) { mockDao.updateContentAndStatus(any(), any(), any()) }
    }

    @Test
    fun `同类型内容应使用节流延迟写入`() = runTest(testDispatcher) {
        // Given
        val blockId = "block-1"

        // When - 连续写入同类型内容
        blockUpdateManager.smartUpdate(
            blockId = blockId,
            content = "Hello",
            blockType = MessageBlockType.MAIN_TEXT,
            status = MessageBlockStatus.STREAMING
        )

        blockUpdateManager.smartUpdate(
            blockId = blockId,
            content = "Hello World",
            blockType = MessageBlockType.MAIN_TEXT,
            status = MessageBlockStatus.STREAMING
        )

        // Then - 在节流时间内不应立即写入
        // 等待节流时间
        advanceTimeBy(350)

        // 验证最终写入
        coVerify(atLeast = 1) { mockDao.updateContentAndStatus(blockId, any(), any()) }
    }

    @Test
    fun `isComplete为true时应强制刷新`() = runTest(testDispatcher) {
        // Given
        val blockId = "block-1"

        // When
        blockUpdateManager.smartUpdate(
            blockId = blockId,
            content = "Final content",
            blockType = MessageBlockType.MAIN_TEXT,
            status = MessageBlockStatus.SUCCESS,
            isComplete = true
        )

        // Then - 应立即写入
        coVerify { mockDao.updateContentAndStatus(blockId, "Final content", "SUCCESS") }
    }

    @Test
    fun `reset应清除所有待处理更新`() = runTest(testDispatcher) {
        // Given
        val blockId = "block-1"

        // When - 添加待处理更新
        blockUpdateManager.smartUpdate(
            blockId = blockId,
            content = "Pending",
            blockType = MessageBlockType.MAIN_TEXT,
            status = MessageBlockStatus.STREAMING
        )

        // 重置
        blockUpdateManager.reset()

        // Then - 等待节流时间后不应有写入
        advanceTimeBy(350)
        // reset后不应有额外的写入
    }

    @Test
    fun `多个不同Block应独立管理`() = runTest(testDispatcher) {
        // Given
        val blockId1 = "block-1"
        val blockId2 = "block-2"

        // When
        blockUpdateManager.smartUpdate(
            blockId = blockId1,
            content = "Content 1",
            blockType = MessageBlockType.MAIN_TEXT,
            status = MessageBlockStatus.STREAMING
        )

        blockUpdateManager.smartUpdate(
            blockId = blockId2,
            content = "Content 2",
            blockType = MessageBlockType.THINKING,
            status = MessageBlockStatus.STREAMING
        )

        // 等待节流时间
        advanceTimeBy(350)

        // Then - 两个Block都应被更新
        coVerify { mockDao.updateContentAndStatus(blockId1, any(), any()) }
        coVerify { mockDao.updateContentAndStatus(blockId2, any(), any()) }
    }
}
