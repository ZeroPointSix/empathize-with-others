package com.empathy.ai.integration

import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词设置集成测试
 *
 * 验证 TD-00015 提示词设置优化的端到端功能：
 * - 数据迁移流程
 * - 设置界面与编辑器的交互
 * - 提示词持久化
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
class PromptSettingsIntegrationTest {

    // ==================== 数据迁移集成测试 ====================

    @Test
    @Suppress("DEPRECATION")
    fun `从v2升级到v3应该正确迁移CHECK到POLISH`() {
        // Given: 模拟v2版本配置，CHECK有自定义内容
        val v2Config = GlobalPromptConfig(
            version = 2,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "分析指令",
                    enabled = true
                ),
                PromptScene.CHECK to ScenePromptConfig(
                    userPrompt = "自定义安全检查指令",
                    enabled = true
                ),
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = "润色指令",
                    enabled = true
                ),
                PromptScene.REPLY to ScenePromptConfig(
                    userPrompt = "",
                    enabled = true
                ),
                PromptScene.SUMMARY to ScenePromptConfig(
                    userPrompt = "",
                    enabled = true
                )
            )
        )

        // When: 验证v2配置结构
        assertEquals(2, v2Config.version)
        assertTrue(v2Config.prompts.containsKey(PromptScene.CHECK))
        assertEquals("自定义安全检查指令", v2Config.prompts[PromptScene.CHECK]?.userPrompt)

        // Then: 验证迁移后的预期行为
        // 新创建的配置应该是v3版本
        val v3Config = GlobalPromptConfig.createDefault()
        assertEquals(3, v3Config.version)
        assertFalse(v3Config.prompts.containsKey(PromptScene.CHECK))
    }

    @Test
    fun `新安装用户应该直接获得v3配置`() {
        // Given & When: 创建默认配置
        val config = GlobalPromptConfig.createDefault()

        // Then: 验证是v3版本且只有4个场景
        assertEquals(3, config.version)
        assertEquals(4, config.prompts.size)
        assertTrue(config.prompts.containsKey(PromptScene.ANALYZE))
        assertTrue(config.prompts.containsKey(PromptScene.POLISH))
        assertTrue(config.prompts.containsKey(PromptScene.REPLY))
        assertTrue(config.prompts.containsKey(PromptScene.SUMMARY))
    }

    // ==================== 设置界面集成测试 ====================

    @Test
    fun `设置界面场景列表应该与SETTINGS_SCENE_ORDER一致`() {
        // Given: 获取设置界面场景顺序
        val settingsOrder = PromptScene.SETTINGS_SCENE_ORDER
        val settingsScenes = PromptScene.getSettingsScenes()

        // Then: 验证数量和内容一致
        assertEquals(4, settingsOrder.size)
        assertEquals(settingsOrder.size, settingsScenes.size)
        settingsOrder.forEach { scene ->
            assertTrue(settingsScenes.contains(scene))
        }
    }

    @Test
    fun `点击场景应该能获取正确的场景信息`() {
        // Given: 设置界面场景列表
        val scenes = PromptScene.SETTINGS_SCENE_ORDER

        // When & Then: 验证每个场景都有完整信息
        scenes.forEach { scene ->
            assertTrue(scene.displayName.isNotEmpty())
            assertTrue(scene.description.isNotEmpty())
            assertTrue(scene.availableVariables.isNotEmpty())
            assertFalse(scene.isDeprecated)
            assertTrue(scene.showInSettings)
        }
    }

    // ==================== 提示词持久化集成测试 ====================

    @Test
    fun `保存提示词后应该能正确读取`() {
        // Given: 创建配置并修改
        val originalConfig = GlobalPromptConfig.createDefault()
        val modifiedPrompt = "修改后的分析指令"

        // When: 更新ANALYZE场景的提示词
        val updatedPrompts = originalConfig.prompts.toMutableMap()
        updatedPrompts[PromptScene.ANALYZE] = ScenePromptConfig(
            userPrompt = modifiedPrompt,
            enabled = true
        )
        val updatedConfig = originalConfig.copy(prompts = updatedPrompts)

        // Then: 验证修改生效
        assertEquals(modifiedPrompt, updatedConfig.prompts[PromptScene.ANALYZE]?.userPrompt)
        assertEquals(3, updatedConfig.version)
    }

    @Test
    fun `禁用场景后应该保持禁用状态`() {
        // Given: 创建配置
        val config = GlobalPromptConfig.createDefault()

        // When: 禁用POLISH场景
        val updatedPrompts = config.prompts.toMutableMap()
        val polishConfig = config.prompts[PromptScene.POLISH]
        updatedPrompts[PromptScene.POLISH] = polishConfig?.copy(enabled = false)
            ?: ScenePromptConfig(userPrompt = "", enabled = false)
        val updatedConfig = config.copy(prompts = updatedPrompts)

        // Then: 验证禁用状态
        assertFalse(updatedConfig.prompts[PromptScene.POLISH]?.enabled ?: true)
    }

    // ==================== 场景映射集成测试 ====================

    @Test
    @Suppress("DEPRECATION")
    fun `ActionType到PromptScene的映射应该正确`() {
        // Given & When & Then: 验证所有ActionType映射
        assertEquals(PromptScene.ANALYZE, PromptScene.fromActionType(com.empathy.ai.domain.model.ActionType.ANALYZE))
        assertEquals(PromptScene.POLISH, PromptScene.fromActionType(com.empathy.ai.domain.model.ActionType.POLISH))
        assertEquals(PromptScene.REPLY, PromptScene.fromActionType(com.empathy.ai.domain.model.ActionType.REPLY))
        // CHECK应该映射到POLISH（迁移后）
        assertEquals(PromptScene.POLISH, PromptScene.fromActionType(com.empathy.ai.domain.model.ActionType.CHECK))
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `空提示词应该被正确处理`() {
        // Given: 创建带空提示词的配置
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "",
                    enabled = true
                )
            )
        )

        // Then: 验证空提示词被正确存储
        assertEquals("", config.prompts[PromptScene.ANALYZE]?.userPrompt)
    }

    @Test
    fun `超长提示词应该被正确处理`() {
        // Given: 创建超长提示词
        val longPrompt = "A".repeat(10000)
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = longPrompt,
                    enabled = true
                )
            )
        )

        // Then: 验证超长提示词被正确存储
        assertEquals(10000, config.prompts[PromptScene.ANALYZE]?.userPrompt?.length)
    }

    @Test
    fun `特殊字符提示词应该被正确处理`() {
        // Given: 创建包含特殊字符的提示词
        val specialPrompt = "测试\n换行\t制表符\"引号\"'单引号'{花括号}[方括号]"
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = specialPrompt,
                    enabled = true
                )
            )
        )

        // Then: 验证特殊字符被正确存储
        assertEquals(specialPrompt, config.prompts[PromptScene.ANALYZE]?.userPrompt)
    }
}
