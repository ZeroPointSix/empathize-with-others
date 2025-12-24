package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.PromptContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提示词变量解析器
 *
 * 负责解析模板中的{{变量名}}格式占位符，并替换为实际值
 *
 * 设计决策（CR-00012优化）：
 * - 移除缓存机制：模板数量有限，缓存收益微乎其微，简化代码复杂度
 * - 正则表达式预编译：避免重复编译开销
 */
@Singleton
class PromptVariableResolver @Inject constructor() {

    companion object {
        /**
         * 预编译的变量匹配正则表达式
         * 匹配格式：{{变量名}}，变量名只能包含字母、数字和下划线
         */
        private val VARIABLE_PATTERN: Regex by lazy {
            Regex("\\{\\{(\\w+)\\}\\}", RegexOption.IGNORE_CASE)
        }
    }

    /**
     * 解析模板中的变量并替换为实际值
     *
     * @param template 包含{{变量名}}占位符的模板
     * @param context 变量上下文，提供变量值
     * @return 替换后的字符串，未知变量保持原样
     */
    fun resolve(template: String, context: PromptContext): String {
        return VARIABLE_PATTERN.replace(template) { match ->
            val variableName = match.groupValues[1].lowercase()
            context.getVariable(variableName) ?: match.value
        }
    }

    /**
     * 提取模板中使用的所有变量名
     *
     * @param template 模板字符串
     * @return 变量名列表（小写，去重）
     */
    fun extractVariables(template: String): List<String> {
        return VARIABLE_PATTERN.findAll(template)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .toList()
    }

    /**
     * 查找模板中的无效变量
     *
     * @param template 模板字符串
     * @param allowedVariables 允许使用的变量列表
     * @return 无效变量名列表
     */
    fun findInvalidVariables(
        template: String,
        allowedVariables: List<String>
    ): List<String> {
        val usedVariables = extractVariables(template)
        val allowedSet = allowedVariables.map { it.lowercase() }.toSet()
        return usedVariables.filter { it !in allowedSet }
    }

    /**
     * 清除缓存（保留接口兼容性）
     *
     * 注意：CR-00012优化后已移除缓存，此方法为空实现，保持API兼容
     */
    fun clearCache() {
        // 已移除缓存机制，保留方法签名以兼容现有调用
    }
}
