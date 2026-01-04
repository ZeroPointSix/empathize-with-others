package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.SystemPrompts
import javax.inject.Inject

/**
 * 发送AI军师消息用例（核心）
 *
 * 发送用户消息并获取AI军师的回复。
 * 这是AI军师对话功能的核心用例，负责：
 * 1. 保存用户消息
 * 2. 获取联系人画像
 * 3. 获取对话历史
 * 4. 构建提示词
 * 5. 调用AI获取回复
 * 6. 保存AI回复
 */
class SendAdvisorMessageUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val aiRepository: AiRepository,
    private val contactRepository: ContactRepository,
    private val aiProviderRepository: AiProviderRepository
) {
    companion object {
        /** 历史对话记录限制 */
        const val HISTORY_LIMIT = 20

        /** 会话上下文限制 */
        const val SESSION_CONTEXT_LIMIT = 10
    }

    /**
     * 发送消息并获取AI回复
     *
     * @param contactId 联系人ID
     * @param sessionId 会话ID
     * @param userMessage 用户消息内容
     * @return AI回复的对话记录
     */
    suspend operator fun invoke(
        contactId: String,
        sessionId: String,
        userMessage: String
    ): Result<AiAdvisorConversation> {
        // 1. 保存用户消息
        val userConversation = AiAdvisorConversation.createUserMessage(
            contactId = contactId,
            sessionId = sessionId,
            content = userMessage
        )
        aiAdvisorRepository.saveMessage(userConversation).getOrElse {
            return Result.failure(Exception("保存用户消息失败: ${it.message}"))
        }

        // 2. 获取默认AI服务商
        val provider = aiProviderRepository.getDefaultProvider().getOrNull()
            ?: return Result.failure(Exception("未配置AI服务商，请先在设置中配置"))

        // 3. 获取联系人画像
        val contact = contactRepository.getProfile(contactId).getOrNull()
            ?: return Result.failure(Exception("联系人不存在: $contactId"))

        // 4. 获取对话历史（限制数量）
        val history = aiAdvisorRepository.getRecentConversations(contactId, HISTORY_LIMIT)
            .getOrElse { emptyList() }

        // 5. 构建提示词
        val prompt = buildPrompt(contact, history, userMessage)
        val systemInstruction = buildSystemInstruction()

        // 6. 调用AI获取回复
        val aiResponse = aiRepository.generateText(provider, prompt, systemInstruction)
            .getOrElse { error ->
                // 保存失败的AI消息
                val failedConversation = AiAdvisorConversation.createAiMessage(
                    contactId = contactId,
                    sessionId = sessionId,
                    content = "",
                    sendStatus = SendStatus.FAILED
                )
                aiAdvisorRepository.saveMessage(failedConversation)
                return Result.failure(error)
            }

        // 7. 保存AI回复
        val aiConversation = AiAdvisorConversation.createAiMessage(
            contactId = contactId,
            sessionId = sessionId,
            content = aiResponse,
            sendStatus = SendStatus.SUCCESS
        )
        aiAdvisorRepository.saveMessage(aiConversation).getOrElse {
            return Result.failure(Exception("保存AI回复失败: ${it.message}"))
        }

        return Result.success(aiConversation)
    }

    /**
     * 构建提示词
     */
    private fun buildPrompt(
        contact: ContactProfile,
        history: List<AiAdvisorConversation>,
        userMessage: String
    ): String {
        val sb = StringBuilder()

        // 联系人画像
        sb.appendLine("【联系人画像】")
        sb.appendLine("姓名：${contact.name}")
        if (contact.targetGoal.isNotBlank()) {
            sb.appendLine("沟通目标：${contact.targetGoal}")
        }
        sb.appendLine()

        // 对话历史（限制为SESSION_CONTEXT_LIMIT条）
        if (history.isNotEmpty()) {
            sb.appendLine("【对话历史】")
            history.takeLast(SESSION_CONTEXT_LIMIT).forEach { conv ->
                val role = if (conv.messageType == MessageType.USER) "用户" else "AI军师"
                sb.appendLine("$role：${conv.content}")
            }
            sb.appendLine()
        }

        // 当前问题
        sb.appendLine("【当前问题】")
        sb.appendLine(userMessage)

        return sb.toString()
    }

    /**
     * 构建系统指令
     */
    private fun buildSystemInstruction(): String {
        return """${SystemPrompts.AI_ADVISOR_HEADER}

${SystemPrompts.AI_ADVISOR_FOOTER}"""
    }
}
