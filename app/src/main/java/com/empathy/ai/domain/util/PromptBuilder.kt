package com.empathy.ai.domain.util

import android.util.Log
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.PromptRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提示词构建器
 *
 * 负责按合并规则构建完整的系统指令，合并顺序：
 * 1. System Header（系统角色定义）
 * 2. 用户自定义指令（变量已替换）
 * 3. 联系人专属指令（如有）
 * 4. 上下文数据占位符（可选）
 * 5. System Footer（输出格式约束）
 */
@Singleton
class PromptBuilder @Inject constructor(
    private val promptRepository: PromptRepository,
    private val variableResolver: PromptVariableResolver
) {
    companion object {
        private const val TAG = "PromptBuilder"
        
        /** 上下文数据占位符 */
        const val CONTEXT_PLACEHOLDER = "{{CONTEXT_DATA_PLACEHOLDER}}"
        
        // 标题常量，避免硬编码
        private const val USER_PROMPT_TITLE = "【用户自定义指令】"
        private const val CONTACT_PROMPT_TITLE = "【针对此联系人的特殊指令】"
        private const val CONTEXT_DATA_TITLE = "【上下文数据】"
    }

    /**
     * 构建完整的系统指令（包含上下文占位符）
     *
     * @param scene 场景类型
     * @param contactId 联系人ID（可选，用于获取联系人专属提示词）
     * @param context 变量上下文
     * @return 构建好的系统指令
     */
    suspend fun buildSystemInstruction(
        scene: PromptScene,
        contactId: String? = null,
        context: PromptContext
    ): String = buildInstructionBase(scene, contactId, context, includeContextPlaceholder = true)

    /**
     * 构建简化的系统指令（不包含上下文占位符）
     *
     * 用于不需要额外上下文数据的场景
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
    ): String = buildInstructionBase(scene, contactId, context, includeContextPlaceholder = false)

    /**
     * 注入上下文数据
     *
     * 将实际的上下文数据替换到占位符位置
     *
     * @param instruction 包含占位符的指令
     * @param contextData 实际的上下文数据
     * @return 替换后的完整指令
     */
    fun injectContextData(instruction: String, contextData: String): String {
        return instruction.replace(CONTEXT_PLACEHOLDER, contextData)
    }

    /**
     * 内部方法：构建指令基础逻辑
     *
     * 消除buildSystemInstruction和buildSimpleInstruction的重复代码
     *
     * @param scene 场景类型
     * @param contactId 联系人ID（可选）
     * @param context 变量上下文
     * @param includeContextPlaceholder 是否包含上下文占位符
     * @return 构建好的系统指令
     */
    private suspend fun buildInstructionBase(
        scene: PromptScene,
        contactId: String?,
        context: PromptContext,
        includeContextPlaceholder: Boolean
    ): String {
        return buildString {
            // 1. 系统角色定义
            append(SystemPrompts.getHeader(scene))
            appendLine()
            appendLine()

            // 2. 用户自定义指令
            val globalPrompt = getGlobalPromptSafely(scene)
            if (globalPrompt.isNotBlank()) {
                appendLine(USER_PROMPT_TITLE)
                val resolvedPrompt = variableResolver.resolve(globalPrompt, context)
                appendLine(resolvedPrompt)
                appendLine()
            }

            // 3. 联系人专属提示词
            if (contactId != null) {
                val contactPrompt = getContactPromptSafely(contactId)
                if (!contactPrompt.isNullOrBlank()) {
                    appendLine(CONTACT_PROMPT_TITLE)
                    val resolvedPrompt = variableResolver.resolve(contactPrompt, context)
                    appendLine(resolvedPrompt)
                    appendLine()
                }
            }

            // 4. 上下文数据占位（可选）
            if (includeContextPlaceholder) {
                appendLine(CONTEXT_DATA_TITLE)
                appendLine(CONTEXT_PLACEHOLDER)
                appendLine()
            }

            // 5. 输出格式约束
            append(SystemPrompts.getFooter(scene))
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
