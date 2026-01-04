package com.empathy.ai.domain.util

/**
 * 提示词安全过滤器
 *
 * 检测Prompt Injection攻击，防止恶意用户通过提示词注入危险指令。
 * 这是应用安全的重要防线，确保AI系统提示词不被恶意篡改。
 *
 * 业务背景:
 * - Prompt Injection是一种常见的AI安全攻击方式
 * - 攻击者试图通过特殊指令覆盖系统的原始指令
 * - 需要在用户输入到达AI之前进行过滤
 *
 * 检测策略:
 * - 中英文双语检测（覆盖更广泛场景）
 * - 正则表达式匹配常见攻击模式
 * - 检测到危险内容时返回警告而非直接拒绝
 *
 * @see PRD-00006 安全与权限管理需求
 */
object PromptSanitizer {

    /**
     * 危险内容匹配模式列表
     *
     * 包含中英文常见的注入攻击模式，按风险等级分类：
     * - 指令覆盖类: ignore、disregard、forget
     * - 角色扮演类: you are now、act as、pretend
     * - 限制绕过类: jailbreak、bypass restriction
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
     * 执行流程:
     * 1. 遍历所有危险模式
     * 2. 使用正则表达式检测匹配
     * 3. 收集所有警告信息
     * 4. 返回检测结果
     *
     * 设计权衡:
     * - 不直接拒绝，而是返回警告
     * - 让用户决定是否继续使用
     * - 避免过度拦截影响正常使用
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
