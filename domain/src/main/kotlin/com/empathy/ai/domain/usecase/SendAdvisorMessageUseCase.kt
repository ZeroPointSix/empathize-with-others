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
 * 这是AI军师对话功能的核心用例。
 *
 * 业务背景:
 *   - PRD-00026: AI军师对话功能需求
 *   - 场景: 用户在AI军师界面输入问题，获得专业的关系分析建议
 *
 * 核心职责:
 *   1. 保存用户消息到 ai_advisor_conversations 表
 *   2. 获取联系人画像（用于个性化分析）
 *   3. 获取对话历史（支持上下文连续性）
 *   4. 构建提示词（联系人画像 + 历史 + 当前问题）
 *   5. 调用AI获取回复
 *   6. 保存AI回复
 *
 * 已知限制（TDD-00026）:
 *   - HISTORY_LIMIT = 20: 受AI模型Context Window限制
 *   - SESSION_CONTEXT_LIMIT = 10: 控制Token消耗
 *
 * @see AiAdvisorRepository AI军师仓库接口
 * @see SystemPrompts.AI_ADVISOR_HEADER AI军师系统提示词头部
 * @see SystemPrompts.AI_ADVISOR_FOOTER AI军师系统提示词尾部
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
     *
     * 提示词结构（PRD-00026/3.3.1）:
     *   1. 【联系人画像】姓名、沟通目标
     *   2. 【对话历史】最近10条（SESSION_CONTEXT_LIMIT）
     *   3. 【当前问题】用户输入
     *
     * 设计权衡:
     *   - 为什么限制对话历史为10条? Token消耗控制，避免超出模型Context Window
     *   - 为什么用 takeLast()? 保留最近上下文，远期对话相关性较低
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
