package com.empathy.ai.presentation.ui.screen.settings

import com.empathy.ai.domain.model.PromptScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词设置区域测试
 *
 * 验证 TD-00015 UI 相关功能：
 * - 场景列表正确性
 * - 场景顺序
 * - 废弃场景过滤
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
class PromptSettingsSectionTest {

    @Test
    fun `设置界面应该显示4个提示词选项`() {
        val scenes = PromptScene.SETTINGS_SCENE_ORDER

        assertEquals(4, scenes.size)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `设置界面不应该显示废弃场景`() {
        val scenes = PromptScene.SETTINGS_SCENE_ORDER

        assertFalse(scenes.contains(PromptScene.CHECK))
        assertFalse(scenes.contains(PromptScene.EXTRACT))
    }

    @Test
    fun `场景顺序应该是分析_润色_回复_总结`() {
        val scenes = PromptScene.SETTINGS_SCENE_ORDER

        assertEquals(PromptScene.ANALYZE, scenes[0])
        assertEquals(PromptScene.POLISH, scenes[1])
        assertEquals(PromptScene.REPLY, scenes[2])
        assertEquals(PromptScene.SUMMARY, scenes[3])
    }

    @Test
    fun `每个场景应该有正确的显示名称`() {
        assertEquals("聊天分析", PromptScene.ANALYZE.displayName)
        assertEquals("润色优化", PromptScene.POLISH.displayName)
        assertEquals("生成回复", PromptScene.REPLY.displayName)
        assertEquals("每日总结", PromptScene.SUMMARY.displayName)
    }

    @Test
    fun `每个场景应该有正确的描述`() {
        assertEquals("分析聊天上下文，提供沟通建议", PromptScene.ANALYZE.description)
        assertEquals("优化用户草稿，使表达更得体", PromptScene.POLISH.description)
        assertEquals("根据对方消息生成合适的回复", PromptScene.REPLY.description)
        assertEquals("生成每日对话总结", PromptScene.SUMMARY.description)
    }

    @Test
    fun `getSettingsScenes应该与SETTINGS_SCENE_ORDER包含相同场景`() {
        val settingsScenes = PromptScene.getSettingsScenes()
        val orderedScenes = PromptScene.SETTINGS_SCENE_ORDER

        assertEquals(settingsScenes.size, orderedScenes.size)
        settingsScenes.forEach { scene ->
            assertTrue(orderedScenes.contains(scene))
        }
    }

    @Test
    fun `所有设置场景都应该有可用变量`() {
        val scenes = PromptScene.SETTINGS_SCENE_ORDER

        scenes.forEach { scene ->
            assertTrue(
                "场景 ${scene.name} 应该有可用变量",
                scene.availableVariables.isNotEmpty()
            )
        }
    }

    @Test
    fun `ANALYZE场景应该有正确的可用变量`() {
        val variables = PromptScene.ANALYZE.availableVariables

        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("risk_tags"))
        assertTrue(variables.contains("strategy_tags"))
        assertTrue(variables.contains("facts_count"))
    }

    @Test
    fun `POLISH场景应该有正确的可用变量`() {
        val variables = PromptScene.POLISH.availableVariables

        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("risk_tags"))
    }

    @Test
    fun `REPLY场景应该有正确的可用变量`() {
        val variables = PromptScene.REPLY.availableVariables

        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("risk_tags"))
        assertTrue(variables.contains("strategy_tags"))
    }

    @Test
    fun `SUMMARY场景应该有正确的可用变量`() {
        val variables = PromptScene.SUMMARY.availableVariables

        assertTrue(variables.contains("contact_name"))
        assertTrue(variables.contains("relationship_status"))
        assertTrue(variables.contains("facts_count"))
        assertTrue(variables.contains("today_date"))
    }
}
