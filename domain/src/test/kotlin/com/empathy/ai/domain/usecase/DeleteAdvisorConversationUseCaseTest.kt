package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * DeleteAdvisorConversationUseCase 单元测试
 *
 * 业务背景 (PRD-00026):
 *   AI军师对话删除功能测试，验证单条消息的移除能力
 *
 * 功能验证:
 *   - 按消息ID删除单条对话记录
 *   - 删除失败的错误传递
 *   - 空ID的边界情况（允许删除不做拦截）
 *
 * 设计权衡 (TDD-00026):
 *   - 不在UseCase层做ID格式验证，依赖Repository层处理
 *   - 采用宽松策略：空ID也尝试删除，由DB层决定结果
 *   - 单条删除而非清空会话，符合用户精细化管理需求
 *
 * 任务追踪: FD-00026/阶段2-领域层实现
 */
class DeleteAdvisorConversationUseCaseTest {

    private lateinit var useCase: DeleteAdvisorConversationUseCase
    private lateinit var repository: AiAdvisorRepository

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteAdvisorConversationUseCase(repository)
    }

    @Test
    fun `invoke should call repository deleteConversation`() = runTest {
        // Given
        val conversationId = "conv-1"
        coEvery { repository.deleteConversation(conversationId) } returns Result.success(Unit)

        // When
        val result = useCase(conversationId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.deleteConversation(conversationId) }
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        val conversationId = "conv-1"
        coEvery {
            repository.deleteConversation(conversationId)
        } returns Result.failure(Exception("Delete failed"))

        // When
        val result = useCase(conversationId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Delete failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should handle empty conversationId`() = runTest {
        // Given
        val conversationId = ""
        coEvery { repository.deleteConversation(conversationId) } returns Result.success(Unit)

        // When
        val result = useCase(conversationId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.deleteConversation(conversationId) }
    }
}
