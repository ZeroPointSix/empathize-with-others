package com.empathy.ai.presentation.ui.screen.prompt

import com.empathy.ai.domain.model.PromptScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptSceneTab相关测试
 *
 * 测试场景Tab组件的数据逻辑
 */
class PromptSceneTabTest {

    // ========== PromptScene.SETTINGS_SCENE_ORDER测试 ==========

    @Test
    fun `SETTINGS_SCENE_ORDER contains exactly 4 scenes`() {
        assertEquals(4, PromptScene.SETTINGS_SCENE_ORDER.size)
    }

    @Test
    fun `SETTINGS_SCENE_ORDER contains ANALYZE scene`() {
        assertTrue(PromptScene.SETTINGS_SCENE_ORDER.contains(PromptScene.ANALYZE))
    }

    @Test
    fun `SETTINGS_SCENE_ORDER contains POLISH scene`() {
        assertTrue(PromptScene.SETTINGS_SCENE_ORDER.contains(PromptScene.POLISH))
    }

    @Test
    fun `SETTINGS_SCENE_ORDER contains REPLY scene`() {
        assertTrue(PromptScene.SETTINGS_SCENE_ORDER.contains(PromptScene.REPLY))
    }

    @Test
    fun `SETTINGS_SCENE_ORDER contains SUMMARY scene`() {
        assertTrue(PromptScene.SETTINGS_SCENE_ORDER.contains(PromptScene.SUMMARY))
    }

    @Test
    fun `SETTINGS_SCENE_ORDER does not contain CHECK scene`() {
        assertTrue(!PromptScene.SETTINGS_SCENE_ORDER.contains(PromptScene.CHECK))
    }

    @Test
    fun `SETTINGS_SCENE_ORDER does not contain EXTRACT scene`() {
        assertTrue(!PromptScene.SETTINGS_SCENE_ORDER.contains(PromptScene.EXTRACT))
    }

    // ========== PromptScene.displayName测试 ==========

    @Test
    fun `ANALYZE displayName is correct`() {
        assertEquals("分析", PromptScene.ANALYZE.displayName)
    }

    @Test
    fun `POLISH displayName is correct`() {
        assertEquals("润色", PromptScene.POLISH.displayName)
    }

    @Test
    fun `REPLY displayName is correct`() {
        assertEquals("回复", PromptScene.REPLY.displayName)
    }

    @Test
    fun `SUMMARY displayName is correct`() {
        assertEquals("总结", PromptScene.SUMMARY.displayName)
    }

    // ========== 场景顺序测试 ==========

    @Test
    fun `SETTINGS_SCENE_ORDER has correct order`() {
        val expectedOrder = listOf(
            PromptScene.ANALYZE,
            PromptScene.POLISH,
            PromptScene.REPLY,
            PromptScene.SUMMARY
        )
        assertEquals(expectedOrder, PromptScene.SETTINGS_SCENE_ORDER)
    }
}
