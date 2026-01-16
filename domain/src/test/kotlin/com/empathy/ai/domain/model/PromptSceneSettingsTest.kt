package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptScene 设置优化相关测试
 *
 * 验证 TD-00015 提示词设置优化功能：
 * - 场景过滤方法
 * - 废弃标记
 * - ActionType 映射
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
class PromptSceneSettingsTest {

    @Test
    fun `getSettingsScenes应该返回4个核心场景`() {
        val scenes = PromptScene.getSettingsScenes()

        assertEquals(5, scenes.size)
        assertTrue(scenes.contains(PromptScene.ANALYZE))
        assertTrue(scenes.contains(PromptScene.POLISH))
        assertTrue(scenes.contains(PromptScene.REPLY))
        assertTrue(scenes.contains(PromptScene.SUMMARY))
        assertTrue(scenes.contains(PromptScene.AI_ADVISOR))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `getSettingsScenes不应该包含废弃场景`() {
        val scenes = PromptScene.getSettingsScenes()

        assertFalse(scenes.contains(PromptScene.CHECK))
        assertFalse(scenes.contains(PromptScene.EXTRACT))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `fromActionType应该将CHECK映射到POLISH`() {
        val scene = PromptScene.fromActionType(ActionType.CHECK)

        assertEquals(PromptScene.POLISH, scene)
    }

    @Test
    fun `fromActionType应该正确映射ANALYZE`() {
        val scene = PromptScene.fromActionType(ActionType.ANALYZE)

        assertEquals(PromptScene.ANALYZE, scene)
    }

    @Test
    fun `fromActionType应该正确映射POLISH`() {
        val scene = PromptScene.fromActionType(ActionType.POLISH)

        assertEquals(PromptScene.POLISH, scene)
    }

    @Test
    fun `fromActionType应该正确映射REPLY`() {
        val scene = PromptScene.fromActionType(ActionType.REPLY)

        assertEquals(PromptScene.REPLY, scene)
    }

    @Test
    fun `SETTINGS_SCENE_ORDER应该按正确顺序排列`() {
        val order = PromptScene.SETTINGS_SCENE_ORDER

        assertEquals(5, order.size)
        assertEquals(PromptScene.ANALYZE, order[0])
        assertEquals(PromptScene.POLISH, order[1])
        assertEquals(PromptScene.REPLY, order[2])
        assertEquals(PromptScene.SUMMARY, order[3])
        assertEquals(PromptScene.AI_ADVISOR, order[4])
    }

    @Test
    fun `getActiveScenes应该返回非废弃场景`() {
        val activeScenes = PromptScene.getActiveScenes()

        assertEquals(5, activeScenes.size)
        activeScenes.forEach { scene ->
            assertFalse(scene.isDeprecated)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `废弃场景的isDeprecated应该为true`() {
        assertTrue(PromptScene.CHECK.isDeprecated)
        assertTrue(PromptScene.EXTRACT.isDeprecated)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `废弃场景的showInSettings应该为false`() {
        assertFalse(PromptScene.CHECK.showInSettings)
        assertFalse(PromptScene.EXTRACT.showInSettings)
    }

    @Test
    fun `活跃场景的isDeprecated应该为false`() {
        assertFalse(PromptScene.ANALYZE.isDeprecated)
        assertFalse(PromptScene.POLISH.isDeprecated)
        assertFalse(PromptScene.REPLY.isDeprecated)
        assertFalse(PromptScene.SUMMARY.isDeprecated)
        assertFalse(PromptScene.AI_ADVISOR.isDeprecated)
    }

    @Test
    fun `活跃场景的showInSettings应该为true`() {
        assertTrue(PromptScene.ANALYZE.showInSettings)
        assertTrue(PromptScene.POLISH.showInSettings)
        assertTrue(PromptScene.REPLY.showInSettings)
        assertTrue(PromptScene.SUMMARY.showInSettings)
        assertTrue(PromptScene.AI_ADVISOR.showInSettings)
    }

    @Test
    fun `getSettingsScenes返回的场景应该都有showInSettings为true`() {
        val scenes = PromptScene.getSettingsScenes()

        scenes.forEach { scene ->
            assertTrue("场景 ${scene.name} 的 showInSettings 应该为 true", scene.showInSettings)
        }
    }

    @Test
    fun `getActiveScenes返回的场景应该都有isDeprecated为false`() {
        val scenes = PromptScene.getActiveScenes()

        scenes.forEach { scene ->
            assertFalse("场景 ${scene.name} 的 isDeprecated 应该为 false", scene.isDeprecated)
        }
    }
}
