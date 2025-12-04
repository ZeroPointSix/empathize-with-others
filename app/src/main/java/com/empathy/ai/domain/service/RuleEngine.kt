package com.empathy.ai.domain.service

/**
 * 检查两个 IntRange 是否相交
 */
private fun IntRange.intersects(other: IntRange): Boolean {
    return this.first <= other.last && this.last >= other.first
}

/**
 * 规则匹配策略接口
 *
 * 定义不同的匹配算法（精确、子串、正则等）
 */
interface RuleMatchStrategy {
    /**
     * 检查文本是否匹配规则模式
     *
     * @param text 待检查的文本
     * @param pattern 规则模式
     * @return 是否匹配
     */
    fun matches(text: String, pattern: String): Boolean
}

/**
 * 精确匹配策略
 *
 * 完全匹配整个字符串，区分大小写
 */
class ExactMatchStrategy : RuleMatchStrategy {
    override fun matches(text: String, pattern: String): Boolean {
        return text == pattern
    }
}

/**
 * 子串匹配策略
 *
 * 检查文本是否包含规则模式，支持忽略大小写
 */
class SubstringMatchStrategy : RuleMatchStrategy {
    override fun matches(text: String, pattern: String): Boolean {
        return text.contains(pattern, ignoreCase = true)
    }
}

/**
 * 正则匹配策略
 *
 * 使用正则表达式进行模式匹配
 */
class RegexMatchStrategy : RuleMatchStrategy {
    override fun matches(text: String, pattern: String): Boolean {
        return try {
            pattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(text)
        } catch (e: Exception) {
            // 如果正则表达式无效，回退到子串匹配
            false
        }
    }
}

/**
 * 匹配类型枚举
 *
 * 定义支持的匹配策略类型
 */
enum class MatchType {
    EXACT,      // 精确匹配
    SUBSTRING,  // 子串匹配
    REGEX,      // 正则匹配
}

/**
 * 业务规则
 *
 * @param id 规则唯一标识
 * @param name 规则名称
 * @param pattern 匹配模式
 * @param matchType 匹配类型
 * @param priority 优先级（数值越大优先级越高）
 * @param enabled 是否启用
 */
data class BusinessRule(
    val id: String,
    val name: String,
    val pattern: String,
    val matchType: MatchType = MatchType.SUBSTRING,
    val priority: Int = 50,
    val enabled: Boolean = true
)

/**
 * 规则匹配结果
 *
 * @param rule 匹配的规则
 * @param matchedText 匹配的文本片段
 * @param position 匹配位置
 */
data class RuleMatchResult(
    val rule: BusinessRule,
    val matchedText: String,
    val position: IntRange
)

/**
 * 业务规则引擎
 *
 * 支持可扩展的匹配策略，用于执行各种业务规则检查
 *
 * 使用示例：
 * ```kotlin
 * // 创建引擎
 * val engine = RuleEngine()
 *
 * // 添加规则
 * engine.addRule(BusinessRule(
 *     id = "rule_001",
 *     name = "禁止提及money",
 *     pattern = "money",
 *     matchType = MatchType.SUBSTRING
 * ))
 *
 * // 执行检查
 * val matches = engine.evaluate("I need money")
 * // 返回匹配的规则列表
 * ```
 */
class RuleEngine {

    private val rules = mutableListOf<BusinessRule>()
    private val strategies = mutableMapOf<MatchType, RuleMatchStrategy>()

    init {
        // 注册默认策略
        registerStrategy(MatchType.EXACT, ExactMatchStrategy())
        registerStrategy(MatchType.SUBSTRING, SubstringMatchStrategy())
        registerStrategy(MatchType.REGEX, RegexMatchStrategy())
    }

    /**
     * 注册匹配策略
     *
     * @param matchType 匹配类型
     * @param strategy 策略实现
     */
    fun registerStrategy(matchType: MatchType, strategy: RuleMatchStrategy) {
        strategies[matchType] = strategy
    }

    /**
     * 添加业务规则
     *
     * @param rule 规则
     */
    fun addRule(rule: BusinessRule) {
        rules.add(rule)
    }

    /**
     * 批量添加规则
     *
     * @param rules 规则列表
     */
    fun addRules(rules: List<BusinessRule>) {
        this.rules.addAll(rules)
    }

    /**
     * 移除规则
     *
     * @param ruleId 规则ID
     */
    fun removeRule(ruleId: String) {
        rules.removeAll { it.id == ruleId }
    }

    /**
     * 清空所有规则
     */
    fun clearRules() {
        rules.clear()
    }

    /**
     * 获取所有规则
     */
    fun getAllRules(): List<BusinessRule> {
        return rules.toList()
    }

    /**
     * 评估文本是否匹配任意规则
     *
     * 按优先级从高到低执行，返回所有匹配结果
     * 结果保持评估顺序（优先级降序），不重新排序
     *
     * @param text 待检查的文本
     * @return 匹配结果列表（按优先级排序）
     */
    fun evaluate(text: String): List<RuleMatchResult> {
        val matchedResults = mutableListOf<RuleMatchResult>()
        val processedRanges = mutableListOf<IntRange>() // 记录已处理的范围，避免重复匹配

        // 按优先级排序（从高到低）
        val sortedRules = rules
            .filter { it.enabled }
            .sortedByDescending { it.priority }

        for (rule in sortedRules) {
            val strategy = strategies[rule.matchType] ?: continue

            if (strategy.matches(text, rule.pattern)) {
                // 找到匹配位置
                val matchPositions = findMatchPositions(text, rule.pattern, rule.matchType)

                // 过滤掉已处理的范围
                val newPositions = matchPositions.filter { position ->
                    !isRangeOverlapping(position, processedRanges)
                }

                // 为每个新位置创建结果（保持优先级顺序）
                for (position in newPositions) {
                    matchedResults.add(
                        RuleMatchResult(
                            rule = rule,
                            matchedText = text.substring(position),
                            position = position
                        )
                    )
                    processedRanges.add(position)
                }
            }
        }

        return matchedResults
    }

    /**
     * 快速检查是否有任何规则匹配
     *
     * @param text 待检查的文本
     * @return 是否有匹配
     */
    fun hasMatch(text: String): Boolean {
        return rules
            .filter { it.enabled }
            .any { rule ->
                val strategy = strategies[rule.matchType] ?: return@any false
                strategy.matches(text, rule.pattern)
            }
    }

    /**
     * 查找匹配位置
     */
    private fun findMatchPositions(
        text: String,
        pattern: String,
        matchType: MatchType
    ): List<IntRange> {
        val positions = mutableListOf<IntRange>()

        when (matchType) {
            MatchType.EXACT -> {
                if (text.equals(pattern, ignoreCase = true)) {
                    positions.add(0 until text.length)
                }
            }

            MatchType.SUBSTRING -> {
                var startIndex = 0
                while (true) {
                    val index = text.indexOf(pattern, startIndex, ignoreCase = true)
                    if (index == -1) break

                    positions.add(index until (index + pattern.length))
                    startIndex = index + 1
                }
            }

            MatchType.REGEX -> {
                try {
                    val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
                    regex.findAll(text).forEach { matchResult ->
                        positions.add(matchResult.range)
                    }
                } catch (e: Exception) {
                    // 正则表达式无效，回退到子串匹配
                    return findMatchPositions(text, pattern, MatchType.SUBSTRING)
                }
            }
        }

        return positions
    }

    /**
     * 检查范围是否与已处理范围重叠
     */
    private fun isRangeOverlapping(range: IntRange, processedRanges: List<IntRange>): Boolean {
        return processedRanges.any { processed ->
            range.intersects(processed)
        }
    }
}
