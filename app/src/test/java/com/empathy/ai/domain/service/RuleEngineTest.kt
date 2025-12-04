package com.empathy.ai.domain.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * RuleEngine 单元测试
 *
 * 测试规则引擎的各项功能，包括不同匹配策略、优先级、重叠处理等
 */
class RuleEngineTest {

    private lateinit var ruleEngine: RuleEngine

    @Before
    fun setup() {
        ruleEngine = RuleEngine()
        ruleEngine.clearRules()
    }

    // ========== 精确匹配策略测试 ==========

    @Test
    fun `exact match should match identical strings`() {
        // Given
        val rule = BusinessRule(
            id = "exact_001",
            name = "精确匹配测试",
            pattern = "money",
            matchType = MatchType.EXACT
        )
        ruleEngine.addRule(rule)

        // When & Then
        assertTrue(ruleEngine.hasMatch("money"))
        assertFalse(ruleEngine.hasMatch("I need money"))
        assertFalse(ruleEngine.hasMatch("Money"))
    }

    @Test
    fun `exact match should be case sensitive by default`() {
        // Given
        val rule = BusinessRule(
            id = "exact_002",
            name = "大小写敏感测试",
            pattern = "MONEY",
            matchType = MatchType.EXACT
        )
        ruleEngine.addRule(rule)

        // When & Then
        assertTrue(ruleEngine.hasMatch("MONEY"))
        assertFalse(ruleEngine.hasMatch("money")) // 区分大小写，所以不匹配
    }

    // ========== 子串匹配策略测试 ==========

    @Test
    fun `substring match should detect pattern within text`() {
        // Given
        val rule = BusinessRule(
            id = "substr_001",
            name = "子串匹配测试",
            pattern = "money",
            matchType = MatchType.SUBSTRING
        )
        ruleEngine.addRule(rule)

        // When
        val matches = ruleEngine.evaluate("I need money for the project")

        // Then
        assertEquals(1, matches.size)
        assertEquals("money", matches[0].matchedText)
        assertEquals("substr_001", matches[0].rule.id)
    }

    @Test
    fun `substring match should be case insensitive`() {
        // Given
        val rule = BusinessRule(
            id = "substr_002",
            name = "大小写不敏感测试",
            pattern = "money",
            matchType = MatchType.SUBSTRING
        )
        ruleEngine.addRule(rule)

        // When & Then
        assertTrue(ruleEngine.hasMatch("I need MONEY"))
        assertTrue(ruleEngine.hasMatch("Money is important"))
    }

    @Test
    fun `substring match should find multiple occurrences`() {
        // Given
        val rule = BusinessRule(
            id = "substr_003",
            name = "多匹配测试",
            pattern = "money",
            matchType = MatchType.SUBSTRING
        )
        ruleEngine.addRule(rule)

        // When
        val matches = ruleEngine.evaluate("I need money, money, and more money")

        // Then
        assertEquals(3, matches.size)
        matches.forEach { match ->
            assertEquals("money", match.matchedText)
            assertEquals("substr_003", match.rule.id)
        }
    }

    // ========== 正则匹配策略测试 ==========

    @Test
    fun `regex match should support pattern matching`() {
        // Given
        val rule = BusinessRule(
            id = "regex_001",
            name = "正则匹配手机号",
            pattern = "1[3-9]\\d{9}",
            matchType = MatchType.REGEX
        )
        ruleEngine.addRule(rule)

        // When
        val matches = ruleEngine.evaluate("我的手机号是13800138000")

        // Then
        assertEquals(1, matches.size)
        assertEquals("13800138000", matches[0].matchedText)
        assertEquals("regex_001", matches[0].rule.id)
    }

    @Test
    fun `regex match should handle complex patterns`() {
        // Given
        val rule = BusinessRule(
            id = "regex_002",
            name = "正则匹配邮箱",
            pattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            matchType = MatchType.REGEX
        )
        ruleEngine.addRule(rule)

        // When & Then
        assertTrue(ruleEngine.hasMatch("联系我：test@example.com"))
        assertTrue(ruleEngine.hasMatch("邮箱是user.name+tag@company.co.uk"))
    }

    // ========== 优先级测试 ==========

    @Test
    fun `evaluate should respect priority order`() {
        // Given
        val lowPriorityRule = BusinessRule(
            id = "low_001",
            name = "低优先级规则",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            priority = 10
        )
        val highPriorityRule = BusinessRule(
            id = "high_001",
            name = "高优先级规则",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            priority = 90
        )
        ruleEngine.addRule(lowPriorityRule)
        ruleEngine.addRule(highPriorityRule)

        // When
        val matches = ruleEngine.evaluate("I need money")

        // Then - 应该只返回高优先级的匹配（因为两者匹配同一内容）
        assertEquals(1, matches.size)
        assertEquals("high_001", matches[0].rule.id)
    }

    @Test
    fun `evaluate should process high priority rules first`() {
        // Given
        val rule1 = BusinessRule(
            id = "rule_1",
            name = "规则1",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            priority = 100
        )
        val rule2 = BusinessRule(
            id = "rule_2",
            name = "规则2",
            pattern = "need",
            matchType = MatchType.SUBSTRING,
            priority = 50
        )
        ruleEngine.addRule(rule1)
        ruleEngine.addRule(rule2)

        // When
        val matches = ruleEngine.evaluate("I need money")

        // Then - 按优先级排序
        assertEquals(2, matches.size)
        assertEquals("rule_1", matches[0].rule.id) // 高优先级在前
        assertEquals("rule_2", matches[1].rule.id)
    }

    // ========== 规则管理测试 ==========

    @Test
    fun `addRule should add rule to engine`() {
        // Given
        val rule = BusinessRule(
            id = "test_001",
            name = "测试规则",
            pattern = "test",
            matchType = MatchType.SUBSTRING
        )

        // When
        ruleEngine.addRule(rule)

        // Then
        assertEquals(1, ruleEngine.getAllRules().size)
        assertEquals("test_001", ruleEngine.getAllRules()[0].id)
    }

    @Test
    fun `addRules should add multiple rules`() {
        // Given
        val rules = listOf(
            BusinessRule(id = "test_001", name = "规则1", pattern = "test1", matchType = MatchType.SUBSTRING),
            BusinessRule(id = "test_002", name = "规则2", pattern = "test2", matchType = MatchType.SUBSTRING),
            BusinessRule(id = "test_003", name = "规则3", pattern = "test3", matchType = MatchType.SUBSTRING)
        )

        // When
        ruleEngine.addRules(rules)

        // Then
        assertEquals(3, ruleEngine.getAllRules().size)
    }

    @Test
    fun `removeRule should remove rule by id`() {
        // Given
        val rule1 = BusinessRule(id = "test_001", name = "规则1", pattern = "test1", matchType = MatchType.SUBSTRING)
        val rule2 = BusinessRule(id = "test_002", name = "规则2", pattern = "test2", matchType = MatchType.SUBSTRING)
        ruleEngine.addRule(rule1)
        ruleEngine.addRule(rule2)

        // When
        ruleEngine.removeRule("test_001")

        // Then
        assertEquals(1, ruleEngine.getAllRules().size)
        assertEquals("test_002", ruleEngine.getAllRules()[0].id)
    }

    @Test
    fun `clearRules should remove all rules`() {
        // Given
        ruleEngine.addRule(BusinessRule(id = "test_001", name = "规则1", pattern = "test1", matchType = MatchType.SUBSTRING))
        ruleEngine.addRule(BusinessRule(id = "test_002", name = "规则2", pattern = "test2", matchType = MatchType.SUBSTRING))

        // When
        ruleEngine.clearRules()

        // Then
        assertEquals(0, ruleEngine.getAllRules().size)
    }

    // ========== 重叠匹配处理测试 ==========

    @Test
    fun `evaluate should avoid overlapping matches`() {
        // Given
        // 两个规则匹配同一内容，但不同优先级
        val rule1 = BusinessRule(
            id = "rule_1",
            name = "规则1",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            priority = 90
        )
        val rule2 = BusinessRule(
            id = "rule_2",
            name = "规则2",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            priority = 80
        )
        ruleEngine.addRule(rule1)
        ruleEngine.addRule(rule2)

        // When
        val matches = ruleEngine.evaluate("I need money")

        // Then - 应该只返回高优先级的匹配
        assertEquals(1, matches.size)
        assertEquals("rule_1", matches[0].rule.id)
    }

    @Test
    fun `evaluate should handle non overlapping matches`() {
        // Given
        val rule1 = BusinessRule(
            id = "rule_1",
            name = "money规则",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            priority = 50
        )
        val rule2 = BusinessRule(
            id = "rule_2",
            name = "need规则",
            pattern = "need",
            matchType = MatchType.SUBSTRING,
            priority = 50
        )
        ruleEngine.addRule(rule1)
        ruleEngine.addRule(rule2)

        // When
        val matches = ruleEngine.evaluate("I need money")

        // Then - 两个非重叠匹配都应该返回
        assertEquals(2, matches.size)
    }

    // ========== hasMatch 快速检查测试 ==========

    @Test
    fun `hasMatch should return true when any rule matches`() {
        // Given
        val rule = BusinessRule(
            id = "test_001",
            name = "测试规则",
            pattern = "test",
            matchType = MatchType.SUBSTRING
        )
        ruleEngine.addRule(rule)

        // When & Then
        assertTrue(ruleEngine.hasMatch("this is a test"))
        assertFalse(ruleEngine.hasMatch("no match here"))
    }

    @Test
    fun `hasMatch should respect enabled flag`() {
        // Given
        val enabledRule = BusinessRule(
            id = "enabled_001",
            name = "启用规则",
            pattern = "test",
            matchType = MatchType.SUBSTRING,
            enabled = true
        )
        val disabledRule = BusinessRule(
            id = "disabled_001",
            name = "禁用规则",
            pattern = "test",
            matchType = MatchType.SUBSTRING,
            enabled = false
        )
        ruleEngine.addRule(enabledRule)
        ruleEngine.addRule(disabledRule)

        // When & Then
        assertTrue(ruleEngine.hasMatch("this is a test"))
    }

    // ========== 位置信息测试 ==========

    @Test
    fun `evaluate should return correct match positions`() {
        // Given
        val rule = BusinessRule(
            id = "pos_001",
            name = "位置测试",
            pattern = "money",
            matchType = MatchType.SUBSTRING
        )
        ruleEngine.addRule(rule)

        val text = "I need money, lots of money"

        // When
        val matches = ruleEngine.evaluate(text)

        // Then
        assertEquals(2, matches.size)
        assertEquals(7..11, matches[0].position)  // 第一个 "money" (索引 7-11)
        assertEquals(22..26, matches[1].position) // 第二个 "money" (索引 22-26)
    }

    // ========== 禁用规则测试 ==========

    @Test
    fun `evaluate should skip disabled rules`() {
        // Given
        val enabledRule = BusinessRule(
            id = "enabled_001",
            name = "启用规则",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            enabled = true
        )
        val disabledRule = BusinessRule(
            id = "disabled_001",
            name = "禁用规则",
            pattern = "money",
            matchType = MatchType.SUBSTRING,
            enabled = false
        )
        ruleEngine.addRule(enabledRule)
        ruleEngine.addRule(disabledRule)

        // When
        val matches = ruleEngine.evaluate("I need money")

        // Then - 只匹配启用的规则
        assertEquals(1, matches.size)
        assertEquals("enabled_001", matches[0].rule.id)
    }
}
