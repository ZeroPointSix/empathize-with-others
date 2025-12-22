package com.empathy.ai.domain.util

import android.util.Log
import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.PromptRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提示词构建器（三层分离架构）
 *
 * 设计原则：
 * - 用户只需定义"AI应该怎么做"，不需要关心"AI要处理什么数据"
 * - 上下文数据（联系人信息、聊天记录）由系统自动注入，对用户透明
 * - 系统约束（角色定义、输出格式）用户不可见、不可编辑
 *
 * 三层架构：
 * 1. 系统约束层（System Layer）- 用户不可见
 *    - Header: 角色定义
 *    - Footer: 输出格式约束
 *
 * 2. 用户指令层（User Instruction Layer）- 用户可编辑
 *    - 全局场景指令：定义AI的分析风格、关注重点
 *    - 联系人专属指令：针对特定联系人的特殊要求
 *
 * 3. 运行时数据层（Runtime Data Layer）- 系统自动注入
 *    - 联系人画像（Facts、Tags）
 *    - 聊天记录
 *    - 攻略目标
 *
 * 最终组装顺序：
 * [系统Header] → [用户全局指令] → [用户联系人指令] → [运行时数据] → [系统Footer]
 */
@Singleton
class PromptBuilder @Inject constructor(
    private val promptRepository: PromptRepository,
    private val variableResolver: PromptVariableResolver
) {
    companion object {
        private const val TAG = "PromptBuilder"
        
        // 标题常量
        private const val USER_PROMPT_TITLE = "【用户自定义指令】"
        private const val CONTACT_PROMPT_TITLE = "【针对此联系人的特殊指令】"
        private const val CONTEXT_DATA_TITLE = "【上下文数据】"
        private const val TOPIC_TITLE = "【当前对话主题】"
        
        /**
         * 上下文数据占位符（已废弃，保留用于向后兼容）
         */
        @Deprecated("使用 buildSystemInstruction 的 runtimeData 参数替代")
        const val CONTEXT_PLACEHOLDER = "{{CONTEXT_DATA_PLACEHOLDER}}"
    }

    /**
     * 构建完整的系统指令（包含运行时数据）
     *
     * 这是主要的构建方法，用于需要上下文数据的场景（如聊天分析）
     * 
     * 组装顺序：
     * 1. 系统Header（角色定义）
     * 2. 用户指令层（联系人专属指令优先，存在则跳过全局指令）
     * 3. 运行时数据（联系人信息、聊天记录等）
     * 4. 系统Footer（输出格式约束）
     *
     * 优先级规则：
     * - 联系人专属指令存在时，只使用联系人指令（覆盖语义）
     * - 联系人专属指令不存在时，使用全局指令
     *
     * @param scene 场景类型
     * @param contactId 联系人ID（可选，用于获取联系人专属提示词）
     * @param context 变量上下文（用于替换用户指令中的变量）
     * @param runtimeData 运行时数据（联系人画像、聊天记录等，由UseCase构建）
     * @return 构建好的完整系统指令
     */
    suspend fun buildSystemInstruction(
        scene: PromptScene,
        contactId: String? = null,
        context: PromptContext,
        runtimeData: String = ""
    ): String {
        return buildString {
            // 1. 系统角色定义（用户不可见）
            append(SystemPrompts.getHeader(scene))
            appendLine()
            appendLine()

            // 2. 用户指令层（联系人专属指令优先）
            // 先获取联系人专属指令
            val contactPrompt = if (contactId != null) {
                getContactPromptSafely(contactId)
            } else null

            if (!contactPrompt.isNullOrBlank()) {
                // 联系人专属指令存在，只使用联系人指令（覆盖全局）
                appendLine(CONTACT_PROMPT_TITLE)
                val resolvedPrompt = variableResolver.resolve(contactPrompt, context)
                appendLine(resolvedPrompt)
                appendLine()
            } else {
                // 联系人指令不存在，使用全局指令
                val globalPrompt = getGlobalPromptSafely(scene)
                if (globalPrompt.isNotBlank()) {
                    appendLine(USER_PROMPT_TITLE)
                    val resolvedPrompt = variableResolver.resolve(globalPrompt, context)
                    appendLine(resolvedPrompt)
                    appendLine()
                }
            }

            // 3. 运行时数据（系统自动注入，用户不可见）
            if (runtimeData.isNotBlank()) {
                appendLine(CONTEXT_DATA_TITLE)
                appendLine(runtimeData)
                appendLine()
            }

            // 4. 输出格式约束（用户不可见）
            append(SystemPrompts.getFooter(scene))
        }
    }

    /**
     * 构建简化的系统指令（不包含运行时数据）
     *
     * 用于不需要额外上下文数据的场景，如简单的文本检查
     *
     * @param scene 场景类型
     * @param contactId 联系人ID（可选）
     * @param context 变量上下文
     * @return 构建好的系统指令
     */
    suspend fun buildSimpleInstruction(
        scene: PromptScene,
        contactId: String? = null,
        context: PromptContext
    ): String = buildSystemInstruction(scene, contactId, context, runtimeData = "")

    /**
     * 构建包含对话主题的系统指令
     *
     * 当用户设置了对话主题时，将主题内容注入到系统提示词中，
     * 帮助AI更好地理解对话背景并提供更精准的回复。
     *
     * @param scene 场景类型
     * @param contactId 联系人ID（可选）
     * @param context 变量上下文
     * @param topic 当前对话主题（可选）
     * @param runtimeData 运行时数据
     * @return 构建好的包含主题的系统指令
     */
    suspend fun buildWithTopic(
        scene: PromptScene,
        contactId: String? = null,
        context: PromptContext,
        topic: ConversationTopic?,
        runtimeData: String = ""
    ): String {
        // 先构建基础指令
        val baseInstruction = buildSystemInstruction(scene, contactId, context, runtimeData)

        // 如果没有主题，直接返回基础指令
        if (topic == null || topic.content.isBlank()) {
            return baseInstruction
        }

        // 构建主题部分
        val topicSection = buildTopicSection(topic)

        // 将主题注入到系统指令中（在Footer之前）
        return "$baseInstruction\n\n$topicSection"
    }

    /**
     * 构建主题部分的提示词
     */
    private fun buildTopicSection(topic: ConversationTopic): String {
        return buildString {
            appendLine(TOPIC_TITLE)
            appendLine("用户设置了以下对话主题，请在回复时充分考虑这个背景：")
            appendLine()
            appendLine(topic.content)
            appendLine()
            append("请确保你的回复与上述主题保持一致和相关。")
        }
    }

    /**
     * 仅获取用户自定义指令部分
     *
     * 用于需要单独获取用户指令的场景（如Function Calling模式下追加到基础指令后）
     * 不包含系统Header/Footer，只返回用户编辑的内容
     *
     * 优先级规则：
     * - 联系人专属指令存在时，只返回联系人指令（覆盖语义）
     * - 联系人专属指令不存在时，返回全局指令
     *
     * @param scene 场景类型
     * @param contactId 联系人ID（可选）
     * @param context 变量上下文
     * @return 用户自定义指令（联系人专属优先，否则全局）
     */
    suspend fun getUserInstructionOnly(
        scene: PromptScene,
        contactId: String? = null,
        context: PromptContext
    ): String {
        return buildString {
            // 先获取联系人专属指令
            val contactPrompt = if (contactId != null) {
                getContactPromptSafely(contactId)
            } else null

            if (!contactPrompt.isNullOrBlank()) {
                // 联系人专属指令存在，只使用联系人指令
                appendLine(CONTACT_PROMPT_TITLE)
                val resolvedPrompt = variableResolver.resolve(contactPrompt, context)
                appendLine(resolvedPrompt)
            } else {
                // 联系人指令不存在，使用全局指令
                val globalPrompt = getGlobalPromptSafely(scene)
                if (globalPrompt.isNotBlank()) {
                    appendLine(USER_PROMPT_TITLE)
                    val resolvedPrompt = variableResolver.resolve(globalPrompt, context)
                    appendLine(resolvedPrompt)
                }
            }
        }.trim()
    }

    // ==================== 兼容性方法（已废弃，保留向后兼容）====================

    /**
     * @deprecated 使用 buildSystemInstruction(scene, contactId, context, runtimeData) 替代
     * 
     * 注入上下文数据到包含占位符的指令中
     * 此方法保留用于向后兼容，新代码应直接使用 buildSystemInstruction 并传入 runtimeData
     */
    @Deprecated(
        message = "使用 buildSystemInstruction(scene, contactId, context, runtimeData) 替代",
        replaceWith = ReplaceWith("buildSystemInstruction(scene, contactId, context, runtimeData)")
    )
    fun injectContextData(instruction: String, contextData: String): String {
        // 兼容旧的占位符方式
        val placeholder = "{{CONTEXT_DATA_PLACEHOLDER}}"
        return if (instruction.contains(placeholder)) {
            instruction.replace(placeholder, contextData)
        } else {
            // 如果没有占位符，直接追加
            "$instruction\n\n$CONTEXT_DATA_TITLE\n$contextData"
        }
    }

    /**
     * 安全获取全局提示词，失败时记录日志并返回空字符串
     */
    private suspend fun getGlobalPromptSafely(scene: PromptScene): String {
        val result = promptRepository.getGlobalPrompt(scene)
        return if (result.isSuccess) {
            result.getOrNull() ?: ""
        } else {
            Log.w(TAG, "获取全局提示词失败: ${result.exceptionOrNull()?.message}")
            ""
        }
    }

    /**
     * 安全获取联系人提示词，失败时记录日志并返回null
     */
    private suspend fun getContactPromptSafely(contactId: String): String? {
        val result = promptRepository.getContactPrompt(contactId)
        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            Log.w(TAG, "获取联系人提示词失败: ${result.exceptionOrNull()?.message}")
            null
        }
    }
}
