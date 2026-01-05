package com.empathy.ai.data.util

import com.empathy.ai.data.di.IoDispatcher
import com.empathy.ai.data.local.dao.AiAdvisorMessageBlockDao
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.MessageBlockType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Block更新管理器
 *
 * 参考Cherry Studio的BlockManager智能节流策略：
 * - 块类型变化时立即写入（如从THINKING切换到MAIN_TEXT）
 * - 同类型内容使用节流（300ms），减少数据库写入频率
 * - 完成时强制刷新，确保数据完整性
 *
 * 业务背景 (FD-00028):
 * - 流式响应会产生大量小增量更新
 * - 频繁数据库写入会影响性能和电池寿命
 * - 智能节流在保证实时性的同时优化性能
 *
 * 设计决策:
 * - 使用ConcurrentHashMap存储待更新内容
 * - 节流延迟300ms，平衡实时性和性能
 * - 块类型变化时立即写入，确保UI正确渲染
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see RESEARCH-00004 Cherry项目AI对话实现深度分析报告
 */
@Singleton
class BlockUpdateManager @Inject constructor(
    private val aiAdvisorMessageBlockDao: AiAdvisorMessageBlockDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "BlockUpdateManager"
        private const val THROTTLE_DELAY_MS = 300L
    }

    private var lastBlockType: MessageBlockType? = null
    private val pendingUpdates = ConcurrentHashMap<String, PendingUpdate>()
    private var throttleJob: Job? = null
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    /**
     * 待更新数据
     *
     * @property blockId Block ID
     * @property content 内容
     * @property status 状态
     */
    data class PendingUpdate(
        val blockId: String,
        val content: String,
        val status: MessageBlockStatus
    )

    /**
     * 智能更新Block内容
     *
     * 根据块类型变化和完成状态决定是立即写入还是节流写入：
     * - 块类型变化：立即写入（确保UI正确切换）
     * - 完成状态：立即写入（确保数据完整）
     * - 同类型增量：节流写入（优化性能）
     *
     * @param blockId Block ID
     * @param content 新内容
     * @param blockType 块类型
     * @param status 块状态
     * @param isComplete 是否完成
     */
    suspend fun smartUpdate(
        blockId: String,
        content: String,
        blockType: MessageBlockType,
        status: MessageBlockStatus = MessageBlockStatus.STREAMING,
        isComplete: Boolean = false
    ) {
        val isTypeChanged = lastBlockType != null && lastBlockType != blockType

        if (isTypeChanged || isComplete) {
            // 块类型变化或完成时，先刷新待更新内容，再立即写入当前内容
            flushPendingUpdates()
            withContext(ioDispatcher) {
                aiAdvisorMessageBlockDao.updateContentAndStatus(
                    blockId = blockId,
                    content = content,
                    status = status.name
                )
            }
        } else {
            // 同类型增量更新，使用节流
            pendingUpdates[blockId] = PendingUpdate(blockId, content, status)
            scheduleFlush()
        }

        lastBlockType = blockType
    }


    /**
     * 调度节流刷新
     *
     * 取消之前的节流任务，重新开始计时。
     * 这确保了在持续更新时不会频繁写入数据库。
     */
    private fun scheduleFlush() {
        throttleJob?.cancel()
        throttleJob = scope.launch {
            delay(THROTTLE_DELAY_MS)
            flushPendingUpdates()
        }
    }

    /**
     * 刷新所有待更新内容
     *
     * 将pendingUpdates中的所有内容写入数据库。
     * 在块类型变化、完成状态或节流超时时调用。
     */
    private suspend fun flushPendingUpdates() {
        val updates = pendingUpdates.toMap()
        pendingUpdates.clear()

        if (updates.isEmpty()) return

        withContext(ioDispatcher) {
            updates.values.forEach { update ->
                aiAdvisorMessageBlockDao.updateContentAndStatus(
                    blockId = update.blockId,
                    content = update.content,
                    status = update.status.name
                )
            }
        }
    }

    /**
     * 强制刷新
     *
     * 立即将所有待更新内容写入数据库。
     * 在流式响应完成时调用，确保数据完整性。
     */
    suspend fun forceFlush() {
        throttleJob?.cancel()
        flushPendingUpdates()
    }

    /**
     * 重置状态
     *
     * 清除所有待更新内容和状态。
     * 在开始新的流式响应前调用。
     */
    fun reset() {
        lastBlockType = null
        pendingUpdates.clear()
        throttleJob?.cancel()
    }

    /**
     * 清理资源
     *
     * 取消协程作用域，释放资源。
     * 在不再需要此管理器时调用。
     */
    fun cleanup() {
        scope.cancel()
    }
}
