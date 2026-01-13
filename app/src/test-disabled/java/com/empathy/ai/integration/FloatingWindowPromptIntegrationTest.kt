package com.empathy.ai.integration

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 悬浮窗提示词集成测试
 *
 * 验证悬浮窗各模式与提示词系统的集成：
 * - 分析模式使用ANALYZE场景
 * - 润色模式使用POLISH场景
 * - 回复模式使用REPLY场景
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
class FloatingWindowPromptIntegrationTest {

    // ==================== ActionType到PromptScene映射测试 ====================

    @Test
    fun `悬浮窗分析模式应该映射到ANALYZE场景`() {
        val scene = PromptScene.fromActionType(ActionType.ANALYZE)
        assertEquals(PromptScene.ANALYZE, scene)
    }

    @Test
    fun `悬浮窗润色模式应该映射到POLISH场景`() {
        val scene = PromptScene.fromActionType(ActionType.POLISH)
        assertEquals(PromptScene.POLISH, scene)
    }

    @Test
    fun `悬浮窗回复模式应该映射到REPLY场景`() {
        val scene = PromptScene.fromActionType(ActionType.REPLY)
        assertEquals(PromptScene.REPLY, scene)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `废弃的CHECK模式应该映射到POLISH场景`() {
        val scene = PromptScene.fromActionType(ActionType.CHECK)
        assertEquals(PromptScene.POLISH, scene)
    }

    // ==================== 提示词配置获取测试 ====================

    @Test
    fun `分析模式应该能获取ANALYZE场景的提示词配置`() {
        // Given: 创建包含自定义分析提示词的配置
        val customPrompt = "自定义分析指令"
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = customPrompt,
                    enabled = true
                )
            )
        )

        // When: 通过ActionType获取场景
        val scene = PromptScene.fromActionType(ActionType.ANALYZE)
        val sceneConfig = config.prompts[scene]

        // Then: 验证获取到正确的提示词
        assertEquals(customPrompt, sceneConfig?.userPrompt)
    }

    @Test
    fun `润色模式应该能获取POLISH场景的提示词配置`() {
        // Given: 创建包含自定义润色提示词的配置
        val customPrompt = "自定义润色指令"
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = customPrompt,
                    enabled = true
                )
            )
        )

        // When: 通过ActionType获取场景
        val scene = PromptScene.fromActionType(ActionType.POLISH)
        val sceneConfig = config.prompts[scene]

        // Then: 验证获取到正确的提示词
        assertEquals(customPrompt, sceneConfig?.userPrompt)
    }

    @Test
    fun `回复模式应该能获取REPLY场景的提示词配置`() {
        // Given: 创建包含自定义回复提示词的配置
        val customPrompt = "自定义回复指令"
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.REPLY to ScenePromptConfig(
                    userPrompt = customPrompt,
                    enabled = true
                )
            )
        )

        // When: 通过ActionType获取场景
        val scene = PromptScene.fromActionType(ActionType.REPLY)
        val sceneConfig = config.prompts[scene]

        // Then: 验证获取到正确的提示词
        assertEquals(customPrompt, sceneConfig?.userPrompt)
    }

    // ==================== 场景启用状态测试 ====================

    @Test
    fun `禁用的场景应该返回enabled为false`() {
        // Given: 创建禁用ANALYZE场景的配置
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "测试",
                    enabled = false
                )
            )
        )

        // When: 获取场景配置
        val sceneConfig = config.prompts[PromptScene.ANALYZE]

        // Then: 验证场景被禁用
        assertFalse(sceneConfig?.enabled ?: true)
    }

    @Test
    fun `启用的场景应该返回enabled为true`() {
        // Given: 创建启用POLISH场景的配置
        val config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.POLISH to ScenePromptConfig(
                    userPrompt = "测试",
                    enabled = true
                )
            )
        )

        // When: 获取场景配置
        val sceneConfig = config.prompts[PromptScene.POLISH]

        // Then: 验证场景被启用
        assertTrue(sceneConfig?.enabled ?: false)
    }

    // ==================== 提示词变更测试 ====================

    @Test
    fun `修改提示词后应该能获取新值`() {
        // Given: 创建初始配置
        val initialPrompt = "初始提示词"
        val updatedPrompt = "更新后的提示词"
        
        var config = GlobalPromptConfig(
            version = 3,
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = initialPrompt,
                    enabled = true
                )
            )
        )

        // When: 更新提示词
        val updatedPrompts = config.prompts.toMutableMap()
        updatedPrompts[PromptScene.ANALYZE] = ScenePromptConfig(
            userPrompt = updatedPrompt,
            enabled = true
        )
        config = config.copy(prompts = updatedPrompts)

        // Then: 验证获取到更新后的提示词
        assertEquals(updatedPrompt, config.prompts[PromptScene.ANALYZE]?.userPrompt)
    }

    // ==================== 悬浮窗三种模式完整性测试 ====================

    @Test
    fun `悬浮窗三种模式都应该有对应的活跃场景`() {
        val floatingWindowActions = listOf(
            ActionType.ANALYZE,
            ActionType.POLISH,
            ActionType.REPLY
        )

        val activeScenes = PromptScene.getActiveScenes()

        floatingWindowActions.forEach { action ->
            val scene = PromptScene.fromActionType(action)
            assertTrue(
                "ActionType.$action 对应的场景 $scene 应该是活跃场景",
                activeScenes.contains(scene)
            )
        }
    }

    @Test
    fun `悬浮窗三种模式对应的场景都应该在设置界面显示`() {
        val floatingWindowActions = listOf(
            ActionType.ANALYZE,
            ActionType.POLISH,
            ActionType.REPLY
        )

        val settingsScenes = PromptScene.getSettingsScenes()

        floatingWindowActions.forEach { action ->
            val scene = PromptScene.fromActionType(action)
            assertTrue(
                "ActionType.$action 对应的场景 $scene 应该在设置界面显示",
                settingsScenes.contains(scene)
            )
        }
    }

    @Test
    fun `默认配置应该包含悬浮窗三种模式的场景`() {
        val config = GlobalPromptConfig.createDefault()

        assertTrue(config.prompts.containsKey(PromptScene.ANALYZE))
        assertTrue(config.prompts.containsKey(PromptScene.POLISH))
        assertTrue(config.prompts.containsKey(PromptScene.REPLY))
    }
}
