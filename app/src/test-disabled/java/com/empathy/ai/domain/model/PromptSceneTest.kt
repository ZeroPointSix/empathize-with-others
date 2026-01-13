package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptScene枚举单元测试
 */
class PromptSceneTest {

    @Test
    fun `should have four scenes`() {
        assertEquals(4, PromptScene.entries.size)
    }

    @Test
    fun `ANALYZE scene should have correct properties`() {
        val scene = PromptScene.ANALYZE
        assertEquals("聊天分析", scene.displayName)
        assertTrue(scene.description.isNotBlank())
        assertTrue(scene.availableVariables.contains("contact_name"))
        assertTrue(scene.availableVariables.contains("relationship_status"))
        assertTrue(scene.availableVariables.contains("risk_tags"))
        assertTrue(scene.availableVariables.contains("strategy_tags"))
        assertTrue(scene.availableVariables.contains("facts_count"))
    }

    @Test
    fun `CHECK scene should have correct properties`() {
        val scene = PromptScene.CHECK
        assertEquals("安全检查", scene.displayName)
        assertTrue(scene.description.isNotBlank())
        assertTrue(scene.availableVariables.contains("contact_name"))
        assertTrue(scene.availableVariables.contains("risk_tags"))
        assertFalse(scene.availableVariables.contains("strategy_tags"))
    }

    @Test
    fun `EXTRACT scene should have correct properties`() {
        val scene = PromptScene.EXTRACT
        assertEquals("信息提取", scene.displayName)
        assertTrue(scene.description.isNotBlank())
        assertTrue(scene.availableVariables.contains("contact_name"))
        assertEquals(1, scene.availableVariables.size)
    }

    @Test
    fun `SUMMARY scene should have correct properties`() {
        val scene = PromptScene.SUMMARY
        assertEquals("每日总结", scene.displayName)
        assertTrue(scene.description.isNotBlank())
        assertTrue(scene.availableVariables.contains("contact_name"))
        assertTrue(scene.availableVariables.contains("relationship_status"))
        assertTrue(scene.availableVariables.contains("facts_count"))
        assertTrue(scene.availableVariables.contains("today_date"))
    }

    @Test
    fun `all scenes should have non-empty displayName`() {
        PromptScene.entries.forEach { scene ->
            assertTrue("${scene.name} should have displayName", scene.displayName.isNotBlank())
        }
    }

    @Test
    fun `all scenes should have non-empty description`() {
        PromptScene.entries.forEach { scene ->
            assertTrue("${scene.name} should have description", scene.description.isNotBlank())
        }
    }

    @Test
    fun `all scenes should have at least one available variable`() {
        PromptScene.entries.forEach { scene ->
            assertTrue(
                "${scene.name} should have at least one variable",
                scene.availableVariables.isNotEmpty()
            )
        }
    }
}
