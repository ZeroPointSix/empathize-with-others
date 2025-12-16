package com.empathy.ai.domain.util

/**
 * 提示词安全过滤器
 *
 * 检测Prompt Injection攻击，防止恶意用户通过提示词注入危险指令
 */
object PromptSanitizer {

    /**
     * 危险内容匹配模式列表
     *
     * 包含中英文常见的注入攻击模式
     */
    private val DANGEROUS_PATTERNS = listOf(
        // 中文注入模式
        Regex("忽略.*指令", RegexOption.IGNORE_CASE),
        Regex("忽略.*上面", RegexOption.IGNORE_CASE),
        Regex("忽略.*之前", RegexOption.IGNORE_CASE),
        Regex("新的角色", RegexOption.IGNORE_CASE),
        Regex("你现在是", RegexOption.IGNORE_CASE),
        Regex("扮演.*角色", RegexOption.IGNORE_CASE),
        Regex("假装.*是", RegexOption.IGNORE_CASE),
        Regex("不要.*限制", RegexOption.IGNORE_CASE),
        Regex("取消.*限制", RegexOption.IGNORE_CASE),

        // 英文注入模式
        Regex("ignore.*instruction", RegexOption.IGNORE_CASE),
        Regex("ignore.*above", RegexOption.IGNORE_CASE),
        Regex("ignore.*previous", RegexOption.IGNORE_CASE),
        Regex("disregard.*above", RegexOption.IGNORE_CASE),
        Regex("disregard.*previous", RegexOption.IGNORE_CASE),
        Regex("forget.*previous", RegexOption.IGNORE_CASE),
        Regex("forget.*above", RegexOption.IGNORE_CASE),
        Regex("you are now", RegexOption.IGNORE_CASE),
        Regex("act as", RegexOption.IGNORE_CASE),
        Regex("pretend.*to be", RegexOption.IGNORE_CASE),
        Regex("new role", RegexOption.IGNORE_CASE),
        Regex("jailbreak", RegexOption.IGNORE_CASE),
        Regex("bypass.*restriction", RegexOption.IGNORE_CASE)
    )

    /**
     * 检测提示词中的危险内容
     *
     * @param prompt 要检测的提示词
     * @return 检测结果，包含是否安全和警告列表
     */
    fun detectDangerousContent(prompt: String): SanitizeResult {
        val warnings = mutableListOf<String>()

        DANGEROUS_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(prompt)) {
                warnings.add("检测到可能的指令覆盖尝试: ${pattern.pattern}")
            }
        }

        return SanitizeResult(
            isSafe = warnings.isEmpty(),
            warnings = warnings
        )
    }

    /**
     * 检测结果
     *
     * @property isSafe 是否安全（无危险内容）
     * @property warnings 警告消息列表
     */
    data class SanitizeResult(
        val isSafe: Boolean,
        val warnings: List<String>
    )
}
