package com.empathy.ai.data.local

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * PromptFileBackup单元测试
 *
 * 测试覆盖：
 * - 创建备份
 * - 恢复备份
 * - 清理旧备份
 * - 列出备份
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PromptFileBackupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var backup: PromptFileBackup
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = mockk()
        val filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir

        backup = PromptFileBackup(context, testDispatcher)
    }

    // ========== createBackup() 测试 ==========

    @Test
    fun `createBackup should create backup file`() = runTest(testDispatcher) {
        // Given
        val sourceFile = tempFolder.newFile("source.json")
        sourceFile.writeText("test content")

        // When
        val result = backup.createBackup(sourceFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val backups = backup.listBackups()
        advanceUntilIdle()
        assertEquals(1, backups.size)
    }

    @Test
    fun `createBackup should preserve file content`() = runTest(testDispatcher) {
        // Given
        val content = """{"version":1,"prompts":{}}"""
        val sourceFile = tempFolder.newFile("source.json")
        sourceFile.writeText(content)

        // When
        backup.createBackup(sourceFile)
        advanceUntilIdle()

        // Then
        val backups = backup.listBackups()
        advanceUntilIdle()
        assertEquals(1, backups.size)
        assertEquals(content, backups[0].readText())
    }

    @Test
    fun `createBackup should succeed for non-existent source file`() = runTest(testDispatcher) {
        // Given
        val sourceFile = File(tempFolder.root, "non_existent.json")

        // When
        val result = backup.createBackup(sourceFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `createBackup should clean old backups`() = runTest(testDispatcher) {
        // Given - 创建多个备份
        val sourceFile = tempFolder.newFile("source.json")
        sourceFile.writeText("content")

        // When - 创建5个备份
        repeat(5) {
            sourceFile.writeText("content $it")
            backup.createBackup(sourceFile)
            advanceUntilIdle()
            Thread.sleep(10) // 确保时间戳不同
        }

        // Then - 应该只保留3个最新的
        val backups = backup.listBackups()
        advanceUntilIdle()
        assertTrue(backups.size <= 3)
    }

    // ========== restoreFromLatestBackup() 测试 ==========

    @Test
    fun `restoreFromLatestBackup should restore latest backup`() = runTest(testDispatcher) {
        // Given
        val sourceFile = tempFolder.newFile("source.json")
        sourceFile.writeText("original content")
        backup.createBackup(sourceFile)
        advanceUntilIdle()

        val targetFile = File(tempFolder.root, "target.json")

        // When
        val result = backup.restoreFromLatestBackup(targetFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(targetFile.exists())
        assertEquals("original content", targetFile.readText())
    }

    @Test
    fun `restoreFromLatestBackup should fail when no backup exists`() = runTest(testDispatcher) {
        // Given
        val targetFile = File(tempFolder.root, "target.json")

        // When
        val result = backup.restoreFromLatestBackup(targetFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertFalse(targetFile.exists())
    }

    @Test
    fun `restoreFromLatestBackup should restore most recent backup`() = runTest(testDispatcher) {
        // Given - 创建多个备份
        val sourceFile = tempFolder.newFile("source.json")

        sourceFile.writeText("content 1")
        backup.createBackup(sourceFile)
        advanceUntilIdle()
        Thread.sleep(10)

        sourceFile.writeText("content 2")
        backup.createBackup(sourceFile)
        advanceUntilIdle()
        Thread.sleep(10)

        sourceFile.writeText("content 3")
        backup.createBackup(sourceFile)
        advanceUntilIdle()

        val targetFile = File(tempFolder.root, "target.json")

        // When
        val result = backup.restoreFromLatestBackup(targetFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        assertEquals("content 3", targetFile.readText())
    }

    @Test
    fun `restoreFromLatestBackup should create target directory if not exists`() = runTest(testDispatcher) {
        // Given
        val sourceFile = tempFolder.newFile("source.json")
        sourceFile.writeText("content")
        backup.createBackup(sourceFile)
        advanceUntilIdle()

        val targetDir = File(tempFolder.root, "new_dir")
        val targetFile = File(targetDir, "target.json")

        // When
        val result = backup.restoreFromLatestBackup(targetFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(targetFile.exists())
    }

    // ========== listBackups() 测试 ==========

    @Test
    fun `listBackups should return empty list when no backups`() = runTest(testDispatcher) {
        // When
        val backups = backup.listBackups()
        advanceUntilIdle()

        // Then
        assertTrue(backups.isEmpty())
    }

    @Test
    fun `listBackups should return backups sorted by time descending`() = runTest(testDispatcher) {
        // Given
        val sourceFile = tempFolder.newFile("source.json")

        repeat(3) { index ->
            sourceFile.writeText("content $index")
            backup.createBackup(sourceFile)
            advanceUntilIdle()
            Thread.sleep(10)
        }

        // When
        val backups = backup.listBackups()
        advanceUntilIdle()

        // Then
        assertEquals(3, backups.size)
        // 验证按时间倒序排列
        for (i in 0 until backups.size - 1) {
            assertTrue(backups[i].lastModified() >= backups[i + 1].lastModified())
        }
    }

    // ========== 边界情况测试 ==========

    @Test
    fun `createBackup should handle empty file`() = runTest(testDispatcher) {
        // Given
        val sourceFile = tempFolder.newFile("empty.json")
        // 文件为空

        // When
        val result = backup.createBackup(sourceFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `createBackup should handle large file`() = runTest(testDispatcher) {
        // Given
        val sourceFile = tempFolder.newFile("large.json")
        val largeContent = "A".repeat(100000) // 100KB
        sourceFile.writeText(largeContent)

        // When
        val result = backup.createBackup(sourceFile)
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        val backups = backup.listBackups()
        advanceUntilIdle()
        assertEquals(1, backups.size)
        assertEquals(largeContent, backups[0].readText())
    }
}
