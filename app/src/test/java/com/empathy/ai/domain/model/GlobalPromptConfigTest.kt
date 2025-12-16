package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GlobalPromptConfig单元测试
 *
 * 测试覆盖：
 * - createDefault()工厂方法
 * - 默认值初始化
 * - 配置属性访问
 */
class GlobalPromptConfigTest {

    @Test
    fun `createDefault should create config with all scenes`() {
        // When
        val config = GlobalPromptConfig.createDefault { scene -> "default for ${scene.name}" }

        // Then
        assertEquals(1, config.version)
        assertNotNull(config.lastModified)
        assertTrue(config.lastModified.isNotEmpty())
        assertEquals(PromptScene.entries.size, config.prompts.size)
    }

    @Test
    fun `createDefault should initialize all scenes with provided prompts`() {
        // Given
        val expectedPrompts = mapOf(
            PromptScene.ANALYZE to "分析提示词",
            PromptScene.CHECK to "检查提示词",
            PromptScene.EXTRACT to "提取提示词",
            PromptScene.SUMMARY to "总结提示词"
        )

        // When
        val config = GlobalPromptConfig.createDefault { scene ->
            expectedPrompts[scene] ?: ""
        }

        // Then
        PromptScene.entries.forEach { scene ->
            val sceneConfig = config.prompts[scene]
            assertNotNull("Scene $scene should have config", sceneConfig)
            assertEquals(expectedPrompts[scene], sceneConfig?.userPrompt)
            assertTrue(sceneConfig?.enabled == true)
            assertTrue(sceneConfig?.history?.isEmpty() == true)
        }
    }

    @Test
    fun `createDefault with empty provider should create empty prompts`() {
        // When
        val config = GlobalPromptConfig.createDefault { "" }

        // Then
        config.prompts.values.forEach { sceneConfig ->
            assertEquals("", sceneConfig.userPrompt)
        }
    }

    @Test
    fun `default constructor should create empty config`() {
        // When
        val config = GlobalPromptConfig()

        // Then
        assertEquals(1, config.version)
        assertEquals("", config.lastModified)
        assertTrue(config.prompts.isEmpty())
    }

    @Test
    fun `copy should preserve all fields`() {
        // Given
        val original = GlobalPromptConfig.createDefault { "test" }

        // When
        val copied = original.copy(version = 2)

        // Then
        assertEquals(2, copied.version)
        assertEquals(original.lastModified, copied.lastModified)
        assertEquals(original.prompts, copied.prompts)
    }

    @Test
    fun `prompts map should be accessible by scene`() {
        // Given
        val config = GlobalPromptConfig.createDefault { scene ->
            "Prompt for ${scene.displayName}"
        }

        // When & Then
        val analyzeConfig = config.prompts[PromptScene.ANALYZE]
        assertNotNull(analyzeConfig)
        assertEquals("Prompt for 聊天分析", analyzeConfig?.userPrompt)
    }
}
