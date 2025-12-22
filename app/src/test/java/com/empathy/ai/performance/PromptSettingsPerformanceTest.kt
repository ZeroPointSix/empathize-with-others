package com.empathy.ai.performance

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * 提示词设置性能影响评估测试
 *
 * 验证TD-00015提示词设置优化后的性能表现：
 * - 场景过滤性能
 * - 配置创建性能
 * - 上下文构建性能
 * - 变量解析性能
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
class PromptSettingsPerformanceTest {

    companion object {
        // 性能阈值（毫秒）
        private const val SCENE_FILTER_THRESHOLD_MS = 10L
        private const val CONFIG_CREATE_THRESHOLD_MS = 50L
        private const val CONTEXT_BUILD_THRESHOLD_MS = 20L
        private const val VARIABLE_RESOLVE_THRESHOLD_MS = 5L
        private const val BATCH_OPERATION_THRESHOLD_MS = 100L

        // 批量操作次数
        private const val BATCH_SIZE = 1000
    }

    // ==================== 场景过滤性能测试 ====================

    @Test
    fun `getSettingsScenes性能应该在阈值内`() {
        // Warm up
        repeat(100) { PromptScene.getSettingsScenes() }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                PromptScene.getSettingsScenes()
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("getSettingsScenes 平均耗时: ${avgTime}ms")

        assertTrue(
            "getSettingsScenes 平均耗时应该小于 ${SCENE_FILTER_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < SCENE_FILTER_THRESHOLD_MS
        )
    }

    @Test
    fun `getActiveScenes性能应该在阈值内`() {
        // Warm up
        repeat(100) { PromptScene.getActiveScenes() }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                PromptScene.getActiveScenes()
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("getActiveScenes 平均耗时: ${avgTime}ms")

        assertTrue(
            "getActiveScenes 平均耗时应该小于 ${SCENE_FILTER_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < SCENE_FILTER_THRESHOLD_MS
        )
    }

    @Test
    fun `SETTINGS_SCENE_ORDER访问性能应该在阈值内`() {
        // Warm up
        repeat(100) { PromptScene.SETTINGS_SCENE_ORDER }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                PromptScene.SETTINGS_SCENE_ORDER.forEach { it.displayName }
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("SETTINGS_SCENE_ORDER 访问平均耗时: ${avgTime}ms")

        assertTrue(
            "SETTINGS_SCENE_ORDER 访问平均耗时应该小于 ${SCENE_FILTER_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < SCENE_FILTER_THRESHOLD_MS
        )
    }

    // ==================== 配置创建性能测试 ====================

    @Test
    fun `createDefault性能应该在阈值内`() {
        // Warm up
        repeat(10) { GlobalPromptConfig.createDefault() }

        // Measure
        val time = measureTimeMillis {
            repeat(100) {
                GlobalPromptConfig.createDefault()
            }
        }

        val avgTime = time.toDouble() / 100
        println("createDefault 平均耗时: ${avgTime}ms")

        assertTrue(
            "createDefault 平均耗时应该小于 ${CONFIG_CREATE_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < CONFIG_CREATE_THRESHOLD_MS
        )
    }

    @Test
    fun `配置复制性能应该在阈值内`() {
        val config = GlobalPromptConfig.createDefault()

        // Warm up
        repeat(100) { config.copy() }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                config.copy(version = config.version + 1)
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("配置复制 平均耗时: ${avgTime}ms")

        assertTrue(
            "配置复制 平均耗时应该小于 ${SCENE_FILTER_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < SCENE_FILTER_THRESHOLD_MS
        )
    }

    @Test
    fun `配置更新性能应该在阈值内`() {
        var config = GlobalPromptConfig.createDefault()

        // Warm up
        repeat(10) {
            val updatedPrompts = config.prompts.toMutableMap()
            updatedPrompts[PromptScene.ANALYZE] = ScenePromptConfig(
                userPrompt = "测试提示词",
                enabled = true
            )
            config = config.copy(prompts = updatedPrompts)
        }

        // Measure
        val time = measureTimeMillis {
            repeat(100) {
                val updatedPrompts = config.prompts.toMutableMap()
                updatedPrompts[PromptScene.ANALYZE] = ScenePromptConfig(
                    userPrompt = "更新的提示词 $it",
                    enabled = true
                )
                config = config.copy(prompts = updatedPrompts)
            }
        }

        val avgTime = time.toDouble() / 100
        println("配置更新 平均耗时: ${avgTime}ms")

        assertTrue(
            "配置更新 平均耗时应该小于 ${CONFIG_CREATE_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < CONFIG_CREATE_THRESHOLD_MS
        )
    }

    // ==================== 上下文构建性能测试 ====================

    @Test
    fun `fromContact性能应该在阈值内`() {
        val contact = createTestContact()

        // Warm up
        repeat(100) { PromptContext.fromContact(contact) }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                PromptContext.fromContact(contact)
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("fromContact 平均耗时: ${avgTime}ms")

        assertTrue(
            "fromContact 平均耗时应该小于 ${CONTEXT_BUILD_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < CONTEXT_BUILD_THRESHOLD_MS
        )
    }

    @Test
    fun `大量事实的联系人上下文构建性能应该在阈值内`() {
        // 创建包含大量事实的联系人
        val now = System.currentTimeMillis()
        val facts = (1..100).map { i ->
            when {
                i % 3 == 0 -> Fact(
                    key = "雷区",
                    value = "雷区话题$i",
                    timestamp = now,
                    source = FactSource.MANUAL
                )
                i % 3 == 1 -> Fact(
                    key = "策略",
                    value = "策略$i",
                    timestamp = now,
                    source = FactSource.MANUAL
                )
                else -> Fact(
                    key = "事实$i",
                    value = "内容$i",
                    timestamp = now,
                    source = FactSource.AI_INFERRED
                )
            }
        }
        val contact = ContactProfile(
            id = "contact-large",
            name = "大量事实联系人",
            targetGoal = "测试目标",
            facts = facts
        )

        // Warm up
        repeat(10) { PromptContext.fromContact(contact) }

        // Measure
        val time = measureTimeMillis {
            repeat(100) {
                PromptContext.fromContact(contact)
            }
        }

        val avgTime = time.toDouble() / 100
        println("大量事实联系人上下文构建 平均耗时: ${avgTime}ms")

        assertTrue(
            "大量事实联系人上下文构建 平均耗时应该小于 ${BATCH_OPERATION_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < BATCH_OPERATION_THRESHOLD_MS
        )
    }

    // ==================== 变量解析性能测试 ====================

    @Test
    fun `getVariable性能应该在阈值内`() {
        val context = PromptContext(
            contactName = "测试联系人",
            relationshipStatus = "普通朋友",
            riskTags = listOf("话题A", "话题B", "话题C"),
            strategyTags = listOf("策略A", "策略B"),
            factsCount = 10,
            todayDate = "2025-12-22"
        )

        val variables = listOf(
            "contact_name",
            "relationship_status",
            "risk_tags",
            "strategy_tags",
            "facts_count",
            "today_date"
        )

        // Warm up
        repeat(100) {
            variables.forEach { context.getVariable(it) }
        }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                variables.forEach { context.getVariable(it) }
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("getVariable (6个变量) 平均耗时: ${avgTime}ms")

        assertTrue(
            "getVariable 平均耗时应该小于 ${VARIABLE_RESOLVE_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < VARIABLE_RESOLVE_THRESHOLD_MS
        )
    }

    @Test
    fun `未知变量查询性能应该在阈值内`() {
        val context = PromptContext()

        // Warm up
        repeat(100) { context.getVariable("unknown_variable") }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                context.getVariable("unknown_variable_$it")
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("未知变量查询 平均耗时: ${avgTime}ms")

        assertTrue(
            "未知变量查询 平均耗时应该小于 ${VARIABLE_RESOLVE_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < VARIABLE_RESOLVE_THRESHOLD_MS
        )
    }

    // ==================== ActionType映射性能测试 ====================

    @Test
    fun `fromActionType性能应该在阈值内`() {
        val actionTypes = com.empathy.ai.domain.model.ActionType.entries

        // Warm up
        repeat(100) {
            actionTypes.forEach { PromptScene.fromActionType(it) }
        }

        // Measure
        val time = measureTimeMillis {
            repeat(BATCH_SIZE) {
                actionTypes.forEach { PromptScene.fromActionType(it) }
            }
        }

        val avgTime = time.toDouble() / BATCH_SIZE
        println("fromActionType (所有类型) 平均耗时: ${avgTime}ms")

        assertTrue(
            "fromActionType 平均耗时应该小于 ${SCENE_FILTER_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < SCENE_FILTER_THRESHOLD_MS
        )
    }

    // ==================== 综合性能测试 ====================

    @Test
    fun `完整提示词准备流程性能应该在阈值内`() {
        val contact = createTestContact()

        // 模拟完整的提示词准备流程
        fun preparePrompt(): String {
            // 1. 获取场景
            val scene = PromptScene.fromActionType(
                com.empathy.ai.domain.model.ActionType.ANALYZE
            )
            // 2. 构建上下文
            val context = PromptContext.fromContact(contact)
            // 3. 获取配置
            val config = GlobalPromptConfig.createDefault()
            val sceneConfig = config.prompts[scene]
            // 4. 解析变量
            val variables = scene.availableVariables.map { variable ->
                variable to (context.getVariable(variable) ?: "")
            }
            // 5. 返回结果
            return "${sceneConfig?.userPrompt} - ${variables.joinToString()}"
        }

        // Warm up
        repeat(10) { preparePrompt() }

        // Measure
        val time = measureTimeMillis {
            repeat(100) {
                preparePrompt()
            }
        }

        val avgTime = time.toDouble() / 100
        println("完整提示词准备流程 平均耗时: ${avgTime}ms")

        assertTrue(
            "完整提示词准备流程 平均耗时应该小于 ${BATCH_OPERATION_THRESHOLD_MS}ms，实际: ${avgTime}ms",
            avgTime < BATCH_OPERATION_THRESHOLD_MS
        )
    }

    @Test
    fun `并发场景过滤性能应该稳定`() {
        // Warm up - 预热JIT编译
        repeat(100) {
            PromptScene.getSettingsScenes()
            PromptScene.getActiveScenes()
            PromptScene.SETTINGS_SCENE_ORDER
        }

        // 模拟多次连续调用（模拟UI快速刷新场景）
        val times = mutableListOf<Long>()

        repeat(10) { round ->
            val time = measureTimeMillis {
                repeat(100) {
                    PromptScene.getSettingsScenes()
                    PromptScene.getActiveScenes()
                    PromptScene.SETTINGS_SCENE_ORDER
                }
            }
            times.add(time)
        }

        val avgTime = times.average()
        val maxTime = times.maxOrNull() ?: 0
        val minTime = times.minOrNull() ?: 0

        println("并发场景过滤 - 平均: ${avgTime}ms, 最大: ${maxTime}ms, 最小: ${minTime}ms")

        // 验证性能稳定性（最大值不应该超过平均值的5倍，考虑JIT编译和GC影响）
        // 同时确保最大值在合理范围内（小于100ms）
        assertTrue(
            "并发场景过滤性能应该稳定，最大值($maxTime)不应超过平均值(${avgTime})的5倍或100ms",
            maxTime < avgTime * 5 || maxTime < 100
        )
    }

    // ==================== 辅助方法 ====================

    private fun createTestContact(): ContactProfile {
        val now = System.currentTimeMillis()
        return ContactProfile(
            id = "contact-test",
            name = "测试联系人",
            targetGoal = "测试目标",
            facts = listOf(
                Fact(key = "雷区", value = "敏感话题1", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "雷区", value = "敏感话题2", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "策略", value = "沟通策略1", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "策略", value = "沟通策略2", timestamp = now, source = FactSource.AI_INFERRED),
                Fact(key = "爱好", value = "读书", timestamp = now, source = FactSource.MANUAL),
                Fact(key = "职业", value = "工程师", timestamp = now, source = FactSource.AI_INFERRED)
            )
        )
    }
}
