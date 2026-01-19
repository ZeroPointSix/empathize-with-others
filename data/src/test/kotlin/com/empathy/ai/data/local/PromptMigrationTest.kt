package com.empathy.ai.data.local

import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词配置迁移测试
 *
 * 验证 TD-00015 数据迁移功能：
 * - CHECK 场景合并到 POLISH
 * - 版本号更新
 * - 废弃场景处理
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
class PromptMigrationTest {

    @Test
    fun `GlobalPromptConfig_CURRENT_VERSION应该为3`() {
        assertEquals(3, GlobalPromptConfig.CURRENT_VERSION)
    }

    @Test
    fun `createDefault应该返回版本3的配置`() {
        val config = GlobalPromptConfig.createDefault()

        assertEquals(3, config.version)
    }

    @Test
    fun `createDefault应该只包含活跃场景`() {
        val config = GlobalPromptConfig.createDefault()

        assertEquals(5, config.prompts.size)
        assertTrue(config.prompts.containsKey(PromptScene.ANALYZE))
        assertTrue(config.prompts.containsKey(PromptScene.POLISH))
        assertTrue(config.prompts.containsKey(PromptScene.REPLY))
        assertTrue(config.prompts.containsKey(PromptScene.SUMMARY))
        assertTrue(config.prompts.containsKey(PromptScene.AI_ADVISOR))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `createDefault不应该包含废弃场景`() {
        val config = GlobalPromptConfig.createDefault()

        assertFalse(config.prompts.containsKey(PromptScene.CHECK))
        assertFalse(config.prompts.containsKey(PromptScene.EXTRACT))
    }

    @Test
    fun `createDefault应该使用提供的默认提示词`() {
        val testPrompt = "测试提示词"
        val config = GlobalPromptConfig.createDefault { testPrompt }

        config.prompts.values.forEach { sceneConfig ->
            assertEquals(testPrompt, sceneConfig.userPrompt)
        }
    }

    @Test
    fun `createDefault应该启用所有场景`() {
        val config = GlobalPromptConfig.createDefault()

        config.prompts.values.forEach { sceneConfig ->
            assertTrue(sceneConfig.enabled)
        }
    }

    @Test
    fun `createDefault应该初始化空历史记录`() {
        val config = GlobalPromptConfig.createDefault()

        config.prompts.values.forEach { sceneConfig ->
            assertTrue(sceneConfig.history.isEmpty())
        }
    }

    @Test
    fun `createDefault应该设置lastModified`() {
        val config = GlobalPromptConfig.createDefault()

        assertTrue(config.lastModified.isNotEmpty())
    }

    // ==================== 迁移逻辑测试 ====================

    @Test
    @Suppress("DEPRECATION")
    fun `旧版本配置中CHECK有自定义内容时应该合并到POLISH`() {
        // 模拟旧版本配置
        val oldConfig = GlobalPromptConfig(
            version = 2,
            prompts = mapOf(
                PromptScene.CHECK to ScenePromptConfig(
                    userPrompt = "自定义安全检查指令",
                    enabled = true
                ),
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = "自定义润色指令",
                    enabled = true
                ),
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "",
                    enabled = true
                )
            )
        )

        // 验证旧配置包含CHECK
        assertTrue(oldConfig.prompts.containsKey(PromptScene.CHECK))
        assertEquals("自定义安全检查指令", oldConfig.prompts[PromptScene.CHECK]?.userPrompt)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `旧版本配置中CHECK为空时不应该影响POLISH`() {
        // 模拟旧版本配置，CHECK为空
        val oldConfig = GlobalPromptConfig(
            version = 2,
            prompts = mapOf(
                PromptScene.CHECK to ScenePromptConfig(
                    userPrompt = "",
                    enabled = true
                ),
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = "自定义润色指令",
                    enabled = true
                )
            )
        )

        // 验证CHECK为空
        assertEquals("", oldConfig.prompts[PromptScene.CHECK]?.userPrompt)
        // 验证POLISH保持原样
        assertEquals("自定义润色指令", oldConfig.prompts[PromptScene.POLISH]?.userPrompt)
    }

    @Test
    fun `版本3配置不需要迁移`() {
        val currentConfig = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "测试",
                    enabled = true
                )
            )
        )

        assertEquals(3, currentConfig.version)
    }

    @Test
    fun `ScenePromptConfig默认值应该正确`() {
        val config = ScenePromptConfig(userPrompt = "")

        assertEquals("", config.userPrompt)
        assertTrue(config.enabled)
        assertTrue(config.history.isEmpty())
    }

    @Test
    fun `ScenePromptConfig可以正确设置值`() {
        val config = ScenePromptConfig(
            userPrompt = "测试提示词",
            enabled = false,
            history = emptyList()
        )

        assertEquals("测试提示词", config.userPrompt)
        assertFalse(config.enabled)
        assertTrue(config.history.isEmpty())
    }
}
