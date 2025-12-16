package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.PromptValidationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提示词输入验证器
 *
 * 验证提示词的长度、变量有效性等，返回Success/Warning/Error三种结果
 */
@Singleton
class PromptValidator @Inject constructor(
    private val variableResolver: PromptVariableResolver
) {
    companion object {
        /**
         * 提示词最大长度（字符数）
         */
        const val MAX_PROMPT_LENGTH = 1000

        /**
         * 警告长度阈值（80%时警告）
         */
        const val WARNING_LENGTH_THRESHOLD = 800
    }

    /**
     * 验证提示词
     *
     * 验证顺序：
     * 1. 检查空值 -> Error
     * 2. 检查长度超限 -> Error
     * 3. 检查无效变量 -> Error
     * 4. 检查接近长度限制 -> Warning
     * 5. 全部通过 -> Success
     *
     * @param prompt 要验证的提示词
     * @param scene 场景类型（用于确定可用变量）
     * @return 验证结果
     */
    fun validate(prompt: String, scene: PromptScene): PromptValidationResult {
        // 1. 检查空值
        if (prompt.isBlank()) {
            return PromptValidationResult.Error(
                message = "提示词不能为空",
                errorType = PromptValidationResult.ErrorType.EMPTY_PROMPT
            )
        }

        // 2. 检查长度限制
        if (prompt.length > MAX_PROMPT_LENGTH) {
            return PromptValidationResult.Error(
                message = "提示词长度不能超过${MAX_PROMPT_LENGTH}字符，当前${prompt.length}字符",
                errorType = PromptValidationResult.ErrorType.EXCEEDS_LENGTH_LIMIT
            )
        }

        // 3. 检查变量有效性
        val invalidVariables = variableResolver.findInvalidVariables(
            template = prompt,
            allowedVariables = scene.availableVariables
        )
        if (invalidVariables.isNotEmpty()) {
            return PromptValidationResult.Error(
                message = "使用了无效变量: ${invalidVariables.joinToString(", ")}",
                errorType = PromptValidationResult.ErrorType.INVALID_VARIABLES
            )
        }

        // 4. 检查是否接近长度限制（警告）
        if (prompt.length > WARNING_LENGTH_THRESHOLD) {
            return PromptValidationResult.Warning(
                message = "提示词长度接近限制（${prompt.length}/${MAX_PROMPT_LENGTH}）",
                warningType = PromptValidationResult.WarningType.NEAR_LENGTH_LIMIT
            )
        }

        return PromptValidationResult.Success
    }

    /**
     * 批量验证多个场景的提示词
     *
     * @param prompts 场景到提示词的映射
     * @return 场景到验证结果的映射
     */
    fun validateAll(
        prompts: Map<PromptScene, String>
    ): Map<PromptScene, PromptValidationResult> {
        return prompts.mapValues { (scene, prompt) ->
            validate(prompt, scene)
        }
    }
}
