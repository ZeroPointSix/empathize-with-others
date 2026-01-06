package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorMessageBlock
import com.empathy.ai.domain.model.AiStreamChunk
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.model.StreamingState
import com.empathy.ai.domain.model.TokenUsage
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

/**
 * 发送AI军师消息（流式版本）
 *
 * 参考Cherry Studio的消息发送流程，实现：
 * 1. 保存用户消息
 * 2. 创建AI消息占位
 * 3. 调用流式API
 * 4. 实时更新Block内容
 *
 * 业务背景 (FD-00028):
 * - 流式响应提升用户体验，实现打字机效果
 * - 支持DeepSeek R1等模型的思考过程展示
 * - Block架构支持多种内容类型（文本、思考、错误）
 *
 * 设计决策 (TDD-00028):
 * - 返回Flow<StreamingState>，支持UI实时更新
 * - 使用Block架构存储消息内容
 * - 思考过程和主文本分别存储在不同Block中
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see StreamingState 流式状态定义
 */
class SendAdvisorMessageStreamingUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val aiRepository: AiRepository,
    private val contactRepository: ContactRepository,
    private val aiProviderRepository: AiProviderRepository
) {
    companion object {
        private const val DEFAULT_HISTORY_LIMIT = 10

        /**
         * AI军师系统提示词
         *
         * 定义AI军师的角色和行为规范。
         */
        private const val SYSTEM_INSTRUCTION = """你是一位专业的社交沟通顾问，帮助用户分析聊天场景并提供建议。

你的职责：
1. 分析对话中的情感和意图
2. 识别潜在的沟通风险
3. 提供具体、实用的沟通建议
4. 帮助用户理解对方的心理状态

请根据联系人画像和对话历史，给出具体、实用的沟通建议。回复时请直接给出分析和建议，不需要JSON格式。"""
    }

    /**
     * 发送消息并返回流式响应
     *
     * @param contactId 联系人ID
     * @param sessionId 会话ID
     * @param userMessage 用户消息内容
     * @param skipUserMessage 是否跳过保存用户消息（用于重新生成场景）
     * @param relatedUserMessageId 关联的用户消息ID（用于重新生成场景，BUG-00048-V4）
     * @return 流式响应Flow
     */
    operator fun invoke(
        contactId: String,
        sessionId: String,
        userMessage: String,
        skipUserMessage: Boolean = false,
        relatedUserMessageId: String? = null
    ): Flow<StreamingState> = flow {
        // 1. 保存用户消息（如果不是重新生成场景）
        val userMessageId = if (!skipUserMessage) {
            val id = UUID.randomUUID().toString()
            val userConversation = AiAdvisorConversation(
                id = id,
                contactId = contactId,
                sessionId = sessionId,
                messageType = MessageType.USER,
                content = userMessage,
                timestamp = System.currentTimeMillis(),
                sendStatus = SendStatus.SUCCESS
            )
            aiAdvisorRepository.saveMessage(userConversation)
            id
        } else {
            // BUG-00048-V4: 重新生成场景，使用传入的relatedUserMessageId
            relatedUserMessageId
        }

        // 2. 创建AI消息占位
        val aiMessageId = UUID.randomUUID().toString()
        val aiMessage = AiAdvisorConversation(
            id = aiMessageId,
            contactId = contactId,
            sessionId = sessionId,
            messageType = MessageType.AI,
            content = "",
            timestamp = System.currentTimeMillis(),
            sendStatus = SendStatus.PENDING,
            // BUG-00048-V4: 关联用户消息ID，用于重新生成时获取原始用户输入
            relatedUserMessageId = userMessageId
        )
        aiAdvisorRepository.saveMessage(aiMessage)

        // 3. 创建初始Block
        val mainTextBlock = AiAdvisorMessageBlock.createMainTextBlock(aiMessageId)
        aiAdvisorRepository.saveBlock(mainTextBlock)

        emit(StreamingState.Started(aiMessageId))

        // 4. 获取Provider和构建提示词
        val providerResult = aiProviderRepository.getDefaultProvider()
        val provider = providerResult.getOrElse {
            emit(StreamingState.Error(it))
            return@flow
        }
        
        if (provider == null) {
            emit(StreamingState.Error(IllegalStateException("未配置默认AI服务商")))
            return@flow
        }
        
        val contact = contactRepository.getProfile(contactId).getOrNull()
        val historyResult = aiAdvisorRepository.getRecentConversations(contactId, DEFAULT_HISTORY_LIMIT)
        val history = historyResult.getOrNull() ?: emptyList()
        val prompt = buildPrompt(contact?.name, history, userMessage)

        // 5. 调用流式API并处理响应
        var thinkingBlockId: String? = null
        val contentBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()
        var thinkingStartTime = 0L

        aiRepository.generateTextStream(provider, prompt, SYSTEM_INSTRUCTION)
            .collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.Started -> {
                        // 已在上面处理
                    }

                    is AiStreamChunk.ThinkingDelta -> {
                        if (thinkingBlockId == null) {
                            thinkingStartTime = System.currentTimeMillis()
                            val thinkingBlock = AiAdvisorMessageBlock.createThinkingBlock(
                                aiMessageId,
                                status = MessageBlockStatus.STREAMING
                            )
                            thinkingBlockId = thinkingBlock.id
                            aiAdvisorRepository.saveBlock(thinkingBlock)
                        }
                        thinkingBuilder.append(chunk.text)
                        emit(
                            StreamingState.ThinkingUpdate(
                                content = thinkingBuilder.toString(),
                                elapsedMs = System.currentTimeMillis() - thinkingStartTime
                            )
                        )
                    }

                    is AiStreamChunk.ThinkingComplete -> {
                        thinkingBlockId?.let { id ->
                            aiAdvisorRepository.updateBlockContent(
                                blockId = id,
                                content = chunk.fullThinking,
                                status = MessageBlockStatus.SUCCESS
                            )
                        }
                    }

                    is AiStreamChunk.TextDelta -> {
                        contentBuilder.append(chunk.text)
                        emit(StreamingState.TextUpdate(contentBuilder.toString()))
                    }

                    is AiStreamChunk.Complete -> {
                        val finalContent = chunk.fullText.ifEmpty { contentBuilder.toString() }
                        
                        // 更新Block内容
                        aiAdvisorRepository.updateBlockContent(
                            blockId = mainTextBlock.id,
                            content = finalContent,
                            status = MessageBlockStatus.SUCCESS
                        )
                        
                        // 【BUG-047修复】更新Message的content和status
                        // 原因：getConversationsFlow()返回的是Message，UI使用Message.content渲染
                        // 必须同步更新Message.content，否则UI显示空白
                        aiAdvisorRepository.updateMessageContentAndStatus(
                            messageId = aiMessageId,
                            content = finalContent,
                            status = SendStatus.SUCCESS
                        )
                        
                        emit(StreamingState.Completed(finalContent, chunk.usage))
                    }

                    is AiStreamChunk.Error -> {
                        // 【BUG-047修复】错误时也更新content，显示错误信息
                        val errorContent = "[AI响应失败: ${chunk.error.message ?: "未知错误"}]"
                        aiAdvisorRepository.updateMessageContentAndStatus(
                            messageId = aiMessageId,
                            content = errorContent,
                            status = SendStatus.FAILED
                        )
                        emit(StreamingState.Error(chunk.error))
                    }
                }
            }
    }.catch { e ->
        emit(StreamingState.Error(e))
    }


    /**
     * 构建AI军师提示词
     *
     * 将联系人信息、对话历史和用户消息组合成完整的提示词。
     *
     * @param contactName 联系人名称
     * @param history 对话历史
     * @param userMessage 用户消息
     * @return 构建好的提示词
     */
    private fun buildPrompt(
        contactName: String?,
        history: List<AiAdvisorConversation>,
        userMessage: String
    ): String {
        val sb = StringBuilder()

        // 联系人信息
        if (!contactName.isNullOrBlank()) {
            sb.appendLine("【联系人】$contactName")
            sb.appendLine()
        }

        // 对话历史
        if (history.isNotEmpty()) {
            sb.appendLine("【对话历史】")
            // 按时间正序排列（oldest -> newest）
            history.sortedBy { it.timestamp }.forEach { conv ->
                val role = if (conv.messageType == MessageType.USER) "用户" else "AI军师"
                sb.appendLine("$role: ${conv.content}")
            }
            sb.appendLine()
        }

        // 当前问题
        sb.appendLine("【当前问题】")
        sb.appendLine(userMessage)

        return sb.toString()
    }
}
