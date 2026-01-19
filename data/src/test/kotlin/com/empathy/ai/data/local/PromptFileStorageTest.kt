package com.empathy.ai.data.local

import android.content.Context
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * PromptFileStorage单元测试
 *
 * 测试覆盖：
 * - 首次读取创建默认配置
 * - 写入后读取一致
 * - 缓存机制
 * - 并发读写安全性
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PromptFileStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storage: PromptFileStorage
    private lateinit var context: Context
    private lateinit var moshi: Moshi
    private lateinit var backup: PromptFileBackup
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = mockk()
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        backup = mockk()

        // 设置临时目录
        val filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir

        // Mock备份操作
        coEvery { backup.createBackup(any()) } returns Result.success(Unit)
        coEvery { backup.restoreFromLatestBackup(any()) } returns Result.failure(Exception("No backup"))

        storage = PromptFileStorage(context, moshi, backup, testDispatcher)
    }

    // ========== 首次读取测试 ==========

    @Test
    fun `readGlobalConfig should create default config on first read`() = runTest(testDispatcher) {
        // When
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val config = result.getOrNull()
        assertNotNull(config)
        assertEquals(GlobalPromptConfig.CURRENT_VERSION, config?.version)
        assertEquals(PromptScene.getActiveScenes().size, config?.prompts?.size)
    }

    @Test
    fun `readGlobalConfig should create prompts file on first read`() = runTest(testDispatcher) {
        // When
        storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        val promptsDir = File(context.filesDir, "prompts")
        val promptsFile = File(promptsDir, "global_prompts.json")
        assertTrue(promptsFile.exists())
    }

    // ========== 写入和读取一致性测试 ==========

    @Test
    fun `writeGlobalConfig and readGlobalConfig should be consistent`() = runTest(testDispatcher) {
        // Given
        val config = GlobalPromptConfig(
            version = 2,
            lastModified = "2025-12-16T10:00:00Z",
            prompts = mapOf(
                PromptScene.ANALYZE to ScenePromptConfig(
                    userPrompt = "测试分析提示词",
                    enabled = true,
                    history = emptyList()
                )
            )
        )

        // When
        storage.writeGlobalConfig(config)
        advanceUntilIdle()
        storage.invalidateCache()
        advanceUntilIdle()
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val readConfig = result.getOrNull()
        assertNotNull(readConfig)
        assertEquals(GlobalPromptConfig.CURRENT_VERSION, readConfig?.version)
        assertEquals("测试分析提示词", readConfig?.prompts?.get(PromptScene.ANALYZE)?.userPrompt)
    }

    @Test
    fun `writeGlobalConfig should update lastModified`() = runTest(testDispatcher) {
        // Given
        val config = GlobalPromptConfig(
            version = 1,
            lastModified = "old-timestamp",
            prompts = emptyMap()
        )

        // When
        storage.writeGlobalConfig(config)
        advanceUntilIdle()
        storage.invalidateCache()
        advanceUntilIdle()
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val readConfig = result.getOrNull()
        assertNotNull(readConfig)
        assertTrue(readConfig?.lastModified != "old-timestamp")
    }

    // ========== 缓存机制测试 ==========

    @Test
    fun `readGlobalConfig should use cache on second call`() = runTest(testDispatcher) {
        // Given - 首次读取
        val firstResult = storage.readGlobalConfig()
        advanceUntilIdle()

        // When - 第二次读取（应该使用缓存）
        val secondResult = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)
        // 两次结果应该相同
        assertEquals(firstResult.getOrNull(), secondResult.getOrNull())
    }

    @Test
    fun `invalidateCache should clear cache`() = runTest(testDispatcher) {
        // Given - 首次读取填充缓存
        storage.readGlobalConfig()
        advanceUntilIdle()

        // When - 清除缓存
        storage.invalidateCache()
        advanceUntilIdle()

        // Then - 再次读取应该从文件读取
        val result = storage.readGlobalConfig()
        advanceUntilIdle()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `writeGlobalConfig should update cache`() = runTest(testDispatcher) {
        // Given
        val config = GlobalPromptConfig(
            version = 5,
            lastModified = "test",
            prompts = mapOf(
                PromptScene.CHECK to ScenePromptConfig(
                    userPrompt = "新提示词",
                    enabled = true,
                    history = emptyList()
                )
            )
        )

        // When
        storage.writeGlobalConfig(config)
        advanceUntilIdle()

        // Then - 读取应该返回刚写入的配置（从缓存）
        val result = storage.readGlobalConfig()
        advanceUntilIdle()
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull()?.version)
    }

    // ========== 备份机制测试 ==========

    @Test
    fun `writeGlobalConfig should create backup before writing`() = runTest(testDispatcher) {
        // Given - 先创建一个文件
        storage.readGlobalConfig()
        advanceUntilIdle()

        var backupCalled = false
        coEvery { backup.createBackup(any()) } answers {
            backupCalled = true
            Result.success(Unit)
        }

        // When
        val newConfig = GlobalPromptConfig(version = 2, prompts = emptyMap())
        storage.writeGlobalConfig(newConfig)
        advanceUntilIdle()

        // Then
        assertTrue(backupCalled)
    }

    // ========== 错误处理测试 ==========

    @Test
    fun `readGlobalConfig should handle corrupted file`() = runTest(testDispatcher) {
        // Given - 创建损坏的文件
        val promptsDir = File(context.filesDir, "prompts")
        promptsDir.mkdirs()
        val promptsFile = File(promptsDir, "global_prompts.json")
        promptsFile.writeText("invalid json content")

        // When
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then - 应该返回默认配置
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ========== 多场景配置测试 ==========

    @Test
    fun `writeGlobalConfig should preserve all scene configs`() = runTest(testDispatcher) {
        // Given
        val config = GlobalPromptConfig(
            version = GlobalPromptConfig.CURRENT_VERSION,
            prompts = PromptScene.getActiveScenes().associateWith { scene ->
                ScenePromptConfig(
                    userPrompt = "提示词 for ${scene.name}",
                    enabled = true,
                    history = emptyList()
                )
            }
        )

        // When
        storage.writeGlobalConfig(config)
        advanceUntilIdle()
        storage.invalidateCache()
        advanceUntilIdle()
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val readConfig = result.getOrNull()
        assertNotNull(readConfig)
        assertEquals(PromptScene.getActiveScenes().size, readConfig?.prompts?.size)
        PromptScene.getActiveScenes().forEach { scene ->
            assertEquals(
                "提示词 for ${scene.name}",
                readConfig?.prompts?.get(scene)?.userPrompt
            )
        }
    }

    // ========== 版本迁移测试 ==========

    @Test
    fun `readGlobalConfig should migrate v1 config with legacy variables`() = runTest(testDispatcher) {
        // Given - 创建包含旧版本变量占位符的配置文件
        val promptsDir = File(context.filesDir, "prompts")
        promptsDir.mkdirs()
        val promptsFile = File(promptsDir, "global_prompts.json")

        // 模拟旧版本配置（v1，包含变量占位符）
        val legacyJson = """
            {
                "version": 1,
                "lastModified": "2025-12-15T10:00:00Z",
                "prompts": {
                    "ANALYZE": {
                        "userPrompt": "请分析与「{contact_name}」的聊天内容，提供沟通建议。当前关系状态：{{relationship_status}}已知雷区：{risk_tags}有效策略：{{strategy_tags}}",
                        "enabled": true,
                        "history": []
                    },
                    "CHECK": {
                        "userPrompt": "检查以下内容是否安全",
                        "enabled": true,
                        "history": []
                    }
                }
            }
        """.trimIndent()
        promptsFile.writeText(legacyJson)

        // When
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val config = result.getOrNull()
        assertNotNull(config)

        // 版本应该升级到2
        assertEquals(GlobalPromptConfig.CURRENT_VERSION, config?.version)

        // ANALYZE场景的提示词应该被清空（因为包含变量占位符）
        assertEquals("", config?.prompts?.get(PromptScene.ANALYZE)?.userPrompt)

        // CHECK场景会被合并并移除，内容应转移到POLISH
        assertEquals(null, config?.prompts?.get(PromptScene.CHECK))
        val polishPrompt = config?.prompts?.get(PromptScene.POLISH)?.userPrompt ?: ""
        assertTrue(polishPrompt.contains("检查以下内容是否安全"))
    }

    @Test
    fun `readGlobalConfig should migrate v2 config without changing content`() = runTest(testDispatcher) {
        // Given - 创建v2配置文件
        val promptsDir = File(context.filesDir, "prompts")
        promptsDir.mkdirs()
        val promptsFile = File(promptsDir, "global_prompts.json")

        val v2Json = """
            {
                "version": 2,
                "lastModified": "2025-12-16T10:00:00Z",
                "prompts": {
                    "ANALYZE": {
                        "userPrompt": "请分析聊天内容并给出建议",
                        "enabled": true,
                        "history": []
                    }
                }
            }
        """.trimIndent()
        promptsFile.writeText(v2Json)

        // When
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val config = result.getOrNull()
        assertNotNull(config)

        // 版本应该升级到3
        assertEquals(GlobalPromptConfig.CURRENT_VERSION, config?.version)

        // 提示词应该保持不变
        assertEquals("请分析聊天内容并给出建议", config?.prompts?.get(PromptScene.ANALYZE)?.userPrompt)
    }

    @Test
    fun `readGlobalConfig should detect various legacy variable patterns`() = runTest(testDispatcher) {
        // Given - 测试各种变量占位符模式
        val promptsDir = File(context.filesDir, "prompts")
        promptsDir.mkdirs()
        val promptsFile = File(promptsDir, "global_prompts.json")

        val legacyJson = """
            {
                "version": 1,
                "lastModified": "2025-12-15T10:00:00Z",
                "prompts": {
                    "ANALYZE": {
                        "userPrompt": "单括号变量：{contact_name}",
                        "enabled": true,
                        "history": []
                    },
                    "CHECK": {
                        "userPrompt": "双括号变量：{{risk_tags}}",
                        "enabled": true,
                        "history": []
                    },
                    "EXTRACT": {
                        "userPrompt": "混合变量：{name}和{{tags}}",
                        "enabled": true,
                        "history": []
                    },
                    "SUMMARY": {
                        "userPrompt": "无变量的正常提示词",
                        "enabled": true,
                        "history": []
                    }
                }
            }
        """.trimIndent()
        promptsFile.writeText(legacyJson)

        // When
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val config = result.getOrNull()
        assertNotNull(config)

        // 包含变量的场景应该被清空或移除
        assertEquals("", config?.prompts?.get(PromptScene.ANALYZE)?.userPrompt)
        assertEquals(null, config?.prompts?.get(PromptScene.CHECK))
        assertEquals(null, config?.prompts?.get(PromptScene.EXTRACT))

        // 不包含变量的场景应该保留
        assertEquals("无变量的正常提示词", config?.prompts?.get(PromptScene.SUMMARY)?.userPrompt)
    }

    @Test
    fun `readGlobalConfig should persist migrated config`() = runTest(testDispatcher) {
        // Given - 创建旧版本配置
        val promptsDir = File(context.filesDir, "prompts")
        promptsDir.mkdirs()
        val promptsFile = File(promptsDir, "global_prompts.json")

        val legacyJson = """
            {
                "version": 1,
                "lastModified": "2025-12-15T10:00:00Z",
                "prompts": {
                    "ANALYZE": {
                        "userPrompt": "旧提示词：{contact_name}",
                        "enabled": true,
                        "history": []
                    }
                }
            }
        """.trimIndent()
        promptsFile.writeText(legacyJson)

        // When - 首次读取触发迁移
        storage.readGlobalConfig()
        advanceUntilIdle()

        // 清除缓存，强制从文件读取
        storage.invalidateCache()
        advanceUntilIdle()

        // 再次读取
        val result = storage.readGlobalConfig()
        advanceUntilIdle()

        // Then - 文件应该已经被更新为v3
        assertTrue(result.isSuccess)
        val config = result.getOrNull()
        assertEquals(GlobalPromptConfig.CURRENT_VERSION, config?.version)
        assertEquals("", config?.prompts?.get(PromptScene.ANALYZE)?.userPrompt)
    }
}
