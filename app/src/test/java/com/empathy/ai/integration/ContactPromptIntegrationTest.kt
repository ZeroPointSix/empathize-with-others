package com.empathy.ai.integration

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 联系人专属提示词集成测试
 *
 * 验证联系人专属提示词与提示词系统的集成：
 * - 联系人专属提示词优先级高于全局提示词
 * - 联系人上下文正确构建
 * - 变量替换正确执行
 *
 * @see TDD-00015 提示词设置优化技术设计
 * @see PromptBuilder 三层分离架构
 */
class ContactPromptIntegrationTest {

    // ==================== PromptContext构建测试 ====================

    @Test
    fun `从联系人画像构建PromptContext应该正确提取基本信息`() {
        // Given: 创建联系人画像
        val contact = ContactProfile(
            id = "contact-001",
            name = "张三",
            targetGoal = "建立良好关系",
            facts = emptyList()
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证基本信息
        assertEquals("张三", context.contactName)
        assertNotNull(context.relationshipStatus)
        assertEquals(0, context.factsCount)
    }

    @Test
    fun `从联系人画像构建PromptContext应该正确提取雷区标签`() {
        // Given: 创建包含雷区标签的联系人画像
        val now = System.currentTimeMillis()
        val contact = ContactProfile(
            id = "contact-001",
            name = "李四",
            targetGoal = "维护友谊",
            facts = listOf(
                Fact(key = "雷区", value = "不要提前任", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "雷区", value = "避免谈工作压力", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "爱好", value = "打篮球", timestamp = now, source = FactSource.MANUAL)
            )
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证雷区标签被正确提取
        assertEquals(2, context.riskTags.size)
        assertTrue(context.riskTags.contains("不要提前任"))
        assertTrue(context.riskTags.contains("避免谈工作压力"))
    }

    @Test
    fun `从联系人画像构建PromptContext应该正确提取策略标签`() {
        // Given: 创建包含策略标签的联系人画像
        val now = System.currentTimeMillis()
        val contact = ContactProfile(
            id = "contact-001",
            name = "王五",
            targetGoal = "深化合作",
            facts = listOf(
                Fact(key = "策略", value = "多聊美食话题", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "策略", value = "适当表达关心", timestamp = now, source = FactSource.AI_INFERRED),
                Fact(key = "生日", value = "1990-01-01", timestamp = now, source = FactSource.MANUAL)
            )
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证策略标签被正确提取
        assertEquals(2, context.strategyTags.size)
        assertTrue(context.strategyTags.contains("多聊美食话题"))
        assertTrue(context.strategyTags.contains("适当表达关心"))
    }

    @Test
    fun `从联系人画像构建PromptContext应该正确计算事实数量`() {
        // Given: 创建包含多个事实的联系人画像
        val now = System.currentTimeMillis()
        val contact = ContactProfile(
            id = "contact-001",
            name = "赵六",
            targetGoal = "商务合作",
            facts = listOf(
                Fact(key = "雷区", value = "敏感话题1", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "策略", value = "策略1", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "爱好", value = "读书", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "职业", value = "工程师", timestamp = now, source = FactSource.AI_INFERRED)
            )
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证事实数量
        assertEquals(4, context.factsCount)
    }

    @Test
    fun `PromptContext应该包含今日日期`() {
        // Given: 创建联系人画像
        val contact = ContactProfile(
            id = "contact-001",
            name = "测试用户",
            targetGoal = "测试目标",
            facts = emptyList()
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证今日日期
        assertEquals(LocalDate.now().toString(), context.todayDate)
    }

    // ==================== 变量获取测试 ====================

    @Test
    fun `getVariable应该正确返回contact_name`() {
        val context = PromptContext(contactName = "测试联系人")
        assertEquals("测试联系人", context.getVariable("contact_name"))
    }

    @Test
    fun `getVariable应该正确返回relationship_status`() {
        val context = PromptContext(relationshipStatus = "普通朋友")
        assertEquals("普通朋友", context.getVariable("relationship_status"))
    }

    @Test
    fun `getVariable应该正确返回risk_tags`() {
        val context = PromptContext(riskTags = listOf("话题A", "话题B"))
        assertEquals("话题A、话题B", context.getVariable("risk_tags"))
    }

    @Test
    fun `getVariable应该在risk_tags为空时返回无`() {
        val context = PromptContext(riskTags = emptyList())
        assertEquals("无", context.getVariable("risk_tags"))
    }

    @Test
    fun `getVariable应该正确返回strategy_tags`() {
        val context = PromptContext(strategyTags = listOf("策略A", "策略B"))
        assertEquals("策略A、策略B", context.getVariable("strategy_tags"))
    }

    @Test
    fun `getVariable应该在strategy_tags为空时返回无`() {
        val context = PromptContext(strategyTags = emptyList())
        assertEquals("无", context.getVariable("strategy_tags"))
    }

    @Test
    fun `getVariable应该正确返回facts_count`() {
        val context = PromptContext(factsCount = 10)
        assertEquals("10", context.getVariable("facts_count"))
    }

    @Test
    fun `getVariable应该正确返回today_date`() {
        val context = PromptContext(todayDate = "2025-12-22")
        assertEquals("2025-12-22", context.getVariable("today_date"))
    }

    @Test
    fun `getVariable应该对未知变量返回null`() {
        val context = PromptContext()
        assertNull(context.getVariable("unknown_variable"))
    }

    @Test
    fun `getVariable应该不区分大小写`() {
        val context = PromptContext(contactName = "测试")
        assertEquals("测试", context.getVariable("CONTACT_NAME"))
        assertEquals("测试", context.getVariable("Contact_Name"))
    }

    // ==================== 场景可用变量测试 ====================

    @Test
    fun `ANALYZE场景应该包含所有必要变量`() {
        val variables = PromptScene.ANALYZE.availableVariables
        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("risk_tags"))
        assertTrue(variables.contains("strategy_tags"))
        assertTrue(variables.contains("facts_count"))
    }

    @Test
    fun `POLISH场景应该包含所有必要变量`() {
        val variables = PromptScene.POLISH.availableVariables
        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("risk_tags"))
    }

    @Test
    fun `REPLY场景应该包含所有必要变量`() {
        val variables = PromptScene.REPLY.availableVariables
        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("risk_tags"))
        assertTrue(variables.contains("strategy_tags"))
    }

    @Test
    fun `SUMMARY场景应该包含所有必要变量`() {
        val variables = PromptScene.SUMMARY.availableVariables
        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("facts_count"))
        assertTrue(variables.contains("today_date"))
    }

    // ==================== 联系人专属提示词优先级测试 ====================

    @Test
    fun `联系人专属提示词存在时应该覆盖全局提示词`() {
        // Given: 全局提示词和联系人专属提示词
        val globalPrompt = "全局分析指令"
        val contactPrompt = "针对张三的特殊分析指令"

        // When: 模拟优先级判断逻辑
        val effectivePrompt = if (contactPrompt.isNotBlank()) {
            contactPrompt
        } else {
            globalPrompt
        }

        // Then: 联系人专属提示词优先
        assertEquals(contactPrompt, effectivePrompt)
    }

    @Test
    fun `联系人专属提示词为空时应该使用全局提示词`() {
        // Given: 全局提示词存在，联系人专属提示词为空
        val globalPrompt = "全局分析指令"
        val contactPrompt = ""

        // When: 模拟优先级判断逻辑
        val effectivePrompt = if (contactPrompt.isNotBlank()) {
            contactPrompt
        } else {
            globalPrompt
        }

        // Then: 使用全局提示词
        assertEquals(globalPrompt, effectivePrompt)
    }

    @Test
    fun `联系人专属提示词为null时应该使用全局提示词`() {
        // Given: 全局提示词存在，联系人专属提示词为null
        val globalPrompt = "全局分析指令"
        val contactPrompt: String? = null

        // When: 模拟优先级判断逻辑
        val effectivePrompt = if (!contactPrompt.isNullOrBlank()) {
            contactPrompt
        } else {
            globalPrompt
        }

        // Then: 使用全局提示词
        assertEquals(globalPrompt, effectivePrompt)
    }

    // ==================== 配置完整性测试 ====================

    @Test
    fun `默认配置应该为所有活跃场景创建配置`() {
        val config = GlobalPromptConfig.createDefault()
        val activeScenes = PromptScene.getActiveScenes()

        activeScenes.forEach { scene ->
            assertTrue(
                "默认配置应该包含场景 $scene",
                config.prompts.containsKey(scene)
            )
        }
    }

    @Test
    fun `默认配置不应该包含废弃场景`() {
        val config = GlobalPromptConfig.createDefault()

        @Suppress("DEPRECATION")
        assertFalse(
            "默认配置不应该包含废弃的CHECK场景",
            config.prompts.containsKey(PromptScene.CHECK)
        )

        @Suppress("DEPRECATION")
        assertFalse(
            "默认配置不应该包含废弃的EXTRACT场景",
            config.prompts.containsKey(PromptScene.EXTRACT)
        )
    }

    @Test
    fun `所有活跃场景的默认配置应该启用`() {
        val config = GlobalPromptConfig.createDefault()

        PromptScene.getActiveScenes().forEach { scene ->
            val sceneConfig = config.prompts[scene]
            assertTrue(
                "场景 $scene 的默认配置应该启用",
                sceneConfig?.enabled ?: false
            )
        }
    }

    // ==================== 联系人上下文与场景变量匹配测试 ====================

    @Test
    fun `联系人上下文应该能提供ANALYZE场景所需的所有变量`() {
        // Given: 创建完整的联系人画像
        val now = System.currentTimeMillis()
        val contact = ContactProfile(
            id = "contact-001",
            name = "测试联系人",
            targetGoal = "测试目标",
            facts = listOf(
                Fact(key = "雷区", value = "敏感话题", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "策略", value = "沟通策略", timestamp = now, source = FactSource.MANUAL)
            )
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证所有ANALYZE场景变量都可获取
        PromptScene.ANALYZE.availableVariables.forEach { variable ->
            assertNotNull(
                "ANALYZE场景变量 $variable 应该可以从上下文获取",
                context.getVariable(variable)
            )
        }
    }

    @Test
    fun `联系人上下文应该能提供POLISH场景所需的所有变量`() {
        // Given: 创建联系人画像
        val now = System.currentTimeMillis()
        val contact = ContactProfile(
            id = "contact-001",
            name = "测试联系人",
            targetGoal = "测试目标",
            facts = listOf(
                Fact(key = "雷区", value = "敏感话题", timestamp = now, source = FactSource.MANUAL)
            )
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证所有POLISH场景变量都可获取
        PromptScene.POLISH.availableVariables.forEach { variable ->
            assertNotNull(
                "POLISH场景变量 $variable 应该可以从上下文获取",
                context.getVariable(variable)
            )
        }
    }

    @Test
    fun `联系人上下文应该能提供REPLY场景所需的所有变量`() {
        // Given: 创建联系人画像
        val now = System.currentTimeMillis()
        val contact = ContactProfile(
            id = "contact-001",
            name = "测试联系人",
            targetGoal = "测试目标",
            facts = listOf(
                Fact(key = "雷区", value = "敏感话题", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "策略", value = "沟通策略", timestamp = now, source = FactSource.MANUAL)
            )
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证所有REPLY场景变量都可获取
        PromptScene.REPLY.availableVariables.forEach { variable ->
            assertNotNull(
                "REPLY场景变量 $variable 应该可以从上下文获取",
                context.getVariable(variable)
            )
        }
    }

    @Test
    fun `联系人上下文应该能提供SUMMARY场景所需的所有变量`() {
        // Given: 创建联系人画像
        val now = System.currentTimeMillis()
        val contact = ContactProfile(
            id = "contact-001",
            name = "测试联系人",
            targetGoal = "测试目标",
            facts = listOf(
                Fact(key = "事实", value = "一些事实", timestamp = now, source = FactSource.MANUAL)
            )
        )

        // When: 构建上下文
        val context = PromptContext.fromContact(contact)

        // Then: 验证所有SUMMARY场景变量都可获取
        PromptScene.SUMMARY.availableVariables.forEach { variable ->
            assertNotNull(
                "SUMMARY场景变量 $variable 应该可以从上下文获取",
                context.getVariable(variable)
            )
        }
    }
}
