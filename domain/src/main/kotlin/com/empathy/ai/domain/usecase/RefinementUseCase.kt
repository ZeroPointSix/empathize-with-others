package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.AiResult
import com.empathy.ai.domain.model.FloatingWindowError
import com.empathy.ai.domain.model.RefinementRequest
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.util.Logger
import javax.inject.Inject

/**
 * 微调重新生成用例
 *
 * 根据用户反馈重新生成AI结果，支持两种模式：
 * 1. 直接重新生成（无微调指令）
 * 2. 按方向生成（有微调指令）
 *
 * 【重要设计决策】
 * 此用例直接调用 AiRepository，而不是原始的 UseCase（如 AnalyzeChatUseCase）。
 * 原因：原始 UseCase 会将用户输入保存到对话历史记录中。如果重新生成时再次调用
 * 这些 UseCase，会导致同一条输入被重复保存到历史记录中。
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
class RefinementUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "RefinementUseCase"
    }

    /**
     * 执行微调重新生成
     *
     * @param request 微调请求
     * @return AI结果
     */
    suspend operator fun invoke(request: RefinementRequest): Result<AiResult> {
        return try {
            if (request.hasInstruction()) {
                // 有微调指令，使用特殊的微调提示词
                refineWithInstruction(request)
            } else {
                // 无微调指令，直接重新调用原UseCase
                regenerateDirectly(request)
            }
        } catch (e: Exception) {
            logger.e(TAG, "微调失败", e)
            // 使用细粒度错误处理
            Result.failure(FloatingWindowError.fromThrowable(e))
        }
    }

    /**
     * 带微调指令的重新生成
     */
    private suspend fun refineWithInstruction(
        request: RefinementRequest
    ): Result<AiResult> {
        val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
            ?: return Result.failure(FloatingWindowError.NoProviderConfigured)

        // 构建微调提示词
        val refinementPrompt = buildRefinementPrompt(request)

        // 根据原任务类型调用对应的AI接口
        return when (request.originalTask) {
            ActionType.ANALYZE -> {
                aiRepository.refineAnalysis(defaultProvider, refinementPrompt)
                    .map { AiResult.Analysis(it) }
            }
            ActionType.POLISH -> {
                aiRepository.refinePolish(defaultProvider, refinementPrompt)
                    .map { AiResult.Polish(it) }
            }
            ActionType.REPLY -> {
                aiRepository.refineReply(defaultProvider, refinementPrompt)
                    .map { AiResult.Reply(it) }
            }
            @Suppress("DEPRECATION")
            ActionType.CHECK -> {
                // CHECK 已废弃，使用 POLISH 替代
                aiRepository.refinePolish(defaultProvider, refinementPrompt)
                    .map { AiResult.Polish(it) }
            }
        }
    }

    /**
     * 直接重新生成（不带微调指令）
     *
     * 【重要】此方法直接调用 AiRepository，而不是原始 UseCase。
     * 原因：原始 UseCase（如 AnalyzeChatUseCase、GenerateReplyUseCase）会将用户输入
     * 保存到对话历史记录中。如果重新生成时再次调用这些 UseCase，会导致同一条输入
     * 被重复保存到历史记录中。
     *
     * 解决方案：直接调用 AiRepository 的 refine* 方法，这些方法只执行 AI 调用，
     * 不会保存历史记录。
     */
    private suspend fun regenerateDirectly(
        request: RefinementRequest
    ): Result<AiResult> {
        val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
            ?: return Result.failure(FloatingWindowError.NoProviderConfigured)

        // 构建重新生成的提示词（不包含微调指令，只包含原始输入）
        val regeneratePrompt = buildRegeneratePrompt(request)

        return when (request.originalTask) {
            ActionType.ANALYZE -> {
                aiRepository.refineAnalysis(defaultProvider, regeneratePrompt)
                    .map { AiResult.Analysis(it) }
            }
            ActionType.POLISH -> {
                aiRepository.refinePolish(defaultProvider, regeneratePrompt)
                    .map { AiResult.Polish(it) }
            }
            ActionType.REPLY -> {
                aiRepository.refineReply(defaultProvider, regeneratePrompt)
                    .map { AiResult.Reply(it) }
            }
            @Suppress("DEPRECATION")
            ActionType.CHECK -> {
                // CHECK 已废弃，使用 POLISH 替代
                aiRepository.refinePolish(defaultProvider, regeneratePrompt)
                    .map { AiResult.Polish(it) }
            }
        }
    }

    /**
     * 构建重新生成的提示词
     *
     * 用于直接重新生成（无微调指令）的场景。
     * 提示词结构简单，只包含原始输入，让 AI 重新处理。
     */
    private fun buildRegeneratePrompt(request: RefinementRequest): String {
        return buildString {
            appendLine("请重新处理以下内容，给出不同的结果。")
            appendLine()
            appendLine("【原始内容】")
            appendLine(request.originalInput)
            appendLine()
            appendLine("【要求】")
            appendLine("1. 给出与之前不同的回答")
            appendLine("2. 直接输出结果，不要解释")
        }
    }

    /**
     * 构建微调提示词
     * 
     * 注意：为避免历史记录重复叠加，提示词结构设计如下：
     * - 只在首次微调时包含原始输入
     * - 后续微调只基于上次结果进行修改
     * - 明确告诉AI不要重复引用原始内容
     */
    private fun buildRefinementPrompt(request: RefinementRequest): String {
        return buildString {
            appendLine("请根据用户的修改意见，对以下内容进行调整。")
            appendLine()
            appendLine("【需要修改的内容】")
            appendLine(request.lastAiResponse)
            appendLine()
            appendLine("【用户的修改意见】")
            appendLine(request.refinementInstruction)
            appendLine()
            appendLine("【重要要求】")
            appendLine("1. 直接输出修改后的结果，不要解释")
            appendLine("2. 不要重复引用或提及原始内容")
            appendLine("3. 只输出最终结果，不要包含任何前缀说明")
        }
    }
}
