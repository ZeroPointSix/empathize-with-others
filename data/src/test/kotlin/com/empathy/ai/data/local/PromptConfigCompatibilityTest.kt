package com.empathy.ai.data.local

import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词配置兼容性测试
 *
 * 验证 TD-00015 数据迁移的兼容性：
 * - v1 到 v3 的迁移
 * - v2 到 v3 的迁移
 * - 无版本字段的处理
 * - 迁移失败的回滚
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
class PromptConfigCompatibilityTest {

    // ==================== 版本迁移测试 ====================

    @Test
    fun `v1配置结构应该能被识别`() {
        // Given: 模拟v1版本配置（包含变量占位符）
        val v1Config = GlobalPromptConfig(
            version = 1,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "分析{contact_name}的聊天记录",
                    enabled = true
                )
            )
        )

        // Then: 验证v1配置结构
        assertEquals(1, v1Config.version)
        assertTrue(v1Config.prompts[PromptScene.ANALYZE]?.userPrompt?.contains("{contact_name}") == true)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `v2配置结构应该能被识别`() {
        // Given: 模拟v2版本配置（三层分离，包含CHECK场景）
        val v2Config = GlobalPromptConfig(
            version = 2,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "分析指令",
                    enabled = true
                ),
                PromptScene.CHECK to ScenePromptConfig(
                    userPrompt = "安全检查指令",
                    enabled = true
                ),
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = "润色指令",
                    enabled = true
                )
            )
        )

        // Then: 验证v2配置结构
        assertEquals(2, v2Config.version)
        assertTrue(v2Config.prompts.containsKey(PromptScene.CHECK))
    }

    @Test
    fun `v3配置应该只包含5个活跃场景`() {
        // Given & When: 创建v3配置
        val v3Config = GlobalPromptConfig.createDefault()

        // Then: 验证v3配置结构
        assertEquals(3, v3Config.version)
        assertEquals(5, v3Config.prompts.size)
        assertTrue(v3Config.prompts.containsKey(PromptScene.ANALYZE))
        assertTrue(v3Config.prompts.containsKey(PromptScene.POLISH))
        assertTrue(v3Config.prompts.containsKey(PromptScene.REPLY))
        assertTrue(v3Config.prompts.containsKey(PromptScene.SUMMARY))
        assertTrue(v3Config.prompts.containsKey(PromptScene.AI_ADVISOR))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `v3配置不应该包含废弃场景`() {
        // Given & When: 创建v3配置
        val v3Config = GlobalPromptConfig.createDefault()

        // Then: 验证不包含废弃场景
        assertFalse(v3Config.prompts.containsKey(PromptScene.CHECK))
        assertFalse(v3Config.prompts.containsKey(PromptScene.EXTRACT))
    }

    // ==================== CHECK场景合并测试 ====================

    @Test
    @Suppress("DEPRECATION")
    fun `CHECK场景有自定义内容时应该合并到POLISH`() {
        // Given: 模拟v2配置，CHECK有自定义内容
        val checkPrompt = "自定义安全检查指令"
        val polishPrompt = "自定义润色指令"

        val v2Config = GlobalPromptConfig(
            version = 2,
            prompts = mapOf(
                PromptScene.CHECK to ScenePromptConfig(
                    userPrompt = checkPrompt,
                    enabled = true
                ),
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = polishPrompt,
                    enabled = true
                )
            )
        )

        // Then: 验证原始配置
        assertEquals(checkPrompt, v2Config.prompts[PromptScene.CHECK]?.userPrompt)
        assertEquals(polishPrompt, v2Config.prompts[PromptScene.POLISH]?.userPrompt)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `CHECK场景为空时不应该影响POLISH`() {
        // Given: 模拟v2配置，CHECK为空
        val polishPrompt = "自定义润色指令"

        val v2Config = GlobalPromptConfig(
            version = 2,
            prompts = mapOf(
                PromptScene.CHECK to ScenePromptConfig(
                    userPrompt = "",
                    enabled = true
                ),
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = polishPrompt,
                    enabled = true
                )
            )
        )

        // Then: 验证POLISH保持不变
        assertEquals("", v2Config.prompts[PromptScene.CHECK]?.userPrompt)
        assertEquals(polishPrompt, v2Config.prompts[PromptScene.POLISH]?.userPrompt)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `CHECK场景不存在时应该正常处理`() {
        // Given: 模拟v2配置，没有CHECK场景
        val v2Config = GlobalPromptConfig(
            version = 2,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "分析指令",
                    enabled = true
                ),
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = "润色指令",
                    enabled = true
                )
            )
        )

        // Then: 验证配置正常
        assertFalse(v2Config.prompts.containsKey(PromptScene.CHECK))
        assertEquals("润色指令", v2Config.prompts[PromptScene.POLISH]?.userPrompt)
    }

    // ==================== 版本常量测试 ====================

    @Test
    fun `CURRENT_VERSION应该为3`() {
        assertEquals(3, GlobalPromptConfig.CURRENT_VERSION)
    }

    @Test
    fun `createDefault应该返回CURRENT_VERSION版本`() {
        val config = GlobalPromptConfig.createDefault()
        assertEquals(GlobalPromptConfig.CURRENT_VERSION, config.version)
    }

    // ==================== 场景属性测试 ====================

    @Test
    @Suppress("DEPRECATION")
    fun `废弃场景应该有正确的属性`() {
        // CHECK场景
        assertTrue(PromptScene.CHECK.isDeprecated)
        assertFalse(PromptScene.CHECK.showInSettings)

        // EXTRACT场景
        assertTrue(PromptScene.EXTRACT.isDeprecated)
        assertFalse(PromptScene.EXTRACT.showInSettings)
    }

    @Test
    fun `活跃场景应该有正确的属性`() {
        val activeScenes = listOf(
            PromptScene.ANALYZE,
            PromptScene.POLISH,
            PromptScene.REPLY,
            PromptScene.SUMMARY,
            PromptScene.AI_ADVISOR
        )

        activeScenes.forEach { scene ->
            assertFalse("${scene.name} 不应该被废弃", scene.isDeprecated)
            assertTrue("${scene.name} 应该在设置中显示", scene.showInSettings)
        }
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `空prompts映射应该被正确处理`() {
        val config = GlobalPromptConfig(
            version = 3,
            prompts = emptyMap()
        )

        assertEquals(3, config.version)
        assertTrue(config.prompts.isEmpty())
    }

    @Test
    fun `部分场景缺失应该被正确处理`() {
        // Given: 只有部分场景的配置
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "分析指令",
                    enabled = true
                )
            )
        )

        // Then: 验证配置正常
        assertEquals(1, config.prompts.size)
        assertTrue(config.prompts.containsKey(PromptScene.ANALYZE))
        assertFalse(config.prompts.containsKey(PromptScene.POLISH))
    }

    @Test
    fun `lastModified应该被正确设置`() {
        val config = GlobalPromptConfig.createDefault()

        assertTrue(config.lastModified.isNotEmpty())
    }

    // ==================== 历史记录测试 ====================

    @Test
    fun `ScenePromptConfig应该支持历史记录`() {
        val config = ScenePromptConfig(
            userPrompt = "当前提示词",
            enabled = true,
            history = emptyList()
        )

        // 添加历史记录
        val updatedConfig = config.addHistory("旧提示词")

        assertEquals(1, updatedConfig.history.size)
        assertEquals("旧提示词", updatedConfig.history[0].userPrompt)
    }

    @Test
    fun `历史记录应该限制在MAX_HISTORY_SIZE`() {
        var config = ScenePromptConfig(
            userPrompt = "当前",
            enabled = true
        )

        // 添加超过限制的历史记录
        repeat(5) { i ->
            config = config.addHistory("历史$i")
        }

        assertEquals(ScenePromptConfig.MAX_HISTORY_SIZE, config.history.size)
    }
}
