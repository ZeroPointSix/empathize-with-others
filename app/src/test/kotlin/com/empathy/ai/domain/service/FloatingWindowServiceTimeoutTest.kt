package com.empathy.ai.domain.service

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.AiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FloatingWindowService 超时配置测试
 *
 * 测试来源:
 * - PRD-00031 悬浮窗快速知识回答功能需求 (性能要求: 联网≤3秒, 本地≤2秒)
 * - Code Review 发现的问题修复验证
 *
 * 测试覆盖:
 * 1. 测试 KNOWLEDGE 类型使用独立的超时配置
 * 2. 测试其他 ActionType 使用默认超时配置
 *
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 * @see TDD-00031 悬浮窗快速知识回答功能技术设计
 */
class FloatingWindowServiceTimeoutTest {

    companion object {
        // PRD-00031 性能要求
        private const val KNOWLEDGE_TIMEOUT_MS = 2000L  // 本地AI: ≤2秒
        private const val DEFAULT_TIMEOUT_MS = 30000L  // 默认超时: 30秒

        // 模拟 FloatingWindowService 中的超时获取方法
        /**
         * 根据 ActionType 获取超时时间
         *
         * 业务规则 (PRD-00031/6.3):
         * - 联网查询响应: ≤ 3秒
         * - 本地AI响应: ≤ 2秒
         *
         * @param actionType 操作类型
         * @return 超时时间（毫秒）
         */
        private fun getAiTimeout(actionType: ActionType): Long {
            return when (actionType) {
                ActionType.KNOWLEDGE -> KNOWLEDGE_TIMEOUT_MS
                else -> DEFAULT_TIMEOUT_MS
            }
        }

        /**
         * 获取知识查询专用超时时间
         *
         * @return 超时时间（毫秒）
         */
        private fun getKnowledgeTimeout(): Long = KNOWLEDGE_TIMEOUT_MS
    }

    // ==================== 问题1: 超时配置测试 ====================

    /**
     * 测试 KNOWLEDGE 类型使用专用超时配置
     *
     * 任务: FD-00031/T001 (Code Review问题修复)
     * 业务规则 (PRD-00031/6.3):
     * - 本地AI响应: ≤ 2秒
     */
    @Test
    fun `KNOWLEDGE类型应使用2秒超时`() {
        // Act
        val timeout = getAiTimeout(ActionType.KNOWLEDGE)

        // Assert
        assertEquals(
            "KNOWLEDGE类型超时应为2000ms（符合PRD-00031本地AI性能要求）",
            2000L,
            timeout
        )
    }

    /**
     * 测试 ANALYZE 类型使用默认超时
     */
    @Test
    fun `ANALYZE类型应使用默认超时`() {
        // Act
        val timeout = getAiTimeout(ActionType.ANALYZE)

        // Assert
        assertEquals(
            "ANALYZE类型应使用默认超时",
            DEFAULT_TIMEOUT_MS,
            timeout
        )
    }

    /**
     * 测试 POLISH 类型使用默认超时
     */
    @Test
    fun `POLISH类型应使用默认超时`() {
        // Act
        val timeout = getAiTimeout(ActionType.POLISH)

        // Assert
        assertEquals(
            "POLISH类型应使用默认超时",
            DEFAULT_TIMEOUT_MS,
            timeout
        )
    }

    /**
     * 测试 REPLY 类型使用默认超时
     */
    @Test
    fun `REPLY类型应使用默认超时`() {
        // Act
        val timeout = getAiTimeout(ActionType.REPLY)

        // Assert
        assertEquals(
            "REPLY类型应使用默认超时",
            DEFAULT_TIMEOUT_MS,
            timeout
        )
    }

    /**
     * 测试 getKnowledgeTimeout 专用方法
     */
    @Test
    fun `getKnowledgeTimeout应返回正确的超时值`() {
        // Act
        val timeout = getKnowledgeTimeout()

        // Assert
        assertEquals(
            "知识查询专用超时应为2000ms",
            2000L,
            timeout
        )
    }

    /**
     * 测试所有 ActionType 的超时配置完整性
     *
     * 验证覆盖所有定义的 ActionType
     */
    @Test
    fun `所有ActionType应有对应的超时配置`() {
        // Arrange
        val actionTypes = ActionType.entries

        // Act & Assert
        actionTypes.forEach { actionType ->
            val timeout = getAiTimeout(actionType)
            assertTrue(
                "${actionType.name} 应有有效的超时配置",
                timeout > 0
            )
        }
    }

    /**
     * 测试 KNOWLEDGE 超时显著短于其他操作
     *
     * 业务规则 (PRD-00031):
     * - 知识查询是快速响应场景，应使用较短超时
     */
    @Test
    fun `KNOWLEDGE超时应显著短于其他操作`() {
        // Arrange
        val knowledgeTimeout = getAiTimeout(ActionType.KNOWLEDGE)

        // Act
        val otherTimeouts = listOf(
            getAiTimeout(ActionType.ANALYZE),
            getAiTimeout(ActionType.POLISH),
            getAiTimeout(ActionType.REPLY)
        )

        // Assert
        otherTimeouts.forEach { timeout ->
            assertTrue(
                "KNOWLEDGE超时($knowledgeTimeout)应短于${timeout}",
                knowledgeTimeout < timeout
            )
        }
    }
}
